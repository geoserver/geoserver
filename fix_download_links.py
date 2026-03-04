#!/usr/bin/env python3
"""
Fix broken download links in converted Markdown documentation.

The conversion tool incorrectly converted RST interpreted text roles like:
  :download_extension:`mbstyle`
  :nightly_extension:`mbstyle`

To broken Markdown links like:
  [mbstyle](https://build.geoserver.org/geoserver/main/ext-latest/mbstyle)
  [mbstyle](https://build.geoserver.org/geoserver/main/ext-latest/geoserver-{{ version }}-SNAPSHOT-mbstyle-plugin.zip)

This script fixes them to proper download URLs:
  [mbstyle](https://sourceforge.net/projects/geoserver/files/GeoServer/{{ release }}/extensions/geoserver-{{ release }}-mbstyle-plugin.zip)
  [mbstyle](https://build.geoserver.org/geoserver/main/extensions/geoserver-{{ snapshot }}-mbstyle-plugin.zip)

Note: Uses {{ snapshot }} macro (e.g., "3.0-SNAPSHOT") instead of {{ version }} for snapshot downloads.
"""

import re
import sys
from pathlib import Path

def fix_download_links(content, filepath):
    """Fix broken download links in Markdown content."""
    changes = []
    
    # Pattern 1: Fix snapshot version links FIRST (more specific pattern)
    # FROM: [name](https://build.geoserver.org/geoserver/main/ext-latest/geoserver-{{ version }}-SNAPSHOT-name-plugin.zip)
    # TO:   [name](https://build.geoserver.org/geoserver/main/extensions/geoserver-{{ snapshot }}-name-plugin.zip)
    pattern1 = r'\[([^\]]+)\]\(https://build\.geoserver\.org/geoserver/main/ext-latest/(geoserver-\{\{ version \}\}-SNAPSHOT-[^\)]+)\)'
    
    def replace1(match):
        link_text = match.group(1)
        filename = match.group(2)
        # Replace "{{ version }}-SNAPSHOT" with "{{ snapshot }}"
        new_filename = filename.replace('{{ version }}-SNAPSHOT', '{{ snapshot }}')
        changes.append(f"  - Fixed snapshot link: {filename} -> {new_filename}")
        return f'[{link_text}](https://build.geoserver.org/geoserver/main/extensions/{new_filename})'
    
    content = re.sub(pattern1, replace1, content)
    
    # Pattern 2: Fix release version links ({{ release }}) - directory URLs only
    # FROM: [name](https://build.geoserver.org/geoserver/main/ext-latest/name)
    # TO:   [name](https://sourceforge.net/projects/geoserver/files/GeoServer/{{ release }}/extensions/geoserver-{{ release }}-name-plugin.zip)
    # Only match if it's NOT already a .zip file
    pattern2 = r'\[([^\]]+)\]\(https://build\.geoserver\.org/geoserver/main/ext-latest/([^/\)]+)\)(?!\S)'
    
    def replace2(match):
        link_text = match.group(1)
        plugin_name = match.group(2)
        # Skip if it looks like a filename
        if '.zip' in plugin_name or 'geoserver-' in plugin_name:
            return match.group(0)
        changes.append(f"  - Fixed release link: {plugin_name}")
        return f'[{link_text}](https://sourceforge.net/projects/geoserver/files/GeoServer/{{{{ release }}}}/extensions/geoserver-{{{{ release }}}}-{plugin_name}-plugin.zip)'
    
    content = re.sub(pattern2, replace2, content)
    
    # Pattern 3: Fix community module snapshot links
    # FROM: [name](https://build.geoserver.org/geoserver/main/community-latest/name)
    # TO:   [name](https://build.geoserver.org/geoserver/main/community-latest/geoserver-{{ snapshot }}-name-plugin.zip)
    pattern3 = r'\[([^\]]+)\]\(https://build\.geoserver\.org/geoserver/main/community-latest/([^/\)]+)\)(?!\S)'
    
    def replace3(match):
        link_text = match.group(1)
        plugin_name = match.group(2)
        # Check if it's already a full filename
        if '.zip' in plugin_name or 'geoserver-' in plugin_name:
            return match.group(0)  # Don't change if already correct
        changes.append(f"  - Fixed community link: {plugin_name}")
        return f'[{link_text}](https://build.geoserver.org/geoserver/main/community-latest/geoserver-{{{{ snapshot }}}}-{plugin_name}-plugin.zip)'
    
    content = re.sub(pattern3, replace3, content)
    
    return content, changes

def process_file(filepath):
    """Process a single Markdown file."""
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            original_content = f.read()
        
        new_content, changes = fix_download_links(original_content, filepath)
        
        if changes:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(new_content)
            print(f"✓ {filepath}")
            for change in changes:
                print(change)
            return 1
        return 0
    except Exception as e:
        print(f"✗ Error processing {filepath}: {e}", file=sys.stderr)
        return 0

def main():
    """Main function to process all Markdown files."""
    docs_dir = Path('doc/en')
    
    if not docs_dir.exists():
        print(f"Error: {docs_dir} not found", file=sys.stderr)
        return 1
    
    # Find all Markdown files
    md_files = list(docs_dir.rglob('*.md'))
    
    print(f"Processing {len(md_files)} Markdown files...")
    print()
    
    fixed_count = 0
    for md_file in sorted(md_files):
        fixed_count += process_file(md_file)
    
    print()
    print(f"Fixed {fixed_count} files with broken download links")
    return 0

if __name__ == '__main__':
    sys.exit(main())
