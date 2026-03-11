#!/usr/bin/env python3
"""
Fix Download Links Script

This script fixes broken download links across GeoServer documentation files
that resulted from the RST/Sphinx to Markdown/MkDocs migration.

Bug Condition: Links matching pattern `{{ (release|version|snapshot) }} [plugin-name](URL)`
where URL contains download paths and link text does NOT contain "geoserver-" prefix
or file extensions (.zip, .war).

Expected Behavior: Transform incomplete link text to full filename patterns:
- Extensions/Community: geoserver-{{ macro }}-{name}-plugin.zip
- WAR binaries: geoserver-{{ macro }}.war
- Bin binaries: geoserver-{{ macro }}-bin.zip
"""

import re
import os
from pathlib import Path
from typing import List, Tuple, Optional


class DownloadLinkFixer:
    """Fixes broken download links in markdown documentation."""
    
    # Pattern to match: {{ macro }} [text](url)
    LINK_PATTERN = r'\{\{\s*(release|version|snapshot)\s*\}\}\s*\[([^\]]+)\]\(([^)]+)\)'
    
    # Download URL indicators
    DOWNLOAD_PATHS = ['/release/', '/nightly/', '/community/', 'sourceforge.net', 'build.geoserver.org']
    
    def __init__(self, dry_run: bool = False):
        self.dry_run = dry_run
        self.stats = {
            'files_processed': 0,
            'files_modified': 0,
            'links_fixed': 0
        }
    
    def is_download_link(self, url: str) -> bool:
        """Check if URL is a download link."""
        return any(path in url for path in self.DOWNLOAD_PATHS)
    
    def is_buggy_link(self, link_text: str) -> bool:
        """
        Check if link text exhibits the bug condition.
        
        Bug condition: Missing "geoserver-" prefix AND missing file extensions.
        """
        has_geoserver_prefix = 'geoserver-' in link_text.lower()
        has_file_extension = any(ext in link_text.lower() for ext in ['.zip', '.war'])
        
        return not (has_geoserver_prefix or has_file_extension)
    
    def classify_link_type(self, link_text: str, url: str) -> str:
        """
        Classify the download link type based on URL and context.
        
        Returns: 'war', 'bin', 'extension', or 'community'
        """
        url_lower = url.lower()
        text_lower = link_text.lower()
        
        # Check for WAR binary
        if '.war' in url_lower or text_lower == 'war':
            return 'war'
        
        # Check for bin binary
        if 'bin.zip' in url_lower or '-bin' in url_lower or text_lower == 'bin':
            return 'bin'
        
        # Check for community module
        if '/community/' in url_lower:
            return 'community'
        
        # Default to extension
        return 'extension'
    
    def reconstruct_link_text(self, macro: str, link_text: str, url: str) -> str:
        """
        Reconstruct the link text with full filename pattern.
        
        Args:
            macro: The macro variable (release, version, or snapshot)
            link_text: Current incomplete link text
            url: The download URL
        
        Returns:
            Reconstructed link text with full filename
        """
        link_type = self.classify_link_type(link_text, url)
        plugin_name = link_text.strip()
        
        if link_type == 'war':
            return f"geoserver-{{{{ {macro} }}}}.war"
        elif link_type == 'bin':
            return f"geoserver-{{{{ {macro} }}}}-bin.zip"
        else:  # extension or community
            return f"geoserver-{{{{ {macro} }}}}-{plugin_name}-plugin.zip"
    
    def fix_line(self, line: str) -> Tuple[str, int]:
        """
        Fix all buggy download links in a line.
        
        Returns:
            Tuple of (fixed_line, number_of_fixes)
        """
        fixes_count = 0
        
        def replace_link(match):
            nonlocal fixes_count
            
            macro = match.group(1)
            link_text = match.group(2)
            url = match.group(3)
            
            # Only fix if it's a download link and exhibits bug condition
            if self.is_download_link(url) and self.is_buggy_link(link_text):
                new_link_text = self.reconstruct_link_text(macro, link_text, url)
                fixes_count += 1
                return f"{{{{ {macro} }}}} [{new_link_text}]({url})"
            
            # Return original if not buggy
            return match.group(0)
        
        fixed_line = re.sub(self.LINK_PATTERN, replace_link, line)
        return fixed_line, fixes_count
    
    def fix_file(self, file_path: Path) -> bool:
        """
        Fix all buggy download links in a file.
        
        Returns:
            True if file was modified, False otherwise
        """
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                lines = f.readlines()
        except Exception as e:
            print(f"Error reading {file_path}: {e}")
            return False
        
        modified = False
        fixed_lines = []
        file_fixes = 0
        
        for line in lines:
            fixed_line, fixes = self.fix_line(line)
            fixed_lines.append(fixed_line)
            
            if fixes > 0:
                modified = True
                file_fixes += fixes
        
        if modified and not self.dry_run:
            try:
                with open(file_path, 'w', encoding='utf-8') as f:
                    f.writelines(fixed_lines)
                print(f"Fixed {file_fixes} link(s) in: {file_path}")
            except Exception as e:
                print(f"Error writing {file_path}: {e}")
                return False
        elif modified and self.dry_run:
            print(f"[DRY RUN] Would fix {file_fixes} link(s) in: {file_path}")
        
        if modified:
            self.stats['links_fixed'] += file_fixes
        
        return modified
    
    def find_markdown_files(self, root_dir: str = "doc/en/user/docs") -> List[Path]:
        """Find all markdown files in the documentation directory."""
        root = Path(root_dir)
        if not root.exists():
            print(f"Warning: Directory {root_dir} does not exist")
            return []
        return list(root.rglob("*.md"))
    
    def fix_all(self, root_dir: str = "doc/en/user/docs"):
        """Fix all markdown files in the documentation directory."""
        print("="*80)
        print("Download Link Fixer")
        print("="*80)
        
        if self.dry_run:
            print("\n[DRY RUN MODE] No files will be modified\n")
        
        md_files = self.find_markdown_files(root_dir)
        print(f"\nFound {len(md_files)} markdown files to process\n")
        
        for file_path in md_files:
            self.stats['files_processed'] += 1
            if self.fix_file(file_path):
                self.stats['files_modified'] += 1
        
        # Print summary
        print("\n" + "="*80)
        print("Summary")
        print("="*80)
        print(f"Files processed: {self.stats['files_processed']}")
        print(f"Files modified: {self.stats['files_modified']}")
        print(f"Links fixed: {self.stats['links_fixed']}")
        print("="*80)


def main():
    """Main entry point."""
    import argparse
    
    parser = argparse.ArgumentParser(
        description='Fix broken download links in GeoServer documentation'
    )
    parser.add_argument(
        '--dry-run',
        action='store_true',
        help='Show what would be changed without modifying files'
    )
    parser.add_argument(
        '--root-dir',
        default='doc/en/user/docs',
        help='Root directory to scan for markdown files (default: doc/en/user/docs)'
    )
    
    args = parser.parse_args()
    
    fixer = DownloadLinkFixer(dry_run=args.dry_run)
    fixer.fix_all(root_dir=args.root_dir)


if __name__ == "__main__":
    main()
