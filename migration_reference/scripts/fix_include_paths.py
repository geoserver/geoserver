#!/usr/bin/env python3
"""
Fix include paths to be relative to docs directory instead of current file.

mkdocs-macros with include_dir: docs expects paths relative to the docs directory,
not relative to the current file.

This script converts:
  {% include "./include/stroke.md" %}
to:
  {% include "styling/ysld/reference/symbolizers/include/stroke.md" %}
"""

import re
from pathlib import Path

def fix_include_paths_in_file(file_path, docs_root):
    """Fix include paths in a single file."""
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original_content = content
    
    # Find all {% include "..." %} statements
    include_pattern = r'{%\s*include\s+"([^"]+)"\s*%}'
    
    def replace_include(match):
        include_path = match.group(1)
        
        # Skip if already absolute (doesn't start with ./)
        if not include_path.startswith('./'):
            return match.group(0)
        
        # Calculate the path relative to docs directory
        file_dir = file_path.parent
        # Resolve the include path relative to the current file
        absolute_include = (file_dir / include_path).resolve()
        
        # Make it relative to docs_root
        try:
            relative_to_docs = absolute_include.relative_to(docs_root)
            # Convert Windows path to forward slashes
            relative_to_docs_str = str(relative_to_docs).replace('\\', '/')
            return f'{{% include "{relative_to_docs_str}" %}}'
        except ValueError:
            # Path is outside docs_root, keep original
            return match.group(0)
    
    content = re.sub(include_pattern, replace_include, content)
    
    if content != original_content:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)
        return True
    return False

def main():
    """Find and fix all files with relative include paths."""
    docs_root = Path('doc/en/user/docs').resolve()
    
    files_to_fix = []
    
    for md_file in docs_root.rglob('*.md'):
        with open(md_file, 'r', encoding='utf-8') as f:
            content = f.read()
            if re.search(r'{%\s*include\s+"\./[^"]+"\s*%}', content):
                files_to_fix.append(md_file)
    
    print(f"Found {len(files_to_fix)} files with relative include paths:")
    for file_path in files_to_fix:
        print(f"  {file_path.relative_to(docs_root.parent)}")
    
    print("\nFixing files...")
    fixed_count = 0
    for file_path in files_to_fix:
        if fix_include_paths_in_file(file_path, docs_root):
            print(f"  ✓ Fixed: {file_path.relative_to(docs_root.parent)}")
            fixed_count += 1
        else:
            print(f"  ✗ No changes: {file_path.relative_to(docs_root.parent)}")
    
    print(f"\nFixed {fixed_count} files")
    
    # Also fix developer and docguide docs
    for manual in ['developer', 'docguide']:
        docs_root = Path(f'doc/en/{manual}/docs').resolve()
        if not docs_root.exists():
            continue
            
        files_to_fix = []
        for md_file in docs_root.rglob('*.md'):
            with open(md_file, 'r', encoding='utf-8') as f:
                content = f.read()
                if re.search(r'{%\s*include\s+"\./[^"]+"\s*%}', content):
                    files_to_fix.append(md_file)
        
        if files_to_fix:
            print(f"\nFound {len(files_to_fix)} files in {manual} manual:")
            for file_path in files_to_fix:
                if fix_include_paths_in_file(file_path, docs_root):
                    print(f"  ✓ Fixed: {file_path.relative_to(docs_root.parent)}")
                    fixed_count += 1

if __name__ == '__main__':
    main()
