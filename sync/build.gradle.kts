plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

dependencies { implementation(project(":core:model")); implementation(project(":core:database")); implementation(libs.work.runtime) }

android { namespace = "com.tecladop2pv2.sync"; compileSdk = 35; defaultConfig { minSdk = 26 } }
