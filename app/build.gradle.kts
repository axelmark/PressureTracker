plugins {
    alias(libs.plugins.androidApplication)
}

android {
    namespace = "ru.axelmark.pressuretracker"
    compileSdk = 34

    defaultConfig {
        applicationId = "ru.axelmark.pressuretracker"
        minSdk = 24
        targetSdk = 34
        versionCode = 2
        versionName = "2.0.1"

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
}

dependencies {

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    // ----
    implementation(libs.appcompat.v161)
    implementation(libs.material)
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")

    // Room components
    implementation("androidx.room:room-runtime:2.6.0")
    annotationProcessor("androidx.room:room-compiler:2.6.0")

    // Lifecycle components
    implementation("androidx.lifecycle:lifecycle-livedata:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.6.2")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // MPAndroidChart for graphs
    implementation(files("libs/MPAndroidchart-v3.1.0.aar"))

    // WorkManager for notifications
    implementation("androidx.work:work-runtime:2.8.1")

    // Permissions handling
    implementation("com.karumi:dexter:6.2.3")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.work:work-runtime:2.9.0")

    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.core:core:1.12.0")
    implementation("androidx.print:print:1.1.0")
    implementation("com.itextpdf:itext7-core:7.2.5")
    implementation("androidx.core:core-ktx:1.12.0")
}