#!/usr/bin/env python3
"""
Fix all remaining warnings based on MkDocs suggestions.
"""

import sys
from pathlib import Path
import re

fixes = [
    # YSLD symbolizers - fix SLD extension links (remove one ../)
    {
        'file': 'doc/en/user/docs/styling/ysld/reference/symbolizers/index.md',
        'old': '../../../../sld/extensions/',
        'new': '../../../sld/extensions/',
        'desc': 'Fix YSLD symbolizers SLD extension paths'
    },
    {
        'file': 'doc/en/user/docs/styling/ysld/reference/symbolizers/index.md',
        'old': '../../featurestyles.md',
        'new': '../featurestyles.md',
        'desc': 'Fix YSLD symbolizers featurestyles path'
    },
    # YSLD symbolizers/line.md
    {
        'file': 'doc/en/user/docs/styling/ysld/reference/symbolizers/line.md',
        'old': '../../../../sld/extensions/',
        'new': '../../../sld/extensions/',
        'desc': 'Fix YSLD line SLD extension paths'
    },
    {
        'file': 'doc/en/user/docs/styling/ysld/reference/symbolizers/line.md',
        'old': '../../featurestyles.md',
        'new': '../featurestyles.md',
        'desc': 'Fix YSLD line featurestyles path'
    },
    {
        'file': 'doc/en/user/docs/styling/ysld/reference/symbolizers/line.md',
        'old': '../point.md',
        'new': 'point.md',
        'desc': 'Fix YSLD line point.md reference'
    },
    {
        'file': 'doc/en/user/docs/styling/ysld/reference/symbolizers/line.md',
        'old': '../text.md',
        'new': 'text.md',
        'desc': 'Fix YSLD line text.md reference'
    },
    # YSLD symbolizers/point.md
    {
        'file': 'doc/en/user/docs/styling/ysld/reference/symbolizers/point.md',
        'old': '../../../../sld/extensions/',
        'new': '../../../sld/extensions/',
        'desc': 'Fix YSLD point SLD extension paths'
    },
    {
        'file': 'doc/en/user/docs/styling/ysld/reference/symbolizers/point.md',
        'old': '../../featurestyles.md',
        'new': '../featurestyles.md',
        'desc': 'Fix YSLD point featurestyles path'
    },
    {
        'file': 'doc/en/user/docs/styling/ysld/reference/symbolizers/point.md',
        'old': '../point.md',
        'new': 'point.md',
        'desc': 'Fix YSLD point point.md reference'
    },
    {
        'file': 'doc/en/user/docs/styling/ysld/reference/symbolizers/point.md',
        'old': '../text.md',
        'new': 'text.md',
        'desc': 'Fix YSLD point text.md reference'
    },
    # YSLD symbolizers/polygon.md
    {
        'file': 'doc/en/user/docs/styling/ysld/reference/symbolizers/polygon.md',
        'old': '../../../../sld/extensions/',
        'new': '../../../sld/extensions/',
        'desc': 'Fix YSLD polygon SLD extension paths'
    },
    {
        'file': 'doc/en/user/docs/styling/ysld/reference/symbolizers/polygon.md',
        'old': '../../featurestyles.md',
        'new': '../featurestyles.md',
        'desc': 'Fix YSLD polygon featurestyles path'
    },
    {
        'file': 'doc/en/user/docs/styling/ysld/reference/symbolizers/polygon.md',
        'old': '../point.md',
        'new': 'point.md',
        'desc': 'Fix YSLD polygon point.md reference'
    },
    {
        'file': 'doc/en/user/docs/styling/ysld/reference/symbolizers/polygon.md',
        'old': '../text.md',
        'new': 'text.md',
        'desc': 'Fix YSLD polygon text.md reference'
    },
    # YSLD symbolizers/raster.md
    {
        'file': 'doc/en/user/docs/styling/ysld/reference/symbolizers/raster.md',
        'old': '../../../../sld/extensions/',
        'new': '../../../sld/extensions/',
        'desc': 'Fix YSLD raster SLD extension paths'
    },
    {
        'file': 'doc/en/user/docs/styling/ysld/reference/symbolizers/raster.md',
        'old': '../../featurestyles.md',
        'new': '../featurestyles.md',
        'desc': 'Fix YSLD raster featurestyles path'
    },
    # YSLD symbolizers/text.md
    {
        'file': 'doc/en/user/docs/styling/ysld/reference/symbolizers/text.md',
        'old': '../../../../sld/extensions/',
        'new': '../../../sld/extensions/',
        'desc': 'Fix YSLD text SLD extension paths'
    },
    {
        'file': 'doc/en/user/docs/styling/ysld/reference/symbolizers/text.md',
        'old': '../../featurestyles.md',
        'new': '../featurestyles.md',
        'desc': 'Fix YSLD text featurestyles path'
    },
    # CQL tutorial
    {
        'file': 'doc/en/user/docs/tutorials/cql/cql_tutorial.md',
        'old': '../../../services/wms/vendor.md',
        'new': '../../services/wms/vendor.md',
        'desc': 'Fix CQL tutorial vendor.md path'
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
