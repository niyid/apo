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


# ============================================================================
# MONERO WALLET SPECIFIC RULES (Added by setup_wallet.py)
# ============================================================================

# Keep all native methods for Monero JNI
-keepclasseswithmembernames class * {
    native <methods>;
}

# ============================================================================
# WALLETSUITE & CORE CLASSES
# ============================================================================

# Keep WalletSuite and all nested classes/interfaces
-keep class com.techducat.apo.WalletSuite { *; }
-keep class com.techducat.apo.WalletSuite$* { *; }
-keepclassmembers class com.techducat.apo.WalletSuite {
    public *;
    protected *;
}

# Keep PermissionHandler
-keep class com.techducat.apo.PermissionHandler { *; }
-keep class com.techducat.apo.PermissionHandler$* { *; }

# ============================================================================
# MONERUJO LIBRARY (com.m2049r.xmrwallet)
# ============================================================================

# Keep ALL Monerujo classes (critical for JNI stability)
-keep class com.m2049r.xmrwallet.** { *; }
-keepclassmembers class com.m2049r.xmrwallet.** {
    public *;
    protected *;
    native <methods>;
}

# Keep specific model classes that are frequently accessed via JNI
-keep class com.m2049r.xmrwallet.model.Wallet { *; }
-keep class com.m2049r.xmrwallet.model.Wallet$* { *; }
-keep class com.m2049r.xmrwallet.model.WalletManager { *; }
-keep class com.m2049r.xmrwallet.model.WalletListener { *; }
-keep class com.m2049r.xmrwallet.model.PendingTransaction { *; }
-keep class com.m2049r.xmrwallet.model.PendingTransaction$* { *; }
-keep class com.m2049r.xmrwallet.model.TransactionInfo { *; }
-keep class com.m2049r.xmrwallet.model.TransactionInfo$* { *; }
-keep class com.m2049r.xmrwallet.model.TransactionHistory { *; }
-keep class com.m2049r.xmrwallet.model.NetworkType { *; }

# Keep Node class for daemon configuration
-keep class com.m2049r.xmrwallet.data.Node { *; }
-keep class com.m2049r.xmrwallet.data.TxData { *; }

# ============================================================================
# ENUMS USED IN JNI
# ============================================================================

# Keep all enum methods used by JNI
-keepclassmembers enum com.m2049r.xmrwallet.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    **[] $VALUES;
    public *;
}

# Specific enums that MUST be preserved
-keep enum com.m2049r.xmrwallet.model.Wallet$Status { *; }
-keep enum com.m2049r.xmrwallet.model.Wallet$ConnectionStatus { *; }
-keep enum com.m2049r.xmrwallet.model.Wallet$Device { *; }
-keep enum com.m2049r.xmrwallet.model.PendingTransaction$Status { *; }
-keep enum com.m2049r.xmrwallet.model.PendingTransaction$Priority { *; }
-keep enum com.m2049r.xmrwallet.model.NetworkType { *; }

# ============================================================================
# CALLBACK INTERFACES
# ============================================================================

# Keep all callback interfaces for async operations
-keep interface com.techducat.apo.WalletSuite$* { *; }
-keep class * implements com.techducat.apo.WalletSuite$* { *; }

# Keep Wallet's RescanCallback interface
-keep interface com.m2049r.xmrwallet.model.Wallet$RescanCallback { *; }
-keep class * implements com.m2049r.xmrwallet.model.Wallet$RescanCallback { *; }

# ============================================================================
# JNI STRING & REFLECTION OPERATIONS
# ============================================================================

# Keep String operations used in JNI
-keepclassmembers class java.lang.String {
    public byte[] getBytes(java.lang.String);
    public java.lang.String(byte[], java.lang.String);
}

# Keep fields accessed via reflection
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# ============================================================================
# SERIALIZATION SUPPORT
# ============================================================================

# Keep serialization support for wallet data
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ============================================================================
# ATOMIC OPERATIONS
# ============================================================================

# Keep atomic classes used in WalletSuite for thread-safe operations
-keep class java.util.concurrent.atomic.** { *; }

# ============================================================================
# CHANGENOW EXCHANGE SERVICE
# ============================================================================

# Keep ChangeNowSwapService if exchange feature is enabled
-keep class com.techducat.apo.ChangeNowSwapService { *; }
-keep class com.techducat.apo.ChangeNowSwapService$* { *; }

# ============================================================================
# DEBUGGING (Remove in production)
# ============================================================================

# Keep line numbers for better crash reports
-keepattributes SourceFile,LineNumberTable

# Keep parameter names for debugging
-keepattributes MethodParameters

# ============================================================================
# END MONERO WALLET RULES
# ============================================================================
