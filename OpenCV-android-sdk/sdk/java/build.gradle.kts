plugins {
    id("com.android.library")
}

android {
    namespace = "org.opencv"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }

    sourceSets {
        getByName("main") {
            java.srcDirs("src")
            jniLibs.srcDirs("libs")
        }
    }
}