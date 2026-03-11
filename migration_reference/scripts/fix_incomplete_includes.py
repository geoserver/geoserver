#!/usr/bin/env python3
"""
Fix incomplete include statements with only start parameter (missing end).

These are invalid Jinja2 syntax and need to be commented out or wrapped.
"""

import os
import re
from pathlib import Path

def fix_incomplete_includes(file_path):
    """Fix include statements that have start but no end parameter."""
    
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original_content = content
    changes = []
    
    # Pattern 1: {% include "path" start="..." %} (missing end parameter)
    pattern1 = r'{%\s*include\s+"([^"]+)"\s+start="([^"]+)"\s*%}'
    
    # Pattern 2: {%raw%}{%endraw%}{% include ... %} (malformed raw tags)
    pattern2 = r'{%raw%}{%endraw%}{%\s*include\s+"([^"]+)"[^}]*%}'
    
    def replace_incomplete_include(match):
        path = match.group(1)
        start_marker = match.group(2) if len(match.groups()) > 1 else "unknown"
        
        # Check if path goes outside docs directory
        if path.startswith('../../../'):
            # Comment it out
            return f'<!-- Include with incomplete parameters: {path} -->\n<!-- Start marker: "{start_marker}" -->\n<!-- TODO: Copy relevant section to docs directory -->'
        else:
            # Wrap in raw tags (for code blocks)
            return f'{{%raw%}}{{% include "{path}" start="{start_marker}" %}}{{%endraw%}}'
    
    def replace_malformed_raw(match):
        path = match.group(1)
        # Comment it out since it's malformed
        return f'<!-- Malformed include statement: {path} -->\n<!-- TODO: Fix or remove -->'
    
    if re.search(pattern1, content):
        content = re.sub(pattern1, replace_incomplete_include, content)
        changes.append("Fixed incomplete include statements (missing end parameter)")
    
    if re.search(pattern2, content):
        content = re.sub(pattern2, replace_malformed_raw, content)
        changes.append("Fixed malformed {%raw%}{%endraw%} tags")
    
    if content != original_content:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)
        return changes
    
    return None

def main():
    """Main function to fix all incomplete includes."""
    
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
            changes = fix_incomplete_includes(str(md_file))
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
