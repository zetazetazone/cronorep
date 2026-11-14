# Brief for @gitrepo.es: CronoRep reel

For the gitrepo reels session (`C:\Users\zetaz\gitrepo`). Make one reel about this repo with your normal
pipeline and rules (animated + simple versions, Spanish script, README outro, keyword, dm.json).
This file gives the facts and the open points. Facts in the script must come from the README.

## The repo

- Repo: https://github.com/zetazetazone/cronorep (public, MIT)
- Website + waitlist: https://enautomatico.online/cronorep/
- Privacy: https://enautomatico.online/cronorep/privacy
- Owner: the account owner made this app. It is our own product, so say so honestly if asked; do not
  present it as a stranger's repo we "found".

## What it is (facts from the README)

A gym rest timer that runs **on the watch**: Wear OS and Apple Watch. No phone needed.

- Tap anywhere on the watch to start or restart the rest. No small buttons.
- 3 soft ticks in the last 3 seconds.
- One long vibration when the rest ends, also with the wrist down.
- After that it counts up, so you see how long you go over.
- Hold the screen to set the rest time in 5 second steps.
- Presets: 0:30, 1:00, 1:30, 2:00.
- Optional funny gym GIF behind the timer, a new one each rest (GIPHY).
- No ads, no account, no analytics.
- Price: 10 hours free, then one payment of 1.99 USD, forever. No subscription.
  The code is free on GitHub (MIT). Say both; never call the app "100 % gratis".
- Status: **not in the stores yet** (Google Play and App Store "coming soon"). The waitlist sends one
  email on launch day.

## Angle ideas (pick or improve)

- Hook: "Deja de mirar el celular entre series." / "Tu reloj te avisa cuándo volver a levantar."
- Problem: phone timers need unlock + small buttons + attention between sets.
- Show: tap anywhere → countdown → 3 ticks → long buzz → "GO" in orange, counting up.
- Funny beat: the gym GIFs behind the timer.
- Comparison allowed by your rules: "alternativa gratis y open source a las apps de cronómetro de gym con suscripción".

## Assets you can use

| What | Path |
| --- | --- |
| 15 s promo (9:16, no sound) | `C:\Users\zetaz\Projects\cronorep-promo\out\cronorep-full-15s.mp4` |
| App demo only (10 s) | `C:\Users\zetaz\Projects\cronorep-promo\out\cronorep-demo-10s.mp4` |
| End card (4 s) | `C:\Users\zetaz\Projects\cronorep-promo\out\cronorep-whatitdoes-4s.mp4` |
| Demo GIF (watch flow) | `C:\Users\zetaz\Projects\cronorep\docs\readme\demo.gif` |
| Real watch screenshots | `C:\Users\zetaz\Projects\cronorep\docs\store\assets\wear-*.png` |
| App icon | `C:\Users\zetaz\Projects\cronorep\docs\store\assets\icon-512.png` |
| Feature graphic | `C:\Users\zetaz\Projects\cronorep\docs\store\assets\feature-graphic-1024x500.png` |
| Gym GIF ids used by the app | `C:\Users\zetaz\Projects\cronorep\app\src\main\java\com\cronorep\Gifs.kt` (`https://media.giphy.com/media/<id>/200.gif`) |
| Brand colours | green `#4ade80`, orange `#fb923c`, black screen, Roboto |
| Brand logos to show | Wear OS, Apple Watch, GitHub (Simple Icons) |

The website hero has a live 3D watch: a screen recording of https://enautomatico.online/cronorep/ is good B-roll.

## Open points (decide before you render)

1. **Keyword**: `GYM` is taken (openGym). Suggest **`RELOJ`** (free in `bot/links.json`). `DESCANSO` is also free.
2. **DM "Cómo instalarlo"**: there is no GitHub release and no store link yet, so `device: "android"`
   (sends to `/releases/latest`) would send people to an empty page. Until launch, the DM should send
   the **website/waitlist link** instead of an install prompt. If `bot/ig_bot.py` has no way to do that,
   add a small option (for example `"device": "watch"` → waitlist link + repo link) and tell the owner.
3. **Spanish README**: the repo has no `README.es.md`. `capture.mjs` will use Google Translate.
   Check the translation; if you want a clean one, ask the owner and the cronorep session will add
   `README.es.md` to the repo.
4. **Timing**: posting before the store launch sends traffic to a waitlist, not to downloads. Ask the
   owner whether to post now (waitlist) or schedule it for launch day.
