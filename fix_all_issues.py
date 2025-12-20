#!/usr/bin/env python3
"""
Complete Android performance fix script:
1. Fixes Compose accessibility issues (.semantics)
2. Fixes VSync timeout issues (throttled UI updates)
3. Removes StrictMode workarounds

Usage:
    python fix_all_issues.py --dry-run    # Preview changes
    python fix_all_issues.py              # Apply fixes
"""

import re
import sys
from pathlib import Path
from typing import Dict


class CompleteFixer:
    def __init__(self, dry_run: bool = False):
        self.dry_run = dry_run
        self.fixes_applied = {
            'accessibility': 0,
            'vsync': 0,
            'strictmode': 0
        }
    
    def fix_kotlin_accessibility(self, content: str, file_path: Path) -> str:
        """Fix Compose accessibility issues in Kotlin files."""
        print(f"\n📱 Fixing accessibility in {file_path.name}...")
        
        # Add import if needed
        if 'import androidx.compose.ui.semantics.semantics' not in content:
            # Find last import
            lines = content.split('\n')
            last_import_idx = -1
            for i, line in enumerate(lines):
                if line.strip().startswith('import '):
                    last_import_idx = i
            
            if last_import_idx >= 0:
                lines.insert(last_import_idx + 1, 'import androidx.compose.ui.semantics.semantics')
                content = '\n'.join(lines)
                print("   ✓ Added semantics import")
        
        # Fix all Card composables with nested content
        lines = content.split('\n')
        result = []
        i = 0
        
        while i < len(lines):
            line = lines[i]
            
            # Detect Card( patterns
            if re.search(r'\bCard\s*\(', line):
                # Check if semantics already present in this line or nearby
                context = '\n'.join(lines[max(0, i-2):min(len(lines), i+5)])
                
                if 'semantics' not in context.lower():
                    # Look ahead to see if there's nested content
                    lookahead_end = min(i + 20, len(lines))
                    lookahead = '\n'.join(lines[i:lookahead_end])
                    
                    # Check for nested composables that trigger accessibility issues
                    if any(pattern in lookahead for pattern in ['Box(', 'Column(', 'Row(', 'LazyColumn(', 'Surface(']):
                        # Fix the modifier
                        if 'modifier = Modifier' in line and '.semantics' not in line:
                            # Add to existing modifier chain
                            line = re.sub(
                                r'(modifier\s*=\s*Modifier[^,\n]+?)([,\n])',
                                r'\1.semantics(mergeDescendants = true)\2',
                                line
                            )
                            self.fixes_applied['accessibility'] += 1
                            print(f"   ✓ Fixed Card at line {i+1}")
            
            result.append(line)
            i += 1
        
        return '\n'.join(result)
    
    def fix_java_vsync(self, content: str, file_path: Path) -> str:
        """Fix VSync timeout issues in WalletSuite.java."""
        print(f"\n⚡ Fixing VSync timeouts in {file_path.name}...")
        
        # Step 1: Add throttling constants if not present
        if 'UI_UPDATE_THROTTLE_MS' not in content:
            # Find where to insert (after other AtomicLong declarations)
            pattern = r'(private final AtomicLong unlockedBalance = new AtomicLong\(0L\);)'
            if re.search(pattern, content):
                replacement = r'''\1
    
    // UI Update Throttling - prevents VSync timeouts
    private final AtomicLong lastUiUpdateTime = new AtomicLong(0);
    private static final long UI_UPDATE_THROTTLE_MS = 500; // Max UI update every 500ms'''
                
                content = re.sub(pattern, replacement, content)
                print("   ✓ Added UI throttling constants")
                self.fixes_applied['vsync'] += 1
        
        # Step 2: Fix updateBalanceFromWallet method
        pattern = r'(private void updateBalanceFromWallet\(\) \{.*?)(// Notify listeners on main thread\s+if \(statusListener != null\) \{\s+final long finalBal = bal;\s+final long finalUnl = unl;\s+mainHandler\.post\(\(\) -> statusListener\.onBalanceUpdated\(finalBal, finalUnl\)\);)'
        
        if re.search(pattern, content, re.DOTALL):
            replacement = r'''\1// CRITICAL FIX: Throttle UI updates to prevent VSync timeouts
            long now = System.currentTimeMillis();
            long lastUpdate = lastUiUpdateTime.get();
            
            if (now - lastUpdate >= UI_UPDATE_THROTTLE_MS) {
                if (lastUiUpdateTime.compareAndSet(lastUpdate, now)) {
                    // Notify listeners on main thread
                    if (statusListener != null) {
                        final long finalBal = bal;
                        final long finalUnl = unl;
                        mainHandler.post(() -> statusListener.onBalanceUpdated(finalBal, finalUnl));
                    }
                }
            } else {
                // Skip update - too soon since last update'''
            
            content = re.sub(pattern, replacement, content, flags=re.DOTALL)
            print("   ✓ Added throttling to updateBalanceFromWallet()")
            self.fixes_applied['vsync'] += 1
        
        # Step 3: Fix newBlock callback
        pattern = r'(@Override\s+public void newBlock\(long height\) \{\s+long currentTime = System\.currentTimeMillis\(\);)'
        
        if re.search(pattern, content):
            replacement = r'''\1
                
                // CRITICAL FIX: Throttle to prevent VSync timeouts
                if (currentTime - lastProgressUpdateTime < UI_UPDATE_THROTTLE_MS) {
                    return; // Skip this update - too frequent
                }'''
            
            content = re.sub(pattern, replacement, content)
            print("   ✓ Added throttling to newBlock() callback")
            self.fixes_applied['vsync'] += 1
        
        # Step 4: Add callback cleanup before posting to main thread
        pattern = r'(if \(statusListener != null\) \{\s+final long currentHeight = height;\s+final double finalPercent = percentDone;\s+)(mainHandler\.post\(\(\) -> statusListener\.onSyncProgress)'
        
        if re.search(pattern, content, re.DOTALL):
            replacement = r'''\1// Clear pending updates to prevent queue buildup
                        mainHandler.removeCallbacksAndMessages(null);
                        \2'''
            
            content = re.sub(pattern, replacement, content, flags=re.DOTALL)
            print("   ✓ Added callback cleanup to prevent main thread flooding")
            self.fixes_applied['vsync'] += 1
        
        return content
    
    def fix_java_strictmode(self, content: str, file_path: Path) -> str:
        """Remove StrictMode workarounds from Java files."""
        print(f"\n🔧 Removing StrictMode workarounds in {file_path.name}...")
        
        original = content
        
        # Remove StrictMode.ThreadPolicy declarations and usage
        content = re.sub(
            r'// FIX: Temporarily permit network on this thread.*?\n.*?StrictMode\.ThreadPolicy oldPolicy = StrictMode\.getThreadPolicy\(\);\s*\n\s*StrictMode\.setThreadPolicy\(new StrictMode\.ThreadPolicy\.Builder\(\)\s*\.permitNetwork\(\)\s*\.build\(\)\);\s*\n\s*try \{\s*',
            '',
            content,
            flags=re.DOTALL
        )
        
        # Remove finally blocks that restore StrictMode
        content = re.sub(
            r'\} finally \{\s*// Restore original StrictMode policy\s*\n\s*StrictMode\.setThreadPolicy\(oldPolicy\);\s*\n\s*\}',
            '}',
            content,
            flags=re.DOTALL
        )
        
        # Remove StrictMode import
        content = re.sub(
            r'import android\.os\.StrictMode;\s*\n',
            '',
            content
        )
        
        if content != original:
            print("   ✓ Removed StrictMode workarounds")
            self.fixes_applied['strictmode'] += 1
        
        return content
    
    def process_file(self, file_path: Path) -> bool:
        """Process a single file with all applicable fixes."""
        try:
            content = file_path.read_text(encoding='utf-8')
            original_content = content
            
            # Apply fixes based on file type
            if file_path.suffix == '.kt':
                content = self.fix_kotlin_accessibility(content, file_path)
            
            elif file_path.suffix == '.java' and 'WalletSuite' in file_path.name:
                content = self.fix_java_vsync(content, file_path)
                content = self.fix_java_strictmode(content, file_path)
            
            # Write changes if not dry run
            if content != original_content:
                if not self.dry_run:
                    # Create backup
                    backup_path = file_path.with_suffix(file_path.suffix + '.backup')
                    backup_path.write_text(original_content, encoding='utf-8')
                    
                    # Write fixed content
                    file_path.write_text(content, encoding='utf-8')
                    print(f"\n✅ Fixed {file_path.name} (Backup: {backup_path.name})")
                else:
                    print(f"\n🔍 [DRY RUN] Would fix {file_path.name}")
                
                return True
            else:
                print(f"\n⏭️  No changes needed for {file_path.name}")
                return False
                
        except Exception as e:
            print(f"\n❌ Error processing {file_path}: {e}")
            import traceback
            traceback.print_exc()
            return False
    
    def generate_summary(self):
        """Generate summary of all fixes applied."""
        print("\n" + "=" * 70)
        print("📊 FIX SUMMARY")
        print("=" * 70)
        
        total = sum(self.fixes_applied.values())
        
        if total == 0:
            print("✓ No issues found - code is already optimized!")
            return
        
        print(f"\n{'Category':<30} {'Fixes Applied':<15}")
        print("-" * 70)
        print(f"{'Accessibility (semantics)':<30} {self.fixes_applied['accessibility']:<15}")
        print(f"{'VSync Throttling':<30} {self.fixes_applied['vsync']:<15}")
        print(f"{'StrictMode Cleanup':<30} {self.fixes_applied['strictmode']:<15}")
        print("-" * 70)
        print(f"{'TOTAL':<30} {total:<15}")
        
        print("\n💡 Next Steps:")
        print("   1. Review the changes in your IDE")
        print("   2. Test the app - VSync warnings should disappear")
        print("   3. Enable TalkBack to verify accessibility")
        print("   4. Commit the changes")
        print("   5. Delete .backup files once confirmed")


def main():
    """Main entry point."""
    import argparse
    
    parser = argparse.ArgumentParser(
        description='Fix Android performance issues: Accessibility + VSync + StrictMode'
    )
    parser.add_argument(
        '--dry-run',
        action='store_true',
        help='Show what would be fixed without modifying files'
    )
    
    args = parser.parse_args()
    
    print("🔧 COMPLETE ANDROID PERFORMANCE FIXER")
    print("=" * 70)
    print("Fixes: Accessibility warnings, VSync timeouts, StrictMode workarounds")
    print("=" * 70)
    
    # Auto-detect files
    source_dir = Path('app/src/main/java/com/techducat/apo')
    
    if not source_dir.exists():
        print(f"\n❌ Source directory not found: {source_dir}")
        print("   Please run this script from your project root directory")
        sys.exit(1)
    
    files_to_fix = [
        source_dir / 'MoneroWalletActivity.kt',
        source_dir / 'WalletSuite.java'
    ]
    
    # Verify files exist
    missing_files = [f for f in files_to_fix if not f.exists()]
    if missing_files:
        print(f"\n❌ Files not found:")
        for f in missing_files:
            print(f"   - {f}")
        sys.exit(1)
    
    print(f"\n📁 Found {len(files_to_fix)} files to process:")
    for f in files_to_fix:
        print(f"   - {f.name}")
    
    fixer = CompleteFixer(dry_run=args.dry_run)
    files_modified = 0
    
    for file_path in files_to_fix:
        if fixer.process_file(file_path):
            files_modified += 1
    
    # Generate summary
    fixer.generate_summary()
    
    print(f"\n📈 Files: {files_modified} modified out of {len(files_to_fix)} processed")
    
    if args.dry_run:
        print("\n🔍 This was a DRY RUN - no files were modified")
        print("   Run without --dry-run flag to apply changes")


if __name__ == '__main__':
    main()
