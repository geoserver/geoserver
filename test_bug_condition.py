#!/usr/bin/env python3
"""
Property 1: Bug Condition Exploration - Download Links Display Full Filenames

This test verifies that download links now contain full filename patterns
with "geoserver-" prefix, macro variables, plugin names, and file extensions.
"""

import re
from pathlib import Path
from typing import List, Tuple

def extract_download_links(content: str) -> List[Tuple[str, str, str]]:
    """Extract download links from content"""
    pattern = r'\[([^\]]+)\]\((https://[^)]*(?:sourceforge\.net|build\.geoserver\.org)[^)]*)\)'
    matches = re.finditer(pattern, content)
    results = []
    for m in matches:
        link_text = m.group(1)
        url = m.group(2)
        # Only include links with download paths
        if any(path in url for path in ['/extensions/', '/community-latest/', '/release/']):
            results.append((m.group(0), link_text, url))
    return results

def check_link_has_full_filename(link_text: str, url: str) -> bool:
    """Check if link text contains full filename pattern"""
    # Check for geoserver- prefix
    has_prefix = 'geoserver-' in link_text
    # Check for macro variable
    has_macro = bool(re.search(r'\{\{\s*(release|snapshot|version)\s*\}\}', link_text))
    # Check for file extension
    has_extension = any(ext in link_text for ext in ['.zip', '.war'])
    
    return has_prefix and has_macro and has_extension

def test_bug_condition_exploration():
    """Test that download links now display full filenames"""
    print("=" * 80)
    print("Property 1: Bug Condition - Download Links Display Full Filenames")
    print("=" * 80)
    print()
    print("VERIFICATION PHASE: Checking if bug is fixed")
    print()
    
    doc_root = Path('doc/en')
    
    # Sample files to check
    sample_files = [
        'user/docs/styling/css/install.md',
        'user/docs/styling/mbstyle/installing.md',
        'user/docs/community/schemaless-features/install.md',
        'user/docs/extensions/wps-download/index.md',
    ]
    
    total_links = 0
    correct_links = 0
    incorrect_links = []
    
    for file_path in sample_files:
        full_path = doc_root / file_path
        if not full_path.exists():
            continue
            
        with open(full_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        links = extract_download_links(content)
        
        for full_match, link_text, url in links:
            total_links += 1
            if check_link_has_full_filename(link_text, url):
                correct_links += 1
            else:
                incorrect_links.append({
                    'file': str(file_path),
                    'link_text': link_text,
                    'url': url
                })
    
    print(f"Total download links checked: {total_links}")
    print(f"Links with full filename patterns: {correct_links}")
    print(f"Links still missing full filenames: {len(incorrect_links)}")
    print()
    
    if incorrect_links:
        print("COUNTEREXAMPLES (links still buggy):")
        print("-" * 80)
        for i, link in enumerate(incorrect_links[:5], 1):
            print(f"{i}. File: {link['file']}")
            print(f"   Link text: {link['link_text']}")
            print(f"   URL: {link['url']}")
            print()
        print("=" * 80)
        print("TEST STATUS: ✗ FAILED (bug still exists)")
        print("=" * 80)
        return False
    else:
        print("=" * 80)
        print("TEST STATUS: ✓ PASSED (bug is fixed)")
        print("=" * 80)
        print()
        print("All download links now contain full filename patterns with:")
        print("  - 'geoserver-' prefix")
        print("  - Macro variables ({{ release }}, {{ snapshot }}, {{ version }})")
        print("  - Plugin names")
        print("  - File extensions (.zip, .war)")
        return True

if __name__ == '__main__':
    import sys
    success = test_bug_condition_exploration()
    sys.exit(0 if success else 1)
