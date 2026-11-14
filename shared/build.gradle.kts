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
        // The free trial server (trial-api/). Set in gradle.properties.
        val trialApiUrl = providers.gradleProperty("trialApiUrl").get()
        buildConfigField("String", "TRIAL_API_URL", "\"$trialApiUrl\"")
    }

    buildFeatures {
        buildConfig = true
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
