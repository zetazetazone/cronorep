# Free Trial That Survives a New Install: Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A new install (or "Clear data") on the same device no longer gives a new 10 h free trial.

**Architecture:** The Android apps (watch `app/`, phone `mobile/`) send a SHA-256 hash of `ANDROID_ID` and the used trial time to a new Neon Function `trial-api/`. The server keeps the biggest value per ID and sends it back; the app keeps the bigger of its own and the server value. The Apple Watch app keeps the used time in the Keychain instead of only `UserDefaults`.

**Tech Stack:** Kotlin (Android, Compose state, `HttpURLConnection`, JUnit 4), Node 24 ESM + esbuild + `node:test`, Neon Postgres over HTTP, Swift (watchOS Security framework).

Spec: `docs/superpowers/specs/2026-11-14-trial-reset-guard-design.md`

## Global Constraints

- Trial limit: 10 h = `36_000_000` ms (Android `Trial.LIMIT_MS`), `36_000` s (Apple `trialLimit`).
- Trial ID: `sha256("cronorep-trial:" + ANDROID_ID)`, lowercase hex, 64 chars. The raw `ANDROID_ID` never leaves the device.
- Platform values: `"wear"` (watch app), `"phone"` (phone app). No other value.
- No new Android library. No new npm runtime dependency (`dependencies: {}` stays empty).
- The timer never waits for the server. Every network error is ignored.
- The stored trial value never goes down, on the device or on the server.
- Code comments and docs use the project's plain, short English style.
- Commits: Conventional Commits, stage files by path, end with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## File Map

| File | Job |
| --- | --- |
| `trial-api/handler.mjs` (new) | Input checks, rate limit, SQL call, answer. No I/O of its own. |
| `trial-api/index.mjs` (new) | Real `sql()` over Neon HTTP, wires the handler. Build entry. |
| `trial-api/test.mjs` (new) | `node:test` tests of the handler with a fake `sql`. |
| `trial-api/package.json` (new) | build + test scripts. |
| `trial-api/schema.sql` (new) | `trial_usage` table. |
| `shared/src/main/java/com/cronorep/TrialMath.kt` (new) | Pure functions: merge, hash, JSON body, JSON parse. |
| `shared/src/main/java/com/cronorep/TrialSync.kt` (new) | Background POST, result on the main thread. |
| `shared/src/main/java/com/cronorep/Trial.kt` | New `merge()`. |
| `shared/build.gradle.kts`, `gradle.properties` | `TRIAL_API_URL` build field. |
| `app/.../MainActivity.kt`, `mobile/.../MainActivity.kt` | Call the sync. |
| `apple/Watch/TrialKeychain.swift` (new), `apple/Watch/RestTimer.swift` | Keychain store. |
| `docs/store/privacy-policy.html`, `docs/store/play-listing.md`, `README.md`, `README.es.md` | True text. |

---

### Task 1: Server `trial-api` with tests

**Files:**
- Create: `trial-api/handler.mjs`, `trial-api/index.mjs`, `trial-api/test.mjs`, `trial-api/package.json`, `trial-api/schema.sql`
- Modify: `.gitignore` (add `trial-api/node_modules/`, `trial-api/dist/`, `trial-api/function.zip` only if `git check-ignore` shows they are not ignored)

**Interfaces:**
- Produces: HTTP API. `POST /` body `{"id":"<64 hex>","platform":"wear"|"phone","usedMs":<int>}` → `200 {"ok":true,"usedMs":<int>}`; errors `400 {"ok":false,"error":"json"|"id"|"platform"|"usedMs"}`, `429 {"ok":false,"error":"rate"}`, `500 {"ok":false,"error":"server"}`, `405`. `GET /` → `200 {"ok":true,"service":"cronorep-trial"}`.

- [ ] **Step 1: Write `trial-api/package.json`**

```json
{
  "name": "cronorep-trial-api",
  "private": true,
  "type": "module",
  "scripts": {
    "build": "esbuild index.mjs --bundle --minify --platform=node --target=node24 --format=esm --outfile=dist/index.mjs",
    "test": "node --test test.mjs"
  },
  "dependencies": {},
  "devDependencies": {
    "esbuild": "^0.24.2"
  }
}
```

- [ ] **Step 2: Write the failing tests `trial-api/test.mjs`**

```js
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { makeHandler, makeLimiter, LIMIT_MS } from './handler.mjs';

const ID = 'a'.repeat(64);

function post(body, ip = '1.2.3.4') {
  return new Request('https://x/', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'x-forwarded-for': ip },
    body: typeof body === 'string' ? body : JSON.stringify(body),
  });
}

function fakeSql(storedValue) {
  const calls = [];
  const sql = async (query, params) => {
    calls.push({ query, params });
    return { rows: [{ used_ms: String(storedValue ?? params[2]) }] };
  };
  return { sql, calls };
}

const noLimit = () => false;

test('GET is a health check', async () => {
  const h = makeHandler({ sql: fakeSql().sql, limited: noLimit });
  const res = await h(new Request('https://x/', { method: 'GET' }));
  assert.equal(res.status, 200);
  assert.deepEqual(await res.json(), { ok: true, service: 'cronorep-trial' });
});

test('stores the value and returns the stored one', async () => {
  const { sql, calls } = fakeSql(9000);
  const h = makeHandler({ sql, limited: noLimit });
  const res = await h(post({ id: ID, platform: 'wear', usedMs: 5000 }));
  assert.equal(res.status, 200);
  assert.deepEqual(await res.json(), { ok: true, usedMs: 9000 });
  assert.deepEqual(calls[0].params, [ID, 'wear', 5000]);
  assert.match(calls[0].query, /GREATEST\(trial_usage\.used_ms, EXCLUDED\.used_ms\)/);
});

test('cuts a value above the limit', async () => {
  const { sql, calls } = fakeSql();
  const h = makeHandler({ sql, limited: noLimit });
  const res = await h(post({ id: ID, platform: 'phone', usedMs: LIMIT_MS + 1 }));
  assert.equal(res.status, 200);
  assert.equal(calls[0].params[2], LIMIT_MS);
});

test('rejects bad input', async () => {
  const h = makeHandler({ sql: fakeSql().sql, limited: noLimit });
  const cases = [
    ['not json', 'json'],
    [{ id: 'ABC', platform: 'wear', usedMs: 1 }, 'id'],
    [{ id: 'A'.repeat(64), platform: 'wear', usedMs: 1 }, 'id'],
    [{ id: ID, platform: 'ios', usedMs: 1 }, 'platform'],
    [{ id: ID, platform: 'wear', usedMs: -1 }, 'usedMs'],
    [{ id: ID, platform: 'wear', usedMs: 1.5 }, 'usedMs'],
    [{ id: ID, platform: 'wear', usedMs: '5' }, 'usedMs'],
  ];
  for (const [body, error] of cases) {
    const res = await h(post(body));
    assert.equal(res.status, 400, `case ${JSON.stringify(body)}`);
    assert.deepEqual(await res.json(), { ok: false, error });
  }
});

test('rate limit answers 429', async () => {
  const h = makeHandler({ sql: fakeSql().sql, limited: () => true });
  const res = await h(post({ id: ID, platform: 'wear', usedMs: 1 }));
  assert.equal(res.status, 429);
});

test('database error answers 500', async () => {
  const sql = async () => { throw new Error('down'); };
  const h = makeHandler({ sql, limited: noLimit });
  const res = await h(post({ id: ID, platform: 'wear', usedMs: 1 }));
  assert.equal(res.status, 500);
  assert.deepEqual(await res.json(), { ok: false, error: 'server' });
});

test('other methods answer 405', async () => {
  const h = makeHandler({ sql: fakeSql().sql, limited: noLimit });
  const res = await h(new Request('https://x/', { method: 'PUT' }));
  assert.equal(res.status, 405);
});

test('limiter allows 30 posts per window, then blocks', () => {
  const limited = makeLimiter(30, 600_000);
  for (let i = 0; i < 30; i++) assert.equal(limited('ip'), false);
  assert.equal(limited('ip'), true);
  assert.equal(limited('other'), false);
});
```

- [ ] **Step 3: Run the tests, see them fail**

Run: `cd trial-api && node --test test.mjs`
Expected: FAIL, `Cannot find module ... handler.mjs`.

- [ ] **Step 4: Write `trial-api/handler.mjs`**

```js
// CronoRep trial API logic. index.mjs gives it the real database.
// POST / {id: 64 hex, platform: "wear"|"phone", usedMs: int} -> 200 {ok:true, usedMs}
// The stored value only goes up, so a client cannot lower it.
export const LIMIT_MS = 10 * 60 * 60 * 1000;

const ID_RE = /^[0-9a-f]{64}$/;
const PLATFORMS = new Set(['wear', 'phone']);

/** In-memory limit per IP: `max` posts per `windowMs`. */
export function makeLimiter(max = 30, windowMs = 600_000) {
  const hits = new Map();
  return function limited(ip) {
    const now = Date.now();
    const list = (hits.get(ip) || []).filter((t) => now - t < windowMs);
    list.push(now);
    hits.set(ip, list);
    if (hits.size > 5000) hits.clear();
    return list.length > max;
  };
}

function json(status, body) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

export function makeHandler({ sql, limited }) {
  return async function handler(request) {
    if (request.method === 'GET') return json(200, { ok: true, service: 'cronorep-trial' });
    if (request.method !== 'POST') return json(405, { ok: false, error: 'method' });

    const ip = (request.headers.get('x-forwarded-for') || '').split(',')[0].trim() || 'unknown';
    if (limited(ip)) return json(429, { ok: false, error: 'rate' });

    let data;
    try {
      data = await request.json();
    } catch {
      return json(400, { ok: false, error: 'json' });
    }

    const id = data?.id;
    const platform = data?.platform;
    const usedMs = data?.usedMs;
    if (typeof id !== 'string' || !ID_RE.test(id)) return json(400, { ok: false, error: 'id' });
    if (!PLATFORMS.has(platform)) return json(400, { ok: false, error: 'platform' });
    if (!Number.isInteger(usedMs) || usedMs < 0) return json(400, { ok: false, error: 'usedMs' });

    try {
      const result = await sql(
        `INSERT INTO trial_usage (trial_id, platform, used_ms) VALUES ($1, $2, $3)
         ON CONFLICT (trial_id) DO UPDATE
           SET used_ms = GREATEST(trial_usage.used_ms, EXCLUDED.used_ms), updated_at = now()
         RETURNING used_ms`,
        [id, platform, Math.min(usedMs, LIMIT_MS)],
      );
      return json(200, { ok: true, usedMs: Number(result.rows[0].used_ms) });
    } catch (err) {
      console.error('upsert failed', err?.message);
      return json(500, { ok: false, error: 'server' });
    }
  };
}
```

- [ ] **Step 5: Write `trial-api/index.mjs`**

```js
// CronoRep trial API (Neon Function). Logic is in handler.mjs.
// No dependencies: SQL goes over Neon's HTTP endpoint, so the bundle stays tiny.
import { makeHandler, makeLimiter } from './handler.mjs';

async function sql(query, params) {
  const url = process.env.DATABASE_URL;
  const host = new URL(url).hostname;
  const res = await fetch(`https://${host}/sql`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'Neon-Connection-String': url },
    body: JSON.stringify({ query, params }),
  });
  if (!res.ok) throw new Error(`sql ${res.status}: ${(await res.text()).slice(0, 200)}`);
  return res.json();
}

export default makeHandler({ sql, limited: makeLimiter(30, 600_000) });
```

- [ ] **Step 6: Write `trial-api/schema.sql`**

```sql
-- Free trial time per device. trial_id = sha256("cronorep-trial:" + ANDROID_ID), hex.
CREATE TABLE IF NOT EXISTS trial_usage (
  trial_id   text PRIMARY KEY CHECK (trial_id ~ '^[0-9a-f]{64}$'),
  platform   text NOT NULL CHECK (platform IN ('wear', 'phone')),
  used_ms    bigint NOT NULL CHECK (used_ms >= 0),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);
```

- [ ] **Step 7: Run the tests, see them pass**

Run: `cd trial-api && node --test test.mjs`
Expected: `# pass 8`, `# fail 0`.

- [ ] **Step 8: Install and build**

Run: `cd trial-api && npm install && npm run build`
Expected: `dist/index.mjs` exists. Run `git status --short trial-api` and check that `node_modules/` and `dist/` are not listed (the root `.gitignore` has `build/` only). If they are listed, add these lines to `.gitignore`:

```
trial-api/node_modules/
trial-api/dist/
trial-api/function.zip
```

- [ ] **Step 9: Commit**

```bash
git add trial-api/handler.mjs trial-api/index.mjs trial-api/test.mjs trial-api/package.json trial-api/package-lock.json trial-api/schema.sql .gitignore
git commit -m "feat: trial API that keeps the biggest used time per device"
```

(Leave `.gitignore` out of `git add` if Step 8 did not change it.)

---

### Task 2: Create the table and deploy the server

This task uses the Neon MCP tools. It changes a live cloud project, so ask the user before each write call (`run_sql`, `deploy_function`).

**Files:**
- Modify: `gradle.properties` (add `trialApiUrl=`)

**Interfaces:**
- Produces: `trialApiUrl` Gradle property with the live HTTPS URL of `trial-api`. Task 4 reads it.

- [ ] **Step 1: Find the waitlist project**

Call `mcp__neon__list_projects`, then `mcp__neon__list_functions` for each project until the waitlist function shows up. Note its `project_id`, branch, and how it is set up (runtime, env vars). Use the same project and branch.

- [ ] **Step 2: Create the table**

Call `mcp__neon__run_sql` with the full text of `trial-api/schema.sql`, on that project and branch.
Then call `mcp__neon__describe_table_schema` for `trial_usage`. Expected: the 5 columns from the schema.

- [ ] **Step 3: Deploy**

Call `mcp__neon__deploy_function` with the bundle `trial-api/dist/index.mjs`, name `cronorep-trial`, env `DATABASE_URL` set the same way as the waitlist function (read it from `mcp__neon__get_function` of the waitlist; never print the connection string in chat or in a file).

- [ ] **Step 4: Smoke test**

```bash
curl -s https://<function-url>/
curl -s -X POST https://<function-url>/ -H 'Content-Type: application/json' \
  -d '{"id":"0000000000000000000000000000000000000000000000000000000000000000","platform":"wear","usedMs":1000}'
curl -s -X POST https://<function-url>/ -H 'Content-Type: application/json' \
  -d '{"id":"0000000000000000000000000000000000000000000000000000000000000000","platform":"wear","usedMs":10}'
```

Expected: `{"ok":true,"service":"cronorep-trial"}`, then `{"ok":true,"usedMs":1000}`, then `{"ok":true,"usedMs":1000}` (it did not go down).
Then delete the test row: `mcp__neon__run_sql` with `DELETE FROM trial_usage WHERE trial_id = repeat('0', 64);`

- [ ] **Step 5: Save the URL**

Append to `gradle.properties`:

```properties
# Free trial server (trial-api/). Not a secret: the app calls it in the open.
trialApiUrl=https://<function-url>/
```

- [ ] **Step 6: Commit**

```bash
git add gradle.properties
git commit -m "chore: point the apps at the deployed trial API"
```

---

### Task 3: Pure trial functions in `shared/`

**Files:**
- Create: `shared/src/main/java/com/cronorep/TrialMath.kt`
- Modify: `shared/src/main/java/com/cronorep/Trial.kt`
- Test: `shared/src/test/java/com/cronorep/TrialMathTest.kt`

**Interfaces:**
- Produces:
  - `TrialMath.mergeUsed(local: Long, server: Long): Long`
  - `TrialMath.trialId(androidId: String): String`
  - `TrialMath.requestBody(id: String, platform: String, usedMs: Long): String`
  - `TrialMath.parseUsedMs(body: String): Long?`
  - `Trial.merge(context: Context, serverMs: Long)`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.cronorep

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TrialMathTest {

    @Test
    fun mergeKeepsTheBiggerValue() {
        assertEquals(5_000L, TrialMath.mergeUsed(5_000L, 1_000L))
        assertEquals(9_000L, TrialMath.mergeUsed(5_000L, 9_000L))
        assertEquals(0L, TrialMath.mergeUsed(0L, 0L))
    }

    @Test
    fun mergeNeverTrustsAServerValueAboveTheLimitOrBelowZero() {
        assertEquals(Trial.LIMIT_MS, TrialMath.mergeUsed(0L, Trial.LIMIT_MS * 5))
        assertEquals(3_000L, TrialMath.mergeUsed(3_000L, -50L))
    }

    @Test
    fun trialIdIsTheSha256OfThePrefixedId() {
        assertEquals(
            "40369e4366938eb4dc6a12d2848d87b50ee0d352e3e584d2be16aaaa57229e7a",
            TrialMath.trialId("abc"),
        )
        assertEquals(64, TrialMath.trialId("9774d56d682e549c").length)
    }

    @Test
    fun requestBodyIsTheServerJson() {
        assertEquals(
            """{"id":"ab","platform":"wear","usedMs":42}""",
            TrialMath.requestBody("ab", "wear", 42L),
        )
    }

    @Test
    fun parseReadsUsedMsFromAnOkAnswer() {
        assertEquals(9_000L, TrialMath.parseUsedMs("""{"ok":true,"usedMs":9000}"""))
        assertEquals(7L, TrialMath.parseUsedMs("""{ "usedMs" : 7, "ok" : true }"""))
    }

    @Test
    fun parseRejectsErrorsAndJunk() {
        assertNull(TrialMath.parseUsedMs("""{"ok":false,"error":"id"}"""))
        assertNull(TrialMath.parseUsedMs("""{"ok":true}"""))
        assertNull(TrialMath.parseUsedMs("<html>502</html>"))
        assertNull(TrialMath.parseUsedMs(""))
    }
}
```

- [ ] **Step 2: Run it, see it fail**

Run: `./gradlew :shared:testDebugUnitTest --tests com.cronorep.TrialMathTest`
Expected: FAIL, `Unresolved reference: TrialMath`.

- [ ] **Step 3: Write `TrialMath.kt`**

```kotlin
package com.cronorep

import java.security.MessageDigest

/**
 * Pure helpers for the free trial sync. No Android code here, so unit tests
 * run on the JVM. org.json is not used: it is a stub in local unit tests.
 */
object TrialMath {

    /** The used time never goes down. A server value is trusted only from 0 to the limit. */
    fun mergeUsed(local: Long, server: Long): Long =
        maxOf(local, server.coerceIn(0L, Trial.LIMIT_MS))

    /** sha256("cronorep-trial:" + ANDROID_ID) as lowercase hex. The raw ID never leaves the device. */
    fun trialId(androidId: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest("cronorep-trial:$androidId".toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    /** id is hex and platform is a fixed word, so no JSON escaping is needed. */
    fun requestBody(id: String, platform: String, usedMs: Long): String =
        """{"id":"$id","platform":"$platform","usedMs":$usedMs}"""

    private val okRe = Regex(""""ok"\s*:\s*true""")
    private val usedRe = Regex(""""usedMs"\s*:\s*(\d{1,12})""")

    /** The usedMs of a {"ok":true,...} answer, or null for anything else. */
    fun parseUsedMs(body: String): Long? {
        if (!okRe.containsMatchIn(body)) return null
        return usedRe.find(body)?.groupValues?.get(1)?.toLongOrNull()
    }
}
```

- [ ] **Step 4: Add `merge` to `Trial.kt`**

Add this function after `load()` in `shared/src/main/java/com/cronorep/Trial.kt`:

```kotlin
    /** Takes the server value when it is bigger, for example after a new install. */
    fun merge(context: Context, serverMs: Long) {
        val merged = TrialMath.mergeUsed(usedMs, serverMs)
        if (merged == usedMs) return
        usedMs = merged
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putLong(KEY_USED_MS, usedMs).apply()
    }
```

- [ ] **Step 5: Run the tests, see them pass**

Run: `./gradlew :shared:testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`, `TrialMathTest` 6 tests pass, the old tests still pass.

- [ ] **Step 6: Commit**

```bash
git add shared/src/main/java/com/cronorep/TrialMath.kt shared/src/main/java/com/cronorep/Trial.kt shared/src/test/java/com/cronorep/TrialMathTest.kt
git commit -m "feat: trial merge, device hash and JSON helpers"
```

---

### Task 4: `TrialSync` and the build URL

**Files:**
- Create: `shared/src/main/java/com/cronorep/TrialSync.kt`
- Modify: `shared/build.gradle.kts`

**Interfaces:**
- Consumes: `TrialMath.trialId`, `TrialMath.requestBody`, `TrialMath.parseUsedMs`, `Trial.usedMs`, Gradle property `trialApiUrl` (Task 2).
- Produces: `TrialSync.sync(context: Context, platform: String, onResult: (Long) -> Unit)` — `onResult` runs on the main thread, only on success.

- [ ] **Step 1: Add the build field in `shared/build.gradle.kts`**

Change the `defaultConfig` block and add `buildFeatures` inside `android { }`:

```kotlin
    defaultConfig {
        minSdk = 26
        // The free trial server (trial-api/). Set in gradle.properties.
        val trialApiUrl = providers.gradleProperty("trialApiUrl").get()
        buildConfigField("String", "TRIAL_API_URL", "\"$trialApiUrl\"")
    }

    buildFeatures {
        buildConfig = true
    }
```

- [ ] **Step 2: Write `TrialSync.kt`**

```kotlin
package com.cronorep

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import com.cronorep.shared.BuildConfig
import java.net.HttpURLConnection
import java.net.URL

/**
 * Sends the used trial time to the trial server and gets the biggest value it
 * has for this device. A new install of the app gets the old used time back.
 * Every error is ignored: the timer never waits for the network.
 */
object TrialSync {

    private const val TIMEOUT_MS = 5_000

    /** onResult runs on the main thread, and only when the server answered. */
    fun sync(context: Context, platform: String, onResult: (Long) -> Unit) {
        val id = deviceTrialId(context) ?: return
        val usedMs = Trial.usedMs
        val main = Handler(Looper.getMainLooper())
        Thread {
            val serverMs = post(TrialMath.requestBody(id, platform, usedMs)) ?: return@Thread
            main.post { onResult(serverMs) }
        }.start()
    }

    /** ANDROID_ID stays the same after a new install. Only its hash is sent. */
    @SuppressLint("HardwareIds")
    private fun deviceTrialId(context: Context): String? {
        val raw = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        return if (raw.isNullOrBlank()) null else TrialMath.trialId(raw)
    }

    private fun post(body: String): Long? {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(BuildConfig.TRIAL_API_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            if (connection.responseCode != 200) return null
            val answer = connection.inputStream.bufferedReader().use { it.readText() }
            TrialMath.parseUsedMs(answer)
        } catch (e: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }
}
```

- [ ] **Step 3: Build**

Run: `./gradlew :shared:assembleDebug :shared:testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add shared/build.gradle.kts shared/src/main/java/com/cronorep/TrialSync.kt
git commit -m "feat: send the used trial time to the trial server"
```

---

### Task 5: Call the sync from the watch and phone apps

**Files:**
- Modify: `app/src/main/java/com/cronorep/MainActivity.kt:69` and `:112-115`
- Modify: `mobile/src/main/java/com/cronorep/mobile/MainActivity.kt:45`, `:28` (import) and `:98-101`

**Interfaces:**
- Consumes: `TrialSync.sync(context, platform, onResult)`, `Trial.merge(context, serverMs)`.

- [ ] **Step 1: Watch app `onCreate`**

In `app/src/main/java/com/cronorep/MainActivity.kt`, after `Trial.load(this)`:

```kotlin
        Trial.load(this)
        // A new install gets the used trial time back from the server.
        TrialSync.sync(this, "wear") { Trial.merge(this, it) }
```

- [ ] **Step 2: Watch app `onPause`**

```kotlin
    override fun onPause() {
        Trial.stopCounting(this)
        TrialSync.sync(this, "wear") { Trial.merge(this, it) }
        super.onPause()
    }
```

(`TrialSync` is in package `com.cronorep`, the same package as this file, so no import.)

- [ ] **Step 3: Phone app**

In `mobile/src/main/java/com/cronorep/mobile/MainActivity.kt` add the import after `import com.cronorep.Trial`:

```kotlin
import com.cronorep.TrialSync
```

After `Trial.load(this)`:

```kotlin
        Trial.load(this)
        // A new install gets the used trial time back from the server.
        TrialSync.sync(this, "phone") { Trial.merge(this, it) }
```

And `onPause`:

```kotlin
    override fun onPause() {
        Trial.stopCounting(this)
        TrialSync.sync(this, "phone") { Trial.merge(this, it) }
        super.onPause()
    }
```

- [ ] **Step 4: Build both apps and run lint**

Run: `./gradlew :app:assembleDebug :mobile:assembleDebug :app:lintRelease :mobile:lintRelease :shared:testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`, no new lint errors.

- [ ] **Step 5: Quick check on a device (debug build)**

Install the phone debug build on the Xiaomi, open it, wait 5 s, then:
`mcp__neon__run_sql`: `SELECT platform, used_ms, updated_at FROM trial_usage ORDER BY updated_at DESC LIMIT 3;`
Expected: a fresh `phone` row.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/cronorep/MainActivity.kt mobile/src/main/java/com/cronorep/mobile/MainActivity.kt
git commit -m "feat: sync the free trial on start and when the app leaves the screen"
```

---

### Task 6: Apple Watch Keychain

There is no Mac here. The check is the Codemagic build (`codemagic.yaml`) and a later test on a real Apple Watch.

**Files:**
- Create: `apple/Watch/TrialKeychain.swift`
- Modify: `apple/Watch/RestTimer.swift:41` and `:166`

**Interfaces:**
- Produces: `TrialKeychain.read() -> Double?`, `TrialKeychain.write(_ value: Double)`.

- [ ] **Step 1: Check that the project picks up new files**

Run: `grep -n "sources" -A3 apple/project.yml`
Expected: the `Watch` folder is a source path, so a new `.swift` file in it is built with no project change. If files are listed one by one, add `TrialKeychain.swift` there.

- [ ] **Step 2: Write `apple/Watch/TrialKeychain.swift`**

```swift
import Foundation
import Security

/// Keeps the used free trial time in the Keychain. Keychain items can stay
/// after the app is deleted, while UserDefaults are always deleted.
enum TrialKeychain {
    private static let service = "com.cronorep.trial"
    private static let account = "trial_used_seconds"

    private static var baseQuery: [String: Any] {
        [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
        ]
    }

    static func read() -> Double? {
        var query = baseQuery
        query[kSecReturnData as String] = true
        query[kSecMatchLimit as String] = kSecMatchLimitOne
        var item: CFTypeRef?
        guard SecItemCopyMatching(query as CFDictionary, &item) == errSecSuccess,
              let data = item as? Data,
              let text = String(data: data, encoding: .utf8) else { return nil }
        return Double(text)
    }

    static func write(_ value: Double) {
        let data = Data(String(value).utf8)
        let update = [kSecValueData as String: data]
        let status = SecItemUpdate(baseQuery as CFDictionary, update as CFDictionary)
        guard status == errSecItemNotFound else { return }
        var add = baseQuery
        add[kSecValueData as String] = data
        add[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlock
        SecItemAdd(add as CFDictionary, nil)
    }
}
```

- [ ] **Step 3: Read from the Keychain in `RestTimer.init`**

Replace line 41 of `apple/Watch/RestTimer.swift`:

```swift
        trialUsed = UserDefaults.standard.double(forKey: Keys.trialUsed)
```

with:

```swift
        // The bigger value wins: the Keychain survives a new install, and old
        // versions saved only to UserDefaults.
        let used = max(TrialKeychain.read() ?? 0, UserDefaults.standard.double(forKey: Keys.trialUsed))
        trialUsed = used
        TrialKeychain.write(used)
```

- [ ] **Step 4: Write to the Keychain in `flushTrial`**

After `defaults.set(trialUsed, forKey: Keys.trialUsed)` (line 166):

```swift
        defaults.set(trialUsed, forKey: Keys.trialUsed)
        TrialKeychain.write(trialUsed)
```

- [ ] **Step 5: Commit**

```bash
git add apple/Watch/TrialKeychain.swift apple/Watch/RestTimer.swift
git commit -m "feat: keep the Apple Watch trial time in the Keychain"
```

(Also add `apple/project.yml` if Step 1 changed it.)

---

### Task 7: True privacy and store text

**Files:**
- Modify: `docs/store/privacy-policy.html:15-23`
- Modify: `docs/store/play-listing.md:80-87`
- Modify: `README.md` and `README.es.md` (module table near line 86)

- [ ] **Step 1: Privacy policy**

In `docs/store/privacy-policy.html` set the date line to `<p>Last updated: 14 November 2026</p>` (keep it if it already says that) and replace the two sections "Data we collect" and "Data that stays on your devices" with:

```html
<h2>Data we collect</h2>
<p>We do not sell data. The app has no account, no analytics and no advertising.</p>
<p>The Android apps send two things to our server: a scrambled device ID and the free trial time you have used. The scrambled ID is made from the Android device ID with a one-way function (SHA-256), so we cannot see the real device ID. We use this data only so that deleting and installing the app again does not restart the free trial. It contains no name, email or account. It is sent over an encrypted connection (HTTPS). We keep it while the app is offered. To ask us to delete it, email us at the address on the store listing.</p>
<p>The Apple Watch app sends no data to us.</p>

<h2>Data that stays on your devices</h2>
<p>The app saves your rest time, your settings, the free trial time you have used and your unlock status on your devices. On Apple Watch, the free trial time is kept in the Keychain, so it can stay after you delete the app.</p>
```

- [ ] **Step 2: Play listing**

Line 43 of `docs/store/play-listing.md` ("Your settings stay on your devices") stays: it is still true. Replace the "Data safety form" section (lines 80-87) with:

```markdown
## Data safety form

- Does the app collect or share user data: Yes, it collects. It does not share.
- Data type: Device or other IDs (a SHA-256 hash of ANDROID_ID).
  - Collected, not shared. Processed: not ephemeral. Required, not optional.
  - Purpose: Fraud prevention, security, and compliance (stops a reset of the free trial).
- Data type: App activity → Other actions (the used free trial time).
  - Collected, not shared. Required. Purpose: App functionality, Fraud prevention.
- Data is encrypted in transit: Yes (HTTPS).
- Users can ask for deletion: Yes, by email to the developer address.
- The app has no account, analytics or ads SDK.
- Google Play Billing is handled by Google.
- GIF downloads go from the watch or the phone to GIPHY. The developer receives no data.
- The look settings go from the phone to the user's own watch through the Wearable
  Data Layer. They are not collected: they do not leave the user's devices.
```

- [ ] **Step 3: README module tables**

In `README.md`, under the row `| \`waitlist-api/\` | Waitlist API (Neon Function) |` add:

```markdown
| `trial-api/` | Free trial API (Neon Function): keeps the used trial time per device |
```

In `README.es.md`, find the same table row for `waitlist-api/` and add under it:

```markdown
| `trial-api/` | API de la prueba gratis (Neon Function): guarda el tiempo de prueba usado por dispositivo |
```

Also search both READMEs for "no data" / "sin datos" / "stays on" claims: `grep -n -i "no data\|stays on\|sin datos\|no sale" README.md README.es.md` and make each one true with the same facts as Step 1.

- [ ] **Step 4: Commit**

```bash
git add docs/store/privacy-policy.html docs/store/play-listing.md README.md README.es.md
git commit -m "docs: say that the trial time and a hashed device ID go to our server"
```

---

### Task 8: Version bump, release build and the real reinstall test

**Files:**
- Modify: `app/build.gradle.kts:24-25`, `mobile/build.gradle.kts:26-27`

- [ ] **Step 1: Bump versions**

`app/build.gradle.kts`: `versionCode = 4`, `versionName = "1.0.1"`.
`mobile/build.gradle.kts`: `versionCode = 1002`, `versionName = "1.0.1"`.

- [ ] **Step 2: Release builds**

Run: `./gradlew :app:bundleRelease :mobile:bundleRelease :app:assembleRelease :mobile:assembleRelease`
Expected: `BUILD SUCCESSFUL`. The compliance guard hook may run on these commands; its known `ANDROID-USER-DATA-DISCLOSURE` finding is the one the user approved before. Any other critical finding: stop and report it.

- [ ] **Step 3: Reinstall test on the phone (release APK, Xiaomi 2407FPN8EG)**

```bash
adb -s <phone> install -r mobile/build/outputs/apk/release/mobile-release.apk
```

1. Open the app, start the timer, keep it on screen for 3 minutes, go home. Note "Free trial: X left" in settings.
2. `adb -s <phone> uninstall com.cronorep` then install the APK again.
3. Open the app online, wait 5 s, open settings. Expected: the same "X left" (±1 min), not "10 h 0 min".
4. Uninstall, turn on airplane mode, install, open. Expected: "10 h 0 min". Turn airplane mode off, leave and open the app again. Expected: back to "X left".

- [ ] **Step 4: Same test on the watch (Galaxy Watch SM-R920)**

Same steps with `app/build/outputs/apk/release/app-release.apk` and the watch's adb serial.

- [ ] **Step 5: Commit**

```bash
git add app/build.gradle.kts mobile/build.gradle.kts
git commit -m "chore: version 1.0.1 with the reinstall-proof trial"
```

- [ ] **Step 6: Tell the user what is left for them**

1. Upload `mobile/build/outputs/bundle/release/mobile-release.aab` (1002) and `app/build/outputs/bundle/release/app-release.aab` (4).
2. Update the Play Console Data safety form with the text in `docs/store/play-listing.md` (give it as a browser-extension task prompt, per the user's global rules).
3. Publish the new privacy policy at `enautomatico.online/cronorep/privacy`.
4. Test the Apple Watch Keychain on a real watch when one is available.
