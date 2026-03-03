#!/usr/bin/env python3
"""Diagnose broken anchor links by file"""

import re
from pathlib import Path
from collections import defaultdict

def check_broken_links_by_file(html_dir: Path):
    """Check for broken anchor links in HTML, grouped by file"""
    
    broken_by_file = defaultdict(list)
    
    for html_file in html_dir.rglob("*.html"):
        content = html_file.read_text(encoding='utf-8', errors='ignore')
        
        # Find anchor links (within same page)
        anchor_links = re.findall(r'href="#([^"]+)"', content)
        
        # Find anchor definitions
        anchor_defs = re.findall(r'id="([^"]+)"', content)
        anchor_set = set(anchor_defs)
        
        # Check for broken anchors
        for anchor in anchor_links:
            if anchor not in anchor_set:
                broken_by_file[html_file.relative_to(html_dir)].append(anchor)
    
    return broken_by_file

def find_markdown_source(html_path: Path, docs_dir: Path):
    """Find the markdown source file for an HTML file"""
    
    # Convert HTML path to potential MD path
    # e.g., community/geopkg/index.html -> community/geopkg/index.md
    md_path = str(html_path).replace('.html', '.md')
    
    full_md_path = docs_dir / md_path
    if full_md_path.exists():
        return full_md_path
    
    return None

def main():
    print("=" * 80)
    print("Broken Anchor Diagnosis by File")
    print("=" * 80)
    
    html_dir = Path("doc/en/user/target/html")
    docs_dir = Path("doc/en/user/docs")
    
    if not html_dir.exists():
        print(f"ERROR: HTML directory not found: {html_dir}")
        return
    
    broken_by_file = check_broken_links_by_file(html_dir)
    
    total_broken = sum(len(anchors) for anchors in broken_by_file.values())
    
    print(f"\nTotal broken anchor links: {total_broken}")
    print(f"Files with broken anchors: {len(broken_by_file)}")
    
    # Show all files with broken anchors
    print("\n" + "=" * 80)
    print("Files with Broken Anchors:")
    print("=" * 80)
    
    for html_file, anchors in sorted(broken_by_file.items(), key=lambda x: -len(x[1])):
        print(f"\n{html_file} ({len(anchors)} broken anchors):")
        
        # Find markdown source
        md_source = find_markdown_source(html_file, docs_dir)
        if md_source:
            print(f"  Source: {md_source.relative_to(docs_dir)}")
        
        # Show first 10 broken anchors
        for anchor in anchors[:10]:
            print(f"  - #{anchor}")
        
        if len(anchors) > 10:
            print(f"  ... and {len(anchors) - 10} more")
    
    # Export detailed report
    report_path = Path("broken_anchors_by_file.txt")
    with open(report_path, 'w', encoding='utf-8') as f:
        f.write("Broken Anchor Links by File\n")
        f.write("=" * 80 + "\n\n")
        f.write(f"Total broken anchor links: {total_broken}\n")
        f.write(f"Files with broken anchors: {len(broken_by_file)}\n\n")
        
        for html_file, anchors in sorted(broken_by_file.items()):
            f.write(f"\n{html_file} ({len(anchors)} broken anchors):\n")
            md_source = find_markdown_source(html_file, docs_dir)
            if md_source:
                f.write(f"  Source: {md_source.relative_to(docs_dir)}\n")
            for anchor in anchors:
                f.write(f"  - #{anchor}\n")
    
    print(f"\n\nDetailed report saved to: {report_path}")

if __name__ == "__main__":
    main()
