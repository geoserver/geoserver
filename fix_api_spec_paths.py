#!/usr/bin/env python3
"""
Fix API spec file paths to point to the correct location in doc/en/api/1.0.0/
"""

import sys
from pathlib import Path

def calculate_relative_path(from_file, to_file):
    """Calculate relative path from one file to another."""
    from_path = Path(from_file).parent
    to_path = Path(to_file)
    
    # Calculate relative path
    try:
        rel_path = Path(to_path).relative_to(from_path)
        return str(rel_path).replace('\\', '/')
    except ValueError:
        # Files are not in a parent-child relationship, need to go up
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
    # OpenSearch EO
    {
        'file': 'doc/en/user/docs/community/opensearch-eo/automation.md',
        'old': '(api/opensearch-eo.yaml)',
        'api_file': 'doc/en/api/1.0.0/opensearch-eo.yaml',
    },
    {
        'file': 'doc/en/user/docs/community/opensearch-eo/upgrading.md',
        'old': '(api/resource.yaml)',
        'api_file': 'doc/en/api/1.0.0/resource.yaml',
    },
    # Proxy Base Ext
    {
        'file': 'doc/en/user/docs/community/proxy-base-ext/usage.md',
        'old': '(api/proxy-base-ext.yaml)',
        'api_file': 'doc/en/api/1.0.0/proxy-base-ext.yaml',
    },
    # Metadata
    {
        'file': 'doc/en/user/docs/extensions/metadata/index.md',
        'old': '(api/metadata.yaml)',
        'api_file': 'doc/en/api/1.0.0/metadata.yaml',
    },
    # Params Extractor
    {
        'file': 'doc/en/user/docs/extensions/params-extractor/usage.md',
        'old': '(api/params-extractor.yaml)',
        'api_file': 'doc/en/api/1.0.0/params-extractor.yaml',
    },
    # RAT
    {
        'file': 'doc/en/user/docs/extensions/rat/using.md',
        'old': '(api/rat.yaml)',
        'api_file': 'doc/en/api/1.0.0/rat.yaml',
    },
    # WPS Download
    {
        'file': 'doc/en/user/docs/extensions/wps-download/index.md',
        'old': '(api/wpsdownload.yaml)',
        'api_file': 'doc/en/api/1.0.0/wpsdownload.yaml',
    },
    # Layer Groups
    {
        'file': 'doc/en/user/docs/styling/sld/working.md',
        'old': '(api/layergroups.yaml)',
        'api_file': 'doc/en/api/1.0.0/layergroups.yaml',
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
        
        # Calculate correct relative path
        rel_path = calculate_relative_path(fix['file'], fix['api_file'])
        new_link = f"({rel_path})"
        
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
        
        if fix['old'] not in content:
            print(f"⚠ Pattern not found in {filepath.name}: {fix['old']}")
            continue
        
        new_content = content.replace(fix['old'], new_link)
        
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)
        
        print(f"✓ {filepath.name}")
        print(f"  Changed: {fix['old']}")
        print(f"  To:      {new_link}")
        fixed_count += 1
    
    print(f"\n✓ Fixed {fixed_count}/{len(fixes)} API spec paths")
    return 0

if __name__ == '__main__':
    sys.exit(apply_fixes())
