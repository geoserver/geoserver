#!/usr/bin/env python3
"""
Fix version macro to snapshot macro in download links.

The second download link should use {{ snapshot }} macro, not {{ version }}.
This script updates all affected files.
"""

import re
from pathlib import Path

def fix_version_to_snapshot(file_path: Path) -> tuple[bool, int]:
    """
    Replace {{ version }} with {{ snapshot }} in download links.
    
    Returns: (changed, count)
    """
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original_content = content
    
    # Pattern: {{ version }} [geoserver-{{ version }}-...-plugin.zip](URL)
    # Replace with: {{ snapshot }} [geoserver-{{ snapshot }}-...-plugin.zip](URL)
    
    # First, replace in link text: [geoserver-{{ version }}-...-plugin.zip]
    pattern1 = r'\[geoserver-\{\{\s*version\s*\}\}-(.*?)-plugin\.zip\]'
    replacement1 = r'[geoserver-{{ snapshot }}-\1-plugin.zip]'
    content = re.sub(pattern1, replacement1, content)
    
    # Second, replace the macro prefix: {{ version }} [geoserver-...
    pattern2 = r'\{\{\s*version\s*\}\}\s+\[geoserver-\{\{\s*snapshot\s*\}\}'
    replacement2 = r'{{ snapshot }} [geoserver-{{ snapshot }}'
    content = re.sub(pattern2, replacement2, content)
    
    # Third, replace in URLs: /geoserver-{{ version }}-
    pattern3 = r'/geoserver-\{\{\s*version\s*\}\}-'
    replacement3 = r'/geoserver-{{ snapshot }}-'
    content = re.sub(pattern3, replacement3, content)
    
    changed = content != original_content
    
    if changed:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)
        
        # Count replacements
        count = len(re.findall(r'\{\{\s*snapshot\s*\}\}', content)) - len(re.findall(r'\{\{\s*snapshot\s*\}\}', original_content))
        return True, count
    
    return False, 0

def main():
    """Process all modified documentation files."""
    
    # List of files from git diff
    files = [
        "doc/en/user/docs/configuration/tools/resource/install.md",
        "doc/en/user/docs/data/database/mysql.md",
        "doc/en/user/docs/data/database/oracle.md",
        "doc/en/user/docs/data/database/sqlserver.md",
        "doc/en/user/docs/data/raster/arcgrid.md",
        "doc/en/user/docs/data/raster/gdal.md",
        "doc/en/user/docs/data/raster/imagepyramid.md",
        "doc/en/user/docs/data/raster/worldimage.md",
        "doc/en/user/docs/data/vector/featurepregen.md",
        "doc/en/user/docs/extensions/arcgrid/index.md",
        "doc/en/user/docs/extensions/geofence-server/installing.md",
        "doc/en/user/docs/extensions/image/index.md",
        "doc/en/user/docs/services/csw/installing.md",
        "doc/en/user/docs/services/features/install.md",
        "doc/en/user/docs/services/wcs/install.md",
        "doc/en/user/docs/services/wps/install.md",
        "doc/en/user/docs/styling/css/install.md",
        "doc/en/user/docs/styling/mbstyle/installing.md",
        "doc/en/user/docs/styling/workshop/setup/install.md",
        "doc/en/user/docs/styling/ysld/installing.md",
    ]
    
    total_changed = 0
    total_replacements = 0
    
    print("Fixing version macro to snapshot macro in download links...")
    print("=" * 80)
    
    for file_path_str in files:
        file_path = Path(file_path_str)
        if not file_path.exists():
            print(f"⚠ Skipping {file_path} (not found)")
            continue
        
        changed, count = fix_version_to_snapshot(file_path)
        if changed:
            total_changed += 1
            total_replacements += count
            print(f"✓ Fixed {file_path} ({count} replacements)")
    
    print("=" * 80)
    print(f"Summary: Fixed {total_changed} files with {total_replacements} replacements")

if __name__ == "__main__":
    main()
