plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

dependencies {
    implementation(project(":core:model"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.datastore.preferences)
    implementation(libs.coroutines.android)
}

android { namespace = "com.tecladop2pv2.feature.customization"; compileSdk = 35; defaultConfig { minSdk = 26 } }
