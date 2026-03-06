#!/usr/bin/env python3
"""
Fix workshop include statements by moving them outside code blocks.
Include statements cannot be inside code blocks - they need to be outside
and the included content will be rendered as code.
"""

import re
from pathlib import Path

def fix_workshop_includes(file_path):
    """Fix include statements in workshop files."""
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original_content = content
    
    # Pattern: ~~~<lang>\n{%raw%}{% include "path" %}{%endraw%}\n~~~
    # Replace with: ~~~<lang>\n{% include "path" %}\n~~~
    # This removes the raw tags so the include actually executes
    pattern = r'(~~~\w+)\n\{%raw%\}(\{% include "[^"]+?" %\})\{%endraw%\}\n(~~~)'
    replacement = r'\1\n\2\n\3'
    
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
            if fix_workshop_includes(path):
                print(f"Fixed: {file_path}")
                fixed_count += 1
            else:
                print(f"No changes needed: {file_path}")
        else:
            print(f"File not found: {file_path}")
    
    print(f"\nTotal files fixed: {fixed_count}")

if __name__ == '__main__':
    main()
