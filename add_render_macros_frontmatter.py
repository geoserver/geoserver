#!/usr/bin/env python3
"""
Add render_macros: true frontmatter to files using {{ api_url }} macro.

This script scans Markdown files for {{ api_url }} usage and adds the
necessary frontmatter to enable mkdocs-macros rendering.
"""

import re
from pathlib import Path


def add_frontmatter_if_needed(file_path: Path) -> bool:
    """
    Add render_macros: true frontmatter if file uses {{ api_url }} macro.
    
    Args:
        file_path: Path to the Markdown file
        
    Returns:
        True if file was modified, False otherwise
    """
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Check if file uses {{ api_url }} macro
        if '{{ api_url }}' not in content:
            return False
        
        # Check for duplicate frontmatter (multiple --- markers)
        frontmatter_markers = [m.start() for m in re.finditer(r'^---$', content, re.MULTILINE)]
        
        if len(frontmatter_markers) > 2:
            print(f"ERROR: Duplicate frontmatter detected in {file_path} ({len(frontmatter_markers)} markers)")
            return False
        
        # Check if file already has frontmatter
        if content.startswith('---\n'):
            # File has frontmatter, check if render_macros is already set
            frontmatter_end = content.find('\n---\n', 4)
            if frontmatter_end == -1:
                print(f"Warning: Malformed frontmatter in {file_path}")
                return False
            
            frontmatter = content[4:frontmatter_end]
            
            if 'render_macros:' in frontmatter:
                # Already has render_macros, no change needed
                return False
            
            # Add render_macros to existing frontmatter
            new_frontmatter = f"---\n{frontmatter}\nrender_macros: true\n---\n"
            new_content = new_frontmatter + content[frontmatter_end + 5:]
        else:
            # No frontmatter, add it
            new_content = "---\nrender_macros: true\n---\n\n" + content
        
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(new_content)
        
        return True
        
    except Exception as e:
        print(f"Error processing {file_path}: {e}")
        return False


def main():
    """Main function to add frontmatter to all files using api_url macro."""
    
    # Find all Markdown files in the REST documentation
    docs_dir = Path('doc/en/user/docs')
    rest_dir = docs_dir / 'rest'
    
    if not rest_dir.exists():
        print(f"Error: REST documentation directory not found: {rest_dir}")
        return 1
    
    # Find all Markdown files
    md_files = list(rest_dir.rglob('*.md'))
    
    # Also check community and extensions directories
    for subdir in ['community', 'extensions']:
        subdir_path = docs_dir / subdir
        if subdir_path.exists():
            md_files.extend(subdir_path.rglob('*.md'))
    
    print(f"Found {len(md_files)} Markdown files to check")
    
    modified_count = 0
    
    for md_file in md_files:
        if add_frontmatter_if_needed(md_file):
            print(f"Added frontmatter: {md_file.relative_to(docs_dir)}")
            modified_count += 1
    
    print(f"\nAdded render_macros frontmatter to {modified_count} files")
    
    return 0


if __name__ == '__main__':
    exit(main())
