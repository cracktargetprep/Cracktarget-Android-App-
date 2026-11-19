package com.cracktarget.app

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
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
import android.widget.ScrollView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    private lateinit var page: FrameLayout
    private lateinit var web: WebView
    private lateinit var progress: ProgressBar
    private lateinit var errorPanel: LinearLayout
    private lateinit var errorMessage: TextView
    private lateinit var googleButton: Button
    private lateinit var toolbar: FrameLayout
    private lateinit var nativePrep: NativePrep
    private var nativeContent: ScrollView? = null
    private var screen = PrepScreen.HOME
    private var reading = false
    private var resetReaderHistory = false
    private var readerTitle = ""
    private val readerScript by lazy { assets.open("reader-shell.js").bufferedReader().use { it.readText() } }
    private var playFilterReady = false
    private lateinit var browserSignIn: BrowserSignIn
    private lateinit var downloads: Downloads
    private val handler = Handler(Looper.getMainLooper())
    private val slowPage = Runnable { if (loading) showError(R.string.page_slow, false) }
    private val tabs = listOf(PrepScreen.HOME, PrepScreen.STUDY, PrepScreen.TESTS, PrepScreen.UPDATES, PrepScreen.ACCOUNT)
    private val navButtons = mutableListOf<Button>()
    private var uploadCallback: ValueCallback<Array<Uri>>? = null
    private var latestUrl = SITE
    private var loading = false
    private var mainFrameError = false
    private var signingIn = false
    private val historyBack = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            when {
                reading && !resetReaderHistory && web.canGoBack() -> web.goBack()
                reading -> showNative(screen)
                screen != screen.tab -> showNative(screen.tab)
                else -> showNative(PrepScreen.HOME)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        configureWindow()
        onBackPressedDispatcher.addCallback(this, historyBack)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(NativePrep.surface)
        }
        toolbar = FrameLayout(this)
        root.addView(toolbar, LinearLayout.LayoutParams(-1, -2))
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
                setTextColor(NativePrep.ink)
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
            setBackgroundColor(Color.WHITE)
            minimumHeight = dp(60)
        }
        for (tab in tabs) {
            val button = Button(this, null, android.R.attr.borderlessButtonStyle).apply {
                text = tab.label
                isAllCaps = false
                textSize = 12f
                gravity = Gravity.CENTER
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                minWidth = 0
                minimumWidth = 0
                minHeight = dp(55)
                minimumHeight = dp(55)
                stateListAnimator = null
                setPadding(dp(2), dp(7), dp(2), dp(7))
                compoundDrawablePadding = dp(4)
                setOnClickListener { showNative(tab) }
            }
            navButtons.add(button)
            nav.addView(button, LinearLayout.LayoutParams(0, -2, 1f).apply {
                marginStart = dp(3)
                marginEnd = dp(3)
                topMargin = dp(3)
                bottomMargin = dp(3)
            })
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
        }) {
            showNative(PrepScreen.ACCOUNT)
            openReader(PrepLink("Account dashboard", "", ACCOUNT))
        }
        nativePrep = NativePrep(this, ::showNative, ::openReader, { browserSignIn.start() }) { url ->
            if (PrepCatalog.official(url)) openExternal(Uri.parse(url))
        }
        CookieManager.getInstance().setAcceptCookie(true)
        createWebView()

        val savedScreen = savedInstanceState?.getString("nativeScreen")?.let { value ->
            PrepScreen.entries.firstOrNull { it.name == value }
        } ?: PrepScreen.HOME
        showNative(savedScreen)
        val savedTitle = savedInstanceState?.getString("readerTitle")
        val savedUrl = savedInstanceState?.getString("readerUrl")
        if (savedTitle != null && savedUrl != null && PrepCatalog.firstParty(savedUrl) && playPageAllowed(savedUrl)) {
            enterReader(savedTitle)
            latestUrl = savedUrl
            val restored = if (playFilterReady) savedInstanceState?.getBundle("web")?.let(web::restoreState) else null
            if (restored?.currentItem == null) load(savedUrl, force = true)
        }
        browserSignIn.accept(intent)
    }

    private fun showNative(destination: PrepScreen) {
        if (reading) web.stopLoading()
        reading = false
        loading = false
        resetReaderHistory = false
        handler.removeCallbacks(slowPage)
        screen = destination
        web.visibility = View.GONE
        errorPanel.visibility = View.GONE
        googleButton.visibility = View.GONE
        toolbar.removeAllViews()
        toolbar.addView(nativePrep.toolbar(screen))
        nativeContent?.let(page::removeView)
        nativeContent = nativePrep.content(screen).also {
            page.addView(it, FrameLayout.LayoutParams(-1, -1))
        }
        updateTab()
        historyBack.isEnabled = screen != PrepScreen.HOME
        showProgress()
    }

    private fun enterReader(title: String) {
        reading = true
        readerTitle = title
        resetReaderHistory = true
        historyBack.isEnabled = true
        nativeContent?.visibility = View.GONE
        web.visibility = View.VISIBLE
        errorPanel.visibility = View.GONE
        googleButton.visibility = View.GONE
        toolbar.removeAllViews()
        toolbar.addView(LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(3), dp(16), dp(3))
            setBackgroundColor(NativePrep.surface)
            addView(Button(this@MainActivity, null, android.R.attr.borderlessButtonStyle).apply {
                contentDescription = "Back to ${screen.label}"
                minWidth = 0
                minimumWidth = 0
                minimumHeight = dp(48)
                setPadding(dp(12), 0, dp(12), 0)
                setCompoundDrawablesRelativeWithIntrinsicBounds(
                    PrepIconDrawable(PrepIcon.BACK, NativePrep.green, dp(23)), null, null, null,
                )
                setOnClickListener { showNative(screen) }
            }, LinearLayout.LayoutParams(dp(49), dp(50)))
            addView(TextView(this@MainActivity).apply {
                text = title
                textSize = 17f
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                maxLines = 2
                ellipsize = android.text.TextUtils.TruncateAt.END
                setTextColor(NativePrep.ink)
            }, LinearLayout.LayoutParams(0, -2, 1f))
        })
        updateTab()
    }

    private fun openReader(link: PrepLink) {
        if (!PrepCatalog.firstParty(link.url) || !playPageAllowed(link.url)) {
            notify(R.string.play_unavailable_link)
            return
        }
        enterReader(link.title)
        latestUrl = link.url
        load(link.url, force = true)
    }

    private fun updateTab() {
        navButtons.forEachIndexed { index, button ->
            val selected = tabs[index] == screen.tab
            val color = if (selected) NativePrep.green else NativePrep.muted
            button.isSelected = selected
            button.setTextColor(color)
            button.setCompoundDrawablesRelativeWithIntrinsicBounds(
                null, PrepIconDrawable(tabs[index].icon, color, dp(22)), null, null,
            )
            button.background = android.graphics.drawable.RippleDrawable(
                ColorStateList.valueOf(0x22136758),
                GradientDrawable().apply {
                    setColor(if (selected) NativePrep.pale else Color.WHITE)
                    cornerRadius = dp(13).toFloat()
                },
                null,
            )
        }
    }

    @Suppress("DEPRECATION")
    private fun configureWindow() {
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }

    private fun bindInsets(root: LinearLayout, nav: LinearLayout) {
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val safe = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            val keyboard = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            root.setPadding(safe.left, safe.top, safe.right, (keyboard - safe.bottom).coerceAtLeast(0))
            nav.setPadding(0, 0, 0, safe.bottom)
            WindowInsetsCompat.CONSUMED
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
            if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                WebViewCompat.addDocumentStartJavaScript(this, readerScript, setOf(PlayWebPolicy.siteOrigin))
            }
            playFilterReady = installPlayFilter(this)
            webViewClient = SiteClient()
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, newProgress: Int) {
                    if (!signingIn) this@MainActivity.progress.progress = newProgress
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
                if (playPageAllowed(url)) downloads.start(Uri.parse(url), userAgent, disposition, mimeType)
                else notify(R.string.play_unavailable_link)
            }
        }
        web.visibility = if (reading) View.VISIBLE else View.GONE
        page.addView(web, 0, FrameLayout.LayoutParams(-1, -1))
    }

    private fun installPlayFilter(view: WebView): Boolean {
        if (!BuildConfig.PLAY_DISTRIBUTION) return true
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) return false
        val rules = JSONObject().apply {
            put("shopSections", JSONArray(PlayWebPolicy.shopSections))
            put("purchaseEndpoints", JSONArray(PlayWebPolicy.purchaseEndpoints))
            put("checkoutQueries", JSONArray(PlayWebPolicy.checkoutQueries))
        }
        val js = assets.open("play-commerce-filter.js").bufferedReader().use { it.readText() }
            .replace("/*CRACKTARGET_PLAY_RULES*/{}", rules.toString())
        WebViewCompat.addDocumentStartJavaScript(view, js, setOf(PlayWebPolicy.siteOrigin))
        return true
    }

    private fun playPageAllowed(url: String) = !BuildConfig.PLAY_DISTRIBUTION || PlayWebPolicy.pageAllowed(url)

    private inner class SiteClient : WebViewClient() {
        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
            if (!BuildConfig.PLAY_DISTRIBUTION ||
                (playFilterReady && PlayWebPolicy.resourceAllowed(request.url.toString(), request.isForMainFrame))) {
                return null
            }
            return WebResourceResponse("text/plain", "UTF-8", 403, "Unavailable", emptyMap(),
                java.io.ByteArrayInputStream(byteArrayOf()))
        }

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val uri = request.url
            if (!request.isForMainFrame) return uri.host == "accounts.google.com"
            if (BuildConfig.PLAY_DISTRIBUTION && uri.scheme == "mailto" &&
                uri.schemeSpecificPart.substringBefore('?').equals("info@cracktarget.com", ignoreCase = true) &&
                request.hasGesture()) {
                openExternal(uri)
                return true
            }
            if (uri.host == "accounts.google.com" || (uri.isSite() && uri.path == "/wp-login.php" &&
                    uri.getQueryParameter("action") == "googlesitekit_auth")) {
                if (request.hasGesture()) browserSignIn.start()
                return true
            }
            if (!playPageAllowed(uri.toString())) {
                if (request.hasGesture()) notify(R.string.play_unavailable_link)
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

        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
            if (!reading || url == null) return
            if (!Uri.parse(url).isSite() || !playPageAllowed(url)) {
                view.stopLoading()
                showError(R.string.page_unavailable)
                return
            }
            latestUrl = url
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
            if (!reading || mainFrameError || url != latestUrl) return
            if (!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                view.evaluateJavascript(readerScript, null)
            }
            if (resetReaderHistory) {
                view.clearHistory()
                resetReaderHistory = false
            }
            errorPanel.visibility = View.GONE
            handler.removeCallbacks(slowPage)
        }

        override fun onPageFinished(view: WebView, url: String?) {
            if (!reading || url != latestUrl) return
            loading = false
            handler.removeCallbacks(slowPage)
            showProgress()
            val uri = Uri.parse(url)
            if (!mainFrameError && uri.isSite() && uri.path?.trimEnd('/') == "/my-account") {
                view.evaluateJavascript(
                    "Boolean(document.querySelector('form.woocommerce-form-login') && !document.body.classList.contains('logged-in'))",
                ) { guest ->
                    if (reading && view === web && url == latestUrl && !mainFrameError) {
                        googleButton.visibility = if (guest == "true") View.VISIBLE else View.GONE
                    }
                }
            }
        }

        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
            if (reading && request.isForMainFrame) showError(R.string.page_unavailable)
        }

        override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
            if (reading && request.isForMainFrame && response.statusCode >= 500) showError(R.string.page_unavailable)
        }

        override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
            page.removeView(view)
            view.destroy()
            createWebView()
            if (reading) showError(R.string.page_unavailable)
            return true
        }
    }

    private fun showError(message: Int, failed: Boolean = true) {
        if (!reading) return
        mainFrameError = failed
        loading = false
        handler.removeCallbacks(slowPage)
        errorMessage.setText(message)
        errorPanel.visibility = View.VISIBLE
        showProgress()
    }

    private fun showProgress() {
        progress.isIndeterminate = signingIn
        progress.visibility = if ((reading && loading) || signingIn) View.VISIBLE else View.INVISIBLE
    }

    private fun load(url: String, force: Boolean = false) {
        if (BuildConfig.PLAY_DISTRIBUTION && !playFilterReady) {
            showError(R.string.play_webview_update)
            return
        }
        if (!playPageAllowed(url)) {
            notify(R.string.play_unavailable_link)
            return
        }
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

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("nativeScreen", screen.name)
        if (reading) {
            val webState = Bundle()
            web.saveState(webState)
            outState.putBundle("web", webState)
            outState.putString("readerTitle", readerTitle)
            outState.putString("readerUrl", latestUrl)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onStop() {
        CookieManager.getInstance().flush()
        super.onStop()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        browserSignIn.destroy()
        nativePrep.destroy()
        historyBack.isEnabled = false
        uploadCallback?.onReceiveValue(null)
        web.destroy()
        super.onDestroy()
    }

    private fun notify(message: Int) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    private fun dp(value: Int) = (value * resources.displayMetrics.density + .5f).toInt()

    private companion object {
        const val FILE_CHOOSER = 1001
    }
}
