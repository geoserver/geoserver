#!/usr/bin/env python3
"""
Fix include-markdown closing syntax.
Changes "%} to " %} (adds space before %})
"""

import re
from pathlib import Path

def fix_include_markdown_closing(file_path):
    """Fix include-markdown closing syntax in a single file."""
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original_content = content
    
    # Pattern to match include-markdown with incorrect closing
    # Matches: {% include-markdown "path"%} 
    # Should be: {% include-markdown "path" %}
    pattern = r'(\{% include-markdown\s+"[^"]+")(%\})'
    replacement = r'\1 \2'
    
    content = re.sub(pattern, replacement, content)
    
    if content != original_content:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)
        return True
    return False

def main():
    """Process all markdown files in doc directories."""
    doc_dirs = [
        Path('doc/en/user/docs'),
        Path('doc/en/developer/docs'),
        Path('doc/en/docguide/docs')
    ]
    
    fixed_files = []
    
    for doc_dir in doc_dirs:
        if not doc_dir.exists():
            continue
        
        for md_file in doc_dir.rglob('*.md'):
            if fix_include_markdown_closing(md_file):
                fixed_files.append(md_file)
                print(f'Fixed: {md_file}')
    
    print(f'\nTotal files fixed: {len(fixed_files)}')
    
    if fixed_files:
        print('\nFixed files:')
        for f in fixed_files:
            print(f'  - {f}')

if __name__ == '__main__':
    main()
