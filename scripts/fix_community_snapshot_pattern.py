#!/usr/bin/env python3
"""
Fix community module URLs to use {{ snapshot }} consistently.

Change from: geoserver-{{ version }}-SNAPSHOT-
Change to:   geoserver-{{ snapshot }}-

This ensures consistency across all download links.
"""

import re
from pathlib import Path

def fix_community_snapshot_pattern(file_path: Path) -> tuple[bool, int]:
    """
    Replace geoserver-{{ version }}-SNAPSHOT- with geoserver-{{ snapshot }}-
    
    Returns: (changed, count)
    """
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original_content = content
    
    # Pattern: geoserver-{{ version }}-SNAPSHOT-
    # Replace with: geoserver-{{ snapshot }}-
    pattern = r'geoserver-\{\{\s*version\s*\}\}-SNAPSHOT-'
    replacement = r'geoserver-{{ snapshot }}-'
    
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
    
    print("Fixing community module snapshot patterns...")
    print("Changing: geoserver-{{ version }}-SNAPSHOT-")
    print("To:       geoserver-{{ snapshot }}-")
    print("=" * 80)
    
    for file_path in md_files:
        # Check if file contains the pattern
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            if 'geoserver-{{ version }}-SNAPSHOT-' in content or 'geoserver-{{version}}-SNAPSHOT-' in content:
                changed, count = fix_community_snapshot_pattern(file_path)
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
