#!/usr/bin/env python3
"""
Fix remaining {{ version }} macros to {{ snapshot }} in download links.

This handles the pattern:
- {{ version }} example: [plugin-name](URL)

Should become:
- {{ snapshot }} example: [plugin-name](URL)
"""

import re
from pathlib import Path

def fix_version_example_pattern(file_path: Path) -> tuple[bool, int]:
    """
    Replace {{ version }} example: with {{ snapshot }} example:
    
    Returns: (changed, count)
    """
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original_content = content
    
    # Pattern: {{ version }} example:
    # Replace with: {{ snapshot }} example:
    pattern = r'\{\{\s*version\s*\}\}\s+example:'
    replacement = r'{{ snapshot }} example:'
    
    content = re.sub(pattern, replacement, content)
    
    changed = content != original_content
    
    if changed:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)
        
        # Count replacements
        count = len(re.findall(pattern, original_content))
        return True, count
    
    return False, 0

def main():
    """Process all documentation files with version example pattern."""
    
    # Find all markdown files in doc/en/user/docs
    doc_root = Path('doc/en/user/docs')
    md_files = list(doc_root.rglob('*.md'))
    
    total_changed = 0
    total_replacements = 0
    
    print("Fixing remaining {{ version }} macros to {{ snapshot }}...")
    print("=" * 80)
    
    for file_path in md_files:
        # Check if file contains the pattern
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            if '{{ version }} example:' in content or '{{version}} example:' in content:
                changed, count = fix_version_example_pattern(file_path)
                if changed:
                    total_changed += 1
                    total_replacements += count
                    print(f"✓ Fixed {file_path} ({count} replacements)")
        except Exception as e:
            print(f"⚠ Error processing {file_path}: {e}")
    
    print("=" * 80)
    print(f"Summary: Fixed {total_changed} files with {total_replacements} replacements")

if __name__ == "__main__":
    main()
