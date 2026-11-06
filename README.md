# Cairo12 Android 1.0.0

Native Java Android app for https://cairo12.ng/. The production package is `ng.cairo12.app`. The debug test package is `ng.cairo12.app.debug`, so it can be installed beside a future release. Android 7.0 and newer is the chosen minimum. Compile and target SDK are 36.

## Start

1. Install the supplied `cairo12-core-v0.2.0.zip` in WordPress, replacing Cairo12 Custom Commerce. This adds mobile sign-in to the existing website plugin. Existing products, settings, orders and browser accounts remain in their current storage.
2. In WordPress General Settings, confirm the site uses https://cairo12.ng/. Ensure WordPress Permalinks are enabled and its `/wp-json/cairo12/v1/catalogue` API returns JSON. Cloudways and any CDN must pass the `Authorization` header to PHP and bypass caching for `/wp-json/cairo12/`.
3. Install the supplied test APK on an Android phone, or upload this source folder's contents to the root of a GitHub repository. Include the `.github` folder and `gradle` wrapper files.
4. In GitHub Actions, run **Cairo12 Android**. Download **Cairo12-test-apk** after a successful build. The default workflow produces the test APK without release signing secrets.

The website returned HTTP 503 during the initial connectivity check in this environment. Real account, payment and media testing needs a responding site with the updated plugin installed.

## Features

- Cairo12 logo, cream/teal/gold colours, serif headings, native bottom navigation icons and shopping bag count.
- Portrait product feed and a horizontally scrolling latest-ten abaya row. The story row scrolls vertically with the feed. Around four and a half cards fit across a phone width.
- Horizontal product media swipes, native Media3 video playback, sound toggle, tap-to-pause and double-tap saving. Video playback pauses when leaving the app.
- Visual and named colour choices, Other colour requests, sizes, quantity, saved items and persistent bag.
- Prices supplied by WordPress. The current plugin uses 25% retail markup and 10% wholesale markup for six mixed pieces. Customer screens show final naira prices.
- Checkout with customer address, standard Kano cargo and fast-track Kano/Abuja cargo choices. Rates display in naira. Nigerian clearing is included, and onward local delivery is paid separately.
- Email and four-digit PIN sign-in/registration using the existing Cairo12 PIN accounts. PIN recovery opens the website email-recovery page in the browser.
- Customer orders, final item prices, bank details, separate cargo quotes, receipt image submission, receipt rejection details and order history.
- Unavailable-item refund/replacement requests use the existing website workflow.
- Status refresh every minute while the Orders screen is active. Cargo arrival/payment holds are displayed. Physical release remains controlled by the WordPress staff workflow.

Product and customer screens use Android views. PIN recovery stays inside the app. Visual spacing and dialogs adapt to Android and phone dimensions, so they may differ slightly from the web rendering.

## Sessions and receipts

PINs are used only during sign-in and registration. The app persists a revocable session token encrypted with Android Keystore. The server stores the token hash with no automatic expiry. Sign-out revokes that device session. Password/PIN reset invalidates existing mobile sessions.

The app sends receipt images through the same private encrypted receipt endpoint used by the website. It uses the Android document picker. Maximum receipt size is 3 MB, with JPG, PNG or WebP formats. Enter the actual transfer date/time when submitting. Finance still verifies the actual bank payment.

The app saves only bag and favourites locally. Those do not sync across devices in this build. For a product that has become unavailable, remove it from the bag and choose an available product.

## GitHub release signing

For production APK/AAB output, add these repository secrets from your own Cairo12 release/upload keystore:

- `CAIRO12_KEYSTORE_BASE64`
- `CAIRO12_STORE_PASSWORD`
- `CAIRO12_KEY_ALIAS`
- `CAIRO12_KEY_PASSWORD`

Run the workflow manually and enable **signed_release**. Download **Cairo12-signed-release**. Protect and back up the original keystore, alias and passwords. The source package contains no production keystore or live secrets. The supplied APK is signed with a test key and uses the `.debug` package.

## Android Studio

Open this folder as a project with JDK 17 or newer and Android SDK 36 installed. Gradle is pinned to 8.11.1 and the Android Gradle plugin to 8.10.1. Run:

```sh
./gradlew :app:assembleDebug
```

Change the backend only in `app/build.gradle` under `BuildConfig.SITE_URL` and the corresponding manifest host for app links. Release package identifiers should be finalized before publishing on Play Console.

## App links

The app handles `https://cairo12.ng/shop/?product=ID`. Android automatic verification requires your production signing certificate fingerprint in `https://cairo12.ng/.well-known/assetlinks.json`. Configure that after release signing is set up. Product share links also work in a regular browser.

## Current workflow scope

Firebase Cloud Messaging integration is included; activation requires your matching Firebase Android configuration and private server service-account setup. Without Firebase, order refresh and app/web promotional popups still work. The app reflects the existing plugin's staff-assisted replacement and refund workflow. Multi-package order splitting, automatic refunds, partial-payment reconciliation and automatic stock checks remain website workflow extensions.

The private staff uploader continues on the website. This customer app includes customer browsing, checkout and tracking.

## Verification

See `VALIDATION.md` for actual checks and remaining device/server checks. The auth test can be run with PHP after extracting the plugin:

```sh
php verification/mobile-auth-test.php /absolute/path/cairo12-core/includes/mobile.php
```

Official build compatibility reference: https://developer.android.com/build/releases/agp-8-10-0-release-notes


## Update 1.0.2
Install Cairo12 core 0.2.1 for native email-code PIN recovery. The website must deliver WordPress mail reliably. Reset codes expire after ten minutes and are single-use. The app no longer opens the WordPress lost-password page.


## Testing URL (1.0.5)
Both debug/test and release builds now use https://cairo12.ng/ . Install the companion Cairo12 core plugin on that site.


## Update 1.0.9: commerce and Paystack
Install Cairo12 Core 0.3.0 on the test WordPress site. In Cairo12 → Settings enable Paystack and add matching test public/secret keys. Set the displayed /wp-json/cairo12/v1/paystack/webhook URL in Paystack. Use only test keys while testing; no keys are bundled in this source. Paystack's InlineJS v2 runs in the payment-only WebView, while the rest of the app remains native. The backend verifies the reference, owner, amount, currency, customer email, environment and quote before marking payment paid. Signed webhooks handle completion after closing the app. Bank transfer is retained. Both product and cargo payment quotes support Paystack.

Product titles are removed from feed/story/saved cards, speaker controls use icons, cards have 12dp horizontal margins and 16dp separation, and story taps scroll to their product in the feed. Customer copy describes pickup from store after payment confirmation.


## Update 1.0.10: saved account details
Install core 0.3.1. Customers edit their name, phone, state, town and delivery address in Account. Checkout fetches and displays saved details, with an Edit account details action. Missing details redirect to the Account editor and return to checkout after save. Updates apply to future orders; existing order snapshots are preserved. Email remains the sign-in identity.


## Update 1.0.11
Install core 0.4.1. Account state and LGA selections use bundled Nigerian location data with 36 states, FCT and 774 local areas. Changing state clears the LGA selection. Existing valid selections are restored. Legacy free-text towns must be updated to a valid local government area before a new order can be placed. See LOCATION-DATA-NOTICE.txt for dataset attribution.

## Update 1.0.12 / Core 0.5.0

- Test and release API / product app-link host: `https://cairo12.ng/`.
- Fixed dialog ownership so Close always dismisses the current dialog. It only waits while an order is submitting.
- Checkout groups delivery details, full multiline cargo radio choices, and readable shipping terms.
- Mobile sessions have no automatic expiry; logout or PIN/password reset revokes access. Existing valid sessions migrate automatically. Website customer sessions use renewable ten-year remembered cookies; clearing browser/app data still signs out.
- Account → Notifications offers explicit Android notification opt-in and phone settings.
- Private Studio → Notifications & ads manages new-product/order alerts and promotional campaigns for app/web/both. Popups show only while browsing, once per visit plus each campaign's once/daily rule. No popups during checkout/account/payment forms.
- Firebase credentials are NOT supplied; this APK intentionally reports push setup is incomplete. See FIREBASE-SETUP.md. No push delivery has been tested with a real Firebase project.
- `tooling/cairo12-test.keystore` is a public debug-only key (standard Android test credentials). It is included so later test builds can update this build. Never use it for production; use your own protected release/upload key.

This test build uses a new test certificate. Uninstall older test APKs before installing it if Android reports an incompatible update. Local bag/saves/sign-in are cleared by uninstall; server-side account details and orders are preserved.

## Update 1.0.13 / Core 0.6.0
The story strip uses the shared rolling-seven-day ranking: active likes × 3 + qualified product views. It shows “Popular this week”, or “Discover this week” until activity is collected. Likes come from the existing save heart and double-tap; unlikes remove their contribution. Install Core 0.6.0 first. Existing local saves are retained and not backdated as new likes. Tapping a story scrolls to its feed card, or opens its options if that product is not in the loaded page. The caption and story row scroll vertically with the feed. The same 1.0.12 test key is retained, so an in-place update from 1.0.12 is supported.

## Update 1.0.14 — pull to refresh
At the top of Discover, drag down and release to reload page one, product prices and weekly stories. The indicator uses Cairo12 teal/gold. Horizontal story/product swipes remain separate. Refresh is available on empty/error screens; failed refreshes preserve existing cached products. Duplicate refresh/pagination requests are blocked, and old responses cannot update a different screen. Refresh resets playback safely and returns the feed to its weekly story strip. Bag, saved products, cached images and secure sign-in are preserved. API requests revalidate rather than reuse HTTP cached responses. Same cairo12.ng host and test signing key as 1.0.12/1.0.13. No WordPress plugin update is required for this Android-only change.

## Update 1.0.15 — video thumbnails and text colours
Install Core 0.7.0 first. Product previews are independent of colour images. Video-only saved items, stories, bag and orders use uploaded thumbnail URLs or locally cached frames read directly from the public video URL. Existing logo references are ignored for previews. Failed JPEG loading falls back to video frames; unavailable media uses a neutral media icon. Colour-picture controls are removed; text colours and Other remain. R2 public URLs are not rewritten through WordPress. Existing WordPress media is not automatically migrated. Same cairo12.ng backend, test signing key and pull-to-refresh support.

## Update 1.0.16 — application cache and Firebase

The supplied google-services.json matches ng.cairo12.app.debug and is included for test APK/GitHub builds. Android client configuration is now present. A production Firebase Android client for ng.cairo12.app is still needed before a production push-enabled build. The server service-account key belongs only in the private WordPress dashboard. Actual push delivery requires matching server setup and customer opt-in; it was not live-tested here.

Images/thumbnails now use Glide 4.16.0 with a shared memory cache, bitmap pool, coalesced loads and a 128 MiB disk LRU. All requests use the same bounded 720×960 target size for reuse. HTTP loads keep direct public media URLs and the Cairo12 User-Agent. The old thumbnail cache (up to 40 MiB) remains readable during upgrades. Legacy video frames use a custom asynchronous JPEG loader, isolated to one worker; Glide caches that JPEG and its decoded thumbnail. It does not download entire video files through Glide to make frames. Cold legacy videos can still require a remote frame read; ready-made thumbnail URLs remain the best first-load path.

The weekly story strip only starts image/frame loads for visible tiles. Look-ahead fetches up to six ready-made image thumbnails on unmetered connections, two on metered connections, and none under Data Saver. Look-ahead never extracts a cold video frame. Current playback and onscreen image requests still work under Data Saver.

Media3 keeps up to 128 MiB of played video segments with LRU eviction. It reuses cached ranges during playback. Only the next video's first 512 KiB may be prefetched, only on unmetered connections and outside Data Saver. Failed speculative loads back off for thirty seconds. The cache does not pre-download the catalogue's videos.

Public catalogue GETs have a separate atomic 8 MiB / 128-entry disk LRU. They are reused for five minutes, reducing repeat WordPress requests. On network/5xx failures a copy up to twenty-four hours old may be shown with an offline notice. Pull-to-refresh bypasses that cache, and bag price checks request fresh product data. Account/profile, orders, receipts, authentication and payments are never stored in this catalogue cache. Checkout remains online and server-validated. Bag/saves/Keystore sign-in remain separate from disposable caches.

Media URLs act as content keys: new upload URLs create new cache entries; an object overwritten at the exact same URL may retain its cached image. Android may clear cache files under storage pressure; uninstall/clear app storage also removes them. Existing cached first-page data is retained for immediate rendering. No site/R2 migration or WordPress plugin change is included in this Android-only release.
