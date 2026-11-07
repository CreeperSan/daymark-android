plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    kotlin("kapt")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.creepersan.memorialday"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.creepersan.memorialday"
        minSdk = 26
        targetSdk = 37
        versionCode = 30000
        versionName = "3.0.0"

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
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.3"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    viewBinding {
        this.enable = true
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("androidx.activity:activity-compose:1.7.0")
    implementation(platform("androidx.compose:compose-bom:2023.03.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.03.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // 引入 Room
    val versionRoom = "2.5.2"
    implementation("androidx.room:room-runtime:$versionRoom")
    kapt("androidx.room:room-compiler:$versionRoom")
    implementation("androidx.room:room-rxjava3:$versionRoom")

    // 引入 RxJava
    implementation("io.reactivex.rxjava3:rxandroid:3.0.2")
    implementation("io.reactivex.rxjava3:rxjava:3.1.5")

    // 网络请求与 JSON 序列化
    implementation("com.squareup.okhttp3:okhttp:5.5.1")
    implementation("com.google.code.gson:gson:2.15.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:5.5.1")

    // 引入 ConstraintLayout
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
//    implementation("androidx.constraintlayout:constraintlayout-compose:1.1.0-alpha11")

    // 引入 Glide
    implementation("com.github.bumptech.glide:glide:4.15.1")

    // 引入 RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.3.1")

    // 引入 Material
    implementation("com.google.android.material:material:1.9.0")

    // 引入下拉刷新组件
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
}
