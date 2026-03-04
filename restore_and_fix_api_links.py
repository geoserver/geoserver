#!/usr/bin/env python3
"""
Restore commented-out API spec links and fix their paths to point to doc/en/api/1.0.0/
"""

import sys
from pathlib import Path
import re

def calculate_relative_path(from_file, to_file):
    """Calculate relative path from one file to another."""
    from_path = Path(from_file).parent
    to_path = Path(to_file)
    
    # Calculate relative path
    from_parts = from_path.parts
    to_parts = to_path.parts
    
    # Find common ancestor
    common = 0
    for i, (f, t) in enumerate(zip(from_parts, to_parts)):
        if f == t:
            common = i + 1
        else:
            break
    
    # Go up from from_file to common ancestor
    ups = len(from_parts) - common
    rel_parts = ['..'] * ups + list(to_parts[common:])
    return '/'.join(rel_parts)

fixes = [
    # OpenSearch EO - already fixed
    {
        'file': 'doc/en/user/docs/community/opensearch-eo/automation.md',
        'pattern': r'\[/oseo\]\(../../../../api/1.0.0/opensearch-eo\.yaml\)',
        'already_fixed': True,
    },
    # OpenSearch EO upgrading - needs restoration
    {
        'file': 'doc/en/user/docs/community/opensearch-eo/upgrading.md',
        'pattern': r'\[/resource\]<!-- MISSING: api/resource\.yaml -->',
        'replacement': '[/resource](../../../../api/1.0.0/resource.yaml)',
    },
    # Proxy Base Ext
    {
        'file': 'doc/en/user/docs/community/proxy-base-ext/usage.md',
        'pattern': r'\[([^\]]+)\]<!-- MISSING: api/proxy-base-ext\.yaml -->',
        'replacement': r'[\1](../../../../api/1.0.0/proxy-base-ext.yaml)',
    },
    # Metadata
    {
        'file': 'doc/en/user/docs/extensions/metadata/index.md',
        'pattern': r'\[([^\]]+)\]<!-- MISSING: api/metadata\.yaml -->',
        'replacement': r'[\1](../../../api/1.0.0/metadata.yaml)',
    },
    # Params Extractor
    {
        'file': 'doc/en/user/docs/extensions/params-extractor/usage.md',
        'pattern': r'\[([^\]]+)\]<!-- MISSING: api/params-extractor\.yaml -->',
        'replacement': r'[\1](../../../api/1.0.0/params-extractor.yaml)',
    },
    # RAT
    {
        'file': 'doc/en/user/docs/extensions/rat/using.md',
        'pattern': r'\[([^\]]+)\]<!-- MISSING: api/rat\.yaml -->',
        'replacement': r'[\1](../../../api/1.0.0/rat.yaml)',
    },
    # WPS Download
    {
        'file': 'doc/en/user/docs/extensions/wps-download/index.md',
        'pattern': r'\[([^\]]+)\]<!-- MISSING: api/wpsdownload\.yaml -->',
        'replacement': r'[\1](../../../api/1.0.0/wpsdownload.yaml)',
    },
    # Layer Groups
    {
        'file': 'doc/en/user/docs/styling/sld/working.md',
        'pattern': r'\[([^\]]+)\]<!-- MISSING: api/layergroups\.yaml -->',
        'replacement': r'[\1](../../../api/1.0.0/layergroups.yaml)',
    },
]

def apply_fixes():
    """Apply all fixes."""
    fixed_count = 0
    
    for fix in fixes:
        if fix.get('already_fixed'):
            print(f"✓ {Path(fix['file']).name} - already fixed")
            continue
            
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
        print(f"  Restored and fixed API spec link")
        fixed_count += 1
    
    print(f"\n✓ Fixed {fixed_count} API spec paths")
    return 0

if __name__ == '__main__':
    sys.exit(apply_fixes())
