plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.bkpit.mangal.llm"
    compileSdk = 35
    defaultConfig {
        minSdk = 26
        ndk { abiFilters += listOf("arm64-v8a") }
        externalNativeBuild {
            cmake {
                arguments += listOf("-DANDROID_STL=c++_shared")
                cppFlags += listOf("-O3")
            }
        }
    }
    // NOTE (read before building): this points at src/main/cpp/CMakeLists.txt,
    // which in turn expects a real llama.cpp checkout at
    // core-llm/src/main/cpp/llama.cpp/ (git submodule, added by you — see
    // README.md "Adding the native engines"). Until that submodule exists,
    // this module will fail to configure. That's intentional: I'm not
    // vendoring an unverified prebuilt .so into your project.
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}
