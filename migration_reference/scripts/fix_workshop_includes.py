#!/usr/bin/env python3
"""
Fix include statement paths in workshop files.

The mkdocs-macros plugin requires include paths to be relative to the docs root,
not relative to the current file location.
"""

import os
import re
from pathlib import Path

def fix_include_paths(file_path, docs_root):
    """Fix include paths in a markdown file to be relative to docs root."""
    
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Find all include statements
    include_pattern = r'{%\s*include\s+"([^"]+)"\s*%}'
    
    def replace_include(match):
        old_path = match.group(1)
        
        # If path is already absolute or doesn't start with ../, leave it
        if not old_path.startswith('../'):
            return match.group(0)
        
        # Calculate the absolute path of the included file
        file_dir = os.path.dirname(file_path)
        included_file = os.path.normpath(os.path.join(file_dir, old_path))
        
        # Make it relative to docs root
        try:
            rel_path = os.path.relpath(included_file, docs_root)
            # Convert backslashes to forward slashes for consistency
            rel_path = rel_path.replace('\\', '/')
            return f'{{% include "{rel_path}" %}}'
        except ValueError:
            # If we can't make it relative, leave it unchanged
            return match.group(0)
    
    original_content = content
    content = re.sub(include_pattern, replace_include, content)
    
    if content != original_content:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)
        return True
    
    return False

def main():
    """Main function to fix all workshop include paths."""
    
    # Define the docs root and workshop directories
    docs_root = 'doc/en/user/docs'
    workshop_dirs = [
        'doc/en/user/docs/styling/workshop/css',
        'doc/en/user/docs/styling/workshop/mbstyle',
        'doc/en/user/docs/styling/workshop/ysld'
    ]
    
    fixed_files = []
    
    for workshop_dir in workshop_dirs:
        if not os.path.exists(workshop_dir):
            print(f"Warning: Directory not found: {workshop_dir}")
            continue
        
        # Process all .md files in the directory
        for md_file in Path(workshop_dir).glob('*.md'):
            if fix_include_paths(str(md_file), docs_root):
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
