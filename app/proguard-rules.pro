# Keep WebRTC native symbols
-keep class org.webrtc.** { *; }
-keep class io.getstream.webrtc.** { *; }

# Keep kotlinx.serialization metadata
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep model classes used in serialization
-keep class com.remotebridge.core.model.** { *; }
