#!/usr/bin/env python3
"""
Fix anchor syntax {#anchor} that conflicts with Jinja2 comment tags.
Wraps {#anchor} patterns in {%raw%}...{%endraw%} tags.
"""

import re
from pathlib import Path

def fix_anchor_conflicts(file_path):
    """Fix {#anchor} syntax conflicts with Jinja2 in a single file."""
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original_content = content
    
    # Pattern to match {#anchor} syntax that's NOT already wrapped in {%raw%}
    # This matches heading lines with {#anchor} at the end
    pattern = r'(^#{1,6}\s+.+?)\s+(\{#[a-zA-Z0-9_-]+\})(\s*)$'
    
    def replace_anchor(match):
        heading = match.group(1)
        anchor = match.group(2)
        trailing = match.group(3)
        return f'{heading} {{%raw%}}{anchor}{{%endraw%}}{trailing}'
    
    content = re.sub(pattern, replace_anchor, content, flags=re.MULTILINE)
    
    # Also fix standalone {#anchor} patterns (like in workspaces.md)
    # Pattern: ::: {#anchor} or :::: {#anchor}
    pattern2 = r'(^:::+)\s+(\{#[a-zA-Z0-9_-]+\})(\s*)$'
    
    def replace_directive_anchor(match):
        directive = match.group(1)
        anchor = match.group(2)
        trailing = match.group(3)
        return f'{directive} {{%raw%}}{anchor}{{%endraw%}}{trailing}'
    
    content = re.sub(pattern2, replace_directive_anchor, content, flags=re.MULTILINE)
    
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
            if fix_anchor_conflicts(md_file):
                fixed_files.append(md_file)
                print(f'Fixed: {md_file}')
    
    print(f'\nTotal files fixed: {len(fixed_files)}')
    
    if fixed_files:
        print('\nFixed files:')
        for f in fixed_files:
            print(f'  - {f}')

if __name__ == '__main__':
    main()
