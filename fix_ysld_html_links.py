#!/usr/bin/env python3
"""
Fix broken YSLD function reference links with .html extensions.

The links have incorrect 'geoserver/' prefix and .html extensions.
FROM: ../../../geoserver/filter/function_reference.html
TO:   ../../../filter/function_reference.md
"""

import sys
from pathlib import Path

def fix_ysld_html_links():
    """Fix broken YSLD function reference links with .html extensions."""
    filepath = Path('doc/en/user/docs/styling/ysld/reference/functions.md')
    
    if not filepath.exists():
        print(f"Error: {filepath} not found", file=sys.stderr)
        return 1
    
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Count occurrences before fix
    count = content.count('../../../geoserver/filter/function_reference.html')
    
    if count == 0:
        print("No broken .html links found - already fixed")
        return 0
    
    # Fix the links - remove 'geoserver/' prefix and change .html to .md
    new_content = content.replace(
        '../../../geoserver/filter/function_reference.html',
        '../../../filter/function_reference.md'
    )
    
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(new_content)
    
    print(f"✓ Fixed {count} broken function reference links in {filepath}")
    print(f"  Changed: ../../../geoserver/filter/function_reference.html")
    print(f"  To:      ../../../filter/function_reference.md")
    return 0

if __name__ == '__main__':
    sys.exit(fix_ysld_html_links())
