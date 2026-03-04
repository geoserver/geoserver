#!/usr/bin/env python3
"""
Fix YSLD include file paths based on MkDocs warnings.
"""

import sys
from pathlib import Path

fixes = [
    # include/stroke.md - fix point.md references
    {
        'file': 'doc/en/user/docs/styling/ysld/reference/symbolizers/include/stroke.md',
        'old': '../point.md',
        'new': 'point.md',
        'desc': 'Fix point.md reference in stroke.md'
    },
    # include/symbol.md - fix sld/extensions paths
    {
        'file': 'doc/en/user/docs/styling/ysld/reference/symbolizers/include/symbol.md',
        'old': '../../../../sld/extensions/',
        'new': '../../../sld/extensions/',
        'desc': 'Fix SLD extension paths in symbol.md'
    },
    # include/misc.md - fix sld/extensions and text.md paths
    {
        'file': 'doc/en/user/docs/styling/ysld/reference/symbolizers/include/misc.md',
        'old': '../../../../sld/extensions/',
        'new': '../../../sld/extensions/',
        'desc': 'Fix SLD extension paths in misc.md'
    },
    {
        'file': 'doc/en/user/docs/styling/ysld/reference/symbolizers/include/misc.md',
        'old': '../text.md',
        'new': 'text.md',
        'desc': 'Fix text.md reference in misc.md'
    },
    # include/composite.md - fix sld/extensions and featurestyles paths
    {
        'file': 'doc/en/user/docs/styling/ysld/reference/symbolizers/include/composite.md',
        'old': '../../../../sld/extensions/',
        'new': '../../../sld/extensions/',
        'desc': 'Fix SLD extension paths in composite.md'
    },
    {
        'file': 'doc/en/user/docs/styling/ysld/reference/symbolizers/include/composite.md',
        'old': '../../featurestyles.md',
        'new': '../featurestyles.md',
        'desc': 'Fix featurestyles.md reference in composite.md'
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
