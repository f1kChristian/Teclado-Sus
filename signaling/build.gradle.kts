plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

dependencies { implementation(project(":core:model")); implementation(libs.coroutines.android) }

android { namespace = "com.tecladop2pv2.signaling"; compileSdk = 35; defaultConfig { minSdk = 26 } }
