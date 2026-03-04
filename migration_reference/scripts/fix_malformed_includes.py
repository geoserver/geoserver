#!/usr/bin/env python3
"""
Fix malformed include statements in markdown files.

The conversion tool sometimes creates include statements without proper Jinja2 syntax.
This script finds patterns like:
  >   include "path/to/file"
And converts them to:
  > {% include "path/to/file" %}
"""

import os
import re
from pathlib import Path

def fix_malformed_includes(file_path):
    """Fix malformed include statements in a markdown file."""
    
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Pattern 1: Lines with ">   include" (blockquote with include)
    # Example: >   include "path/to/file"
    pattern1 = r'^(\s*>?\s*)include\s+"([^"]+)"'
    
    # Pattern 2: Bare include statements in code blocks
    # Example: ```markdown\n      >   include "path"\n```
    pattern2 = r'(```(?:markdown)?.*?\n)(.*?include\s+"[^"]+".*)(\n```)'
    
    original_content = content
    
    # Fix pattern 1: Direct include statements
    def replace_include(match):
        prefix = match.group(1)
        path = match.group(2)
        # Check if it's already wrapped in {% %}
        if '{%' in prefix or '%}' in match.group(0):
            return match.group(0)
        return f'{prefix}{{% include "{path}" %}}'
    
    content = re.sub(pattern1, replace_include, content, flags=re.MULTILINE)
    
    # Fix pattern 2: Include statements in code blocks
    def replace_code_block_include(match):
        before = match.group(1)
        include_line = match.group(2)
        after = match.group(3)
        
        # Check if already has {% %}
        if '{%' in include_line and '%}' in include_line:
            return match.group(0)
        
        # Extract the path
        path_match = re.search(r'include\s+"([^"]+)"', include_line)
        if path_match:
            path = path_match.group(1)
            # Replace the include line
            fixed_line = re.sub(r'include\s+"[^"]+"', f'{{% include "{path}" %}}', include_line)
            return before + fixed_line + after
        
        return match.group(0)
    
    content = re.sub(pattern2, replace_code_block_include, content, flags=re.DOTALL)
    
    if content != original_content:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)
        return True
    
    return False

def main():
    """Main function to fix all malformed includes."""
    
    # Define the directories to search
    search_dirs = [
        'doc/en/user/docs',
        'doc/en/developer/docs',
        'doc/en/docguide/docs'
    ]
    
    fixed_files = []
    
    for search_dir in search_dirs:
        if not os.path.exists(search_dir):
            print(f"Warning: Directory not found: {search_dir}")
            continue
        
        # Process all .md files recursively
        for md_file in Path(search_dir).rglob('*.md'):
            if fix_malformed_includes(str(md_file)):
                fixed_files.append(str(md_file))
                print(f"Fixed: {md_file}")
    
    if fixed_files:
        print(f"\nTotal files fixed: {len(fixed_files)}")
        print("\nFixed files:")
        for f in fixed_files:
            print(f"  - {f}")
    else:
        print("No files needed fixing.")

if __name__ == '__main__':
    main()
