plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.comphone.appmonitor" // Change this if your actual namespace is different
    compileSdk = 35 // Example: Use your current compileSdk version

    defaultConfig {
        applicationId = "com.comphone.appmonitor" // Change this if your actual application ID is different
        minSdk = 24 // Example: Use your current minSdk version
        targetSdk = 35 // Example: Use your current targetSdk version
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    // THIS IS THE IMPORTANT PART FOR VIEW BINDING
    buildFeatures {
        viewBinding = true // Correct Kotlin DSL syntax: use '=' for assignment
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0") // או גרסה יציבה עדכנית יותר
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0") // או גרסה יציבה עדכנית יותר
    implementation("androidx.legacy:legacy-support-v4:1.0.0") // או גרסה עדכנית יותר, 1.0.0 מספיק

    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation (libs.github.glide)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.recyclerview)
    annotationProcessor (libs.compiler)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
