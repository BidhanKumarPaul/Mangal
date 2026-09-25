plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.bkpit.mangal.data"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // SQLCipher's own Android bindings, wired into Room's SupportSQLiteOpenHelper.Factory
    implementation("net.zetetic:android-database-sqlcipher:4.5.4")
   //implementation("net.zetetic:sqlcipher-android:4.9.0")
    implementation("androidx.sqlite:sqlite:2.4.0")

    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Stores the random SQLCipher passphrase itself, Keystore-backed rather
    // than plaintext in SharedPreferences — see db/DatabasePassphrase.kt.
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Resumable, range-request model downloads (Phase 2).
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
