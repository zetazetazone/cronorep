# CronoRep phone companion app: design

Date: 2026-11-14

## Goal

Google Play asks for 12 testers for 14 days before production access. Paid testers
have phones, not watches. A phone app with the same package name (`com.cronorep`)
and the same signing key lets phone testers count for the whole listing.

The phone app must do something real, so Google does not reject it for minimal
functionality. It has two jobs:

1. It is a full rest timer on the phone. A person with no watch can use the core
   function of CronoRep.
2. It controls how the timer looks, on the phone and on the watch.

## Not in scope

- Sync from watch to phone.
- Sync of the rest time or of the running timer between phone and watch. Each
  device has its own timer and its own rest time.
- Sound at the end of the rest. The phone vibrates, as the watch does.
- The Apple Watch app.
- An "install on watch" button.

## Modules

| Module | Type | Purpose |
| --- | --- | --- |
| `:app` | application (exists) | Wear OS app. Gets the three new look features. |
| `:mobile` | application (new) | Phone companion app. |
| `:shared` | Android library (new) | Timer state, timer service, trial, billing, look settings, sync keys, GIF list, gradient list. |

`:mobile` uses `applicationId = "com.cronorep"`, namespace `com.cronorep.mobile`,
`minSdk = 26`, `targetSdk = 35`, `compileSdk = 36`, and the same `release` signing
config from `keystore.properties` as `:app`.

Version codes must be different for each uploaded bundle of one package.
The phone app starts at `versionCode = 1001`. The watch app goes from 2 to 3.

## Timer core (`:shared`)

These files move from `:app` to `:shared` with no change in behaviour. Both apps
use them.

- `RestTimer.kt`: timer state, rest time, presets, `formatTime`.
- `RestTimerService.kt`: foreground service with the wake lock, the 3 warning
  ticks and the long end vibration. It no longer names `MainActivity` or the app
  icon directly. It opens the launch activity of the package and uses an icon from
  `:shared`. Each app declares the service in its own manifest.
- `Trial.kt`: the 10 hour free trial clock. Each device counts its own 10 hours.
- `Billing.kt`: the one-time product `lifetime_unlock`.

**Payment rule:** the phone app uses the same rule as the watch app. 10 hours of
timer use are free. Then the timer needs the one-time unlock. The product belongs
to the package and the Google account, so one payment unlocks the phone app and
the watch app. Debug builds never lock.

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

One activity, Jetpack Compose with Material 3, dark theme with the watch colours
(green `#4ADE80`, orange `#FB923C`). Portrait only. Three screens.

### Timer screen (start screen)

It works the same as the watch timer, on the full phone screen.

- Tap anywhere: start or restart the rest timer. A short vibration confirms the tap.
- Idle: "TAP TO START", the rest time, and a gear button at the top right.
- Running: REST and the time left, then GO and the time over, and the "total" line.
- The progress ring is a large circle in the center of the screen.
- 3 soft ticks in the last 3 seconds and one long vibration at the end, also with
  the screen off (the shared `RestTimerService`).
- The screen stays on while the app is open.
- The timer screen uses the look settings: minimalist mode, save battery mode and
  the background. The phone timer is thus also the preview of the watch look.
- When the trial is over and the app is not unlocked, a tap opens the paywall.

### Settings screen (gear button)

From top to bottom:

1. **Rest time:** minus and plus buttons in 5 second steps, and the four presets
   0:30, 1:00, 1:30, 2:00.
2. **Minimalist mode** switch.
3. **Save battery mode** switch. When it is on, the background choices are greyed
   out and a note says the background is black.
4. **Background** choice: Black, a row of 6 gradient swatches, Random GIF, and a
   grid of the 43 GIF thumbnails to choose one.
5. **Watch status line.** "Watch connected. It uses the same look." or "No watch
   found. Your look settings go to the watch when it connects."
6. **Trial line:** "Free trial: 9 h 58 min left", "Free trial ended" or
   "Unlocked. Thank you!"
7. A small footer: privacy policy link and "GIFs by GIPHY".

Each look change is saved with `LookStore` and sent with `DataClient.putDataItem`
(urgent). The Data Layer keeps the item and delivers it later if no watch is
connected. A send error only changes the status line. The app does not crash and
does not block the user. The rest time is not sent.

### Paywall screen

The same text as the watch paywall: "Free trial ended", "Pay one time. Use it
forever.", the Unlock button with the price from Google Play, the billing message
and the privacy policy link.

### Manifest

Permissions: `INTERNET`, `VIBRATE`, `WAKE_LOCK`, `FOREGROUND_SERVICE`,
`FOREGROUND_SERVICE_SPECIAL_USE`, `POST_NOTIFICATIONS` and
`com.android.vending.BILLING` (from the billing library). The app asks for the
notification permission at the first timer start on Android 13 and later. The timer
works if the user says no. The service has the same `specialUse` property text as
the watch app. No account, no analytics, no ads.

The launcher icon is the same as the watch app icon.

## Tests

- Unit tests in `:shared`: DataMap round trip, fallbacks for bad values, the
  `gifs_enabled` migration, and `effectiveBackground`.
- Build check: `./gradlew :shared:testDebugUnitTest :app:assembleDebug :mobile:assembleDebug`.
- Manual check on a phone emulator: start, restart, end vibration, settings,
  presets, each look setting, and the paywall (debug extra `paywall`).
- Manual check: a phone emulator paired with a Wear OS emulator. Change each
  look setting on the phone and see it on the watch.

## Store

- Update `docs/store/play-listing.md`: CronoRep works on the watch alone and on
  the phone alone. The phone app also changes how the watch timer looks. Add the
  reviewer notes and the foreground service declaration for the phone form factor.
- The privacy policy does not change: no data leaves the device.
- The phone bundle goes to the phone closed testing track. The watch bundle goes to
  the Wear OS track. Both use the same listing.
- Run the app-store-compliance audit before the upload.
