#!/usr/bin/env python3
"""Fix broken anchor links in converted Markdown files"""

import re
from pathlib import Path
from collections import defaultdict

def fix_cross_document_anchors(docs_dir: Path):
    """Fix RST-style cross-document references like (#../../path/file.md)"""
    
    fixed_files = []
    
    for md_file in docs_dir.rglob("*.md"):
        content = md_file.read_text(encoding='utf-8')
        original_content = content
        
        # Pattern: (#../../path/to/file.md) or (#../path/to/file.md)
        # Should be: (../../path/to/file.md) or (../path/to/file.md)
        def fix_rst_reference(match):
            full_match = match.group(0)
            path = match.group(1)
            
            # Remove the leading # from the path
            fixed_path = path[1:]  # Remove the #
            
            return f'({fixed_path})'
        
        # Fix pattern: (#../path or #../../path)
        content = re.sub(r'\(#(\.\./[^)]+\.md)\)', fix_rst_reference, content)
        
        if content != original_content:
            md_file.write_text(content, encoding='utf-8')
            fixed_files.append(md_file.relative_to(docs_dir))
            print(f"Fixed RST cross-references: {md_file.relative_to(docs_dir)}")
    
    return fixed_files

def find_missing_anchor_targets(docs_dir: Path, html_dir: Path):
    """Find where broken anchors should point to"""
    
    # Read the broken anchors from HTML
    index_html = html_dir / "index.html"
    if not index_html.exists():
        return {}
    
    content = index_html.read_text(encoding='utf-8', errors='ignore')
    
    # Find all broken anchor links
    anchor_links = re.findall(r'href="#([^"]+)"', content)
    anchor_defs = re.findall(r'id="([^"]+)"', content)
    anchor_set = set(anchor_defs)
    
    broken_anchors = [a for a in anchor_links if a not in anchor_set]
    
    # Search for these anchors in other HTML files
    anchor_locations = {}
    
    for anchor in set(broken_anchors):
        # Skip RST-style references (already handled)
        if anchor.startswith('../') or anchor.startswith('../../'):
            continue
        
        # Search for this anchor in all HTML files
        for html_file in html_dir.rglob("*.html"):
            if html_file.name == "index.html":
                continue
            
            html_content = html_file.read_text(encoding='utf-8', errors='ignore')
            if f'id="{anchor}"' in html_content:
                # Found it!
                relative_path = html_file.relative_to(html_dir)
                anchor_locations[anchor] = str(relative_path)
                break
    
    return anchor_locations

def fix_missing_cross_references(docs_dir: Path, anchor_locations: dict):
    """Fix links that should be cross-document references"""
    
    fixed_files = []
    fixes_made = defaultdict(list)
    
    for md_file in docs_dir.rglob("*.md"):
        content = md_file.read_text(encoding='utf-8')
        original_content = content
        
        # Find all anchor-only links: [text](#anchor)
        for anchor, target_file in anchor_locations.items():
            # Convert HTML path to MD path
            target_md = target_file.replace('.html', '.md')
            
            # Calculate relative path from current file to target
            try:
                current_rel = md_file.relative_to(docs_dir)
                target_path = Path(target_md)
                
                # Calculate relative path
                current_depth = len(current_rel.parts) - 1
                target_depth = len(target_path.parts) - 1
                
                # Build relative path
                if current_depth == target_depth and current_rel.parent == target_path.parent:
                    # Same directory
                    rel_path = target_path.name
                else:
                    # Different directory - use relative path
                    rel_path = '../' * current_depth + str(target_path)
                
                # Replace [text](#anchor) with [text](rel_path#anchor)
                # But only if the link is actually in this file
                pattern = rf'\[([^\]]+)\]\(#{re.escape(anchor)}\)'
                if re.search(pattern, content):
                    replacement = rf'[\1]({rel_path}#{anchor})'
                    new_content = re.sub(pattern, replacement, content)
                    
                    if new_content != content:
                        fixes_made[md_file.relative_to(docs_dir)].append(f"#{anchor} -> {rel_path}#{anchor}")
                        content = new_content
            
            except Exception as e:
                # Skip if path calculation fails
                pass
        
        if content != original_content:
            md_file.write_text(content, encoding='utf-8')
            fixed_files.append(md_file.relative_to(docs_dir))
    
    return fixed_files, fixes_made

def main():
    print("=" * 80)
    print("Fixing Broken Anchor Links")
    print("=" * 80)
    
    docs_dir = Path("doc/en/user/docs")
    html_dir = Path("doc/en/user/target/html")
    
    if not docs_dir.exists():
        print(f"ERROR: Docs directory not found: {docs_dir}")
        return
    
    # Step 1: Fix RST-style cross-document references
    print("\nStep 1: Fixing RST-style cross-document references...")
    print("-" * 80)
    rst_fixed = fix_cross_document_anchors(docs_dir)
    print(f"Fixed {len(rst_fixed)} files with RST-style references")
    
    # Step 2: Find where broken anchors actually exist
    if html_dir.exists():
        print("\nStep 2: Finding anchor locations in HTML...")
        print("-" * 80)
        anchor_locations = find_missing_anchor_targets(docs_dir, html_dir)
        print(f"Found {len(anchor_locations)} anchors in other pages")
        
        if anchor_locations:
            print("\nSample anchor locations:")
            for anchor, location in list(anchor_locations.items())[:10]:
                print(f"  #{anchor} -> {location}")
        
        # Step 3: Fix cross-document references
        print("\nStep 3: Fixing cross-document anchor references...")
        print("-" * 80)
        cross_fixed, fixes_made = fix_missing_cross_references(docs_dir, anchor_locations)
        print(f"Fixed {len(cross_fixed)} files with cross-document references")
        
        if fixes_made:
            print("\nSample fixes made:")
            for file, fixes in list(fixes_made.items())[:5]:
                print(f"\n  {file}:")
                for fix in fixes[:3]:
                    print(f"    - {fix}")
    else:
        print(f"\nWARNING: HTML directory not found: {html_dir}")
        print("Skipping steps 2 and 3 (requires built HTML)")
    
    print("\n" + "=" * 80)
    print("Summary:")
    print(f"  RST-style references fixed: {len(rst_fixed)}")
    if html_dir.exists():
        print(f"  Cross-document references fixed: {len(cross_fixed)}")
    print("=" * 80)
    
    print("\nNext steps:")
    print("1. Rebuild HTML: cd doc/en/user && mkdocs build")
    print("2. Re-run validation: python quick_validation.py")
    print("3. Review remaining broken anchors")

if __name__ == "__main__":
    main()
