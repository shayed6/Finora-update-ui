# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Room Database rules
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class com.example.data.local.entity.** { *; }
-keep class com.example.data.local.dao.** { *; }

# Moshi rules
-keepclassmembers class * {
    @com.squareup.moshi.* <fields>;
}

# Unity LevelPlay (IronSource) Mediation SDK
-keepclassmembers class * implements com.ironsource.mediationsdk.adunit.adapter.internal.AdapterBaseInterface {
    public *;
}
-keepclassmembers class * implements com.ironsource.mediationsdk.adunit.adapter.internal.AdapterConsentInterface {
    public *;
}
-keep class com.ironsource.mediationsdk.** { *; }
-keep class com.ironsource.lifecycle.** { *; }
-keep class com.ironsource.adapters.** { *; }
-dontwarn com.ironsource.mediationsdk.**

# InMobi Adapter & SDK
-keep class com.inmobi.** { *; }
-dontwarn com.inmobi.**
-keepattributes SourceFile,LineNumberTable

# Mintegral Adapter & SDK
-keepattributes Signature
-keepattributes *Annotation*
-keepclassmembers class com.mbridge.msdk.** {
    <fields>;
    <methods>;
}
-keep class com.mbridge.msdk.** { *; }
-dontwarn com.mbridge.msdk.**

# Strip verbose and debug logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}
