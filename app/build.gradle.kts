plugins { id("com.android.application") }

android {
    namespace = "com.amarbangladesh.names"
    compileSdk { version = release(37) }
    defaultConfig {
        applicationId = "com.amarbangladesh.names"
        minSdk = 26
        targetSdk = 37
        versionCode = 15
        versionName = "1.14"
        testInstrumentationRunner = "com.amarbangladesh.names.OfflineSmokeTest"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    androidResources { noCompress += "mp3" }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
}
