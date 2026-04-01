# KatchIt! ProGuard/R8 Rules

# Keep Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class pg.geobingo.one.**$$serializer { *; }
-keepclassmembers class pg.geobingo.one.** {
    *** Companion;
}
-keepclasseswithmembers class pg.geobingo.one.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Supabase / Ktor
-keep class io.github.jan.supabase.** { *; }
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep data classes used with Supabase
-keep class pg.geobingo.one.network.*Dto { *; }
-keep class pg.geobingo.one.network.*Dto$* { *; }

# OkHttp (Ktor engine dependency)
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Google Play Services
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Google Mobile Ads
-keep class com.google.android.ump.** { *; }
-keep class com.google.ads.** { *; }
-dontwarn com.google.ads.**

# Google Billing
-keep class com.android.vending.billing.** { *; }
