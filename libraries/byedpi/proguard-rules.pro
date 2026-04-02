# ProGuard rules for ByeDPI Library
-keep class io.github.romanvht.byedpi.library.** { *; }
-keep class io.github.romanvht.byedpi.library.data.** { *; }
-dontwarn io.github.romanvht.byedpi.library.**

# Gson
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**
