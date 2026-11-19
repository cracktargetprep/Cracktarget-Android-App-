package com.cracktarget.app

import java.net.URI
import java.net.URLDecoder
import java.util.Locale

internal object PlayWebPolicy {
    const val siteOrigin = "https://cracktarget.com"
    const val privacyUrl = "$siteOrigin/privacy-policy/"
    const val deletionUrl = "$siteOrigin/account-deletion/"

    val shopSections = listOf(
        "shop", "shop-2", "store", "cart", "cart-2", "basket", "checkout", "checkout-2",
        "product", "products", "product-category", "product-tag", "shop-now",
    )
    val purchaseEndpoints = listOf("order-pay", "order-received", "add-payment-method", "payment-methods")
    val checkoutQueries = listOf("add-to-cart", "wc-ajax", "pay_for_order", "wc-api", "remove_item", "update_cart")
    private val siteHosts = setOf("cracktarget.com", "www.cracktarget.com")
    private val imageHosts = setOf("i0.wp.com", "i1.wp.com", "i2.wp.com", "0.gravatar.com", "1.gravatar.com", "2.gravatar.com")

    fun pageAllowed(url: String): Boolean {
        val route = route(url) ?: return false
        return route.host in siteHosts && !isCommerce(route)
    }

    fun resourceAllowed(url: String, mainFrame: Boolean): Boolean {
        val route = route(url) ?: return false
        if (route.host in siteHosts) return !isCommerce(route)
        if (mainFrame) return false
        return when (route.host) {
            "c0.wp.com" -> route.path.startsWith("/c/") ||
                (route.path.startsWith("/p/woocommerce/") && !isCommerce(route))
            "fonts-api.wp.com", "fonts.wp.com", "fonts.googleapis.com", "fonts.gstatic.com" -> true
            in imageHosts -> true
            else -> false
        }
    }

    private fun isCommerce(route: Route): Boolean {
        val segments = route.path.split('/').filter(String::isNotBlank)
        return segments.firstOrNull() in shopSections ||
            segments.any { it in purchaseEndpoints } ||
            route.queryKeys.any { it in checkoutQueries } ||
            route.path.contains("/wc/store/") ||
            route.path.contains("/wc-api/") ||
            route.path.contains("add-to-cart") ||
            route.query.contains("rest_route=/wc/store") ||
            route.query.contains("rest_route=/wc/v")
    }

    private data class Route(val host: String, val path: String, val query: String) {
        val queryKeys get() = query.split('&', ';').map { it.substringBefore('=') }
    }

    private fun route(url: String): Route? {
        val uri = runCatching { URI(url) }.getOrNull() ?: return null
        val host = uri.host?.lowercase(Locale.ROOT) ?: return null
        if (uri.scheme != "https" || uri.userInfo != null || uri.port !in listOf(-1, 443)) return null
        val path = decode(uri.path.orEmpty()).replace('\\', '/').replace(Regex("/+"), "/")
        val cleanPath = runCatching { URI(null, null, path, null).normalize().path }
            .getOrNull() ?: return null
        return Route(host, cleanPath, decode(uri.query.orEmpty()))
    }

    private fun decode(value: String): String =
        runCatching { URLDecoder.decode(value.replace("+", "%2B"), "UTF-8") }
            .getOrDefault(value).lowercase(Locale.ROOT)
}
