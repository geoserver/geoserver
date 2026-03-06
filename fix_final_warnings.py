#!/usr/bin/env python3
"""
Fix final remaining warnings in 2.28.x branch.
"""

import sys
from pathlib import Path
import re

fixes = [
    # Fix API spec paths - MkDocs resolves relative to docs dir, not file location
    # From community/opensearch-eo/ (4 levels deep) to api/1.0.0/
    {
        'file': 'doc/en/user/docs/community/opensearch-eo/upgrading.md',
        'old': '../../../../api/1.0.0/resource.yaml',
        'new': '../../api/1.0.0/resource.yaml',
        'desc': 'Fix opensearch-eo resource API path'
    },
    {
        'file': 'doc/en/user/docs/community/proxy-base-ext/usage.md',
        'old': '../../../../api/1.0.0/proxy-base-ext.yaml',
        'new': '../../api/1.0.0/proxy-base-ext.yaml',
        'desc': 'Fix proxy-base-ext API path'
    },
    # From extensions/ (2 levels deep) to api/1.0.0/
    {
        'file': 'doc/en/user/docs/extensions/metadata/index.md',
        'old': '../../../api/1.0.0/metadata.yaml',
        'new': '../api/1.0.0/metadata.yaml',
        'desc': 'Fix metadata API path'
    },
    {
        'file': 'doc/en/user/docs/extensions/params-extractor/usage.md',
        'old': '../../../api/1.0.0/params-extractor.yaml',
        'new': '../api/1.0.0/params-extractor.yaml',
        'desc': 'Fix params-extractor API path'
    },
    {
        'file': 'doc/en/user/docs/extensions/rat/using.md',
        'old': '../../../api/1.0.0/rat.yaml',
        'new': '../api/1.0.0/rat.yaml',
        'desc': 'Fix rat API path'
    },
    {
        'file': 'doc/en/user/docs/extensions/wps-download/index.md',
        'old': '../../../api/1.0.0/wpsdownload.yaml',
        'new': '../api/1.0.0/wpsdownload.yaml',
        'desc': 'Fix wpsdownload API path'
    },
    # From styling/sld/ (2 levels deep) to api/1.0.0/
    {
        'file': 'doc/en/user/docs/styling/sld/working.md',
        'old': '../../../api/1.0.0/layergroups.yaml',
        'new': '../api/1.0.0/layergroups.yaml',
        'desc': 'Fix layergroups API path'
    },
    # Fix jdbcstore path (one too many ../)
    {
        'file': 'doc/en/user/docs/community/jdbcstore/configuration.md',
        'old': '.../../data/app-schema/index.md',
        'new': '../../data/app-schema/index.md',
        'desc': 'Fix jdbcstore app-schema path'
    },
    # Fix datadirectory rest path (extra .../)
    {
        'file': 'doc/en/user/docs/datadirectory/structure.md',
        'old': '.../rest/index.md',
        'new': '../rest/index.md',
        'desc': 'Fix datadirectory rest path'
    },
    # Fix googleearth image path (one too many ../)
    {
        'file': 'doc/en/user/docs/services/wms/googleearth/tutorials/superoverlaysgwc.md',
        'old': '../../img/googleearth.jpg',
        'new': '../img/googleearth.jpg',
        'desc': 'Fix googleearth image path'
    },
]

# Workshop link fixes - these reference non-existent index.md files
workshop_fixes = [
    # CSS workshop
    {
        'file': 'doc/en/user/docs/styling/workshop/css/done.md',
        'pattern': r'\.\./line/index\.md',
        'replacement': 'linestring.md',
    },
    {
        'file': 'doc/en/user/docs/styling/workshop/css/done.md',
        'pattern': r'\.\./polygon/index\.md',
        'replacement': 'polygon.md',
    },
    {
        'file': 'doc/en/user/docs/styling/workshop/css/done.md',
        'pattern': r'\.\./point/index\.md',
        'replacement': 'point.md',
    },
    {
        'file': 'doc/en/user/docs/styling/workshop/css/done.md',
        'pattern': r'\.\./raster/index\.md',
        'replacement': 'raster.md',
    },
    {
        'file': 'doc/en/user/docs/styling/workshop/css/linestring.md',
        'pattern': r'\.\./line/index\.md',
        'replacement': 'linestring.md',
    },
    {
        'file': 'doc/en/user/docs/styling/workshop/css/point.md',
        'pattern': r'\.\./point/index\.md',
        'replacement': 'point.md',
    },
    {
        'file': 'doc/en/user/docs/styling/workshop/css/polygon.md',
        'pattern': r'\.\./polygon/index\.md',
        'replacement': 'polygon.md',
    },
    {
        'file': 'doc/en/user/docs/styling/workshop/css/raster.md',
        'pattern': r'\.\./raster/index\.md',
        'replacement': 'raster.md',
    },
    # MBStyle workshop
    {
        'file': 'doc/en/user/docs/styling/workshop/mbstyle/done.md',
        'pattern': r'\.\./line/index\.md',
        'replacement': 'linestring.md',
    },
    {
        'file': 'doc/en/user/docs/styling/workshop/mbstyle/done.md',
        'pattern': r'\.\./polygon/index\.md',
        'replacement': 'polygon.md',
    },
    {
        'file': 'doc/en/user/docs/styling/workshop/mbstyle/done.md',
        'pattern': r'\.\./point/index\.md',
        'replacement': 'point.md',
    },
]

def apply_fixes():
    """Apply all fixes."""
    fixed_count = 0
    
    # Apply simple string replacements
    for fix in fixes:
        filepath = Path(fix['file'])
        
        if not filepath.exists():
            print(f"⚠ File not found: {filepath}")
            continue
        
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
        
        if fix['old'] not in content:
            print(f"⚠ Pattern not found in {filepath.name}: {fix['old'][:60]}...")
            continue
        
        new_content = content.replace(fix['old'], fix['new'])
        
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)
        
        print(f"✓ {filepath.name}")
        print(f"  {fix['desc']}")
        fixed_count += 1
    
    # Apply regex replacements for workshop links
    for fix in workshop_fixes:
        filepath = Path(fix['file'])
        
        if not filepath.exists():
            continue
        
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
        
        new_content = re.sub(fix['pattern'], fix['replacement'], content)
        
        if new_content != content:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(new_content)
            
            if filepath not in [Path(f['file']) for f in fixes if 'file' in f]:
                print(f"✓ {filepath.name} - fixed workshop links")
                fixed_count += 1
    
    print(f"\n✓ Applied {fixed_count} fixes")
    return 0

if __name__ == '__main__':
    sys.exit(apply_fixes())
