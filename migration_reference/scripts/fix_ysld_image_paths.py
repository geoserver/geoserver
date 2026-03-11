#!/usr/bin/env python3
"""Fix YSLD reference image paths that have too many ../"""

import re
from pathlib import Path

def fix_ysld_paths(file_path: Path):
    """Fix image paths in YSLD reference files"""
    content = file_path.read_text(encoding='utf-8')
    original = content
    
    # Replace ../../../img/ with img/ (for files in reference/)
    # Replace ../../../../img/ with img/ (for files in reference/symbolizers/)
    
    if 'symbolizers' in str(file_path):
        # Files in reference/symbolizers/ need img/ (same directory)
        content = re.sub(r'\!\[(.*?)\]\(\.\.\/\.\.\/\.\.\/\.\./img/', r'![\1](img/', content)
        content = re.sub(r'\!\[(.*?)\]\(\.\.\/img/', r'![\1](img/', content)
    else:
        # Files in reference/ need img/
        content = re.sub(r'\!\[(.*?)\]\(\.\.\/\.\.\/\.\./img/', r'![\1](img/', content)
    
    # Fix fs_roadcasing to use .png instead of .svg (only PNG exists)
    content = content.replace('img/fs_roadcasing.svg', 'img/fs_roadcasing.png')
    
    if content != original:
        file_path.write_text(content, encoding='utf-8')
        return True
    return False

# Fix the files
files_to_fix = [
    'doc/en/user/docs/styling/ysld/reference/featurestyles.md',
    'doc/en/user/docs/styling/ysld/reference/structure.md',
    'doc/en/user/docs/styling/ysld/reference/symbolizers/index.md'
]

fixed = 0
for file_path in files_to_fix:
    path = Path(file_path)
    if path.exists():
        if fix_ysld_paths(path):
            print(f"Fixed: {file_path}")
            fixed += 1
    else:
        print(f"Not found: {file_path}")

print(f"\nTotal files fixed: {fixed}")
