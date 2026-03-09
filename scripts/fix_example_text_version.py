#!/usr/bin/env python3
"""
Fix example text in warnings to use {{ snapshot }} instead of {{ version }}.

The warning text says "for example geoserver-{{ version }}-..." but should
match the actual download link which uses {{ snapshot }}.
"""

import re
from pathlib import Path

def fix_example_text(file_path: Path) -> tuple[bool, int]:
    """
    Replace geoserver-{{ version }}- with geoserver-{{ snapshot }}- in example text
    
    Returns: (changed, count)
    """
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original_content = content
    
    # Pattern: for example geoserver-{{ version }}-
    # Replace with: for example geoserver-{{ snapshot }}-
    pattern = r'(for example geoserver-)\{\{\s*version\s*\}\}(-)'
    replacement = r'\1{{ snapshot }}\2'
    
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
    """Process all community module documentation files."""
    
    # Find all markdown files in doc/en/user/docs/community
    community_root = Path('doc/en/user/docs/community')
    md_files = list(community_root.rglob('*.md'))
    
    total_changed = 0
    total_replacements = 0
    
    print("Fixing example text in warnings...")
    print("Changing: for example geoserver-{{ version }}-")
    print("To:       for example geoserver-{{ snapshot }}-")
    print("=" * 80)
    
    for file_path in md_files:
        # Check if file contains the pattern
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            if 'for example geoserver-{{ version }}-' in content:
                changed, count = fix_example_text(file_path)
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
