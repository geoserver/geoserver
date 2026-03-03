#!/usr/bin/env python3
"""
Fix multi-line {% include %} statements that cause macro syntax errors.

This script converts multi-line include statements to single-line format
which is required by mkdocs-macros plugin.
"""

import os
import re
from pathlib import Path

def fix_multiline_includes(content):
    """
    Fix multi-line {% include %} statements by converting them to single line.
    
    Handles patterns like:
    {% 
      include "path"
       start="..."
       end="..."
    %}
    
    Converts to:
    {% include "path" start="..." end="..." %}
    """
    # Pattern to match multi-line include statements
    # This matches {% followed by whitespace/newlines, then include, then parameters, then %}
    pattern = r'{%\s+include\s+"([^"]+)"([^%]*?)%}'
    
    def replace_include(match):
        path = match.group(1)
        params = match.group(2)
        
        # Clean up the parameters - remove extra whitespace and newlines
        params = re.sub(r'\s+', ' ', params).strip()
        
        # Build single-line include
        if params:
            return f'{{% include "{path}" {params} %}}'
        else:
            return f'{{% include "{path}" %}}'
    
    # Replace all multi-line includes
    fixed_content = re.sub(pattern, replace_include, content, flags=re.DOTALL)
    
    return fixed_content

def process_file(filepath):
    """Process a single file, fixing multi-line includes."""
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Check if file has multi-line includes
        if re.search(r'{%\s+include', content):
            fixed_content = fix_multiline_includes(content)
            
            # Only write if content changed
            if fixed_content != content:
                with open(filepath, 'w', encoding='utf-8') as f:
                    f.write(fixed_content)
                return True
        
        return False
    except Exception as e:
        print(f"Error processing {filepath}: {e}")
        return False

def find_and_fix_files(base_dir):
    """Find all markdown files and fix multi-line include statements."""
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
    
    print(f"Fixing multi-line include statements in: {workspace_root}")
    print("=" * 60)
    
    fixed_files = find_and_fix_files(workspace_root)
    
    print("\n" + "=" * 60)
    print(f"Fixed {len(fixed_files)} files")
    
    if fixed_files:
        print("\nFiles fixed:")
        for f in fixed_files:
            print(f"  - {f}")
