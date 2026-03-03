#!/usr/bin/env python3
"""
Fix anchor syntax that conflicts with Jinja2 template syntax.

Markdown uses {#anchor} syntax for custom anchors, but mkdocs-macros
interprets {...} as Jinja2 template syntax, causing "Missing end of comment tag" errors.

We need to escape these by wrapping them in {%raw%}...{%endraw%}.
"""

import os
import re
from pathlib import Path

def fix_anchor_syntax(file_path):
    """Fix {#anchor} syntax that conflicts with Jinja2."""
    
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original_content = content
    changes = []
    
    # Pattern 1: [text]{#anchor} - inline anchor
    pattern1 = r'(\[([^\]]+)\])\{#([^}]+)\}'
    
    # Pattern 2: []{#anchor} {: #anchor } - heading anchor (Material for MkDocs syntax)
    pattern2 = r'(\[([^\]]*)\])\{#([^}]+)\}\s*\{:\s*#([^}]+)\s*\}'
    
    def replace_inline_anchor(match):
        full_match = match.group(0)
        text = match.group(2)
        anchor = match.group(3)
        # Wrap in raw tags to prevent Jinja2 processing
        return f'{{%raw%}}[{text}]{{#{anchor}}}{{%endraw%}}'
    
    def replace_heading_anchor(match):
        full_match = match.group(0)
        text = match.group(2)
        anchor1 = match.group(3)
        anchor2 = match.group(4)
        # Wrap in raw tags
        return f'{{%raw%}}[{text}]{{#{anchor1}}} {{: #{anchor2} }}{{%endraw%}}'
    
    if re.search(pattern2, content):
        content = re.sub(pattern2, replace_heading_anchor, content)
        changes.append("Fixed heading anchor syntax with {: #anchor }")
    
    if re.search(pattern1, content):
        content = re.sub(pattern1, replace_inline_anchor, content)
        changes.append("Fixed inline anchor syntax {#anchor}")
    
    if content != original_content:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)
        return changes
    
    return None

def main():
    """Main function to fix all anchor syntax issues."""
    
    # Define the directories to search
    search_dirs = [
        'doc/en/user/docs',
        'doc/en/developer/docs',
        'doc/en/docguide/docs'
    ]
    
    fixed_files = {}
    
    for search_dir in search_dirs:
        if not os.path.exists(search_dir):
            print(f"Warning: Directory not found: {search_dir}")
            continue
        
        # Process all .md files recursively
        for md_file in Path(search_dir).rglob('*.md'):
            changes = fix_anchor_syntax(str(md_file))
            if changes:
                fixed_files[str(md_file)] = changes
                print(f"Fixed: {md_file}")
                for change in changes:
                    print(f"  - {change}")
    
    if fixed_files:
        print(f"\nTotal files fixed: {len(fixed_files)}")
    else:
        print("No files needed fixing.")

if __name__ == '__main__':
    main()
