plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.reboot.rebootearth"
    compileSdk {
        version = release(37)
    }

    androidResources {
        noCompress += "tflite"
    }

    defaultConfig {
        applicationId = "com.reboot.rebootearth"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"


        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    implementation("org.tensorflow:tensorflow-lite-task-vision:0.4.4")

    implementation("org.tensorflow:tensorflow-lite:2.16.1")
    implementation("org.tensorflow:tensorflow-lite:2.16.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
}