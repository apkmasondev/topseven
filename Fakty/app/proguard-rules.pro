# Reguły ProGuard/R8 dla aplikacji Top Seven.
#
# Zasada: trzymamy tylko to, co faktycznie jest odczytywane przez refleksję
# (kotlinx.serialization). Wcześniejsze reguły były zbyt szerokie - m.in.
# `-keep class kotlinx.serialization.** { *; }` oraz `-keepclassmembers class * { *** Companion; }`
# blokowały obfuskację WSZYSTKICH obiektów towarzyszących w aplikacji i jej zależnościach,
# co zauważalnie osłabiało minifikację.

# --- kotlinx.serialization ---------------------------------------------------
# Wygenerowane serializatory są wyszukiwane po nazwie.
-keepattributes *Annotation*, InnerClasses, Signature, RuntimeVisibleAnnotations, AnnotationDefault

-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <1>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Modele danych i trasy nawigacji ----------------------------------------
# Pola modeli są mapowane po nazwie z data.json, a klasy tras - z argumentów nawigacji.
-keep,allowobfuscation @kotlinx.serialization.Serializable class com.topseven.fakty.data.models.** { *; }
-keep,allowobfuscation @kotlinx.serialization.Serializable class com.topseven.fakty.ui.navigation.** { *; }

# --- Diagnostyka -------------------------------------------------------------
# Zachowanie numerów linii w stack trace przy zachowanej obfuskacji nazw plików.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
