#!/usr/bin/env python3
"""
Fix include statements with start/end parameters.

Sphinx supports include directives with start/end parameters:
  .. literalinclude:: file.xml
     :start-after: <!-- START -->
     :end-before: <!-- END -->

These were incorrectly converted to:
  {% include "file.xml" start="<!-- START -->" end="<!-- END -->" %}

Jinja2 include does NOT support start/end parameters.
We need to either:
1. Comment them out (they reference external files anyway)
2. Or wrap them in {%raw%} tags if they're in code blocks
"""

import os
import re
from pathlib import Path

def fix_include_with_params(file_path):
    """Fix include statements that have start/end parameters."""
    
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original_content = content
    changes = []
    
    # Pattern: {% include "path" start="..." end="..." %}
    # This is invalid Jinja2 syntax
    pattern = r'{%\s*include\s+"([^"]+)"\s+start="([^"]+)"\s+end="([^"]+)"\s*%}'
    
    def replace_invalid_include(match):
        path = match.group(1)
        start_marker = match.group(2)
        end_marker = match.group(3)
        
        # Check if path goes outside docs directory
        if path.startswith('../../../'):
            # Comment it out
            return f'<!-- Include with start/end not supported: {path} -->\n<!-- Extract: from "{start_marker}" to "{end_marker}" -->\n<!-- TODO: Copy relevant section to docs directory -->'
        else:
            # Wrap in raw tags (for code blocks)
            return f'{{%raw%}}{{% include "{path}" start="{start_marker}" end="{end_marker}" %}}{{%endraw%}}'
    
    if re.search(pattern, content):
        content = re.sub(pattern, replace_invalid_include, content)
        changes.append("Fixed include statements with start/end parameters")
    
    if content != original_content:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)
        return changes
    
    return None

def main():
    """Main function to fix all includes with parameters."""
    
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
            changes = fix_include_with_params(str(md_file))
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
