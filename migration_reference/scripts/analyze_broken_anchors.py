#!/usr/bin/env python3
"""Analyze broken anchor links in detail"""

import re
from pathlib import Path
from collections import defaultdict

def analyze_broken_anchors(html_dir: Path):
    """Analyze broken anchor links with detailed categorization"""
    
    broken_by_file = defaultdict(list)
    anchor_patterns = defaultdict(int)
    
    for html_file in html_dir.rglob("*.html"):
        content = html_file.read_text(encoding='utf-8', errors='ignore')
        
        # Find anchor links
        anchor_links = re.findall(r'href="#([^"]+)"', content)
        
        # Find anchor definitions
        anchor_defs = re.findall(r'id="([^"]+)"', content)
        anchor_set = set(anchor_defs)
        
        # Check for broken anchors
        for anchor in anchor_links:
            if anchor not in anchor_set:
                broken_by_file[html_file.name].append(anchor)
                
                # Categorize by pattern
                if anchor.isupper():
                    anchor_patterns['ALL_UPPERCASE'] += 1
                elif anchor[0].isupper():
                    anchor_patterns['TitleCase'] += 1
                elif '-' in anchor:
                    anchor_patterns['kebab-case'] += 1
                elif '_' in anchor:
                    anchor_patterns['snake_case'] += 1
                else:
                    anchor_patterns['lowercase'] += 1
    
    return broken_by_file, anchor_patterns

def find_markdown_sources(docs_dir: Path, broken_by_file: dict):
    """Find the markdown source files for broken anchors"""
    
    md_sources = {}
    
    for html_file, anchors in broken_by_file.items():
        # Convert HTML filename to potential MD path
        # e.g., "index.html" -> look for "index.md" in various locations
        base_name = html_file.replace('.html', '')
        
        # Search for matching markdown files
        for md_file in docs_dir.rglob(f"*{base_name}.md"):
            if html_file not in md_sources:
                md_sources[html_file] = []
            md_sources[html_file].append(md_file)
    
    return md_sources

def main():
    print("=" * 80)
    print("Broken Anchor Link Analysis")
    print("=" * 80)
    
    html_dir = Path("doc/en/user/target/html")
    docs_dir = Path("doc/en/user/docs")
    
    if not html_dir.exists():
        print(f"ERROR: HTML directory not found: {html_dir}")
        return
    
    # Analyze broken anchors
    broken_by_file, anchor_patterns = analyze_broken_anchors(html_dir)
    
    total_broken = sum(len(anchors) for anchors in broken_by_file.values())
    
    print(f"\nTotal broken anchor links: {total_broken}")
    print(f"Files with broken anchors: {len(broken_by_file)}")
    
    # Show pattern distribution
    print("\n" + "=" * 80)
    print("Anchor Pattern Distribution:")
    print("=" * 80)
    for pattern, count in sorted(anchor_patterns.items(), key=lambda x: -x[1]):
        print(f"  {pattern:20s}: {count:3d} ({count/total_broken*100:.1f}%)")
    
    # Show top files with most broken anchors
    print("\n" + "=" * 80)
    print("Top 20 Files with Most Broken Anchors:")
    print("=" * 80)
    sorted_files = sorted(broken_by_file.items(), key=lambda x: -len(x[1]))
    for html_file, anchors in sorted_files[:20]:
        print(f"\n{html_file} ({len(anchors)} broken anchors):")
        for anchor in anchors[:10]:  # Show first 10 anchors
            print(f"  - #{anchor}")
        if len(anchors) > 10:
            print(f"  ... and {len(anchors) - 10} more")
    
    # Sample some broken anchors to understand the issue
    print("\n" + "=" * 80)
    print("Sample Analysis (First 5 Files):")
    print("=" * 80)
    
    for html_file, anchors in list(sorted_files)[:5]:
        print(f"\n{html_file}:")
        
        # Read the HTML to see what anchors exist
        html_path = html_dir / html_file
        content = html_path.read_text(encoding='utf-8', errors='ignore')
        existing_anchors = re.findall(r'id="([^"]+)"', content)
        
        print(f"  Broken anchors: {anchors[:5]}")
        print(f"  Existing anchors (sample): {existing_anchors[:5]}")
        
        # Try to find similar anchors
        for broken in anchors[:3]:
            similar = [a for a in existing_anchors if broken.lower() in a.lower() or a.lower() in broken.lower()]
            if similar:
                print(f"  Possible match for '{broken}': {similar[:3]}")
    
    # Export detailed report
    report_path = Path("broken_anchors_detailed_report.txt")
    with open(report_path, 'w', encoding='utf-8') as f:
        f.write("Broken Anchor Links - Detailed Report\n")
        f.write("=" * 80 + "\n\n")
        f.write(f"Total broken anchor links: {total_broken}\n")
        f.write(f"Files with broken anchors: {len(broken_by_file)}\n\n")
        
        f.write("All Files with Broken Anchors:\n")
        f.write("=" * 80 + "\n")
        for html_file, anchors in sorted(broken_by_file.items()):
            f.write(f"\n{html_file} ({len(anchors)} broken anchors):\n")
            for anchor in anchors:
                f.write(f"  - #{anchor}\n")
    
    print(f"\n\nDetailed report saved to: {report_path}")

if __name__ == "__main__":
    main()
