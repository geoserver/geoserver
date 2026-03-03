#!/usr/bin/env python3
"""
Fix anchor case sensitivity in Markdown files
Converts explicit anchor IDs to lowercase to match MkDocs behavior
"""

import re
from pathlib import Path

def fix_anchor_case(docs_dir: Path, dry_run: bool = False):
    """Convert explicit anchor IDs to lowercase"""
    
    fixed_files = 0
    total_fixes = 0
    
    print(f"\nProcessing: {docs_dir}")
    print(f"Dry run: {dry_run}\n")
    
    for md_file in sorted(docs_dir.rglob("*.md")):
        content = md_file.read_text(encoding='utf-8')
        original_content = content
        file_fixes = 0
        
        # Pattern: ## Heading {: #AnchorID }
        # MkDocs generates lowercase anchors, so explicit IDs should match
        def lowercase_anchor(match):
            nonlocal file_fixes
            heading_prefix = match.group(1)  # ## or ### etc
            heading_text = match.group(2)
            anchor_id = match.group(3)
            
            # Convert to lowercase
            anchor_id_lower = anchor_id.lower()
            
            if anchor_id != anchor_id_lower:
                file_fixes += 1
                return f'{heading_prefix} {heading_text} {{: #{anchor_id_lower} }}'
            
            return match.group(0)
        
        # Match: (##+ )(heading text)( {: #AnchorID })
        content = re.sub(
            r'(##+ )(.+?) \{: #([A-Za-z0-9_-]+) \}',
            lowercase_anchor,
            content
        )
        
        # Write back if changed
        if content != original_content:
            if not dry_run:
                md_file.write_text(content, encoding='utf-8')
            
            rel_path = md_file.relative_to(docs_dir)
            print(f"  {'[DRY RUN] ' if dry_run else ''}Fixed {file_fixes} anchor(s) in: {rel_path}")
            fixed_files += 1
            total_fixes += file_fixes
    
    print(f"\nSummary for {docs_dir.name}:")
    print(f"  Files modified: {fixed_files}")
    print(f"  Total anchors fixed: {total_fixes}")
    
    return fixed_files, total_fixes

def main():
    """Main entry point"""
    import argparse
    
    parser = argparse.ArgumentParser(description='Fix anchor case sensitivity in Markdown files')
    parser.add_argument('--dry-run', action='store_true', help='Show what would be changed without modifying files')
    parser.add_argument('--doc-type', choices=['user', 'developer', 'docguide', 'all'], default='all',
                       help='Which documentation type to process')
    args = parser.parse_args()
    
    print("=" * 60)
    print("Anchor Case Fixer for RST to Markdown Migration")
    print("=" * 60)
    
    doc_types = ['user', 'developer', 'docguide'] if args.doc_type == 'all' else [args.doc_type]
    
    total_files = 0
    total_fixes = 0
    
    for doc_type in doc_types:
        docs_dir = Path(f"doc/en/{doc_type}/docs")
        
        if not docs_dir.exists():
            print(f"\n⚠ Skipping {doc_type} (directory not found: {docs_dir})")
            continue
        
        files, fixes = fix_anchor_case(docs_dir, dry_run=args.dry_run)
        total_files += files
        total_fixes += fixes
    
    print("\n" + "=" * 60)
    print("Overall Summary:")
    print(f"  Total files modified: {total_files}")
    print(f"  Total anchors fixed: {total_fixes}")
    print("=" * 60)
    
    if args.dry_run:
        print("\n⚠ This was a dry run. No files were modified.")
        print("Run without --dry-run to apply changes.")
    else:
        print("\n✓ Anchor case has been fixed!")
        print("Next steps:")
        print("  1. Review changes: git diff doc/en/")
        print("  2. Test build: cd doc/en/user && mkdocs build")
        print("  3. Commit changes: git add doc/en/ && git commit -m 'Fix anchor case sensitivity'")

if __name__ == "__main__":
    main()
