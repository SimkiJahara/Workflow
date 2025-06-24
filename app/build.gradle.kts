
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.0"
}

android {
    namespace = "com.simki.workflowapp"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.simki.workflowapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
<<<<<<< Updated upstream

=======
>>>>>>> Stashed changes
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
<<<<<<< Updated upstream
=======
    composeOptions {
        kotlinCompilerExtensionVersion = "2.0.21"
<<<<<<< Updated upstream
=======
    }
    externalNativeBuild {
        cmake {
            var cppFlags = "-std=c++17"
            path = file("D:/llm/llama.cpp/android/CMakeLists.txt")
            version = "3.22.1"
        }
>>>>>>> Stashed changes
    }
    externalNativeBuild {
        cmake {
            var cppFlags = "-std=c++17"
            path = file("D:/llm/llama.cpp/android/CMakeLists.txt")
            version = "3.22.1"
        }
    }
>>>>>>> Stashed changes
}

dependencies {
    // Core and Lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
<<<<<<< Updated upstream
<<<<<<< Updated upstream
=======
=======
>>>>>>> Stashed changes

    // Compose
    implementation(platform(libs.androidx.compose.bom))
>>>>>>> Stashed changes
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
<<<<<<< Updated upstream
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation(platform("androidx.compose:compose-bom:2024.10.00"))
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.material:material-icons-extended:1.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.compose.material3:material3:1.3.0")
=======
    implementation(libs.compose.material.icons.extended)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Firebase and Google Sign-In
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.play.services.auth)

    // Testing
<<<<<<< Updated upstream
>>>>>>> Stashed changes
=======
>>>>>>> Stashed changes
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
<<<<<<< Updated upstream
<<<<<<< Updated upstream
}





=======
}
>>>>>>> Stashed changes
=======
}
>>>>>>> Stashed changes
