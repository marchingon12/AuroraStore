/*
 * Aurora Store
 *  Copyright (C) 2021, Rahul Kumar Patel <whyorean@gmail.com>
 *  Copyright (C) 2022, The Calyx Institute
 *  Copyright (C) 2023, grrfe <grrfe@420blaze.it>
 *
 *  Aurora Store is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  Aurora Store is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Aurora Store.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.parcelize")
    id("com.google.devtools.ksp")
    id("androidx.navigation.safeargs.kotlin")
    id("org.jlleitschuh.gradle.ktlint")
    id("dev.rikka.tools.refine")
    id("com.google.dagger.hilt.android")
}

kotlin {
    jvmToolchain(17)
}

android {
    namespace = "com.aurora.store"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.aurora.store"
        minSdk = 21
        targetSdk = 33

        versionCode = 53
        versionName = "4.3.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (File("signing.properties").exists()) {
            create("release") {
                val properties = Properties().apply {
                    File("signing.properties").inputStream().use { load(it) }
                }

                keyAlias = properties["KEY_ALIAS"] as String
                keyPassword = properties["KEY_PASSWORD"] as String
                storeFile = file(properties["STORE_FILE"] as String)
                storePassword = properties["KEY_PASSWORD"] as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (File("signing.properties").exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }

        register("nightly") {
            initWith(getByName("release"))
            applicationIdSuffix = ".nightly"
        }

        debug {
            applicationIdSuffix = ".debug"
        }
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
        aidl = true
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        lintConfig = file("lint.xml")
    }

    androidResources {
        generateLocaleConfig = true
    }
}

dependencies {

    //Protobuf
    implementation("com.google.protobuf:protobuf-javalite:3.25.1")

    //Google's Goodies
    implementation("com.google.android.material:material:1.10.0")
    implementation("com.google.code.gson:gson:2.10.1")

    //AndroidX
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    implementation("androidx.vectordrawable:vectordrawable:1.1.0")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    //Arch LifeCycle
    val lifeVersion = "2.6.2"
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifeVersion")
    implementation("androidx.lifecycle:lifecycle-service:$lifeVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifeVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-savedstate:$lifeVersion")
    implementation("androidx.lifecycle:lifecycle-process:$lifeVersion")

    //Arch Navigation
    val navVersion = "2.7.5"
    implementation("androidx.navigation:navigation-fragment-ktx:$navVersion")
    implementation("androidx.navigation:navigation-ui-ktx:$navVersion")

    //Coil
    implementation("io.coil-kt:coil:2.5.0")

    //Shimmer
    implementation("com.facebook.shimmer:shimmer:0.5.0")

    //Epoxy
    val epoxyVersion = "5.1.3"
    implementation("com.airbnb.android:epoxy:$epoxyVersion")
    ksp("com.airbnb.android:epoxy-processor:$epoxyVersion")

    //HTTP Clients
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    //Fetch - Downloader
    implementation("androidx.tonyodev.fetch2:xfetch2:3.1.6")

    //EventBus
    implementation("org.greenrobot:eventbus:3.3.1")

    //Lib-SU
    implementation("com.github.topjohnwu.libsu:core:5.0.5")

    //Love <3
    implementation("com.gitlab.AuroraOSS:gplayapi:3.2.5")

    //Browser
    implementation("androidx.browser:browser:1.7.0")

    //Shizuku
    val shizukuVersion = "13.1.5"
    compileOnly("dev.rikka.hidden:stub:4.3.2")
    implementation("dev.rikka.tools.refine:runtime:4.4.0")
    implementation("dev.rikka.shizuku:api:${shizukuVersion}")
    implementation("dev.rikka.shizuku:provider:${shizukuVersion}")

    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:4.3")

    //Test
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    //WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    //Hilt
    val hiltVersion = "2.49"
    ksp("com.google.dagger:hilt-android-compiler:$hiltVersion")
    implementation("com.google.dagger:hilt-android:$hiltVersion")

    // LeakCanary
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.12")
}
