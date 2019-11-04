package towhid.icurious.webview

import android.media.MediaPlayer
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.widget.FrameLayout

/**
 * This class serves as a WebChromeClient to be set to a WebView, allowing it to play video.
 * Video will play differently depending on target API level (in-line, fullscreen, or both).
 *
 * It has been tested with the following video classes:
 * - android.widget.VideoView (typically API level <11)
 * - android.webkit.HTML5VideoFullScreen$VideoSurfaceView/VideoTextureView (typically API level 11-18)
 * - com.android.org.chromium.content.browser.ContentVideoView$VideoSurfaceView (typically API level 19+)
 *
 * Important notes:
 * - For API level 11+, android:hardwareAccelerated="true" must be set in the application manifest.
 * - The invoking activity must call VideoEnabledWebChromeClient's onBackPressed() inside of its own onBackPressed().
 * - Tested in Android API levels 8-19. Only tested on http://m.youtube.com.
 *
 * @author Cristian Perez (http://cpr.name)
 */
open class VideoEnabledWebChromeClient
/**
 * Builds a video enabled WebChromeClient.
 * @param activityNonVideoView A View in the activity's layout that contains every other view that should be hidden when the video goes full-screen.
 * @param activityVideoView A ViewGroup in the activity's layout that will display the video. Typically you would like this to fill the whole layout.
 * @param loadingView A View to be shown while the video is loading (typically only used in API level <11). Must be already inflated and not attached to a parent view.
 * @param webView The owner VideoEnabledWebView. Passing it will enable the VideoEnabledWebChromeClient to detect the HTML5 video ended event and exit full-screen.
 * Note: The web page must only contain one video tag in order for the HTML5 video ended event to work. This could be improved if needed (see Javascript code).
 */(private val activityNonVideoView: View,
    private val activityVideoView: ViewGroup,
    private val loadingView: View?,
    private val webView: VideoEnabledWebView
) : WebChromeClient(), MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

    /**
     * Indicates if the video is being displayed using a custom view (typically full-screen)
     * @return true it the video is being displayed using a custom view (typically full-screen)
     */
    var isVideoFullscreen: Boolean = false
        private set // Indicates if the video is being displayed using a custom view (typically full-screen)
    private var videoViewContainer: FrameLayout? = null
    private var videoViewCallback: CustomViewCallback? = null

    /**
     * Set a callback that will be fired when the video starts or finishes displaying using a custom view (typically full-screen)
     * @param callback A VideoEnabledWebChromeClient.ToggledFullscreenCallback callback
     */
    var toggledFullscreenCallback: ((Boolean) -> Unit)? = null

    override fun onShowCustomView(view: View, callback: CustomViewCallback) {
        if (view is FrameLayout) {
            // Save video related variables
            isVideoFullscreen = true
            videoViewContainer = view
            videoViewCallback = callback

            // Hide the non-video view, add the video view, and show it
            activityNonVideoView.visibility = View.INVISIBLE
            activityVideoView.addView(videoViewContainer, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            activityVideoView.visibility = View.VISIBLE

            // A video wants to be shown
            val focusedChild = view.focusedChild
            if (focusedChild is android.widget.VideoView) { // android.widget.VideoView (typically API level <11)
                // Handle all the required events
                focusedChild.apply {
                    setOnPreparedListener(this@VideoEnabledWebChromeClient)
                    setOnCompletionListener(this@VideoEnabledWebChromeClient)
                    setOnErrorListener(this@VideoEnabledWebChromeClient)
                }
            } else {
                // Other classes, including:
                // - android.webkit.HTML5VideoFullScreen$VideoSurfaceView, which inherits from android.view.SurfaceView (typically API level 11-18)
                // - android.webkit.HTML5VideoFullScreen$VideoTextureView, which inherits from android.view.TextureView (typically API level 11-18)
                // - com.android.org.chromium.content.browser.ContentVideoView$VideoSurfaceView, which inherits from android.view.SurfaceView (typically API level 19+)

                // Handle HTML5 video ended event only if the class is a SurfaceView
                // Test case: TextureView of Sony Xperia T API level 16 doesn't work fullscreen when loading the javascript below
                if (webView.settings.javaScriptEnabled && focusedChild is SurfaceView) {
                    // Run javascript code that detects the video end and notifies the Javascript interface
                    var js = "javascript:"
                    js += "var _ytrp_html5_video_last;"
                    js += "var _ytrp_html5_video = document.getElementsByTagName('video')[0];"
                    js += "if (_ytrp_html5_video != undefined && _ytrp_html5_video != _ytrp_html5_video_last) {"
                    run {
                        js += "_ytrp_html5_video_last = _ytrp_html5_video;"
                        js += "function _ytrp_html5_video_ended() {"
                        run {
                            js += "_VideoEnabledWebView.notifyVideoEnd();" // Must match Javascript interface name and method of VideoEnableWebView
                        }
                        js += "}"
                        js += "_ytrp_html5_video.addEventListener('ended', _ytrp_html5_video_ended);"
                    }
                    js += "}"
                    webView.loadUrl(js)
                }
            }

            // Notify full-screen change
            toggledFullscreenCallback?.invoke(true)
        }
    }

    // Available in API level 14+, deprecated in API level 18+
    override fun onShowCustomView(view: View, requestedOrientation: Int, callback: CustomViewCallback) = onShowCustomView(view, callback)

    override fun onHideCustomView() {
        // This method should be manually called on video end in all cases because it's not always called automatically.
        // This method must be manually called on back key press (from this class' onBackPressed() method).

        if (isVideoFullscreen) {
            // Hide the video view, remove it, and show the non-video view
            activityVideoView.visibility = View.INVISIBLE
            activityVideoView.removeView(videoViewContainer)
            activityNonVideoView.visibility = View.VISIBLE

            // Call back (only in API level <19, because in API level 19+ with chromium webview it crashes)
            if (videoViewCallback?.javaClass?.name?.contains(".chromium.") == false) videoViewCallback?.onCustomViewHidden()

            // Reset video related variables
            isVideoFullscreen = false
            videoViewContainer = null
            videoViewCallback = null

            // Notify full-screen change
            toggledFullscreenCallback?.invoke(false)
        }
    }

    // Video will start loading
    override fun getVideoLoadingProgressView(): View? = loadingView?.also { it.visibility = View.VISIBLE }
            ?: super.getVideoLoadingProgressView()

    // Video will start playing, only called in the case of android.widget.VideoView (typically API level <11)
    override fun onPrepared(mp: MediaPlayer) {
        loadingView?.visibility = View.GONE
    }

    // Video finished playing, only called in the case of android.widget.VideoView (typically API level <11)
    override fun onCompletion(mp: MediaPlayer) = onHideCustomView()

    // Error while playing video, only called in the case of android.widget.VideoView (typically API level <11)
    override fun onError(mp: MediaPlayer, what: Int, extra: Int) = false // By returning false, onCompletion() will be called

    /**
     * Notifies the class that the back key has been pressed by the user.
     * This must be called from the Activity's onBackPressed(), and if it returns false, the activity itself should handle it. Otherwise don't do anything.
     * @return Returns true if the event was handled, and false if was not (video view is not visible)
     */
    fun onBackPressed() = isVideoFullscreen.also { if (it) onHideCustomView() }
}
