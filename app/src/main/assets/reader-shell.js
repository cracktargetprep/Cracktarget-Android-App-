(() => {
    if (window.top !== window) return;

    const apply = () => {
        if (!document.documentElement || document.getElementById("ct-native-reader")) return;
        const style = document.createElement("style");
        style.id = "ct-native-reader";
        style.textContent = `
            #cth-root,
            .wp-site-blocks > header.wp-block-template-part,
            .wp-site-blocks > footer.wp-block-template-part {
                display: none !important;
            }
            body, .wp-site-blocks {
                padding-top: 0 !important;
                margin-top: 0 !important;
            }
            .wp-site-blocks > main { margin-block-start: 0 !important; }
        `;
        (document.head || document.documentElement).append(style);
    };

    if (document.documentElement) apply();
    else {
        const observer = new MutationObserver(() => {
            if (document.documentElement) {
                apply();
                observer.disconnect();
            }
        });
        observer.observe(document, {childList: true});
    }
})();
