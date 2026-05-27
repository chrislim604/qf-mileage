plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "ca.quantumform.mileage"
    compileSdk = 36

    defaultConfig {
        applicationId = "ca.quantumform.mileage.free"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.4.1"
    }

    flavorDimensions += "store"
    productFlavors {
        create("free") {
            dimension = "store"
            applicationId = "ca.quantumform.mileage.free"
            manifestPlaceholders["launcherName"] = "QF Mileage"
        }
        create("paid") {
            dimension = "store"
            applicationId = "ca.quantumform.mileage.paid"
            manifestPlaceholders["launcherName"] = "QF Mileage"
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

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
}
