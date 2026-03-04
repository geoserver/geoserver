#!/usr/bin/env python3
"""
Fix image paths in converted Markdown files
Converts absolute-from-docs-root paths to relative paths
"""

import os
import re
from pathlib import Path
from typing import Tuple

def calculate_relative_path(md_file: Path, docs_dir: Path, img_path: str) -> str:
    """Calculate relative path from markdown file to image"""
    
    # Get file's directory relative to docs root
    file_rel_dir = md_file.parent.relative_to(docs_dir)
    
    # Calculate depth (number of parent directories to traverse)
    depth = len(file_rel_dir.parts)
    
    # Build relative path
    if depth == 0:
        # File is in docs root
        return img_path
    else:
        # File is in subdirectory, need ../ for each level
        prefix = '../' * depth
        return prefix + img_path

def fix_image_paths(docs_dir: Path, dry_run: bool = False):
    """Fix image paths to be relative to file location"""
    
    fixed_files = 0
    total_fixes = 0
    
    print(f"\nProcessing: {docs_dir}")
    print(f"Dry run: {dry_run}\n")
    
    for md_file in sorted(docs_dir.rglob("*.md")):
        content = md_file.read_text(encoding='utf-8')
        original_content = content
        file_fixes = 0
        
        # Find all image references: ![alt](path)
        def fix_path(match):
            nonlocal file_fixes
            alt_text = match.group(1)
            img_path = match.group(2)
            
            # Skip external URLs
            if img_path.startswith(('http://', 'https://', '//')):
                return match.group(0)
            
            # Skip already relative paths
            if img_path.startswith(('./', '../')):
                return match.group(0)
            
            # Skip absolute paths (starting with /)
            if img_path.startswith('/'):
                return match.group(0)
            
            # Check if this looks like an absolute-from-docs-root path
            # (contains directory separators but doesn't start with ./ or ../)
            if '/' in img_path or '\\' in img_path:
                # Normalize path separators
                img_path_normalized = img_path.replace('\\', '/')
                
                # Calculate relative path
                new_path = calculate_relative_path(md_file, docs_dir, img_path_normalized)
                
                file_fixes += 1
                return f'![{alt_text}]({new_path})'
            
            # Path is already relative (no separators), leave as-is
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
    """Main entry point"""
    import argparse
    
    parser = argparse.ArgumentParser(description='Fix image paths in Markdown files')
    parser.add_argument('--dry-run', action='store_true', help='Show what would be changed without modifying files')
    parser.add_argument('--doc-type', choices=['user', 'developer', 'docguide', 'all'], default='all',
                       help='Which documentation type to process')
    args = parser.parse_args()
    
    print("=" * 60)
    print("Image Path Fixer for RST to Markdown Migration")
    print("=" * 60)
    
    doc_types = ['user', 'developer', 'docguide'] if args.doc_type == 'all' else [args.doc_type]
    
    total_files = 0
    total_fixes = 0
    
    for doc_type in doc_types:
        docs_dir = Path(f"doc/en/{doc_type}/docs")
        
        if not docs_dir.exists():
            print(f"\n⚠ Skipping {doc_type} (directory not found: {docs_dir})")
            continue
        
        files, fixes = fix_image_paths(docs_dir, dry_run=args.dry_run)
        total_files += files
        total_fixes += fixes
    
    print("\n" + "=" * 60)
    print("Overall Summary:")
    print(f"  Total files modified: {total_files}")
    print(f"  Total image paths fixed: {total_fixes}")
    print("=" * 60)
    
    if args.dry_run:
        print("\n⚠ This was a dry run. No files were modified.")
        print("Run without --dry-run to apply changes.")
    else:
        print("\n✓ Image paths have been fixed!")
        print("Next steps:")
        print("  1. Review changes: git diff doc/en/")
        print("  2. Test build: cd doc/en/user && mkdocs build")
        print("  3. Commit changes: git add doc/en/ && git commit -m 'Fix image paths'")

if __name__ == "__main__":
    main()
