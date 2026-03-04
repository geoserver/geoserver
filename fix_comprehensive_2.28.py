#!/usr/bin/env python3
"""
Comprehensive fix for remaining 2.28.x branch warnings.
"""

import sys
from pathlib import Path

GITHUB_BASE = "https://github.com/geoserver/geoserver/blob/2.28.x"

fixes = [
    # Fix geopkg output link (duplicate path)
    {
        'file': 'doc/en/user/docs/community/geopkg/index.md',
        'old': 'community/geopkg/output.md',
        'new': 'output.md',
        'desc': 'Fix duplicate path in geopkg output link'
    },
    # Fix jdbcstore app-schema link (wrong path)
    {
        'file': 'doc/en/user/docs/community/jdbcstore/configuration.md',
        'old': './../data/app-schema/index.md',
        'new': '../../data/app-schema/index.md',
        'desc': 'Fix jdbcstore app-schema link path'
    },
    # Fix datadirectory rest link
    {
        'file': 'doc/en/user/docs/datadirectory/structure.md',
        'old': './rest/index.md',
        'new': '../rest/index.md',
        'desc': 'Fix datadirectory rest link'
    },
    # Fix mbstyle vectortiles link (too many ../)
    {
        'file': 'doc/en/user/docs/styling/mbstyle/source.md',
        'old': '../../../extensions/vectortiles/install.md',
        'new': '../../extensions/vectortiles/install.md',
        'desc': 'Fix mbstyle vectortiles link'
    },
    # Fix WMS get_legend_graphic SLD links (wrong path - services/sld doesn't exist)
    {
        'file': 'doc/en/user/docs/services/wms/get_legend_graphic/index.md',
        'old': '../../sld/reference/linesymbolizer.md',
        'new': '../../../styling/sld/reference/linesymbolizer.md',
        'desc': 'Fix WMS legend SLD linesymbolizer link'
    },
    {
        'file': 'doc/en/user/docs/services/wms/get_legend_graphic/index.md',
        'old': '../../sld/reference/polygonsymbolizer.md',
        'new': '../../../styling/sld/reference/polygonsymbolizer.md',
        'desc': 'Fix WMS legend SLD polygonsymbolizer link'
    },
    # Fix workshop CSS done.md links (line/polygon/point should be linestring/polygon/point)
    {
        'file': 'doc/en/user/docs/styling/workshop/css/done.md',
        'old': '../line/index.md',
        'new': 'linestring.md',
        'desc': 'Fix CSS workshop line links'
    },
    {
        'file': 'doc/en/user/docs/styling/workshop/css/done.md',
        'old': '../polygon/index.md',
        'new': 'polygon.md',
        'desc': 'Fix CSS workshop polygon links'
    },
    {
        'file': 'doc/en/user/docs/styling/workshop/css/done.md',
        'old': '../point/index.md',
        'new': 'point.md',
        'desc': 'Fix CSS workshop point links'
    },
    # Fix developer manual docguide link
    {
        'file': 'doc/en/developer/docs/policies/service_providers.md',
        'old': '../docguide/quickfix.html',
        'new': 'https://docs.geoserver.org/latest/en/docguide/quickfix.html',
        'desc': 'Fix developer manual docguide cross-manual link'
    },
    # Comment out missing API spec files
    {
        'file': 'doc/en/user/docs/community/opensearch-eo/automation.md',
        'old': '[/oseo](api/opensearch-eo.yaml)',
        'new': '<!-- MISSING: /oseo (api/opensearch-eo.yaml) -->/oseo',
        'desc': 'Comment out missing opensearch-eo API spec'
    },
    {
        'file': 'doc/en/user/docs/community/opensearch-eo/upgrading.md',
        'old': '(api/resource.yaml)',
        'new': '<!-- MISSING: api/resource.yaml -->',
        'desc': 'Comment out missing resource API spec'
    },
    {
        'file': 'doc/en/user/docs/community/proxy-base-ext/usage.md',
        'old': '(api/proxy-base-ext.yaml)',
        'new': '<!-- MISSING: api/proxy-base-ext.yaml -->',
        'desc': 'Comment out missing proxy-base-ext API spec'
    },
    {
        'file': 'doc/en/user/docs/extensions/metadata/index.md',
        'old': '(api/metadata.yaml)',
        'new': '<!-- MISSING: api/metadata.yaml -->',
        'desc': 'Comment out missing metadata API spec'
    },
    {
        'file': 'doc/en/user/docs/extensions/params-extractor/usage.md',
        'old': '(api/params-extractor.yaml)',
        'new': '<!-- MISSING: api/params-extractor.yaml -->',
        'desc': 'Comment out missing params-extractor API spec'
    },
    {
        'file': 'doc/en/user/docs/extensions/rat/using.md',
        'old': '(api/rat.yaml)',
        'new': '<!-- MISSING: api/rat.yaml -->',
        'desc': 'Comment out missing rat API spec'
    },
    {
        'file': 'doc/en/user/docs/extensions/wps-download/index.md',
        'old': '(api/wpsdownload.yaml)',
        'new': '<!-- MISSING: api/wpsdownload.yaml -->',
        'desc': 'Comment out missing wpsdownload API spec'
    },
    {
        'file': 'doc/en/user/docs/styling/sld/working.md',
        'old': '(api/layergroups.yaml)',
        'new': '<!-- MISSING: api/layergroups.yaml -->',
        'desc': 'Comment out missing layergroups API spec'
    },
    # Comment out missing image files
    {
        'file': 'doc/en/user/docs/gettingstarted/shapefile-quickstart/index.md',
        'old': '![](new_workspace.png)',
        'new': '<!-- MISSING IMAGE: new_workspace.png -->',
        'desc': 'Comment out missing new_workspace.png'
    },
    {
        'file': 'doc/en/user/docs/services/wms/googleearth/tutorials/superoverlaysgwc.md',
        'old': '![](../googleearth.jpg)',
        'new': '<!-- MISSING IMAGE: ../googleearth.jpg -->',
        'desc': 'Comment out missing googleearth.jpg'
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
            print(f"⚠ Pattern not found in {filepath.name}: {fix['old'][:60]}...")
            continue
        
        new_content = content.replace(fix['old'], fix['new'])
        
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)
        
        print(f"✓ {filepath.name}")
        print(f"  {fix['desc']}")
        fixed_count += 1
    
    print(f"\n✓ Applied {fixed_count}/{len(fixes)} fixes")
    return 0

if __name__ == '__main__':
    sys.exit(apply_fixes())
