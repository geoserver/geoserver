#!/usr/bin/env python3
"""
Fix links that still reference .rst files instead of .md files.
Converts patterns like [text](file.rst#anchor) to [text](file.md#anchor)
"""

import re
from pathlib import Path

def fix_rst_links(file_path):
    """Fix .rst links in a single file."""
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original_content = content
    
    # Pattern to match markdown links with .rst extension
    # Matches: [text](path/file.rst) or [text](path/file.rst#anchor)
    pattern = r'\[([^\]]+)\]\(([^)]+)\.rst(#[^)]+)?\)'
    
    def replace_link(match):
        text = match.group(1)
        path = match.group(2)
        anchor = match.group(3) if match.group(3) else ''
        return f'[{text}]({path}.md{anchor})'
    
    content = re.sub(pattern, replace_link, content)
    
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
    total_fixes = 0
    
    for doc_dir in doc_dirs:
        if not doc_dir.exists():
            continue
        
        for md_file in doc_dir.rglob('*.md'):
            if fix_rst_links(md_file):
                fixed_files.append(md_file)
                # Count how many .rst links were in the file
                with open(md_file, 'r', encoding='utf-8') as f:
                    content = f.read()
                    count = content.count('.md#') + content.count('.md)')
                total_fixes += count
                print(f'Fixed: {md_file}')
    
    print(f'\nTotal files fixed: {len(fixed_files)}')
    print(f'Total .rst links converted: {total_fixes}')
    
    if fixed_files:
        print('\nFixed files:')
        for f in fixed_files:
            print(f'  - {f}')

if __name__ == '__main__':
    main()
