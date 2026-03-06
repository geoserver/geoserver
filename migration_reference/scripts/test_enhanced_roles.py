#!/usr/bin/env python3
"""
Test script for enhanced interpreted text role conversion
"""

import re
import tempfile
import os
from pathlib import Path

# Test cases for different role types
test_cases = {
    'api_role': {
        'input': '`/about/manifests <manifests.yaml>`{.interpreted-text role="api"}',
        'expected': '[/about/manifests](api/manifests.yaml)',
        'description': 'API role with YAML file'
    },
    'ref_role_with_anchor': {
        'input': '`Time <ecql_reference.rst#ecql_literal>`{.interpreted-text role="ref"}',
        'expected': '[Time](ecql_reference.rst#ecql_literal)',
        'description': 'Ref role with file and anchor'
    },
    'ref_role_anchor_only': {
        'input': '`some section <anchor_name>`{.interpreted-text role="ref"}',
        'expected': '[some section](#anchor_name)',
        'description': 'Ref role with anchor only'
    },
    'geotools_role': {
        'input': '`YSLD <extension/ysld.html>`{.interpreted-text role="geotools"}',
        'expected': '[YSLD](https://docs.geotools.org/latest/userguide/extension/ysld.html)',
        'description': 'GeoTools role'
    },
    'wiki_role': {
        'input': '`some page <PageName>`{.interpreted-text role="wiki"}',
        'expected': '[some page](https://github.com/geoserver/geoserver/wiki/PageName)',
        'description': 'Wiki role'
    },
    'geos_role': {
        'input': '`3586`{.interpreted-text role="geos"}',
        'expected': '[3586](https://osgeo-org.atlassian.net/browse/GEOS-3586)',
        'description': 'GEOS role (JIRA issues) - simple pattern'
    },
    'geot_role': {
        'input': '`12345`{.interpreted-text role="geot"}',
        'expected': '[12345](https://osgeo-org.atlassian.net/browse/GEOT-12345)',
        'description': 'GEOT role (GeoTools JIRA issues) - simple pattern'
    },
    'docguide_role': {
        'input': '`documentation guide <index.md>`{.interpreted-text role="docguide"}',
        'expected': '[documentation guide](../docguide/index.md)',
        'description': 'Docguide role'
    },
    'download_community_simple': {
        'input': '`features-templating`{.interpreted-text role="download_community"}',
        'expected': '[features-templating](https://build.geoserver.org/geoserver/main/community-latest/features-templating)',
        'description': 'Download community role - simple pattern'
    },
    'download_extension_simple': {
        'input': '`gwc-mbtiles`{.interpreted-text role="download_extension"}',
        'expected': '[gwc-mbtiles](https://build.geoserver.org/geoserver/main/ext-latest/gwc-mbtiles)',
        'description': 'Download extension role - simple pattern'
    },
    'download_community_colon': {
        'input': '`ogcapi-3d-geovolumes`{.interpreted-text role=":download_community"}',
        'expected': '[ogcapi-3d-geovolumes](https://build.geoserver.org/geoserver/main/community-latest/ogcapi-3d-geovolumes)',
        'description': 'Download community role with colon prefix'
    }
}

def test_role_conversion():
    """Test the enhanced role conversion logic"""
    
    # Role mappings (same as in migration.py)
    role_mappings = {
        'website': 'https://geoserver.org/',
        'developer': 'https://docs.geoserver.org/latest/en/developer/',
        'user': '../user/',
        'api': 'api/',
        'geotools': 'https://docs.geotools.org/latest/userguide/',
        'wiki': 'https://github.com/geoserver/geoserver/wiki/',
        'geos': 'https://osgeo-org.atlassian.net/browse/GEOS-',
        'geot': 'https://osgeo-org.atlassian.net/browse/GEOT-',
        'docguide': '../docguide/',
        'download_community': 'https://build.geoserver.org/geoserver/main/community-latest/',
        'download_extension': 'https://build.geoserver.org/geoserver/main/ext-latest/',
        ':download_community': 'https://build.geoserver.org/geoserver/main/community-latest/',
    }
    
    # Patterns
    pattern = r'`([^`]+) <([^>]+)>`\{\.interpreted-text role="([^"]+)"\}'
    simple_pattern = r'`([^`]+)`\{\.interpreted-text role="([^"]+)"\}'
    
    def replace_role(match):
        text = match.group(1)
        url = match.group(2)
        role = match.group(3)
        
        if role in role_mappings:
            full_url = role_mappings[role] + url
            return f'[{text}]({full_url})'
        else:
            if role == 'ref':
                if '#' in url:
                    return f'[{text}]({url})'
                else:
                    return f'[{text}](#{url})'
            return match.group(0)
    
    def replace_simple_role(match):
        text = match.group(1)
        role = match.group(2)
        
        if role in ['download_community', 'download_extension', ':download_community']:
            full_url = role_mappings.get(role, role_mappings.get(':' + role, '')) + text
            return f'[{text}]({full_url})'
        
        # For geos role (GitHub issues), use text as issue number
        if role == 'geos':
            full_url = role_mappings['geos'] + text
            return f'[{text}]({full_url})'
        
        # For geot role (GeoTools JIRA), use text as issue number
        if role == 'geot':
            full_url = role_mappings['geot'] + text
            return f'[{text}]({full_url})'
        
        return match.group(0)
    
    print("Testing Enhanced Interpreted Text Role Conversion")
    print("=" * 60)
    
    passed = 0
    failed = 0
    
    for test_name, test_data in test_cases.items():
        input_text = test_data['input']
        expected = test_data['expected']
        description = test_data['description']
        
        # Apply both patterns
        result = re.sub(pattern, replace_role, input_text)
        result = re.sub(simple_pattern, replace_simple_role, result)
        
        if result == expected:
            print(f"✓ {test_name}: {description}")
            passed += 1
        else:
            print(f"✗ {test_name}: {description}")
            print(f"  Input:    {input_text}")
            print(f"  Expected: {expected}")
            print(f"  Got:      {result}")
            failed += 1
    
    print()
    print(f"Results: {passed} passed, {failed} failed")
    
    return failed == 0

if __name__ == "__main__":
    success = test_role_conversion()
    exit(0 if success else 1)
