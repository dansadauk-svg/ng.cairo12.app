# Cairo12 notifications setup

Current 1.0.16 test build: the supplied Android configuration for ng.cairo12.app.debug is included. Client setup is enabled; a real delivery test still needs the matching server service-account credentials and notification permission. Release package ng.cairo12.app is not in the supplied file.

Promotional popups work without Firebase. Android background push uses Firebase Cloud Messaging HTTP v1. Website background push is not included in this release; website promotional popups are included.

1. Create a Firebase project and enable Firebase Cloud Messaging API (HTTP v1).
2. Register Android app `ng.cairo12.app.debug` for test APKs, and `ng.cairo12.app` for release. Download `google-services.json` to `app/google-services.json`. It must contain a client matching the variant you build; unmatched variants have push disabled. This is public client configuration, not the server credential.
3. Rebuild the APK with that file. Without it, the app works with push disabled. BuildConfig reads application ID, project ID, API key and sender ID; no Google services Gradle plugin is required. CI may supply the file before building. Restrict the client API key appropriately to your Firebase APIs and Android signing/package identity.
4. In Google Cloud/Firebase, create a service account with permission to send Firebase Cloud Messaging messages for that same project. Generate its JSON key. Paste it only into the private Studio → Notifications & ads service-account field. Never include it in the APK, source repository, chat or Media Library. It is stored encrypted using the WordPress auth salt and never rendered back. Keep WordPress salts stable or re-enter credentials after rotating them.
5. Enable new-product and/or order alerts. Configure Cloudways cron to invoke WordPress scheduled tasks every minute; low-traffic sites otherwise delay delivery. PHP OpenSSL and outbound HTTPS to oauth2.googleapis.com and fcm.googleapis.com are required.
6. On your test phone (with Google Play services), sign in, open Account → Notifications, enable it and allow the Android notification permission. Order alerts are generic on the lock screen and only go to the order owner. Logout or PIN reset invalidates delivery for that session; the phone also rejects stale-account notifications.
7. In Studio enter that customer account's email in the test notification form. Check that a notification arrives while the app is backgrounded. Tap it to open My orders. Test opt-out, logout, account switching, first product publication, and a real order status change. Device restrictions and battery settings can delay delivery; Firebase cannot guarantee permanent connectivity.

## Promotions
Open `https://cairo12.ng/?c12_admin=notifications` as the owner. Create title/message, optional product ID and HTTPS image, app/web target, start/end in UTC and once/daily frequency. Product IDs appear in Product list. Paste an image URL from WordPress Media Library. Publish, pause or enable campaigns. The latest eligible campaign is prioritized. The app checks after five seconds and then each minute; the website after seven seconds and then each minute. It waits while the customer is in a form, checkout, order screen, or another dialog. Frequency is stored per phone/browser; clearing local data resets it.

## Sessions
Native tokens have no automatic expiry. They remain revocable on logout, PIN/password reset, or administrative session removal. Website cookies renew with a ten-year duration for customer accounts. Device/browser data deletion and revoked sessions can require sign-in again.
