#!/usr/bin/env python3
"""
Fix duplicate frontmatter blocks in Markdown files.

This script removes duplicate frontmatter blocks, keeping only the first one.
"""

import os
import re
from pathlib import Path


def fix_duplicate_frontmatter(content):
    """
    Remove duplicate frontmatter blocks, keeping only the first one.
    
    Frontmatter is delimited by --- at the start and end.
    """
    # Pattern to match frontmatter blocks
    frontmatter_pattern = r'^---\n(.*?)\n---\n'
    
    # Find all frontmatter blocks
    matches = list(re.finditer(frontmatter_pattern, content, re.MULTILINE | re.DOTALL))
    
    if len(matches) <= 1:
        # No duplicate frontmatter
        return content, False
    
    # Keep only the first frontmatter block
    first_match = matches[0]
    
    # Remove all other frontmatter blocks
    new_content = content
    for match in reversed(matches[1:]):  # Reverse to maintain positions
        new_content = new_content[:match.start()] + new_content[match.end():]
    
    return new_content, True


def process_file(filepath):
    """Process a single markdown file."""
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
        
        new_content, modified = fix_duplicate_frontmatter(content)
        
        if modified:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(new_content)
            print(f"  ✓ Fixed: {filepath}")
            return True
        
        return False
    
    except Exception as e:
        print(f"  ✗ Error processing {filepath}: {e}")
        return False


def main():
    """Main function to process all markdown files."""
    
    # List of files with duplicate frontmatter
    files_to_fix = [
        "doc/en/developer/docs/index.md",
        "doc/en/docguide/docs/index.md",
        "doc/en/user/docs/community/backuprestore/installation.md",
        "doc/en/user/docs/community/features-templating/installing.md",
        "doc/en/user/docs/community/gsr/installing.md",
        "doc/en/user/docs/community/gwc-mbtiles/index.md",
        "doc/en/user/docs/community/jdbcconfig/installing.md",
        "doc/en/user/docs/community/jdbcstore/installing.md",
        "doc/en/user/docs/community/jwt-headers/installing.md",
        "doc/en/user/docs/community/mbtiles/installing.md",
        "doc/en/user/docs/community/monitor-micrometer/installation.md",
        "doc/en/user/docs/community/ogc-api/3dgeovolumes/index.md",
        "doc/en/user/docs/community/ogc-api/coverages/index.md",
        "doc/en/user/docs/community/ogc-api/maps/index.md",
        "doc/en/user/docs/community/ogc-api/processes/index.md",
        "doc/en/user/docs/community/ogc-api/styles/index.md",
        "doc/en/user/docs/community/ogc-api/testbed.md",
        "doc/en/user/docs/community/ogc-api/tiled-features/index.md",
        "doc/en/user/docs/community/ogc-api/tiles/index.md",
        "doc/en/user/docs/community/oidc/installing.md",
        "doc/en/user/docs/community/opensearch-eo/installation.md",
        "doc/en/user/docs/community/proxy-base-ext/install.md",
        "doc/en/user/docs/community/schemaless-features/install.md",
        "doc/en/user/docs/community/singlestore/index.md",
        "doc/en/user/docs/community/smart-data-loader/install.md",
        "doc/en/user/docs/community/spatialjson/installation.md",
        "doc/en/user/docs/community/stac-datastore/install.md",
        "doc/en/user/docs/community/taskmanager/user.md",
        "doc/en/user/docs/community/vector-mosaic/installing.md",
        "doc/en/user/docs/community/web-service-auth/install.md",
        "doc/en/user/docs/community/wfs-freemarker/installing.md",
        "doc/en/user/docs/community/wps-download-netcdf/index.md",
        "doc/en/user/docs/configuration/tools/resource/install.md",
        "doc/en/user/docs/data/database/db2.md",
        "doc/en/user/docs/data/database/mysql.md",
        "doc/en/user/docs/data/database/oracle.md",
        "doc/en/user/docs/data/database/sqlserver.md",
        "doc/en/user/docs/data/raster/arcgrid.md",
        "doc/en/user/docs/data/raster/gdal.md",
        "doc/en/user/docs/data/raster/imagepyramid.md",
        "doc/en/user/docs/data/raster/worldimage.md",
        "doc/en/user/docs/data/vector/featurepregen.md",
        "doc/en/user/docs/datadirectory/location.md",
        "doc/en/user/docs/extensions/arcgrid/index.md",
        "doc/en/user/docs/extensions/authkey/index.md",
        "doc/en/user/docs/extensions/cas/index.md",
        "doc/en/user/docs/extensions/controlflow/index.md",
        "doc/en/user/docs/extensions/csw-iso/installing.md",
        "doc/en/user/docs/extensions/dxf/index.md",
        "doc/en/user/docs/extensions/excel.md",
        "doc/en/user/docs/extensions/geofence/installing.md",
        "doc/en/user/docs/extensions/geofence-server/installing.md",
        "doc/en/user/docs/extensions/geofence-wps/installing.md",
        "doc/en/user/docs/extensions/geopkg-output/install.md",
        "doc/en/user/docs/extensions/grib/index.md",
        "doc/en/user/docs/extensions/gwc-s3/install.md",
        "doc/en/user/docs/extensions/iau/install.md",
        "doc/en/user/docs/extensions/image/index.md",
        "doc/en/user/docs/extensions/importer/installing.md",
        "doc/en/user/docs/extensions/inspire/installing.md",
        "doc/en/user/docs/extensions/libjpeg-turbo/index.md",
        "doc/en/user/docs/extensions/mapml/installation.md",
        "doc/en/user/docs/extensions/mongodb/index.md",
        "doc/en/user/docs/extensions/monitoring/installation.md",
        "doc/en/user/docs/extensions/params-extractor/install.md",
        "doc/en/user/docs/extensions/printing/install.md",
        "doc/en/user/docs/extensions/querylayer/index.md",
        "doc/en/user/docs/extensions/rat/installing.md",
        "doc/en/user/docs/extensions/vectortiles/install.md",
        "doc/en/user/docs/extensions/wcs20eo/index.md",
        "doc/en/user/docs/extensions/wmts-multidimensional/install.md",
        "doc/en/user/docs/extensions/wps-jdbc/index.md",
        "doc/en/user/docs/installation/docker.md",
        "doc/en/user/docs/installation/linux.md",
        "doc/en/user/docs/installation/war.md",
        "doc/en/user/docs/installation/win_binary.md",
        "doc/en/user/docs/installation/win_installer.md",
        "doc/en/user/docs/services/csw/installing.md",
        "doc/en/user/docs/services/features/install.md",
        "doc/en/user/docs/services/features/templates.md",
        "doc/en/user/docs/services/wcs/install.md",
        "doc/en/user/docs/services/wps/install.md",
        "doc/en/user/docs/styling/css/install.md",
        "doc/en/user/docs/styling/mbstyle/installing.md",
        "doc/en/user/docs/styling/workshop/setup/install.md",
        "doc/en/user/docs/styling/ysld/installing.md",
    ]
    
    print("Fixing duplicate frontmatter blocks...")
    print()
    
    modified_count = 0
    
    for filepath in files_to_fix:
        if Path(filepath).exists():
            if process_file(filepath):
                modified_count += 1
        else:
            print(f"  ⚠ File not found: {filepath}")
    
    print()
    print(f"Summary:")
    print(f"  Total files to fix: {len(files_to_fix)}")
    print(f"  Files modified: {modified_count}")
    print(f"  Files skipped: {len(files_to_fix) - modified_count}")


if __name__ == '__main__':
    main()
