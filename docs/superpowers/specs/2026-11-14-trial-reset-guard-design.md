# Free trial that survives a new install: design

Date: 2026-11-14

## Goal

Today the 10 h free trial is saved only on the device. A user can delete the app
(or use "Clear data"), install it again and get a new 10 h trial. After this work,
a new install on the same device gets the old used time back.

## Decisions

- **No sign-in.** The app knows the device by a device ID, not by an account.
- **Offline keeps working.** The timer never waits for the server. Used time is
  counted on the device and sent when the internet comes back.
- **Apple Watch uses the Keychain, not the server.**

## Not in scope

- One trial shared by the phone and the watch. Each device keeps its own 10 h
  trial, as today.
- Protection after a factory reset. A factory reset gives a new `ANDROID_ID`.
- Protection against a user who never goes online after a new install.
- Any change to the `lifetime_unlock` purchase. Play and the App Store already
  restore it.

## Android (watch app `app/` and phone app `mobile/`)

### Device ID

- Read `Settings.Secure.ANDROID_ID`. Since Android 8 it is unique per app signing
  key, user and device, and it stays the same after a new install.
- Send only `trialId = sha256("cronorep-trial:" + ANDROID_ID)` as lowercase hex.
  The raw ID never leaves the device.
- Debug and release builds have different signing keys, so they have different
  IDs. That is correct.

### New code in `shared/`

`TrialSync.kt`, one object, one job: send the local used time, get the server
value back.

- `suspend fun sync(context: Context, usedMs: Long): Long?` — POST to the server,
  return the server value, or `null` on any error (no internet, timeout, bad
  answer). Timeout: 5 s connect, 5 s read. Runs on `Dispatchers.IO`.
- Uses `HttpURLConnection` and `org.json`. No new library.
- The URL is a `BuildConfig` field (`TRIAL_API_URL`) in `shared/build.gradle.kts`.

`Trial.kt` gets one new function:

- `fun merge(context: Context, serverMs: Long)` — sets
  `usedMs = max(usedMs, serverMs)`, saves it. It never makes `usedMs` smaller.

The merge rule lives in a pure function `mergeUsed(local: Long, server: Long): Long`
so a unit test can check it without Android.

### When the app syncs

Both `MainActivity` files (`app/` and `mobile/`):

- After `Trial.load(this)` in `onCreate`: launch a sync in `lifecycleScope`, then
  `Trial.merge` with the result.
- After `Trial.stopCounting(this)` when the app leaves the screen: launch a sync.
  The local value is already saved, so a lost sync loses nothing.
- If the sync returns `null`, do nothing. Try again next time.

The lock check (`isLocked()`) does not change. It reads `Trial.expired`, which now
also includes the server value.

## Server: `trial-api/`

A new Neon Function. Same pattern as `waitlist-api/` (no dependencies, SQL over
Neon's HTTP endpoint, esbuild bundle). It uses the same Neon project as the
waitlist, in a new table.

### Table

```sql
CREATE TABLE trial_usage (
  trial_id   text PRIMARY KEY,          -- sha256 hex, 64 chars
  platform   text NOT NULL,             -- 'wear' | 'phone'
  used_ms    bigint NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);
```

### API

`POST /` with JSON `{ "id": "<64 hex>", "platform": "wear"|"phone", "usedMs": <int> }`

- Check: `id` matches `^[0-9a-f]{64}$`, `platform` is known, `usedMs` is an
  integer from 0 to 36 000 000 (the 10 h limit). A bigger value is cut to the limit.
- Store:
  ```sql
  INSERT INTO trial_usage (trial_id, platform, used_ms) VALUES ($1, $2, $3)
  ON CONFLICT (trial_id) DO UPDATE
    SET used_ms = GREATEST(trial_usage.used_ms, EXCLUDED.used_ms), updated_at = now()
  RETURNING used_ms;
  ```
- Answer: `200 { "ok": true, "usedMs": <stored value> }`.
- Errors: `400` bad input, `429` rate limit, `500` database error. The app treats
  every error as "no answer".
- `GET /` returns `{ ok: true, service: "cronorep-trial" }` for a health check.
- Rate limit: in-memory, 30 posts per 10 minutes per IP.
- No CORS. Only the apps call it.

The stored value only goes up. A client that sends a small number cannot lower it.

## Apple Watch (`apple/Watch/`)

- Move `trialUsed` from `UserDefaults` to the Keychain (`kSecClassGenericPassword`,
  service `com.cronorep.trial`, accessible `AfterFirstUnlock`).
- Small `TrialKeychain` helper with `read() -> Double` and `write(Double)`.
- On first start of the new version: if the Keychain is empty and `UserDefaults`
  has a value, copy it to the Keychain.
- Risk: Apple does not promise that Keychain items stay after an app delete on
  watchOS. The real-device test below decides. If they do not stay, we accept the
  gap on Apple Watch for now and note it in the README.

## Privacy and store text

- `docs/store/privacy-policy.html`: replace "this data stays on your devices" for
  the trial. New text: the app sends a scrambled device ID and the used trial time
  to our server, only to stop a reset of the free trial. No name, email or
  account. Data is kept while the app is offered.
- `docs/store/play-listing.md`: update any "no data leaves the device" lines.
- Google Play Data safety form (done by the user in Play Console): "Device or
  other IDs" — collected, not shared, not optional, purpose "Fraud prevention,
  security, and compliance", encrypted in transit.
- Apple privacy label: no change (no data leaves the Apple Watch).

## Testing

- Unit test (`shared/src/test`): `mergeUsed` never goes down; `trialId` gives the
  same hash for the same input and a 64-char hex string.
- Server test (`trial-api/test.mjs`, Node test runner, fake `sql`): input checks,
  cap at the limit, `GREATEST` result returned.
- Real devices (Xiaomi 2407FPN8EG phone, Galaxy Watch SM-R920), release-signed
  builds so the build-type check does not hide the lock:
  1. Use some trial time. Close the app.
  2. Delete the app. Install it again.
  3. Open it online: the remaining time must match step 1.
  4. Repeat step 2 offline: the trial shows 10 h until the device goes online, then
     drops to the step 1 value.
- Apple Watch: same steps 1–3 on a real watch, to check the Keychain.

## Deploy

1. Create the `trial_usage` table in the Neon project of the waitlist.
2. Build and deploy `trial-api` as a Neon Function with `DATABASE_URL`.
3. Put its URL in `TRIAL_API_URL`.
4. New version codes for the watch and phone apps, then upload.
