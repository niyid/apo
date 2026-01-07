#!/bin/bash

# scripts/prepare-fdroid.sh
# Prepares the repository for F-Droid compatibility

set -e

echo "=========================================="
echo "Preparing repository for F-Droid"
echo "=========================================="
echo ""

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 1. Update build.gradle.kts (app level)
echo "1. Updating app/build.gradle.kts for F-Droid compatibility..."
cat > app/build.gradle.kts << 'EOF'
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.protobuf")    
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.parcelize")
}

android {
    namespace = "com.techducat.apo"
    compileSdk = 35
    
    defaultConfig {
        applicationId = "com.techducat.apo"
        minSdk = 24
        targetSdk = 35
        versionCode = project.property("VERSION_CODE").toString().toInt()
        versionName = project.property("VERSION_NAME").toString()
        
        buildConfigField("String", "CHANGENOW_API_KEY", "\"\"")
        buildConfigField("Boolean", "USE_FIREBASE", "false")
        buildConfigField("Boolean", "USE_MLKIT", "false")
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        resourceConfigurations += listOf(
            "en", "es", "fr", "de", "it", "pt", "ja", "ko", "zh", "ar", "ru"
        )
        
        ndk {
            abiFilters.addAll(listOf("arm64-v8a", "x86_64"))
        }
    }

    lint {
        disable += setOf(
            "NullSafeMutableLiveData",
            "RememberInComposition",
            "FrequentlyChangingValue",
            "OpaqueUnitKey",
            "ComposableNaming",
            "ComposableLambdaParameterNaming",
            "ComposableLambdaParameterPosition",
            "AutoboxingStateCreation"
        )
        abortOnError = false
        checkDependencies = false
    }

    packaging.resources {
        excludes += setOf(
            "META-INF/DEPENDENCIES",
            "META-INF/LICENSE.md",
            "META-INF/NOTICE.md",
            "/META-INF/{AL2.0,LGPL2.1}"
        )
    }
    
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
    
    sourceSets {
        getByName("main") {
            assets.srcDirs("$projectDir/schemas")
        }
    }    
    
    signingConfigs {
        create("release") {
            // F-Droid handles signing
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
        }
    }
    
    packaging {
        jniLibs {
            useLegacyPackaging = false
            pickFirsts += "**/libc++_shared.so"
            pickFirsts += "**/libjsc.so"
            pickFirsts += "**/libmonerujo.so"
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
        }
    }    
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
    
    buildFeatures {
        buildConfig = true
        compose = true
        viewBinding = false
        dataBinding = false
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
            all {
                it.systemProperty("robolectric.offline", "true")
                it.systemProperty("koin.test", "false")
                it.systemProperty("robolectric.nativedeps.skip", "true")
            }
        }
    }
}

afterEvaluate {
    tasks.matching { task ->
        task.name.startsWith("strip") && task.name.contains("DebugSymbol")
    }.configureEach {
        doLast {
            println("=== Realigning native libraries for 16KB page size ===")
            
            val libsToAlign = listOf("libbarhopper_v3.so", "libimage_processing_util_jni.so")
            val alignScript = File(project.rootDir, "align_elf.py")
            
            if (!alignScript.exists()) {
                println("ERROR: align_elf.py not found at ${alignScript.absolutePath}")
                println("Please ensure align_elf.py is in the project root directory")
                return@doLast
            }
            
            val strippedLibsDir = File(project.layout.buildDirectory.get().asFile, "intermediates/stripped_native_libs")
            
            var filesProcessed = 0
            var filesAligned = 0
            if (strippedLibsDir.exists()) {
                val archsToProcess = listOf("arm64-v8a", "x86_64")
                
                project.fileTree(strippedLibsDir) {
                    libsToAlign.forEach { include("**/$it") }
                }.forEach { file ->
                    if (archsToProcess.any { file.path.contains("/$it/") }) {
                        filesProcessed++
                        try {
                            val tempFile = File("${file.absolutePath}.tmp")
                            
                            println("  Realigning: ${file.name} (${file.parentFile.name})")
                            
                            val result = project.exec {
                                commandLine("python3", alignScript.absolutePath, file.absolutePath, tempFile.absolutePath)
                                isIgnoreExitValue = true
                            }
                            
                            if (result.exitValue == 0 && tempFile.exists()) {
                                val originalSize = file.length()
                                
                                file.delete()
                                tempFile.renameTo(file)
                                
                                println("    ✓ Aligned successfully (${originalSize} bytes)")
                                filesAligned++
                            } else {
                                println("    ✗ Alignment failed")
                                if (tempFile.exists()) tempFile.delete()
                            }
                        } catch (e: Exception) {
                            println("    ✗ Error: ${e.message}")
                        }
                    }
                }
            }
            
            println("=== Realignment complete: $filesAligned/$filesProcessed files aligned ===")
        }
    }
}

protobuf {
    protoc { artifact = "com.google.protobuf:protoc:3.22.3" }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.57.0"
        }
    }
    generateProtoTasks {
        all().configureEach {
            builtins {
                create("java") { option("lite") }
            }
            plugins {
                create("grpc") { option("lite") }
            }
        }
    }
}

dependencies {
    compileOnly("org.projectlombok:lombok:1.18.34")
    annotationProcessor("org.projectlombok:lombok:1.18.34")

    testCompileOnly("org.projectlombok:lombok:1.18.34")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.34")
    
    // Compose
    implementation(platform("androidx.compose:compose-bom:2025.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended:1.7.6")
    
    // Monerujo dependencies
    implementation("org.json:json:20231013")
    
    // QR Code scanning - FOSS alternatives only
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    
    // Biometric Authentication
    implementation("androidx.biometric:biometric:1.2.0-alpha05")
    
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    
    // Lifecycle & Activity
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")
    implementation("androidx.lifecycle:lifecycle-service:2.9.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    
    // AndroidX Core
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("com.jakewharton.timber:timber:5.0.1")
    
    // Preferences
    implementation("androidx.preference:preference-ktx:1.2.1")
    
    // SwipeRefreshLayout
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    
    // Lifecycle & ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")
    
    // Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.5")
    
    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    
    // Kotlin Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    
    // Protobuf & gRPC
    implementation("com.google.protobuf:protobuf-kotlin:3.22.3")
    implementation("com.google.protobuf:protobuf-java:3.22.3")
    implementation("com.google.protobuf:protobuf-java-util:3.22.3")
    implementation("io.grpc:grpc-stub:1.57.0")
    implementation("io.grpc:grpc-kotlin-stub:1.3.0")
    implementation("io.grpc:grpc-protobuf:1.57.0")
    implementation("io.grpc:grpc-netty-shaded:1.57.0")    
    
    // Networking
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    
    // Dependency Injection
    implementation("io.insert-koin:koin-android:4.0.0")
    implementation("io.insert-koin:koin-androidx-navigation:4.0.0")
    
    // Security
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.biometric:biometric:1.2.0-alpha05")
    
    // Gson
    implementation("com.google.code.gson:gson:2.11.0")
    
    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.8.1")
    
    // Unit Testing - MockK
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("io.mockk:mockk-android:1.13.8")
    
    // Unit Testing - Core libraries
    testImplementation("junit:junit:4.13.2")
    testImplementation(kotlin("test-junit"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.test.ext:junit:1.1.5")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.11.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.0")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("androidx.room:room-testing:2.6.1")
    
    // Android Instrumentation Testing
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.08.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")    
}
EOF
echo -e "${GREEN}✓ Updated app/build.gradle.kts${NC}"

# 2. Update project-level build.gradle.kts to remove Google Services
echo ""
echo "2. Updating build.gradle.kts (project level)..."
if [ -f "build.gradle.kts" ]; then
    # Create backup
    cp build.gradle.kts build.gradle.kts.backup
    
    # Remove Google Services plugin
    sed -i.tmp '/google-services/d' build.gradle.kts
    sed -i.tmp '/firebase.crashlytics/d' build.gradle.kts
    rm -f build.gradle.kts.tmp
    
    echo -e "${GREEN}✓ Updated build.gradle.kts${NC}"
else
    echo -e "${YELLOW}⚠ build.gradle.kts not found, skipping${NC}"
fi

# 3. Remove google-services.json if it exists
echo ""
echo "3. Removing proprietary configuration files..."
if [ -f "app/google-services.json" ]; then
    rm app/google-services.json
    echo -e "${GREEN}✓ Removed google-services.json${NC}"
else
    echo "  google-services.json not found (OK)"
fi

# 4. Create Fastlane metadata structure
echo ""
echo "4. Creating Fastlane metadata structure..."
mkdir -p fastlane/metadata/android/en-US/changelogs
mkdir -p fastlane/metadata/android/en-US/images/phoneScreenshots

# Create title
cat > fastlane/metadata/android/en-US/title.txt << 'EOF'
Àpò - Monero Wallet
EOF

# Create short description
cat > fastlane/metadata/android/en-US/short_description.txt << 'EOF'
Privacy-focused Monero wallet with modern Material Design 3 interface
EOF

# Create full description
cat > fastlane/metadata/android/en-US/full_description.txt << 'EOF'
Àpò is a native Monero wallet application for Android built with modern technologies including Kotlin and Jetpack Compose, offering a secure and user-friendly way to manage your XMR cryptocurrency.

<b>Key Features</b>

• Create new Monero wallets or restore existing ones using seed phrases
• Send and receive XMR transactions with ease
• Real-time blockchain synchronization
• Complete transaction history with powerful search functionality
• Blockchain rescan capability for wallet recovery and troubleshooting
• QR code generation and scanning for seamless address sharing
• Biometric authentication (fingerprint/face unlock) for enhanced security
• Modern, intuitive Material Design 3 interface
• Multi-language support

<b>Privacy & Security</b>

Àpò prioritizes your privacy and security:

• Native Monero C++ libraries ensure protocol compatibility
• All wallet data encrypted and stored locally on your device
• No analytics or tracking - your financial data stays private
• Biometric authentication protects wallet access
• Open source - auditable by anyone

<b>Technology</b>

Built with cutting-edge Android development tools:

• Kotlin programming language for reliability
• Jetpack Compose for smooth, modern UI
• MVVM architecture with coroutines for responsive performance
• Room database for efficient local data management
• Native Monero libraries via JNI for secure wallet operations

<b>Important Security Note</b>

Always backup your seed phrase securely. Never share your seed phrase or private keys with anyone. Àpò developers will never ask for your seed phrase or private keys.

<b>About Monero</b>

Monero (XMR) is a privacy-focused cryptocurrency that uses ring signatures, stealth addresses, and confidential transactions to keep your financial information private.
EOF

# Create changelog
cat > fastlane/metadata/android/en-US/changelogs/31.txt << 'EOF'
Version 0.0.31

• Initial F-Droid release
• Native Monero wallet functionality
• Material Design 3 interface
• Biometric authentication support
• QR code scanning and generation
• Transaction history with search
• Multi-language support
• Enhanced security features
• FOSS-only dependencies (no Firebase, no ML Kit)
EOF

echo -e "${GREEN}✓ Created Fastlane metadata${NC}"

# 5. Create F-Droid specific documentation
echo ""
echo "5. Creating F-Droid documentation..."
cat > FDROID.md << 'EOF'
# F-Droid Build Instructions

This is the F-Droid compatible branch of Àpò.

## Differences from Main Branch

- **No Firebase**: Crashlytics and Analytics removed
- **No ML Kit**: Using ZXing for QR code scanning instead
- **No Google Play Services**: Fully FOSS stack
- **Build Flavor**: Uses `fdroid` flavor

## Building

```bash
# Debug build
./gradlew assembleFdroidDebug

# Release build
./gradlew assembleFdroidRelease
```

## Testing

```bash
# Unit tests
./gradlew testFdroidDebugUnitTest

# Lint
./gradlew lintFdroidRelease
```

## Dependencies

All dependencies are FOSS-compatible and available from Maven Central or Google's Maven repository.

## Notes for F-Droid Maintainers

- The app uses native Monero libraries (C++) via JNI
- Native libraries are included in the `app/src/main/jniLibs` directory
- The `align_elf.py` script is required for 16KB page size compatibility
- Build flavor: `fdroid`
- Minimum SDK: 24 (Android 7.0)
- Target SDK: 35 (Android 15)
EOF
echo -e "${GREEN}✓ Created FDROID.md${NC}"

# 6. Update README for F-Droid branch
echo ""
echo "6. Creating F-Droid specific README..."
cat > README.fdroid.md << 'EOF'
# Àpò - Monero Wallet for Android (F-Droid Edition)

A native Android Monero wallet application built with Kotlin and Jetpack Compose.

**This is the F-Droid compatible branch** - contains only FOSS dependencies.

## Features

- ✅ Create and restore Monero wallets
- ✅ Send and receive XMR transactions
- ✅ Real-time blockchain synchronization
- ✅ Transaction history with search
- ✅ Blockchain rescan functionality
- ✅ QR code support (ZXing-based)
- ✅ Biometric authentication
- ✅ Modern Material Design 3 UI
- ✅ 100% FOSS - No proprietary dependencies

## Technology Stack

- **Language**: Kotlin + Java
- **UI**: Jetpack Compose with Material Design 3
- **Wallet**: Monero C++ libraries (via JNI)
- **Architecture**: MVVM with coroutines
- **QR Codes**: ZXing (FOSS alternative to ML Kit)

## Building

1. Clone the repository and checkout the `fdroid` branch
2. Build with Gradle:

```bash
./gradlew assembleRelease
```

## Differences from Play Store Version

The F-Droid version uses only FOSS dependencies:

| Feature | F-Droid | Play Store |
|---------|---------|------------|
| QR Scanning | ZXing | ML Kit |
| Crash Reporting | Timber logs | Firebase Crashlytics |
| Analytics | None | Firebase Analytics |

## Security

⚠️ **IMPORTANT**: Never commit wallet files, keys, or seed phrases to version control!

## License

MIT

## Links

- Main Repository: https://github.com/niyid/apo
- Issues: https://github.com/niyid/apo/issues
- F-Droid: https://f-droid.org/packages/com.techducat.apo
EOF
echo -e "${GREEN}✓ Created README.fdroid.md${NC}"

# 7. Create .fdroidignore file
echo ""
echo "7. Creating .fdroidignore..."
cat > .fdroidignore << 'EOF'
# Files to ignore in F-Droid builds
google-services.json
app/google-services.json
*.jks
*.keystore
keystore.properties
wallet.properties
EOF
echo -e "${GREEN}✓ Created .fdroidignore${NC}"

echo ""
echo -e "${GREEN}=========================================="
echo "F-Droid preparation complete!"
echo "==========================================${NC}"
echo ""
echo "Created/Updated files:"
echo "  - app/build.gradle.kts (F-Droid compatible)"
echo "  - fastlane/metadata/ (App metadata)"
echo "  - FDROID.md (Build instructions)"
echo "  - README.fdroid.md (F-Droid README)"
echo "  - .fdroidignore (Ignore list)"
echo ""
