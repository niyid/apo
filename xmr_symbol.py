#!/usr/bin/env python3
"""
Script to replace hardcoded 'XMR' references with stringResource(R.string.monero_symbol)
in MoneroWalletActivity.kt
"""

import re
import sys
from pathlib import Path
from typing import List, Tuple

# Places where XMR should NOT be replaced (API identifiers, keys, etc)
XMR_KEEP_AS_IS = [
    'fromCurrency = "xmr"',
    'toCurrency = "xmr"',
    'if (fromCurrency == "xmr")',
    'ids=monero',  # API parameter
    '"monero"',  # API identifier
    'getDouble("xmr")',  # JSON key
    '"xmr"',  # Currency code
    '"btc"',  # Other currency codes
]

def analyze_file(content: str) -> List[Tuple[int, str, str]]:
    """
    Analyze file and return list of (line_number, line_content, context).
    """
    occurrences = []
    lines = content.split('\n')
    
    for i, line in enumerate(lines, 1):
        # Skip comments
        if line.strip().startswith('//'):
            continue
        
        # Look for XMR in various contexts
        if 'XMR' in line:
            # Check if it's in a keep-as-is context
            should_skip = False
            for keep_pattern in XMR_KEEP_AS_IS:
                if keep_pattern.lower() in line.lower():
                    should_skip = True
                    break
            
            if not should_skip:
                # Determine context
                context = "unknown"
                if 'String.format' in line and 'XMR' in line:
                    context = "String.format"
                elif 'prefix = { Text("XMR' in line:
                    context = "Text prefix"
                elif 'Text("XMR' in line:
                    context = "Text literal"
                elif '"' in line and 'XMR' in line:
                    context = "String literal"
                
                occurrences.append((i, line.strip(), context))
    
    return occurrences

def replace_xmr_references(content: str) -> Tuple[str, int]:
    """
    Replace hardcoded XMR references with stringResource calls.
    Returns the modified content and count of replacements.
    """
    replacements = 0
    
    # Pattern 1: String.format with " XMR" at the end
    # "%.6f XMR" -> "%.6f %s" with stringResource as additional argument
    def replace_format_string(match):
        nonlocal replacements
        format_part = match.group(1)
        rest_of_call = match.group(2)
        
        # Replace the format string and add stringResource parameter
        new_format = f'String.format("{format_part} %s"'
        
        # Check if there's a closing paren immediately or more args
        if rest_of_call.strip().startswith(')'):
            # No other args, add stringResource before closing
            result = f'{new_format}, stringResource(R.string.monero_symbol){rest_of_call}'
        else:
            # Has other args, add stringResource at appropriate position
            result = f'{new_format}{rest_of_call.rstrip(")")}, stringResource(R.string.monero_symbol))'
        
        replacements += 1
        return result
    
    pattern1 = r'String\.format\("([^"]+)\s+XMR"([^)]*)\)'
    content = re.sub(pattern1, replace_format_string, content)
    
    # Pattern 2: Text("XMR ") in prefix/suffix
    pattern2 = r'Text\("XMR "\)'
    replacement2 = r'Text(stringResource(R.string.monero_symbol) + " ")'
    count_before = content.count('Text("XMR ")')
    content = re.sub(pattern2, replacement2, content)
    count_after = content.count('Text(stringResource(R.string.monero_symbol) + " ")')
    replacements += count_after - (count_before - content.count('Text("XMR ")'))
    
    # Pattern 3: "${...} XMR" in string templates
    pattern3 = r'\$\{([^}]+)\}\s+XMR'
    replacement3 = r'${\1} ${stringResource(R.string.monero_symbol)}'
    count_before = len(re.findall(pattern3, content))
    content = re.sub(pattern3, replacement3, content)
    replacements += count_before
    
    return content, replacements

def add_string_import(content: str) -> str:
    """Add stringResource import if not present."""
    import_line = "import androidx.compose.ui.res.stringResource"
    
    if import_line not in content:
        # Find last import line
        last_import_pos = 0
        for match in re.finditer(r'^import [^\n]+$', content, re.MULTILINE):
            last_import_pos = match.end()
        
        if last_import_pos > 0:
            content = content[:last_import_pos] + '\n' + import_line + content[last_import_pos:]
    
    return content

def print_analysis(file_path: Path):
    """Print analysis of XMR occurrences in the file."""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        occurrences = analyze_file(content)
        
        print(f"\n{'='*80}")
        print(f"ANALYSIS: XMR Occurrences in {file_path.name}")
        print(f"{'='*80}\n")
        
        if not occurrences:
            print("No XMR references found that need replacement.")
            return
        
        print(f"Found {len(occurrences)} XMR references that need replacement:\n")
        
        # Group by context
        by_context = {}
        for line_num, line_content, context in occurrences:
            if context not in by_context:
                by_context[context] = []
            by_context[context].append((line_num, line_content))
        
        for context, items in sorted(by_context.items()):
            print(f"\n{context.upper()} ({len(items)} occurrences):")
            print("-" * 80)
            for line_num, line_content in items:
                # Truncate long lines for display
                display_content = line_content if len(line_content) <= 90 else line_content[:87] + "..."
                print(f"  Line {line_num:4d}: {display_content}")
        
        print(f"\n{'='*80}")
        print("\nXMR REFERENCES TO KEEP AS-IS (API identifiers, currency codes):")
        print("-" * 80)
        for pattern in XMR_KEEP_AS_IS:
            print(f"  • {pattern}")
        
        print(f"\n{'='*80}\n")
        
    except FileNotFoundError:
        print(f"✗ Error: File not found: {file_path}", file=sys.stderr)
    except Exception as e:
        print(f"✗ Error analyzing {file_path}: {e}", file=sys.stderr)

def process_file(file_path: Path, dry_run: bool = False) -> bool:
    """
    Process the Kotlin file and replace XMR references.
    Returns True if successful, False otherwise.
    """
    try:
        # Read the file
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        original_content = content
        
        # Add stringResource import if needed
        content = add_string_import(content)
        
        # Replace XMR references
        modified_content, count = replace_xmr_references(content)
        
        if count == 0:
            print(f"No replacements needed in {file_path.name}")
            return True
        
        if dry_run:
            print(f"\n[DRY RUN] Would replace {count} XMR references in {file_path.name}")
            
            # Show a diff preview
            print("\nPreview of changes:")
            print("-" * 80)
            
            orig_lines = original_content.split('\n')
            new_lines = modified_content.split('\n')
            
            changes_shown = 0
            for i, (orig, new) in enumerate(zip(orig_lines, new_lines), 1):
                if orig != new and changes_shown < 5:  # Show first 5 changes
                    print(f"\nLine {i}:")
                    print(f"  - {orig[:100]}")
                    print(f"  + {new[:100]}")
                    changes_shown += 1
            
            if count > 5:
                print(f"\n... and {count - 5} more changes")
            
            print("-" * 80)
            return True
        
        # Create backup
        backup_path = file_path.with_suffix(file_path.suffix + '.backup')
        with open(backup_path, 'w', encoding='utf-8') as f:
            f.write(original_content)
        print(f"✓ Backup created: {backup_path.name}")
        
        # Write the modified content back
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(modified_content)
        
        print(f"✓ Successfully replaced {count} XMR references in {file_path.name}")
        return True
        
    except FileNotFoundError:
        print(f"✗ Error: File not found: {file_path}", file=sys.stderr)
        return False
    except Exception as e:
        print(f"✗ Error processing {file_path}: {e}", file=sys.stderr)
        import traceback
        traceback.print_exc()
        return False

def main():
    """Main entry point."""
    # Default file path
    default_path = Path("MoneroWalletActivity.kt")
    
    # Parse arguments
    analyze_only = '--analyze' in sys.argv or '-a' in sys.argv
    dry_run = '--dry-run' in sys.argv or '-d' in sys.argv
    
    # Get file path
    file_args = [arg for arg in sys.argv[1:] if not arg.startswith('-')]
    file_path = Path(file_args[0]) if file_args else default_path
    
    # Validate file exists
    if not file_path.exists():
        print(f"✗ Error: File not found: {file_path}", file=sys.stderr)
        return 1
    
    if analyze_only:
        print_analysis(file_path)
        return 0
    
    print(f"Processing: {file_path}")
    if dry_run:
        print("(DRY RUN MODE - no changes will be made)")
    print("-" * 80)
    
    if process_file(file_path, dry_run):
        print("-" * 80)
        if not dry_run:
            print("✓ Done! A backup was created with .backup extension")
        print("\nNote: The following XMR references were intentionally NOT changed:")
        print("  - API currency codes (fromCurrency, toCurrency)")
        print("  - URL parameters and JSON keys")
        print("  - Internal identifiers (e.g., 'xmr' in lowercase)")
        return 0
    else:
        return 1

if __name__ == "__main__":
    if len(sys.argv) > 1 and sys.argv[1] in ['--help', '-h']:
        print("""
Usage: python3 xmr_symbol.py [OPTIONS] [FILE]

Options:
  -a, --analyze    Only analyze and show XMR occurrences, don't modify
  -d, --dry-run    Show what would be changed without modifying the file
  -h, --help       Show this help message

Examples:
  python3 xmr_symbol.py --analyze MoneroWalletActivity.kt    # Analyze file
  python3 xmr_symbol.py --dry-run MoneroWalletActivity.kt    # Test run
  python3 xmr_symbol.py MoneroWalletActivity.kt              # Process file
        """)
        sys.exit(0)
    
    sys.exit(main())
