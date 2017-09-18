package towhid.icurious.webview

import android.content.Context
import android.databinding.DataBindingUtil
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import towhid.icurious.webview.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private var binding: ActivityMainBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        if (isOnline()) initWebView()
        viewSplash()
    }

    private fun initWebView() {
        binding?.webView!!.settings.javaScriptEnabled = true

        // Stop local links and redirects from opening in browser instead of WebView
        binding?.webView!!.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                view.loadUrl(url)
                return true
            }

            override fun onPageFinished(view: WebView, url: String) {
                binding?.progressBar?.visibility = View.INVISIBLE
            }
        }

        binding?.webView!!.loadUrl(getString(R.string.url))
    }

    private fun viewSplash() {
        if (supportActionBar != null) supportActionBar!!.hide()
        Handler().postDelayed({
            binding?.splash?.visibility = View.GONE
        }, 3000)
    }

    override fun onBackPressed() {
        if (binding?.webView!!.canGoBack()) binding?.webView!!.goBack()
        else super.onBackPressed()
    }

    private fun isOnline(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val connected = cm.activeNetworkInfo != null && cm.activeNetworkInfo.isConnectedOrConnecting
        if (!connected) Toast.makeText(this, "Internet Connection Required", Toast.LENGTH_LONG).show()
        return connected
    }
}