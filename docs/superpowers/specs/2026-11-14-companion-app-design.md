# CronoRep phone companion app: design

Date: 2026-11-14

## Goal

Google Play asks for 12 testers for 14 days before production access. Paid testers
have phones, not watches. A phone app with the same package name (`com.cronorep`)
and the same signing key lets phone testers count for the whole listing.

The phone app must do something real, so Google does not reject it for minimal
functionality. It controls how the watch timer looks.

## Not in scope

- A working rest timer on the phone. It would give away what the watch app sells.
- Billing or the free trial on the phone. All look settings are free.
- Sync from watch to phone.
- The Apple Watch app.
- An "install on watch" button.

## Modules

| Module | Type | Purpose |
| --- | --- | --- |
| `:app` | application (exists) | Wear OS app. Gets the three new look features. |
| `:mobile` | application (new) | Phone companion app. |
| `:shared` | Android library (new) | Look settings model, storage, sync keys, GIF list, gradient list. |

`:mobile` uses `applicationId = "com.cronorep"`, namespace `com.cronorep.mobile`,
`minSdk = 26`, `targetSdk = 35`, `compileSdk = 36`, and the same `release` signing
config from `keystore.properties` as `:app`.

Version codes must be different for each uploaded bundle of one package.
The phone app starts at `versionCode = 1001`. The watch app goes from 2 to 3.

## Look settings (`:shared`)

```kotlin
enum class BackgroundKind { BLACK, GRADIENT, GIF_RANDOM, GIF_FIXED }

data class LookSettings(
    val minimalist: Boolean = false,
    val saveBattery: Boolean = false,
    val background: BackgroundKind = BackgroundKind.BLACK,
    val gradientId: Int = 0,      // index in Gradients.ALL
    val gifId: String? = null,    // GIPHY id, used when background == GIF_FIXED
)
```

- `LookSettings.effectiveBackground`: returns `BLACK` when `saveBattery` is true.
  If not, it returns `background`. `GIF_FIXED` with an unknown or null `gifId`
  falls back to `GIF_RANDOM`. A `gradientId` out of range falls back to 0.
- `LookStore`: reads and writes `LookSettings` in the `cronorep` SharedPreferences
  file. Migration: if no background is saved and the old key `gifs_enabled` is true,
  the background is `GIF_RANDOM`.
- `LookSync`: constants and converters for the Data Layer. Path `/look`. Functions
  `toDataMap(LookSettings)` and `fromDataMap(DataMap)`. Unknown values fall back to
  the defaults.
- `GifCatalog`: the 43 GIPHY ids (moved from `Gifs.kt`) and
  `url(id) = "https://media.giphy.com/media/$id/200.gif"`.
- `Gradients.ALL`: 6 gradients. Each one has a name and two ARGB colours. Both apps
  draw them in code. No image files.

## Watch app changes (`:app`)

- **Minimalist mode:** the timer screen does not show the progress ring and does
  not show the small "total" line. The big time and the REST / GO label stay.
- **Save battery mode:** the background is black (no GIF, no gradient). The screen
  updates one time each second and not ten times. The vibrations do not change.
- **Background:** black, gradient, random GIF (as today), or one fixed GIF.
  Gradients get the same dark layer as GIFs, so the text stays easy to read.
- `Gifs.kt` becomes `Look.kt`: an object that holds the current `LookSettings` as
  Compose state, loads it from `LookStore`, and gives the current GIF URL.
- **Watch settings:** the "GIF on / GIF off" button is replaced by a third settings
  page with three buttons: Minimal on/off, Battery on/off, and Background (each tap
  goes to the next one: Black, Gradient, GIF). The watch keeps the gradient and the
  fixed GIF that the phone chose. The watch still works without a phone.
- **Receive settings:** a `WearableListenerService` (`LookListenerService`) listens
  for data changes on path `/look`. It saves the settings with `LookStore` and
  updates `Look`. It works when the watch app is closed. The last change wins.
- New dependency: `com.google.android.gms:play-services-wearable`.

## Phone app (`:mobile`)

One activity, one screen, Jetpack Compose with Material 3, dark theme with the
watch colours (green `#4ADE80`, orange `#FB923C`).

From top to bottom:

1. **Live preview.** A round black "watch" that shows a demo countdown from 0:20,
   then "GO" for 5 seconds, then starts again. It uses the current settings: ring or
   no ring, background, one update each second in save battery mode.
2. **Status line.** "Watch connected" or "No watch found. Your settings are saved.
   They go to the watch when it connects."
3. **Minimalist mode** switch.
4. **Save battery mode** switch. When it is on, the background choices are greyed
   out and a note says the background is black.
5. **Background** choice: Black, a row of 6 gradient swatches, Random GIF, and a
   grid of the 43 GIF thumbnails to choose one.
6. A small footer: privacy policy link and "GIFs by GIPHY".

Each change is saved with `LookStore` and sent with `DataClient.putDataItem`
(urgent). The Data Layer keeps the item and delivers it later if no watch is
connected. A send error only changes the status line. The app does not crash and
does not block the user.

Permissions: `INTERNET` only (for the GIFs). No account, no analytics, no ads.

The launcher icon is the same as the watch app icon.

## Tests

- Unit tests in `:shared`: DataMap round trip, fallbacks for bad values, the
  `gifs_enabled` migration, and `effectiveBackground`.
- Build check: `./gradlew :shared:testDebugUnitTest :app:assembleDebug :mobile:assembleDebug`.
- Manual check: a phone emulator paired with a Wear OS emulator. Change each
  setting on the phone and see it on the watch.

## Store

- Update `docs/store/play-listing.md`: the watch app works alone, and an optional
  phone app changes how the timer looks. Add notes for the phone form factor.
- The phone bundle goes to the phone closed testing track. The watch bundle goes to
  the Wear OS track. Both use the same listing.
- Run the app-store-compliance audit before the upload.
