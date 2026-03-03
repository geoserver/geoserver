#!/usr/bin/env python3
"""
Fix include statements inside code blocks in workshop files.
These need to be wrapped with {%raw%} tags so mkdocs-macros doesn't try to process them.
"""

import re
from pathlib import Path

def fix_code_block_includes(file_path):
    """Fix include statements inside code blocks."""
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original_content = content
    
    # Pattern: ~~~<lang>\n{% include "..." %}\n~~~
    # Replace with: ~~~<lang>\n{%raw%}{% include "..." %}{%endraw%}\n~~~
    pattern = r'(~~~\w+\n)(\{% include "[^"]+?" %\})\n(~~~)'
    replacement = r'\1{%raw%}\2{%endraw%}\n\3'
    
    content = re.sub(pattern, replacement, content)
    
    if content != original_content:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)
        return True
    return False

def main():
    """Main function."""
    files_to_fix = [
        'doc/en/user/docs/styling/workshop/css/css.md',
        'doc/en/user/docs/styling/workshop/mbstyle/mbstyle.md',
        'doc/en/user/docs/styling/workshop/ysld/ysld.md'
    ]
    
    fixed_count = 0
    for file_path in files_to_fix:
        path = Path(file_path)
        if path.exists():
            if fix_code_block_includes(path):
                print(f"Fixed: {file_path}")
                fixed_count += 1
            else:
                print(f"No changes needed: {file_path}")
        else:
            print(f"File not found: {file_path}")
    
    print(f"\nTotal files fixed: {fixed_count}")

if __name__ == '__main__':
    main()
