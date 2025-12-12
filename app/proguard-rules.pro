# Add project specific ProGuard rules here.

# Keep Monero native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep WalletSuite
-keep class com.techducat.apo.monero.wallet.WalletSuite { *; }
-keep class com.techducat.apo.monero.wallet.WalletSuite$* { *; }

# Keep Monerujo library classes
-keep class com.m2049r.xmrwallet.** { *; }

# Keep Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep Material3
-keep class androidx.compose.material3.** { *; }

# Kotlin
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# ZXing
-keep class com.google.zxing.** { *; }
-keep class com.journeyapps.barcodescanner.** { *; }
-dontwarn com.google.zxing.**

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# gRPC Netty - Ignore BlockHound integration (development tool)
-dontwarn reactor.blockhound.integration.BlockHoundIntegration
-dontwarn io.grpc.netty.shaded.io.netty.util.internal.Hidden$NettyBlockHoundIntegration

# gRPC - Keep runtime classes
-keep class io.grpc.** { *; }
-keepclassmembers class io.grpc.** { *; }
-dontwarn io.grpc.**

# gRPC Netty Shaded
-keep class io.grpc.netty.shaded.** { *; }
-dontwarn io.grpc.netty.shaded.**

# Protobuf
-keep class com.google.protobuf.** { *; }
-keep class * extends com.google.protobuf.GeneratedMessageLite { *; }
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
    <methods>;
}
-dontwarn com.google.protobuf.**

# gRPC stub classes (auto-generated)
-keep class **.*Grpc { *; }
-keep class **.*Stub { *; }

# ZXing QR Code library
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**
