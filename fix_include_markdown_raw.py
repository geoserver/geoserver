#!/usr/bin/env python3
"""
Fix include-markdown statements by wrapping them in raw tags.

The mkdocs-macros plugin tries to parse include-markdown as Jinja2 syntax,
causing Macro Syntax Errors. We need to wrap these in {%raw%}...{%endraw%} tags.
"""

import os
import re
from pathlib import Path

def fix_include_markdown_in_file(filepath):
    """Fix include-markdown statements in a single file."""
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original_content = content
    
    # Pattern to match {% include-markdown "..." %}
    # We need to wrap it in {%raw%}...{%endraw%}
    pattern = r'(\s*)({% include-markdown "([^"]+)" %})'
    
    def replace_func(match):
        indent = match.group(1)
        full_statement = match.group(2)
        # Wrap in raw tags
        return f'{indent}{{%raw%}}{full_statement}{{%endraw%}}'
    
    content = re.sub(pattern, replace_func, content)
    
    if content != original_content:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        return True
    return False

def main():
    """Main function to fix all include-markdown statements."""
    docs_dir = Path('doc/en/user/docs')
    
    if not docs_dir.exists():
        print(f"Error: {docs_dir} does not exist")
        return
    
    fixed_count = 0
    total_replacements = 0
    
    # Process all .md files recursively
    for md_file in docs_dir.rglob('*.md'):
        if fix_include_markdown_in_file(md_file):
            # Count replacements
            with open(md_file, 'r', encoding='utf-8') as f:
                content = f.read()
            count = content.count('{%raw%}{% include-markdown')
            total_replacements += count
            fixed_count += 1
            print(f"Fixed {md_file.relative_to(docs_dir)}: {count} include-markdown statements")
    
    print(f"\nSummary:")
    print(f"  Files fixed: {fixed_count}")
    print(f"  Total include-markdown statements wrapped: {total_replacements}")

if __name__ == '__main__':
    main()
