#!/usr/bin/env python3
"""
Fix all remaining broken links in the documentation.

This script fixes:
1. Links to source code files outside docs directory (convert to GitHub links)
2. Links to license files outside docs directory (convert to GitHub links)
3. Broken placeholder links (-broken.md)
4. Missing files that should exist
"""

import re
import sys
from pathlib import Path

# GitHub repository base URL for source code links
GITHUB_BASE = "https://github.com/geoserver/geoserver/blob/main"

def fix_source_code_links(content, filepath):
    """Convert source code file links to GitHub URLs."""
    changes = []
    
    # Pattern: ../../../../src/... or ../../../src/...
    pattern = r'\[([^\]]+)\]\((\.\./)+src/([^\)]+)\)'
    
    def replace_src_link(match):
        link_text = match.group(1)
        path = match.group(3)
        github_url = f"{GITHUB_BASE}/src/{path}"
        changes.append(f"  - Converted source link: src/{path}")
        return f'[{link_text}]({github_url})'
    
    content = re.sub(pattern, replace_src_link, content)
    return content, changes

def fix_license_links(content, filepath):
    """Convert license file links to GitHub URLs."""
    changes = []
    
    # Pattern: ../../../../licenses/...
    pattern = r'\[([^\]]+)\]\((\.\./)+licenses/([^\)]+)\)'
    
    def replace_license_link(match):
        link_text = match.group(1)
        filename = match.group(3)
        github_url = f"{GITHUB_BASE}/licenses/{filename}"
        changes.append(f"  - Converted license link: licenses/{filename}")
        return f'[{link_text}]({github_url})'
    
    content = re.sub(pattern, replace_license_link, content)
    return content, changes

def fix_broken_placeholder_links(content, filepath):
    """Remove or fix broken placeholder links."""
    changes = []
    
    # Pattern: ../-broken.md or ./-broken.md
    if '-broken.md' in content:
        # Comment out broken links
        content = re.sub(
            r'\[([^\]]+)\]\([^\)]*-broken\.md\)',
            r'<!-- BROKEN LINK: \1 -->',
            content
        )
        changes.append("  - Commented out broken placeholder links")
    
    return content, changes

def fix_missing_docguide_link(content, filepath):
    """Fix link to docguide that crosses manual boundaries."""
    changes = []
    
    # Pattern: ../docguide/quickfix.md (from user manual to docguide)
    if '../docguide/quickfix.md' in content:
        # Convert to external link to deployed docs
        content = content.replace(
            '../docguide/quickfix.md',
            'https://docs.geoserver.org/latest/en/docguide/quickfix.html'
        )
        changes.append("  - Fixed cross-manual link to docguide")
    
    return content, changes

def process_file(filepath):
    """Process a single file and apply all fixes."""
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
        
        original_content = content
        all_changes = []
        
        # Apply all fixes
        content, changes = fix_source_code_links(content, filepath)
        all_changes.extend(changes)
        
        content, changes = fix_license_links(content, filepath)
        all_changes.extend(changes)
        
        content, changes = fix_broken_placeholder_links(content, filepath)
        all_changes.extend(changes)
        
        content, changes = fix_missing_docguide_link(content, filepath)
        all_changes.extend(changes)
        
        if content != original_content:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(content)
            print(f"✓ {filepath}")
            for change in all_changes:
                print(change)
            return 1
        return 0
    except Exception as e:
        print(f"✗ Error processing {filepath}: {e}", file=sys.stderr)
        return 0

def main():
    """Main function to process all documentation files."""
    docs_dirs = [
        Path('doc/en/user/docs'),
        Path('doc/en/developer/docs'),
        Path('doc/en/docguide/docs')
    ]
    
    fixed_count = 0
    for docs_dir in docs_dirs:
        if not docs_dir.exists():
            continue
        
        md_files = list(docs_dir.rglob('*.md'))
        print(f"\nProcessing {len(md_files)} files in {docs_dir}...")
        
        for md_file in sorted(md_files):
            fixed_count += process_file(md_file)
    
    print(f"\n✓ Fixed {fixed_count} files with broken links")
    return 0

if __name__ == '__main__':
    sys.exit(main())
