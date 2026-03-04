#!/usr/bin/env python3
"""
Fix workshop include paths to be relative to docs directory.
mkdocs-macros requires paths relative to docs_dir, not relative to the file.
"""

import re
from pathlib import Path

def fix_include_paths(file_path, replacements):
    """Fix include paths in a file."""
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original_content = content
    
    for old_path, new_path in replacements.items():
        content = content.replace(f'include "{old_path}"', f'include "{new_path}"')
    
    if content != original_content:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)
        return True
    return False

def main():
    """Main function."""
    # Map of file -> {old_path: new_path}
    files_to_fix = {
        'doc/en/user/docs/styling/workshop/css/css.md': {
            '../files/airports2.sld': 'styling/workshop/files/airports2.sld'
        },
        'doc/en/user/docs/styling/workshop/mbstyle/mbstyle.md': {
            '../files/airports0.sld': 'styling/workshop/files/airports0.sld',
            '../files/airports0.json': 'styling/workshop/files/airports0.json'
        },
        'doc/en/user/docs/styling/workshop/ysld/ysld.md': {
            '../files/airports0.sld': 'styling/workshop/files/airports0.sld',
            '../files/airports0.ysld': 'styling/workshop/files/airports0.ysld'
        }
    }
    
    fixed_count = 0
    for file_path, replacements in files_to_fix.items():
        path = Path(file_path)
        if path.exists():
            if fix_include_paths(path, replacements):
                print(f"Fixed: {file_path}")
                fixed_count += 1
            else:
                print(f"No changes needed: {file_path}")
        else:
            print(f"File not found: {file_path}")
    
    print(f"\nTotal files fixed: {fixed_count}")

if __name__ == '__main__':
    main()
