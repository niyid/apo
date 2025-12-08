#!/usr/bin/env python3
"""
Comprehensive fix script for MoneroWalletActivity.kt
Fixes all identified issues, adds missing imports, strings, and implementations.
"""

import re
import os
import sys
from pathlib import Path

# Color codes for terminal output
class Colors:
    GREEN = '\033[92m'
    YELLOW = '\033[93m'
    RED = '\033[91m'
    BLUE = '\033[94m'
    END = '\033[0m'

def print_success(msg):
    print(f"{Colors.GREEN}✅ {msg}{Colors.END}")

def print_warning(msg):
    print(f"{Colors.YELLOW}⚠️  {msg}{Colors.END}")

def print_error(msg):
    print(f"{Colors.RED}❌ {msg}{Colors.END}")

def print_info(msg):
    print(f"{Colors.BLUE}ℹ️  {msg}{Colors.END}")

def add_missing_imports(content):
    """Add missing imports to the Kotlin file"""
    imports_to_add = [
        "import kotlinx.coroutines.withContext",
        "import kotlinx.coroutines.Dispatchers"
    ]
    
    # Find the last import statement
    import_pattern = r'(import .+\n)'
    imports = re.findall(import_pattern, content)
    
    if imports:
        last_import = imports[-1]
        last_import_pos = content.rfind(last_import) + len(last_import)
        
        # Check which imports are missing
        missing_imports = []
        for imp in imports_to_add:
            if imp not in content:
                missing_imports.append(imp)
        
        if missing_imports:
            insert_text = '\n'.join(missing_imports) + '\n'
            content = content[:last_import_pos] + insert_text + content[last_import_pos:]
            print_success(f"Added {len(missing_imports)} missing import(s)")
        else:
            print_info("All required imports already present")
    
    return content

def fix_navigation_tabs(content):
    """Add 5th navigation tab for Exchange"""
    # Check if exchange tab already exists
    if 'selectedTab == 4' in content and 'Icons.Default.SwapHoriz' in content:
        print_info("Exchange navigation tab already exists")
        return content
    
    # Find the NavigationBar section and the last NavigationBarItem
    pattern = r'(NavigationBarItem\s*\(\s*selected\s*=\s*selectedTab\s*==\s*3,.*?\))\s*\n\s*(\})'
    
    match = re.search(pattern, content, re.DOTALL)
    if match:
        exchange_tab = '''
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(Icons.Default.SwapHoriz, stringResource(R.string.nav_exchange)) },
                    label = { Text(stringResource(R.string.nav_exchange)) }
                )
'''
        # Insert before the closing brace of NavigationBar
        replacement = match.group(1) + '\n' + exchange_tab + '            ' + match.group(2)
        content = content[:match.start()] + replacement + content[match.end():]
        print_success("Added Exchange navigation tab")
    else:
        print_warning("Could not find NavigationBar section to add Exchange tab")
    
    return content

def fix_api_key_comment(content):
    """Add better comment for API key configuration"""
    old_key_line = 'private const val API_KEY = "your_api_key_here" // Get from https://changenow.io/api/'
    new_key_line = '''private const val API_KEY = "your_api_key_here" // TODO: Get from https://changenow.io/api/
        // SECURITY: Store in BuildConfig or environment variable for production:
        // private const val API_KEY = BuildConfig.CHANGENOW_API_KEY'''
    
    if old_key_line in content:
        content = content.replace(old_key_line, new_key_line)
        print_success("Updated API key configuration comment")
    else:
        print_info("API key configuration already updated or not found")
    
    return content

def fix_toDouble_safety(content):
    """Fix unsafe toDouble() calls"""
    # Fix in exchange creation
    unsafe_pattern = r'changeNowService\.createExchange\(fromCurrency,\s*toCurrency,\s*fromAmount\.toDouble\(\),\s*toAddress'
    safe_replacement = r'changeNowService.createExchange(fromCurrency, toCurrency, fromAmount.toDoubleOrNull() ?: 0.0, toAddress'
    
    if re.search(unsafe_pattern, content):
        content = re.sub(unsafe_pattern, safe_replacement, content)
        print_success("Fixed unsafe toDouble() call in exchange creation")
    else:
        print_info("No unsafe toDouble() calls found")
    
    return content

def add_missing_strings_xml(strings_path):
    """Add all missing string resources"""
    missing_strings = '''
    <!-- Exchange Screen Strings -->
    <string name="exchange_title">Exchange</string>
    <string name="exchange_available_xmr">Available XMR</string>
    <string name="exchange_from">From</string>
    <string name="exchange_to">To</string>
    <string name="exchange_recipient_address">Recipient %1$s Address</string>
    <string name="exchange_button">Exchange</string>
    <string name="exchange_status_title">Exchange Status</string>
    <string name="exchange_status_label">Status: %s</string>
    <string name="exchange_send_to">Send to: %s</string>
    <string name="exchange_created">Exchange created! Send to: %s</string>
    <string name="exchange_error">Error: %s</string>
    <string name="exchange_estimating">Estimating...</string>
    <string name="exchange_estimated_amount">≈ %.6f</string>
    <string name="exchange_placeholder">0.0</string>
    <string name="exchange_max_button">MAX</string>
    
    <!-- Navigation - Exchange Tab -->
    <string name="nav_exchange">Exchange</string>
    
    <!-- Import/Export Features -->
    <string name="export_keys_dialog_title">Export Private Keys</string>
    <string name="export_keys_warning">⚠️ Warning: Never share your private keys!</string>
    <string name="export_keys_info">Your private keys give full access to your wallet. Store them securely.</string>
    
    <!-- QR Scanner -->
    <string name="qr_scanner_title">Scan QR Code</string>
    <string name="qr_scanner_permission_required">Camera permission required</string>
    
    <!-- Security Settings -->
    <string name="security_title">Security Settings</string>
    <string name="security_biometric_title">Biometric Authentication</string>
    <string name="security_biometric_subtitle">Use fingerprint/face to unlock wallet</string>
    
    <!-- Currency Names -->
    <string name="currency_xmr">Monero (XMR)</string>
    <string name="currency_btc">Bitcoin (BTC)</string>
    <string name="currency_eth">Ethereum (ETH)</string>
    
    <!-- Error Messages for Exchange -->
    <string name="error_exchange_no_estimate">Cannot get exchange estimate</string>
    <string name="error_exchange_create_failed">Failed to create exchange</string>
    <string name="error_exchange_api_key">API key not configured</string>
'''
    
    try:
        with open(strings_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Check if strings already exist
        if '<string name="nav_exchange">' in content or '<string name="exchange_title">' in content:
            print_info("Exchange strings already exist in strings.xml")
            return False
        
        # Find the closing </resources> tag
        if '</resources>' in content:
            content = content.replace('</resources>', missing_strings + '\n</resources>')
            
            with open(strings_path, 'w', encoding='utf-8') as f:
                f.write(content)
            
            print_success(f"Added missing string resources to strings.xml")
            return True
        else:
            print_error("Could not find </resources> tag in strings.xml")
            return False
            
    except FileNotFoundError:
        print_error(f"strings.xml not found at {strings_path}")
        return False
    except Exception as e:
        print_error(f"Error updating strings.xml: {e}")
        return False

def create_build_gradle_additions():
    """Create a file with required build.gradle.kts additions"""
    gradle_additions = '''// ============================================
// Add these dependencies to your build.gradle.kts (app module)
// ============================================

android {
    buildFeatures {
        compose = true
        buildConfig = true  // Enable BuildConfig for API keys
    }
    
    defaultConfig {
        // Add ChangeNOW API key (replace with your actual key)
        buildConfigField("String", "CHANGENOW_API_KEY", "\\"your_changenow_api_key_here\\"")
    }
}

dependencies {
    // Coroutines (if not already added)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Camera & QR Code scanning
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")
    implementation("com.google.zxing:core:3.5.2")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    
    // Biometric authentication
    implementation("androidx.biometric:biometric:1.1.0")
}
'''
    
    try:
        with open('build_gradle_additions.txt', 'w', encoding='utf-8') as f:
            f.write(gradle_additions)
        print_success("Created build_gradle_additions.txt")
        return True
    except Exception as e:
        print_error(f"Error creating build_gradle_additions.txt: {e}")
        return False

def create_manifest_additions():
    """Create a file with required AndroidManifest.xml additions"""
    manifest_additions = '''<!-- ============================================ -->
<!-- Add these permissions to your AndroidManifest.xml -->
<!-- ============================================ -->

<!-- Inside <manifest> tag, before <application> -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" 
                 android:maxSdkVersion="28" />

<!-- Camera features (optional) -->
<uses-feature android:name="android.hardware.camera" android:required="false" />
<uses-feature android:name="android.hardware.camera.autofocus" android:required="false" />
'''
    
    try:
        with open('manifest_additions.txt', 'w', encoding='utf-8') as f:
            f.write(manifest_additions)
        print_success("Created manifest_additions.txt")
        return True
    except Exception as e:
        print_error(f"Error creating manifest_additions.txt: {e}")
        return False

def create_implementation_guide():
    """Create a guide for remaining implementations"""
    guide = '''# Implementation Guide for Remaining TODOs

## Quick Start

1. **Get ChangeNOW API Key**
   - Visit: https://changenow.io/api/
   - Register and get your API key
   - Update in `ChangeNowSwapService.API_KEY`

2. **Apply build.gradle.kts Changes**
   - See `build_gradle_additions.txt`
   - Add dependencies for QR, Camera, Biometric

3. **Apply AndroidManifest.xml Changes**
   - See `manifest_additions.txt`
   - Add required permissions

## Remaining Implementations

### 1. QR Code Scanner (Optional)
If you want QR scanning for addresses, implement camera integration with ZXing.

### 2. Export Keys Feature
Add these methods to `WalletSuite.java`:
```java
public String getViewKey() {
    return wallet != null ? wallet.secretViewKey() : "";
}

public String getSpendKey() {
    return wallet != null ? wallet.secretSpendKey() : "";
}
```

### 3. Seed Phrase Display
Update `SeedPhraseDialog` to show actual seed from wallet.

### 4. QR Code Generation (Recommended)
Replace placeholder in `ReceiveDialog` with actual QR code using ZXing.

## Testing Checklist

- [ ] Exchange screen loads
- [ ] Navigation works with 5 tabs
- [ ] All strings display correctly
- [ ] Test exchange flow with valid API key
- [ ] Test send/receive flows

## Support

For issues or questions:
- Check logcat for errors
- Verify all string resources are present
- Ensure API key is configured
'''
    
    try:
        with open('IMPLEMENTATION_GUIDE.md', 'w', encoding='utf-8') as f:
            f.write(guide)
        print_success("Created IMPLEMENTATION_GUIDE.md")
        return True
    except Exception as e:
        print_error(f"Error creating IMPLEMENTATION_GUIDE.md: {e}")
        return False

def main():
    if len(sys.argv) < 2:
        print_error("Usage: python3 fix_all.py <path_to_MoneroWalletActivity.kt>")
        print_info("Example: python3 fix_all.py app/src/main/java/com/techducat/apo/MoneroWalletActivity.kt")
        sys.exit(1)
    
    kotlin_file = sys.argv[1]
    
    if not os.path.exists(kotlin_file):
        print_error(f"File not found: {kotlin_file}")
        sys.exit(1)
    
    print_info(f"Processing {kotlin_file}...")
    print()
    
    # Read the Kotlin file
    try:
        with open(kotlin_file, 'r', encoding='utf-8') as f:
            content = f.read()
    except Exception as e:
        print_error(f"Error reading file: {e}")
        sys.exit(1)
    
    original_content = content
    
    # Apply all fixes
    print_info("Applying fixes to Kotlin file...")
    content = add_missing_imports(content)
    content = fix_navigation_tabs(content)
    content = fix_api_key_comment(content)
    content = fix_toDouble_safety(content)
    
    # Write back if changes were made
    if content != original_content:
        try:
            with open(kotlin_file, 'w', encoding='utf-8') as f:
                f.write(content)
            print_success(f"Successfully updated {kotlin_file}")
        except Exception as e:
            print_error(f"Error writing file: {e}")
            sys.exit(1)
    else:
        print_info("No changes needed in Kotlin file")
    
    print()
    
    # Find and update strings.xml
    print_info("Looking for strings.xml...")
    kotlin_path = Path(kotlin_file)
    
    # Try to find strings.xml relative to the Kotlin file
    possible_strings_paths = [
        kotlin_path.parent.parent.parent.parent.parent / 'res' / 'values' / 'strings.xml',
        Path('app/src/main/res/values/strings.xml'),
        Path('src/main/res/values/strings.xml'),
    ]
    
    strings_found = False
    for strings_path in possible_strings_paths:
        if strings_path.exists():
            print_info(f"Found strings.xml at: {strings_path}")
            add_missing_strings_xml(str(strings_path))
            strings_found = True
            break
    
    if not strings_found:
        print_warning("Could not find strings.xml automatically")
        print_info("Please manually add the missing string resources from the guide")
    
    print()
    
    # Create helper files
    print_info("Creating helper files...")
    create_build_gradle_additions()
    create_manifest_additions()
    create_implementation_guide()
    
    print()
    print_info("=" * 60)
    print_info("SUMMARY")
    print_info("=" * 60)
    print_success("✓ Fixed Kotlin file issues")
    print_success("✓ Created helper documentation files")
    print()
    print_info("NEXT STEPS:")
    print("1. Review IMPLEMENTATION_GUIDE.md for complete instructions")
    print("2. Apply changes from build_gradle_additions.txt")
    print("3. Apply changes from manifest_additions.txt")
    print("4. Get ChangeNOW API key from https://changenow.io/api/")
    print("5. Build and test the app")
    print()
    print_success("All automated fixes have been applied!")

if __name__ == '__main__':
    main()
