# CrackTarget Android — Version 1.1 testing source

**Status:** Android Studio source only. NOT a compiled or tested APK. Existing WordPress/WooCommerce site is the backend. Package `com.cracktarget.app`.

## Build a test APK
1. Install Android Studio with JDK 17 and Android SDK Platform 35.
2. Open the `CrackTargetAndroid` folder in Android Studio, and wait for Gradle sync. Internet access is needed to download Gradle dependencies.
3. Select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
4. Install the generated `app-debug.apk` on a test phone. Do **not** upload the debug APK to Play Store.
5. Run the QA checklist below. Website URL paths are provisional and must be verified on the live site.

## Real-device QA checklist
- Home, Study, Tests, Affairs and Account navigation all resolve to correct pages.
- WooCommerce account login, logout, password reset, and session persistence work.
- New users and existing purchasers see the correct entitlements; never unlock content based on client-side checks.
- Each test allows attempts as purchased; countdown, submit, scoring, explanations and persisted result history work after app restart.
- File upload, ebook download and PDF viewing work; private files stay access-controlled.
- External payment authentication and return flow work on the device.
- Offline/no network, back button, external links, rotation and accessibility are tested.

## Play Store release checklist
- Replace simple WebView-first UX with meaningful app-specific functionality; review Play Store minimum functionality rules.
- Verify Google Play payments policy for digital goods. Do not assume website checkout for in-app digital purchases is allowed; choose a compliant Play Billing design where required.
- Add brand-owned launcher icon, splash, screenshots, privacy policy, data safety, account deletion pathway and support contact.
- Confirm package name, Play Console ownership, app signing and release keystore (never commit keystore/passwords).
- Produce signed Android App Bundle (AAB), conduct internal/closed testing and submit for review.
- App-specific native mock-test APIs and push notifications are **not yet implemented**.

## Security notes
- No WordPress/WooCommerce API keys or user passwords are embedded in this app.
- HTTPS-only; unknown non-web schemes are blocked; links outside CrackTarget open externally.
- Login via existing website cookies, **not** a verified native single-sign-on implementation.
- Downloads are saved to the app-specific Downloads folder, not public shared Downloads.
