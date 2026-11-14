# Google Play listing: CronoRep

Package name: `com.cronorep`
In-app product ID: `lifetime_unlock` (one-time product, 1.99 USD)
Category: Health & Fitness
Form factor: Wear OS (watch only)
Privacy policy URL: https://enautomatico.online/cronorep/privacy

## App name (max 30)

CronoRep: Gym Rest Timer

## Short description (max 80)

Rest timer for the gym. Tap anywhere to restart. Feel the buzz when rest ends.

## Full description (max 4000)

CronoRep is a rest timer made for the gym, on your Wear OS watch.

Finish your set. Tap anywhere on the screen. Rest. Your watch vibrates when the rest time ends. No small buttons, no menus, no phone.

HOW IT WORKS
• Tap anywhere to start or restart the rest timer.
• Three soft ticks warn you in the last 3 seconds.
• One long vibration tells you the rest is over, also with your wrist down.
• After that, the timer counts up, so you see how long you go over.
• Hold the screen to set the rest time in 5 second steps.
• Swipe for quick presets: 0:30, 1:00, 1:30 and 2:00.

FREE TRIAL AND PRICE
• You get 10 hours of timer use for free.
• After the 10 hours, the timer needs a one-time payment to unlock.
• Pay one time. Use it forever. No subscription. No ads.

GIF BACKGROUNDS
• Optional animated gym GIF backgrounds from GIPHY. They are free. They need an internet connection.

PRIVACY
• No account. No analytics. No ads. Your settings stay on your watch.

CronoRep works alone on your watch. You do not need a phone app.

## Notes for the reviewer (App content / testing instructions)

- This is a standalone Wear OS app. No login and no account.
- Tap anywhere on the main screen to start the rest timer (default 90 seconds).
- Long-press the main screen to open the settings.
- The foreground service (type specialUse) runs only during the countdown.
  It holds a wake lock so the end-of-rest vibration is exact when the screen is off.
  It stops itself when the vibration ends.
- The free trial is 10 hours of timer use. The paywall then offers the
  one-time product `lifetime_unlock`. Use a license tester account to buy it.

## Foreground service declaration (specialUse)

Use case text for the Play Console form:

"CronoRep is a workout rest timer. When the user taps the screen, a countdown
starts (default 90 seconds). The foreground service runs only during this
countdown. It keeps the countdown exact when the watch screen turns off, and it
plays the vibration that tells the user the rest time is over. The user starts
the task with a tap and sees an ongoing notification. The service stops itself
right after the vibration. No other foreground service type fits a short
user-started haptic timer."

The form also asks for a short video: start the timer, lower the wrist so the
screen is off, and show the vibration at the end.

## Data safety form

- Does the app collect or share user data: No.
- The app has no account, analytics or ads SDK.
- Google Play Billing is handled by Google.
- GIF downloads go from the watch to GIPHY. The developer receives no data.

## Release notes, version 1.0.0 (max 500 characters)

<en-US>
First version of CronoRep, the gym rest timer for your watch.
• Tap anywhere to start or restart your rest.
• Three soft ticks, then one long vibration when the rest ends.
• Rest time in 5 second steps, plus quick presets.
• Optional funny gym GIF backgrounds.
• 10 hours free. Then one payment unlocks it forever.
</en-US>
