#!/usr/bin/env python3
"""
Fix include-markdown statements wrapped in {%raw%} tags.

These should be processed by mkdocs-macros, not rendered as literal text.
"""

import os
import re
from pathlib import Path


def fix_raw_includes(content):
    """
    Remove {%raw%} and {%endraw%} tags around {% include-markdown %} statements.
    """
    # Pattern to match {%raw%}{% include-markdown ... %}{%endraw%}
    pattern = r'\{%raw%\}(\{% include-markdown .*? %\})\{%endraw%\}'
    
    # Replace with just the include-markdown statement
    new_content = re.sub(pattern, r'\1', content)
    
    return new_content, new_content != content


def process_file(filepath):
    """Process a single markdown file."""
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
        
        new_content, modified = fix_raw_includes(content)
        
        if modified:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(new_content)
            print(f"  ✓ Fixed: {filepath}")
            return True
        
        return False
    
    except Exception as e:
        print(f"  ✗ Error processing {filepath}: {e}")
        return False


def main():
    """Main function to process all markdown files in developer docs."""
    
    print("Fixing include-markdown statements wrapped in raw tags...")
    print()
    
    modified_count = 0
    total_count = 0
    
    # Process all markdown files in developer docs
    docs_dir = Path('doc/en/developer/docs')
    
    for md_file in docs_dir.rglob('*.md'):
        total_count += 1
        if process_file(md_file):
            modified_count += 1
    
    print()
    print(f"Summary:")
    print(f"  Total files scanned: {total_count}")
    print(f"  Files modified: {modified_count}")
    print(f"  Files skipped: {total_count - modified_count}")


if __name__ == '__main__':
    main()
