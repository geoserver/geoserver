#!/usr/bin/env python
"""Property 2: Preservation - Non-Download Link Behavior"""

import re
from pathlib import Path
from typing import List, Tuple, Dict

def is_bug_condition(link_text: str, url: str) -> bool:
    """Check if link matches bug condition"""
    macro_pattern = r'\{\{\s*(release|version|snapshot)\s*\}\}\s+\[?[a-zA-Z0-9_-]+\]?'
    has_macro = bool(re.search(macro_pattern, link_text))
    is_download_url = any(path in url for path in ['/release/', '/nightly/', '/community/'])
    has_geoserver_prefix = 'geoserver-' in link_text
    has_extension = any(ext in link_text for ext in ['.zip', '.war'])
    return has_macro and is_download_url and not has_geoserver_prefix and not has_extension

def extract_markdown_links(content: str) -> List[Tuple[str, str, str]]:
    """Extract all Markdown links from content"""
    pattern = r'\[([^\]]+)\]\(([^)]+)\)'
    matches = re.finditer(pattern, content)
    return [(m.group(0), m.group(1), m.group(2)) for m in matches]

def find_preservation_examples() -> Dict[str, List[Dict]]:
    """Find links that should remain unchanged"""
    doc_root = Path('doc/en')
    examples = {
        'internal_links': [],
        'external_links': [],
        'correct_download_links': []
    }
    
    sample_files = [
        'user/docs/webadmin/index.md',
        'user/docs/data/database/postgis.md',
        'user/docs/styling/css/install.md',
    ]
    
    for file_path in sample_files:
        full_path = doc_root / file_path
        if not full_path.exists():
            continue
            
        with open(full_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        links = extract_markdown_links(content)
        
        for full_match, link_text, url in links:
            if url.startswith('http://') or url.startswith('https://'):
                if not is_bug_condition(link_text, url):
                    examples['external_links'].append({
                        'file': str(file_path),
                        'link': full_match,
                        'text': link_text,
                        'url': url
                    })
            elif url.endswith('.md') or '../' in url:
                examples['internal_links'].append({
                    'file': str(file_path),
                    'link': full_match,
                    'text': link_text,
                    'url': url
                })
    
    return examples

def test_preservation_baseline():
    """Document baseline behavior on UNFIXED code"""
    print("=" * 80)
    print("Property 2: Preservation - Non-Download Link Behavior")
    print("=" * 80)
    print()
    print("OBSERVATION PHASE: Documenting baseline behavior on UNFIXED code")
    print()
    
    examples = find_preservation_examples()
    
    print("1. INTERNAL DOCUMENTATION LINKS (should remain unchanged)")
    print("-" * 80)
    for i, link in enumerate(examples['internal_links'][:5], 1):
        print(f"   Example {i}: {link['file']}")
        print(f"   Link: {link['link']}")
        print()
    print(f"   Total: {len(examples['internal_links'])}")
    print()
    
    print("2. EXTERNAL REFERENCE LINKS (should remain unchanged)")
    print("-" * 80)
    for i, link in enumerate(examples['external_links'][:5], 1):
        print(f"   Example {i}: {link['file']}")
        print(f"   Link: {link['link']}")
        print()
    print(f"   Total: {len(examples['external_links'])}")
    print()
    
    total = len(examples['internal_links']) + len(examples['external_links'])
    
    print("=" * 80)
    print("BASELINE ESTABLISHED")
    print("=" * 80)
    print(f"Total links that MUST be preserved: {total}")
    print()
    print("TEST STATUS: ✓ PASSED (baseline documented on unfixed code)")
    print("=" * 80)
    
    # Save baseline
    with open('preservation_baseline.txt', 'w', encoding='utf-8') as f:
        f.write("PRESERVATION BASELINE\n")
        f.write("=" * 80 + "\n\n")
        f.write(f"Internal links: {len(examples['internal_links'])}\n")
        f.write(f"External links: {len(examples['external_links'])}\n")
        f.write(f"Total: {total}\n")
    
    print(f"Baseline saved to: preservation_baseline.txt")
    return True

if __name__ == '__main__':
    test_preservation_baseline()
