# Cairo12 Android verification

The source targets https://cairo12.ng/ and uses the production ID ng.cairo12.app with a separate test package ng.cairo12.app.debug.

## Completed checks

- PHP syntax checks passed for every file in the supplied WordPress plugin, using PHP 8.4 CLI.
- Nine isolated mobile-auth assertions passed. These cover leading-zero PIN login, token-hash storage, token use on Cairo12 orders, browser-nonce independence, rejection outside the Cairo12 namespace, reset invalidation and five-attempt PIN lockout and rejection of a namespace string inserted in a query. These use WordPress/database doubles.
- Gradle `:app:assembleDebug` and `:app:lintDebug` passed with SDK 36. Lint reports no errors and remaining nonblocking style/accessibility warnings. The test APK signature passed Android apksigner verification.
- APK metadata confirms version 1.0.0-test, package ng.cairo12.app.debug, minimum SDK 24 and target SDK 36.

## Remaining live checks

- The initial request to https://cairo12.ng/wp-json/cairo12/v1/catalogue?page=1 returned HTTP 503 in this environment. Live site API, real customer login, receipt submission, mail delivery and cargo changes need a responding site and the supplied plugin update.
- No emulator or physical Android device was available for interactive verification. Check feed gestures, video sound, system insets, keyboard visibility and document picking on a phone.
- The supplied APK uses test signing. A production upload key and Play Console release setup remain under your control.
- Foreground order refresh is implemented. Background mobile push awaits Firebase configuration.


## Version 1.0.1 UI update
- Centered, single-line bottom navigation labels with bounded widths and side padding.
- Disabled baseline alignment in horizontal rows so navigation tabs align.
- Added field borders, hints and spacing; separated action buttons and simplified PIN recovery.
- Replaced DNS, timeout and connection exception details with customer-facing messages.
- Domain DNS configuration is external to this APK. No device UI test has been performed for this update.


## Version 1.0.3
Cream Android launch background, teal/gold shopping bag and native animated introduction. Indeterminate indicator does not claim percentage progress. API loading continues behind the intro; a separate spinner remains while the catalogue loads. Reduced system animation settings are respected. Dark-mode screenshots and physical-device launch testing remain pending.


## Version 1.0.4
- System launch splash uses the Cairo12 app icon.
- Native intro lasts 3.2 seconds then fades; centered wordmark, tagline and caption.
- Shopping bag sways, rises and subtly pulses instead of showing a progress bar.
- Reduced-motion setting disables the bag motion.
- Phone visual verification remains pending.


## Version 1.0.5
Debug uses original Cloudways host; release retains cairo12.ng. Read-only Cloudways catalogue request returned HTTP 200 with seven products. Launch and adaptive icons now have safe padding and cream-blended JPEG backgrounds through Android drawable resources. Assemble, lint and APK signature verification passed. Physical-device verification remains pending.


## Version 1.0.6
Removed vertical pager snapping to allow complete story-row recovery. Autoplay selects the largest visible product and retries after media attachment, validates active product before deferred playback, tracks media URL when views are reused and resumes in foreground only. Player blank surface uses the card background. Live Cloudways namespace returned no mobile routes: companion core 0.2.1 must be installed there for PIN login. Device swipe/playback testing remains pending.


## Version 1.0.7
- Cached first catalogue page shown immediately while server refreshes; saved catalogue retained on network failure. Checkout still refreshes products.
- Images and extracted video thumbnails persist in a bounded 40 MB disk cache and 24 MB memory cache. Source thumbnail or product image is preferred; otherwise a frame is extracted. Brand fallback remains if extraction fails.
- Video poster stays above the player until its first rendered frame, and returns when the player moves away.
- Shared 80 MB LRU video cache supports replay across app launches. Up to 512 KB of the next video is prefetched on unmetered networks only.
- Physical-device scrolling, offline restart and buffering verification remain pending.


## Version 1.0.8
Signed-in account screen now reserves a teal profile card above actions, with initials avatar, name and email; grouped order/saved/bag links and security section; separate sign-out confirmation. Profile fetch updates existing fields instead of appending below actions. Reuses existing actions and endpoints. Physical-device visual verification remains pending.


## Version 1.0.9
- Android assemble/lint, PHP syntax and shop.js syntax checked.
- 13 isolated gateway checks cover ownership, key modes, amount/currency, idempotency, quote changes, signed/forged webhook and secret non-disclosure. Existing 16 auth tests retained. These use stubs, not real payments.
- No Paystack credentials supplied: real/test gateway transactions, 3DS and on-device payment UI remain unverified. Configure keys/webhook and use test payments before live deployment.
- No site changes deployed by this task; deliverables require installation.


## Version 1.0.10
Added validated authenticated profile update endpoint and account delivery card/editor in Android and storefront. Checkout displays saved details and rejects missing or changed account delivery data before placing an order. Existing orders retain snapshots. Profile tests check required fields, phone validation, saved/returned details and account identity protection. Phone and live profile round-trip testing remain pending.


## Version 1.0.11
State and dependent LGA dropdowns replace free-text State/Town fields. Bundled dataset has 37 state/FCT entries and 774 LGAs/area councils; exact app/plugin data match and per-state uniqueness verified. Profile tests now include count, FCT aliases and invalid state-LGA pair rejection. Android assemble/lint pass; on-device dropdown interaction remains pending. Theme does not require updating.

## 1.0.12 / Core 0.5.0 — 2026-11-05

- Debug and release host changed to https://cairo12.ng/ per latest instruction; manifest app-link host matches.
- Build and lint passed (0 errors; 23 existing/advisory warnings). APK signature verified. DEX inspection confirms cairo12.ng and no original Cloudways host.
- Backend syntax: 13 PHP files. Behavioral suites: mobile auth/profile 28, Paystack 13, portal permissions 16, notifications/promotions 17 (74 total). JavaScript syntax passed.
- Live read-only catalogue with Cairo12 Android User-Agent: HTTP 200, nine products. Unauthenticated mobile/session: expected 401. New announcements endpoint: 404 until companion plugin is installed. Default automated request was blocked by Cloudflare 1010; app API, image and video requests now identify as Cairo12-Android/version.
- No physical-device UI run, live payment, or real Firebase delivery performed. Firebase configuration has not been supplied, so background push is disabled in the delivered test APK. Promotional popups work independently after installing Core 0.5.0.
- The earlier temporary test signing key was unavailable. This build uses a new public debug-only test key included in tooling for future test upgrades. Updating the older APK in place will fail signature matching; uninstall it before installing this one. This clears local bag/saved media/sign-in, but website orders and account details remain. Production signing remains separate.

## 1.0.13 / Core 0.6.0 — 2026-11-05
Android assembleDebug and lintDebug passed; signature verified and matches v1.0.12. APK DEX checks confirm 1.0.13-test, cairo12.ng, weekly stories and like/view endpoints. Browser QA covered the actual storefront JavaScript in desktop and 390px mobile previews, hero CTA, full-width stories above the product grid, and the mobile weekly caption. Backend: 87 behavioral checks passed across auth, payments, portal, notifications and popularity. Actual weekly SQL tested with SQLite fixtures; live WordPress/MySQL mutations and physical-phone UI remain untested. Firebase configuration is still absent.

## 1.0.14 — pull-to-refresh — 2026-11-05
Added AndroidX SwipeRefreshLayout 1.1.0 explicitly and FeedRefreshLayout to protect horizontal story/media gestures. Refresh is gated to the top of Discover and one in-flight load; products/weekly stories reload from page one with HTTP revalidation. Existing data remains on failure. Error and empty notices retain the swipe container. Both response paths stop the indicator, screen-generation guards reject stale responses, and refresh resets attached playback before rebinding. Bag/saves/sign-in storage is not modified. AssembleDebug/lintDebug passed with zero lint errors; APK signature verified and matches v1.0.13. DEX checks confirm version, refresh classes and cairo12.ng. Physical-phone gesture verification remains pending.

## 1.0.15 / Core 0.7.0 — 2026-11-06
Video-only previews separated from colour images; neutral placeholders replace product-logo fallbacks. New browser uploads prepare a JPEG locally; existing video-only items use direct media frame fallbacks, cached/coalesced on Android. Colours are text-only; IDs retained. API and display paths retain public R2 URLs with no media proxy. Saved Abayas, bag and text colour selection reviewed in browser preview using an existing video-only product. New backend media tests: 9 passed, existing backend suites: 87 passed. Browser preview-helper/media setup and syntax checks passed. Android preview unit tests: 6 passed. Assemble/lint passed (0 errors, 24 warnings); signature matches v1.0.14. No live R2 upload or physical-phone frame extraction test performed. No Firebase configuration is included.

Final lifecycle check: thumbnail loaders ignore new work after shutdown, including delayed fallback callbacks. Rebuilt and reran six unit tests, assembleDebug and lintDebug successfully after this guard.

## 1.0.16-test
- Debug APK assembled; Android lint passed.
- 13 unit tests passed (catalogue persistence, expiry, eviction, corruption and preview handling).
- Firebase Android configuration included for ng.cairo12.app.debug.
- No physical-device or live push-delivery test performed.
