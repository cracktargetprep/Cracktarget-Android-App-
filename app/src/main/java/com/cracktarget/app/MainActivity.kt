package com.cracktarget.app

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.view.View
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var web: WebView
    private lateinit var progress: ProgressBar
    private var uploadCallback: ValueCallback<Array<Uri>>? = null
    private val uploadRequest = 1001
    private val home = "https://cracktarget.com/"
    private val navy = Color.rgb(16, 37, 68)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE) }
        val header = TextView(this).apply {
            text = "CrackTarget   |   Learn · Test · Improve"
            textSize = 17f; setTextColor(Color.WHITE); setBackgroundColor(navy)
            setPadding(20, 26, 12, 26); gravity = Gravity.CENTER_VERTICAL
        }
        root.addView(header, LinearLayout.LayoutParams(-1, -2))
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { max = 100 }
        root.addView(progress, LinearLayout.LayoutParams(-1, 5))
        web = WebView(this)
        root.addView(web, LinearLayout.LayoutParams(-1, 0, 1f))
        val nav = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setBackgroundColor(navy) }
        val destinations = listOf(
            "Home" to home,
            "Study" to "https://cracktarget.com/study-directory/",
            "Tests" to "https://cracktarget.com/mocktest/",
            "Affairs" to "https://cracktarget.com/current-affairs/",
            "Account" to "https://cracktarget.com/my-account/"
        )
        for ((label, url) in destinations) {
            val button = Button(this).apply {
                text = label; textSize = 11f; isAllCaps = false
                setTextColor(Color.WHITE); setBackgroundColor(navy)
                setOnClickListener { web.loadUrl(url) }
            }
            nav.addView(button, LinearLayout.LayoutParams(0, -2, 1f))
        }
        root.addView(nav, LinearLayout.LayoutParams(-1, -2))
        setContentView(root)

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true)
        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = true
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            builtInZoomControls = false
            displayZoomControls = false
        }
        WebView.setWebContentsDebuggingEnabled(false)
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url
                val scheme = uri.scheme?.lowercase()
                if (scheme == "https" && (uri.host == "cracktarget.com" || uri.host == "www.cracktarget.com")) return false
                if (scheme == "https" || scheme == "mailto" || scheme == "tel" || scheme == "intent") {
                    try { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                    catch (_: Exception) { Toast.makeText(this@MainActivity, "No app can open this link", Toast.LENGTH_SHORT).show() }
                }
                return true
            }
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                progress.progress = newProgress
                progress.visibility = if (newProgress >= 100) View.GONE else View.VISIBLE
            }
            override fun onShowFileChooser(view: WebView, callback: ValueCallback<Array<Uri>>, params: FileChooserParams): Boolean {
                uploadCallback?.onReceiveValue(null)
                uploadCallback = callback
                return try { startActivityForResult(params.createIntent(), uploadRequest); true }
                catch (_: Exception) { uploadCallback = null; false }
            }
        }
        web.setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            try {
                val uri = Uri.parse(url)
                if (uri.scheme != "https") throw IllegalArgumentException("HTTPS required")
                val request = DownloadManager.Request(uri)
                    .setMimeType(mimeType)
                    .addRequestHeader("User-Agent", userAgent)
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                val cookie = CookieManager.getInstance().getCookie(url)
                if (!cookie.isNullOrBlank()) request.addRequestHeader("Cookie", cookie)
                request.setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, "cracktarget-download-${System.currentTimeMillis()}")
                (getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
                Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) { Toast.makeText(this, "Download could not start", Toast.LENGTH_SHORT).show() }
        })
        if (savedInstanceState == null) web.loadUrl(home) else web.restoreState(savedInstanceState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == uploadRequest) {
            uploadCallback?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data))
            uploadCallback = null
        }
    }
    @Deprecated("Legacy back navigation")
    override fun onBackPressed() { if (web.canGoBack()) web.goBack() else super.onBackPressed() }
    override fun onSaveInstanceState(outState: Bundle) { web.saveState(outState); super.onSaveInstanceState(outState) }
    override fun onDestroy() { uploadCallback?.onReceiveValue(null); web.destroy(); super.onDestroy() }
}
