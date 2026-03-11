#!/usr/bin/env python3
"""
Fix all include-markdown Macro Syntax Errors across all manuals.

This script wraps {% include-markdown "..." %} statements in {%raw%}...{%endraw%}
tags to prevent mkdocs-macros from trying to parse them as Jinja2 syntax.
"""

import os
import re
from pathlib import Path

def fix_includes_in_file(filepath):
    """Fix include-markdown statements in a single file."""
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original_content = content
    changes = []
    
    # Pattern 1: {% include-markdown "..." %} NOT already wrapped in raw tags
    pattern1 = r'(?<!{%raw%})({% include-markdown "[^"]*" %})(?!{%endraw%})'
    matches1 = list(re.finditer(pattern1, content))
    if matches1:
        content = re.sub(pattern1, r'{%raw%}\1{%endraw%}', content)
        changes.append(f"{len(matches1)} include-markdown statements")
    
    # Pattern 2: {% include "..." %} NOT already wrapped in raw tags (in code blocks)
    pattern2 = r'(?<!{%raw%})({% include "[^"]*" %})(?!{%endraw%})'
    matches2 = list(re.finditer(pattern2, content))
    if matches2:
        content = re.sub(pattern2, r'{%raw%}\1{%endraw%}', content)
        changes.append(f"{len(matches2)} include statements")
    
    # Pattern 3: Fix incomplete raw tags (Missing end of raw directive)
    # {%raw%}<!-- Include path goes outside docs directory: ... -->
    # Should be: {%raw%}<!-- Include path goes outside docs directory: ... -->{%endraw%}
    pattern3 = r'({%raw%}<!--[^>]*-->)(?!{%endraw%})'
    matches3 = list(re.finditer(pattern3, content))
    if matches3:
        content = re.sub(pattern3, r'\1{%endraw%}', content)
        changes.append(f"{len(matches3)} incomplete raw tags")
    
    # Pattern 4: Fix malformed include statements (missing {% %})
    # >   include "./ComponentInfo.java"
    # Should be wrapped or commented out
    pattern4 = r'^(\s*>?\s*include\s+"[^"]*")$'
    matches4 = list(re.finditer(pattern4, content, re.MULTILINE))
    if matches4:
        content = re.sub(pattern4, r'{%raw%}<!-- \1 -->{%endraw%}', content, flags=re.MULTILINE)
        changes.append(f"{len(matches4)} malformed include statements")
    
    if content != original_content:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        return changes
    return None

def main():
    """Main function to fix all include errors across all manuals."""
    base_dir = Path('doc/en')
    
    if not base_dir.exists():
        print(f"Error: {base_dir} does not exist")
        return
    
    # Process all three manuals
    manuals = ['user', 'developer', 'docguide']
    
    total_fixed = 0
    
    for manual in manuals:
        manual_dir = base_dir / manual / 'docs'
        if not manual_dir.exists():
            print(f"Skipping {manual} (directory not found)")
            continue
        
        print(f"\n=== Processing {manual.upper()} manual ===")
        fixed_count = 0
        
        # Process all .md files recursively
        for md_file in manual_dir.rglob('*.md'):
            changes = fix_includes_in_file(md_file)
            if changes:
                fixed_count += 1
                total_fixed += 1
                rel_path = md_file.relative_to(manual_dir)
                print(f"Fixed {rel_path}: {', '.join(changes)}")
        
        print(f"Files fixed in {manual}: {fixed_count}")
    
    print(f"\n=== SUMMARY ===")
    print(f"Total files fixed: {total_fixed}")

if __name__ == '__main__':
    main()
