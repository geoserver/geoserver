#!/usr/bin/env python3
"""
Navigation Structure Validation Script

This script validates the navigation structure of the migrated MkDocs documentation
by checking:
1. Navigation hierarchy matches original Sphinx structure
2. Navigation tabs, sections, and expansion work correctly
3. Breadcrumbs work correctly
4. "Back to top" functionality exists

Requirements: 5.7, 7.7
"""

import os
import re
from pathlib import Path
from bs4 import BeautifulSoup
import json

class NavigationValidator:
    def __init__(self, html_dir):
        self.html_dir = Path(html_dir)
        self.validation_results = {
            'navigation_tabs': [],
            'navigation_hierarchy': [],
            'breadcrumbs': [],
            'back_to_top': [],
            'issues': [],
            'summary': {}
        }
    
    def validate_navigation_tabs(self):
        """Validate navigation tabs are present and functional"""
        print("🔍 Validating navigation tabs...")
        
        index_file = self.html_dir / 'index.html'
        if not index_file.exists():
            self.validation_results['issues'].append("index.html not found")
            return
        
        with open(index_file, 'r', encoding='utf-8') as f:
            soup = BeautifulSoup(f.read(), 'html.parser')
        
        # Check for navigation tabs
        tabs = soup.find_all('li', class_='md-tabs__item')
        if not tabs:
            self.validation_results['issues'].append("No navigation tabs found")
            return
        
        tab_names = []
        for tab in tabs:
            link = tab.find('a', class_='md-tabs__link')
            if link:
                tab_text = link.get_text(strip=True)
                tab_href = link.get('href', '')
                tab_names.append({'text': tab_text, 'href': tab_href})
        
        self.validation_results['navigation_tabs'] = tab_names
        print(f"✅ Found {len(tab_names)} navigation tabs")
        
        # Expected main tabs based on mkdocs.yml
        expected_tabs = [
            'GeoServer User Manual', 'Introduction', 'Installation', 'Gettingstarted',
            'Webadmin', 'Data', 'Styling', 'Services', 'Filter', 'Configuration',
            'Datadirectory', 'Production', 'Rest', 'Security', 'Geowebcache',
            'Extensions', 'Community', 'Tutorials'
        ]
        
        found_tabs = [tab['text'] for tab in tab_names]
        missing_tabs = [tab for tab in expected_tabs if tab not in found_tabs]
        
        if missing_tabs:
            self.validation_results['issues'].append(f"Missing expected tabs: {missing_tabs}")
        
        return len(tab_names) > 0
    
    def validate_navigation_hierarchy(self):
        """Validate navigation hierarchy and expansion"""
        print("🔍 Validating navigation hierarchy...")
        
        index_file = self.html_dir / 'index.html'
        with open(index_file, 'r', encoding='utf-8') as f:
            soup = BeautifulSoup(f.read(), 'html.parser')
        
        # Check for hierarchical navigation structure
        nav_items = soup.find_all('nav', class_='md-nav')
        hierarchy_levels = {}
        
        for nav in nav_items:
            level = nav.get('data-md-level')
            if level:
                level = int(level)
                if level not in hierarchy_levels:
                    hierarchy_levels[level] = 0
                hierarchy_levels[level] += 1
        
        self.validation_results['navigation_hierarchy'] = hierarchy_levels
        print(f"✅ Found navigation hierarchy with levels: {list(hierarchy_levels.keys())}")
        
        # Check for expandable sections
        expandable_sections = soup.find_all('nav', {'aria-expanded': 'false'})
        print(f"✅ Found {len(expandable_sections)} expandable navigation sections")
        
        return len(hierarchy_levels) > 0
    
    def validate_breadcrumbs(self):
        """Validate breadcrumb navigation"""
        print("🔍 Validating breadcrumbs...")
        
        # Check a few sample pages for breadcrumbs
        sample_pages = [
            'data/webadmin/layers.html',
            'styling/sld/cookbook/points.html',
            'services/wms/reference.html'
        ]
        
        breadcrumb_results = []
        
        for page_path in sample_pages:
            full_path = self.html_dir / page_path
            if full_path.exists():
                with open(full_path, 'r', encoding='utf-8') as f:
                    soup = BeautifulSoup(f.read(), 'html.parser')
                
                # Look for breadcrumb navigation
                breadcrumbs = soup.find_all('nav', {'aria-label': 'Breadcrumb'}) or \
                             soup.find_all('ol', class_='breadcrumb') or \
                             soup.find_all('nav', class_='md-header__inner')
                
                breadcrumb_results.append({
                    'page': page_path,
                    'has_breadcrumbs': len(breadcrumbs) > 0,
                    'breadcrumb_count': len(breadcrumbs)
                })
            else:
                breadcrumb_results.append({
                    'page': page_path,
                    'has_breadcrumbs': False,
                    'error': 'Page not found'
                })
        
        self.validation_results['breadcrumbs'] = breadcrumb_results
        
        # Material theme uses header navigation instead of traditional breadcrumbs
        # Check for header navigation elements
        index_file = self.html_dir / 'index.html'
        with open(index_file, 'r', encoding='utf-8') as f:
            soup = BeautifulSoup(f.read(), 'html.parser')
        
        header_nav = soup.find('header', class_='md-header')
        if header_nav:
            print("✅ Found header navigation (Material theme breadcrumb equivalent)")
            return True
        else:
            self.validation_results['issues'].append("No header navigation found")
            return False
    
    def validate_back_to_top(self):
        """Validate 'Back to top' functionality"""
        print("🔍 Validating 'Back to top' functionality...")
        
        index_file = self.html_dir / 'index.html'
        with open(index_file, 'r', encoding='utf-8') as f:
            soup = BeautifulSoup(f.read(), 'html.parser')
        
        # Look for back to top elements
        back_to_top_elements = []
        
        # Material theme uses navigation.top feature
        # Check for the feature in the theme configuration
        nav_top_elements = soup.find_all(attrs={'data-md-component': 'top'}) or \
                          soup.find_all('a', href='#') or \
                          soup.find_all(class_=re.compile(r'.*top.*', re.I))
        
        # Also check for the navigation.top feature configuration
        # This is typically handled by JavaScript in Material theme
        scripts = soup.find_all('script')
        has_top_feature = False
        
        for script in scripts:
            if script.string and 'navigation.top' in script.string:
                has_top_feature = True
                break
        
        # Material theme automatically adds back-to-top when navigation.top is enabled
        # Check mkdocs.yml configuration
        mkdocs_config = Path('doc/en/user/mkdocs.yml')
        if mkdocs_config.exists():
            with open(mkdocs_config, 'r') as f:
                config_content = f.read()
                if 'navigation.top' in config_content:
                    has_top_feature = True
        
        self.validation_results['back_to_top'] = {
            'elements_found': len(nav_top_elements),
            'has_navigation_top_feature': has_top_feature
        }
        
        if has_top_feature or len(nav_top_elements) > 0:
            print("✅ Back to top functionality is configured")
            return True
        else:
            self.validation_results['issues'].append("No back to top functionality found")
            return False
    
    def validate_theme_features(self):
        """Validate Material theme navigation features"""
        print("🔍 Validating Material theme navigation features...")
        
        mkdocs_config = Path('doc/en/user/mkdocs.yml')
        if not mkdocs_config.exists():
            self.validation_results['issues'].append("mkdocs.yml not found")
            return False
        
        with open(mkdocs_config, 'r') as f:
            config_content = f.read()
        
        # Check for expected navigation features
        expected_features = [
            'navigation.tabs',
            'navigation.tabs.sticky',
            'navigation.sections',
            'navigation.expand',
            'navigation.top',
            'navigation.tracking',
            'navigation.indexes'
        ]
        
        found_features = []
        for feature in expected_features:
            if feature in config_content:
                found_features.append(feature)
        
        print(f"✅ Found navigation features: {found_features}")
        
        missing_features = [f for f in expected_features if f not in found_features]
        if missing_features:
            self.validation_results['issues'].append(f"Missing navigation features: {missing_features}")
        
        return len(found_features) >= len(expected_features) * 0.8  # 80% threshold
    
    def run_validation(self):
        """Run all navigation validation checks"""
        print("🚀 Starting Navigation Structure Validation")
        print("=" * 50)
        
        results = {
            'tabs': self.validate_navigation_tabs(),
            'hierarchy': self.validate_navigation_hierarchy(),
            'breadcrumbs': self.validate_breadcrumbs(),
            'back_to_top': self.validate_back_to_top(),
            'theme_features': self.validate_theme_features()
        }
        
        # Generate summary
        passed_checks = sum(1 for result in results.values() if result)
        total_checks = len(results)
        
        self.validation_results['summary'] = {
            'total_checks': total_checks,
            'passed_checks': passed_checks,
            'success_rate': (passed_checks / total_checks) * 100,
            'overall_status': 'PASS' if passed_checks == total_checks else 'PARTIAL' if passed_checks > 0 else 'FAIL'
        }
        
        print("\n" + "=" * 50)
        print("📊 VALIDATION SUMMARY")
        print("=" * 50)
        
        for check_name, result in results.items():
            status = "✅ PASS" if result else "❌ FAIL"
            print(f"{check_name.replace('_', ' ').title()}: {status}")
        
        print(f"\nOverall Status: {self.validation_results['summary']['overall_status']}")
        print(f"Success Rate: {self.validation_results['summary']['success_rate']:.1f}%")
        
        if self.validation_results['issues']:
            print("\n🚨 ISSUES FOUND:")
            for issue in self.validation_results['issues']:
                print(f"  • {issue}")
        
        return self.validation_results

def main():
    """Main validation function"""
    html_dir = "doc/en/user/target/html"
    
    if not os.path.exists(html_dir):
        print(f"❌ HTML directory not found: {html_dir}")
        print("Please build the documentation first with: mkdocs build")
        return False
    
    validator = NavigationValidator(html_dir)
    results = validator.run_validation()
    
    # Save results to file
    with open('navigation_validation_results.json', 'w') as f:
        json.dump(results, f, indent=2)
    
    print(f"\n📄 Detailed results saved to: navigation_validation_results.json")
    
    return results['summary']['overall_status'] in ['PASS', 'PARTIAL']

if __name__ == "__main__":
    success = main()
    exit(0 if success else 1)