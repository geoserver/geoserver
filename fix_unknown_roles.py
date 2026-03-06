#!/usr/bin/env python3
"""
Fix unknown interpreted text roles in converted Markdown files.

This script handles role types that were not converted by the initial migration:
- nightly_community: Links to community module nightly builds
- nightly_extension: Links to extension nightly builds
- doc: Cross-references to other documentation pages
- abbr: Abbreviations with hover definitions
- wiki: Links to GitHub wiki pages
- docguide: Links to documentation guide
"""

import re
import sys
from pathlib import Path
from typing import Dict, Tuple, List


class UnknownRoleFixer:
    """Fix unknown interpreted text roles in Markdown files."""
    
    def __init__(self):
        self.role_mappings = {
            'nightly_community': 'https://build.geoserver.org/geoserver/main/community-latest/',
            'nightly_extension': 'https://build.geoserver.org/geoserver/main/ext-latest/',
            'nightly_release': 'https://build.geoserver.org/geoserver/main/release/',
            'wiki': 'https://github.com/geoserver/geoserver/wiki/',
            'docguide': '../docguide/',
            'doc': '',  # Relative path, use URL as-is
            'website': 'https://geoserver.org/',
            'developer': '../developer/',
            'geotools': 'https://docs.geotools.org/',
            'github': 'https://github.com/geoserver/geoserver/',
            'download_release': 'https://sourceforge.net/projects/geoserver/files/GeoServer/',
            'download_pending': 'https://sourceforge.net/projects/geoserver/files/GeoServer/',
        }
        
        # Pattern to match Pandoc interpreted text roles: `text`{.interpreted-text role="role_name"}
        self.role_pattern = re.compile(
            r'`([^`]+?)`\{\.interpreted-text role="([^"]+?)"\}'
        )
        
        # Pattern to match malformed roles with colon prefix: `:role_name:`
        self.malformed_role_pattern = re.compile(
            r'`([^`]+?)`\{\.interpreted-text role=":([^"]+?):"\}'
        )
        
        # Statistics
        self.stats = {
            'files_processed': 0,
            'files_modified': 0,
            'roles_fixed': {},
            'unknown_roles': set(),
        }
    
    def fix_role(self, text: str, role: str) -> str:
        """
        Convert an interpreted text role to Markdown.
        
        Args:
            text: The display text (which may contain the filename/URL)
            role: The role name (e.g., 'nightly_community')
            
        Returns:
            Converted Markdown string
        """
        # Handle abbr specially - convert to HTML
        # Format: `text`{.interpreted-text role="abbr"}
        # The text should contain the abbreviation, and we need to extract definition
        if role == 'abbr':
            # For abbr, text is just the abbreviation - keep as-is for now
            # (proper abbr handling would require parsing the definition from context)
            return f'**{text}**'
        
        # Handle doc role - the text is the filename
        if role == 'doc':
            # Text is the filename, convert to link
            filename = text
            if not filename.endswith('.md') and not filename.endswith('.html'):
                filename = filename + '.md'
            return f'[{text}]({filename})'
        
        # Handle ref role - internal anchors
        if role == 'ref':
            # Text is the anchor name
            return f'[{text}](#{text})'
        
        # Handle nightly_community and nightly_extension
        # Format: `filename`{.interpreted-text role="nightly_extension"}
        # The text is the filename to download
        if role in ['nightly_community', 'nightly_extension']:
            base_url = self.role_mappings[role]
            # Construct the full download URL
            # Pattern: geoserver-{{ version }}-SNAPSHOT-{text}-plugin.zip
            download_url = f'{base_url}geoserver-{{{{ version }}}}-SNAPSHOT-{text}-plugin.zip'
            return f'[{text}]({download_url})'
        
        # Handle other mapped roles
        if role in self.role_mappings:
            base_url = self.role_mappings[role]
            full_url = base_url + text if base_url else text
            return f'[{text}]({full_url})'
        
        # Unknown role - return as-is and track
        self.stats['unknown_roles'].add(role)
        return f'`{text}`{{.interpreted-text role="{role}"}}'
    
    def fix_file(self, file_path: Path) -> bool:
        """
        Fix unknown roles in a single Markdown file.
        
        Args:
            file_path: Path to the Markdown file
            
        Returns:
            True if file was modified, False otherwise
        """
        try:
            content = file_path.read_text(encoding='utf-8')
            original_content = content
            
            def replace_role(match):
                text = match.group(1)
                role = match.group(2)
                
                # Track statistics
                if role not in self.stats['roles_fixed']:
                    self.stats['roles_fixed'][role] = 0
                self.stats['roles_fixed'][role] += 1
                
                return self.fix_role(text, role)
            
            def replace_malformed_role(match):
                text = match.group(1)
                role = match.group(2)
                
                # Track statistics
                role_key = f':{role}:'
                if role_key not in self.stats['roles_fixed']:
                    self.stats['roles_fixed'][role_key] = 0
                self.stats['roles_fixed'][role_key] += 1
                
                return self.fix_role(text, role)
            
            # Replace all role occurrences
            content = self.role_pattern.sub(replace_role, content)
            content = self.malformed_role_pattern.sub(replace_malformed_role, content)
            
            # Write back if modified
            if content != original_content:
                file_path.write_text(content, encoding='utf-8')
                self.stats['files_modified'] += 1
                return True
            
            return False
            
        except Exception as e:
            print(f"Error processing {file_path}: {e}", file=sys.stderr)
            return False
    
    def fix_directory(self, docs_dir: Path) -> None:
        """
        Fix unknown roles in all Markdown files in a directory.
        
        Args:
            docs_dir: Path to the docs directory
        """
        if not docs_dir.exists():
            print(f"Directory not found: {docs_dir}")
            return
        
        print(f"\nProcessing: {docs_dir}")
        
        # Find all Markdown files
        md_files = list(docs_dir.rglob('*.md'))
        
        for md_file in md_files:
            self.stats['files_processed'] += 1
            self.fix_file(md_file)
    
    def print_summary(self) -> None:
        """Print summary statistics."""
        print("\n" + "=" * 70)
        print("UNKNOWN ROLE FIX SUMMARY")
        print("=" * 70)
        print(f"Files processed: {self.stats['files_processed']}")
        print(f"Files modified: {self.stats['files_modified']}")
        print()
        
        if self.stats['roles_fixed']:
            print("Roles fixed:")
            total_fixed = 0
            for role, count in sorted(self.stats['roles_fixed'].items()):
                print(f"  {role}: {count} occurrences")
                total_fixed += count
            print(f"  TOTAL: {total_fixed} occurrences")
        else:
            print("No roles were fixed.")
        
        if self.stats['unknown_roles']:
            print()
            print("⚠️  Unknown roles encountered (not fixed):")
            for role in sorted(self.stats['unknown_roles']):
                print(f"  - {role}")
        
        print("=" * 70)


def main():
    """Main entry point."""
    fixer = UnknownRoleFixer()
    
    # Process all three manuals
    manuals = [
        Path('doc/en/user/docs'),
        Path('doc/en/developer/docs'),
        Path('doc/en/docguide/docs'),
    ]
    
    for manual in manuals:
        fixer.fix_directory(manual)
    
    fixer.print_summary()
    
    # Return exit code based on success
    if fixer.stats['unknown_roles']:
        print("\n⚠️  Warning: Some unknown roles were not fixed.")
        return 1
    
    return 0


if __name__ == '__main__':
    sys.exit(main())
