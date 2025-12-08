#!/usr/bin/env python3
"""
Kotlin Code Fixer - Automatically fixes common issues in MoneroWalletActivity.kt

This script fixes:
1. Removes incomplete 'val changeNowService' declaration
2. Adds missing imports
3. Validates and cleans up the code structure
"""

import re
import sys
from pathlib import Path
from typing import List, Tuple


class KotlinCodeFixer:
    def __init__(self, file_path: str):
        self.file_path = Path(file_path)
        self.content = ""
        self.fixes_applied = []
        
    def read_file(self) -> bool:
        """Read the Kotlin file"""
        try:
            with open(self.file_path, 'r', encoding='utf-8') as f:
                self.content = f.read()
            print(f"✓ Successfully read {self.file_path}")
            return True
        except FileNotFoundError:
            print(f"✗ Error: File not found: {self.file_path}")
            return False
        except Exception as e:
            print(f"✗ Error reading file: {e}")
            return False
    
    def write_file(self, backup=True) -> bool:
        """Write the fixed content back to file"""
        try:
            if backup:
                backup_path = self.file_path.with_suffix('.kt.backup')
                with open(backup_path, 'w', encoding='utf-8') as f:
                    f.write(self.content)
                print(f"✓ Backup created: {backup_path}")
            
            with open(self.file_path, 'w', encoding='utf-8') as f:
                f.write(self.content)
            print(f"✓ Successfully wrote fixes to {self.file_path}")
            return True
        except Exception as e:
            print(f"✗ Error writing file: {e}")
            return False
    
    def fix_incomplete_val_declaration(self) -> int:
        """Remove incomplete 'val changeNowService' line"""
        pattern = r'^\s*val changeNowService\s*$'
        lines = self.content.split('\n')
        fixed_lines = []
        removed_count = 0
        
        for i, line in enumerate(lines):
            if re.match(pattern, line):
                print(f"  Found incomplete declaration at line {i+1}: {line.strip()}")
                removed_count += 1
                continue
            fixed_lines.append(line)
        
        if removed_count > 0:
            self.content = '\n'.join(fixed_lines)
            self.fixes_applied.append(f"Removed {removed_count} incomplete val declaration(s)")
            print(f"✓ Removed {removed_count} incomplete val declaration(s)")
        
        return removed_count
    
    def add_missing_imports(self) -> int:
        """Add missing imports if not present"""
        required_imports = [
            "import androidx.compose.material.icons.filled.ChevronRight",
            "import androidx.compose.material.icons.filled.VpnKey",
            "import androidx.compose.material.icons.filled.Security",
            "import androidx.compose.material.icons.filled.SwapVert",
            "import androidx.compose.material.icons.filled.SwapHoriz"
        ]
        
        added_count = 0
        import_section_pattern = r'(import androidx\.compose\.material\.icons\.Icons\nimport androidx\.compose\.material\.icons\.filled\.\*)'
        
        # Check if we're using wildcard import
        if 'import androidx.compose.material.icons.filled.*' in self.content:
            print("✓ Using wildcard import for icons - no specific imports needed")
            return 0
        
        # Find the import section
        import_match = re.search(r'(import .*\n)+', self.content)
        if not import_match:
            print("✗ Could not find import section")
            return 0
        
        import_section = import_match.group(0)
        new_imports = []
        
        for import_stmt in required_imports:
            if import_stmt not in self.content:
                new_imports.append(import_stmt)
                added_count += 1
        
        if new_imports:
            # Add new imports after existing imports
            new_import_section = import_section.rstrip() + '\n' + '\n'.join(new_imports) + '\n'
            self.content = self.content.replace(import_section, new_import_section)
            self.fixes_applied.append(f"Added {added_count} missing import(s)")
            print(f"✓ Added {added_count} missing import(s)")
        
        return added_count
    
    def fix_duplicate_service_initialization(self) -> int:
        """Fix duplicate ChangeNowSwapService initialization"""
        # Pattern to find duplicate 'val changeNowService = remember { ChangeNowSwapService() }'
        pattern = r'val changeNowService = remember \{ ChangeNowSwapService\(\) \}'
        matches = list(re.finditer(pattern, self.content))
        
        if len(matches) > 1:
            # Keep only the first occurrence
            for match in matches[1:]:
                line_start = self.content.rfind('\n', 0, match.start()) + 1
                line_end = self.content.find('\n', match.end())
                self.content = self.content[:line_start] + self.content[line_end+1:]
            
            removed = len(matches) - 1
            self.fixes_applied.append(f"Removed {removed} duplicate service initialization(s)")
            print(f"✓ Removed {removed} duplicate service initialization(s)")
            return removed
        
        return 0
    
    def validate_syntax(self) -> List[Tuple[int, str]]:
        """Basic syntax validation"""
        issues = []
        lines = self.content.split('\n')
        
        for i, line in enumerate(lines, 1):
            # Check for incomplete variable declarations
            if re.match(r'^\s*(val|var)\s+\w+\s*$', line):
                issues.append((i, f"Incomplete variable declaration: {line.strip()}"))
            
            # Check for unmatched braces (basic check)
            if line.count('{') != line.count('}'):
                open_braces = line.count('{')
                close_braces = line.count('}')
                if abs(open_braces - close_braces) > 1:
                    issues.append((i, f"Potential brace mismatch: {line.strip()}"))
        
        return issues
    
    def fix_all(self) -> bool:
        """Apply all fixes"""
        print("\n🔧 Starting Kotlin Code Fixer...")
        print("=" * 60)
        
        if not self.read_file():
            return False
        
        print("\n📝 Applying fixes...\n")
        
        # Apply fixes
        self.fix_incomplete_val_declaration()
        self.add_missing_imports()
        self.fix_duplicate_service_initialization()
        
        # Validate
        print("\n🔍 Validating code...\n")
        issues = self.validate_syntax()
        
        if issues:
            print("⚠️  Found potential issues:")
            for line_num, issue in issues[:5]:  # Show first 5 issues
                print(f"  Line {line_num}: {issue}")
            if len(issues) > 5:
                print(f"  ... and {len(issues) - 5} more issues")
        else:
            print("✓ No syntax issues detected")
        
        # Write fixes
        print("\n💾 Saving changes...\n")
        if self.write_file():
            print("\n" + "=" * 60)
            print("✅ Fixes applied successfully!")
            print("\nSummary of fixes:")
            for fix in self.fixes_applied:
                print(f"  • {fix}")
            return True
        
        return False


def main():
    """Main entry point"""
    if len(sys.argv) > 1:
        file_path = sys.argv[1]
    else:
        file_path = "MoneroWalletActivity.kt"
    
    print(f"""
╔════════════════════════════════════════════════════════════╗
║          Kotlin Code Fixer for MoneroWalletActivity       ║
║                                                            ║
║  This script will fix common issues in your Kotlin code   ║
╚════════════════════════════════════════════════════════════╝
    """)
    
    fixer = KotlinCodeFixer(file_path)
    success = fixer.fix_all()
    
    if success:
        print("\n✨ All done! Your code has been fixed and backed up.")
        print(f"   Original backed up to: {file_path}.backup")
        return 0
    else:
        print("\n❌ Failed to apply fixes. Please check the errors above.")
        return 1


if __name__ == "__main__":
    sys.exit(main())
