import java.util.Properties
import org.gradle.process.ExecOperations
import org.gradle.kotlin.dsl.support.serviceOf

// SECURED build.gradle.kts
// Security improvements:
// - No hardcoded API keys
// - Secrets loaded from local.properties
// - ProGuard enabled for release
// - Debug symbols removed
// - Code obfuscation enabled

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.parcelize")
    id("androidx.navigation.safeargs.kotlin")
}

// ===== SECURITY: Load secrets from local.properties =====
// local.properties is NOT committed to version control
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

// Helper function to get property with fallback
fun getLocalProperty(key: String, defaultValue: String = ""): String {
    return localProperties.getProperty(key) ?: System.getenv(key) ?: defaultValue
}

// ── Signing & environment properties (-P flags or env vars) ──────────────────
val keyAlias      = findProperty("KEY_ALIAS")?.toString()      ?: System.getenv("VERZUS_KEY_ALIAS")      ?: ""
val keyPassword   = findProperty("KEY_PASSWORD")?.toString()   ?: System.getenv("VERZUS_KEY_PASSWORD")   ?: ""
val keystorePath  = findProperty("KEYSTORE")?.toString()       ?: System.getenv("VERZUS_KEYSTORE")        ?: ""
val storePassword = findProperty("STORE_PASSWORD")?.toString() ?: System.getenv("VERZUS_STORE_PASSWORD")  ?: ""
val buildEnv      = findProperty("BUILD_ENV")?.toString()      ?: "STAGENET"
val isMainnet     = buildEnv.uppercase() == "MAINNET"

android {
    namespace = "com.techducat.apo"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.techducat.apo"
        minSdk = 24
        targetSdk = 35
        versionCode = project.property("VERSION_CODE").toString().toInt()
        versionName = project.property("VERSION_NAME").toString()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        resourceConfigurations += listOf(
            "en",     // English
            "es",     // Spanish
            "fr",     // French
            "de",     // German
            "it",     // Italian
            "pt",     // Portuguese
            "ja",     // Japanese
            "ko",     // Korean
            "zh",     // Chinese
            "ar",     // Arabic
            "ru",     // Russian
        )

        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
        }

        buildConfigField("boolean", "IS_MAINNET", "$isMainnet")
        buildConfigField("String",  "SERVER_ENV", "\"$buildEnv\"")
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

        // SECURITY: Check for common security issues
        checkOnly += setOf(
            "HardcodedText",
            "SetJavaScriptEnabled",
            "ExportedContentProvider",
            "ExportedService",
            "ExportedReceiver"
        )
    }

    packaging.resources {
        excludes += setOf(
            "META-INF/DEPENDENCIES",
            "META-INF/LICENSE.md",
            "META-INF/NOTICE.md",
            "/META-INF/{AL2.0,LGPL2.1}"
        )
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

    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }

    sourceSets {
        getByName("main") {
            assets.srcDirs("$projectDir/schemas")
        }
    }

    // ===== SIGNING CONFIGS =====
    // SECURITY: Load from local.properties or CI environment variable.
    // No silent fallback — build fails explicitly if RELEASE_STORE_FILE is missing.
    //
    // Add to local.properties (never commit this file):
    //   RELEASE_STORE_FILE=/path/to/techducat.jks
    //   RELEASE_STORE_PASSWORD=your_store_password
    //   RELEASE_KEY_ALIAS=your_key_alias
    //   RELEASE_KEY_PASSWORD=your_key_password
    signingConfigs {
        create("release") {
            val resolvedKeystorePath = getLocalProperty("RELEASE_STORE_FILE")
            require(resolvedKeystorePath.isNotEmpty()) {
                "\n\n❌ RELEASE_STORE_FILE is not set!\n" +
                "Add to local.properties or as an environment variable:\n" +
                "  RELEASE_STORE_FILE=/path/to/techducat.jks\n"
            }
            storeFile     = file(resolvedKeystorePath)
            storePassword = getLocalProperty("RELEASE_STORE_PASSWORD")
            keyAlias      = getLocalProperty("RELEASE_KEY_ALIAS")
            keyPassword   = getLocalProperty("RELEASE_KEY_PASSWORD")
        }
    }

    // ===== FLAVOR DIMENSIONS =====
    // dimension "distribution" separates Play Store (proprietary) from F-Droid (FOSS-only).
    // All Play-Store-exclusive features (Firebase, ML Kit, signing) are scoped to
    // the `playstore` flavor. The `fdroid` flavor must remain free of proprietary deps.
    flavorDimensions += "distribution"

    productFlavors {
        // ── Play Store flavor ────────────────────────────────────────────────────
        // Includes: Firebase Crashlytics, Firebase Analytics, ML Kit barcode scanning,
        //           Google Services plugin, release signing config.
        create("playstore") {
            dimension = "distribution"
            buildConfigField("boolean", "IS_FDROID", "false")
            buildConfigField("boolean", "HAS_MLKIT",   "true")

            // Play Store release builds must be signed with the release keystore.
            // Applying signingConfig here (flavor-level) instead of in the release
            // buildType ensures F-Droid release builds are NOT signed by our keystore
            // (F-Droid signs its own APKs during their build process).
            signingConfig = signingConfigs.getByName("release")
        }

        // ── F-Droid flavor ───────────────────────────────────────────────────────
        // FOSS-only: no Firebase, no ML Kit, no proprietary Google dependencies.
        // F-Droid's build server will sign the APK with their own key.
        // ZXing is used for barcode scanning as a fully open-source alternative.
        create("fdroid") {
            dimension = "distribution"
            buildConfigField("boolean", "IS_FDROID",   "true")
            buildConfigField("boolean", "HAS_MLKIT",   "false")

            // No signingConfig: F-Droid's build server handles signing independently.
        }
    }

    // ===== BUILD TYPES =====
    // Build types define debug vs. release behaviour — shared across both flavors.
    // Flavor-specific concerns (Firebase, signing) live in productFlavors above,
    // NOT here, to prevent Play Store config leaking into F-Droid builds.
    buildTypes {
        release {
            // ===== SECURITY: Load API keys from environment/local.properties =====
            // NEVER hardcode secrets in build files!
            buildConfigField(
                "String",
                "SERVICE_BACKEND_URL",
                "\"${getLocalProperty("SERVICE_BACKEND_URL", "")}\""
            )
            buildConfigField(
                "String",
                "SERVICE_API_KEY",
                "\"${getLocalProperty("SERVICE_API_KEY", "")}\""
            )
            buildConfigField(
                "String",
                "BACKEND_URL",
                "\"${getLocalProperty("BACKEND_URL", "")}\""
            )

            // ===== SECURITY: Enable code obfuscation =====
            isMinifyEnabled = true
            isShrinkResources = true

            // ===== SECURITY: ProGuard configuration =====
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // NOTE: signingConfig intentionally NOT set here.
            //       The `playstore` flavor sets it in productFlavors above so that
            //       `fdroidRelease` is never signed with the Play Store keystore.

            // ===== SECURITY: Disable debugging =====
            isDebuggable = false
            isJniDebuggable = false

            // ===== SECURITY: Remove debug symbols =====
            ndk {
                debugSymbolLevel = "NONE"
            }
        }

        debug {
            // Debug configuration — uses localhost/emulator placeholder values.
            // Safe defaults keep the build runnable without a local.properties entry.
            buildConfigField(
                "String",
                "SERVICE_BACKEND_URL",
                "\"${getLocalProperty("DEBUG_SERVICE_BACKEND_URL", "http://10.0.2.2:8080")}\""
            )
            buildConfigField(
                "String",
                "SERVICE_API_KEY",
                "\"${getLocalProperty("DEBUG_SERVICE_API_KEY", "debug-key")}\""
            )
            buildConfigField(
                "String",
                "BACKEND_URL",
                "\"${getLocalProperty("DEBUG_BACKEND_URL", "http://10.0.2.2:3000")}\""
            )

            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
        }
    }

    // ===== JNI PACKAGING (Monerujo native libs) =====
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

    buildFeatures {
        buildConfig = true
        compose = true
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // Required for Java 8+ API support on older Android versions
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

// ===== 16KB ELF ALIGNMENT (Android 16KB page size requirement) =====
//
// WHY: Android devices with 16KB page sizes (Pixel 9+, future ARM SoCs) require
// ELF PT_LOAD segments to be aligned to 16384 bytes. Google Play will flag APKs
// whose .so files are not aligned and will block them from 16KB-page devices.
//
// SCOPE: Only applied to `playstore` variants — F-Droid builds their own APKs
// independently and the alignment check references Play Store submission policy.
//
// HOW: Tools shared across TechDucat projects in ~/git/server_extras/:
//   align_elf.py           – patches PT_LOAD p_align header field to 16384 in-place
//   check_elf_alignment.sh – uses objdump to verify; reports ALIGNED / UNALIGNED
//
// NOTE: useLegacyPackaging = false (set above in jniLibs) is equally required.

val serverExtras = "${System.getProperty("user.home")}/git/server_extras"
val alignElfPy   = "$serverExtras/align_elf.py"
val checkElfSh   = "$serverExtras/check_elf_alignment.sh"

androidComponents {
    val execOps: ExecOperations = project.serviceOf<ExecOperations>()
    onVariants { variant ->
        // ── Scope ELF alignment to Play Store variants only ──────────────────────
        // F-Droid builds are excluded: F-Droid's infrastructure handles its own
        // APK post-processing, and the check message references Play Store rejection.
        if (!variant.name.contains("playstore", ignoreCase = true)) return@onVariants

        val variantName    = variant.name
        val variantNameCap = variantName.replaceFirstChar { it.uppercase() }

        val strippedDir = layout.buildDirectory.dir(
            "intermediates/stripped_native_libs/$variantName/out/lib"
        )
        val mergedDir = layout.buildDirectory.dir(
            "intermediates/merged_native_libs/$variantName/out/lib"
        )

        // ── Task 1: alignElf<Variant> ─────────────────────────────────────────
        val alignTask = tasks.register("alignElf${variantNameCap}") {
            description = "Patch PT_LOAD p_align to 16384 bytes for all .so files in variant $variantName"
            group       = "build"

            dependsOn(providers.provider {
                val stripTask = "strip${variantNameCap}DebugSymbols"
                val mergeTask = "merge${variantNameCap}NativeLibs"
                listOfNotNull(
                    runCatching { tasks.named(stripTask) }.getOrNull(),
                    runCatching { tasks.named(mergeTask) }.getOrNull()
                )
            })

            inputs.files(strippedDir, mergedDir)
            outputs.files(strippedDir, mergedDir)

            doFirst {
                if (!file(alignElfPy).exists())
                    throw GradleException(
                        "\n❌ align_elf.py not found at: $alignElfPy\n" +
                        "   Copy it from the Ajo project or clone server_extras."
                    )
            }

            doLast {
                var patched = 0
                var skipped = 0

                for (root in listOf(strippedDir.get().asFile, mergedDir.get().asFile)) {
                    if (!root.exists()) continue

                    root.walkTopDown()
                        .filter { it.isFile && it.extension == "so" }
                        .forEach { so ->
                            val tmp = File("${so.absolutePath}.16kb_tmp")
                            try {
                                val result = execOps.exec {
                                    commandLine("python3", alignElfPy, so.absolutePath, tmp.absolutePath)
                                    isIgnoreExitValue = true
                                }
                                if (result.exitValue == 0 && tmp.exists()) {
                                    tmp.renameTo(so)
                                    patched++
                                    logger.lifecycle("  ✓ aligned  [${so.parentFile.name}] ${so.name}")
                                } else {
                                    skipped++
                                    logger.warn("  ⚠ skipped  [${so.parentFile.name}] ${so.name} (not ELF or script error)")
                                }
                            } finally {
                                if (tmp.exists()) tmp.delete()
                            }
                        }
                }

                logger.lifecycle("=== alignElf${variantNameCap}: $patched aligned, $skipped skipped ===")
                if (patched == 0 && skipped == 0)
                    logger.warn("  ⚠ No .so files found — is this a native build? Check abiFilters.")
            }
        }

        // ── Task 2: checkElfAlignment<Variant> ───────────────────────────────
        val checkTask = tasks.register("checkElfAlignment${variantNameCap}") {
            description = "Verify 16KB ELF alignment for all .so files in variant $variantName"
            group       = "verification"

            dependsOn(alignTask)

            inputs.files(strippedDir)

            doFirst {
                if (!file(checkElfSh).exists())
                    throw GradleException(
                        "\n❌ check_elf_alignment.sh not found at: $checkElfSh\n" +
                        "   Copy it from the Ajo project or clone server_extras."
                    )
            }

            doLast {
                val soDir = strippedDir.get().asFile
                if (!soDir.exists()) {
                    logger.warn("checkElfAlignment${variantNameCap}: no stripped libs dir — skipping verification.")
                    return@doLast
                }

                logger.lifecycle("=== checkElfAlignment${variantNameCap}: verifying $soDir ===")
                val result = execOps.exec {
                    commandLine("bash", checkElfSh, soDir.absolutePath)
                    isIgnoreExitValue = true
                }
                if (result.exitValue != 0) {
                    throw GradleException(
                        "\n❌ 16KB alignment check FAILED for variant $variantName!\n" +
                        "   One or more .so files are not aligned to 16384 bytes.\n" +
                        "   Re-run with --info for details, or check the output above.\n" +
                        "   APK packaging has been blocked to prevent Play Store rejection."
                    )
                }
                logger.lifecycle("=== checkElfAlignment${variantNameCap}: all libraries ALIGNED ✓ ===")
            }
        }

        // ── Hook: block APK packaging until alignment is verified ────────────
        tasks.configureEach {
            if (name == "package${variantNameCap}") {
                dependsOn(checkTask)
            }
        }
    }
}
// ===== END 16KB ELF ALIGNMENT =====

dependencies {

    // ===== CORE LIBRARY DESUGARING =====
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // ===== LOMBOK =====
    compileOnly("org.projectlombok:lombok:1.18.34")
    annotationProcessor("org.projectlombok:lombok:1.18.34")
    testCompileOnly("org.projectlombok:lombok:1.18.34")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.34")

    // ===== COMPOSE =====
    implementation(platform("androidx.compose:compose-bom:2025.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended:1.7.6")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // ===== CORE ANDROID =====
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // ===== LIFECYCLE =====
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")
    implementation("androidx.lifecycle:lifecycle-service:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.9.0")

    // ===== NAVIGATION =====
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.5")

    // ===== ROOM DATABASE =====
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // ===== COROUTINES =====
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

    // ===== SERIALIZATION =====
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // ===== DEPENDENCY INJECTION (Koin) =====
    implementation("io.insert-koin:koin-android:4.0.0")
    implementation("io.insert-koin:koin-androidx-navigation:4.0.0")

    // ===== WORKMANAGER =====
    implementation("androidx.work:work-runtime-ktx:2.8.1")

    // ===== NETWORKING =====
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    // ===== SECURITY =====
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("com.google.crypto.tink:tink-android:1.10.0")
    implementation("androidx.biometric:biometric:1.2.0-alpha05")

    // ===== QR / BARCODE SCANNING =====
    // ZXing: FOSS implementation used by ALL builds (both Play Store and F-Droid).
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    // ML Kit: proprietary Google library — Play Store flavor ONLY.
    // F-Droid builds fall back to ZXing above; this dep is never included in fdroid variants.
    "playstoreImplementation"("com.google.mlkit:barcode-scanning:17.2.0")

    // Camera (shared — used by both flavors for QR scanning via ZXing or ML Kit)
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")

    // ===== GSON =====
    implementation("com.google.code.gson:gson:2.11.0")

    // ===== LOGGING =====
    implementation("com.jakewharton.timber:timber:5.0.1")

    // ===== PROTOBUF & gRPC =====
    implementation("org.json:json:20231013")
    implementation("com.google.protobuf:protobuf-kotlin:3.22.3")
    implementation("com.google.protobuf:protobuf-java:3.22.3")
    implementation("com.google.protobuf:protobuf-java-util:3.22.3")
    implementation("io.grpc:grpc-stub:1.57.0")
    implementation("io.grpc:grpc-kotlin-stub:1.3.0")
    implementation("io.grpc:grpc-protobuf:1.57.0")
    implementation("io.grpc:grpc-netty-shaded:1.57.0")

    // ===== TESTING =====
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
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("io.mockk:mockk-android:1.13.8")

    androidTestImplementation(platform("androidx.compose:compose-bom:2025.08.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.navigation:navigation-testing:2.8.5")
}

// ===== SECURITY NOTES FOR local.properties =====
// Create a local.properties file in your project root with:
/*
# Release signing (Play Store only — never commit this file)
RELEASE_STORE_FILE=/path/to/techducat.jks
RELEASE_STORE_PASSWORD=your_store_password
RELEASE_KEY_ALIAS=your_key_alias
RELEASE_KEY_PASSWORD=your_key_password

# Production API keys
SERVICE_BACKEND_URL=https://apo-backend.fly.dev
SERVICE_API_KEY=your_actual_service_api_key
BACKEND_URL=https://api.yourapp.com

# Debug API keys (optional — defaults to emulator localhost if unset)
DEBUG_SERVICE_BACKEND_URL=http://10.0.2.2:8080
DEBUG_SERVICE_API_KEY=debug-service-key
DEBUG_BACKEND_URL=http://10.0.2.2:3000
*/
