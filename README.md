# EURO MALPAE Android wrapper

This Android app opens the existing hosted EURO MALPAE recruitment portal:
https://maple-ncubez-recruitment.onrender.com

Features:
- WebView wrapper for the existing portal and backend
- JavaScript and DOM storage enabled for web application compatibility
- Android file picker support for document/video uploads
- External handling for WhatsApp, email, telephone, and external websites
- Download support and Android back navigation

## Build from an Android phone using GitHub Actions

1. Create a GitHub repository named `euro-malpae-android`.
2. Upload the contents of this project, keeping the directory structure.
3. Open the repository's **Actions** tab and enable Actions if prompted.
4. Select **Build EURO MALPAE Android app** and tap **Run workflow**.
5. Open the completed workflow run, then download the `euro-malpae-android-builds` artifact.
6. Extract the ZIP. The debug APK can be installed for testing after allowing installs from your browser/files app.

## Google Play release

The workflow's release APK and AAB are unsigned. They are included as build outputs, not publish-ready files.
Before Play Store submission, create and securely store a private upload keystore, configure Gradle signing, and build a signed Android App Bundle (AAB). Never commit the keystore or passwords to GitHub. Use Play App Signing in Play Console.

## Important

- This is a WebView wrapper. It requires internet access and uses the hosted site; it does not work offline.
- The app does not change the backend or repair missing server endpoints.
- Before public release, test candidate login/link validation, file uploads, admin login, contact buttons, privacy disclosures, and account deletion/data safety requirements.
- Confirm that you own or are authorized to publish the EURO MALPAE brand and website.
