#!/usr/bin/env python3
"""
Restore commented-out image links and fix their paths.
"""

import sys
from pathlib import Path
import re

fixes = [
    # new_workspace.png - should be in img subfolder
    {
        'file': 'doc/en/user/docs/gettingstarted/shapefile-quickstart/index.md',
        'pattern': r'<!-- MISSING IMAGE: new_workspace\.png -->',
        'replacement': '![](img/new_workspace.png)',
        'desc': 'Restore new_workspace.png image'
    },
    # googleearth.jpg - should be in img subfolder
    {
        'file': 'doc/en/user/docs/services/wms/googleearth/tutorials/superoverlaysgwc.md',
        'pattern': r'<!-- MISSING IMAGE: \.\./googleearth\.jpg -->',
        'replacement': '![](../../img/googleearth.jpg)',
        'desc': 'Restore googleearth.jpg image'
    },
]

def apply_fixes():
    """Apply all fixes."""
    fixed_count = 0
    
    for fix in fixes:
        filepath = Path(fix['file'])
        
        if not filepath.exists():
            print(f"⚠ File not found: {filepath}")
            continue
        
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Use regex to find and replace
        new_content = re.sub(fix['pattern'], fix['replacement'], content)
        
        if new_content == content:
            print(f"⚠ Pattern not found in {filepath.name}")
            continue
        
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)
        
        print(f"✓ {filepath.name}")
        print(f"  {fix['desc']}")
        fixed_count += 1
    
    print(f"\n✓ Fixed {fixed_count} image paths")
    return 0

if __name__ == '__main__':
    sys.exit(apply_fixes())
