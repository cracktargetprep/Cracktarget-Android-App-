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
        versionCode = 4
        versionName = "1.4.0"
        buildConfigField("boolean", "PLAY_DISTRIBUTION", "false")
    }
    buildFeatures { buildConfig = true }
    buildTypes {
        getByName("release") {
            buildConfigField("boolean", "PLAY_DISTRIBUTION", "true")
        }
        create("playQa") {
            initWith(getByName("debug"))
            matchingFallbacks += listOf("debug")
            buildConfigField("boolean", "PLAY_DISTRIBUTION", "true")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.activity:activity:1.10.1")
    implementation("androidx.core:core:1.16.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.webkit:webkit:1.12.1")
    testImplementation("junit:junit:4.13.2")
}
