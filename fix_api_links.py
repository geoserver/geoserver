#!/usr/bin/env python3
"""
Fix API YAML links in REST documentation to use absolute paths.

This script updates all relative API links (api/*.yaml) to absolute paths (/api/*.yaml)
so they resolve correctly in the MkDocs build output.
"""

import re
from pathlib import Path


def fix_api_links_in_file(file_path: Path) -> bool:
    """
    Fix API links in a single Markdown file.
    
    Args:
        file_path: Path to the Markdown file
        
    Returns:
        True if file was modified, False otherwise
    """
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        original_content = content
        
        # Pattern to match: [text](api/filename.yaml)
        # Replace with: [text](/api/filename.yaml)
        # Only replace if it doesn't already start with /
        pattern = r'\]\(api/([^)]+\.yaml)\)'
        replacement = r'](/api/\1)'
        
        content = re.sub(pattern, replacement, content)
        
        if content != original_content:
            with open(file_path, 'w', encoding='utf-8') as f:
                f.write(content)
            return True
        
        return False
        
    except Exception as e:
        print(f"Error processing {file_path}: {e}")
        return False


def main():
    """Main function to fix API links in all REST documentation files."""
    
    # Find all Markdown files in the REST documentation
    docs_dir = Path('doc/en/user/docs')
    rest_dir = docs_dir / 'rest'
    
    if not rest_dir.exists():
        print(f"Error: REST documentation directory not found: {rest_dir}")
        return 1
    
    # Find all Markdown files
    md_files = list(rest_dir.rglob('*.md'))
    
    # Also check community and extensions directories for API links
    for subdir in ['community', 'extensions']:
        subdir_path = docs_dir / subdir
        if subdir_path.exists():
            md_files.extend(subdir_path.rglob('*.md'))
    
    print(f"Found {len(md_files)} Markdown files to check")
    
    modified_count = 0
    
    for md_file in md_files:
        if fix_api_links_in_file(md_file):
            print(f"Fixed: {md_file.relative_to(docs_dir)}")
            modified_count += 1
    
    print(f"\nFixed {modified_count} files")
    
    return 0


if __name__ == '__main__':
    exit(main())
