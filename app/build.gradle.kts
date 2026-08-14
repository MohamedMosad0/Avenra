import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.net.URI

val releaseApiBaseUrl = providers.gradleProperty("AVENRA_RELEASE_BASE_URL")
    .orElse(providers.environmentVariable("AVENRA_RELEASE_BASE_URL"))
    .orNull

val releaseStoreFile = providers.gradleProperty("AVENRA_RELEASE_STORE_FILE")
    .orElse(providers.environmentVariable("AVENRA_RELEASE_STORE_FILE"))
    .orNull
val releaseStorePassword = providers.gradleProperty("AVENRA_RELEASE_STORE_PASSWORD")
    .orElse(providers.environmentVariable("AVENRA_RELEASE_STORE_PASSWORD"))
    .orNull
val releaseKeyAlias = providers.gradleProperty("AVENRA_RELEASE_KEY_ALIAS")
    .orElse(providers.environmentVariable("AVENRA_RELEASE_KEY_ALIAS"))
    .orNull
val releaseKeyPassword = providers.gradleProperty("AVENRA_RELEASE_KEY_PASSWORD")
    .orElse(providers.environmentVariable("AVENRA_RELEASE_KEY_PASSWORD"))
    .orNull
val hasReleaseSigningCredentials = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword
).all { !it.isNullOrBlank() }

fun isValidReleaseApiBaseUrl(value: String?): Boolean = runCatching {
    val uri = URI(value)
    uri.scheme.equals("https", ignoreCase = true) &&
        !uri.host.isNullOrBlank() &&
        !uri.host.equals("localhost", ignoreCase = true) &&
        !uri.host.endsWith(".localhost", ignoreCase = true) &&
        !uri.host.endsWith(".invalid", ignoreCase = true)
}.getOrDefault(false)

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.avenra.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.avenra.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

    }

    signingConfigs {
        create("release") {
            if (hasReleaseSigningCredentials) {
                storeFile = file(requireNotNull(releaseStoreFile))
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            // Local development uses ADB reverse to the HTTP backend.
            buildConfigField("String", "BASE_URL", "\"http://localhost:3000/\"")
        }
        release {
            // Release packaging is blocked below until this is a configured public HTTPS URL.
            buildConfigField("String", "BASE_URL", "\"${releaseApiBaseUrl.orEmpty()}\"")
            if (hasReleaseSigningCredentials) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
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
}

val verifyReleaseConfiguration by tasks.registering {
    group = "verification"
    description = "Verifies the production API URL and release signing inputs before packaging."
    doLast {
        check(isValidReleaseApiBaseUrl(releaseApiBaseUrl)) {
            "Release requires AVENRA_RELEASE_BASE_URL to be a public HTTPS URL (not localhost or .invalid)."
        }
        check(hasReleaseSigningCredentials) {
            "Release requires AVENRA_RELEASE_STORE_FILE, AVENRA_RELEASE_STORE_PASSWORD, " +
                "AVENRA_RELEASE_KEY_ALIAS, and AVENRA_RELEASE_KEY_PASSWORD."
        }
        check(file(requireNotNull(releaseStoreFile)).isFile) {
            "Release keystore file does not exist: $releaseStoreFile"
        }
    }
}

tasks.matching { it.name == "assembleRelease" || it.name == "bundleRelease" }.configureEach {
    dependsOn(verifyReleaseConfiguration)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging)
    implementation(libs.coil.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.security.crypto)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
