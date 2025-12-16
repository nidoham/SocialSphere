plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
    id("kotlin-parcelize")  // ✅ রাখুন data class এর জন্য
}

android {
    namespace = "com.nidoham.socialsphere"
    compileSdk = 36  // ✅ Simplified syntax

    defaultConfig {
        applicationId = "com.nidoham.socialsphere"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
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
        debug {
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17  // ✅ Kotlin 2.2.21 এর জন্য 17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)  // ✅ Modern Kotlin syntax
    }

    buildFeatures {
        compose = true
        buildConfig = true  // ✅ BuildConfig access
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.20"  // ✅ Latest stable
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core Android (Kotlin-First)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)

    // Compose BOM (Version management)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.coil.compose)

    // ViewModel & Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Material (Optional - Material3 দিয়ে replace করুন)
    implementation(libs.material)

    // Firebase BOM + KTX (Kotlin optimized)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)  // ✅ KTX version
    implementation(libs.firebase.firestore.ktx)  // ✅ KTX version
    implementation(libs.firebase.database.ktx)  // ✅ Fixed duplicate

    // Kotlin Coroutines & Serialization
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.serialization.json)  // ✅ JSON serialization
    implementation(libs.kotlinx.datetime)  // ✅ Date handling

    // Auth & Credentials
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.play.services.auth)
    implementation(libs.okhttp)

    // NewPipe (Conflict resolved)
    implementation(libs.newpipeextractor) {
        exclude(group = "com.google.protobuf", module = "protobuf-javalite")
        exclude(group = "com.google.code.findbugs", module = "jsr305")
    }

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.compose.ui.test.manifest)

    // Debug only
    debugImplementation(libs.androidx.compose.ui.tooling)
}
