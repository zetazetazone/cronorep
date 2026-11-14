# CronoRep Phone Companion App Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a phone app (same package `com.cronorep`) that is a full rest timer and that controls the look of the watch timer, and add minimalist mode, save battery mode and background choices to the watch app.

**Architecture:** A new Android library `:shared` holds the timer core (moved from `:app`) and the look settings. The Wear OS app `:app` and the new phone app `:mobile` both use it. The phone sends the look settings to the watch through the Wearable Data Layer at path `/look`.

**Tech Stack:** Kotlin 2.2.20, AGP 8.12.0, Jetpack Compose (BOM 2025.05.00), Wear Compose 1.4.1, Material 3, Coil 2.7.0, Play Billing 8.0.0, play-services-wearable 19.0.0, JUnit 4.

Spec: `docs/superpowers/specs/2026-11-14-companion-app-design.md`

## Global Constraints

- Package name of both apps: `applicationId = "com.cronorep"`. Do not change it.
- `:mobile`: namespace `com.cronorep.mobile`, `minSdk = 26`, `targetSdk = 35`, `compileSdk = 36`, `versionCode = 1001`, `versionName = "1.0.0"`.
- `:app` (watch): `versionCode = 3`. All other values stay.
- `:shared`: namespace `com.cronorep.shared`, `minSdk = 26`. Kotlin package of all its files: `com.cronorep`.
- SharedPreferences file name: `cronorep`. Data Layer path: `/look`.
- Colours: green `0xFF4ADE80`, orange `0xFFFB923C`, dim `0xFF9CA3AF`.
- All user text is short plain English, the same style as the watch app.
- No analytics, no ads, no account, no new network calls except GIPHY images.
- Run Gradle from the repo root in the Bash tool: `./gradlew <tasks>`. `JAVA_HOME` is already set.
- Do NOT run release build tasks (`bundleRelease`, `assembleRelease`). Debug builds only.
- Never stage `keystore.properties`, `keystore/`, `local.properties`, `*.jks`, `*.aab`, `*.apk`. Stage files by exact path. Never `git add .` or `git add -A`.
- Commit messages: Conventional Commits, subject ≤ 72 characters, and this last line:
  `Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>`
- Do not push.

## File Structure

| File | Responsibility |
| --- | --- |
| `shared/build.gradle.kts` | Library build file |
| `shared/src/main/AndroidManifest.xml` | Empty library manifest |
| `shared/src/main/java/com/cronorep/LookSettings.kt` | Look model, fallbacks, background cycle (pure Kotlin) |
| `shared/src/main/java/com/cronorep/Gradients.kt` | The 6 gradients |
| `shared/src/main/java/com/cronorep/GifCatalog.kt` | GIPHY ids and URLs |
| `shared/src/main/java/com/cronorep/LookStore.kt` | Look settings in SharedPreferences |
| `shared/src/main/java/com/cronorep/LookSync.kt` | Data Layer path, DataMap converters, send |
| `shared/src/main/java/com/cronorep/Look.kt` | Current look as Compose state, current GIF URL |
| `shared/src/main/java/com/cronorep/{RestTimer,RestTimerService,Trial,Billing}.kt` | Timer core, moved from `:app` |
| `shared/src/main/res/drawable/ic_launcher.xml` | Icon, moved from `:app` |
| `shared/src/test/java/com/cronorep/LookSettingsTest.kt` | Unit tests |
| `app/src/main/java/com/cronorep/MainActivity.kt` | Watch UI (modified) |
| `app/src/main/java/com/cronorep/LookListenerService.kt` | Watch: receives look settings |
| `mobile/build.gradle.kts`, `mobile/src/main/AndroidManifest.xml`, `mobile/src/main/res/values/strings.xml` | Phone app build files |
| `mobile/src/main/java/com/cronorep/mobile/Ui.kt` | Colours, privacy URL |
| `mobile/src/main/java/com/cronorep/mobile/MainActivity.kt` | Phone activity, screen switch, timer start |
| `mobile/src/main/java/com/cronorep/mobile/TimerScreen.kt` | Phone timer screen, ring, backgrounds |
| `mobile/src/main/java/com/cronorep/mobile/SettingsScreen.kt` | Phone settings screen |
| `mobile/src/main/java/com/cronorep/mobile/PaywallScreen.kt` | Phone paywall |
| `mobile/src/main/java/com/cronorep/mobile/WatchLink.kt` | Watch connection status and send |

---

### Task 1: `:shared` module with the look model

**Files:**
- Modify: `settings.gradle.kts`, `build.gradle.kts`
- Create: `shared/build.gradle.kts`, `shared/src/main/AndroidManifest.xml`
- Create: `shared/src/main/java/com/cronorep/LookSettings.kt`, `Gradients.kt`, `GifCatalog.kt`
- Test: `shared/src/test/java/com/cronorep/LookSettingsTest.kt`

**Interfaces:**
- Produces: `enum class BackgroundKind { BLACK, GRADIENT, GIF_RANDOM, GIF_FIXED }`;
  `data class LookSettings(minimalist: Boolean, saveBattery: Boolean, background: BackgroundKind, gradientId: Int, gifId: String?)` with `effectiveBackground: BackgroundKind`, `withNextBackground(): LookSettings`, and
  `LookSettings.fromRaw(minimalist: Boolean?, saveBattery: Boolean?, background: String?, gradientId: Int?, gifId: String?, legacyGifsEnabled: Boolean = false): LookSettings`;
  `data class Gradient(val name: String, val top: Long, val bottom: Long)`, `Gradients.ALL: List<Gradient>` (6 items);
  `GifCatalog.IDS: List<String>`, `GifCatalog.url(id: String): String`, `GifCatalog.thumbUrl(id: String): String`.

- [ ] **Step 1: Add the module to Gradle**

In `settings.gradle.kts` replace `include(":app")` with:

```kotlin
include(":app")
include(":shared")
```

In `build.gradle.kts` (root) add one line in the `plugins` block:

```kotlin
    id("com.android.library") version "8.12.0" apply false
```

Create `shared/build.gradle.kts`:

```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.cronorep.shared"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    api(platform("androidx.compose:compose-bom:2025.05.00"))
    api("androidx.compose.runtime:runtime")
    api("com.android.billingclient:billing:8.0.0")
    api("com.google.android.gms:play-services-wearable:19.0.0")
    testImplementation("junit:junit:4.13.2")
}
```

Create `shared/src/main/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest />
```

- [ ] **Step 2: Write the failing test**

Create `shared/src/test/java/com/cronorep/LookSettingsTest.kt`:

```kotlin
package com.cronorep

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LookSettingsTest {
    private val knownGif = GifCatalog.IDS.first()

    @Test
    fun nullValuesGiveTheDefaults() {
        assertEquals(LookSettings(), LookSettings.fromRaw(null, null, null, null, null))
    }

    @Test
    fun oldGifSwitchBecomesRandomGif() {
        val look = LookSettings.fromRaw(null, null, null, null, null, legacyGifsEnabled = true)
        assertEquals(BackgroundKind.GIF_RANDOM, look.background)
    }

    @Test
    fun savedBackgroundWinsOverTheOldGifSwitch() {
        val look = LookSettings.fromRaw(null, null, "BLACK", null, null, legacyGifsEnabled = true)
        assertEquals(BackgroundKind.BLACK, look.background)
    }

    @Test
    fun unknownBackgroundNameGivesBlack() {
        assertEquals(BackgroundKind.BLACK, LookSettings.fromRaw(null, null, "PLASMA", null, null).background)
    }

    @Test
    fun gradientOutOfRangeGivesZero() {
        assertEquals(0, LookSettings.fromRaw(null, null, "GRADIENT", 99, null).gradientId)
        assertEquals(0, LookSettings.fromRaw(null, null, "GRADIENT", -1, null).gradientId)
        assertEquals(5, LookSettings.fromRaw(null, null, "GRADIENT", 5, null).gradientId)
    }

    @Test
    fun unknownGifIdIsDroppedAndFixedFallsBackToRandom() {
        val look = LookSettings.fromRaw(null, null, "GIF_FIXED", null, "not-a-real-id")
        assertNull(look.gifId)
        assertEquals(BackgroundKind.GIF_RANDOM, look.effectiveBackground)
    }

    @Test
    fun knownGifIdIsKept() {
        val look = LookSettings.fromRaw(true, false, "GIF_FIXED", 2, knownGif)
        assertEquals(LookSettings(true, false, BackgroundKind.GIF_FIXED, 2, knownGif), look)
        assertEquals(BackgroundKind.GIF_FIXED, look.effectiveBackground)
    }

    @Test
    fun saveBatteryForcesBlack() {
        val look = LookSettings(saveBattery = true, background = BackgroundKind.GRADIENT)
        assertEquals(BackgroundKind.BLACK, look.effectiveBackground)
        assertEquals(BackgroundKind.GRADIENT, look.background)
    }

    @Test
    fun nextBackgroundSkipsFixedGifWhenNoGifIsChosen() {
        var look = LookSettings()
        look = look.withNextBackground(); assertEquals(BackgroundKind.GRADIENT, look.background)
        look = look.withNextBackground(); assertEquals(BackgroundKind.GIF_RANDOM, look.background)
        look = look.withNextBackground(); assertEquals(BackgroundKind.BLACK, look.background)
    }

    @Test
    fun nextBackgroundIncludesFixedGifWhenAGifIsChosen() {
        var look = LookSettings(background = BackgroundKind.GIF_RANDOM, gifId = knownGif)
        look = look.withNextBackground(); assertEquals(BackgroundKind.GIF_FIXED, look.background)
        look = look.withNextBackground(); assertEquals(BackgroundKind.BLACK, look.background)
    }

    @Test
    fun catalogsAreSane() {
        assertEquals(6, Gradients.ALL.size)
        assertTrue(GifCatalog.IDS.isNotEmpty())
        assertEquals(GifCatalog.IDS.size, GifCatalog.IDS.toSet().size)
        assertEquals("https://media.giphy.com/media/$knownGif/200.gif", GifCatalog.url(knownGif))
        assertEquals("https://media.giphy.com/media/$knownGif/100_s.gif", GifCatalog.thumbUrl(knownGif))
    }
}
```

- [ ] **Step 3: Run the test to verify it fails**

Run: `./gradlew :shared:testDebugUnitTest`
Expected: FAIL at compile, "Unresolved reference: LookSettings" (and `GifCatalog`, `Gradients`).

- [ ] **Step 4: Write the implementation**

Create `shared/src/main/java/com/cronorep/Gradients.kt`:

```kotlin
package com.cronorep

/** A still background. [top] and [bottom] are ARGB colours, for example 0xFFF97316. */
data class Gradient(val name: String, val top: Long, val bottom: Long)

object Gradients {
    val ALL = listOf(
        Gradient("Ember", 0xFFF97316, 0xFF7F1D1D),
        Gradient("Ocean", 0xFF06B6D4, 0xFF1E3A8A),
        Gradient("Forest", 0xFF4ADE80, 0xFF064E3B),
        Gradient("Violet", 0xFFA855F7, 0xFF312E81),
        Gradient("Steel", 0xFF94A3B8, 0xFF0F172A),
        Gradient("Sunset", 0xFFF472B6, 0xFF9A3412),
    )
}
```

Create `shared/src/main/java/com/cronorep/GifCatalog.kt`. Copy every id line of the
`GIF_IDS` list from `app/src/main/java/com/cronorep/Gifs.kt` (the quoted strings between
`private val GIF_IDS = listOf(` and its closing `)`) into `IDS`, unchanged and in the same
order. Do not change `Gifs.kt` in this task.

```kotlin
package com.cronorep

/** The gym GIFs. The app owner chose this list. No API call is needed. */
object GifCatalog {
    /** GIPHY ids. */
    val IDS = listOf(
        // <-- the id lines from Gifs.kt go here, for example:
        // "HSBsApUGF4erUp7NHV",
    )

    /** 200 px high: sharp enough for the timer background and a small download. */
    fun url(id: String) = "https://media.giphy.com/media/$id/200.gif"

    /** 100 px high still image for the chooser grid. */
    fun thumbUrl(id: String) = "https://media.giphy.com/media/$id/100_s.gif"
}
```

Create `shared/src/main/java/com/cronorep/LookSettings.kt`:

```kotlin
package com.cronorep

enum class BackgroundKind { BLACK, GRADIENT, GIF_RANDOM, GIF_FIXED }

/** How the timer looks. The phone and the watch use the same model. */
data class LookSettings(
    /** No progress ring and no "total" line. */
    val minimalist: Boolean = false,
    /** Black background and one screen update each second. */
    val saveBattery: Boolean = false,
    val background: BackgroundKind = BackgroundKind.BLACK,
    /** Index in [Gradients.ALL]. */
    val gradientId: Int = 0,
    /** GIPHY id for [BackgroundKind.GIF_FIXED]. Null = no GIF chosen. */
    val gifId: String? = null,
) {
    /** The background to draw. */
    val effectiveBackground: BackgroundKind
        get() = when {
            saveBattery -> BackgroundKind.BLACK
            background == BackgroundKind.GIF_FIXED && gifId == null -> BackgroundKind.GIF_RANDOM
            else -> background
        }

    /** For the one-button background control on the watch. */
    fun withNextBackground(): LookSettings = copy(
        background = when (background) {
            BackgroundKind.BLACK -> BackgroundKind.GRADIENT
            BackgroundKind.GRADIENT -> BackgroundKind.GIF_RANDOM
            BackgroundKind.GIF_RANDOM -> if (gifId != null) BackgroundKind.GIF_FIXED else BackgroundKind.BLACK
            BackgroundKind.GIF_FIXED -> BackgroundKind.BLACK
        }
    )

    companion object {
        /**
         * Builds settings from stored or received values. Bad values fall back to the
         * defaults. [legacyGifsEnabled] is the old "GIF on" switch: it applies only
         * when no background is saved.
         */
        fun fromRaw(
            minimalist: Boolean?,
            saveBattery: Boolean?,
            background: String?,
            gradientId: Int?,
            gifId: String?,
            legacyGifsEnabled: Boolean = false,
        ): LookSettings = LookSettings(
            minimalist = minimalist ?: false,
            saveBattery = saveBattery ?: false,
            background = when {
                background != null ->
                    BackgroundKind.entries.firstOrNull { it.name == background } ?: BackgroundKind.BLACK
                legacyGifsEnabled -> BackgroundKind.GIF_RANDOM
                else -> BackgroundKind.BLACK
            },
            gradientId = gradientId?.takeIf { it in Gradients.ALL.indices } ?: 0,
            gifId = gifId?.takeIf { it in GifCatalog.IDS },
        )
    }
}
```

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./gradlew :shared:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, 11 tests pass.

- [ ] **Step 6: Commit**

```bash
git add settings.gradle.kts build.gradle.kts shared/build.gradle.kts shared/src/main/AndroidManifest.xml shared/src/main/java/com/cronorep/LookSettings.kt shared/src/main/java/com/cronorep/Gradients.kt shared/src/main/java/com/cronorep/GifCatalog.kt shared/src/test/java/com/cronorep/LookSettingsTest.kt
git commit -m "feat: add shared module with the look settings model"
```

---

### Task 2: Move the timer core to `:shared`

No behaviour change. The watch app must build and work as before.

**Files:**
- Move: `app/src/main/java/com/cronorep/{RestTimer,RestTimerService,Trial,Billing}.kt` → `shared/src/main/java/com/cronorep/`
- Move: `app/src/main/res/drawable/ic_launcher.xml` → `shared/src/main/res/drawable/ic_launcher.xml`
- Modify: `shared/src/main/java/com/cronorep/RestTimerService.kt`, `app/build.gradle.kts`

**Interfaces:**
- Consumes: the `:shared` module from Task 1.
- Produces (unchanged public API, now in `:shared`, package `com.cronorep`): `RestTimer` (`startedAt`, `runSeconds`, `restSeconds`, `load`, `changeRest`, `setRest`, `PRESETS_SECONDS`, `STEP_SECONDS`), `formatTime(Long): String`, `RestTimerService.start(context, delayMs)`, `RestTimerService.stop(context)`, `Trial` (`load`, `startCounting`, `flush`, `stopCounting`, `expired`, `remainingMinutes`, `remainingText`), `Billing` (`init`, `refresh`, `buy`, `unlocked`, `priceText`, `message`). Resource `@drawable/ic_launcher` now comes from `:shared`.

- [ ] **Step 1: Move the files with git**

```bash
mkdir -p shared/src/main/res/drawable
git mv app/src/main/java/com/cronorep/RestTimer.kt shared/src/main/java/com/cronorep/RestTimer.kt
git mv app/src/main/java/com/cronorep/RestTimerService.kt shared/src/main/java/com/cronorep/RestTimerService.kt
git mv app/src/main/java/com/cronorep/Trial.kt shared/src/main/java/com/cronorep/Trial.kt
git mv app/src/main/java/com/cronorep/Billing.kt shared/src/main/java/com/cronorep/Billing.kt
git mv app/src/main/res/drawable/ic_launcher.xml shared/src/main/res/drawable/ic_launcher.xml
```

- [ ] **Step 2: Make the service independent of the app module**

In `shared/src/main/java/com/cronorep/RestTimerService.kt`:

Add this import (keep the imports sorted):

```kotlin
import com.cronorep.shared.R
```

Change the class comment's word "watch" to "device":

```kotlin
/**
 * Runs during the countdown only. It holds a wake lock so the device
 * cannot sleep through the end of the rest time, then it vibrates and stops.
 */
```

Replace:

```kotlin
        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
```

with:

```kotlin
        // The launch activity of this app: the watch app or the phone app.
        val openApp = PendingIntent.getActivity(
            this, 0, packageManager.getLaunchIntentForPackage(packageName), PendingIntent.FLAG_IMMUTABLE
        )
```

- [ ] **Step 3: Make the watch app use `:shared`**

In `app/build.gradle.kts`, in `dependencies`, remove the line
`implementation("com.android.billingclient:billing:8.0.0")` and add as the first line:

```kotlin
    implementation(project(":shared"))
```

- [ ] **Step 4: Build to verify**

Run: `./gradlew :shared:testDebugUnitTest :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/build.gradle.kts shared/src/main/java/com/cronorep/RestTimer.kt shared/src/main/java/com/cronorep/RestTimerService.kt shared/src/main/java/com/cronorep/Trial.kt shared/src/main/java/com/cronorep/Billing.kt shared/src/main/res/drawable/ic_launcher.xml
git commit -m "refactor: move the timer core to the shared module"
```

(`git mv` already staged the removals in `app/`. Check with `git status --short` that no
`app/.../RestTimer.kt` etc. shows as unstaged.)

---

### Task 3: Look features on the watch

**Files:**
- Create: `shared/src/main/java/com/cronorep/LookStore.kt`, `LookSync.kt`, `Look.kt`
- Create: `app/src/main/java/com/cronorep/LookListenerService.kt`
- Delete: `app/src/main/java/com/cronorep/Gifs.kt`
- Modify: `app/src/main/java/com/cronorep/MainActivity.kt`, `app/src/main/AndroidManifest.xml`, `app/build.gradle.kts`

**Interfaces:**
- Consumes: `LookSettings`, `BackgroundKind`, `Gradients`, `GifCatalog` (Task 1).
- Produces:
  - `LookStore.load(context: Context): LookSettings`, `LookStore.save(context: Context, settings: LookSettings)`
  - `LookSync.PATH = "/look"`, `LookSync.write(settings: LookSettings, map: DataMap)`, `LookSync.fromDataMap(map: DataMap): LookSettings`, `LookSync.send(context: Context, settings: LookSettings): Task<DataItem>`
  - `Look.settings: LookSettings` (Compose state, read only), `Look.gifUrl: String?` (Compose state), `Look.load(context: Context)`, `Look.update(context: Context, newSettings: LookSettings)`, `Look.onTimerStart()`

- [ ] **Step 1: Create `shared/src/main/java/com/cronorep/LookStore.kt`**

```kotlin
package com.cronorep

import android.content.Context

/** Keeps the look settings on the device. */
object LookStore {
    private const val PREFS = "cronorep"
    private const val KEY_MINIMALIST = "look_minimalist"
    private const val KEY_SAVE_BATTERY = "look_save_battery"
    private const val KEY_BACKGROUND = "look_background"
    private const val KEY_GRADIENT = "look_gradient"
    private const val KEY_GIF_ID = "look_gif_id"

    /** The old "GIF on" switch from version 1.0.0 of the watch app. */
    private const val KEY_LEGACY_GIFS = "gifs_enabled"

    fun load(context: Context): LookSettings {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return LookSettings.fromRaw(
            minimalist = prefs.getBoolean(KEY_MINIMALIST, false),
            saveBattery = prefs.getBoolean(KEY_SAVE_BATTERY, false),
            background = prefs.getString(KEY_BACKGROUND, null),
            gradientId = prefs.getInt(KEY_GRADIENT, 0),
            gifId = prefs.getString(KEY_GIF_ID, null),
            legacyGifsEnabled = prefs.getBoolean(KEY_LEGACY_GIFS, false),
        )
    }

    fun save(context: Context, settings: LookSettings) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_MINIMALIST, settings.minimalist)
            .putBoolean(KEY_SAVE_BATTERY, settings.saveBattery)
            .putString(KEY_BACKGROUND, settings.background.name)
            .putInt(KEY_GRADIENT, settings.gradientId)
            .putString(KEY_GIF_ID, settings.gifId)
            .apply()
    }
}
```

- [ ] **Step 2: Create `shared/src/main/java/com/cronorep/LookSync.kt`**

```kotlin
package com.cronorep

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

/** Sends the look settings from the phone to the watch through the Data Layer. */
object LookSync {
    const val PATH = "/look"

    private const val KEY_MINIMALIST = "minimalist"
    private const val KEY_SAVE_BATTERY = "save_battery"
    private const val KEY_BACKGROUND = "background"
    private const val KEY_GRADIENT = "gradient"
    private const val KEY_GIF_ID = "gif_id"

    fun write(settings: LookSettings, map: DataMap) {
        map.putBoolean(KEY_MINIMALIST, settings.minimalist)
        map.putBoolean(KEY_SAVE_BATTERY, settings.saveBattery)
        map.putString(KEY_BACKGROUND, settings.background.name)
        map.putInt(KEY_GRADIENT, settings.gradientId)
        // The Data Layer has no null strings: empty = no GIF chosen.
        map.putString(KEY_GIF_ID, settings.gifId ?: "")
    }

    fun fromDataMap(map: DataMap): LookSettings = LookSettings.fromRaw(
        minimalist = if (map.containsKey(KEY_MINIMALIST)) map.getBoolean(KEY_MINIMALIST) else null,
        saveBattery = if (map.containsKey(KEY_SAVE_BATTERY)) map.getBoolean(KEY_SAVE_BATTERY) else null,
        background = map.getString(KEY_BACKGROUND),
        gradientId = if (map.containsKey(KEY_GRADIENT)) map.getInt(KEY_GRADIENT) else null,
        gifId = map.getString(KEY_GIF_ID),
    )

    /** The Data Layer keeps the item and delivers it when a watch connects. */
    fun send(context: Context, settings: LookSettings): Task<DataItem> {
        val request = PutDataMapRequest.create(PATH)
            .apply { write(settings, dataMap) }
            .asPutDataRequest()
            .setUrgent()
        return Wearable.getDataClient(context).putDataItem(request)
    }
}
```

- [ ] **Step 3: Create `shared/src/main/java/com/cronorep/Look.kt`**

```kotlin
package com.cronorep

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** The current look of the timer, for the whole app. */
object Look {
    var settings by mutableStateOf(LookSettings())
        private set

    /** URL of the GIF to show. Null = no GIF. */
    var gifUrl by mutableStateOf<String?>(null)
        private set

    private var gifId: String? = null

    fun load(context: Context) {
        apply(LookStore.load(context))
    }

    fun update(context: Context, newSettings: LookSettings) {
        LookStore.save(context, newSettings)
        apply(newSettings)
    }

    /** Call at each timer start. Random mode then shows a different GIF. */
    fun onTimerStart() {
        if (settings.effectiveBackground == BackgroundKind.GIF_RANDOM) showRandomGif()
    }

    private fun apply(newSettings: LookSettings) {
        settings = newSettings
        when (newSettings.effectiveBackground) {
            // Show a GIF at once. The user must not wait for the next tap.
            BackgroundKind.GIF_RANDOM -> if (gifId == null) showRandomGif()
            BackgroundKind.GIF_FIXED -> showGif(newSettings.gifId ?: return)
            BackgroundKind.BLACK, BackgroundKind.GRADIENT -> {
                gifId = null
                gifUrl = null
            }
        }
    }

    /** Picks a random GIF that is not the current one. */
    private fun showRandomGif() = showGif(GifCatalog.IDS.filter { it != gifId }.random())

    private fun showGif(id: String) {
        gifId = id
        gifUrl = GifCatalog.url(id)
    }
}
```

- [ ] **Step 4: Create `app/src/main/java/com/cronorep/LookListenerService.kt`**

```kotlin
package com.cronorep

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService

/** Receives the look settings from the phone app, also when the watch app is closed. */
class LookListenerService : WearableListenerService() {
    override fun onDataChanged(events: DataEventBuffer) {
        for (event in events) {
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == LookSync.PATH) {
                Look.update(this, LookSync.fromDataMap(DataMapItem.fromDataItem(event.dataItem).dataMap))
            }
        }
    }
}
```

In `app/src/main/AndroidManifest.xml`, add inside `<application>`, after the `RestTimerService` block:

```xml

        <!-- Receives the look settings from the optional phone app. -->
        <service
            android:name=".LookListenerService"
            android:exported="true">
            <intent-filter>
                <action android:name="com.google.android.gms.wearable.DATA_CHANGED" />
                <data
                    android:host="*"
                    android:path="/look"
                    android:scheme="wear" />
            </intent-filter>
        </service>
```

- [ ] **Step 5: Delete `Gifs.kt` and raise the version code**

```bash
git rm app/src/main/java/com/cronorep/Gifs.kt
```

In `app/build.gradle.kts` change `versionCode = 2` to `versionCode = 3`.

- [ ] **Step 6: Update the watch UI in `app/src/main/java/com/cronorep/MainActivity.kt`**

a) Add this import next to the other `androidx.compose.ui.graphics` import:

```kotlin
import androidx.compose.ui.graphics.Brush
```

b) In `onCreate`, replace `Gifs.load(this)` with `Look.load(this)`.

c) In `onCreate`, replace:

```kotlin
                    SettingsScreen(
                        onDone = { showSettings = false },
                        onGifToggle = { Gifs.toggle(this) },
                    )
```

with:

```kotlin
                    SettingsScreen(onDone = { showSettings = false })
```

d) In `restartTimer`, replace `if (Gifs.enabled) Gifs.showNext()` with `Look.onTimerStart()`.

e) In `TimerScreen`, replace:

```kotlin
    val startedAt = RestTimer.startedAt
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }

    LaunchedEffect(startedAt) {
        while (startedAt != 0L) {
            now = SystemClock.elapsedRealtime()
            delay(100)
        }
    }
```

with:

```kotlin
    val startedAt = RestTimer.startedAt
    val look = Look.settings
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }

    LaunchedEffect(startedAt, look.saveBattery) {
        while (startedAt != 0L) {
            now = SystemClock.elapsedRealtime()
            // Save battery mode: one screen update each second.
            delay(if (look.saveBattery) 1000L else 100L)
        }
    }
```

f) In `TimerScreen`, replace:

```kotlin
        // Behind the idle screen and the running timer.
        Gifs.currentUrl?.let { gifUrl -> GifBackground(gifUrl) }
```

with:

```kotlin
        // Behind the idle screen and the running timer.
        when (look.effectiveBackground) {
            BackgroundKind.GRADIENT -> GradientBackground(Gradients.ALL[look.gradientId])
            BackgroundKind.GIF_RANDOM, BackgroundKind.GIF_FIXED -> Look.gifUrl?.let { GifBackground(it) }
            BackgroundKind.BLACK -> Unit
        }
```

g) In `TimerScreen`, wrap the progress ring. Replace:

```kotlin
            CircularProgressIndicator(
                progress = if (resting) 1f - elapsedMs.toFloat() / totalMs else 1f,
                modifier = Modifier.fillMaxSize().padding(4.dp),
                indicatorColor = if (resting) Green else Orange,
                trackColor = Color(0xFF1F2937),
                strokeWidth = 6.dp,
            )
```

with:

```kotlin
            if (!look.minimalist) {
                CircularProgressIndicator(
                    progress = if (resting) 1f - elapsedMs.toFloat() / totalMs else 1f,
                    modifier = Modifier.fillMaxSize().padding(4.dp),
                    indicatorColor = if (resting) Green else Orange,
                    trackColor = Color(0xFF1F2937),
                    strokeWidth = 6.dp,
                )
            }
```

h) In `TimerScreen`, replace:

```kotlin
                Text("total " + formatTime(elapsedMs / 1000), color = Dim, fontSize = 13.sp)
```

with:

```kotlin
                if (!look.minimalist) {
                    Text("total " + formatTime(elapsedMs / 1000), color = Dim, fontSize = 13.sp)
                }
```

i) In `GifBackground`, replace the last two lines of the function body:

```kotlin
    // Dark layer: the timer text must stay easy to read.
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)))
```

with:

```kotlin
    DarkLayer()
```

and add these two functions directly after `GifBackground`:

```kotlin
@Composable
private fun GradientBackground(gradient: Gradient) {
    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(gradient.top), Color(gradient.bottom))))
    )
    DarkLayer()
}

/** The timer text must stay easy to read on each background. */
@Composable
private fun DarkLayer() {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)))
}
```

j) In `SettingsScreen`: change the signature to
`private fun SettingsScreen(onDone: () -> Unit) {`, change the comment and page count to:

```kotlin
    // Page 0: the +/- control. Page 1 (swipe): the preset bubbles. Page 2: the look.
    // Each page fills the screen, so its content is always in the center.
    VerticalPager(
        state = rememberPagerState(pageCount = { 3 }),
```

Remove the GIF button from page 0: delete this block (the `Spacer` before it too):

```kotlin
                    Spacer(Modifier.height(5.dp))
                    Button(
                        onClick = onGifToggle,
                        modifier = Modifier.width(84.dp).height(30.dp),
                        colors = if (Gifs.enabled) {
                            ButtonDefaults.buttonColors(backgroundColor = Orange, contentColor = Color.Black)
                        } else {
                            ButtonDefaults.secondaryButtonColors()
                        },
                    ) {
                        Text(
                            if (Gifs.enabled) "GIF on" else "GIF off",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
```

Change the page switch from `if (page == 0) { ... } else { ...presets... }` to
`if (page == 0) { ... } else if (page == 1) { ...presets... } else { LookPage() }`.
The content of the page 0 and preset branches does not change.

k) Add these functions directly after `SettingsScreen`:

```kotlin
@Composable
private fun LookPage() {
    val context = LocalContext.current
    val look = Look.settings
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text("Look", color = Dim, fontSize = 13.sp)
        LookButton(if (look.minimalist) "Minimal on" else "Minimal off", look.minimalist) {
            Look.update(context, look.copy(minimalist = !look.minimalist))
        }
        LookButton(if (look.saveBattery) "Battery on" else "Battery off", look.saveBattery) {
            Look.update(context, look.copy(saveBattery = !look.saveBattery))
        }
        val backgroundName = when (look.background) {
            BackgroundKind.BLACK -> "Black"
            BackgroundKind.GRADIENT -> "Colour"
            BackgroundKind.GIF_RANDOM -> "GIF"
            BackgroundKind.GIF_FIXED -> "My GIF"
        }
        LookButton("Back: $backgroundName", look.background != BackgroundKind.BLACK) {
            Look.update(context, look.withNextBackground())
        }
    }
}

@Composable
private fun LookButton(label: String, active: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.width(120.dp).height(32.dp),
        colors = if (active) {
            ButtonDefaults.buttonColors(backgroundColor = Orange, contentColor = Color.Black)
        } else {
            ButtonDefaults.secondaryButtonColors()
        },
    ) { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
}
```

- [ ] **Step 7: Build to verify**

Run: `./gradlew :shared:testDebugUnitTest :app:assembleDebug`
Expected: BUILD SUCCESSFUL. No "Unresolved reference: Gifs".

- [ ] **Step 8: Commit**

```bash
git add shared/src/main/java/com/cronorep/LookStore.kt shared/src/main/java/com/cronorep/LookSync.kt shared/src/main/java/com/cronorep/Look.kt app/src/main/java/com/cronorep/LookListenerService.kt app/src/main/java/com/cronorep/MainActivity.kt app/src/main/AndroidManifest.xml app/build.gradle.kts
git commit -m "feat: add minimalist, save battery and background looks to the watch"
```

(`git rm` already staged the removal of `Gifs.kt`.)

---

### Task 4: Phone app with the rest timer and the paywall

**Files:**
- Modify: `settings.gradle.kts`
- Create: `mobile/build.gradle.kts`, `mobile/src/main/AndroidManifest.xml`, `mobile/src/main/res/values/strings.xml`
- Create: `mobile/src/main/java/com/cronorep/mobile/Ui.kt`, `MainActivity.kt`, `TimerScreen.kt`, `PaywallScreen.kt`, `SettingsScreen.kt` (simple first version; Task 5 replaces it)

**Interfaces:**
- Consumes: `RestTimer`, `formatTime`, `RestTimerService`, `Trial`, `Billing` (Task 2); `Look`, `LookSettings`, `BackgroundKind`, `Gradients`, `Gradient` (Tasks 1 and 3).
- Produces (package `com.cronorep.mobile`): `Green`, `Orange`, `Dim`, `Panel` colours and `PRIVACY_POLICY_URL` in `Ui.kt`; `@Composable fun TimerScreen(onTap: () -> Unit, onOpenSettings: () -> Unit)`; `@Composable fun PaywallScreen(onUnlock: () -> Unit)`; `@Composable fun SettingsScreen(onDone: () -> Unit)`.

- [ ] **Step 1: Add the module**

In `settings.gradle.kts` add after `include(":shared")`:

```kotlin
include(":mobile")
```

Create `mobile/build.gradle.kts`:

```kotlin
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// The same upload key as the watch app. Google Play needs the same key for one package.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "com.cronorep.mobile"
    compileSdk = 36

    defaultConfig {
        // The same package as the watch app: both are one Google Play listing.
        applicationId = "com.cronorep"
        minSdk = 26
        targetSdk = 35
        // Phone bundles use 1001 and up. Watch bundles use the low numbers.
        versionCode = 1001
        versionName = "1.0.0"
    }

    signingConfigs {
        if (keystoreProperties.containsKey("storeFile")) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(platform("androidx.compose:compose-bom:2025.05.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-gif:2.7.0")
}
```

Create `mobile/src/main/res/values/strings.xml`:

```xml
<resources>
    <string name="app_name">CronoRep</string>
</resources>
```

Create `mobile/src/main/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Only for the optional GIF backgrounds and Google Play Billing. -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.VIBRATE" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <application
        android:allowBackup="false"
        android:icon="@drawable/ic_launcher"
        android:label="@string/app_name"
        android:theme="@android:style/Theme.Material.NoActionBar">

        <activity
            android:name="com.cronorep.mobile.MainActivity"
            android:exported="true"
            android:launchMode="singleTask"
            android:screenOrientation="portrait">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- Keeps the countdown alive and vibrates when the screen is off. -->
        <service
            android:name="com.cronorep.RestTimerService"
            android:exported="false"
            android:foregroundServiceType="specialUse">
            <property
                android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
                android:value="Workout rest timer that vibrates when the rest time ends" />
        </service>
    </application>
</manifest>
```

- [ ] **Step 2: Create `mobile/src/main/java/com/cronorep/mobile/Ui.kt`**

```kotlin
package com.cronorep.mobile

import androidx.compose.ui.graphics.Color

val Green = Color(0xFF4ADE80)
val Orange = Color(0xFFFB923C)
val Dim = Color(0xFF9CA3AF)
val Panel = Color(0xFF111827)

/** The same address is in the Play Console listing. */
const val PRIVACY_POLICY_URL = "https://enautomatico.online/cronorep/privacy"
```

- [ ] **Step 3: Create `mobile/src/main/java/com/cronorep/mobile/TimerScreen.kt`**

```kotlin
package com.cronorep.mobile

import android.os.Build
import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.cronorep.BackgroundKind
import com.cronorep.Gradient
import com.cronorep.Gradients
import com.cronorep.Look
import com.cronorep.LookSettings
import com.cronorep.RestTimer
import com.cronorep.formatTime
import kotlinx.coroutines.delay

/** The rest timer. It works the same as the watch timer: tap anywhere to start or restart. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimerScreen(onTap: () -> Unit, onOpenSettings: () -> Unit) {
    val startedAt = RestTimer.startedAt
    val look = Look.settings
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }

    LaunchedEffect(startedAt, look.saveBattery) {
        while (startedAt != 0L) {
            now = SystemClock.elapsedRealtime()
            // Save battery mode: one screen update each second.
            delay(if (look.saveBattery) 1000L else 100L)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTap,
                onLongClick = onOpenSettings,
            ),
        contentAlignment = Alignment.Center,
    ) {
        LookBackground(look)

        if (startedAt == 0L) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("TAP TO START", color = Green, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(formatTime(RestTimer.restSeconds.toLong()), color = Color.White, fontSize = 96.sp)
                Spacer(Modifier.height(8.dp))
                Text("Hold to set time", color = Dim, fontSize = 16.sp)
            }
        } else {
            val totalMs = RestTimer.runSeconds * 1000L
            val elapsedMs = (now - startedAt).coerceAtLeast(0L)
            val resting = elapsedMs < totalMs

            if (!look.minimalist) {
                ProgressRing(
                    progress = if (resting) 1f - elapsedMs.toFloat() / totalMs else 1f,
                    color = if (resting) Green else Orange,
                    modifier = Modifier.fillMaxWidth().padding(20.dp).aspectRatio(1f),
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (resting) {
                    val remainingSeconds = (totalMs - elapsedMs + 999) / 1000
                    Text("REST", color = Green, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text(formatTime(remainingSeconds), color = Color.White, fontSize = 104.sp)
                } else {
                    Text("GO", color = Orange, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("+" + formatTime((elapsedMs - totalMs) / 1000), color = Orange, fontSize = 92.sp)
                }
                if (!look.minimalist) {
                    Text("total " + formatTime(elapsedMs / 1000), color = Dim, fontSize = 18.sp)
                }
            }
        }

        TextButton(
            onClick = onOpenSettings,
            modifier = Modifier.align(Alignment.TopEnd).safeDrawingPadding().padding(8.dp),
        ) { Text("Settings", color = Dim, fontSize = 16.sp) }
    }
}

@Composable
private fun ProgressRing(progress: Float, color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val stroke = 10.dp.toPx()
        val topLeft = Offset(stroke / 2, stroke / 2)
        val arcSize = Size(size.width - stroke, size.height - stroke)
        drawArc(Color(0xFF1F2937), -90f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
        drawArc(color, -90f, 360f * progress, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
    }
}

/** Behind the idle screen and the running timer. */
@Composable
private fun LookBackground(look: LookSettings) {
    when (look.effectiveBackground) {
        BackgroundKind.GRADIENT -> GradientBackground(Gradients.ALL[look.gradientId])
        BackgroundKind.GIF_RANDOM, BackgroundKind.GIF_FIXED -> Look.gifUrl?.let { GifBackground(it) }
        BackgroundKind.BLACK -> Unit
    }
}

@Composable
private fun GradientBackground(gradient: Gradient) {
    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(gradient.top), Color(gradient.bottom))))
    )
    DarkLayer()
}

@Composable
private fun GifBackground(url: String) {
    val context = LocalContext.current
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) add(ImageDecoderDecoder.Factory()) else add(GifDecoder.Factory())
            }
            .crossfade(true)
            .build()
    }
    AsyncImage(
        model = url,
        contentDescription = null,
        imageLoader = imageLoader,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
    )
    DarkLayer()
}

/** The timer text must stay easy to read on each background. */
@Composable
private fun DarkLayer() {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)))
}
```

- [ ] **Step 4: Create `mobile/src/main/java/com/cronorep/mobile/PaywallScreen.kt`**

```kotlin
package com.cronorep.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cronorep.Billing

@Composable
fun PaywallScreen(onUnlock: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black).safeDrawingPadding().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Free trial ended", color = Orange, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Pay one time.\nUse it forever.",
            color = Color.White,
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onUnlock,
            colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Color.Black),
        ) {
            Text("Unlock " + (Billing.priceText ?: ""), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Billing.message?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = Dim, fontSize = 14.sp, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "Privacy policy",
            color = Dim,
            fontSize = 14.sp,
            modifier = Modifier.clickable { uriHandler.openUri(PRIVACY_POLICY_URL) }.padding(8.dp),
        )
    }
}
```

- [ ] **Step 5: Create the first version of `mobile/src/main/java/com/cronorep/mobile/SettingsScreen.kt`**

Task 5 replaces this file. This version only sets the rest time, so the app is usable.

```kotlin
package com.cronorep.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cronorep.RestTimer
import com.cronorep.formatTime

@Composable
fun SettingsScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black).safeDrawingPadding().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text("Rest time", color = Dim, fontSize = 16.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { RestTimer.changeRest(context, -RestTimer.STEP_SECONDS) }) { Text("-", fontSize = 26.sp) }
            Text(
                formatTime(RestTimer.restSeconds.toLong()),
                color = Color.White,
                fontSize = 48.sp,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Button(onClick = { RestTimer.changeRest(context, RestTimer.STEP_SECONDS) }) { Text("+", fontSize = 26.sp) }
        }
        Button(
            onClick = onDone,
            colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Color.Black),
        ) { Text("Done", fontWeight = FontWeight.Bold) }
    }
}
```

- [ ] **Step 6: Create `mobile/src/main/java/com/cronorep/mobile/MainActivity.kt`**

```kotlin
package com.cronorep.mobile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.cronorep.Billing
import com.cronorep.Look
import com.cronorep.RestTimer
import com.cronorep.RestTimerService
import com.cronorep.Trial

class MainActivity : ComponentActivity() {

    /** The answer does not matter: the timer works with and without the notification. */
    private val askNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    private var askedNotifications = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // The screen stays on, so one tap always restarts the timer.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        RestTimer.load(this)
        Trial.load(this)
        Billing.init(this)
        Look.load(this)
        // Debug only: adb shell am start -n com.cronorep/com.cronorep.mobile.MainActivity --ez paywall true
        val previewPaywall = BuildConfig.DEBUG && intent.getBooleanExtra("paywall", false)

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Green,
                    onPrimary = Color.Black,
                    secondary = Orange,
                    background = Color.Black,
                    surface = Panel,
                )
            ) {
                var showSettings by rememberSaveable { mutableStateOf(false) }
                var showPaywall by rememberSaveable { mutableStateOf(previewPaywall) }
                if (showPaywall && !Billing.unlocked) {
                    BackHandler { showPaywall = false }
                    PaywallScreen(onUnlock = { Billing.buy(this) })
                } else if (showSettings) {
                    BackHandler { showSettings = false }
                    SettingsScreen(onDone = { showSettings = false })
                } else {
                    TimerScreen(
                        onTap = {
                            Trial.flush(this)
                            if (isLocked()) showPaywall = true else restartTimer()
                        },
                        onOpenSettings = {
                            Trial.flush(this)
                            showSettings = true
                        },
                    )
                }
            }
        }
    }

    /** Test builds never lock, so development is not blocked. */
    private fun isLocked() = !Billing.unlocked && Trial.expired && !BuildConfig.DEBUG

    override fun onResume() {
        super.onResume()
        Billing.refresh()
        if (RestTimer.startedAt != 0L) Trial.startCounting()
    }

    override fun onPause() {
        Trial.stopCounting(this)
        super.onPause()
    }

    override fun onDestroy() {
        if (isFinishing) {
            RestTimerService.stop(this)
            RestTimer.startedAt = 0L
        }
        super.onDestroy()
    }

    private fun restartTimer() {
        RestTimer.runSeconds = RestTimer.restSeconds
        RestTimer.startedAt = SystemClock.elapsedRealtime()
        RestTimerService.start(this, RestTimer.runSeconds * 1000L)
        Trial.startCounting()
        Look.onTimerStart()
        // Short tick: the tap worked.
        getSystemService(Vibrator::class.java)
            .vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
        askForNotificationsOnce()
    }

    /** Android 13 and later: the "Rest timer is running" notification needs a permission. */
    private fun askForNotificationsOnce() {
        if (Build.VERSION.SDK_INT < 33 || askedNotifications) return
        askedNotifications = true
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
```

- [ ] **Step 7: Build to verify**

Run: `./gradlew :mobile:assembleDebug :app:assembleDebug`
Expected: BUILD SUCCESSFUL. `mobile/build/outputs/apk/debug/mobile-debug.apk` exists.

- [ ] **Step 8: Commit**

```bash
git add settings.gradle.kts mobile/build.gradle.kts mobile/src/main/AndroidManifest.xml mobile/src/main/res/values/strings.xml mobile/src/main/java/com/cronorep/mobile/Ui.kt mobile/src/main/java/com/cronorep/mobile/MainActivity.kt mobile/src/main/java/com/cronorep/mobile/TimerScreen.kt mobile/src/main/java/com/cronorep/mobile/PaywallScreen.kt mobile/src/main/java/com/cronorep/mobile/SettingsScreen.kt
git commit -m "feat: add phone app with the rest timer and the paywall"
```

---

### Task 5: Phone settings screen with look settings and watch sync

**Files:**
- Create: `mobile/src/main/java/com/cronorep/mobile/WatchLink.kt`
- Replace: `mobile/src/main/java/com/cronorep/mobile/SettingsScreen.kt`

**Interfaces:**
- Consumes: `Look.settings`, `Look.update(context, LookSettings)`, `LookSync.send(context, LookSettings): Task<DataItem>`, `LookSettings`, `BackgroundKind`, `Gradients.ALL`, `GifCatalog.IDS`, `GifCatalog.thumbUrl(id)`, `RestTimer`, `Trial`, `Billing`, `formatTime`; `Green`, `Orange`, `Dim`, `Panel`, `PRIVACY_POLICY_URL` (Task 4).
- Produces: `WatchLink.connected: Boolean` (Compose state), `WatchLink.check(context: Context)`, `WatchLink.send(context: Context, settings: LookSettings)`; the full `SettingsScreen(onDone: () -> Unit)`.

- [ ] **Step 1: Create `mobile/src/main/java/com/cronorep/mobile/WatchLink.kt`**

```kotlin
package com.cronorep.mobile

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.cronorep.LookSettings
import com.cronorep.LookSync
import com.google.android.gms.wearable.Wearable

/** The link to the watch. A phone with no watch is normal: nothing here can fail loudly. */
object WatchLink {
    var connected by mutableStateOf(false)
        private set

    fun check(context: Context) {
        Wearable.getNodeClient(context).connectedNodes
            .addOnSuccessListener { nodes -> connected = nodes.isNotEmpty() }
            .addOnFailureListener { connected = false }
    }

    fun send(context: Context, settings: LookSettings) {
        LookSync.send(context, settings).addOnFailureListener { connected = false }
        check(context)
    }
}
```

- [ ] **Step 2: Replace `mobile/src/main/java/com/cronorep/mobile/SettingsScreen.kt` with the full screen**

```kotlin
package com.cronorep.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cronorep.BackgroundKind
import com.cronorep.Billing
import com.cronorep.GifCatalog
import com.cronorep.Gradients
import com.cronorep.Look
import com.cronorep.LookSettings
import com.cronorep.RestTimer
import com.cronorep.Trial
import com.cronorep.formatTime

/** Rest time and the look of the timer. Look changes also go to the watch. */
@Composable
fun SettingsScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val look = Look.settings
    // Save battery mode forces a black background, so the choices are off.
    val canChooseBackground = !look.saveBattery

    LaunchedEffect(Unit) { WatchLink.check(context) }

    fun change(newLook: LookSettings) {
        Look.update(context, newLook)
        WatchLink.send(context, newLook)
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier.fillMaxSize().background(Color.Black).safeDrawingPadding(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        fullRow {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Settings", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Box(Modifier.weight(1f))
                Button(
                    onClick = onDone,
                    colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Color.Black),
                ) { Text("Done", fontWeight = FontWeight.Bold) }
            }
        }

        fullRow { SectionTitle("Rest time") }
        fullRow {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = { RestTimer.changeRest(context, -RestTimer.STEP_SECONDS) },
                    colors = ButtonDefaults.buttonColors(containerColor = Panel, contentColor = Color.White),
                ) { Text("-", fontSize = 26.sp) }
                Text(
                    formatTime(RestTimer.restSeconds.toLong()),
                    color = Color.White,
                    fontSize = 44.sp,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Button(
                    onClick = { RestTimer.changeRest(context, RestTimer.STEP_SECONDS) },
                    colors = ButtonDefaults.buttonColors(containerColor = Panel, contentColor = Color.White),
                ) { Text("+", fontSize = 26.sp) }
            }
        }
        fullRow {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            ) {
                RestTimer.PRESETS_SECONDS.forEach { seconds ->
                    FilterChip(
                        selected = seconds == RestTimer.restSeconds,
                        onClick = { RestTimer.setRest(context, seconds) },
                        label = { Text(formatTime(seconds.toLong()), fontSize = 16.sp) },
                    )
                }
            }
        }

        fullRow { SectionTitle("Look") }
        fullRow {
            SwitchRow("Minimalist mode", "No progress ring. Only the time.", look.minimalist) {
                change(look.copy(minimalist = it))
            }
        }
        fullRow {
            SwitchRow("Save battery mode", "Black background. One screen update each second.", look.saveBattery) {
                change(look.copy(saveBattery = it))
            }
        }

        fullRow { SectionTitle("Background") }
        if (look.saveBattery) {
            fullRow { Text("Save battery mode is on. The background is black.", color = Orange, fontSize = 14.sp) }
        }
        fullRow {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = look.background == BackgroundKind.BLACK,
                    enabled = canChooseBackground,
                    onClick = { change(look.copy(background = BackgroundKind.BLACK)) },
                    label = { Text("Black") },
                )
                FilterChip(
                    selected = look.background == BackgroundKind.GIF_RANDOM,
                    enabled = canChooseBackground,
                    onClick = { change(look.copy(background = BackgroundKind.GIF_RANDOM)) },
                    label = { Text("Random GIF") },
                )
            }
        }
        fullRow {
            Row(
                modifier = Modifier.fillMaxWidth().alpha(if (canChooseBackground) 1f else 0.35f),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Gradients.ALL.forEachIndexed { index, gradient ->
                    val selected = look.background == BackgroundKind.GRADIENT && look.gradientId == index
                    Box(
                        Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Brush.verticalGradient(listOf(Color(gradient.top), Color(gradient.bottom))))
                            .border(3.dp, if (selected) Color.White else Color.Transparent, CircleShape)
                            .clickable(enabled = canChooseBackground) {
                                change(look.copy(background = BackgroundKind.GRADIENT, gradientId = index))
                            }
                    )
                }
            }
        }
        fullRow { Text("Or choose one GIF", color = Dim, fontSize = 14.sp) }
        items(GifCatalog.IDS) { id ->
            val selected = look.background == BackgroundKind.GIF_FIXED && look.gifId == id
            AsyncImage(
                model = GifCatalog.thumbUrl(id),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .aspectRatio(1f)
                    .alpha(if (canChooseBackground) 1f else 0.35f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Panel)
                    .border(3.dp, if (selected) Green else Color.Transparent, RoundedCornerShape(8.dp))
                    .clickable(enabled = canChooseBackground) {
                        change(look.copy(background = BackgroundKind.GIF_FIXED, gifId = id))
                    },
            )
        }

        fullRow { SectionTitle("Watch") }
        fullRow {
            Text(
                if (WatchLink.connected) {
                    "Watch connected. It uses the same look."
                } else {
                    "No watch found. Your look settings go to the watch when it connects."
                },
                color = if (WatchLink.connected) Green else Dim,
                fontSize = 14.sp,
            )
        }
        fullRow {
            Text(
                if (Billing.unlocked) {
                    "Unlocked. Thank you!"
                } else if (Trial.remainingMinutes > 0) {
                    "Free trial: ${Trial.remainingText} left"
                } else {
                    "Free trial ended"
                },
                color = Dim,
                fontSize = 14.sp,
            )
        }
        fullRow {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Privacy policy",
                    color = Dim,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable { uriHandler.openUri(PRIVACY_POLICY_URL) },
                )
                Text("GIFs by GIPHY", color = Dim, fontSize = 13.sp)
            }
        }
    }
}

/** One item that uses the full width of the grid. */
private fun LazyGridScope.fullRow(content: @Composable () -> Unit) {
    item(span = { GridItemSpan(maxLineSpan) }) { content() }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text.uppercase(),
        color = Green,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 10.dp),
    )
}

@Composable
private fun SwitchRow(title: String, note: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Panel)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 17.sp)
            Text(note, color = Dim, fontSize = 13.sp)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
```

- [ ] **Step 3: Build to verify**

Run: `./gradlew :shared:testDebugUnitTest :mobile:assembleDebug :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add mobile/src/main/java/com/cronorep/mobile/WatchLink.kt mobile/src/main/java/com/cronorep/mobile/SettingsScreen.kt
git commit -m "feat: add look settings and watch sync to the phone app"
```

---

### Task 6: Store listing and README

**Files:**
- Modify: `docs/store/play-listing.md`, `README.md`

**Interfaces:**
- Consumes: nothing from code. Facts: phone app is a full rest timer with the same 10 hour trial and the same `lifetime_unlock`; look settings are free; phone bundle `versionCode 1001`, watch bundle `versionCode 3`.

- [ ] **Step 1: Update `docs/store/play-listing.md`**

a) Replace the line `Form factor: Wear OS (watch only)` with:

```markdown
Form factors: Wear OS (module `app`, version code 3) and phone (module `mobile`, version code 1001)
```

b) Replace the first two paragraphs of the full description (from `CronoRep is a rest timer made for the gym` to `No small buttons, no menus, no phone.`) with:

```markdown
CronoRep is a rest timer made for the gym. Use it on your Wear OS watch, on your phone, or on both.

Finish your set. Tap anywhere on the screen. Rest. Your watch or phone vibrates when the rest time ends. No small buttons, no menus.
```

c) In the full description, replace the `GIF BACKGROUNDS` section with:

```markdown
MAKE IT YOURS
• Minimalist mode: no progress ring, only the time.
• Save battery mode: black background and fewer screen updates.
• Backgrounds: black, 6 colour gradients, or animated gym GIFs from GIPHY. GIFs need an internet connection.
• Set the look in the phone app. Your watch gets the same look.
```

d) Replace the line `• No account. No analytics. No ads. Your settings stay on your watch.` with:

```markdown
• No account. No analytics. No ads. Your settings stay on your devices.
```

e) Replace the line `CronoRep works alone on your watch. You do not need a phone app.` with:

```markdown
The watch app works alone. The phone app works alone. One payment unlocks both.
```

f) At the end of the section `## Notes for the reviewer (App content / testing instructions)`, add:

```markdown
- The phone app has the same timer. Tap anywhere to start. Tap "Settings" or hold the
  screen for the rest time and the look settings.
- The phone app sends only the look settings to the watch app, through the Wearable
  Data Layer. A watch is not needed to use the phone app.
- The same one-time product `lifetime_unlock` unlocks the phone app and the watch app.
```

g) At the end of the file add:

```markdown
## Upload

- Phone bundle (`mobile`, version code 1001 and up): the phone tracks. Closed testing with
  12 testers for 14 days runs on this track.
- Watch bundle (`app`, version code 3 and up): the Wear OS tracks.
- Both bundles use the package `com.cronorep` and the same upload key.
- The foreground service declaration (specialUse) is the same for the phone app.
```

- [ ] **Step 2: Update `README.md`**

a) Replace the title line `# CronoRep: gym rest timer for your watch` with `# CronoRep: gym rest timer for your watch and phone`.

b) Replace the line `Your wrist buzzes when the rest is over. No small buttons, no menus, no phone.` with:

```markdown
Your wrist buzzes when the rest is over. No small buttons, no menus.
No watch? The phone app has the same timer.
```

c) Replace the bullet `- Optional funny gym GIF backgrounds (GIPHY).` with:

```markdown
- Minimalist mode, save battery mode, and backgrounds: black, colour gradients or funny gym GIFs (GIPHY).
- The phone app sets the look for the watch too.
```

d) In the project layout table, add these two rows after the `app/` row:

```markdown
| `mobile/` | Android phone app: the same timer, and look settings for the watch |
| `shared/` | Code that the watch app and the phone app share |
```

e) Replace the line `2. Run the `app` configuration on a Wear OS emulator or watch.` with:

```markdown
2. Run the `app` configuration on a Wear OS emulator or watch, or the `mobile` configuration on a phone.
```

- [ ] **Step 3: Commit**

```bash
git add docs/store/play-listing.md README.md
git commit -m "docs: describe the phone app in the listing and the README"
```
