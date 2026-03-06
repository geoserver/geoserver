#!/usr/bin/env python3
"""
Fix all remaining .html and .rst link references in documentation.

This script fixes:
1. .html links that should be .md
2. .rst links that should be .md
3. Self-referencing .html links (same file anchors)
"""

import sys
from pathlib import Path
import re

def fix_file(filepath):
    """Fix .html and .rst links in a single file."""
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original_content = content
    changes = []
    
    # Fix .html links to .md (but not external URLs)
    # Pattern: [text](path.html) or [text](path.html#anchor)
    def replace_html(match):
        full_link = match.group(1)
        if full_link.startswith('http'):
            return match.group(0)  # Don't change external links
        
        # Replace .html with .md
        new_link = full_link.replace('.html', '.md')
        return f']({new_link})'
    
    new_content = re.sub(r'\]\(([^)]+\.html[^)]*)\)', replace_html, content)
    if new_content != content:
        changes.append("Fixed .html → .md links")
        content = new_content
    
    # Fix .rst links to .md
    def replace_rst(match):
        full_link = match.group(1)
        new_link = full_link.replace('.rst', '.md')
        return f']({new_link})'
    
    new_content = re.sub(r'\]\(([^)]+\.rst[^)]*)\)', replace_rst, content)
    if new_content != content:
        changes.append("Fixed .rst → .md links")
        content = new_content
    
    if content != original_content:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        return changes
    
    return []

def main():
    """Fix all .html and .rst links in documentation."""
    docs_dir = Path('doc/en/user/docs')
    
    if not docs_dir.exists():
        print(f"Error: {docs_dir} not found", file=sys.stderr)
        return 1
    
    total_files = 0
    total_changes = 0
    
    for filepath in docs_dir.rglob('*.md'):
        changes = fix_file(filepath)
        if changes:
            total_files += 1
            total_changes += len(changes)
            print(f"✓ {filepath.relative_to('doc/en/user/docs')}: {', '.join(changes)}")
    
    print(f"\n✓ Fixed {total_changes} types of issues in {total_files} files")
    return 0

if __name__ == '__main__':
    sys.exit(main())
