#!/bin/bash
# ============================================
# setup_project.sh - Complete Project Setup Script
# ============================================

set -e  # Exit on error

PROJECT_NAME="apo"
PACKAGE_NAME="com.techducat.apo"
PACKAGE_PATH="com/techducat/apo"
APP_ID="com.techducat.apo"

echo "=================================================="
echo "  Monero Wallet Android Project Setup"
echo "  Project: $PROJECT_NAME"
echo "  Package: $PACKAGE_NAME"
echo "=================================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored messages
print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}→ $1${NC}"
}

print_step() {
    echo -e "${BLUE}━━━ $1 ━━━${NC}"
}

# Check if we're in the project root
if [ ! -d "app" ]; then
    print_error "Error: app/ directory not found. Please run this script from the project root."
    exit 1
fi

print_step "Step 1: Creating project structure"

# Create directory structure
mkdir -p app/src/main/java/$PACKAGE_PATH/monero/ui
mkdir -p app/src/main/java/$PACKAGE_PATH/monero/wallet
mkdir -p app/src/main/java/$PACKAGE_PATH/monero/util
mkdir -p app/src/main/res/xml
mkdir -p app/src/main/res/values
mkdir -p app/src/main/jniLibs/armeabi-v7a
mkdir -p app/src/main/jniLibs/arm64-v8a
mkdir -p app/src/main/jniLibs/x86
mkdir -p app/src/main/jniLibs/x86_64
mkdir -p app/src/main/assets

print_success "Directory structure created"

# ============================================
# Create settings.gradle.kts
# ============================================
print_step "Step 2: Creating settings.gradle.kts"

cat > settings.gradle.kts << 'EOF'
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "apo"
include(":app")
EOF

print_success "settings.gradle.kts created"

# ============================================
# Create Root build.gradle.kts
# ============================================
print_step "Step 3: Creating root build.gradle.kts"

cat > build.gradle.kts << 'EOF'
// Top-level build file
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("com.android.library") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.20" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
EOF

print_success "Root build.gradle.kts created"

# ============================================
# Create App build.gradle.kts
# ============================================
print_step "Step 4: Creating app/build.gradle.kts"

cat > app/build.gradle.kts << EOF
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "$PACKAGE_NAME"
    compileSdk = 34

    defaultConfig {
        applicationId = "$APP_ID"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        vectorDrawables {
            useSupportLibrary = true
        }
        
        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            isDebuggable = true
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
    }
    
    buildFeatures {
        compose = true
        buildConfig = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }
    
    packaging {
        resources {
            excludes += listOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/DEPENDENCIES",
                "/META-INF/LICENSE",
                "/META-INF/LICENSE.txt",
                "/META-INF/license.txt",
                "/META-INF/NOTICE",
                "/META-INF/NOTICE.txt",
                "/META-INF/notice.txt",
                "/META-INF/ASL2.0",
                "/META-INF/*.kotlin_module"
            )
        }
    }
}

dependencies {
    // Kotlin & Coroutines
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.20")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // AndroidX Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Jetpack Compose
    val composeBom = platform("androidx.compose:compose-bom:2024.01.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.runtime:runtime-livedata")
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.6")
    
    // QR Code Generation & Scanning
    implementation("com.google.zxing:core:3.5.2")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    
    // Image Loading
    implementation("io.coil-kt:coil-compose:2.5.0")
    
    // Networking
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // JSON
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    
    // DataStore (for preferences)
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    // Bluetooth
    implementation("androidx.bluetooth:bluetooth:1.0.0-alpha01")
    
    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    
    // Accompanist (for permissions)
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.32.0")
    
    // Security
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    
    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
EOF

print_success "app/build.gradle.kts created"

# ============================================
# Create ProGuard Rules
# ============================================
print_step "Step 5: Creating proguard-rules.pro"

cat > app/proguard-rules.pro << 'EOF'
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
EOF

print_success "proguard-rules.pro created"

# ============================================
# Create AndroidManifest.xml
# ============================================
print_step "Step 6: Creating AndroidManifest.xml"

cat > app/src/main/AndroidManifest.xml << EOF
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <!-- Internet permissions -->
    <uses-permission android:name="android.permission.INTERNET"/>
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32"/>
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="32"/>
    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
    
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    
    <!-- Bluetooth permissions -->
    <uses-permission android:name="android.permission.BLUETOOTH" android:maxSdkVersion="30" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" android:maxSdkVersion="30" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
    <uses-permission android:name="android.permission.BLUETOOTH_SCAN" 
        android:usesPermissionFlags="neverForLocation" />
    
    <!-- Location permission (BLE scanning on older Android) -->
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" android:maxSdkVersion="30" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" android:maxSdkVersion="30" />
    
    <!-- Notification permissions -->
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    
    <!-- Haptic feedback -->
    <uses-permission android:name="android.permission.VIBRATE" />
    
    <!-- Battery optimization -->
    <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
    
    <!-- Camera for QR scanning -->
    <uses-permission android:name="android.permission.CAMERA" />
    
    <!-- Hardware features -->
    <uses-feature android:name="android.hardware.bluetooth_le" android:required="true" />
    <uses-feature android:name="android.hardware.bluetooth" android:required="true" />
    <uses-feature android:name="android.hardware.camera" android:required="false" />
    <uses-feature android:name="android.hardware.camera.autofocus" android:required="false" />

    <application
        android:name=".ApoApplication"
        android:allowBackup="false"
        android:usesCleartextTraffic="true"
        android:icon="@mipmap/ic_launcher"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.Apo"
        android:hardwareAccelerated="true"
        android:largeHeap="true"
        tools:targetApi="31">
        
        <!-- Main Activity -->
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:label="@string/app_name"
            android:theme="@style/Theme.Apo"
            android:screenOrientation="portrait"
            android:windowSoftInputMode="adjustResize"
            android:launchMode="singleTop">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        
        <!-- Monero Wallet Activity -->
        <activity
            android:name=".monero.ui.MoneroWalletActivity"
            android:exported="false"
            android:label="Monero Wallet"
            android:theme="@style/Theme.Apo"
            android:screenOrientation="portrait"
            android:windowSoftInputMode="adjustResize" />
        
        <!-- QR Code Capture Activity -->
        <activity
            android:name="com.journeyapps.barcodescanner.CaptureActivity"
            android:screenOrientation="fullSensor"
            tools:replace="screenOrientation" />
        
        <!-- File Provider for sharing -->
        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${APP_ID}.fileprovider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths" />
        </provider>
        
    </application>
</manifest>
EOF

print_success "AndroidManifest.xml created"

# ============================================
# Create file_paths.xml
# ============================================
print_step "Step 7: Creating file_paths.xml"

cat > app/src/main/res/xml/file_paths.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<paths xmlns:android="http://schemas.android.com/apk/res/android">
    <external-path name="external_files" path="."/>
    <cache-path name="cache" path="."/>
    <files-path name="internal_files" path="."/>
</paths>
EOF

print_success "file_paths.xml created"

# ============================================
# Create strings.xml
# ============================================
print_step "Step 8: Creating strings.xml"

cat > app/src/main/res/values/strings.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Apo Wallet</string>
    <string name="wallet_title">Monero Wallet</string>
    <string name="send_title">Send XMR</string>
    <string name="receive_title">Receive XMR</string>
    <string name="history_title">Transaction History</string>
    <string name="settings_title">Settings</string>
</resources>
EOF

print_success "strings.xml created"

# ============================================
# Create themes.xml
# ============================================
print_step "Step 9: Creating themes.xml"

cat > app/src/main/res/values/themes.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.Apo" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
EOF

print_success "themes.xml created"

# ============================================
# Create gradle.properties
# ============================================
print_step "Step 10: Creating gradle.properties"

cat > gradle.properties << 'EOF'
# Project-wide Gradle settings.
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.caching=true

# AndroidX
android.useAndroidX=true
android.enableJetifier=true

# Kotlin
kotlin.code.style=official

# Build optimizations
android.nonTransitiveRClass=true
android.defaults.buildfeatures.buildconfig=true
EOF

print_success "gradle.properties created"

# ============================================
# Create gradle-wrapper.properties
# ============================================
print_step "Step 11: Creating gradle wrapper"

mkdir -p gradle/wrapper

cat > gradle/wrapper/gradle-wrapper.properties << 'EOF'
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.2-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
EOF

print_success "gradle-wrapper.properties created"

# ============================================
# Create local.properties template
# ============================================
print_step "Step 12: Creating local.properties template"

if [ ! -f "local.properties" ]; then
    cat > local.properties << 'EOF'
## This file must *NOT* be checked into Version Control Systems,
# as it contains information specific to your local configuration.
#
# Location of the SDK. This is only used by Gradle.
# For customization when using a Version Control System, please read the
# header note.
sdk.dir=/path/to/your/Android/sdk
EOF
    print_info "Please update local.properties with your Android SDK path"
else
    print_info "local.properties already exists, skipping"
fi

# ============================================
# Create .gitignore
# ============================================
print_step "Step 13: Creating .gitignore"

cat > .gitignore << 'EOF'
# Built application files
*.apk
*.aar
*.ap_
*.aab

# Files for the ART/Dalvik VM
*.dex

# Java class files
*.class

# Generated files
bin/
gen/
out/
release/

# Gradle files
.gradle/
build/

# Local configuration file (sdk path, etc)
local.properties

# Log Files
*.log

# Android Studio
*.iml
.idea/
.DS_Store
/captures
.externalNativeBuild
.cxx

# Keystore files
*.jks
*.keystore

# Native libraries
*.so

# Backup files
*.bak
*~

# Wallet files (keep secure!)
*.keys
wallet.properties

EOF

print_success ".gitignore created"

# ============================================
# Summary
# ============================================
echo ""
print_step "Setup Complete!"
echo ""
echo "Project structure created for:"
echo "  📦 Package: $PACKAGE_NAME"
echo "  📱 App ID:  $APP_ID"
echo ""
echo "Next steps:"
echo "  1. Copy your Monerujo native libraries (.so files) to:"
echo "     app/src/main/jniLibs/{armeabi-v7a,arm64-v8a,x86,x86_64}/"
echo ""
echo "  2. Copy WalletSuite.java to:"
echo "     app/src/main/java/$PACKAGE_PATH/monero/wallet/"
echo ""
echo "  3. Copy Kotlin UI files to:"
echo "     app/src/main/java/$PACKAGE_PATH/monero/ui/"
echo "     app/src/main/java/$PACKAGE_PATH/monero/util/"
echo ""
echo "  4. Update local.properties with your Android SDK path"
echo ""
echo "  5. Run: ./gradlew assembleDebug"
echo ""
print_success "All configuration files created successfully!"
echo ""
