#!/usr/bin/env python3
"""Fix wildcard image references to use .png extension"""

import re
from pathlib import Path

def fix_wildcard_images(file_path: Path):
    """Replace wildcard image references with .svg"""
    content = file_path.read_text(encoding='utf-8')
    original = content
    
    # Replace .*) with .svg)
    content = re.sub(r'(\!\[.*?\]\(.*?)\.\*\)', r'\1.svg)', content)
    
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
        if fix_wildcard_images(path):
            print(f"Fixed: {file_path}")
            fixed += 1
    else:
        print(f"Not found: {file_path}")

print(f"\nTotal files fixed: {fixed}")
