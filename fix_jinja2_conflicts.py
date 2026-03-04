#!/usr/bin/env python3
"""
Fix Jinja2 syntax conflicts in Markdown files.

Issues to fix:
1. [text]{#anchor} patterns that look like Jinja2 comments
2. Other patterns that conflict with Jinja2 syntax
"""

import os
import re

def fix_jinja2_conflicts(content):
    """Fix patterns that conflict with Jinja2 syntax."""
    fixes = 0
    
    # Pattern 1: [text]{#anchor} - wrap in raw tags
    # This pattern is used for anchor links but looks like Jinja2 comment start
    pattern = r'(\[.*?\]\{#[^}]+\})'
    matches = list(re.finditer(pattern, content))
    
    if matches:
        # Replace from end to start to preserve positions
        for match in reversed(matches):
            original = match.group(1)
            replacement = f'{{%raw%}}{original}{{%endraw%}}'
            content = content[:match.start()] + replacement + content[match.end():]
            fixes += 1
    
    return content, fixes

def process_file(filepath):
    """Process a single markdown file."""
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
        
        fixed_content, fixes = fix_jinja2_conflicts(content)
        
        if fixes > 0:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(fixed_content)
            return fixes
        
        return 0
    except Exception as e:
        print(f"Error processing {filepath}: {e}")
        return 0

def main():
    """Process all markdown files in documentation directories."""
    base_dirs = [
        'doc/en/user/docs',
        'doc/en/developer/docs',
        'doc/en/docguide/docs'
    ]
    
    total_fixes = 0
    fixed_files = []
    
    for base_dir in base_dirs:
        if not os.path.exists(base_dir):
            continue
        
        print(f"\nSearching in {base_dir}...")
        
        for root, dirs, files in os.walk(base_dir):
            for file in files:
                if file.endswith('.md'):
                    filepath = os.path.join(root, file)
                    fixes = process_file(filepath)
                    if fixes > 0:
                        total_fixes += fixes
                        fixed_files.append((filepath, fixes))
                        print(f"  Fixed: {filepath} ({fixes} patterns)")
    
    print(f"\n{'='*60}")
    print(f"Total: Fixed {total_fixes} Jinja2 conflict patterns in {len(fixed_files)} files")
    
    if fixed_files:
        print("\nFixed files:")
        for filepath, fixes in fixed_files:
            print(f"  - {filepath} ({fixes} patterns)")

if __name__ == '__main__':
    main()
