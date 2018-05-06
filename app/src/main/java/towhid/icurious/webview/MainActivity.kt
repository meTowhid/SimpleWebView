package towhid.icurious.webview

import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()
        if (isOnline) initWebView()
        viewSplash()
    }

    private fun initWebView() {
        webView?.apply {
            settings.javaScriptEnabled = true

            // Stop local links and redirects from opening in browser instead of WebView
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                    view.loadUrl(url)
                    return true
                }

                override fun onPageFinished(view: WebView, url: String) {
                    progressBar?.visibility = View.INVISIBLE
                }
            }

            loadUrl(getString(R.string.url))
        }
    }

    private fun viewSplash() {
        Handler().postDelayed({
            splash?.visibility = View.GONE
        }, 3000)
    }

    override fun onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack()
        else super.onBackPressed()
    }

    private val isOnline: Boolean
        get() {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val connected = cm.activeNetworkInfo != null && cm.activeNetworkInfo.isConnectedOrConnecting
            if (!connected) Toast.makeText(this, "Internet Connection Required", Toast.LENGTH_LONG).show()
            return connected
        }
}