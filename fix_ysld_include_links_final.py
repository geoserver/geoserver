#!/usr/bin/env python3
"""
Fix YSLD include file links to work with MkDocs include directive.
The include directive processes files from docs root, so links need to be 
relative to where they're included (symbolizers directory), not the include directory.
"""

import sys
from pathlib import Path

fixes = [
    # include/stroke.md - point.md is in same directory as where this is included
    {
        'file': 'doc/en/user/docs/styling/ysld/reference/symbolizers/include/stroke.md',
        'old': '../point.md',
        'new': 'point.md',
        'desc': 'Fix point.md reference (same directory as inclusion point)'
    },
    # include/symbol.md - sld/extensions is 3 levels up from symbolizers/
    {
        'file': 'doc/en/user/docs/styling/ysld/reference/symbolizers/include/symbol.md',
        'old': '../../../../sld/extensions/',
        'new': '../../../sld/extensions/',
        'desc': 'Fix SLD extension paths (3 levels up from symbolizers/)'
    },
    # include/misc.md - sld/extensions is 3 levels up, text.md is in same directory
    {
        'file': 'doc/en/user/docs/styling/ysld/reference/symbolizers/include/misc.md',
        'old': '../../../../sld/extensions/',
        'new': '../../../sld/extensions/',
        'desc': 'Fix SLD extension paths (3 levels up from symbolizers/)'
    },
    {
        'file': 'doc/en/user/docs/styling/ysld/reference/symbolizers/include/misc.md',
        'old': '../text.md',
        'new': 'text.md',
        'desc': 'Fix text.md reference (same directory as inclusion point)'
    },
    # include/composite.md - sld/extensions is 3 levels up, featurestyles is 1 level up
    {
        'file': 'doc/en/user/docs/styling/ysld/reference/symbolizers/include/composite.md',
        'old': '../../../../sld/extensions/',
        'new': '../../../sld/extensions/',
        'desc': 'Fix SLD extension paths (3 levels up from symbolizers/)'
    },
    {
        'file': 'doc/en/user/docs/styling/ysld/reference/symbolizers/include/composite.md',
        'old': '../../featurestyles.md',
        'new': '../featurestyles.md',
        'desc': 'Fix featurestyles.md reference (1 level up from symbolizers/)'
    },
]

def apply_fixes():
    """Apply all fixes."""
    fixed_count = 0
    fixed_files = set()
    
    for fix in fixes:
        filepath = Path(fix['file'])
        
        if not filepath.exists():
            print(f"⚠ File not found: {filepath}")
            continue
        
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
        
        if fix['old'] not in content:
            print(f"⚠ Pattern not found in {filepath.name}: {fix['old']}")
            continue
        
        new_content = content.replace(fix['old'], fix['new'])
        
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)
        
        if filepath not in fixed_files:
            print(f"✓ {filepath.name}")
            fixed_files.add(filepath)
        print(f"  {fix['desc']}")
        fixed_count += 1
    
    print(f"\n✓ Applied {fixed_count} fixes to {len(fixed_files)} files")
    return 0

if __name__ == '__main__':
    sys.exit(apply_fixes())
