#!/usr/bin/env python3
"""
Fix macro rendering issues in Markdown files.

This script adds 'render_macros: true' frontmatter to files that contain
{{ version }} or {{ release }} macros but don't have the frontmatter.
"""

import os
import re
from pathlib import Path

def has_macros(content):
    """Check if content contains {{ version }} or {{ release }} macros."""
    return '{{ version }}' in content or '{{ release }}' in content

def has_render_macros_frontmatter(content):
    """Check if content already has render_macros: true in frontmatter."""
    # Check for YAML frontmatter
    if content.startswith('---\n'):
        frontmatter_end = content.find('\n---\n', 4)
        if frontmatter_end != -1:
            frontmatter = content[4:frontmatter_end]
            return 'render_macros:' in frontmatter or 'render_macros :' in frontmatter
    return False

def add_render_macros_frontmatter(content):
    """Add render_macros: true to frontmatter, or create frontmatter if missing."""
    if content.startswith('---\n'):
        # Has frontmatter, add render_macros to it
        frontmatter_end = content.find('\n---\n', 4)
        if frontmatter_end != -1:
            frontmatter = content[4:frontmatter_end]
            body = content[frontmatter_end + 5:]
            
            # Add render_macros: true to frontmatter
            new_frontmatter = frontmatter.rstrip() + '\nrender_macros: true\n'
            return f'---\n{new_frontmatter}---\n{body}'
    
    # No frontmatter, create it
    return f'---\nrender_macros: true\n---\n\n{content}'

def process_file(filepath):
    """Process a single file, adding render_macros if needed."""
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Check if file needs fixing
        if has_macros(content) and not has_render_macros_frontmatter(content):
            new_content = add_render_macros_frontmatter(content)
            
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(new_content)
            
            return True
        
        return False
    except Exception as e:
        print(f"Error processing {filepath}: {e}")
        return False

def find_and_fix_files(base_dir):
    """Find all markdown files and fix macro rendering issues."""
    base_path = Path(base_dir)
    fixed_files = []
    
    # Search in user, developer, and docguide docs
    for docs_dir in ['doc/en/user/docs', 'doc/en/developer/docs', 'doc/en/docguide/docs']:
        docs_path = base_path / docs_dir
        if not docs_path.exists():
            print(f"Directory not found: {docs_path}")
            continue
        
        print(f"\nSearching in {docs_dir}...")
        
        for md_file in docs_path.rglob('*.md'):
            if process_file(md_file):
                rel_path = md_file.relative_to(base_path)
                fixed_files.append(str(rel_path))
                print(f"  Fixed: {rel_path}")
    
    return fixed_files

if __name__ == '__main__':
    import sys
    
    # Get workspace root (current directory or provided as argument)
    workspace_root = sys.argv[1] if len(sys.argv) > 1 else '.'
    
    print(f"Fixing macro rendering in: {workspace_root}")
    print("=" * 60)
    
    fixed_files = find_and_fix_files(workspace_root)
    
    print("\n" + "=" * 60)
    print(f"Fixed {len(fixed_files)} files")
    
    if fixed_files:
        print("\nFiles fixed:")
        for f in fixed_files:
            print(f"  - {f}")
