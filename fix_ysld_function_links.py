#!/usr/bin/env python3
"""
Fix broken YSLD function reference links.

The conversion tool incorrectly added 'geoserver/' prefix to filter function reference links.
The correct path from styling/ysld/reference/functions.md to filter/function_reference.md
is ../../../filter/function_reference.md (up 3 levels, then into filter/).

FROM: ../../../geoserver/filter/function_reference.md
TO:   ../../../filter/function_reference.md
"""

import sys
from pathlib import Path

def fix_ysld_function_links():
    """Fix broken YSLD function reference links."""
    filepath = Path('doc/en/user/docs/styling/ysld/reference/functions.md')
    
    if not filepath.exists():
        print(f"Error: {filepath} not found", file=sys.stderr)
        return 1
    
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Count occurrences before fix
    count = content.count('../../../geoserver/filter/function_reference.md')
    
    if count == 0:
        print("No broken links found - already fixed or file structure changed")
        return 0
    
    # Fix the links - remove the incorrect 'geoserver/' prefix
    new_content = content.replace(
        '../../../geoserver/filter/function_reference.md',
        '../../../filter/function_reference.md'
    )
    
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(new_content)
    
    print(f"✓ Fixed {count} broken function reference links in {filepath}")
    print(f"  Changed: ../../../geoserver/filter/function_reference.md")
    print(f"  To:      ../../../filter/function_reference.md")
    return 0

if __name__ == '__main__':
    sys.exit(fix_ysld_function_links())
