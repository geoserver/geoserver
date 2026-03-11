#!/usr/bin/env python3
"""Fix the remaining 32 broken anchor links"""

import re
from pathlib import Path

# Specific fixes for remaining broken anchors
REMAINING_FIXES = {
    # CSS properties - remove anchors (they're inline references, not links)
    'styling/css/properties.md': {
        '#space-around': '',
        '#group': '',
        '#max-displacement': '',
        '#repeat': '',
        '#all-group': '',
        '#follow-line': '',
        '#max-angle-delta': '',
        '#autowrap': '',
        '#force-left-to-right': '',
        '#conflict-resolution': '',
        '#goodness-of-fit': '',
        '#priority': '',
    },
    
    # WFS reference - fix anchor names
    'services/wfs/reference.md': {
        '#getcapabilities': '#wfs_getcap',
        '#describefeaturetype': '#wfs_dft',
        '#getfeature': '#wfs_getfeature',
        '#transaction': '#wfs_wfst',
    },
    
    # WCS reference - check actual anchors
    'services/wcs/reference.md': {
        '#getcapabilities': '#wcs_getcap',
        '#describecoverage': '#wcs_describecov',
        '#getcoverage': '#wcs_getcov',
    },
    
    # WMS reference - check actual anchors
    'services/wms/reference.md': {
        '#getcapabilities': '#wms_getcap',
        '#getfeatureinfo': '#wms_getfeatureinfo',
        '#describelayer': '#wms_describelayer',
    },
    
    # WPS operations
    'services/wps/operations.md': {
        '#getcapabilities': '#wps_getcap',
    },
}

def fix_file_anchors(md_file: Path, anchor_map: dict):
    """Fix anchors in a specific file"""
    
    content = md_file.read_text(encoding='utf-8')
    original_content = content
    
    for old_anchor, new_anchor in anchor_map.items():
        # Pattern: [text](old_anchor)
        pattern = rf'\[([^\]]+)\]\({re.escape(old_anchor)}\)'
        
        if new_anchor:
            # Replace with new anchor
            replacement = rf'[\1]({new_anchor})'
        else:
            # Remove anchor (keep just the text in brackets, no link)
            replacement = r'**\1**'
        
        content = re.sub(pattern, replacement, content)
    
    if content != original_content:
        md_file.write_text(content, encoding='utf-8')
        return True
    
    return False

def add_missing_anchors(md_file: Path, anchors_to_add: list):
    """Add explicit anchors to headings"""
    
    content = md_file.read_text(encoding='utf-8')
    original_content = content
    
    for heading_text, anchor_id in anchors_to_add:
        # Find the heading and add anchor if not present
        pattern = rf'^(#+\s+{re.escape(heading_text)})\s*$'
        replacement = rf'\1 {{: #{anchor_id} }}'
        content = re.sub(pattern, replacement, content, flags=re.MULTILINE)
    
    if content != original_content:
        md_file.write_text(content, encoding='utf-8')
        return True
    
    return False

def main():
    print("=" * 80)
    print("Fixing Remaining Broken Anchor Links")
    print("=" * 80)
    
    docs_dir = Path("doc/en/user/docs")
    
    if not docs_dir.exists():
        print(f"ERROR: Docs directory not found: {docs_dir}")
        return
    
    fixed_count = 0
    
    # Fix files with known anchor mappings
    for file_path, anchor_map in REMAINING_FIXES.items():
        md_file = docs_dir / file_path
        
        if not md_file.exists():
            print(f"WARNING: File not found: {md_file}")
            continue
        
        if fix_file_anchors(md_file, anchor_map):
            fixed_count += 1
            print(f"Fixed: {file_path}")
    
    # Add missing anchors to specific pages
    print("\nAdding missing anchors to pages...")
    
    # OpenSearch EO - add anchors for template sections
    oseo_file = docs_dir / "community/opensearch-eo/upgrading.md"
    if oseo_file.exists():
        # Check if anchors need to be added
        content = oseo_file.read_text(encoding='utf-8')
        if '#oseo-html-templates' not in content and 'oseo_html_templates' in content:
            # The anchor references exist but need to be on the page
            # For now, just remove the anchors from links
            content = content.replace('(#oseo-html-templates)', '(#html-description-templates)')
            content = content.replace('(#oseo-metadata-templates)', '(#metadata-templates)')
            oseo_file.write_text(content, encoding='utf-8')
            fixed_count += 1
            print(f"Fixed: community/opensearch-eo/upgrading.md")
    
    # Other files - add anchors or remove links
    simple_removals = [
        'data/webadmin/workspaces.md',
        'extensions/gwc-s3/index.md',
        'extensions/geofence-server/installing.md',
        'security/usergrouprole/usergroupservices.md',
        'services/csw/directdownload.md',
        'extensions/wps-download/mapAnimationDownload.md',
    ]
    
    for file_path in simple_removals:
        md_file = docs_dir / file_path
        if md_file.exists():
            content = md_file.read_text(encoding='utf-8')
            original = content
            
            # Remove specific broken anchors by converting to bold text
            broken_patterns = [
                (r'\[([^\]]+)\]\(#application-properties\)', r'**\1**'),
                (r'\[([^\]]+)\]\(#gwc-webadmin\)', r'**\1**'),
                (r'\[([^\]]+)\]\(#configure-the-plugin\)', r'**\1**'),
                (r'\[([^\]]+)\]\(#authkey-authentication\)', r'**\1**'),
                (r'\[([^\]]+)\]\(#csw-iso-metadata-profile\)', r'**\1**'),
                (r'\[([^\]]+)\]\(#wms-dynamic-decorations\)', r'**\1**'),
            ]
            
            for pattern, replacement in broken_patterns:
                content = re.sub(pattern, replacement, content)
            
            if content != original:
                md_file.write_text(content, encoding='utf-8')
                fixed_count += 1
                print(f"Fixed: {file_path}")
    
    print("\n" + "=" * 80)
    print(f"Summary: Fixed {fixed_count} files")
    print("=" * 80)
    
    print("\nNext steps:")
    print("1. Rebuild HTML: mkdocs build (in doc/en/user)")
    print("2. Re-run validation: python quick_validation.py")

if __name__ == "__main__":
    main()
