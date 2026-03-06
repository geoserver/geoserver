#!/usr/bin/env python3
"""Fix all broken anchor links comprehensively"""

import re
from pathlib import Path
from collections import defaultdict

# Manual mapping of known broken anchors to their correct targets
ANCHOR_FIXES = {
    # GeoPKG - just link to the page without anchor
    'geopkgoutput': ('community/geopkg/output.md', None),
    
    # OpenSearch EO - these anchors should exist in the same page
    'oseo_html_templates': (None, 'oseo-html-templates'),
    'oseo_metadata_templates': (None, 'oseo-metadata-templates'),
    
    # Task Manager - should be in same page
    'parameter-type': (None, 'parameter-types'),
    
    # App Schema - duplicate cache references, remove anchor
    'cache': (None, None),  # Just remove the anchor
    
    # Data directory
    'application_properties': (None, 'application-properties'),
    
    # GWC
    'gwc_webadmin': (None, 'gwc-webadmin'),
    
    # Geofence
    'Configure the plugin': (None, 'configure-the-plugin'),
    
    # WMS decorations
    'wms_dynamic_decorations': (None, 'wms-dynamic-decorations'),
    
    # Security
    'authkey': (None, 'authkey-authentication'),
    
    # CSW
    'csw_iso': (None, 'csw-iso-metadata-profile'),
    
    # WCS operations
    'getcapabilities': (None, 'getcapabilities'),
    'describecoverage': (None, 'describecoverage'),
    'getcoverage': (None, 'getcoverage'),
    
    # WFS operations
    'describefeaturetype': (None, 'describefeaturetype'),
    'getfeature': (None, 'getfeature'),
    'transaction': (None, 'transaction'),
    
    # WMS operations
    'getfeatureinfo': (None, 'getfeatureinfo'),
    'describelayer': (None, 'describelayer'),
    'data_webadmin_layers': ('../../data/webadmin/layers.md', None),
    
    # WPS
    # 'getcapabilities' already mapped above
    
    # SLD reference
    'sld_reference_linesymbolizer_css': ('../../sld/reference/linesymbolizer.md', 'css-parameters'),
    'sld_reference_fill': ('../../sld/reference/polygonsymbolizer.md', 'fill'),
    
    # Labeling (CSS properties page)
    'labeling_space_around': (None, 'space-around'),
    'labeling_group': (None, 'group'),
    'labeling_max_displacement': (None, 'max-displacement'),
    'labeling_repeat': (None, 'repeat'),
    'labeling_all_group': (None, 'all-group'),
    'labeling_follow_line': (None, 'follow-line'),
    'labeling_max_angle_delta': (None, 'max-angle-delta'),
    'labeling_autowrap': (None, 'autowrap'),
    'labeling_force_left_to_right': (None, 'force-left-to-right'),
    'labeling_conflict_resolution': (None, 'conflict-resolution'),
    'labeling_goodness_of_fit': (None, 'goodness-of-fit'),
    'labeling_priority': (None, 'priority'),
    
    # Vector tiles
    'vectortiles.install': ('../../../extensions/vectortiles/install.md', None),
    
    # Filter ECQL
    'filter_ecql_reference': ('../../../filter/ecql_reference.md', None),
    
    # WMS vendor parameters
    'wms_vendor_parameters': ('../../../services/wms/vendor.md', None),
}

# Workshop anchor patterns - these reference other workshop pages
WORKSHOP_PATTERNS = {
    r'css\.(line|polygon|point|raster)\.(q\d+|a\d+)': 'styling/workshop/css/{type}/{anchor}',
    r'ysld\.(line|polygon|point|raster)\.(q\d+|a\d+)': 'styling/workshop/ysld/{type}/{anchor}',
    r'mbstyle\.(line|polygon|point|raster)\.(q\d+|a\d+)': 'styling/workshop/mbstyle/{type}/{anchor}',
}

def fix_simple_anchor_references(docs_dir: Path):
    """Fix simple broken anchor references using the mapping"""
    
    fixed_files = []
    
    for md_file in docs_dir.rglob("*.md"):
        content = md_file.read_text(encoding='utf-8')
        original_content = content
        
        for broken_anchor, (target_file, correct_anchor) in ANCHOR_FIXES.items():
            # Pattern: [text](#broken_anchor)
            pattern = rf'\[([^\]]+)\]\(#{re.escape(broken_anchor)}\)'
            
            if re.search(pattern, content):
                if target_file and correct_anchor:
                    # Link to different file with anchor
                    replacement = rf'[\1]({target_file}#{correct_anchor})'
                elif target_file:
                    # Link to different file without anchor
                    replacement = rf'[\1]({target_file})'
                elif correct_anchor:
                    # Same file, different anchor
                    replacement = rf'[\1](#{correct_anchor})'
                else:
                    # Remove anchor entirely (just text)
                    replacement = r'[\1]'
                
                content = re.sub(pattern, replacement, content)
        
        if content != original_content:
            md_file.write_text(content, encoding='utf-8')
            fixed_files.append(md_file.relative_to(docs_dir))
            print(f"Fixed: {md_file.relative_to(docs_dir)}")
    
    return fixed_files

def fix_workshop_anchors(docs_dir: Path):
    """Fix workshop cross-references"""
    
    fixed_files = []
    
    workshop_dir = docs_dir / "styling" / "workshop"
    if not workshop_dir.exists():
        return fixed_files
    
    for md_file in workshop_dir.rglob("*.md"):
        content = md_file.read_text(encoding='utf-8')
        original_content = content
        
        # Find all anchor links
        anchor_links = re.findall(r'\[([^\]]+)\]\(#([^)]+)\)', content)
        
        for link_text, anchor in anchor_links:
            # Check if this matches a workshop pattern
            for pattern, target_template in WORKSHOP_PATTERNS.items():
                match = re.match(pattern, anchor)
                if match:
                    style_type = match.group(1)  # line, polygon, point, raster
                    anchor_id = match.group(2)   # q0, a1, etc.
                    
                    # Determine if this is a question (q) or answer (a)
                    if anchor_id.startswith('q'):
                        # Question - link to the question page
                        target_page = f"../{style_type}/index.md"
                        target_anchor = anchor
                    elif anchor_id.startswith('a'):
                        # Answer - link to the answer page
                        target_page = f"../{style_type}/index.md"
                        target_anchor = anchor
                    else:
                        continue
                    
                    # Replace the link
                    old_link = f'[{link_text}](#{anchor})'
                    new_link = f'[{link_text}]({target_page}#{target_anchor})'
                    content = content.replace(old_link, new_link)
                    break
        
        if content != original_content:
            md_file.write_text(content, encoding='utf-8')
            fixed_files.append(md_file.relative_to(docs_dir))
            print(f"Fixed workshop: {md_file.relative_to(docs_dir)}")
    
    return fixed_files

def main():
    print("=" * 80)
    print("Fixing All Broken Anchor Links")
    print("=" * 80)
    
    docs_dir = Path("doc/en/user/docs")
    
    if not docs_dir.exists():
        print(f"ERROR: Docs directory not found: {docs_dir}")
        return
    
    # Step 1: Fix simple anchor references
    print("\nStep 1: Fixing simple anchor references...")
    print("-" * 80)
    simple_fixed = fix_simple_anchor_references(docs_dir)
    print(f"Fixed {len(simple_fixed)} files")
    
    # Step 2: Fix workshop cross-references
    print("\nStep 2: Fixing workshop cross-references...")
    print("-" * 80)
    workshop_fixed = fix_workshop_anchors(docs_dir)
    print(f"Fixed {len(workshop_fixed)} workshop files")
    
    print("\n" + "=" * 80)
    print("Summary:")
    print(f"  Simple fixes: {len(simple_fixed)}")
    print(f"  Workshop fixes: {len(workshop_fixed)}")
    print(f"  Total files fixed: {len(set(simple_fixed + workshop_fixed))}")
    print("=" * 80)
    
    print("\nNext steps:")
    print("1. Rebuild HTML: mkdocs build (in doc/en/user)")
    print("2. Re-run validation: python quick_validation.py")
    print("3. Review remaining broken anchors")

if __name__ == "__main__":
    main()
