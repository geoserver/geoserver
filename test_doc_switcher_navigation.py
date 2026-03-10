#!/usr/bin/env python3
"""
Bug Condition Exploration Test for Doc Switcher Navigation

**Property 1: Bug Condition** - Doc Switcher Navigation at Nesting Level 2+

**Validates: Requirements 1.1, 1.2, 1.3, 1.4, 2.1, 2.2, 2.3, 2.4, 2.5**

This test explores the bug condition by building the documentation and examining
the generated doc_switcher URLs at different nesting levels. The test is EXPECTED
TO FAIL on unfixed code - failure confirms the bug exists.

Bug Condition: At nesting level 2 or deeper, doc_switcher links use relative paths
that resolve from the current page location instead of from the documentation root,
resulting in incorrect URLs like `/en/user/developer/` instead of `/en/developer/`.

**CRITICAL**: This test MUST FAIL on unfixed code - failure confirms the bug exists.
**DO NOT attempt to fix the test or the code when it fails.**
**NOTE**: This test encodes the expected behavior - it will validate the fix when
it passes after implementation.
"""

import os
import sys
import yaml
import subprocess
from pathlib import Path
from typing import Dict, List, Tuple
from urllib.parse import urljoin, urlparse


def load_doc_switcher_config() -> List[Dict]:
    """Load the centralized doc_switcher.yml configuration."""
    config_path = Path('doc/themes/geoserver/doc_switcher.yml')
    
    if not config_path.exists():
        raise FileNotFoundError(f"Doc switcher config not found: {config_path}")
    
    with open(config_path, 'r') as f:
        config = yaml.safe_load(f)
    
    return config.get('doc_switcher', [])


def simulate_url_resolution(current_page_path: str, relative_url: str) -> str:
    """
    Simulate how a browser resolves a relative URL from a given page.
    
    Args:
        current_page_path: Current page path (e.g., '/en/user/introduction/')
        relative_url: Relative URL from doc_switcher (e.g., '../developer/')
    
    Returns:
        Resolved absolute path
    """
    # Ensure current_page_path ends with /
    if not current_page_path.endswith('/'):
        current_page_path += '/'
    
    # Use urljoin to simulate browser behavior
    # Add a dummy domain for URL parsing
    base_url = f"https://example.com{current_page_path}"
    resolved = urljoin(base_url, relative_url)
    
    # Extract just the path component
    parsed = urlparse(resolved)
    return parsed.path


def get_nesting_level(page_path: str) -> int:
    """
    Calculate the nesting level of a page path.
    
    Examples:
        '/en/user/' -> 1
        '/en/user/introduction/' -> 2
        '/en/user/introduction/overview/' -> 3
    """
    # Remove leading/trailing slashes and split
    parts = page_path.strip('/').split('/')
    
    # Count parts after language code (en)
    # Format: /en/{doc_type}/{section}/{subsection}/...
    if len(parts) >= 2 and parts[0] == 'en':
        # Subtract 2 for 'en' and doc_type (user/developer/docguide)
        return max(0, len(parts) - 2)
    
    return 0


def check_url_correctness(resolved_url: str, expected_doc_type: str) -> Tuple[bool, str]:
    """
    Check if a resolved URL is correct.
    
    A correct URL should:
    - Navigate to the root level of the target doc type
    - Not include parent directories from the current page
    
    Args:
        resolved_url: The resolved URL path
        expected_doc_type: Expected doc type (user, developer, docguide, swagger)
    
    Returns:
        Tuple of (is_correct, explanation)
    """
    # Expected pattern: /en/{doc_type}/ or /{version}/en/{doc_type}/
    # Should NOT contain extra path segments from current page
    
    parts = resolved_url.strip('/').split('/')
    
    if expected_doc_type == 'swagger':
        # Swagger should resolve to /en/user/api/
        if 'user' in parts and 'api' in parts:
            user_index = parts.index('user')
            api_index = parts.index('api')
            
            # Check if api comes right after user and there are no extra segments
            if api_index == user_index + 1:
                # Check for unexpected segments between 'en' and 'user'
                en_index = parts.index('en') if 'en' in parts else -1
                if en_index >= 0:
                    segments_between = parts[en_index + 1:user_index]
                    if len(segments_between) == 0:
                        return True, "Correct: Swagger URL points to /en/user/api/"
                    else:
                        return False, f"Incorrect: Swagger URL has unexpected segments between en and user: {segments_between}"
                else:
                    return True, "Correct: Swagger URL points to /user/api/"
            else:
                return False, f"Incorrect: Swagger URL has unexpected segments: {resolved_url}"
        else:
            return False, f"Incorrect: Swagger URL missing user/api: {resolved_url}"
    else:
        # For user, developer, docguide - should be at root level
        if expected_doc_type in parts:
            doc_type_index = parts.index(expected_doc_type)
            
            # Check if there are unexpected segments after doc_type
            segments_after = parts[doc_type_index + 1:]
            
            if len(segments_after) == 0:
                # Also check for unexpected segments before doc_type (after 'en')
                en_index = parts.index('en') if 'en' in parts else -1
                if en_index >= 0:
                    segments_between = parts[en_index + 1:doc_type_index]
                    if len(segments_between) == 0:
                        return True, f"Correct: URL points to root of {expected_doc_type}"
                    else:
                        return False, f"Incorrect: URL has unexpected segments between en and {expected_doc_type}: {segments_between}"
                else:
                    return True, f"Correct: URL points to root of {expected_doc_type}"
            else:
                return False, f"Incorrect: URL has unexpected segments after {expected_doc_type}: {segments_after}"
        else:
            return False, f"Incorrect: Expected doc_type '{expected_doc_type}' not found in URL: {resolved_url}"


def test_bug_condition_doc_switcher_navigation():
    """
    Property 1: Bug Condition - Doc Switcher Navigation at Nesting Level 2+
    
    **CRITICAL**: This test MUST FAIL on unfixed code - failure confirms the bug exists.
    
    This test verifies that doc_switcher URLs at nesting level 2+ navigate to incorrect
    paths due to relative path resolution from the current page location.
    
    Expected outcome on UNFIXED code: Test FAILS with counterexamples showing incorrect URLs.
    Expected outcome on FIXED code: Test PASSES (all URLs resolve correctly).
    """
    print("\n" + "="*80)
    print("Bug Condition Exploration Test - Doc Switcher Navigation")
    print("="*80)
    print("\nTesting doc_switcher URL resolution at different nesting levels...")
    print("Bug condition: Relative paths fail at nesting level 2+\n")
    
    # Load doc_switcher configuration
    try:
        doc_switcher = load_doc_switcher_config()
        print(f"Loaded {len(doc_switcher)} doc_switcher entries from config\n")
    except Exception as e:
        print(f"ERROR: Failed to load doc_switcher config: {e}")
        sys.exit(1)
    
    # Test cases: (current_page_path, nesting_level, description)
    test_cases = [
        ('/en/user/introduction/', 2, 'User Manual - Level 2'),
        ('/en/user/introduction/overview/', 3, 'User Manual - Level 3'),
        ('/en/developer/core/', 2, 'Developer Manual - Level 2'),
        ('/en/developer/core/architecture/', 3, 'Developer Manual - Level 3'),
        ('/en/docguide/contributing/', 2, 'Documentation Guide - Level 2'),
        ('/en/user/', 1, 'User Manual - Level 1 (baseline)'),
        ('/en/developer/', 1, 'Developer Manual - Level 1 (baseline)'),
    ]
    
    # Collect counterexamples
    counterexamples = []
    total_tests = 0
    failed_tests = 0
    
    print("Test Results:")
    print("-" * 80)
    
    for current_page, nesting_level, description in test_cases:
        print(f"\n{description}")
        print(f"  Current page: {current_page} (nesting level {nesting_level})")
        print(f"  Testing doc_switcher links:")
        
        for entry in doc_switcher:
            label = entry['label']
            relative_url = entry['url']
            doc_type = entry['type']
            
            # Skip external URLs
            if relative_url.startswith('http'):
                continue
            
            # Simulate browser URL resolution
            resolved_url = simulate_url_resolution(current_page, relative_url)
            
            # Check if URL is correct
            is_correct, explanation = check_url_correctness(resolved_url, doc_type)
            
            total_tests += 1
            
            if not is_correct:
                failed_tests += 1
                counterexamples.append({
                    'current_page': current_page,
                    'nesting_level': nesting_level,
                    'description': description,
                    'label': label,
                    'relative_url': relative_url,
                    'resolved_url': resolved_url,
                    'expected_doc_type': doc_type,
                    'explanation': explanation
                })
                status = "✗ FAIL"
            else:
                status = "✓ PASS"
            
            print(f"    {status} {label}: {relative_url} → {resolved_url}")
            if not is_correct:
                print(f"         {explanation}")
    
    # Report findings
    print("\n" + "="*80)
    print("Test Summary")
    print("="*80)
    print(f"Total URL resolution tests: {total_tests}")
    print(f"Passed: {total_tests - failed_tests}")
    print(f"Failed: {failed_tests}")
    print(f"\nCOUNTEREXAMPLES FOUND: {len(counterexamples)} incorrect URL resolutions\n")
    
    if counterexamples:
        print("Detailed Counterexamples:")
        print("-" * 80)
        
        # Group by nesting level
        level_2_plus = [ce for ce in counterexamples if ce['nesting_level'] >= 2]
        level_1 = [ce for ce in counterexamples if ce['nesting_level'] == 1]
        
        if level_2_plus:
            print(f"\n1. NESTING LEVEL 2+ FAILURES (Bug Condition):")
            print(f"   Found {len(level_2_plus)} failures at nesting level 2+")
            print()
            
            for i, ce in enumerate(level_2_plus[:10], 1):
                print(f"   Example {i}:")
                print(f"     From: {ce['current_page']} (level {ce['nesting_level']})")
                print(f"     Clicking: '{ce['label']}'")
                print(f"     Relative URL: {ce['relative_url']}")
                print(f"     Resolves to: {ce['resolved_url']}")
                print(f"     Problem: {ce['explanation']}")
                
                # Show expected URL
                if ce['expected_doc_type'] == 'swagger':
                    expected = '/en/user/api/'
                else:
                    expected = f"/en/{ce['expected_doc_type']}/"
                print(f"     Expected: {expected}")
                print()
            
            if len(level_2_plus) > 10:
                print(f"   ... and {len(level_2_plus) - 10} more failures at level 2+")
        
        if level_1:
            print(f"\n2. NESTING LEVEL 1 FAILURES (Unexpected - should work):")
            print(f"   Found {len(level_1)} failures at nesting level 1")
            print()
            
            for i, ce in enumerate(level_1[:5], 1):
                print(f"   Example {i}:")
                print(f"     From: {ce['current_page']}")
                print(f"     Clicking: '{ce['label']}'")
                print(f"     Resolves to: {ce['resolved_url']}")
                print(f"     Problem: {ce['explanation']}")
                print()
        
        print("\n" + "="*80)
        print("TEST RESULT: FAILED (Bug condition confirmed)")
        print("="*80)
        print(f"\nThis is the EXPECTED outcome on unfixed code.")
        print(f"The test found {len(level_2_plus)} URL resolution failures at nesting level 2+,")
        print("proving the bug exists in the doc_switcher navigation.")
        print("\nRoot cause: Relative paths (../) only go up one directory level,")
        print("so they cannot reach the documentation root from deeper nesting levels.")
        print("\nAfter implementing the fix (absolute paths), this test should PASS.")
        
        # Save counterexamples to file
        with open('doc_switcher_counterexamples.txt', 'w') as f:
            f.write("DOC SWITCHER NAVIGATION BUG - COUNTEREXAMPLES\n")
            f.write("="*80 + "\n\n")
            f.write(f"Total failures: {len(counterexamples)}\n")
            f.write(f"Failures at level 2+: {len(level_2_plus)}\n")
            f.write(f"Failures at level 1: {len(level_1)}\n\n")
            
            for ce in counterexamples:
                f.write(f"From: {ce['current_page']} (level {ce['nesting_level']})\n")
                f.write(f"Clicking: '{ce['label']}'\n")
                f.write(f"Relative URL: {ce['relative_url']}\n")
                f.write(f"Resolves to: {ce['resolved_url']}\n")
                f.write(f"Problem: {ce['explanation']}\n")
                f.write("-" * 80 + "\n")
        
        print(f"\nCounterexamples saved to: doc_switcher_counterexamples.txt")
        
        # Fail the test to indicate bug exists
        assert False, f"Found {len(counterexamples)} incorrect URL resolutions (bug confirmed)"
    
    else:
        print("TEST RESULT: PASSED (No incorrect URL resolutions found)")
        print("="*80)
        print("\nNo URL resolution failures were found.")
        print("Either the bug has been fixed, or the test needs adjustment.")
        print("\nThis outcome indicates:")
        print("  - Doc switcher URLs resolve correctly at all nesting levels")
        print("  - Absolute paths are being used instead of relative paths")


if __name__ == "__main__":
    test_bug_condition_doc_switcher_navigation()
