package towhid.icurious.webview

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    private val chromeClient: VideoEnabledWebChromeClient by lazy {
        object : VideoEnabledWebChromeClient(
                nonVideoLayout, // Your own view, read class comments
                videoLayout as ViewGroup, // Your own view, read class comments
                layoutInflater.inflate(R.layout.view_loading_video, null), // Your own view, read class comments
                webView) {
            // Subscribe to standard events, such as onProgressChanged()...
            override fun onProgressChanged(view: WebView, progress: Int) {
                // Your code...
            }
        }.also {
            it.setOnToggledFullscreen(object : VideoEnabledWebChromeClient.ToggledFullscreenCallback {
                override fun toggledFullscreen(fullscreen: Boolean) {
                    // Your code to handle the full-screen change, for example showing and hiding the title bar. Example:
                    val attrs = window.attributes
                    if (fullscreen) {
                        attrs.flags = attrs.flags or WindowManager.LayoutParams.FLAG_FULLSCREEN
                        attrs.flags = attrs.flags or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    } else {
                        attrs.flags = attrs.flags and WindowManager.LayoutParams.FLAG_FULLSCREEN.inv()
                        attrs.flags = attrs.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON.inv()
                    }
                    window.attributes = attrs
                    if (android.os.Build.VERSION.SDK_INT >= 14)
                        window.decorView.systemUiVisibility = if (fullscreen) View.SYSTEM_UI_FLAG_LOW_PROFILE else View.SYSTEM_UI_FLAG_VISIBLE
                }
            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        webView?.apply {
            setWebChromeClient(chromeClient)
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, url: String) = true.also { view.loadUrl(url) }
            }
            loadUrl(getString(R.string.url))
        }
    }

    override fun onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack()
        else super.onBackPressed()
    }
}