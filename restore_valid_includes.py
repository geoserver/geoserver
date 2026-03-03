#!/usr/bin/env python3
"""
Restore valid include statements that were incorrectly commented out.

Files that exist in the docs directory should be included properly.
"""

import os
import re
from pathlib import Path

def restore_valid_includes(file_path):
    """Restore include statements for files that exist."""
    
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original_content = content
    changes = []
    
    # Pattern: <!-- Malformed include statement: path -->
    # followed by <!-- TODO: Fix or remove -->
    pattern = r'<!-- Malformed include statement: ([^\s]+) -->\s*\n<!-- TODO: Fix or remove -->'
    
    def replace_with_include(match):
        path = match.group(1)
        
        # Calculate the full path relative to the file
        file_dir = os.path.dirname(file_path)
        docs_root = 'doc/en/user/docs' if '/user/docs/' in file_path else 'doc/en/developer/docs'
        
        # Check if the file exists
        # The path in the comment is relative to docs root
        full_path = os.path.join(docs_root, path)
        
        if os.path.exists(full_path):
            # File exists, restore the include
            return f'{{% include "{path}" %}}'
        else:
            # File doesn't exist, keep the comment
            return match.group(0)
    
    if re.search(pattern, content):
        content = re.sub(pattern, replace_with_include, content)
        changes.append("Restored valid include statements")
    
    if content != original_content:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)
        return changes
    
    return None

def main():
    """Main function to restore all valid includes."""
    
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
            changes = restore_valid_includes(str(md_file))
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
