(() => {
    if (window.top !== window) return;

    const rules = /*CRACKTARGET_PLAY_RULES*/{};
    const blockedClasses = [
        ".add_to_cart_button", ".single_add_to_cart_button", ".woocommerce-mini-cart",
        ".wc-block-mini-cart", ".wc-block-cart", ".wc-block-checkout", ".woocommerce-cart-form",
        ".checkout-button", ".checkout.wc-forward", ".ct-store-cta", ".adsbygoogle",
        "iframe[src*='doubleclick.net']", "iframe[src*='googlesyndication.com']",
    ].join(",");
    const linkSelector = "a[href], form[action], button[formaction]";

    function normalized(value) {
        try { return decodeURIComponent(value).toLowerCase(); }
        catch { return value.toLowerCase(); }
    }

    function commerce(raw) {
        if (!raw) return false;
        let url;
        try { url = new URL(raw, location.href); }
        catch { return true; }
        if (url.protocol === "upi:" || url.protocol === "intent:") return true;
        const host = url.hostname.toLowerCase();
        if (/razorpay|paypal|paytm|cashfree|phonepe|payu/i.test(host)) return true;
        const path = normalized(normalized(url.pathname)).replace(/\\/g, "/");
        const sections = path.split("/").filter(Boolean);
        const query = normalized(url.search);
        return rules.shopSections.includes(sections[0]) ||
            sections.some(section => rules.purchaseEndpoints.includes(section)) ||
            [...url.searchParams.keys()].some(key => rules.checkoutQueries.includes(normalized(key))) ||
            path.includes("add-to-cart") || path.includes("/wc/store/") || path.includes("/wc-api/") ||
            query.includes("rest_route=/wc/store") || query.includes("rest_route=/wc/v");
    }

    function scrub(node) {
        if (node.nodeType !== Node.ELEMENT_NODE) return;
        if (node.matches(blockedClasses)) {
            node.remove();
            return;
        }
        if (node.matches(linkSelector) && commerce(node.getAttribute("href") || node.getAttribute("action") || node.getAttribute("formaction"))) {
            node.remove();
            return;
        }
        node.querySelectorAll(blockedClasses + "," + linkSelector).forEach(child => {
            if (child.matches(blockedClasses) || commerce(child.getAttribute("href") || child.getAttribute("action") || child.getAttribute("formaction"))) child.remove();
        });
    }

    const stopCheckout = (event) => {
        const link = event.target.closest?.(linkSelector);
        if (link && commerce(link.getAttribute("href") || link.getAttribute("action") || link.getAttribute("formaction"))) {
            event.preventDefault();
            event.stopImmediatePropagation();
        }
    };
    document.addEventListener("click", stopCheckout, true);
    document.addEventListener("submit", stopCheckout, true);
    new MutationObserver(records => {
        for (const record of records) {
            if (record.type === "attributes") scrub(record.target);
            else for (const node of record.addedNodes) scrub(node);
        }
    }).observe(document, {subtree: true, childList: true, attributes: true, attributeFilter: ["href", "action", "formaction", "class"]});
    if (document.documentElement) scrub(document.documentElement);
})();
