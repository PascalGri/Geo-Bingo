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
