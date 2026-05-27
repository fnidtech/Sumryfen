# Keep Room entities
-keep class com.sumryfen.data.local.** { *; }

# Keep Gson serialization
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }

# Keep data models used by Gson
-keep class com.sumryfen.data.remote.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# SplashScreen
-keep class androidx.core.splashscreen.** { *; }
