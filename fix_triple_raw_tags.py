#!/usr/bin/env python3
"""
Fix triple {%raw%} and {%endraw%} tags that cause macro syntax errors.
These should be single tags, not triple.
"""

import re
from pathlib import Path

def fix_triple_raw_tags(file_path):
    """Fix triple raw/endraw tags in a file."""
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original_content = content
    
    # Replace triple raw tags with single raw tags
    content = re.sub(r'\{%raw%\}\{%raw%\}\{%raw%\}', r'{%raw%}', content)
    content = re.sub(r'\{%endraw%\}\{%endraw%\}\{%endraw%\}', r'{%endraw%}', content)
    
    if content != original_content:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)
        return True
    return False

def main():
    """Main function."""
    files_to_fix = [
        'doc/en/user/docs/installation/upgrade.md',
        'doc/en/user/docs/community/elasticsearch/index.md',
        'doc/en/developer/docs/cite-test-guide/index.md'
    ]
    
    fixed_count = 0
    for file_path in files_to_fix:
        path = Path(file_path)
        if path.exists():
            if fix_triple_raw_tags(path):
                print(f"Fixed: {file_path}")
                fixed_count += 1
            else:
                print(f"No changes needed: {file_path}")
        else:
            print(f"File not found: {file_path}")
    
    print(f"\nTotal files fixed: {fixed_count}")

if __name__ == '__main__':
    main()
