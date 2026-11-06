
## Update 1.0.16 — application cache and Firebase

The supplied google-services.json matches ng.cairo12.app.debug and is included for test APK/GitHub builds. Android client configuration is now present. A production Firebase Android client for ng.cairo12.app is still needed before a production push-enabled build. The server service-account key belongs only in the private WordPress dashboard. Actual push delivery requires matching server setup and customer opt-in; it was not live-tested here.

Images/thumbnails now use Glide 4.16.0 with a shared memory cache, bitmap pool, coalesced loads and a 128 MiB disk LRU. All requests use the same bounded 720×960 target size for reuse. HTTP loads keep direct public media URLs and the Cairo12 User-Agent. The old thumbnail cache (up to 40 MiB) remains readable during upgrades. Legacy video frames use a custom asynchronous JPEG loader, isolated to one worker; Glide caches that JPEG and its decoded thumbnail. It does not download entire video files through Glide to make frames. Cold legacy videos can still require a remote frame read; ready-made thumbnail URLs remain the best first-load path.

The weekly story strip only starts image/frame loads for visible tiles. Look-ahead fetches up to six ready-made image thumbnails on unmetered connections, two on metered connections, and none under Data Saver. Look-ahead never extracts a cold video frame. Current playback and onscreen image requests still work under Data Saver.

Media3 keeps up to 128 MiB of played video segments with LRU eviction. It reuses cached ranges during playback. Only the next video's first 512 KiB may be prefetched, only on unmetered connections and outside Data Saver. Failed speculative loads back off for thirty seconds. The cache does not pre-download the catalogue's videos.

Public catalogue GETs have a separate atomic 8 MiB / 128-entry disk LRU. They are reused for five minutes, reducing repeat WordPress requests. On network/5xx failures a copy up to twenty-four hours old may be shown with an offline notice. Pull-to-refresh bypasses that cache, and bag price checks request fresh product data. Account/profile, orders, receipts, authentication and payments are never stored in this catalogue cache. Checkout remains online and server-validated. Bag/saves/Keystore sign-in remain separate from disposable caches.

Media URLs act as content keys: new upload URLs create new cache entries; an object overwritten at the exact same URL may retain its cached image. Android may clear cache files under storage pressure; uninstall/clear app storage also removes them. Existing cached first-page data is retained for immediate rendering. No site/R2 migration or WordPress plugin change is included in this Android-only release.
