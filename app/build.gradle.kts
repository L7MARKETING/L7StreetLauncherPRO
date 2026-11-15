plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Assinatura de release opcional (via variáveis de ambiente / secrets do GitHub).
// Sem keystore, o release é assinado com a chave de debug e continua instalável.
val releaseKeystore: String? = System.getenv("L7_KEYSTORE_PATH")

android {
    namespace = "com.l7.streetlauncher"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.l7.streetlauncher"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = System.getenv("L7_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("L7_KEY_ALIAS")
                keyPassword = System.getenv("L7_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (releaseKeystore != null) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
}
