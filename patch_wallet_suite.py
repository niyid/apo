#!/usr/bin/env python3
"""
WalletSuite.java Patcher for NetworkOnMainThreadException Fix

This script patches WalletSuite.java to fix the NetworkOnMainThreadException
that occurs when the wallet library tries to perform DNS lookups on the main thread.

Usage:
    python patch_wallet_suite.py WalletSuite.java

The script will:
1. Create a backup of the original file (WalletSuite.java.backup)
2. Apply the necessary fixes to prevent network operations on main thread
3. Save the patched version
"""

import sys
import os
import re
from datetime import datetime

def create_backup(filepath):
    """Create a backup of the original file"""
    backup_path = f"{filepath}.backup.{datetime.now().strftime('%Y%m%d_%H%M%S')}"
    try:
        with open(filepath, 'r', encoding='utf-8') as original:
            content = original.read()
        with open(backup_path, 'w', encoding='utf-8') as backup:
            backup.write(content)
        print(f"✓ Backup created: {backup_path}")
        return True
    except Exception as e:
        print(f"✗ Failed to create backup: {e}")
        return False

def patch_imports(content):
    """Add missing imports if not present"""
    imports_to_add = [
        "import android.os.StrictMode;",
        "import android.os.Looper;"
    ]
    
    # Check if imports already exist
    existing_imports = set()
    for line in content.split('\n'):
        if line.strip().startswith('import '):
            existing_imports.add(line.strip())
    
    # Find the last import statement
    lines = content.split('\n')
    last_import_index = -1
    for i, line in enumerate(lines):
        if line.strip().startswith('import '):
            last_import_index = i
    
    if last_import_index == -1:
        print("✗ Could not find import statements")
        return content
    
    # Add missing imports
    imports_added = []
    for imp in imports_to_add:
        if imp not in existing_imports:
            lines.insert(last_import_index + 1, imp)
            last_import_index += 1
            imports_added.append(imp)
    
    if imports_added:
        print(f"✓ Added imports: {', '.join(imports_added)}")
    else:
        print("✓ All required imports already present")
    
    return '\n'.join(lines)

def patch_initialize_wallet(content):
    """Add StrictMode workaround to initializeWallet"""
    
    # Pattern to find the start of the try block in initializeWallet
    pattern = r'(    public Future<Boolean> initializeWallet\(\) \{[^\{]*\{[^\{]*\{[\s]*try \{)'
    
    replacement = r'''\1
                // FIX: Temporarily permit network on this thread for wallet initialization
                // This is a workaround for the underlying Monero library's DNS lookups
                StrictMode.ThreadPolicy oldPolicy = StrictMode.getThreadPolicy();
                StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .permitNetwork()
                    .build());
                try {'''
    
    if re.search(pattern, content, re.DOTALL):
        content = re.sub(pattern, replacement, content, flags=re.DOTALL)
        print("✓ Added StrictMode workaround to initializeWallet() - start")
        
        # Now add the finally block to restore StrictMode
        # Find: "Log.i(TAG, "✓ WALLET INITIALIZATION COMPLETE");" followed by catch block
        pattern2 = r'(                Log\.i\(TAG, "✓ WALLET INITIALIZATION COMPLETE"\);[\s]*future\.complete\(true\);[\s]*)(            \} catch \(Exception e\) \{)'
        
        replacement2 = r'''\1
                } finally {
                    // Restore original StrictMode policy
                    StrictMode.setThreadPolicy(oldPolicy);
                }

\2'''
        
        if re.search(pattern2, content, re.DOTALL):
            content = re.sub(pattern2, replacement2, content, flags=re.DOTALL)
            print("✓ Added StrictMode restoration in initializeWallet() - finally block")
        else:
            print("⚠ Could not find location to add finally block in initializeWallet()")
    else:
        print("⚠ Could not find initializeWallet() method to patch")
    
    return content

def patch_initialize_wallet_from_seed(content):
    """Add StrictMode workaround to initializeWalletFromSeed"""
    
    # Pattern to find the start of try block
    pattern = r'(    public void initializeWalletFromSeed\([^\)]*\) \{[^\{]*\{[\s]*try \{)'
    
    replacement = r'''\1
                // FIX: Temporarily permit network on this thread
                StrictMode.ThreadPolicy oldPolicy = StrictMode.getThreadPolicy();
                StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .permitNetwork()
                    .build());
                try {'''
    
    if re.search(pattern, content, re.DOTALL):
        content = re.sub(pattern, replacement, content, flags=re.DOTALL)
        print("✓ Added StrictMode workaround to initializeWalletFromSeed() - start")
        
        # Add finally block
        pattern2 = r'(            \} catch \(Exception e\) \{[\s]*Log\.e\(TAG, "✗ Exception during wallet restoration", e\);[\s]*notifyWalletInitialized\(false, "Error: " \+ e\.getMessage\(\)\);[\s]*)(        \}\s*\}\);)'
        
        replacement2 = r'''\1
            } finally {
                // Restore original StrictMode policy
                StrictMode.setThreadPolicy(oldPolicy);
            }
\2'''
        
        if re.search(pattern2, content, re.DOTALL):
            content = re.sub(pattern2, replacement2, content, flags=re.DOTALL)
            print("✓ Added StrictMode restoration in initializeWalletFromSeed() - finally block")
    else:
        print("⚠ Could not find initializeWalletFromSeed() method to patch")
    
    return content

def patch_setup_wallet(content):
    """Patch setupWallet() to run on background thread"""
    
    # Find setupWallet method and wrap daemon setup in background thread
    pattern = r'(    private void setupWallet\(\) \{[\s]*if \(wallet == null\) return;[\s]*)(boolean daemonSet = setDaemonFromConfigAndApply\(\);)'
    
    replacement = r'''\1
        // FIX: Run daemon setup on background thread to avoid NetworkOnMainThreadException
        executorService.execute(() -> {
            try {
                boolean daemonSet = setDaemonFromConfigAndApply();
                if (!daemonSet) {
                    Log.e(TAG, "Failed to establish daemon connection during setup");
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception during daemon setup", e);
            }
        });
    }

    private void setupWalletSync() {
        if (wallet == null) return;
        boolean daemonSet = setDaemonFromConfigAndApply();'''
    
    if re.search(pattern, content, re.DOTALL):
        content = re.sub(pattern, replacement, content, flags=re.DOTALL)
        print("✓ Patched setupWallet() to use background thread")
    else:
        print("⚠ Could not find setupWallet() method to patch")
    
    return content

def verify_patches(content):
    """Verify that patches were applied successfully"""
    checks = [
        ("StrictMode import", "import android.os.StrictMode;"),
        ("StrictMode workaround", "StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()"),
        ("StrictMode restoration", "StrictMode.setThreadPolicy(oldPolicy);"),
    ]
    
    all_passed = True
    for check_name, check_pattern in checks:
        if check_pattern in content:
            print(f"✓ Verification passed: {check_name}")
        else:
            print(f"✗ Verification failed: {check_name}")
            all_passed = False
    
    return all_passed

def main():
    if len(sys.argv) != 2:
        print("Usage: python3 patch_wallet_suite.py WalletSuite.java")
        sys.exit(1)
    
    filepath = sys.argv[1]
    
    if not os.path.exists(filepath):
        print(f"✗ File not found: {filepath}")
        sys.exit(1)
    
    print(f"Starting patch process for: {filepath}")
    print("=" * 60)
    
    # Create backup
    if not create_backup(filepath):
        sys.exit(1)
    
    # Read original content
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
        print(f"✓ Read original file ({len(content)} bytes)")
    except Exception as e:
        print(f"✗ Failed to read file: {e}")
        sys.exit(1)
    
    # Apply patches
    print("\nApplying patches...")
    print("-" * 60)
    content = patch_imports(content)
    content = patch_initialize_wallet(content)
    content = patch_initialize_wallet_from_seed(content)
    content = patch_setup_wallet(content)
    
    # Verify patches
    print("\nVerifying patches...")
    print("-" * 60)
    if not verify_patches(content):
        print("\n⚠️  Some patches may not have been applied correctly")
        print("Please review the changes manually")
    
    # Write patched content
    try:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"\n✓ Successfully wrote patched file ({len(content)} bytes)")
    except Exception as e:
        print(f"\n✗ Failed to write patched file: {e}")
        sys.exit(1)
    
    print("\n" + "=" * 60)
    print("✓ Patching complete!")
    print("\nNext steps:")
    print("1. Review the changes: git diff app/src/main/java/com/techducat/apo/WalletSuite.java")
    print("2. Build and test your app")
    print("3. If issues occur, restore from backup:")
    print(f"   ls -la app/src/main/java/com/techducat/apo/WalletSuite.java.backup*")
    print("\nThe patches add:")
    print("- StrictMode.permitNetwork() workaround during wallet init")
    print("- Background thread execution for daemon configuration")
    print("- Better exception handling for NetworkOnMainThreadException")

if __name__ == "__main__":
    main()
