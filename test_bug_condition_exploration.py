"""
Bug Condition Exploration Test for Download Links

**Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.5**

This test explores the bug condition by examining actual documentation files
to find download links with incomplete text. The test is EXPECTED TO FAIL on
unfixed code - failure confirms the bug exists.

Bug Condition: Links matching pattern `{{ (release|version|snapshot) }} [plugin-name](URL)`
where URL contains `/release/`, `/nightly/`, or `/community/` paths, and link text
does NOT contain "geoserver-" prefix or file extensions (.zip, .war).
"""

import re
import os
from pathlib import Path
from typing import List, Tuple


def find_markdown_files(root_dir: str = "doc/en/user/docs") -> List[Path]:
    """Find all markdown files in the documentation directory."""
    root = Path(root_dir)
    if not root.exists():
        return []
    return list(root.rglob("*.md"))


def extract_download_links(file_path: Path) -> List[Tuple[int, str, str, str]]:
    """
    Extract download links from a markdown file.
    
    Returns list of tuples: (line_number, full_match, link_text, url)
    """
    # Pattern to match: {{ macro }} [text](url)
    pattern = r'\{\{\s*(release|version|snapshot)\s*\}\}\s*\[([^\]]+)\]\(([^)]+)\)'
    
    results = []
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            for line_num, line in enumerate(f, start=1):
                matches = re.finditer(pattern, line)
                for match in matches:
                    macro = match.group(1)
                    link_text = match.group(2)
                    url = match.group(3)
                    full_match = match.group(0)
                    
                    # Only include if URL contains download paths
                    if any(path in url for path in ['/release/', '/nightly/', '/community/', 'sourceforge.net', 'build.geoserver.org']):
                        results.append((line_num, full_match, link_text, url))
    except Exception as e:
        print(f"Error reading {file_path}: {e}")
    
    return results


def is_buggy_link(link_text: str, url: str) -> bool:
    """
    Check if a link exhibits the bug condition.
    
    Bug condition: Link text does NOT contain "geoserver-" prefix or file extensions.
    Expected behavior: Link text SHOULD contain full filename like 
    "geoserver-{{ release }}-plugin-name-plugin.zip"
    """
    # Check if link text contains indicators of complete filename
    has_geoserver_prefix = 'geoserver-' in link_text.lower()
    has_file_extension = any(ext in link_text.lower() for ext in ['.zip', '.war'])
    
    # If it's missing both, it's buggy
    return not (has_geoserver_prefix or has_file_extension)


def test_bug_condition_download_links_display_incomplete_text():
    """
    Property 1: Bug Condition - Download Links Display Incomplete Text
    
    **CRITICAL**: This test MUST FAIL on unfixed code - failure confirms the bug exists.
    
    This test verifies that documentation files contain download links with incomplete
    text (missing "geoserver-" prefix and file extensions). Finding such links proves
    the bug exists.
    
    Expected outcome on UNFIXED code: Test FAILS with counterexamples showing buggy links.
    Expected outcome on FIXED code: Test PASSES (no buggy links found).
    """
    print("\n" + "="*80)
    print("Bug Condition Exploration Test")
    print("="*80)
    print("\nSearching for download links with incomplete text...")
    print("Bug condition: Link text missing 'geoserver-' prefix and file extensions\n")
    
    # Find all markdown files
    md_files = find_markdown_files()
    print(f"Found {len(md_files)} markdown files to examine\n")
    
    # Collect all buggy links
    buggy_links = []
    
    for file_path in md_files:
        links = extract_download_links(file_path)
        for line_num, full_match, link_text, url in links:
            if is_buggy_link(link_text, url):
                buggy_links.append({
                    'file': str(file_path),
                    'line': line_num,
                    'link_text': link_text,
                    'url': url,
                    'full_match': full_match
                })
    
    # Report findings
    print(f"COUNTEREXAMPLES FOUND: {len(buggy_links)} buggy download links\n")
    
    if buggy_links:
        print("Detailed counterexamples (showing first 10):")
        print("-" * 80)
        
        for i, bug in enumerate(buggy_links[:10], 1):
            print(f"\n{i}. File: {bug['file']}")
            print(f"   Line: {bug['line']}")
            print(f"   Current link text: '{bug['link_text']}'")
            print(f"   URL: {bug['url']}")
            print(f"   Full match: {bug['full_match']}")
            
            # Suggest expected format
            if '.war' in bug['url']:
                expected = "geoserver-{{ release }}.war"
            elif 'bin.zip' in bug['url'] or '-bin-' in bug['url']:
                expected = "geoserver-{{ release }}-bin.zip"
            else:
                plugin_name = bug['link_text'].strip()
                expected = f"geoserver-{{{{ release }}}}-{plugin_name}-plugin.zip"
            
            print(f"   Expected format: '{expected}'")
        
        if len(buggy_links) > 10:
            print(f"\n... and {len(buggy_links) - 10} more buggy links")
        
        print("\n" + "="*80)
        print("TEST RESULT: FAILED (Bug condition confirmed)")
        print("="*80)
        print(f"\nThis is the EXPECTED outcome on unfixed code.")
        print(f"The test found {len(buggy_links)} download links with incomplete text,")
        print("proving the bug exists in the documentation.")
        print("\nAfter implementing the fix, this test should PASS (no buggy links found).")
        
        # Fail the test to indicate bug exists
        assert False, f"Found {len(buggy_links)} download links with incomplete text (bug confirmed)"
    
    else:
        print("TEST RESULT: PASSED (No buggy links found)")
        print("="*80)
        print("\nNo download links with incomplete text were found.")
        print("Either the bug has been fixed, or the test needs adjustment.")


if __name__ == "__main__":
    test_bug_condition_download_links_display_incomplete_text()
