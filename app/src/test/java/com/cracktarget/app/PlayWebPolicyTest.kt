package com.cracktarget.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayWebPolicyTest {
    @Test fun learningAndAccountPagesRemainAvailable() {
        for (path in listOf("/", "/study-directory/", "/mocktest/", "/my-account/", "/my-account/orders/", "/my-account/downloads/", "/my-account/view-order/12/", "/account-deletion/", "/privacy-policy/", "/?ctapp_v2=exchange")) {
            assertTrue(path, PlayWebPolicy.pageAllowed("https://cracktarget.com$path"))
        }
    }

    @Test fun purchaseAndCheckoutRoutesAreBlocked() {
        val routes = listOf(
            "/shop/", "/cart-2/", "/checkout-2/", "/product/sample/", "/product-category/test-series/",
            "/my-account/order-pay/12/", "/my-account/payment-methods/", "/?add-to-cart=42",
            "/?%61dd-to-cart=42", "/?wc-ajax=checkout", "/wp-json/wc/store/v1/checkout",
            "/?rest_route=%2Fwc%2Fstore%2Fv1%2Fcheckout", "/wp-content/js/add-to-cart.min.js",
            "/%252fcheckOUT-2/", "/mocktest/../checkout-2/",
        )
        for (path in routes) {
            val url = "https://cracktarget.com$path"
            assertFalse(path, PlayWebPolicy.pageAllowed(url))
            assertFalse(path, PlayWebPolicy.resourceAllowed(url, false))
        }
    }

    @Test fun outsideNavigationAndTrackingAreBlockedButStaticAssetsLoad() {
        assertFalse(PlayWebPolicy.pageAllowed("https://cracktarget.com@example.com/"))
        assertFalse(PlayWebPolicy.pageAllowed("https://checkout.razorpay.com/"))
        assertFalse(PlayWebPolicy.pageAllowed("https://cracktarget.com:8443/"))
        assertFalse(PlayWebPolicy.pageAllowed("http://cracktarget.com/"))
        assertFalse(PlayWebPolicy.resourceAllowed("https://www.clarity.ms/tag/test", false))
        assertFalse(PlayWebPolicy.resourceAllowed("https://www.googletagmanager.com/gtag/js", false))
        assertFalse(PlayWebPolicy.resourceAllowed("https://c0.wp.com/p/woocommerce/1/assets/js/frontend/add-to-cart.min.js", false))
        assertTrue(PlayWebPolicy.resourceAllowed("https://c0.wp.com/c/7.1.5/wp-includes/js/jquery/jquery.min.js", false))
        assertTrue(PlayWebPolicy.resourceAllowed("https://i0.wp.com/cracktarget.com/wp-content/uploads/logo.png", false))
        assertFalse(PlayWebPolicy.resourceAllowed("https://i0.wp.com/cracktarget.com/wp-content/uploads/logo.png", true))
    }
}
