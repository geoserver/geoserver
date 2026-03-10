#!/usr/bin/env python3
"""
Test to verify the doc_switcher fix generates absolute paths correctly.

This test verifies that the define_env function in version.py converts
relative doc_switcher URLs to absolute paths that work at any nesting level.
"""

import sys
import os
from pathlib import Path
from unittest.mock import Mock

# Add doc directory to path to import version.py
sys.path.insert(0, str(Path(__file__).parent / 'doc'))

import version


def create_mock_env(site_url, doc_type):
    """Create a mock mkdocs-macros environment."""
    env = Mock()
    env.conf = {
        'site_url': site_url,
        'extra': {
            'doc_type': doc_type,
            'doc_switcher': []  # Will be populated by define_env
        }
    }
    env.variables = {}
    return env


def test_doc_switcher_absolute_paths():
    """
    Test that doc_switcher URLs are converted to absolute paths.
    
    This verifies the fix for the bug where relative paths failed at nesting level 2+.
    """
    print("\n" + "="*80)
    print("Doc Switcher Fix Verification Test")
    print("="*80)
    print("\nTesting that define_env converts relative paths to absolute paths...\n")
    
    # Test scenarios with different site_url configurations
    test_scenarios = [
        {
            'name': 'User Manual - Standard deployment',
            'site_url': 'https://docs.geoserver.org/3.0/en/user/',
            'doc_type': 'user',
            'expected_base': '/3.0/en/'
        },
        {
            'name': 'Developer Manual - Standard deployment',
            'site_url': 'https://docs.geoserver.org/3.0/en/developer/',
            'doc_type': 'developer',
            'expected_base': '/3.0/en/'
        },
        {
            'name': 'User Manual - Migration branch (with DOCS_BASE_PATH)',
            'site_url': 'https://docs.geoserver.org/3.0/en/user/',
            'doc_type': 'user',
            'expected_base': '/geoserver/migration/3.0-rst-to-md/en/',
            'env_var': '/geoserver/migration/3.0-rst-to-md'
        },
    ]
    
    all_passed = True
    
    for scenario in test_scenarios:
        print(f"\nScenario: {scenario['name']}")
        print(f"  site_url: {scenario['site_url']}")
        print(f"  doc_type: {scenario['doc_type']}")
        
        # Set environment variable if specified
        if 'env_var' in scenario:
            os.environ['DOCS_BASE_PATH'] = scenario['env_var']
            print(f"  DOCS_BASE_PATH: {scenario['env_var']}")
        else:
            os.environ.pop('DOCS_BASE_PATH', None)
        
        # Create mock environment and call define_env
        env = create_mock_env(scenario['site_url'], scenario['doc_type'])
        version.define_env(env)
        
        # Get the generated doc_switcher
        doc_switcher = env.conf['extra']['doc_switcher']
        
        print(f"\n  Generated doc_switcher URLs:")
        
        # Expected absolute paths
        expected_urls = {
            'user': f"{scenario['expected_base']}user/",
            'developer': f"{scenario['expected_base']}developer/",
            'docguide': f"{scenario['expected_base']}docguide/",
            'swagger': f"{scenario['expected_base']}user/api/"
        }
        
        scenario_passed = True
        
        for entry in doc_switcher:
            label = entry['label']
            url = entry['url']
            doc_type = entry['type']
            expected_url = expected_urls[doc_type]
            
            # Check if URL is absolute (starts with /)
            is_absolute = url.startswith('/')
            
            # Check if URL matches expected
            is_correct = url == expected_url
            
            if is_absolute and is_correct:
                status = "✓ PASS"
            else:
                status = "✗ FAIL"
                scenario_passed = False
                all_passed = False
            
            print(f"    {status} {label}: {url}")
            
            if not is_absolute:
                print(f"         ERROR: URL is not absolute (doesn't start with /)")
            if not is_correct:
                print(f"         ERROR: Expected {expected_url}")
        
        if scenario_passed:
            print(f"\n  ✓ Scenario PASSED")
        else:
            print(f"\n  ✗ Scenario FAILED")
    
    # Clean up environment variable
    os.environ.pop('DOCS_BASE_PATH', None)
    
    print("\n" + "="*80)
    
    if all_passed:
        print("TEST RESULT: PASSED")
        print("="*80)
        print("\nAll doc_switcher URLs are correctly converted to absolute paths.")
        print("The fix ensures navigation works at any nesting level.")
        return True
    else:
        print("TEST RESULT: FAILED")
        print("="*80)
        print("\nSome doc_switcher URLs are not correctly converted to absolute paths.")
        print("The fix may not be working as expected.")
        return False


if __name__ == "__main__":
    success = test_doc_switcher_absolute_paths()
    sys.exit(0 if success else 1)
