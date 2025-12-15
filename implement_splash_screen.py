#!/usr/bin/env python3
"""
Splash Screen Implementation Script for Apo Monero Wallet
Automatically implements splash screen with animation across all Android versions
"""

import os
import sys
import re
from pathlib import Path

# ANSI color codes for pretty output
class Colors:
    GREEN = '\033[92m'
    YELLOW = '\033[93m'
    RED = '\033[91m'
    BLUE = '\033[94m'
    BOLD = '\033[1m'
    END = '\033[0m'

def print_step(msg):
    print(f"{Colors.BLUE}{Colors.BOLD}==> {msg}{Colors.END}")

def print_success(msg):
    print(f"{Colors.GREEN}✓ {msg}{Colors.END}")

def print_warning(msg):
    print(f"{Colors.YELLOW}⚠ {msg}{Colors.END}")

def print_error(msg):
    print(f"{Colors.RED}✗ {msg}{Colors.END}")

# File contents - Updated for actual project structure
SPLASH_COLOR = '''    <!-- Splash screen background color (matches your dark theme) -->
    <color name="splash_background">#0F0F0F</color>'''

# Updated for Theme.Apo parent
SPLASH_THEME_VALUES = '''
    <!-- Splash Screen Theme for all Android versions -->
    <style name="Theme.Apo.Splash" parent="Theme.SplashScreen">
        <!-- Will be overridden by values-v31 for Android 12+ -->
    </style>'''

SPLASH_THEME_V31 = '''<?xml version="1.0" encoding="utf-8"?>
<!-- Android 12+ (API 31+) Splash Screen Theme -->
<resources>
    <style name="Theme.Apo.Splash" parent="Theme.SplashScreen">
        <!-- Set the splash screen background color -->
        <item name="windowSplashScreenBackground">@color/splash_background</item>
        
        <!-- Set the splash screen icon (your animated drawable) -->
        <item name="windowSplashScreenAnimatedIcon">@drawable/apo_sack_anim</item>
        
        <!-- Icon animation duration in milliseconds -->
        <item name="windowSplashScreenAnimationDuration">1000</item>
        
        <!-- Set the theme to use after splash -->
        <item name="postSplashScreenTheme">@style/MyMaterialTheme</item>
        
        <!-- Optional: Icon background (transparent to show animation) -->
        <item name="windowSplashScreenIconBackgroundColor">@android:color/transparent</item>
    </style>
</resources>'''

SPLASH_BACKGROUND_XML = '''<?xml version="1.0" encoding="utf-8"?>
<!-- Legacy splash screen background for Android < 12 -->
<layer-list xmlns:android="http://schemas.android.com/apk/res/android"
    android:opacity="opaque">
    
    <!-- Background color -->
    <item android:drawable="@color/splash_background"/>
    
    <!-- Centered logo -->
    <item>
        <bitmap
            android:gravity="center"
            android:src="@mipmap/ic_launcher_foreground"/>
    </item>
</layer-list>'''

# Update Theme.SplashScreen to use splash_background drawable for pre-Android 12
UPDATED_SPLASH_SCREEN_THEME = '''    <!-- Splash Screen Theme (uses splash_background for pre-Android 12) -->
    <style name="Theme.SplashScreen" parent="Theme.AppCompat.DayNight.NoActionBar">
        <item name="android:windowBackground">@drawable/splash_background</item>
        <item name="android:statusBarColor">@color/splash_background</item>
        <item name="android:navigationBarColor">@color/splash_background</item>
        <item name="android:windowDrawsSystemBarBackgrounds">true</item>
    </style>'''

KOTLIN_IMPORTS = '''import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch'''

KOTLIN_ONCREATE = '''    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen (Android 12+)
        val splashScreen = installSplashScreen()
        
        super.onCreate(savedInstanceState)
        
        // Keep splash screen visible while wallet initializes
        var keepSplashOnScreen = true
        splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }
        
        walletSuite = WalletSuite.getInstance(this)
        
        setContent {
            MoneroWalletTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MoneroWalletScreen(walletSuite)
                }
            }
        }
        
        // Hide splash screen after wallet is initialized
        lifecycleScope.launch {
            delay(1500) // Minimum splash duration
            keepSplashOnScreen = false
        }
    }'''

def find_project_root():
    """Find the Android project root directory"""
    current = Path.cwd()
    
    # Check if we're already in the project root
    if (current / "app" / "src" / "main").exists():
        return current
    
    # Check parent directories
    for parent in current.parents:
        if (parent / "app" / "src" / "main").exists():
            return parent
    
    print_error("Could not find Android project root!")
    print_warning("Please run this script from your project directory")
    sys.exit(1)

def create_file(path, content):
    """Create a file with the given content"""
    try:
        path.parent.mkdir(parents=True, exist_ok=True)
        with open(path, 'w', encoding='utf-8') as f:
            f.write(content)
        print_success(f"Created: {path.relative_to(Path.cwd())}")
        return True
    except Exception as e:
        print_error(f"Failed to create {path}: {e}")
        return False

def update_colors_xml(colors_path):
    """Add splash color to colors.xml"""
    print_step("Updating colors.xml")
    
    if not colors_path.exists():
        print_warning(f"colors.xml not found at {colors_path}")
        return False
    
    with open(colors_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Check if splash_background already exists
    if 'splash_background' in content:
        print_warning("splash_background color already exists")
        return True
    
    # Add color before closing </resources> tag
    if '</resources>' in content:
        content = content.replace('</resources>', f'{SPLASH_COLOR}\n</resources>')
        with open(colors_path, 'w', encoding='utf-8') as f:
            f.write(content)
        print_success("Added splash_background color")
        return True
    else:
        print_error("Could not find </resources> tag in colors.xml")
        return False

def update_themes_xml(themes_path):
    """Update Theme.SplashScreen to use splash_background drawable"""
    print_step("Updating themes.xml")
    
    if not themes_path.exists():
        print_warning(f"themes.xml not found at {themes_path}")
        return False
    
    with open(themes_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Check if already updated
    if 'splash_background' in content and 'drawable' in content:
        print_warning("themes.xml already references splash_background drawable")
        return True
    
    # Replace the existing Theme.SplashScreen definition
    pattern = r'<!-- Splash Screen Theme.*?</style>'
    if re.search(pattern, content, re.DOTALL):
        content = re.sub(pattern, UPDATED_SPLASH_SCREEN_THEME.strip(), content, flags=re.DOTALL)
        
        # Also add Theme.Apo.Splash if it doesn't exist
        if 'Theme.Apo.Splash' not in content:
            content = content.replace('</resources>', f'{SPLASH_THEME_VALUES}\n</resources>')
        
        with open(themes_path, 'w', encoding='utf-8') as f:
            f.write(content)
        print_success("Updated Theme.SplashScreen to use splash_background drawable")
        return True
    else:
        print_warning("Could not find Theme.SplashScreen in themes.xml")
        return False

def update_manifest(manifest_path):
    """Update AndroidManifest.xml to use splash theme"""
    print_step("Updating AndroidManifest.xml")
    
    if not manifest_path.exists():
        print_error(f"AndroidManifest.xml not found at {manifest_path}")
        return False
    
    with open(manifest_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Check if already using splash theme
    if 'Theme.Apo.Splash' in content:
        print_warning("AndroidManifest already uses Theme.Apo.Splash")
        return True
    
    # Replace theme in MoneroWalletActivity
    pattern = r'(<activity\s+android:name="\.MoneroWalletActivity"[^>]*android:theme=")@style/Theme\.Apo(")'
    replacement = r'\1@style/Theme.Apo.Splash\2'
    
    new_content, count = re.subn(pattern, replacement, content)
    
    if count > 0:
        with open(manifest_path, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print_success("Updated MoneroWalletActivity theme to Theme.Apo.Splash")
        return True
    else:
        print_warning("Could not find MoneroWalletActivity theme to update")
        print_warning("You may need to manually change the theme to @style/Theme.Apo.Splash")
        return False

def update_kotlin_activity(kotlin_path):
    """Update MoneroWalletActivity.kt to include splash screen code"""
    print_step("Updating MoneroWalletActivity.kt")
    
    if not kotlin_path.exists():
        print_error(f"MoneroWalletActivity.kt not found at {kotlin_path}")
        return False
    
    with open(kotlin_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Check if already has splash screen code
    if 'installSplashScreen' in content:
        print_warning("MoneroWalletActivity.kt already has splash screen code")
        return True
    
    # Add imports after package declaration
    package_pattern = r'(package com\.techducat\.apo\s*\n)'
    if not re.search(package_pattern, content):
        print_error("Could not find package declaration")
        return False
    
    # Add imports after existing imports
    import_pattern = r'(import[^\n]+\n)+'
    match = re.search(import_pattern, content)
    if match:
        insert_pos = match.end()
        content = content[:insert_pos] + '\n' + KOTLIN_IMPORTS + '\n' + content[insert_pos:]
    
    # Replace onCreate method
    oncreate_pattern = r'override fun onCreate\(savedInstanceState: Bundle\?\) \{[^}]*\n\s+walletSuite = WalletSuite\.getInstance\(this\)[^}]*\n\s+setContent \{[^}]+\}[^}]+\}\s+\}'
    
    if re.search(oncreate_pattern, content, re.DOTALL):
        content = re.sub(oncreate_pattern, KOTLIN_ONCREATE, content, flags=re.DOTALL)
        with open(kotlin_path, 'w', encoding='utf-8') as f:
            f.write(content)
        print_success("Updated onCreate method with splash screen code")
        return True
    else:
        print_warning("Could not automatically update onCreate method")
        print_warning("Please manually add splash screen code to MoneroWalletActivity.kt")
        return False

def update_gradle(gradle_path):
    """Add splash screen dependency to build.gradle.kts"""
    print_step("Updating build.gradle.kts")
    
    if not gradle_path.exists():
        print_error(f"build.gradle.kts not found at {gradle_path}")
        return False
    
    with open(gradle_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Check if dependency already exists
    if 'core-splashscreen' in content:
        print_warning("Splash screen dependency already exists")
        return True
    
    # Find dependencies block and add splash screen
    dependency = '    implementation("androidx.core:core-splashscreen:1.0.1")'
    
    # Try to add after other androidx.core dependencies
    pattern = r'(implementation\("androidx\.core:core[^"]+"\))'
    match = re.search(pattern, content)
    
    if match:
        insert_pos = match.end()
        content = content[:insert_pos] + '\n' + dependency + content[insert_pos:]
    else:
        # Fallback: add in dependencies block
        pattern = r'(dependencies \{)'
        if re.search(pattern, content):
            content = re.sub(pattern, r'\1\n' + dependency, content)
        else:
            print_error("Could not find dependencies block in build.gradle.kts")
            return False
    
    with open(gradle_path, 'w', encoding='utf-8') as f:
        f.write(content)
    print_success("Added splash screen dependency")
    return True

def main():
    print(f"\n{Colors.BOLD}{'='*60}")
    print("  Apo Monero Wallet - Splash Screen Implementation")
    print(f"{'='*60}{Colors.END}\n")
    
    # Find project root
    project_root = find_project_root()
    print_success(f"Found project root: {project_root}\n")
    
    # Define paths
    res_dir = project_root / "app" / "src" / "main" / "res"
    kotlin_dir = project_root / "app" / "src" / "main" / "java" / "com" / "techducat" / "apo"
    
    # Alternative kotlin path
    if not kotlin_dir.exists():
        kotlin_dir = project_root / "app" / "src" / "main" / "kotlin" / "com" / "techducat" / "apo"
    
    manifest_path = project_root / "app" / "src" / "main" / "AndroidManifest.xml"
    gradle_path = project_root / "app" / "build.gradle.kts"
    
    colors_path = res_dir / "values" / "colors.xml"
    themes_path = res_dir / "values" / "themes.xml"
    themes_v31_path = res_dir / "values-v31" / "themes.xml"
    splash_bg_path = res_dir / "drawable" / "splash_background.xml"
    kotlin_path = kotlin_dir / "MoneroWalletActivity.kt"
    
    success_count = 0
    total_steps = 6
    
    # Step 1: Create values-v31/themes.xml
    if create_file(themes_v31_path, SPLASH_THEME_V31):
        success_count += 1
    
    # Step 2: Create drawable/splash_background.xml
    if create_file(splash_bg_path, SPLASH_BACKGROUND_XML):
        success_count += 1
    
    # Step 3: Update colors.xml
    if update_colors_xml(colors_path):
        success_count += 1
    
    # Step 4: Update themes.xml
    if update_themes_xml(themes_path):
        success_count += 1
    
    # Step 5: Update AndroidManifest.xml
    if update_manifest(manifest_path):
        success_count += 1
    
    # Step 6: Update build.gradle.kts
    if update_gradle(gradle_path):
        success_count += 1
    
    # Note about Kotlin update
    print(f"\n{Colors.YELLOW}Note: Kotlin activity update requires manual review{Colors.END}")
    print("The script can attempt to update MoneroWalletActivity.kt, but please review the changes.")
    response = input(f"{Colors.BOLD}Update MoneroWalletActivity.kt? (y/n): {Colors.END}").strip().lower()
    
    if response == 'y':
        if update_kotlin_activity(kotlin_path):
            success_count += 1
        total_steps += 1
    else:
        print_warning("Skipped MoneroWalletActivity.kt update. Please update manually.")
        print(f"\n{Colors.BOLD}Manual steps for MoneroWalletActivity.kt:{Colors.END}")
        print("1. Add these imports at the top:")
        print(f"{Colors.BLUE}{KOTLIN_IMPORTS}{Colors.END}")
        print("\n2. Replace onCreate() method with:")
        print(f"{Colors.BLUE}{KOTLIN_ONCREATE}{Colors.END}\n")
    
    # Summary
    print(f"\n{Colors.BOLD}{'='*60}")
    print(f"  Implementation Complete: {success_count}/{total_steps} steps successful")
    print(f"{'='*60}{Colors.END}\n")
    
    if success_count >= 6:  # All automatic steps successful
        print_success("Core splash screen setup completed! ✨")
        print(f"\n{Colors.BOLD}Next steps:{Colors.END}")
        print("  1. Review the changes (especially in MoneroWalletActivity.kt)")
        print("  2. Sync Gradle (if using Android Studio)")
        print("  3. Clean and rebuild the project")
        print("  4. Uninstall old app version from device")
        print("  5. Install and test the splash screen!")
    else:
        print_warning(f"Some steps failed or were skipped ({total_steps - success_count} issues)")
        print("\nPlease review the warnings above and apply manual fixes if needed.")
    
    print(f"\n{Colors.BLUE}Splash screen will show 'apo_sack_anim' animation on launch! 🎉{Colors.END}\n")

if __name__ == "__main__":
    main()
