plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.budgetbuddy"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.budgetbuddy"
        minSdk = 27
        targetSdk = 36
        versionCode = 19
        versionName = "3.0-stable"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // Obfuscate and trim production builds so implementation details and unused
            // transitive code are not shipped unnecessarily.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.glide.core)
    annotationProcessor(libs.glide.compiler)
    // The recognition model ships in the APK so receipt OCR works without a download.
    implementation(libs.mlkit.text.recognition)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    //MP Android Chart dependencies
    implementation(libs.mpandroidchart)

    implementation(libs.speedviewlib)
}
