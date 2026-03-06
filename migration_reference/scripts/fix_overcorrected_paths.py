#!/usr/bin/env python3
"""
Fix image paths that were over-corrected by the first fix script.
Some paths had ../ added when they shouldn't have.
"""

import re
from pathlib import Path

def fix_overcorrected_paths(docs_dir: Path, dry_run: bool = False):
    """Fix image paths that have too many ../ prefixes"""
    
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
            
            # Skip external URLs
            if img_path.startswith(('http://', 'https://', '//')):
                return match.group(0)
            
            # Skip wildcards
            if '*' in img_path:
                return match.group(0)
            
            # Check if the current path doesn't exist
            current_img = (md_file.parent / img_path).resolve()
            
            if not current_img.exists():
                # Try removing one ../ level
                if img_path.startswith('../'):
                    new_path = img_path[3:]  # Remove '../'
                    new_img = (md_file.parent / new_path).resolve()
                    
                    if new_img.exists():
                        file_fixes += 1
                        return f'![{alt_text}]({new_path})'
            
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
    
    parser = argparse.ArgumentParser(description='Fix over-corrected image paths')
    parser.add_argument('--dry-run', action='store_true', help='Show what would be changed without modifying files')
    parser.add_argument('--doc-type', choices=['user', 'developer', 'docguide', 'all'], default='all',
                       help='Which documentation type to process')
    args = parser.parse_args()
    
    print("=" * 70)
    print("Fix Over-Corrected Image Paths")
    print("=" * 70)
    print("\nThis script removes unnecessary ../ prefixes from image paths")
    print("that were added by the first fix but shouldn't have been.\n")
    
    doc_types = ['user', 'developer', 'docguide'] if args.doc_type == 'all' else [args.doc_type]
    
    total_files = 0
    total_fixes = 0
    
    for doc_type in doc_types:
        docs_dir = Path(f"doc/en/{doc_type}/docs")
        
        if not docs_dir.exists():
            print(f"\n⚠ Skipping {doc_type} (directory not found: {docs_dir})")
            continue
        
        files, fixes = fix_overcorrected_paths(docs_dir, dry_run=args.dry_run)
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
        print("\n✓ Over-corrected image paths have been fixed!")
        print("Next steps:")
        print("  1. Verify: python quick_validation.py")
        print("  2. Test build: cd doc/en/user && mkdocs build")
        print("  3. Commit changes: git add doc/en/ && git commit -m 'Fix over-corrected image paths'")

if __name__ == "__main__":
    main()
