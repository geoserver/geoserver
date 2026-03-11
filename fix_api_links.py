#!/usr/bin/env python3
"""
Fix API YAML links in REST documentation to use the api_url macro.

This script updates all API links to use {{ api_url }}/filename.yaml
which is a macro defined in doc/version.py that resolves to the correct
relative path regardless of deployment structure.
"""

import re
from pathlib import Path


def fix_api_links_in_file(file_path: Path) -> bool:
    """
    Fix API links in a single Markdown file to use the api_url macro.
    
    Args:
        file_path: Path to the Markdown file
        
    Returns:
        True if file was modified, False otherwise
    """
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        original_content = content
        
        # Pattern 1: [text](/api/filename.yaml) -> [text]({{ api_url }}/filename.yaml)
        pattern1 = r'\]\(/api/([^)]+\.yaml)\)'
        replacement1 = r']({{ api_url }}/\1)'
        content = re.sub(pattern1, replacement1, content)
        
        # Pattern 2: [text](api/filename.yaml) -> [text]({{ api_url }}/filename.yaml)
        pattern2 = r'\]\(api/([^)]+\.yaml)\)'
        replacement2 = r']({{ api_url }}/\1)'
        content = re.sub(pattern2, replacement2, content)
        
        # Pattern 3: [text](../api/1.0.0/filename.yaml) -> [text]({{ api_url }}/filename.yaml)
        pattern3 = r'\]\(\.\./api/1\.0\.0/([^)]+\.yaml)\)'
        replacement3 = r']({{ api_url }}/\1)'
        content = re.sub(pattern3, replacement3, content)
        
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
    print(f"\nAPI links now use {{{{ api_url }}}}/filename.yaml macro")
    print(f"The api_url macro is defined in doc/version.py and resolves to '../api/1.0.0'")
    
    return 0


if __name__ == '__main__':
    exit(main())
