#!/usr/bin/env python3
"""
Fix include statements in imagemosaic tutorial files:
1. Restore malformed include statements
2. Add missing {%endraw%} tags
"""

import re
from pathlib import Path

def fix_imagemosaic_includes(file_path):
    """Fix include statements in imagemosaic files."""
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original_content = content
    
    # Fix 1: Restore malformed include statements
    # Replace: <!-- Malformed include statement: path -->\n<!-- TODO: Fix or remove -->
    # With: {%raw%}{% include "path" %}{%endraw%}
    pattern1 = r'<!-- Malformed include statement: ([^\s]+) -->\n<!-- TODO: Fix or remove -->'
    replacement1 = r'{%raw%}{% include "\1" %}{%endraw%}'
    content = re.sub(pattern1, replacement1, content)
    
    # Fix 2: Add missing {%endraw%} tags
    # Pattern: {%raw%}{% include "..." %}\n~~~
    # Replace with: {%raw%}{% include "..." %}{%endraw%}\n~~~
    pattern2 = r'(\{%raw%\}\{% include "[^"]+?" %\})\n(~~~)'
    replacement2 = r'\1{%endraw%}\n\2'
    content = re.sub(pattern2, replacement2, content)
    
    if content != original_content:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)
        return True
    return False

def main():
    """Main function."""
    files_to_fix = [
        'doc/en/user/docs/tutorials/imagemosaic_timeseries/imagemosaic_timeseries.md',
        'doc/en/user/docs/tutorials/imagemosaic_timeseries/imagemosaic_time-elevationseries.md'
    ]
    
    fixed_count = 0
    for file_path in files_to_fix:
        path = Path(file_path)
        if path.exists():
            if fix_imagemosaic_includes(path):
                print(f"Fixed: {file_path}")
                fixed_count += 1
            else:
                print(f"No changes needed: {file_path}")
        else:
            print(f"File not found: {file_path}")
    
    print(f"\nTotal files fixed: {fixed_count}")

if __name__ == '__main__':
    main()
