# === Phase 6: release ProGuard/R8 rules ===

# CRITICAL: never strip JNI-facing classes/methods for llama.cpp / whisper.cpp.
# If these get renamed or removed, native code calling back into Kotlin via
# JNI (UnsatisfiedLinkError / NoSuchMethodError at runtime, only in release
# builds) will crash silently in ways that never show up in debug.
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class com.bkpit.mangal.llm.** { *; }
-keep class com.bkpit.mangal.stt.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Hilt / Dagger generated code
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.HiltAndroidApp
-keepclasseswithmembers class * {
    @dagger.hilt.android.AndroidEntryPoint <methods>;
}

# Kotlin coroutines / serialization metadata
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-dontwarn kotlinx.coroutines.**

# Compose
-dontwarn androidx.compose.**
