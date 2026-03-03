#!/usr/bin/env python3
"""
Fix include-markdown syntax to use mkdocs-macros include syntax.

The conversion tool incorrectly used {% include-markdown %} syntax from
mkdocs-include-markdown-plugin instead of {% include %} from mkdocs-macros.

This script converts:
  {% include-markdown "./path/file.md" %}
to:
  {% include "./path/file.md" %}

And also handles the special case with start/end parameters in license.md.
"""

import re
from pathlib import Path

def fix_include_syntax(file_path):
    """Fix include-markdown syntax in a single file."""
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original_content = content
    
    # Pattern 1: Simple include-markdown without parameters
    # {% include-markdown "./path/file.md" %}
    content = re.sub(
        r'{%\s*include-markdown\s+"([^"]+)"\s*%}',
        r'{% include "\1" %}',
        content
    )
    
    # Pattern 2: include-markdown with start/end parameters (license.md case)
    # Convert to regular include (mkdocs-macros doesn't support start/end)
    # We'll just include the whole file
    content = re.sub(
        r'{%\s*include-markdown\s+"([^"]+)"\s+start="[^"]*"\s+end="[^"]*"\s*%}',
        r'{% include "\1" %}',
        content
    )
    
    if content != original_content:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)
        return True
    return False

def main():
    """Find and fix all files with include-markdown syntax."""
    # Find all markdown files with include-markdown
    doc_root = Path('doc')
    files_to_fix = []
    
    for md_file in doc_root.rglob('*.md'):
        with open(md_file, 'r', encoding='utf-8') as f:
            content = f.read()
            if 'include-markdown' in content:
                files_to_fix.append(md_file)
    
    print(f"Found {len(files_to_fix)} files with include-markdown syntax:")
    for file_path in files_to_fix:
        print(f"  {file_path}")
    
    print("\nFixing files...")
    fixed_count = 0
    for file_path in files_to_fix:
        if fix_include_syntax(file_path):
            print(f"  ✓ Fixed: {file_path}")
            fixed_count += 1
        else:
            print(f"  ✗ No changes: {file_path}")
    
    print(f"\nFixed {fixed_count} files")
    
    # Special note about license.md
    license_file = Path('doc/en/user/docs/introduction/license.md')
    if license_file in files_to_fix:
        print("\n⚠️  NOTE: license.md had start/end parameters that were removed.")
        print("   mkdocs-macros doesn't support partial includes.")
        print("   The entire LICENSE.md file will now be included.")
        print("   You may need to manually adjust the LICENSE.md content or")
        print("   edit license.md to include only the desired section.")

if __name__ == '__main__':
    main()
