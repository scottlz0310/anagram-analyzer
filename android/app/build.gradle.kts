import org.gradle.api.GradleException
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.anagram.analyzer"
    compileSdk = 36
    val releaseStoreFilePath =
        providers.gradleProperty("ANDROID_SIGNING_STORE_FILE").orNull
            ?: providers.environmentVariable("ANDROID_SIGNING_STORE_FILE").orNull
    val releaseStorePassword =
        providers.gradleProperty("ANDROID_SIGNING_STORE_PASSWORD").orNull
            ?: providers.environmentVariable("ANDROID_SIGNING_STORE_PASSWORD").orNull
    val releaseKeyAlias =
        providers.gradleProperty("ANDROID_SIGNING_KEY_ALIAS").orNull
            ?: providers.environmentVariable("ANDROID_SIGNING_KEY_ALIAS").orNull
    val releaseKeyPassword =
        providers.gradleProperty("ANDROID_SIGNING_KEY_PASSWORD").orNull
            ?: providers.environmentVariable("ANDROID_SIGNING_KEY_PASSWORD").orNull
    val signingConfigValues =
        mapOf(
            "ANDROID_SIGNING_STORE_FILE" to releaseStoreFilePath,
            "ANDROID_SIGNING_STORE_PASSWORD" to releaseStorePassword,
            "ANDROID_SIGNING_KEY_ALIAS" to releaseKeyAlias,
            "ANDROID_SIGNING_KEY_PASSWORD" to releaseKeyPassword,
        )
    val hasAnySigningInput = signingConfigValues.values.any { !it.isNullOrBlank() }
    if (hasAnySigningInput) {
        val missingSigningKeys = signingConfigValues.filterValues { it.isNullOrBlank() }.keys
        if (missingSigningKeys.isNotEmpty()) {
            throw GradleException(
                "release署名設定が不完全です。未設定: ${missingSigningKeys.joinToString()}",
            )
        }
    }
    val releaseStoreFile = releaseStoreFilePath?.takeIf { it.isNotBlank() }?.let(::file)
    val hasReleaseSigning =
        releaseStoreFile != null &&
            !releaseStorePassword.isNullOrBlank() &&
            !releaseKeyAlias.isNullOrBlank() &&
            !releaseKeyPassword.isNullOrBlank()
    if (hasReleaseSigning && !requireNotNull(releaseStoreFile).exists()) {
        throw GradleException(
            "ANDROID_SIGNING_STORE_FILE が見つかりません: ${releaseStoreFile.absolutePath}",
        )
    }

    defaultConfig {
        applicationId = "com.anagram.analyzer"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = requireNotNull(releaseStoreFile)
                storePassword = requireNotNull(releaseStorePassword)
                keyAlias = requireNotNull(releaseKeyAlias)
                keyPassword = requireNotNull(releaseKeyPassword)
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.02.00")
    val roomVersion = "2.8.4"

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.12.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.hilt:hilt-navigation-compose:1.3.0")
    implementation("androidx.datastore:datastore-preferences:1.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")
    implementation("com.google.dagger:hilt-android:2.59.2")
    ksp("com.google.dagger:hilt-compiler:2.59.2")
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")

    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
