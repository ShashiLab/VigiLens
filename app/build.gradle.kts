plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "app.shashi.VigiLens"
    compileSdk = 34

    defaultConfig {
        applicationId = "app.shashi.VigiLens"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }


    packaging {
        resources {
            excludes += "META-INF/androidx.cardview_cardview.version"
        }
    }


    buildTypes {
        release {
            isMinifyEnabled = true
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
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.firebase.config)
    implementation(libs.google.firebase.crashlytics)
    implementation(libs.google.firebase.perf)
    implementation(libs.google.firebase.analytics)
    implementation(platform(libs.firebase.bom))
    implementation(libs.glide)
    implementation(libs.play.services.ads)
    implementation(libs.cardview)
    implementation(libs.review)
    androidTestImplementation(libs.espresso.contrib)
    androidTestImplementation(libs.rules)
    annotationProcessor(libs.compiler)
    implementation(libs.core.splashscreen)
    implementation(libs.biometric)
    implementation(libs.play.services.base)
    implementation(libs.gson)
    implementation(libs.recyclerview)
    testImplementation(libs.junit)

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}