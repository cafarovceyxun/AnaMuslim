# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep file names and line numbers.
-keepattributes SourceFile,LineNumberTable
# Keep custom exceptions.
-keep public class * extends java.lang.Exception
# Keep all api models
-keep class com.cafarovceyxun.anamuslim.api.models.** { *; }
-keep class com.cafarovceyxun.anamuslim.utils.supabase.** { *; }

# Glance app widgets
#
# Glance saxlayır: `provider:<GlanceAppWidget sinfinin canonicalName-i>` -> receiver adı, özünün
# DataStore faylında (`GlanceAppWidgetManager`). Kitabxananın öz consumer qaydaları (glance-appwidget
# proguard.txt) yalnız `ActionCallback` varislərini saxlayır, `GlanceAppWidget` varislərini yox — yəni
# release-də bu siniflərin adını R8 verir. Ad buraxılışdan buraxılışa dəyişə (və ya iki sinif birləşə)
# bilər, cihazdakı xəritə isə qalır: köhnə sətir artıq başqa vidcetə aid adı göstərir və bir vidcet
# **digərinin məzmunu ilə** yenilənir (pleyer kartında günün ayəsi görünürdü). Adları sabitləyirik;
# `refreshAllInstances` id-ləri onsuz da manifestdəki ComponentName-dən alır — iki qat müdafiə.
-keep class * extends androidx.glance.appwidget.GlanceAppWidget
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver

# Room
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>(...);
}
-keep class * extends androidx.room.RoomDatabase

# Media3
-keep class androidx.media3.common.** { *; }
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.session.** { *; }

# Supabase / Ktor / Serialization
-keep class io.github.jan.supabase.** { *; }
-keep class io.ktor.** { *; }
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature, Exceptions
-keep @kotlinx.serialization.Serializable class * { *; }

# Keep generic signature of Call, Response (R8 full mode strips signatures from non-kept items).
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

 # With R8 full mode generic signatures are stripped for classes that are not
 # kept. Suspend functions are wrapped in continuations where the type argument
 # is used.
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-ignorewarnings
