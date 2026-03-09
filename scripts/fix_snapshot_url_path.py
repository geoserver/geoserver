#!/usr/bin/env python3
"""
Fix snapshot URL paths to use ext-latest/ instead of extensions/.

Snapshot builds are located at:
https://build.geoserver.org/geoserver/main/ext-latest/

Not at:
https://build.geoserver.org/geoserver/main/extensions/
"""

import re
from pathlib import Path

def fix_snapshot_url_path(file_path: Path) -> tuple[bool, int]:
    """
    Replace /extensions/ with /ext-latest/ in snapshot URLs.
    
    Returns: (changed, count)
    """
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original_content = content
    
    # Pattern: build.geoserver.org/geoserver/main/extensions/
    # Replace with: build.geoserver.org/geoserver/main/ext-latest/
    pattern = r'(build\.geoserver\.org/geoserver/main/)extensions/'
    replacement = r'\1ext-latest/'
    
    content = re.sub(pattern, replacement, content)
    
    changed = content != original_content
    
    if changed:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)
        
        # Count replacements
        count = original_content.count('/extensions/') - content.count('/extensions/')
        return True, count
    
    return False, 0

def main():
    """Process all documentation files with snapshot URLs."""
    
    # Find all markdown files in doc/en/user/docs
    doc_root = Path('doc/en/user/docs')
    md_files = list(doc_root.rglob('*.md'))
    
    total_changed = 0
    total_replacements = 0
    
    print("Fixing snapshot URL paths (extensions/ → ext-latest/)...")
    print("=" * 80)
    
    for file_path in md_files:
        # Check if file contains the pattern
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            if 'build.geoserver.org/geoserver/main/extensions/' in content:
                changed, count = fix_snapshot_url_path(file_path)
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
