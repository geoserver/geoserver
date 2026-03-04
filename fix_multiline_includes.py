#!/usr/bin/env python3
"""
Fix multi-line include statements that cause Jinja2 syntax errors.

Patterns to fix:
{% 
  include "path"
%}

Should become:
{% include "path" %}
"""

import os
import re

def fix_multiline_includes(content):
    """Fix multi-line include statements."""
    fixes = 0
    
    # Pattern: {% \n  include "path" \n %}
    # Match across multiple lines with optional whitespace
    pattern = r'\{%\s*\n\s*include(?:-markdown)?\s+"([^"]+)"\s*\n\s*%\}'
    
    def replace_func(match):
        nonlocal fixes
        fixes += 1
        path = match.group(1)
        # Check if it's include-markdown or just include
        if 'include-markdown' in match.group(0):
            return f'{{%include-markdown "{path}"%}}'
        else:
            return f'{{%include "{path}"%}}'
    
    content = re.sub(pattern, replace_func, content, flags=re.MULTILINE)
    
    return content, fixes

def process_file(filepath):
    """Process a single markdown file."""
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
        
        fixed_content, fixes = fix_multiline_includes(content)
        
        if fixes > 0:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(fixed_content)
            return fixes
        
        return 0
    except Exception as e:
        print(f"Error processing {filepath}: {e}")
        return 0

def main():
    """Process all markdown files in documentation directories."""
    base_dirs = [
        'doc/en/user/docs',
        'doc/en/developer/docs',
        'doc/en/docguide/docs'
    ]
    
    total_fixes = 0
    fixed_files = []
    
    for base_dir in base_dirs:
        if not os.path.exists(base_dir):
            continue
        
        print(f"\nSearching in {base_dir}...")
        
        for root, dirs, files in os.walk(base_dir):
            for file in files:
                if file.endswith('.md'):
                    filepath = os.path.join(root, file)
                    fixes = process_file(filepath)
                    if fixes > 0:
                        total_fixes += fixes
                        fixed_files.append((filepath, fixes))
                        print(f"  Fixed: {filepath} ({fixes} includes)")
    
    print(f"\n{'='*60}")
    print(f"Total: Fixed {total_fixes} multi-line includes in {len(fixed_files)} files")
    
    if fixed_files:
        print("\nFixed files:")
        for filepath, fixes in fixed_files:
            print(f"  - {filepath} ({fixes} includes)")

if __name__ == '__main__':
    main()
