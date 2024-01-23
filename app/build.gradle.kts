plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.facerecognitioninputsapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.facerecognitioninputsapp"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }


    buildFeatures {
        viewBinding = true
        mlModelBinding = true
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("org.tensorflow:tensorflow-lite-metadata:0.1.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("com.google.guava:guava:27.1-jre")
    implementation("com.google.android.gms:play-services-mlkit-face-detection:17.1.0")
    implementation("com.google.mlkit:face-mesh-detection:16.0.0-beta1")
    implementation("com.google.mlkit:barcode-scanning:17.2.0")
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")

    implementation("org.tensorflow:tensorflow-lite:2.12.0")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.12.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.3")

    implementation("com.google.code.gson:gson:2.10")

//    implementation("androidx.datastore:datastore-preferences:1.0.0-alpha04")
//    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.2.0")
//    implementation("androidx.lifecycle:lifecycle-runtime-ktx-2.2.0")
//    implementation("androidx.legacy:legacy-support-v4:1.0.0")
//    implementation("androidx.legacy:legacy-support-core-utils:1.0.0")
}
