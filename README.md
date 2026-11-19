# CrackTarget for Android

Android app for [CrackTarget](https://cracktarget.com/), package `com.cracktarget.app`. WordPress, WooCommerce, and the existing test platform handle customer accounts, purchases, test access, and results.

## Build and test

Use JDK 17, Gradle 8.13, and Android SDK Platform 36 and Build Tools 36.0.0.

```sh
gradle --no-daemon assembleDebug lintDebug testDebugUnitTest
php -l wordpress/ct-app-login-bridge/ct-app-login-bridge.php
python3 wordpress/tests/test_bridge.py
```

The sideload APK is `app/build/outputs/apk/debug/app-debug.apk`. It is debug-signed. Android only installs an update over an existing APK if the signing certificates match. Use a separate test device or Android profile to keep existing app-local data intact when certificates differ.

## Browser sign-in

The [WordPress bridge](wordpress/ct-app-login-bridge/ct-app-login-bridge.php) works with the existing Site Kit sign-in on WooCommerce My Account. The Android app starts a transaction with a random state and a challenge. Chrome completes the existing login and the user confirms the customer account. The browser offers a package-directed Android intent carrying a short-lived code. Only the initiating Android instance can exchange the code because it keeps the verifier. WordPress creates and verifies a fresh session in Android's WebView cookie store. The WooCommerce account page then renders inside the app.

Package only `wordpress/ct-app-login-bridge/` when uploading the WordPress plugin. The test router stays in the repository. On an in-place update, visit a WordPress admin page to run the idempotent schema setup if the activation hook did not run. The previous app's legacy Google sign-in requires an update to use this bridge. Deactivate the new bridge to disable the new endpoints during incident response. Restore the prior package only if legacy handoff is explicitly required.

## Branding and release

The source image is the [CrackTarget website icon](https://cracktarget.com/wp-content/uploads/2026/09/cropped-cracktarget.png), pinned in `branding/official-site-icon.png`. The compiled resources provide adaptive, round, monochrome, and density-specific variants plus a splash screen.

Before Play submission, verify the paid student journey on a physical device with a designated customer account. Confirm signing ownership, the Play payments approach for digital tests and ebooks, privacy and Data Safety disclosures, account deletion, and Play review access. Keep signing keys and production credentials out of Git.
