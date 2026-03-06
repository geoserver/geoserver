#!/usr/bin/env python3
"""
Fix remaining include statements that need raw tags.

Some include statements in code blocks or examples need to be wrapped
in {%raw%}...{%endraw%} tags to prevent mkdocs-macros from processing them.
"""

import os
import re
from pathlib import Path

def fix_includes_in_file(filepath):
    """Fix include statements in a single file."""
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original_content = content
    
    # Pattern to match {% include "..." %} that are NOT already wrapped in raw tags
    # and are inside code blocks (```text, ```json, etc.)
    pattern = r'(```(?:text|json|markdown|md)\s*\n)(.*?{% include "[^"]*" %}.*?)(\n```)'
    
    def replace_func(match):
        opening = match.group(1)
        middle = match.group(2)
        closing = match.group(3)
        
        # Check if already wrapped in raw tags
        if '{%raw%}' in middle and '{%endraw%}' in middle:
            return match.group(0)  # Already wrapped, don't change
        
        # Wrap the include statement in raw tags
        middle_fixed = re.sub(
            r'({% include "[^"]*" %})',
            r'{%raw%}\1{%endraw%}',
            middle
        )
        
        return opening + middle_fixed + closing
    
    content = re.sub(pattern, replace_func, content, flags=re.DOTALL)
    
    if content != original_content:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        return True
    return False

def main():
    """Main function to fix all remaining include statements."""
    docs_dir = Path('doc/en/user/docs')
    
    if not docs_dir.exists():
        print(f"Error: {docs_dir} does not exist")
        return
    
    fixed_count = 0
    
    # Process all .md files recursively
    for md_file in docs_dir.rglob('*.md'):
        if fix_includes_in_file(md_file):
            fixed_count += 1
            print(f"Fixed {md_file.relative_to(docs_dir)}")
    
    print(f"\nSummary:")
    print(f"  Files fixed: {fixed_count}")

if __name__ == '__main__':
    main()
