package com.cracktarget.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom

internal const val SITE = "https://cracktarget.com/"
internal const val ACCOUNT = "${SITE}my-account/"
internal const val APP_AGENT = "CrackTargetAndroid/2"
internal const val SITE_HOST = "cracktarget.com"

internal fun Uri.isSite(): Boolean = scheme == "https" && (encodedAuthority == SITE_HOST || encodedAuthority == "$SITE_HOST:443")

internal class BrowserSignIn(
    private val activity: Activity,
    private val notify: (Int) -> Unit,
    private val showProgress: (Boolean) -> Unit,
    private val success: () -> Unit,
) {
    private val prefs = activity.getSharedPreferences("browser_login", Activity.MODE_PRIVATE)
    private val handler = Handler(Looper.getMainLooper())
    private var exchangeView: WebView? = null
    private val timeout = Runnable { finish(false) }

    fun start() {
        if (exchangeView != null) return
        val state = token()
        val verifier = token()
        prefs.edit().putString("state", state).putString("verifier", verifier)
            .putLong("started", System.currentTimeMillis()).apply()
        val challenge = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray()).hex()
        val startUrl = Uri.parse(SITE).buildUpon()
            .appendQueryParameter("ctapp_v2", "start")
            .appendQueryParameter("state", state)
            .appendQueryParameter("challenge", challenge).build()
        try {
            activity.startActivity(Intent(Intent.ACTION_VIEW, startUrl).addCategory(Intent.CATEGORY_BROWSABLE))
        } catch (_: android.content.ActivityNotFoundException) {
            clearAttempt()
            notify(R.string.browser_unavailable)
        }
    }

    fun accept(intent: Intent?) {
        if (intent?.action != Intent.ACTION_VIEW) return
        val uri = intent.data ?: return
        intent.data = null
        if (uri.scheme != "ctapp" || uri.encodedAuthority != "auth" || uri.encodedPath != "/complete" ||
            uri.fragment != null || uri.queryParameterNames != setOf("state", "code") ||
            uri.getQueryParameters("state").size != 1 || uri.getQueryParameters("code").size != 1
        ) return
        if (exchangeView != null) return
        val state = uri.getQueryParameter("state") ?: return
        val code = uri.getQueryParameter("code") ?: return
        val verifier = prefs.getString("verifier", null)
        val age = System.currentTimeMillis() - prefs.getLong("started", 0)
        if (!tokenPattern.matches(state) || !tokenPattern.matches(code) || verifier == null ||
            !tokenPattern.matches(verifier) || state != prefs.getString("state", null) ||
            age !in 0L..MAX_ATTEMPT_MS
        ) {
            notify(R.string.sign_in_retry)
            return
        }
        val endpoint = Uri.parse(SITE).buildUpon().appendQueryParameter("ctapp_v2", "exchange")
            .appendQueryParameter("state", state).build().toString()
        val body = "code=$code&verifier=$verifier".toByteArray(StandardCharsets.US_ASCII)
        val cookieManager = CookieManager.getInstance()
        val browser = WebView(activity)
        exchangeView = browser
        cookieManager.setAcceptThirdPartyCookies(browser, false)
        browser.settings.apply {
            cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
            allowFileAccess = false
            allowContentAccess = false
        }
        browser.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                if (!request.isForMainFrame) return true
                val target = request.url
                val verify = target.isSite() && target.path == "/" &&
                    target.getQueryParameter("ctapp_v2") == "verify" && target.getQueryParameter("state") == state
                val receipt = target.isSite() && target.path == "/my-account/" &&
                    target.getQueryParameter("ctapp_signed_in") == state
                if (receipt) handler.post { if (exchangeView === view) finish(true) }
                else if (!verify) handler.post { if (exchangeView === view) finish(false) }
                return !verify
            }

            override fun onPageFinished(view: WebView, url: String?) {
                val target = url?.let(Uri::parse)
                if (target?.isSite() == true && target.path == "/my-account/" &&
                    target.getQueryParameter("ctapp_signed_in") == state
                ) handler.post { if (exchangeView === view) finish(true) }
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) handler.post { if (exchangeView === view) finish(false) }
            }

            override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
                if (request.isForMainFrame) handler.post { if (exchangeView === view) finish(false) }
            }
        }
        showProgress(true)
        handler.postDelayed(timeout, 25_000)
        browser.postUrl(endpoint, body)
    }

    fun destroy() {
        handler.removeCallbacksAndMessages(null)
        exchangeView?.destroy()
        exchangeView = null
    }

    private fun finish(signedIn: Boolean) {
        destroy()
        clearAttempt()
        showProgress(false)
        if (signedIn) {
            CookieManager.getInstance().flush()
            success()
        } else {
            notify(R.string.sign_in_retry)
        }
    }

    private fun clearAttempt() = prefs.edit().clear().apply()

    private companion object {
        val random = SecureRandom()
        val tokenPattern = Regex("[a-f0-9]{64}")
        const val MAX_ATTEMPT_MS = 720_000L
        fun token() = ByteArray(32).also(random::nextBytes).hex()
        fun ByteArray.hex() = joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }
}
