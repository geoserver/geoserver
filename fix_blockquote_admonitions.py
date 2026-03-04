#!/usr/bin/env python3
"""
Fix blockquote admonitions with :::: note syntax.

Convert:
> :::: note
> ::: title
> Note
> :::
> content
> ::::

To:
!!! note
    content
"""

import os
import re
from pathlib import Path


def fix_blockquote_admonitions(content):
    """
    Fix blockquote admonitions with :::: note syntax.
    """
    # Pattern to match blockquote admonitions
    # > :::: note
    # > ::: title
    # > Note
    # > :::
    # >
    # > content lines
    # > ::::
    
    pattern = r'> :::: (note|warning|tip|important|caution)\n> ::: title\n> .*?\n> :::\n>\n((?:> .*?\n)*?)> ::::'
    
    def replace_admonition(match):
        admon_type = match.group(1)
        content_lines = match.group(2)
        
        # Remove the '> ' prefix from content lines
        content = '\n'.join(line[2:] if line.startswith('> ') else line 
                           for line in content_lines.strip().split('\n'))
        
        # Indent content with 4 spaces
        indented_content = '\n'.join('    ' + line if line else '' 
                                     for line in content.split('\n'))
        
        return f'!!! {admon_type}\n{indented_content}'
    
    new_content = re.sub(pattern, replace_admonition, content, flags=re.MULTILINE)
    
    return new_content, new_content != content


def process_file(filepath):
    """Process a single markdown file."""
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
        
        new_content, modified = fix_blockquote_admonitions(content)
        
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
    """Main function to process all markdown files."""
    
    print("Fixing blockquote admonitions with :::: syntax...")
    print()
    
    modified_count = 0
    total_count = 0
    
    # Process all markdown files in docs directories
    for docs_dir in ['doc/en/user/docs', 'doc/en/developer/docs', 'doc/en/docguide/docs']:
        docs_path = Path(docs_dir)
        if docs_path.exists():
            for md_file in docs_path.rglob('*.md'):
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
