#!/usr/bin/env python3
"""
Comprehensive Navigation Validation for All Documentation Types

Validates navigation structure for:
- User Manual (doc/en/user/target/html)
- Developer Manual (doc/en/developer/target/html) 
- Documentation Guide (doc/en/docguide/target/html)
"""

import os
import json
from pathlib import Path
from bs4 import BeautifulSoup

def validate_documentation_type(doc_type, html_dir, expected_tabs):
    """Validate navigation for a specific documentation type"""
    print(f"\n🔍 Validating {doc_type} Navigation")
    print("-" * 40)
    
    html_path = Path(html_dir)
    if not html_path.exists():
        print(f"❌ HTML directory not found: {html_dir}")
        return {'status': 'FAIL', 'error': 'HTML directory not found'}
    
    index_file = html_path / 'index.html'
    if not index_file.exists():
        print(f"❌ index.html not found in {html_dir}")
        return {'status': 'FAIL', 'error': 'index.html not found'}
    
    results = {
        'doc_type': doc_type,
        'html_dir': str(html_dir),
        'navigation_tabs': [],
        'navigation_hierarchy': {},
        'theme_features': [],
        'issues': [],
        'status': 'PASS'
    }
    
    try:
        with open(index_file, 'r', encoding='utf-8') as f:
            soup = BeautifulSoup(f.read(), 'html.parser')
        
        # 1. Validate navigation tabs
        tabs = soup.find_all('li', class_='md-tabs__item')
        tab_names = []
        for tab in tabs:
            link = tab.find('a', class_='md-tabs__link')
            if link:
                tab_text = link.get_text(strip=True)
                tab_href = link.get('href', '')
                tab_names.append({'text': tab_text, 'href': tab_href})
        
        results['navigation_tabs'] = tab_names
        print(f"✅ Found {len(tab_names)} navigation tabs")
        
        # Check expected tabs
        found_tab_texts = [tab['text'] for tab in tab_names]
        missing_tabs = [tab for tab in expected_tabs if tab not in found_tab_texts]
        if missing_tabs:
            results['issues'].append(f"Missing expected tabs: {missing_tabs}")
            print(f"⚠️  Missing tabs: {missing_tabs}")
        
        # 2. Validate navigation hierarchy
        nav_items = soup.find_all('nav', class_='md-nav')
        hierarchy_levels = {}
        
        for nav in nav_items:
            level = nav.get('data-md-level')
            if level:
                level = int(level)
                if level not in hierarchy_levels:
                    hierarchy_levels[level] = 0
                hierarchy_levels[level] += 1
        
        results['navigation_hierarchy'] = hierarchy_levels
        print(f"✅ Navigation hierarchy levels: {list(hierarchy_levels.keys())}")
        
        # 3. Check expandable sections
        expandable_sections = soup.find_all('nav', {'aria-expanded': 'false'})
        print(f"✅ Found {len(expandable_sections)} expandable sections")
        
        # 4. Validate header navigation (breadcrumb equivalent)
        header_nav = soup.find('header', class_='md-header')
        if header_nav:
            print("✅ Header navigation present")
        else:
            results['issues'].append("No header navigation found")
            print("❌ No header navigation found")
        
        # 5. Check back to top functionality
        # Check mkdocs.yml for navigation.top feature
        mkdocs_config = html_path.parent / 'mkdocs.yml'
        has_top_feature = False
        if mkdocs_config.exists():
            with open(mkdocs_config, 'r') as f:
                config_content = f.read()
                if 'navigation.top' in config_content:
                    has_top_feature = True
                    print("✅ Back to top functionality configured")
        
        if not has_top_feature:
            results['issues'].append("Back to top functionality not configured")
            print("⚠️  Back to top functionality not configured")
        
        # 6. Validate Material theme features
        theme_features = []
        if mkdocs_config.exists():
            with open(mkdocs_config, 'r') as f:
                config_content = f.read()
                
            expected_features = [
                'navigation.tabs',
                'navigation.sections', 
                'navigation.expand',
                'navigation.top'
            ]
            
            for feature in expected_features:
                if feature in config_content:
                    theme_features.append(feature)
        
        results['theme_features'] = theme_features
        print(f"✅ Theme features: {theme_features}")
        
        # Determine overall status
        if results['issues']:
            results['status'] = 'PARTIAL'
        
    except Exception as e:
        results['status'] = 'FAIL'
        results['error'] = str(e)
        print(f"❌ Error during validation: {e}")
    
    return results

def main():
    """Main validation function for all documentation types"""
    print("🚀 Comprehensive Navigation Structure Validation")
    print("=" * 60)
    
    # Define documentation types and their expected structure
    doc_configs = [
        {
            'type': 'User Manual',
            'html_dir': 'doc/en/user/target/html',
            'expected_tabs': [
                'GeoServer User Manual', 'Introduction', 'Installation', 
                'Gettingstarted', 'Data', 'Styling', 'Services'
            ]
        },
        {
            'type': 'Developer Manual', 
            'html_dir': 'doc/en/developer/target/html',
            'expected_tabs': [
                'GeoServer Developer Manual', 'Introduction', 'Tools',
                'Source', 'Quickstart', 'Programming Guide'
            ]
        },
        {
            'type': 'Documentation Guide',
            'html_dir': 'doc/en/docguide/target/html', 
            'expected_tabs': [
                'GeoServer Documentation Guide', 'Background', 'Contributing',
                'Workflow', 'Install'
            ]
        }
    ]
    
    all_results = []
    overall_status = 'PASS'
    
    for config in doc_configs:
        result = validate_documentation_type(
            config['type'],
            config['html_dir'], 
            config['expected_tabs']
        )
        all_results.append(result)
        
        if result['status'] != 'PASS':
            overall_status = 'PARTIAL' if overall_status == 'PASS' else 'FAIL'
    
    # Generate summary report
    print("\n" + "=" * 60)
    print("📊 COMPREHENSIVE VALIDATION SUMMARY")
    print("=" * 60)
    
    for result in all_results:
        status_icon = "✅" if result['status'] == 'PASS' else "⚠️" if result['status'] == 'PARTIAL' else "❌"
        print(f"{result['doc_type']}: {status_icon} {result['status']}")
        
        if result.get('issues'):
            for issue in result['issues']:
                print(f"  • {issue}")
    
    print(f"\nOverall Status: {overall_status}")
    
    # Save detailed results
    summary_report = {
        'overall_status': overall_status,
        'validation_timestamp': str(Path().resolve()),
        'results': all_results
    }
    
    with open('comprehensive_navigation_validation.json', 'w') as f:
        json.dump(summary_report, f, indent=2)
    
    print(f"\n📄 Detailed results saved to: comprehensive_navigation_validation.json")
    
    return overall_status in ['PASS', 'PARTIAL']

if __name__ == "__main__":
    success = main()
    exit(0 if success else 1)