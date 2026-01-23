# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Keep line numbers for better crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep annotations
-keepattributes *Annotation*

# Keep InputMethodService and related classes
-keep public class * extends android.inputmethodservice.InputMethodService
-keep class com.rr.aido.service.** { *; }

# Keep data models (for Gson/serialization)
-keep class com.rr.aido.data.models.** { *; }
-keepclassmembers class com.rr.aido.data.models.** { *; }

# Keep Retrofit interfaces and models
-keep interface com.rr.aido.data.api.** { *; }
-keep class com.rr.aido.data.repository.** { *; }

# Ensure Gemini request/response models and API interface are preserved
# Sometimes R8 can strip or rename these resulting in runtime failures only
# in release builds. Keep them explicitly so Retrofit/Gson can map JSON.
-keep class com.rr.aido.data.repository.GeminiRequest { *; }
-keep class com.rr.aido.data.repository.GeminiResponse { *; }
-keep class com.rr.aido.data.repository.Content { *; }
-keep class com.rr.aido.data.repository.Part { *; }
-keep interface com.rr.aido.data.repository.GeminiApiService { *; }

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Fix for TypeToken crash in release builds
# Keeps the generic type information for anonymous subclasses of TypeToken
-keep class * extends com.google.gson.reflect.TypeToken

# Retrofit
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keep,includedescriptorclasses class com.rr.aido.**$$serializer { *; }
-keepclassmembers class com.rr.aido.** {
    *** Companion;
}
-keepclasseswithmembers class com.rr.aido.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# DataStore
-keep class androidx.datastore.*.** { *; }

# Compose
-keep class androidx.compose.** { *; }
-keep interface androidx.compose.** { *; }

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Keep custom exceptions for better crash reporting
-keep public class * extends java.lang.Exception

# Keep ViewModels
-keep class * extends androidx.lifecycle.ViewModel {
    <init>();
}
-keep class * extends androidx.lifecycle.AndroidViewModel {
    <init>(android.app.Application);
}

# Kotlin Metadata
-keep class kotlin.Metadata { *; }

# Preserve debugging information
-keepattributes LocalVariableTable
-keepattributes LocalVariableTypeTable

# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Parcelable
-keepclassmembers class * implements android.os.Parcelable {
    static ** CREATOR;
}

# Keep R8 optimization
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify