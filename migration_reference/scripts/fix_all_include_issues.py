#!/usr/bin/env python3
"""
Fix all include-related issues in markdown files.

Issues to fix:
1. Nested {% {% include %} %} statements (should be single {% include %})
2. Include statements in code blocks that should be escaped
3. Include paths that go outside docs directory (../../../../LICENSE.md)
"""

import os
import re
from pathlib import Path

def fix_include_issues(file_path):
    """Fix all include-related issues in a markdown file."""
    
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original_content = content
    changes = []
    
    # Issue 1: Fix nested {% {% include %} %} statements
    # Pattern: {% \n  {% include "path" %}\n %}
    nested_pattern = r'{%\s*\n\s*{%\s*include\s+"([^"]+)"\s*%}\s*\n\s*%}'
    if re.search(nested_pattern, content):
        content = re.sub(nested_pattern, r'{% include "\1" %}', content)
        changes.append("Fixed nested {% {% include %} %} statements")
    
    # Issue 2: Escape include statements in code blocks
    # These should be shown as examples, not processed
    # Pattern: code block containing {% include %}
    def escape_code_block_includes(match):
        code_block = match.group(0)
        # Check if this code block contains {% include %}
        if '{% include' in code_block and not '{%raw%}' in code_block:
            # Wrap the include in raw tags to prevent processing
            code_block = code_block.replace('{% include', '{%raw%}{% include')
            code_block = code_block.replace('%}', '%}{%endraw%}', 1)  # Only first occurrence
        return code_block
    
    # Find code blocks with ~~~ or ```
    code_block_pattern = r'(~~~|```)[^\n]*\n.*?{%\s*include.*?\n\1'
    if re.search(code_block_pattern, content, re.DOTALL):
        content = re.sub(code_block_pattern, escape_code_block_includes, content, flags=re.DOTALL)
        changes.append("Escaped include statements in code blocks")
    
    # Issue 3: Fix include paths that go outside docs directory
    # Pattern: {% include "../../../../LICENSE.md" %}
    outside_docs_pattern = r'{%\s*include\s+"(\.\./\.\./\.\./\.\./[^"]+)"\s*%}'
    if re.search(outside_docs_pattern, content):
        def fix_outside_path(match):
            path = match.group(1)
            # Comment out the include and add a note
            return f'<!-- Include path goes outside docs directory: {path} -->\n<!-- TODO: Copy file to docs directory or use alternative approach -->'
        
        content = re.sub(outside_docs_pattern, fix_outside_path, content)
        changes.append("Commented out include paths outside docs directory")
    
    if content != original_content:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)
        return changes
    
    return None

def main():
    """Main function to fix all include issues."""
    
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
            changes = fix_include_issues(str(md_file))
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
