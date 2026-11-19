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
import android.view.WindowInsets
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
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
    private lateinit var offline: LinearLayout
    private var uploadCallback: ValueCallback<Array<Uri>>? = null
    private val uploadRequest = 1001
    private val home = "https://cracktarget.com/"
    private val account = "https://cracktarget.com/my-account/"
    private val navy = Color.rgb(16, 37, 68)
    private var loginHandoffPending = false
    private var appSessionAvailable = false
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + .5f).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.navigationBarColor = Color.rgb(12, 28, 46)
        window.statusBarColor = navy
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            fitsSystemWindows = false
        }
        // Target SDK 35 is edge-to-edge. Keep native navigation ABOVE the Android
        // gesture/navigation area, including on devices using 3-button navigation.
        root.setOnApplyWindowInsetsListener { view, insets ->
            val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { max = 100 }
        root.addView(progress, LinearLayout.LayoutParams(-1, dp(3)))
        web = WebView(this)
        root.addView(web, LinearLayout.LayoutParams(-1, 0, 1f))

        offline = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(12), dp(20), dp(12))
            visibility = View.GONE
            val message = TextView(this@MainActivity).apply {
                text = "Could not load this page. Check your connection and try again."
                setTextColor(navy); textSize = 14f; gravity = Gravity.CENTER
            }
            addView(message, LinearLayout.LayoutParams(-1, -2))
            addView(Button(this@MainActivity).apply {
                text = "Retry"; isAllCaps = false
                setOnClickListener { visibility = View.GONE; web.reload() }
            }, LinearLayout.LayoutParams(-2, dp(48)))
        }
        root.addView(offline, LinearLayout.LayoutParams(-1, -2))

        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(navy)
            gravity = Gravity.CENTER_VERTICAL
        }
        val destinations = listOf(
            "Home" to home,
            "Study" to "https://cracktarget.com/study-directory/",
            "Tests" to "https://cracktarget.com/mocktest/",
            "Affairs" to "https://cracktarget.com/current-affairs/",
            "Account" to account
        )
        for ((label, url) in destinations) {
            val button = TextView(this).apply {
                text = label
                textSize = 12f
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    if (label == "Account" && !appSessionAvailable) {
                        // Google OAuth must NOT run in an embedded WebView. Open the
                        // existing authenticated browser session for account access.
                        openInBrowser("$account?ct_app=1")
                    } else {
                        offline.visibility = View.GONE
                        web.loadUrl(url)
                    }
                }
            }
            nav.addView(button, LinearLayout.LayoutParams(0, dp(56), 1f))
        }
        root.addView(nav, LinearLayout.LayoutParams(-1, dp(56)))
        setContentView(root)
        root.requestApplyInsets()

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
            override fun onPageFinished(view: WebView, url: String?) {
                if (loginHandoffPending && url?.startsWith(account) == true) {
                    appSessionAvailable = true
                    loginHandoffPending = false
                }
            }
            override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                offline.visibility = View.GONE
            }
            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) offline.visibility = View.VISIBLE
            }
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url
                val scheme = uri.scheme?.lowercase()
                val host = uri.host?.lowercase()
                // Keep WooCommerce's authenticated browser session consistent for
                // Google sign-in. Never attempt to copy Google or WooCommerce cookies.
                if (scheme == "https" && (host == "cracktarget.com" || host == "www.cracktarget.com")) {
                    if (uri.path?.startsWith("/my-account") == true) {
                        if (!appSessionAvailable) { openInBrowser("$account?ct_app=1"); return true }
                        return false
                    }
                    return false
                }
                if (scheme == "https" || scheme == "mailto" || scheme == "tel" || scheme == "intent") {
                    openInBrowser(uri.toString())
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
        web.setDownloadListener(DownloadListener { url, userAgent, _, mimeType, _ ->
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
        handleLoginCallback(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLoginCallback(intent)
    }

    private fun handleLoginCallback(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme != "ctapp" || uri.host != "auth") return
        val code = uri.getQueryParameter("code") ?: return
        if (!Regex("^[a-f0-9]{64}$").matches(code)) return
        // Code is short-lived and single-use; the website sets the WooCommerce
        // authentication cookie in THIS WebView rather than the Chrome session.
        loginHandoffPending = true
        web.loadUrl("https://cracktarget.com/?ctab_exchange=" + Uri.encode(code))
        intent.data = null
    }

    private fun openInBrowser(url: String) {
        try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        catch (_: Exception) { Toast.makeText(this, "Browser unavailable", Toast.LENGTH_SHORT).show() }
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
