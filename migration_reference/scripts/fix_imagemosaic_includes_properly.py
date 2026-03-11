#!/usr/bin/env python3
"""
Fix imagemosaic include statements by removing raw tags.
Include statements need to execute, not be escaped.
"""

import re
from pathlib import Path

def fix_imagemosaic_includes(file_path):
    """Fix include statements in imagemosaic files."""
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original_content = content
    
    # Pattern: {%raw%}{% include "path" %}{%endraw%}
    # Replace with: {% include "path" %}
    pattern = r'\{%raw%\}(\{% include "[^"]+?" %\})\{%endraw%\}'
    replacement = r'\1'
    
    content = re.sub(pattern, replacement, content)
    
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
