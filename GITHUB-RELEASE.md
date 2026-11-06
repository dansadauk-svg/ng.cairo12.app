# Cairo12 signed release

Upload the CONTENTS of cairo12-android to the repository root (app, .github, build.gradle, settings.gradle).
Firebase client configuration for ng.cairo12.app is included.

In GitHub Settings > Secrets and variables > Actions, create repository secrets named:
- CAIRO12_KEYSTORE_BASE64
- CAIRO12_STORE_PASSWORD
- CAIRO12_KEY_ALIAS
- CAIRO12_KEY_PASSWORD

Copy each value from the matching .txt file in the separate PRIVATE signing bundle. Never upload that bundle, the release JKS, or password text files into your repository.

Open Actions > Cairo12 Android > Run workflow, leave signed_release checked. Download Cairo12-signed-release after a successful run: it contains the APK and AAB. Use the AAB for Google Play.

Package ng.cairo12.app; version 1.0.17, code 18; endpoint https://cairo12.ng/. The old debug app uses a different package and may coexist; account login is needed in the release app.
Back up the private bundle securely. Reuse this key for future direct APK updates.
Release build not executed locally for this package; GitHub workflow will compile it. Live FCM delivery remains dependent on server credentials and customer opt-in.
