#!/usr/bin/env python3
"""
Fix YAML references to MD references in community modules.

Similar to REST API fix, but for community module documentation.
"""

import os
import re
from pathlib import Path

def fix_yaml_refs_in_file(filepath):
    """Fix YAML references to MD references in a single file."""
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original_content = content
    
    # Replace api/*.yaml with api/*.md
    content = re.sub(r'\(api/([^)]+)\.yaml\)', r'(api/\1.md)', content)
    
    if content != original_content:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        return True
    return False

def main():
    """Main function to fix all community YAML references."""
    community_dir = Path('doc/en/user/docs/community')
    
    if not community_dir.exists():
        print(f"Error: {community_dir} does not exist")
        return
    
    fixed_count = 0
    total_replacements = 0
    
    # Process all .md files in community directory recursively
    for md_file in community_dir.rglob('*.md'):
        if fix_yaml_refs_in_file(md_file):
            # Count replacements
            with open(md_file, 'r', encoding='utf-8') as f:
                content = f.read()
            count = len(re.findall(r'\(api/[^)]+\.md\)', content))
            total_replacements += count
            fixed_count += 1
            print(f"Fixed {md_file.relative_to(community_dir)}: {count} references")
    
    print(f"\nSummary:")
    print(f"  Files fixed: {fixed_count}")
    print(f"  Total YAML→MD replacements: {total_replacements}")

if __name__ == '__main__':
    main()
