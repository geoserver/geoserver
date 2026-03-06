#!/usr/bin/env python3
"""
Fix duplicate render_macros frontmatter in converted Markdown files.
"""

import os
import re
from pathlib import Path

def fix_duplicate_frontmatter(file_path):
    """Remove duplicate render_macros frontmatter from a file."""
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Pattern to match duplicate frontmatter
    pattern = r'^---\nrender_macros: true\n---\n\n---\nrender_macros: true\n---\n'
    
    if re.match(pattern, content):
        # Replace with single frontmatter
        fixed_content = re.sub(pattern, '---\nrender_macros: true\n---\n', content, count=1)
        
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(fixed_content)
        
        return True
    
    return False

def main():
    """Find and fix all files with duplicate frontmatter."""
    docs_dir = Path('doc/en/user/docs')
    
    if not docs_dir.exists():
        print(f"Error: {docs_dir} does not exist")
        return
    
    fixed_count = 0
    
    for md_file in docs_dir.rglob('*.md'):
        if fix_duplicate_frontmatter(md_file):
            print(f"Fixed: {md_file}")
            fixed_count += 1
    
    print(f"\nTotal files fixed: {fixed_count}")

if __name__ == '__main__':
    main()
