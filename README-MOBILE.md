# CrackTarget Android — mobile-only APK build

This repository contains a preliminary website-based Android app. It has NOT been compiled or tested on a phone yet.

## Build from your Android phone with GitHub Actions
1. Sign in to github.com on your mobile browser, create a **private** repository (e.g. cracktarget-android).
2. Upload the *contents* of the CrackTargetAndroid folder to the repository root, including `.github/workflows/android-apk.yml`. GitHub's mobile browser file upload may be easier with desktop-site mode. Uploading the ZIP itself is NOT sufficient.
3. Open the repository's **Actions** tab, choose **Build CrackTarget test APK**, then **Run workflow**.
4. After the build succeeds, open the run and download the `CrackTarget-debug-apk` artifact ZIP. Extract the APK and install it on your Android phone, approving the one-time Android installation permission if asked.
5. Test WooCommerce login, purchase flows, mock-test results, file downloads and navigation before sharing it.

The debug APK is only for private testing. It is not Play Store signed. A release AAB needs private signing credentials and further review of Google Play billing, privacy policy, account deletion, content and app functionality. Do not publish until those checks pass.
