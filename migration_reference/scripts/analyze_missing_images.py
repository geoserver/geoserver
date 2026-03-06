#!/usr/bin/env python3
"""Analyze remaining missing images to categorize the issues"""

import re
from pathlib import Path
from collections import defaultdict

def analyze_missing_images(docs_dir: Path, source_dir: Path):
    """Analyze missing image references"""
    
    categories = {
        'in_source_not_copied': [],  # Exists in source/ but not in docs/
        'truly_missing': [],           # Doesn't exist anywhere
        'wildcard': [],                # Wildcard references like img/*.* 
        'wrong_path': []               # Path seems incorrect
    }
    
    for md_file in docs_dir.rglob("*.md"):
        content = md_file.read_text(encoding='utf-8', errors='ignore')
        
        # Find image references
        images = re.findall(r'!\[.*?\]\(([^)]+)\)', content)
        
        for img_path in images:
            # Skip external URLs
            if img_path.startswith(('http://', 'https://', '//')):
                continue
            
            # Check if wildcard
            if '*' in img_path:
                categories['wildcard'].append((str(md_file.relative_to(docs_dir)), img_path))
                continue
            
            # Resolve relative path from markdown file
            img_full_path = (md_file.parent / img_path).resolve()
            
            if not img_full_path.exists():
                # Check if it exists in source directory
                rel_path = md_file.relative_to(docs_dir)
                source_img = source_dir / rel_path.parent / img_path
                
                if source_img.exists():
                    categories['in_source_not_copied'].append((
                        str(md_file.relative_to(docs_dir)),
                        img_path,
                        str(source_img)
                    ))
                else:
                    # Try to find it anywhere in source
                    img_name = Path(img_path).name
                    found_in_source = list(source_dir.rglob(img_name))
                    
                    if found_in_source:
                        categories['wrong_path'].append((
                            str(md_file.relative_to(docs_dir)),
                            img_path,
                            str(found_in_source[0])
                        ))
                    else:
                        categories['truly_missing'].append((
                            str(md_file.relative_to(docs_dir)),
                            img_path
                        ))
    
    return categories

def main():
    print("=" * 70)
    print("Missing Images Analysis")
    print("=" * 70)
    
    docs_dir = Path("doc/en/user/docs")
    source_dir = Path("doc/en/user/source")
    
    if not docs_dir.exists():
        print(f"ERROR: Docs directory not found: {docs_dir}")
        return
    
    if not source_dir.exists():
        print(f"ERROR: Source directory not found: {source_dir}")
        return
    
    print(f"\nAnalyzing images...")
    print(f"  Docs dir: {docs_dir}")
    print(f"  Source dir: {source_dir}")
    
    categories = analyze_missing_images(docs_dir, source_dir)
    
    print("\n" + "=" * 70)
    print("Results by Category")
    print("=" * 70)
    
    # Category 1: In source, not copied
    count = len(categories['in_source_not_copied'])
    print(f"\n1. Images in source/ but not copied to docs/: {count}")
    if count > 0:
        print("   These can be automatically copied!")
        for md_file, img_path, source_path in categories['in_source_not_copied'][:5]:
            print(f"   - {md_file}: {img_path}")
            print(f"     Source: {source_path}")
    
    # Category 2: Wrong path
    count = len(categories['wrong_path'])
    print(f"\n2. Images with wrong path (exists elsewhere): {count}")
    if count > 0:
        print("   These need path correction in markdown!")
        for md_file, img_path, actual_path in categories['wrong_path'][:5]:
            print(f"   - {md_file}: {img_path}")
            print(f"     Found at: {actual_path}")
    
    # Category 3: Wildcard references
    count = len(categories['wildcard'])
    print(f"\n3. Wildcard image references: {count}")
    if count > 0:
        print("   These need manual review!")
        for md_file, img_path in categories['wildcard'][:5]:
            print(f"   - {md_file}: {img_path}")
    
    # Category 4: Truly missing
    count = len(categories['truly_missing'])
    print(f"\n4. Truly missing (not found anywhere): {count}")
    if count > 0:
        print("   These images don't exist in the repository!")
        for md_file, img_path in categories['truly_missing'][:10]:
            print(f"   - {md_file}: {img_path}")
    
    print("\n" + "=" * 70)
    print("Summary")
    print("=" * 70)
    print(f"  Can auto-copy: {len(categories['in_source_not_copied'])}")
    print(f"  Need path fix: {len(categories['wrong_path'])}")
    print(f"  Wildcard refs: {len(categories['wildcard'])}")
    print(f"  Truly missing: {len(categories['truly_missing'])}")
    print(f"  TOTAL: {sum(len(v) for v in categories.values())}")
    
    # Save detailed report
    with open('missing_images_analysis.txt', 'w', encoding='utf-8') as f:
        f.write("Missing Images Detailed Analysis\n")
        f.write("=" * 70 + "\n\n")
        
        f.write("1. Images in source/ but not copied to docs/\n")
        f.write("-" * 70 + "\n")
        for md_file, img_path, source_path in categories['in_source_not_copied']:
            f.write(f"{md_file}: {img_path}\n")
            f.write(f"  Source: {source_path}\n\n")
        
        f.write("\n2. Images with wrong path\n")
        f.write("-" * 70 + "\n")
        for md_file, img_path, actual_path in categories['wrong_path']:
            f.write(f"{md_file}: {img_path}\n")
            f.write(f"  Found at: {actual_path}\n\n")
        
        f.write("\n3. Wildcard references\n")
        f.write("-" * 70 + "\n")
        for md_file, img_path in categories['wildcard']:
            f.write(f"{md_file}: {img_path}\n")
        
        f.write("\n4. Truly missing\n")
        f.write("-" * 70 + "\n")
        for md_file, img_path in categories['truly_missing']:
            f.write(f"{md_file}: {img_path}\n")
    
    print("\nDetailed report saved to: missing_images_analysis.txt")

if __name__ == "__main__":
    main()
