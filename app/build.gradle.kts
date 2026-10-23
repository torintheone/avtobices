plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Версия приложения приходит из git-тега: CI передаёт -PappVersion=0.1.0 (тег v0.1.0).
// Без тега (локальная сборка, обычный push) — 0.0.0-dev.
val appVersion: String = providers.gradleProperty("appVersion").orNull
    ?.takeIf { it.isNotBlank() } ?: "0.0.0-dev"

// versionCode растёт с каждым релизом: 1 + major·10000 + minor·100 + patch.
// v0.0.0 → 1, v0.1.0 → 101, v1.2.3 → 10204.
fun versionCodeOf(version: String): Int {
    val parts = version.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 } + listOf(0, 0, 0)
    return 1 + parts[0] * 10_000 + parts[1] * 100 + parts[2]
}

// Постоянный ключ подписи: CI раскладывает секреты GitHub в переменные окружения.
// Нет ключа — release подписывается debug-ключом (с предупреждением в логе сборки).
val releaseStoreFile: File? = providers.environmentVariable("SIGNING_STORE_FILE").orNull
    ?.let { file(it) }
    ?.takeIf { it.exists() }

android {
    namespace = "app.steamgarage"
    compileSdk = 37

    defaultConfig {
        applicationId = "app.steamgarage"
        minSdk = 26
        targetSdk = 37
        versionCode = versionCodeOf(appVersion)
        versionName = appVersion
    }

    signingConfigs {
        if (releaseStoreFile != null) {
            create("release") {
                storeFile = releaseStoreFile
                storePassword = providers.environmentVariable("SIGNING_STORE_PASSWORD").orNull
                keyAlias = providers.environmentVariable("SIGNING_KEY_ALIAS").orNull
                keyPassword = providers.environmentVariable("SIGNING_KEY_PASSWORD").orNull
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = if (releaseStoreFile != null) {
                signingConfigs.getByName("release")
            } else {
                logger.warn("ВНИМАНИЕ: секреты подписи не заданы — release-APK будет подписан debug-ключом.")
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}
