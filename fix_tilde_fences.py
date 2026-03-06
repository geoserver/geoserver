#!/usr/bin/env python3
"""
Fix tilde code fences (~~~) to use backticks (```) in markdown files.
MkDocs requires backticks for code fences, not tildes.
"""

import re
from pathlib import Path

def fix_tilde_fences(file_path):
    """Replace ~~~ code fences with ``` in a file."""
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original_content = content
    
    # Replace ~~~xml with ```xml
    content = re.sub(r'~~~(\w+)', r'```\1', content)
    
    # Replace standalone ~~~ with ```
    content = re.sub(r'~~~', r'```', content)
    
    if content != original_content:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)
        return True
    return False

def main():
    doc_dir = Path('doc/en')
    
    # Find all markdown files
    md_files = list(doc_dir.rglob('*.md'))
    
    fixed_files = []
    
    for md_file in md_files:
        if fix_tilde_fences(md_file):
            fixed_files.append(str(md_file))
            print(f"Fixed: {md_file}")
    
    print(f"\nTotal files fixed: {len(fixed_files)}")
    
    if fixed_files:
        print("\nFixed files:")
        for f in fixed_files:
            print(f"  - {f}")

if __name__ == '__main__':
    main()
