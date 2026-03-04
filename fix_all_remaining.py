#!/usr/bin/env python3
"""
Fix ALL remaining warnings in 2.28.x branch.
"""

import sys
from pathlib import Path
import re

# The API files are in doc/en/api/1.0.0/ which is OUTSIDE the docs directory
# MkDocs docs_dir is doc/en/user/docs, so we need to go up to doc/en/ then into api/
# From doc/en/user/docs/community/opensearch-eo/ we need ../../../../api/1.0.0/
# But MkDocs resolves relative to docs root, so we need ../../../api/1.0.0/

fixes = [
    # API spec paths - need to go from docs dir to api dir
    # docs is at doc/en/user/docs, api is at doc/en/api
    # So from docs we need ../../api/1.0.0/
    {
        'file': 'doc/en/user/docs/community/opensearch-eo/upgrading.md',
        'old': '../../api/1.0.0/resource.yaml',
        'new': '../../../../api/1.0.0/resource.yaml',
        'desc': 'Fix opensearch-eo resource API path'
    },
    {
        'file': 'doc/en/user/docs/community/proxy-base-ext/usage.md',
        'old': '../../api/1.0.0/proxy-base-ext.yaml',
        'new': '../../../../api/1.0.0/proxy-base-ext.yaml',
        'desc': 'Fix proxy-base-ext API path'
    },
    {
        'file': 'doc/en/user/docs/extensions/metadata/index.md',
        'old': '../api/1.0.0/metadata.yaml',
        'new': '../../../api/1.0.0/metadata.yaml',
        'desc': 'Fix metadata API path'
    },
    {
        'file': 'doc/en/user/docs/extensions/params-extractor/usage.md',
        'old': '../api/1.0.0/params-extractor.yaml',
        'new': '../../../api/1.0.0/params-extractor.yaml',
        'desc': 'Fix params-extractor API path'
    },
    {
        'file': 'doc/en/user/docs/extensions/rat/using.md',
        'old': '../api/1.0.0/rat.yaml',
        'new': '../../../api/1.0.0/rat.yaml',
        'desc': 'Fix rat API path'
    },
    {
        'file': 'doc/en/user/docs/extensions/wps-download/index.md',
        'old': '../api/1.0.0/wpsdownload.yaml',
        'new': '../../../api/1.0.0/wpsdownload.yaml',
        'desc': 'Fix wpsdownload API path'
    },
    {
        'file': 'doc/en/user/docs/styling/sld/working.md',
        'old': '../api/1.0.0/layergroups.yaml',
        'new': '../../../api/1.0.0/layergroups.yaml',
        'desc': 'Fix layergroups API path'
    },
    # YSLD symbolizers SLD extension links - too many ../
    {
        'file': 'doc/en/user/docs/styling/ysld/reference/symbolizers/index.md',
        'old': '../../../../sld/extensions/geometry-transformations.md',
        'new': '../../../sld/extensions/geometry-transformations.md',
        'desc': 'Fix YSLD geometry-transformations link'
    },
    {
        'file': 'doc/en/user/docs/styling/ysld/reference/symbolizers/index.md',
        'old': '../../../../sld/extensions/uom.md',
        'new': '../../../sld/extensions/uom.md',
        'desc': 'Fix YSLD uom link'
    },
    {
        'file': 'doc/en/user/docs/styling/ysld/reference/symbolizers/index.md',
        'old': '../../../../sld/extensions/composite-blend/index.md',
        'new': '../../../sld/extensions/composite-blend/index.md',
        'desc': 'Fix YSLD composite-blend link'
    },
    {
        'file': 'doc/en/user/docs/styling/ysld/reference/symbolizers/index.md',
        'old': '../../featurestyles.md',
        'new': '../featurestyles.md',
        'desc': 'Fix YSLD featurestyles link'
    },
]

# Workshop link fixes - ALL workshop files
workshop_files = [
    # MBStyle workshop
    'doc/en/user/docs/styling/workshop/mbstyle/done.md',
    'doc/en/user/docs/styling/workshop/mbstyle/linestring.md',
    'doc/en/user/docs/styling/workshop/mbstyle/point.md',
    'doc/en/user/docs/styling/workshop/mbstyle/polygon.md',
    'doc/en/user/docs/styling/workshop/mbstyle/raster.md',
    # YSLD workshop
    'doc/en/user/docs/styling/workshop/ysld/done.md',
    'doc/en/user/docs/styling/workshop/ysld/linestring.md',
    'doc/en/user/docs/styling/workshop/ysld/point.md',
    'doc/en/user/docs/styling/workshop/ysld/polygon.md',
    'doc/en/user/docs/styling/workshop/ysld/raster.md',
]

workshop_patterns = [
    (r'\.\./line/index\.md', 'linestring.md'),
    (r'\.\./polygon/index\.md', 'polygon.md'),
    (r'\.\./point/index\.md', 'point.md'),
    (r'\.\./raster/index\.md', 'raster.md'),
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
    
    # Apply regex replacements for ALL workshop links
    for workshop_file in workshop_files:
        filepath = Path(workshop_file)
        
        if not filepath.exists():
            continue
        
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
        
        original_content = content
        for pattern, replacement in workshop_patterns:
            content = re.sub(pattern, replacement, content)
        
        if content != original_content:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(content)
            
            print(f"✓ {filepath.name} - fixed workshop links")
            fixed_count += 1
    
    print(f"\n✓ Applied {fixed_count} fixes")
    return 0

if __name__ == '__main__':
    sys.exit(apply_fixes())
