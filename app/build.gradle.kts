plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.cracktarget.app"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.cracktarget.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "1.3.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core:1.16.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
}
