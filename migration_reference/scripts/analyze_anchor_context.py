#!/usr/bin/env python3
"""Analyze the context of broken anchor links to understand the issue"""

import re
from pathlib import Path
from collections import defaultdict

def analyze_index_html(html_dir: Path):
    """Analyze the index.html to understand where broken links come from"""
    
    index_html = html_dir / "index.html"
    if not index_html.exists():
        print(f"ERROR: {index_html} not found")
        return
    
    content = index_html.read_text(encoding='utf-8', errors='ignore')
    
    # Find all anchor links with context
    # Pattern: <a href="#anchor">text</a>
    anchor_pattern = r'<a[^>]*href="#([^"]+)"[^>]*>([^<]*)</a>'
    matches = re.findall(anchor_pattern, content)
    
    # Find all anchor definitions in index.html
    anchor_defs = set(re.findall(r'id="([^"]+)"', content))
    
    # Categorize broken anchors
    broken_anchors = []
    for anchor, link_text in matches:
        if anchor not in anchor_defs:
            broken_anchors.append((anchor, link_text))
    
    print(f"Total broken anchors in index.html: {len(broken_anchors)}")
    print(f"\nSample broken anchors with link text:")
    for anchor, text in broken_anchors[:20]:
        print(f"  #{anchor} - '{text[:50]}'")
    
    # Search for these anchors in other HTML files
    print(f"\n{'='*80}")
    print("Searching for anchor locations in other pages...")
    print('='*80)
    
    anchor_locations = {}
    for anchor, link_text in broken_anchors[:30]:  # Check first 30
        for html_file in html_dir.rglob("*.html"):
            if html_file.name == "index.html":
                continue
            
            html_content = html_file.read_text(encoding='utf-8', errors='ignore')
            if f'id="{anchor}"' in html_content:
                anchor_locations[anchor] = html_file.relative_to(html_dir)
                print(f"  Found #{anchor} in {html_file.relative_to(html_dir)}")
                break
    
    print(f"\nFound {len(anchor_locations)} out of {len(broken_anchors)} anchors")
    
    # Analyze patterns
    print(f"\n{'='*80}")
    print("Anchor Pattern Analysis:")
    print('='*80)
    
    patterns = defaultdict(list)
    for anchor, text in broken_anchors:
        if '.' in anchor:
            # Looks like css.line.q0, ysld.point.a1, etc.
            prefix = anchor.split('.')[0]
            patterns[f'{prefix}.*'].append(anchor)
        elif '_' in anchor:
            # Looks like oseo_html_templates, wms_dynamic_decorations
            patterns['snake_case'].append(anchor)
        else:
            patterns['other'].append(anchor)
    
    for pattern, anchors in sorted(patterns.items()):
        print(f"\n{pattern} ({len(anchors)} anchors):")
        for anchor in anchors[:5]:
            print(f"  - {anchor}")
        if len(anchors) > 5:
            print(f"  ... and {len(anchors) - 5} more")

def check_mkdocs_nav(mkdocs_yml: Path):
    """Check if index.html is generated from a nav structure"""
    
    if not mkdocs_yml.exists():
        print(f"ERROR: {mkdocs_yml} not found")
        return
    
    content = mkdocs_yml.read_text(encoding='utf-8')
    
    print(f"\n{'='*80}")
    print("MkDocs Navigation Structure:")
    print('='*80)
    
    # Check if there's a nav section
    if 'nav:' in content:
        print("Found nav section in mkdocs.yml")
        # Extract nav section
        nav_start = content.find('nav:')
        nav_section = content[nav_start:nav_start+2000]
        print(nav_section[:1000])
    else:
        print("No nav section found - using automatic navigation")

def main():
    print("=" * 80)
    print("Anchor Context Analysis")
    print("=" * 80)
    
    html_dir = Path("doc/en/user/target/html")
    mkdocs_yml = Path("doc/en/user/mkdocs.yml")
    
    if html_dir.exists():
        analyze_index_html(html_dir)
    
    if mkdocs_yml.exists():
        check_mkdocs_nav(mkdocs_yml)

if __name__ == "__main__":
    main()
