#!/usr/bin/env python3
"""
Comprehensive fix for all remaining image path issues.
Tries multiple strategies to find the correct path.
"""

import re
from pathlib import Path

def fix_all_image_paths(docs_dir: Path, source_dir: Path, dry_run: bool = False):
    """Fix all image paths by trying multiple strategies"""
    
    fixed_files = 0
    total_fixes = 0
    
    print(f"\nProcessing: {docs_dir}")
    print(f"Dry run: {dry_run}\n")
    
    for md_file in sorted(docs_dir.rglob("*.md")):
        content = md_file.read_text(encoding='utf-8')
        original_content = content
        file_fixes = 0
        
        # Find all image references
        def fix_path(match):
            nonlocal file_fixes
            alt_text = match.group(1)
            img_path = match.group(2)
            
            # Skip external URLs and wildcards
            if img_path.startswith(('http://', 'https://', '//')) or '*' in img_path:
                return match.group(0)
            
            # Check if the current path exists
            current_img = (md_file.parent / img_path).resolve()
            
            if current_img.exists():
                return match.group(0)  # Path is already correct
            
            # Strategy 1: Try removing all ../ prefixes
            test_path = img_path
            while test_path.startswith('../'):
                test_path = test_path[3:]
                test_img = (md_file.parent / test_path).resolve()
                if test_img.exists():
                    file_fixes += 1
                    return f'![{alt_text}]({test_path})'
            
            # Strategy 2: Try adding ../ prefixes (up to 3 levels)
            for i in range(1, 4):
                test_path = '../' * i + img_path
                test_img = (md_file.parent / test_path).resolve()
                if test_img.exists():
                    file_fixes += 1
                    return f'![{alt_text}]({test_path})'
            
            # Strategy 3: Look for the image filename anywhere in docs or source
            img_name = Path(img_path).name
            
            # Search in docs directory first
            for found_img in docs_dir.rglob(img_name):
                if found_img.is_file():
                    # Calculate relative path from md_file to found_img
                    try:
                        rel_path = found_img.relative_to(md_file.parent)
                        file_fixes += 1
                        return f'![{alt_text}]({rel_path})'
                    except ValueError:
                        # Need to go up directories
                        common = Path(*[p for p in md_file.parent.parts if p in found_img.parts])
                        up_levels = len(md_file.parent.relative_to(common).parts)
                        down_path = found_img.relative_to(common)
                        rel_path = '../' * up_levels + str(down_path)
                        file_fixes += 1
                        return f'![{alt_text}]({rel_path})'
            
            # If still not found, leave as-is
            return match.group(0)
        
        # Replace all image references
        content = re.sub(r'!\[(.*?)\]\(([^)]+)\)', fix_path, content)
        
        # Write back if changed
        if content != original_content:
            if not dry_run:
                md_file.write_text(content, encoding='utf-8')
            
            rel_path = md_file.relative_to(docs_dir)
            print(f"  {'[DRY RUN] ' if dry_run else ''}Fixed {file_fixes} image(s) in: {rel_path}")
            fixed_files += 1
            total_fixes += file_fixes
    
    print(f"\nSummary for {docs_dir.name}:")
    print(f"  Files modified: {fixed_files}")
    print(f"  Total image paths fixed: {total_fixes}")
    
    return fixed_files, total_fixes

def main():
    import argparse
    
    parser = argparse.ArgumentParser(description='Comprehensive fix for all image path issues')
    parser.add_argument('--dry-run', action='store_true', help='Show what would be changed without modifying files')
    parser.add_argument('--doc-type', choices=['user', 'developer', 'docguide', 'all'], default='all',
                       help='Which documentation type to process')
    args = parser.parse_args()
    
    print("=" * 70)
    print("Comprehensive Image Path Fixer")
    print("=" * 70)
    print("\nThis script tries multiple strategies to fix all image paths:\n")
    print("  1. Remove excessive ../ prefixes")
    print("  2. Add missing ../ prefixes")
    print("  3. Search for images by filename and calculate correct path\n")
    
    doc_types = ['user', 'developer', 'docguide'] if args.doc_type == 'all' else [args.doc_type]
    
    total_files = 0
    total_fixes = 0
    
    for doc_type in doc_types:
        docs_dir = Path(f"doc/en/{doc_type}/docs")
        source_dir = Path(f"doc/en/{doc_type}/source")
        
        if not docs_dir.exists():
            print(f"\n⚠ Skipping {doc_type} (docs directory not found: {docs_dir})")
            continue
        
        files, fixes = fix_all_image_paths(docs_dir, source_dir, dry_run=args.dry_run)
        total_files += files
        total_fixes += fixes
    
    print("\n" + "=" * 70)
    print("Overall Summary:")
    print(f"  Total files modified: {total_files}")
    print(f"  Total image paths fixed: {total_fixes}")
    print("=" * 70)
    
    if args.dry_run:
        print("\n⚠ This was a dry run. No files were modified.")
        print("Run without --dry-run to apply changes.")
    else:
        print("\n✓ All fixable image paths have been corrected!")
        print("Next steps:")
        print("  1. Verify: python quick_validation.py")
        print("  2. Test build: cd doc/en/user && mkdocs build")
        print("  3. Commit changes: git add doc/en/ && git commit -m 'Fix all remaining image paths'")

if __name__ == "__main__":
    main()
