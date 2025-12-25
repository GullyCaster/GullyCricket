/*
 * Copyright (C) 2024 GullyCaster Project
 * Licensed under GPLv3. Source: https://github.com/gullycaster/gullycaster
 * Developed for the Cricket Community of South Asia.
 */

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.gullycaster.boss"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.gullycaster.boss"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(project(":common"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.runtime)
    
    // RootEncoder for RTMP/SRT streaming
    implementation(libs.rootencoder.library)
    
    // NanoHTTPD for scoreboard web server
    implementation(libs.nanohttpd)
    
    // Media3 ExoPlayer for video playback
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.rtsp)
    implementation(libs.media3.ui)
}
