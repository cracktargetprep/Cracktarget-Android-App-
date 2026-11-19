package com.cracktarget.app

import android.app.Activity
import android.annotation.TargetApi
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : Activity() {
    private lateinit var page: FrameLayout
    private lateinit var web: WebView
    private lateinit var progress: ProgressBar
    private lateinit var errorPanel: LinearLayout
    private lateinit var errorMessage: TextView
    private lateinit var googleButton: Button
    private lateinit var browserSignIn: BrowserSignIn
    private lateinit var downloads: Downloads
    private val handler = Handler(Looper.getMainLooper())
    private val slowPage = Runnable { if (loading) showError(R.string.page_slow, false) }
    private val destinations = listOf(
        R.string.home to SITE,
        R.string.study to "${SITE}study-directory/",
        R.string.tests to "${SITE}mocktest/",
        R.string.affairs to "${SITE}current-affairs/",
        R.string.account to ACCOUNT,
    )
    private val navButtons = mutableListOf<TextView>()
    private var uploadCallback: ValueCallback<Array<Uri>>? = null
    private var latestUrl = SITE
    private var loading = false
    private var mainFrameError = false
    private var signingIn = false
    private var systemBack: WebHistoryBack? = null
    private val navy = Color.rgb(16, 37, 68)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        configureWindow()
        if (Build.VERSION.SDK_INT >= 33) systemBack = WebHistoryBack(this) { if (web.canGoBack()) web.goBack() }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
        }
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progressTintList = ColorStateList.valueOf(Color.rgb(36, 119, 88))
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        val loadingTrack = FrameLayout(this)
        loadingTrack.addView(progress, FrameLayout.LayoutParams(-1, -1))
        root.addView(loadingTrack, LinearLayout.LayoutParams(-1, dp(2)))
        googleButton = Button(this).apply {
            setText(R.string.sign_in_google)
            isAllCaps = false
            visibility = View.GONE
            setOnClickListener { browserSignIn.start() }
        }
        root.addView(googleButton, LinearLayout.LayoutParams(-1, -2).apply {
            setMargins(dp(12), dp(3), dp(12), dp(3))
        })
        page = FrameLayout(this)
        root.addView(page, LinearLayout.LayoutParams(-1, 0, 1f))
        errorPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            visibility = View.GONE
            setPadding(dp(20), dp(12), dp(20), dp(12))
            setBackgroundColor(Color.WHITE)
            errorMessage = TextView(this@MainActivity).apply {
                setTextColor(navy)
                textSize = 16f
                gravity = Gravity.CENTER
                accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
            }
            addView(errorMessage, LinearLayout.LayoutParams(-1, -2))
            addView(Button(this@MainActivity).apply {
                setText(R.string.retry)
                isAllCaps = false
                setOnClickListener { load(latestUrl, force = true) }
            }, LinearLayout.LayoutParams(-2, -2))
        }
        page.addView(errorPanel, FrameLayout.LayoutParams(-1, -1))
        val nav = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(navy)
            minimumHeight = dp(52)
        }
        for ((label, url) in destinations) {
            val button = Button(this, null, android.R.attr.borderlessButtonStyle).apply {
                setText(label)
                isAllCaps = false
                textSize = 12f
                gravity = Gravity.CENTER
                minWidth = 0
                minimumWidth = 0
                setPadding(dp(2), dp(4), dp(2), dp(4))
                minHeight = dp(52)
                setTextColor(ColorStateList(
                    arrayOf(intArrayOf(android.R.attr.state_selected), intArrayOf()),
                    intArrayOf(Color.rgb(181, 244, 212), Color.WHITE),
                ))
                setOnClickListener { load(url) }
            }
            navButtons.add(button)
            nav.addView(button, LinearLayout.LayoutParams(0, -2, 1f))
        }
        root.addView(nav, LinearLayout.LayoutParams(-1, -2))
        bindInsets(root, nav)
        setContentView(root)
        root.requestApplyInsets()
        downloads = Downloads(this)
        browserSignIn = BrowserSignIn(this, ::notify, { active ->
            signingIn = active
            googleButton.isEnabled = !active
            showProgress()
        }) { load(ACCOUNT, force = true) }
        CookieManager.getInstance().setAcceptCookie(true)
        createWebView()
        val restored = savedInstanceState?.getBundle("web")?.let(web::restoreState)
        if (restored?.currentItem != null) {
            latestUrl = restored.currentItem?.url?.takeIf { Uri.parse(it).isSite() } ?: SITE
            updateNavigation(latestUrl)
            systemBack?.update(web.canGoBack())
            showProgress()
        } else {
            if (intent?.action != Intent.ACTION_VIEW) load(SITE)
        }
        browserSignIn.accept(intent)
        if (restored?.currentItem == null && intent?.action == Intent.ACTION_VIEW) load(ACCOUNT)
    }

    @Suppress("DEPRECATION")
    private fun configureWindow() {
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.setSystemBarsAppearance(
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
            )
        } else {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
    }

    @Suppress("DEPRECATION")
    private fun bindInsets(root: LinearLayout, nav: LinearLayout) {
        root.setOnApplyWindowInsetsListener { _, insets ->
            if (Build.VERSION.SDK_INT >= 30) {
                val safe = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                val keyboard = insets.getInsets(WindowInsets.Type.ime()).bottom
                root.setPadding(safe.left, safe.top, safe.right, (keyboard - safe.bottom).coerceAtLeast(0))
                nav.setPadding(0, 0, 0, safe.bottom)
            } else {
                val cutout = if (Build.VERSION.SDK_INT >= 28) insets.displayCutout else null
                val bar = insets.stableInsetBottom
                val bottom = maxOf(bar, cutout?.safeInsetBottom ?: 0)
                root.setPadding(
                    maxOf(insets.systemWindowInsetLeft, cutout?.safeInsetLeft ?: 0),
                    maxOf(insets.systemWindowInsetTop, cutout?.safeInsetTop ?: 0),
                    maxOf(insets.systemWindowInsetRight, cutout?.safeInsetRight ?: 0),
                    (insets.systemWindowInsetBottom - bottom).coerceAtLeast(0),
                )
                nav.setPadding(0, 0, 0, bottom)
            }
            if (Build.VERSION.SDK_INT >= 30) WindowInsets.CONSUMED else insets.consumeSystemWindowInsets()
        }
    }

    private fun createWebView() {
        web = WebView(this).apply {
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = false
                allowContentAccess = true
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                safeBrowsingEnabled = true
                userAgentString = "$userAgentString $APP_AGENT"
            }
            webViewClient = SiteClient()
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, newProgress: Int) {
                    if (!signingIn) progress.progress = newProgress
                }

                override fun onShowFileChooser(view: WebView, callback: ValueCallback<Array<Uri>>, params: FileChooserParams): Boolean {
                    if (view.url?.let { Uri.parse(it).isSite() } != true) return false
                    uploadCallback?.onReceiveValue(null)
                    uploadCallback = callback
                    return try {
                        @Suppress("DEPRECATION")
                        startActivityForResult(params.createIntent(), FILE_CHOOSER)
                        true
                    } catch (_: ActivityNotFoundException) {
                        uploadCallback = null
                        false
                    }
                }
            }
            setDownloadListener { url, userAgent, disposition, mimeType, _ ->
                downloads.start(Uri.parse(url), userAgent, disposition, mimeType)
            }
        }
        page.addView(web, 0, FrameLayout.LayoutParams(-1, -1))
    }

    private inner class SiteClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val uri = request.url
            if (!request.isForMainFrame) return uri.host == "accounts.google.com"
            if (uri.host == "accounts.google.com" || (uri.isSite() && uri.path == "/wp-login.php" &&
                    uri.getQueryParameter("action") == "googlesitekit_auth")) {
                if (request.hasGesture()) browserSignIn.start()
                return true
            }
            if (uri.isSite()) {
                if (uri.path?.endsWith(".pdf", ignoreCase = true) == true) {
                    downloads.start(uri, web.settings.userAgentString, null, "application/pdf")
                    return true
                }
                return false
            }
            if (uri.scheme == "https" && uri.host == "www.cracktarget.com" && uri.port == -1 && uri.userInfo == null) {
                load(uri.buildUpon().authority(SITE_HOST).build().toString())
            } else if ((uri.scheme == "https" && !uri.host.isNullOrEmpty() && uri.userInfo == null) ||
                (request.hasGesture() && (uri.scheme == "mailto" || uri.scheme == "tel" || uri.scheme == "upi"))) {
                openExternal(uri)
            }
            return true
        }

        override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
            systemBack?.update(view.canGoBack())
        }

        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
            if (url == null) return
            if (!Uri.parse(url).isSite()) {
                view.stopLoading()
                showError(R.string.page_unavailable)
                return
            }
            latestUrl = url
            updateNavigation(url)
            googleButton.visibility = View.GONE
            mainFrameError = false
            loading = true
            progress.progress = 0
            errorPanel.visibility = View.GONE
            handler.removeCallbacks(slowPage)
            handler.postDelayed(slowPage, 25_000)
            showProgress()
        }

        override fun onPageCommitVisible(view: WebView, url: String) {
            if (!mainFrameError && url == latestUrl) {
                errorPanel.visibility = View.GONE
                handler.removeCallbacks(slowPage)
            }
        }

        override fun onPageFinished(view: WebView, url: String?) {
            if (url != latestUrl) return
            loading = false
            handler.removeCallbacks(slowPage)
            showProgress()
            val uri = Uri.parse(url)
            if (!mainFrameError && uri.isSite() && uri.path?.trimEnd('/') == "/my-account") {
                view.evaluateJavascript(
                    "Boolean(document.querySelector('form.woocommerce-form-login') && !document.body.classList.contains('logged-in'))",
                ) { guest ->
                    if (view === web && url == latestUrl && !mainFrameError) {
                        googleButton.visibility = if (guest == "true") View.VISIBLE else View.GONE
                    }
                }
            }
        }

        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
            if (request.isForMainFrame) showError(R.string.page_unavailable)
        }

        override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
            if (request.isForMainFrame && response.statusCode >= 500) showError(R.string.page_unavailable)
        }

        override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
            page.removeView(view)
            view.destroy()
            createWebView()
            systemBack?.update(false)
            showError(R.string.page_unavailable)
            return true
        }
    }

    private fun updateNavigation(url: String) {
        var selected = destinations.indexOfFirst { (_, destination) ->
            destination != SITE && url.startsWith(destination)
        }
        if (selected < 0 && Uri.parse(url).path == "/") selected = 0
        navButtons.forEachIndexed { index, button -> button.isSelected = index == selected }
    }

    private fun showError(message: Int, failed: Boolean = true) {
        mainFrameError = failed
        loading = false
        handler.removeCallbacks(slowPage)
        errorMessage.setText(message)
        errorPanel.visibility = View.VISIBLE
        showProgress()
    }

    private fun showProgress() {
        progress.isIndeterminate = signingIn
        progress.visibility = if (loading || signingIn) View.VISIBLE else View.INVISIBLE
    }

    private fun load(url: String, force: Boolean = false) {
        if (force || web.url != url) {
            if (loading) web.stopLoading()
            if (force && web.url == url && !mainFrameError) web.reload() else web.loadUrl(url)
        }
    }

    private fun openExternal(uri: Uri) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE))
        } catch (_: ActivityNotFoundException) {
            notify(R.string.external_unavailable)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        browserSignIn.accept(intent)
    }

    @Deprecated("Framework file chooser result callback")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_CHOOSER) {
            uploadCallback?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data)
                ?.filter { it.scheme == "content" && it.authority != "$packageName.downloads" }
                ?.toTypedArray()?.takeIf { it.isNotEmpty() })
            uploadCallback = null
        }
    }

    @Deprecated("Activity uses WebView history before exiting")
    override fun onBackPressed() {
        if (web.canGoBack()) web.goBack() else super.onBackPressed()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val webState = Bundle()
        web.saveState(webState)
        outState.putBundle("web", webState)
        super.onSaveInstanceState(outState)
    }

    override fun onStop() {
        CookieManager.getInstance().flush()
        super.onStop()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        browserSignIn.destroy()
        systemBack?.update(false)
        uploadCallback?.onReceiveValue(null)
        web.destroy()
        super.onDestroy()
    }

    private fun notify(message: Int) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    private fun dp(value: Int) = (value * resources.displayMetrics.density + .5f).toInt()

    @TargetApi(33)
    private class WebHistoryBack(activity: Activity, goBack: () -> Unit) {
        private val dispatcher = activity.onBackInvokedDispatcher
        private val callback = android.window.OnBackInvokedCallback { goBack() }
        private var registered = false

        fun update(hasHistory: Boolean) {
            if (hasHistory == registered) return
            if (hasHistory) {
                dispatcher.registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, callback)
            } else {
                dispatcher.unregisterOnBackInvokedCallback(callback)
            }
            registered = hasHistory
        }
    }

    private companion object {
        const val FILE_CHOOSER = 1001
    }
}
