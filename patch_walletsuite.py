#!/usr/bin/env python3
"""
Patch script to update WalletSuite.java to use PermissionHandler for storage paths
This fixes SELinux permission denials by using app-specific directories
"""

import sys
from pathlib import Path

# Color codes
class Colors:
    HEADER = '\033[95m'
    OKGREEN = '\033[92m'
    WARNING = '\033[93m'
    FAIL = '\033[91m'
    ENDC = '\033[0m'
    BOLD = '\033[1m'

def print_header(msg):
    print(f"\n{Colors.HEADER}{Colors.BOLD}{'='*60}{Colors.ENDC}")
    print(f"{Colors.HEADER}{Colors.BOLD}{msg}{Colors.ENDC}")
    print(f"{Colors.HEADER}{Colors.BOLD}{'='*60}{Colors.ENDC}\n")

def print_success(msg):
    print(f"{Colors.OKGREEN}✓ {msg}{Colors.ENDC}")

def print_error(msg):
    print(f"{Colors.FAIL}✗ {msg}{Colors.ENDC}")

def print_warning(msg):
    print(f"{Colors.WARNING}⚠ {msg}{Colors.ENDC}")

def patch_walletsuite(file_path):
    """Patch WalletSuite.java to use PermissionHandler"""
    print_header("PATCHING WalletSuite.java")
    
    try:
        # Read the file
        with open(file_path, 'r') as f:
            content = f.read()
        
        # Track changes
        changes_made = 0
        
        # Pattern 1: In initializeWallet() method
        old_pattern_1 = '''                // Step 2: Determine target directory - ORIGINAL PATH MAINTAINED
                File dir = context.getDir("wallets", Context.MODE_PRIVATE);'''
        
        new_pattern_1 = '''                // Step 2: Determine target directory - USE PERMISSIONHANDLER (SELinux-safe)
                // OLD: File dir = context.getDir("wallets", Context.MODE_PRIVATE);
                String storagePath = PermissionHandler.INSTANCE.getWalletStorageDir(context);
                File dir = new File(storagePath, "wallets");'''
        
        if old_pattern_1 in content:
            content = content.replace(old_pattern_1, new_pattern_1)
            changes_made += 1
            print_success("Patched initializeWallet() method (line ~1019)")
        else:
            print_warning("Pattern 1 not found - initializeWallet() may already be patched")
        
        # Pattern 2: In initializeWalletFromSeed() method
        old_pattern_2 = '''                // USE CONSISTENT PATH with main initializeWallet method
                File dir = context.getDir("wallets", Context.MODE_PRIVATE);'''
        
        new_pattern_2 = '''                // USE CONSISTENT PATH with main initializeWallet method (SELinux-safe)
                // OLD: File dir = context.getDir("wallets", Context.MODE_PRIVATE);
                String storagePath = PermissionHandler.INSTANCE.getWalletStorageDir(context);
                File dir = new File(storagePath, "wallets");'''
        
        if old_pattern_2 in content:
            content = content.replace(old_pattern_2, new_pattern_2)
            changes_made += 1
            print_success("Patched initializeWalletFromSeed() method (line ~1145)")
        else:
            print_warning("Pattern 2 not found - initializeWalletFromSeed() may already be patched")
        
        # Alternative: Try more flexible patterns if exact match fails
        if changes_made == 0:
            print_warning("Trying flexible pattern matching...")
            
            # Flexible pattern for any context.getDir("wallets", ...)
            import re
            
            # Pattern: File dir = context.getDir("wallets", Context.MODE_PRIVATE);
            flexible_pattern = r'File dir = context\.getDir\("wallets", Context\.MODE_PRIVATE\);'
            
            matches = re.findall(flexible_pattern, content)
            if matches:
                print_warning(f"Found {len(matches)} occurrence(s) of the old pattern")
                
                # Replace all occurrences
                replacement = '''String storagePath = PermissionHandler.INSTANCE.getWalletStorageDir(context);
                File dir = new File(storagePath, "wallets");'''
                
                content = re.sub(flexible_pattern, replacement, content)
                changes_made = len(matches)
                print_success(f"Applied flexible patch to {changes_made} location(s)")
        
        if changes_made == 0:
            print_error("No changes made - file may already be patched or patterns have changed")
            print_warning("Please manually update the following lines:")
            print("  File dir = context.getDir(\"wallets\", Context.MODE_PRIVATE);")
            print("\nTo:")
            print("  String storagePath = PermissionHandler.INSTANCE.getWalletStorageDir(context);")
            print("  File dir = new File(storagePath, \"wallets\");")
            return False
        
        # Create backup
        backup_path = file_path.parent / (file_path.name + ".backup")
        with open(backup_path, 'w') as f:
            # Read original again for backup
            with open(file_path, 'r') as orig:
                f.write(orig.read())
        print_success(f"Created backup: {backup_path}")
        
        # Write patched content
        with open(file_path, 'w') as f:
            f.write(content)
        
        print_success(f"Applied {changes_made} patch(es) to WalletSuite.java")
        return True
        
    except FileNotFoundError:
        print_error(f"File not found: {file_path}")
        return False
    except Exception as e:
        print_error(f"Error patching file: {e}")
        return False

def main():
    print_header("WalletSuite.java Storage Path Patcher")
    
    # Find WalletSuite.java
    current_dir = Path.cwd()
    walletsuite_path = current_dir / "app" / "src" / "main" / "java" / "com" / "techducat" / "apo" / "WalletSuite.java"
    
    if not walletsuite_path.exists():
        print_error(f"WalletSuite.java not found at: {walletsuite_path}")
        print_warning("Please run this script from the project root (~/git/apo)")
        sys.exit(1)
    
    print(f"Found: {walletsuite_path}\n")
    
    # Apply patch
    if patch_walletsuite(walletsuite_path):
        print_header("PATCH SUMMARY")
        print_success("WalletSuite.java successfully patched!")
        print("\nChanges made:")
        print("  1. initializeWallet() - Uses PermissionHandler for storage path")
        print("  2. initializeWalletFromSeed() - Uses PermissionHandler for storage path")
        print("\nBackup created: WalletSuite.java.backup")
        print("\nBenefit:")
        print("  ✓ Fixes SELinux permission denials")
        print("  ✓ Uses app-specific directories (Android 11+ compliant)")
        print("  ✓ Consistent storage path across all wallet operations")
        print("\nNext steps:")
        print("  1. Verify the changes in WalletSuite.java")
        print("  2. Build and test: ./gradlew assembleDebug")
        print("  3. Check logcat for 'PermissionHandler' messages")
        print("  4. Verify no SELinux denials in logs")
        sys.exit(0)
    else:
        print_header("PATCH FAILED")
        print_error("Could not apply automatic patch")
        print("\nManual steps required:")
        print("  1. Open app/src/main/java/com/techducat/apo/WalletSuite.java")
        print("  2. Find: File dir = context.getDir(\"wallets\", Context.MODE_PRIVATE);")
        print("     (Should appear in TWO methods: initializeWallet and initializeWalletFromSeed)")
        print("  3. Replace with:")
        print("     String storagePath = PermissionHandler.INSTANCE.getWalletStorageDir(context);")
        print("     File dir = new File(storagePath, \"wallets\");")
        sys.exit(1)

if __name__ == "__main__":
    main()
