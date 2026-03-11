#!/usr/bin/env python3
"""Find where broken anchor links appear in HTML"""

import re
from pathlib import Path

def find_anchor_in_html(html_file: Path, anchor: str):
    """Find all occurrences of an anchor link in HTML with context"""
    
    content = html_file.read_text(encoding='utf-8', errors='ignore')
    
    # Search for href="#anchor"
    pattern = rf'<a[^>]*href="#{re.escape(anchor)}"[^>]*>([^<]*)</a>'
    matches = re.finditer(pattern, content, re.IGNORECASE)
    
    results = []
    for match in matches:
        start = max(0, match.start() - 200)
        end = min(len(content), match.end() + 200)
        context = content[start:end]
        link_text = match.group(1)
        results.append((link_text, context))
    
    return results

def main():
    html_dir = Path("doc/en/user/target/html")
    index_html = html_dir / "index.html"
    
    if not index_html.exists():
        print(f"ERROR: {index_html} not found")
        return
    
    # Test with a few known broken anchors
    test_anchors = [
        'geopkgoutput',
        'oseo_html_templates',
        'parameter-type',
        'cache',
        'getcapabilities'
    ]
    
    print("=" * 80)
    print("Finding Anchor Link Sources in HTML")
    print("=" * 80)
    
    for anchor in test_anchors:
        print(f"\nSearching for #{anchor}...")
        results = find_anchor_in_html(index_html, anchor)
        
        if results:
            print(f"  Found {len(results)} occurrence(s)")
            for i, (link_text, context) in enumerate(results[:2], 1):
                print(f"\n  Occurrence {i}:")
                print(f"    Link text: '{link_text}'")
                print(f"    Context: ...{context[:150]}...")
        else:
            print(f"  Not found in index.html")
    
    # Also check if these anchors exist in other pages
    print("\n" + "=" * 80)
    print("Checking if anchors exist in other pages...")
    print("=" * 80)
    
    for anchor in test_anchors:
        for html_file in html_dir.rglob("*.html"):
            if html_file.name == "index.html":
                continue
            
            content = html_file.read_text(encoding='utf-8', errors='ignore')
            if f'id="{anchor}"' in content:
                print(f"\n  #{anchor} exists in {html_file.relative_to(html_dir)}")
                break

if __name__ == "__main__":
    main()
