#!/usr/bin/env python3
"""
Fix remaining specific broken links.
"""

import sys
from pathlib import Path

GITHUB_BASE = "https://github.com/geoserver/geoserver/blob/main"

fixes = [
    # Fix YSLD filters link (same issue as functions - geoserver/ prefix)
    {
        'file': 'doc/en/user/docs/styling/ysld/reference/filters.md',
        'old': '../../../geoserver/filter/ecql_reference.md',
        'new': '../../../filter/ecql_reference.md',
        'desc': 'Fix YSLD ECQL reference link'
    },
    # Fix malformed CQL tutorial link
    {
        'file': 'doc/en/user/docs/styling/css/tutorial.md',
        'old': 'CQL</tutorials/cql/cql_tutorial>.md',
        'new': '../../tutorials/cql/cql_tutorial.md',
        'desc': 'Fix malformed CQL tutorial link'
    },
    # Convert compose.yml to GitHub link
    {
        'file': 'doc/en/user/docs/community/acl/index.md',
        'old': './compose.yml',
        'new': f'{GITHUB_BASE}/doc/en/user/docs/community/acl/compose.yml',
        'desc': 'Convert compose.yml to GitHub link'
    },
    # Convert codetemplates.xml to GitHub link
    {
        'file': 'doc/en/developer/docs/eclipse-guide/index.md',
        'old': '../../../../build/codetemplates.xml',
        'new': f'{GITHUB_BASE}/build/codetemplates.xml',
        'desc': 'Convert codetemplates.xml to GitHub link'
    },
    {
        'file': 'doc/en/user/docs/extensions/printing/install.md',
        'old': '[sample page](files/print-example.md)',
        'new': '<!-- MISSING: sample page (files/print-example.md) -->sample page',
        'desc': 'Comment out missing print-example.md'
    },
    # Comment out missing styling-workshop-raster.zip
    {
        'file': 'doc/en/user/docs/styling/workshop/setup/data.md',
        'old': '[styling-workshop-raster.zip](styling-workshop-raster.zip)',
        'new': '<!-- MISSING: styling-workshop-raster.zip -->',
        'desc': 'Comment out missing styling-workshop-raster.zip'
    },
    # Comment out missing YSLD image
    {
        'file': 'doc/en/user/docs/styling/ysld/reference/featurestyles.md',
        'old': '![](img/fs_roadcasing.svg)',
        'new': '<!-- MISSING IMAGE: img/fs_roadcasing.svg -->',
        'desc': 'Comment out missing YSLD image'
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
        
        if fix['old'] not in content:
            print(f"⚠ Pattern not found in {filepath}: {fix['old'][:50]}...")
            continue
        
        new_content = content.replace(fix['old'], fix['new'])
        
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)
        
        print(f"✓ {filepath}")
        print(f"  {fix['desc']}")
        fixed_count += 1
    
    print(f"\n✓ Applied {fixed_count} fixes")
    return 0

if __name__ == '__main__':
    sys.exit(apply_fixes())
