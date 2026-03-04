#!/usr/bin/env python3
"""Quick validation check for broken links and images after fixes"""

import re
from pathlib import Path
from collections import defaultdict

def check_broken_links(html_dir: Path):
    """Check for broken anchor links in HTML"""
    broken = []
    
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
                broken.append(f"{html_file.name}#{anchor}")
    
    return broken

def check_missing_images(docs_dir: Path):
    """Check for missing image references in Markdown"""
    missing = []
    
    for md_file in docs_dir.rglob("*.md"):
        content = md_file.read_text(encoding='utf-8', errors='ignore')
        
        # Find image references
        images = re.findall(r'!\[.*?\]\(([^)]+)\)', content)
        
        for img_path in images:
            # Skip external URLs
            if img_path.startswith(('http://', 'https://', '//')):
                continue
            
            # Skip wildcards (these are intentional)
            if '*' in img_path:
                continue
            
            # Resolve relative path
            img_full_path = (md_file.parent / img_path).resolve()
            
            if not img_full_path.exists():
                missing.append((str(md_file.relative_to(docs_dir)), img_path))
    
    return missing

def main():
    print("=" * 60)
    print("Quick Validation Check")
    print("=" * 60)
    
    # Check broken anchor links
    print("\nChecking broken anchor links...")
    html_dir = Path("doc/en/user/target/html")
    if html_dir.exists():
        broken_links = check_broken_links(html_dir)
        print(f"  Broken anchor links: {len(broken_links)}")
        if broken_links and len(broken_links) <= 20:
            for link in broken_links[:20]:
                print(f"    - {link}")
    else:
        print(f"  HTML directory not found: {html_dir}")
    
    # Check missing images
    print("\nChecking missing images...")
    docs_dir = Path("doc/en/user/docs")
    if docs_dir.exists():
        missing_images = check_missing_images(docs_dir)
        print(f"  Missing images: {len(missing_images)}")
        if missing_images:
            for file, img in missing_images[:30]:
                print(f"    - {file}: {img}")
    else:
        print(f"  Docs directory not found: {docs_dir}")
    
    print("\n" + "=" * 60)
    print("Summary:")
    print(f"  Broken anchor links: {len(broken_links) if 'broken_links' in locals() else 'N/A'}")
    print(f"  Missing images: {len(missing_images) if 'missing_images' in locals() else 'N/A'}")
    print("=" * 60)
    
    # Compare with original issues
    print("\nOriginal issues:")
    print("  Broken anchor links: 128")
    print("  Missing images: 2,071")
    
    if 'broken_links' in locals() and 'missing_images' in locals():
        print("\nImprovement:")
        print(f"  Anchor links fixed: {128 - len(broken_links)}")
        print(f"  Images fixed: {2071 - len(missing_images)}")

if __name__ == "__main__":
    main()
