#!/usr/bin/env python3
"""
Fix variable substitution placeholders in Markdown files
Replaces ##SUBST## placeholders with actual values
"""

import re
from pathlib import Path

# Variable substitutions for platform-specific paths
VARIABLE_SUBSTITUTIONS = {
    '##SUBST##|data_directory_win|': 'C:\\ProgramData\\GeoServer\\Data',
    '##SUBST##|data_directory_linux|': '/var/lib/geoserver_data',
    '##SUBST##|data_directory_mac|': '/Users/username/Library/Application Support/GeoServer/data_dir',
}

def fix_variable_substitution(docs_dir: Path, dry_run: bool = False):
    """Replace ##SUBST## placeholders with actual values"""
    
    fixed_files = 0
    total_fixes = 0
    
    print(f"\nProcessing: {docs_dir}")
    print(f"Dry run: {dry_run}\n")
    
    for md_file in sorted(docs_dir.rglob("*.md")):
        content = md_file.read_text(encoding='utf-8')
        original_content = content
        file_fixes = 0
        
        # Replace each substitution
        for placeholder, value in VARIABLE_SUBSTITUTIONS.items():
            if placeholder in content:
                count = content.count(placeholder)
                content = content.replace(placeholder, value)
                file_fixes += count
        
        # Write back if changed
        if content != original_content:
            if not dry_run:
                md_file.write_text(content, encoding='utf-8')
            
            rel_path = md_file.relative_to(docs_dir)
            print(f"  {'[DRY RUN] ' if dry_run else ''}Fixed {file_fixes} substitution(s) in: {rel_path}")
            fixed_files += 1
            total_fixes += file_fixes
    
    print(f"\nSummary for {docs_dir.name}:")
    print(f"  Files modified: {fixed_files}")
    print(f"  Total substitutions fixed: {total_fixes}")
    
    return fixed_files, total_fixes

def main():
    """Main entry point"""
    import argparse
    
    parser = argparse.ArgumentParser(description='Fix variable substitution placeholders in Markdown files')
    parser.add_argument('--dry-run', action='store_true', help='Show what would be changed without modifying files')
    parser.add_argument('--doc-type', choices=['user', 'developer', 'docguide', 'all'], default='all',
                       help='Which documentation type to process')
    args = parser.parse_args()
    
    print("=" * 60)
    print("Variable Substitution Fixer for RST to Markdown Migration")
    print("=" * 60)
    print("\nSubstitutions:")
    for placeholder, value in VARIABLE_SUBSTITUTIONS.items():
        print(f"  {placeholder} → {value}")
    
    doc_types = ['user', 'developer', 'docguide'] if args.doc_type == 'all' else [args.doc_type]
    
    total_files = 0
    total_fixes = 0
    
    for doc_type in doc_types:
        docs_dir = Path(f"doc/en/{doc_type}/docs")
        
        if not docs_dir.exists():
            print(f"\n⚠ Skipping {doc_type} (directory not found: {docs_dir})")
            continue
        
        files, fixes = fix_variable_substitution(docs_dir, dry_run=args.dry_run)
        total_files += files
        total_fixes += fixes
    
    print("\n" + "=" * 60)
    print("Overall Summary:")
    print(f"  Total files modified: {total_files}")
    print(f"  Total substitutions fixed: {total_fixes}")
    print("=" * 60)
    
    if args.dry_run:
        print("\n⚠ This was a dry run. No files were modified.")
        print("Run without --dry-run to apply changes.")
    else:
        print("\n✓ Variable substitutions have been fixed!")
        print("Next steps:")
        print("  1. Review changes: git diff doc/en/")
        print("  2. Test build: cd doc/en/user && mkdocs build")
        print("  3. Commit changes: git add doc/en/ && git commit -m 'Fix variable substitution placeholders'")

if __name__ == "__main__":
    main()
