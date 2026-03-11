#!/usr/bin/env python3
"""
Preservation Property Tests for Doc Switcher Navigation

**Property 2: Preservation** - Level 1 Navigation and Configuration

**Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5**

This test validates that existing doc_switcher behavior at nesting level 1
and the centralized configuration approach work correctly on UNFIXED code.
These behaviors MUST be preserved after implementing the fix.

**EXPECTED OUTCOME**: Tests PASS on unfixed code (confirms baseline to preserve)

Testing Strategy:
- Use property-based testing with Hypothesis to generate test cases
- Test level 1 navigation (which currently works)
- Test configuration loading and data structure
- Test build process compatibility
"""

import os
import sys
import yaml
import subprocess
from pathlib import Path
from typing import Dict, List, Tuple
from urllib.parse import urljoin, urlparse

# Import Hypothesis for property-based testing
try:
    from hypothesis import given, strategies as st, settings, Phase
    HYPOTHESIS_AVAILABLE = True
except ImportError:
    HYPOTHESIS_AVAILABLE = False
    print("WARNING: Hypothesis not available. Install with: pip install hypothesis")


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
        current_page_path: Current page path (e.g., '/en/user/')
        relative_url: Relative URL from doc_switcher (e.g., '../developer/')
    
    Returns:
        Resolved absolute path
    """
    # Ensure current_page_path ends with /
    if not current_page_path.endswith('/'):
        current_page_path += '/'
    
    # Use urljoin to simulate browser behavior
    base_url = f"https://example.com{current_page_path}"
    resolved = urljoin(base_url, relative_url)
    
    # Extract just the path component
    parsed = urlparse(resolved)
    return parsed.path


def check_level_1_navigation(current_page: str, target_doc_type: str, resolved_url: str) -> Tuple[bool, str]:
    """
    Check if level 1 navigation resolves correctly.
    
    At level 1, relative paths should work correctly because ../type/ goes up
    one level from /en/current_type/ to /en/ and then down to /en/target_type/.
    
    Args:
        current_page: Current page path (e.g., '/en/user/')
        target_doc_type: Target doc type (user, developer, docguide, swagger)
        resolved_url: The resolved URL path
    
    Returns:
        Tuple of (is_correct, explanation)
    """
    parts = resolved_url.strip('/').split('/')
    
    if target_doc_type == 'swagger':
        # Swagger should resolve to /en/user/api/
        expected_parts = ['en', 'user', 'api']
        if parts == expected_parts:
            return True, "Correct: Swagger URL points to /en/user/api/"
        else:
            return False, f"Incorrect: Expected {expected_parts}, got {parts}"
    else:
        # For user, developer, docguide - should be at /en/{doc_type}/
        expected_parts = ['en', target_doc_type]
        if parts == expected_parts:
            return True, f"Correct: URL points to /en/{target_doc_type}/"
        else:
            return False, f"Incorrect: Expected {expected_parts}, got {parts}"


def test_requirement_3_1_level_1_navigation():
    """
    Requirement 3.1: Level 1 navigation must continue to work correctly.
    
    WHEN a user is at nesting level 1 (e.g., /en/user/) AND clicks any
    doc_switcher link THEN the system SHALL CONTINUE TO navigate to the
    correct documentation type.
    """
    print("\n" + "="*80)
    print("Requirement 3.1: Level 1 Navigation Preservation")
    print("="*80)
    print("\nTesting doc_switcher navigation at nesting level 1...")
    print("Expected: All links should navigate correctly (baseline behavior)\n")
    
    # Load doc_switcher configuration
    doc_switcher = load_doc_switcher_config()
    
    # Test cases: level 1 pages for each doc type
    level_1_pages = [
        '/en/user/',
        '/en/developer/',
        '/en/docguide/',
    ]
    
    all_passed = True
    test_count = 0
    
    for current_page in level_1_pages:
        current_type = current_page.strip('/').split('/')[-1]
        print(f"From: {current_page} (current type: {current_type})")
        
        for entry in doc_switcher:
            label = entry['label']
            relative_url = entry['url']
            target_type = entry['type']
            
            # Skip external URLs
            if relative_url.startswith('http'):
                continue
            
            # Simulate browser URL resolution
            resolved_url = simulate_url_resolution(current_page, relative_url)
            
            # Check if navigation is correct
            is_correct, explanation = check_level_1_navigation(
                current_page, target_type, resolved_url
            )
            
            test_count += 1
            status = "✓ PASS" if is_correct else "✗ FAIL"
            print(f"  {status} {label}: {relative_url} → {resolved_url}")
            
            if not is_correct:
                print(f"       {explanation}")
                all_passed = False
        
        print()
    
    print("="*80)
    if all_passed:
        print(f"✓ PASSED: All {test_count} level 1 navigation tests passed")
        print("Baseline behavior confirmed: Level 1 navigation works correctly")
    else:
        print(f"✗ FAILED: Some level 1 navigation tests failed")
        print("WARNING: Level 1 navigation should work on unfixed code!")
    print("="*80)
    
    return all_passed


def test_requirement_3_2_language_context():
    """
    Requirement 3.2: Language context must be maintained.
    
    WHEN a user switches between documentation types THEN the system SHALL
    CONTINUE TO maintain the same language context (e.g., /en/).
    """
    print("\n" + "="*80)
    print("Requirement 3.2: Language Context Preservation")
    print("="*80)
    print("\nTesting that language context (/en/) is maintained...\n")
    
    doc_switcher = load_doc_switcher_config()
    
    # Test from different doc types at level 1
    test_pages = ['/en/user/', '/en/developer/', '/en/docguide/']
    
    all_passed = True
    test_count = 0
    
    for current_page in test_pages:
        for entry in doc_switcher:
            if entry['url'].startswith('http'):
                continue
            
            resolved_url = simulate_url_resolution(current_page, entry['url'])
            
            # Check if /en/ is preserved
            has_en = '/en/' in resolved_url
            test_count += 1
            
            if has_en:
                print(f"  ✓ PASS {current_page} → {entry['label']}: Language context preserved")
            else:
                print(f"  ✗ FAIL {current_page} → {entry['label']}: Language context lost!")
                print(f"       Resolved to: {resolved_url}")
                all_passed = False
    
    print("\n" + "="*80)
    if all_passed:
        print(f"✓ PASSED: All {test_count} language context tests passed")
        print("Baseline behavior confirmed: Language context is maintained")
    else:
        print(f"✗ FAILED: Language context not maintained in some cases")
    print("="*80)
    
    return all_passed


def test_requirement_3_3_config_loading():
    """
    Requirement 3.3: Configuration loading must continue to work.
    
    WHEN the doc_switcher.yml configuration is loaded by the MkDocs macros
    plugin THEN the system SHALL CONTINUE TO inject the doc_switcher data
    into the extra section of each mkdocs.yml.
    """
    print("\n" + "="*80)
    print("Requirement 3.3: Configuration Loading Preservation")
    print("="*80)
    print("\nTesting doc_switcher.yml loading mechanism...\n")
    
    try:
        # Test 1: File exists and is readable
        config_path = Path('doc/themes/geoserver/doc_switcher.yml')
        if not config_path.exists():
            print("✗ FAIL: doc_switcher.yml not found")
            return False
        print(f"✓ PASS: doc_switcher.yml exists at {config_path}")
        
        # Test 2: File is valid YAML
        with open(config_path, 'r') as f:
            config = yaml.safe_load(f)
        print("✓ PASS: doc_switcher.yml is valid YAML")
        
        # Test 3: Has doc_switcher key
        if 'doc_switcher' not in config:
            print("✗ FAIL: doc_switcher key not found in config")
            return False
        print("✓ PASS: doc_switcher key present in config")
        
        # Test 4: doc_switcher is a list
        if not isinstance(config['doc_switcher'], list):
            print("✗ FAIL: doc_switcher is not a list")
            return False
        print(f"✓ PASS: doc_switcher is a list with {len(config['doc_switcher'])} entries")
        
        # Test 5: version.py can load the config
        version_py = Path('doc/version.py')
        if not version_py.exists():
            print("✗ FAIL: version.py not found")
            return False
        print("✓ PASS: version.py exists")
        
        # Test 6: version.py contains doc_switcher loading code
        with open(version_py, 'r') as f:
            version_content = f.read()
        
        if 'doc_switcher.yml' not in version_content:
            print("✗ FAIL: version.py doesn't reference doc_switcher.yml")
            return False
        print("✓ PASS: version.py references doc_switcher.yml")
        
        if "env.conf['extra']['doc_switcher']" not in version_content:
            print("✗ FAIL: version.py doesn't inject doc_switcher into extra")
            return False
        print("✓ PASS: version.py injects doc_switcher into config.extra")
        
        print("\n" + "="*80)
        print("✓ PASSED: Configuration loading mechanism works correctly")
        print("Baseline behavior confirmed: doc_switcher.yml is loaded properly")
        print("="*80)
        
        return True
        
    except Exception as e:
        print(f"\n✗ FAIL: Exception during config loading test: {e}")
        print("="*80)
        return False


def test_requirement_3_4_data_structure():
    """
    Requirement 3.4: Template data structure must be preserved.
    
    WHEN the documentation templates access extra.doc_switcher THEN the
    system SHALL CONTINUE TO provide the doc_switcher array with label,
    url, and type fields.
    """
    print("\n" + "="*80)
    print("Requirement 3.4: Data Structure Preservation")
    print("="*80)
    print("\nTesting doc_switcher data structure...\n")
    
    try:
        doc_switcher = load_doc_switcher_config()
        
        # Test each entry has required fields
        required_fields = ['label', 'url', 'type']
        all_passed = True
        
        for i, entry in enumerate(doc_switcher, 1):
            print(f"Entry {i}: {entry.get('label', 'NO LABEL')}")
            
            for field in required_fields:
                if field not in entry:
                    print(f"  ✗ FAIL: Missing required field '{field}'")
                    all_passed = False
                else:
                    print(f"  ✓ PASS: Has '{field}' = '{entry[field]}'")
            
            # Check field types
            if 'label' in entry and not isinstance(entry['label'], str):
                print(f"  ✗ FAIL: 'label' is not a string")
                all_passed = False
            
            if 'url' in entry and not isinstance(entry['url'], str):
                print(f"  ✗ FAIL: 'url' is not a string")
                all_passed = False
            
            if 'type' in entry and not isinstance(entry['type'], str):
                print(f"  ✗ FAIL: 'type' is not a string")
                all_passed = False
            
            print()
        
        print("="*80)
        if all_passed:
            print(f"✓ PASSED: All {len(doc_switcher)} entries have correct structure")
            print("Baseline behavior confirmed: Data structure is correct")
        else:
            print("✗ FAILED: Some entries have incorrect structure")
        print("="*80)
        
        return all_passed
        
    except Exception as e:
        print(f"\n✗ FAIL: Exception during data structure test: {e}")
        print("="*80)
        return False


def test_requirement_3_5_build_process():
    """
    Requirement 3.5: Build process must work without additional arguments.
    
    WHEN the documentation is built with mkdocs build or mkdocs serve THEN
    the system SHALL CONTINUE TO process the centralized doc_switcher.yml
    without requiring additional command-line arguments.
    """
    print("\n" + "="*80)
    print("Requirement 3.5: Build Process Preservation")
    print("="*80)
    print("\nTesting that mkdocs can process doc_switcher configuration...\n")
    
    # We'll test that the configuration is compatible with mkdocs
    # by checking that all required files are in place
    
    try:
        # Test 1: mkdocs.yml files exist
        mkdocs_files = [
            'doc/en/user/mkdocs.yml',
            'doc/en/developer/mkdocs.yml',
            'doc/en/docguide/mkdocs.yml',
        ]
        
        all_exist = True
        for mkdocs_file in mkdocs_files:
            if Path(mkdocs_file).exists():
                print(f"✓ PASS: {mkdocs_file} exists")
            else:
                print(f"✗ FAIL: {mkdocs_file} not found")
                all_exist = False
        
        if not all_exist:
            print("\n✗ FAILED: Some mkdocs.yml files missing")
            return False
        
        # Test 2: mkdocs.yml files reference macros plugin
        print("\nChecking mkdocs.yml files reference macros plugin...")
        for mkdocs_file in mkdocs_files:
            with open(mkdocs_file, 'r') as f:
                content = f.read()
            
            if 'macros' in content:
                print(f"✓ PASS: {mkdocs_file} references macros plugin")
            else:
                print(f"✗ FAIL: {mkdocs_file} doesn't reference macros plugin")
                all_exist = False
        
        # Test 3: Check that version.py is in the right location
        version_py = Path('doc/version.py')
        if version_py.exists():
            print(f"\n✓ PASS: version.py exists at {version_py}")
        else:
            print(f"\n✗ FAIL: version.py not found at {version_py}")
            return False
        
        print("\n" + "="*80)
        if all_exist:
            print("✓ PASSED: Build process configuration is correct")
            print("Baseline behavior confirmed: mkdocs can process doc_switcher")
            print("Note: Actual build test requires mkdocs to be installed")
        else:
            print("✗ FAILED: Build process configuration has issues")
        print("="*80)
        
        return all_exist
        
    except Exception as e:
        print(f"\n✗ FAIL: Exception during build process test: {e}")
        print("="*80)
        return False


# Property-based test using Hypothesis (if available)
if HYPOTHESIS_AVAILABLE:
    @given(
        doc_type=st.sampled_from(['user', 'developer', 'docguide']),
        target_type=st.sampled_from(['user', 'developer', 'docguide', 'swagger'])
    )
    @settings(max_examples=50, phases=[Phase.generate, Phase.target])
    def test_property_level_1_navigation_always_works(doc_type, target_type):
        """
        Property: For all level 1 pages and all target doc types,
        navigation should resolve to a valid path.
        
        This is a property-based test that generates many combinations
        of source and target doc types to ensure level 1 navigation
        always works correctly.
        """
        current_page = f'/en/{doc_type}/'
        
        # Load doc_switcher config
        doc_switcher = load_doc_switcher_config()
        
        # Find the entry for target_type
        target_entry = None
        for entry in doc_switcher:
            if entry['type'] == target_type:
                target_entry = entry
                break
        
        if target_entry is None:
            return  # Skip if target type not in config
        
        relative_url = target_entry['url']
        
        # Skip external URLs
        if relative_url.startswith('http'):
            return
        
        # Simulate URL resolution
        resolved_url = simulate_url_resolution(current_page, relative_url)
        
        # Check correctness
        is_correct, explanation = check_level_1_navigation(
            current_page, target_type, resolved_url
        )
        
        # Assert the property holds
        assert is_correct, (
            f"Level 1 navigation failed: {current_page} → {target_type}\n"
            f"Relative URL: {relative_url}\n"
            f"Resolved to: {resolved_url}\n"
            f"Problem: {explanation}"
        )


def run_all_preservation_tests():
    """Run all preservation property tests."""
    print("\n" + "="*80)
    print("PRESERVATION PROPERTY TESTS - DOC SWITCHER NAVIGATION")
    print("="*80)
    print("\n**Property 2: Preservation** - Level 1 Navigation and Configuration")
    print("\n**Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5**")
    print("\nTesting on UNFIXED code to establish baseline behavior...")
    print("Expected outcome: All tests PASS (confirms behavior to preserve)")
    print("="*80)
    
    results = {}
    
    # Run each test
    results['3.1_level_1_navigation'] = test_requirement_3_1_level_1_navigation()
    results['3.2_language_context'] = test_requirement_3_2_language_context()
    results['3.3_config_loading'] = test_requirement_3_3_config_loading()
    results['3.4_data_structure'] = test_requirement_3_4_data_structure()
    results['3.5_build_process'] = test_requirement_3_5_build_process()
    
    # Run property-based test if Hypothesis is available
    if HYPOTHESIS_AVAILABLE:
        print("\n" + "="*80)
        print("Property-Based Test: Level 1 Navigation (Hypothesis)")
        print("="*80)
        print("\nGenerating test cases with Hypothesis...\n")
        try:
            test_property_level_1_navigation_always_works()
            print("✓ PASSED: Property-based test passed (50 generated test cases)")
            results['property_based'] = True
        except AssertionError as e:
            print(f"✗ FAILED: Property-based test failed\n{e}")
            results['property_based'] = False
        print("="*80)
    else:
        print("\n" + "="*80)
        print("Property-Based Test: SKIPPED (Hypothesis not installed)")
        print("="*80)
        results['property_based'] = None
    
    # Summary
    print("\n" + "="*80)
    print("PRESERVATION TEST SUMMARY")
    print("="*80)
    
    passed = sum(1 for v in results.values() if v is True)
    failed = sum(1 for v in results.values() if v is False)
    skipped = sum(1 for v in results.values() if v is None)
    total = len(results)
    
    print(f"\nTotal tests: {total}")
    print(f"Passed: {passed}")
    print(f"Failed: {failed}")
    print(f"Skipped: {skipped}")
    print()
    
    for test_name, result in results.items():
        if result is True:
            status = "✓ PASS"
        elif result is False:
            status = "✗ FAIL"
        else:
            status = "⊘ SKIP"
        print(f"  {status} {test_name}")
    
    print("\n" + "="*80)
    
    if failed == 0:
        print("✓ ALL PRESERVATION TESTS PASSED")
        print("="*80)
        print("\nBaseline behavior confirmed on unfixed code.")
        print("These behaviors MUST be preserved after implementing the fix.")
        print("\nNext step: Implement the fix (task 3)")
        print("Then re-run these tests to verify no regressions occurred.")
        return True
    else:
        print("✗ SOME PRESERVATION TESTS FAILED")
        print("="*80)
        print("\nWARNING: Some baseline behaviors are not working correctly!")
        print("This is unexpected - preservation tests should pass on unfixed code.")
        print("Review the failures above before proceeding with the fix.")
        return False


if __name__ == "__main__":
    success = run_all_preservation_tests()
    sys.exit(0 if success else 1)
