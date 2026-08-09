# ProGuard rules for Top Seven app

# Keep data classes used by kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class com.topseven.fakty.data.models.** { *; }

# Keep Navigation route classes (used by kotlinx.serialization for type-safe nav)
-keepclassmembers class com.topseven.fakty.ui.navigation.** { *; }
-keep class com.topseven.fakty.ui.navigation.** { *; }

# Keep Kotlin serialization companion objects
-keepclassmembers class * {
    *** Companion;
}
-keep class **$$serializer { *; }
