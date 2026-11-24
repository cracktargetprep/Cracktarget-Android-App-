# CrackTarget — Google Play release handoff

Prepared 24 November 2026. This is a release checklist and proposed listing, not a certification or confirmation that the app has been published.

## Store listing (English, India)

App name: CrackTarget: Exam Prep
Short description: Mock tests, study notes, current affairs and exam preparation in one app.
Category: Education
Price: Free download. Some content requires an existing entitlement.
Website: https://cracktarget.com/
Support: info@cracktarget.com
Privacy policy: https://cracktarget.com/privacy-policy/
Account deletion: https://cracktarget.com/account-deletion/

### Full description

Prepare for competitive examinations with CrackTarget, an independent learning platform bringing together exam-focused study resources, practice tests and current affairs.

EXPLORE EXAM PREPARATION RESOURCES
Find materials for UPSC recruitment examinations, EPFO APFC and EO/AO, DRDO Senior Administrative Officer, banking and regulatory recruitment examinations, civil services and other competitive examinations.

PRACTISE WITH MOCK TESTS
Attempt available practice papers, review answers and explanations, and revisit saved performance wherever supported by the test series.

STAY INFORMED
Read current affairs, explore subject-wise study materials and find examination and recruitment updates.

YOUR LEARNING ACCOUNT
Sign in to your CrackTarget account to access available resources and content already associated with your account. Certain resources require an existing paid entitlement. The Play edition does not offer purchases in the app.

CrackTarget is an independent educational platform and is not affiliated with or endorsed by UPSC, EPFO, DRDO or any government authority. For official examination notifications, refer to the relevant recruiting authority.

Support: info@cracktarget.com

## Source and release controls

- Package com.cracktarget.app; current source version 1.5.0 / code 5; compile and target SDK 36; min SDK 26.
- Release configuration enables PLAY_DISTRIBUTION. Existing PlayWebPolicy route rules and unit tests cover purchase endpoints and external navigation. Their presence does not establish that all live WordPress pages comply.
- Do not distribute a debug-signed APK as the Play production release.
- Build with JDK 17 and Gradle 8.13, SDK 36 and build tools 36.0.0. Run lintRelease, testReleaseUnitTest and bundleRelease; also run the existing bridge and playQa tests where available.
- Generate or select the organization-controlled upload key locally or in a secured CI environment; sign the release bundle and enroll in Play App Signing. Never commit or share private key or passwords. Verify bundle signature and preserve the upload key securely.
- Upload to internal testing first. Test signed production-equivalent app on real devices, including Android 16; retain evidence of checks. No claim of successful build or signing until verified.

## Release acceptance gates (open until tested)

- Google and ordinary login, logout, session persistence, account registration, recovery, browser handoff.
- Real entitled and unentitled test accounts: paid mocks, results, explanations, saved attempts and ebooks/downloads work; access controls remain enforced.
- No visible purchase calls to action, external payment directions, product pricing or checkout links inside the Play edition, including banners, search, account screens, embedded links, redirects, web popups and dynamically updated pages. Verify no checkout can be initiated; do not disable normal website sales.
- Account deletion discoverable within app and externally; test request handling and actual data deletion workflow using an expendable account. Do not delete real student accounts.
- Confirm actual data collection/sharing/retention with website services and SDKs, including WebView, authentication, cookies, analytics, hosting, support, order history, test data. Reconcile with privacy policy; complete Data Safety based on observed data flows, not manifest alone.
- Reviewer access: create a dedicated, least-privilege review account with representative resources accessible; provide credentials privately in Play Console, including instructions that avoid OTP/2FA barriers. Never use owner credentials or commit reviewer passwords.
- Confirm target audience, content rating, ads declaration, government-affiliation disclaimer and appropriate geographic distribution with owner.
- Confirm current Play policies and organization verification; do not submit until all blocking gates pass.

## Store asset requirements

- Official logo: branding/official-site-icon.png. Export 512 x 512 Play icon (32-bit PNG, max 1 MB), preserving official brand.
- Feature graphic: 1024 x 500 JPG or 24-bit PNG without alpha.
- Capture actual app screenshots from release-equivalent build. Minimum two, preferably 5–6 portrait images showing home, current affairs, study directory, mock attempt, results and account/library. Avoid fake screens, private student data, prices and purchase prompts.
- Suggested short overlay labels: Exam-focused preparation; Daily current affairs; Subject-wise study resources; Practise with mock tests; Review your performance.
- Screenshot claims must match actual app functionality.

## Official references
- https://support.google.com/googleplay/android-developer/answer/11926878
- https://support.google.com/googleplay/android-developer/answer/9859152
- https://support.google.com/googleplay/android-developer/answer/10281818
- https://support.google.com/googleplay/android-developer/answer/10144311
- https://support.google.com/googleplay/android-developer/answer/9866151

## External dependency

Organization developer account verification including D-U-N-S Number; registration fee and identity verification require authorized owner action. Do not register another account or publish without verified owner access.
