# Gson: preserva DTOs y modelos de dominio para que la serialización no rompa con R8
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class com.grupo.elevapro.data.model.** { *; }
-keep class com.grupo.elevapro.data.local.entity.** { *; }

# Room: genera código en tiempo de compilación; no necesita reglas extra con KSP

# Stack traces legibles
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile