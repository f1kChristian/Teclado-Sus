plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.tecladop2pv2.webrtc"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }
}

dependencies {
    implementation(libs.webrtc)
    implementation(project(":core:model"))
    implementation(libs.coroutines.android)
}
