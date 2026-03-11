#!/usr/bin/env python3
"""Copy missing images from source/ to docs/ directory"""

import re
import shutil
from pathlib import Path
from collections import defaultdict

def copy_missing_images(docs_dir: Path, source_dir: Path, dry_run: bool = False):
    """Copy images that exist in source/ but are missing in docs/"""
    
    copied = []
    errors = []
    
    for md_file in docs_dir.rglob("*.md"):
        content = md_file.read_text(encoding='utf-8', errors='ignore')
        
        # Find image references
        images = re.findall(r'!\[.*?\]\(([^)]+)\)', content)
        
        for img_path in images:
            # Skip external URLs and wildcards
            if img_path.startswith(('http://', 'https://', '//')) or '*' in img_path:
                continue
            
            # Resolve relative path from markdown file
            img_full_path = (md_file.parent / img_path).resolve()
            
            if not img_full_path.exists():
                # Check if it exists in source directory
                rel_path = md_file.relative_to(docs_dir)
                source_img = source_dir / rel_path.parent / img_path
                
                if source_img.exists():
                    # Create destination directory if needed
                    img_full_path.parent.mkdir(parents=True, exist_ok=True)
                    
                    if not dry_run:
                        try:
                            shutil.copy2(source_img, img_full_path)
                            copied.append((str(source_img), str(img_full_path)))
                        except Exception as e:
                            errors.append((str(source_img), str(img_full_path), str(e)))
                    else:
                        copied.append((str(source_img), str(img_full_path)))
    
    return copied, errors

def main():
    import argparse
    
    parser = argparse.ArgumentParser(description='Copy missing images from source/ to docs/')
    parser.add_argument('--dry-run', action='store_true', help='Show what would be copied without actually copying')
    parser.add_argument('--doc-type', choices=['user', 'developer', 'docguide', 'all'], default='all',
                       help='Which documentation type to process')
    args = parser.parse_args()
    
    print("=" * 70)
    print("Copy Missing Images from source/ to docs/")
    print("=" * 70)
    
    doc_types = ['user', 'developer', 'docguide'] if args.doc_type == 'all' else [args.doc_type]
    
    total_copied = 0
    total_errors = 0
    
    for doc_type in doc_types:
        docs_dir = Path(f"doc/en/{doc_type}/docs")
        source_dir = Path(f"doc/en/{doc_type}/source")
        
        if not docs_dir.exists():
            print(f"\n⚠ Skipping {doc_type} (docs directory not found: {docs_dir})")
            continue
        
        if not source_dir.exists():
            print(f"\n⚠ Skipping {doc_type} (source directory not found: {source_dir})")
            continue
        
        print(f"\nProcessing: {doc_type}")
        print(f"  Docs dir: {docs_dir}")
        print(f"  Source dir: {source_dir}")
        print(f"  Dry run: {args.dry_run}")
        
        copied, errors = copy_missing_images(docs_dir, source_dir, dry_run=args.dry_run)
        
        print(f"\n  {'[DRY RUN] ' if args.dry_run else ''}Copied: {len(copied)} images")
        if errors:
            print(f"  Errors: {len(errors)}")
            for src, dst, err in errors[:5]:
                print(f"    ERROR: {src} -> {dst}")
                print(f"           {err}")
        
        # Show sample of copied files
        if copied and len(copied) <= 10:
            print(f"\n  Files copied:")
            for src, dst in copied:
                print(f"    {Path(src).name} -> {Path(dst).relative_to(docs_dir)}")
        elif copied:
            print(f"\n  Sample files copied:")
            for src, dst in copied[:5]:
                print(f"    {Path(src).name} -> {Path(dst).relative_to(docs_dir)}")
            print(f"    ... and {len(copied) - 5} more")
        
        total_copied += len(copied)
        total_errors += len(errors)
    
    print("\n" + "=" * 70)
    print("Summary")
    print("=" * 70)
    print(f"  Total images copied: {total_copied}")
    print(f"  Total errors: {total_errors}")
    print("=" * 70)
    
    if args.dry_run:
        print("\n⚠ This was a dry run. No files were copied.")
        print("Run without --dry-run to actually copy the images.")
    else:
        print("\n✓ Images have been copied!")
        print("Next steps:")
        print("  1. Verify: python quick_validation.py")
        print("  2. Test build: cd doc/en/user && mkdocs build")
        print("  3. Review: git status")
        print("  4. Commit: git add doc/en/ && git commit -m 'Copy missing images from source to docs'")

if __name__ == "__main__":
    main()
