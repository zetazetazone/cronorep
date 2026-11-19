# App Store listing: CronoRep (Apple Watch)

App ID: 6834925956 · Bundle ID: `com.cronorep` (watch app `com.cronorep.watchkitapp`)
Platform: watch-only app (no iPhone screenshots needed)
Price: Free · In-app purchase: `lifetime_unlock` (Non-Consumable, 1.99 USD, base country United States)
Category: Health & Fitness (secondary: Sports)
Privacy policy URL: https://enautomatico.online/cronorep/privacy
Support URL: https://enautomatico.online/cronorep/
Screenshots: `docs/store/assets/apple-watch-1..5-*.png` (422x514, Apple Watch Ultra size)
IAP review screenshot: `docs/store/assets/apple-watch-6-paywall.png`

The Apple listing describes only the Apple Watch app. Do not name other platforms here (Guideline 2.3.10).

## Name (max 30)

CronoRep: Gym Rest Timer

## Subtitle (max 30)

Tap anywhere. Feel the buzz.

## Promotional text (max 170)

The rest timer you start with one tap. Your Apple Watch buzzes when the rest is over, also with your wrist down.

## Keywords (max 100, commas, no spaces)

rest,timer,gym,workout,sets,lifting,weights,strength,interval,countdown,haptic,training,reps,break

## Description (max 4000)

CronoRep is a rest timer for the gym, made for Apple Watch.

Finish your set. Tap anywhere on the screen. Rest. Your watch buzzes when the rest time ends. No small buttons, no menus.

HOW IT WORKS
• Tap anywhere to start or restart the rest timer.
• Three soft taps warn you in the last 3 seconds.
• One strong buzz tells you the rest is over, also with your wrist down.
• After that, the timer counts up, so you see how long you go over.
• Hold the screen to set the rest time in 5 second steps.
• Quick presets: 0:30, 1:00, 1:30 and 2:00.

MAKE IT YOURS
• Minimal mode: only the time, no ring.
• Dim screen: darker text and a black background to save battery.
• Backgrounds: black, 6 colour gradients, or animated gym GIFs. GIFs need an internet connection.

FREE TRIAL AND PRICE
• You get 10 hours of timer use for free. The trial counts only while a rest timer runs with the app on screen.
• After the 10 hours, unlock CronoRep with one payment.
• Pay one time. Use it forever. No subscription. No ads.

PRIVACY
• No account. No analytics. No ads. The app sends no data to us.
• GIF backgrounds are off by default. When you turn them on, the GIFs download from GIPHY.

## What's New (version 1.0.0)

First release.

## Copyright

2026 <the seller name shown in App Store Connect>

## Age rating

Answer "None" / "No" to every question. Result: 4+.

## App privacy

Data Not Collected. The watch app has no third-party SDKs and sends no data to us. The trial time stays in the watch Keychain.
Its only network use: when the user turns on GIF backgrounds, it downloads GIF images from media.giphy.com. GIPHY sees the IP address only to serve the image (the privacy policy says this). No data is linked to the user or used for tracking, so this stays "Data Not Collected".

## In-app purchase: lifetime_unlock

- Type: Non-Consumable
- Reference name: Lifetime unlock
- Price: 1.99 USD (base country United States)
- Display name (max 35): Lifetime unlock
- Description (max 55): Use the rest timer forever. Pay one time.
- Review screenshot: `docs/store/assets/apple-watch-6-paywall.png`
- Review notes: Open the app, hold the main screen, tap "Unlock now".
- Submit it together with app version 1.0.

## App Review notes

- The app is watch-only. No login and no account.
- Tap anywhere on the main screen to start the rest timer (default 1:30).
- Hold the main screen to open the settings: rest time, presets and the free trial status.
- The free trial is 10 hours of timer use. You do not need to wait for it to end: in the settings, tap "Unlock now" to open the purchase screen for the in-app purchase `lifetime_unlock`. "Restore purchase" is on the same screen.
- Before the first rest, the app shows the trial length and the price one time.
- Background mode: the app uses a WKExtendedRuntimeSession (self-care) only during a rest countdown, which is 10 minutes or less. It keeps the timer running with the wrist down so the end-of-rest haptic plays on time. The session ends when that haptic plays. A local notification is the backup if the system ends the session.
- The app asks for notification permission at the first rest, for that backup alert.
- Look settings are at the bottom of the settings screen: Minimal, Dim screen, and Background (Black, Colour, GIF). GIF backgrounds download gym GIFs from GIPHY and need an internet connection. Without one, the background stays black.
- Guideline 4 (icon): the new icon has a blue background with a light glow, so it shows as a clear circle on Apple Watch.
