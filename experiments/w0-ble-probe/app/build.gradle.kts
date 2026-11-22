plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "io.github.aidarnig.wristporter.w0"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.aidarnig.wristporter.w0.inspector"
        minSdk = 30 // EXPERIMENTAL build floor; W0 does not decide Bridge minSdk
        targetSdk = 35
        versionCode = 4
        versionName = "0.0.4-inspector"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
