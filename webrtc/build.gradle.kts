plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

dependencies { implementation(libs.webrtc); implementation(project(":core:model")) }

android { namespace = "com.tecladop2pv2.webrtc"; compileSdk = 35; defaultConfig { minSdk = 26 } }
