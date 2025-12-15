#!/usr/bin/env python3
"""
Script to fix all identified issues in MoneroWalletActivity.kt
"""

import re
import sys
from pathlib import Path

def fix_kotlin_file(file_path):
    """Fix all issues in the Kotlin file"""
    
    print(f"Reading file: {file_path}")
    with open(file_path, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    
    original_lines = lines.copy()
    fixes_applied = []
    
    # Fix 1: Add missing closing brace after setContent block (around line 234)
    # Look for the pattern where setContent ends and lifecycleScope begins
    for i in range(len(lines)):
        if i < len(lines) - 5:
            # Find "}" followed by blank line and "// Hide splash screen"
            if (lines[i].strip() == '}' and 
                lines[i+1].strip() == '' and 
                i+2 < len(lines) and '// Hide splash screen' in lines[i+2]):
                
                # Check if this is inside setContent (should have MoneroWalletScreen above)
                found_setcontent = False
                for j in range(max(0, i-20), i):
                    if 'MoneroWalletScreen(walletSuite)' in lines[j]:
                        found_setcontent = True
                        break
                
                if found_setcontent:
                    # Add closing brace for setContent
                    lines.insert(i+1, '        }\n')
                    fixes_applied.append("Added missing closing brace for setContent block")
                    break
    
    # Fix 2: Remove extra closing brace in MoneroWalletTheme function (around line 261)
    # Look for the pattern after MoneroWalletTheme function definition
    for i in range(len(lines)):
        if 'fun MoneroWalletTheme' in lines[i]:
            # Find the end of this function (should be around 15-20 lines later)
            for j in range(i, min(i+25, len(lines)-1)):
                if (lines[j].strip() == ')' and 
                    j+1 < len(lines) and lines[j+1].strip() == '}' and
                    j+2 < len(lines) and lines[j+2].strip() == '}'):
                    # Found pattern: ) } } - remove the extra closing brace
                    lines.pop(j+2)
                    fixes_applied.append("Removed extra closing brace in MoneroWalletTheme function")
                    break
            break
    
    # Check if any fixes were applied
    if lines == original_lines:
        print("\n⚠️  No changes were made. Manual inspection required.")
        return False
    
    # Write the fixed content back
    print(f"\nWriting fixes to: {file_path}")
    with open(file_path, 'w', encoding='utf-8') as f:
        f.writelines(lines)
    
    # Print summary
    print("\n✅ Fixes applied:")
    for i, fix in enumerate(fixes_applied, 1):
        print(f"   {i}. {fix}")
    
    return True

def main():
    # Default path
    default_path = Path.home() / "git" / "apo" / "app" / "src" / "main" / "java" / "com" / "techducat" / "apo" / "MoneroWalletActivity.kt"
    
    # Check if custom path provided
    if len(sys.argv) > 1:
        file_path = Path(sys.argv[1])
    else:
        file_path = default_path
    
    # Check if file exists
    if not file_path.exists():
        print(f"❌ Error: File not found: {file_path}")
        print(f"\nUsage: python3 {sys.argv[0]} [path_to_MoneroWalletActivity.kt]")
        print(f"Default path: {default_path}")
        sys.exit(1)
    
    # Create backup
    backup_path = file_path.with_suffix('.kt.backup')
    print(f"Creating backup: {backup_path}")
    with open(file_path, 'r', encoding='utf-8') as f:
        backup_content = f.read()
    with open(backup_path, 'w', encoding='utf-8') as f:
        f.write(backup_content)
    
    # Apply fixes
    try:
        success = fix_kotlin_file(file_path)
        if success:
            print(f"\n✅ All fixes applied successfully!")
            print(f"📁 Backup saved to: {backup_path}")
            print(f"\nTo restore backup if needed:")
            print(f"   cp {backup_path} {file_path}")
            print(f"\nNow try compiling:")
            print(f"   cd ~/git/apo && ./gradlew assembleDebug")
        else:
            print(f"\n⚠️  Automatic fix failed. Please apply manual fixes:")
            print(f"\n1. Line 234: Add '}}' after the setContent block, before '// Hide splash screen'")
            print(f"2. Line 261: Remove extra '}}' after MoneroWalletTheme function")
    except Exception as e:
        print(f"\n❌ Error applying fixes: {e}")
        print(f"Restoring from backup...")
        with open(backup_path, 'r', encoding='utf-8') as f:
            content = f.read()
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)
        print("✅ Backup restored")
        sys.exit(1)

if __name__ == "__main__":
    main()
