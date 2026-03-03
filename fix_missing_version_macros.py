#!/usr/bin/env python3
"""
Automatically fix missing version/release macros in converted Markdown files.

This script reads the RST files from a previous commit and adds back the
{{ version }} and {{ release }} macros that were dropped during conversion.
"""

import subprocess
import re
import sys
from pathlib import Path

def get_file_content_from_commit(filepath, commit="07fe3b2c7d"):
    """Get file content from a specific commit."""
    result = subprocess.run(
        ["git", "show", f"{commit}:{filepath}"],
        capture_output=True,
        text=True
    )
    
    if result.returncode != 0:
        return None
    
    return result.stdout

def convert_rst_path_to_md(rst_path):
    """Convert RST file path to expected Markdown path."""
    md_path = rst_path.replace('/source/', '/docs/').replace('.rst', '.md')
    return md_path

def fix_version_macros_in_file(md_path, rst_path):
    """Fix version/release macros in a Markdown file based on RST original."""
    if not Path(md_path).exists():
        print(f"  ⚠️  Skipping {md_path} (file doesn't exist)")
        return False
    
    # Get RST content
    rst_content = get_file_content_from_commit(rst_path)
    if not rst_content:
        print(f"  ⚠️  Skipping {md_path} (couldn't read RST)")
        return False
    
    # Read current MD content
    with open(md_path, 'r', encoding='utf-8') as f:
        md_content = f.read()
    
    original_md = md_content
    
    # Find patterns in RST and convert to MD equivalents
    # Pattern 1: |version| -> {{ version }}
    # Pattern 2: |release| -> {{ release }}
    
    # Common patterns to fix:
    replacements = [
        # "geoserver-|version|-" -> "geoserver-{{ version }}-"
        (r'geoserver-(\d+\.\d+\.\d+)-', r'geoserver-{{ version }}-'),
        # "GeoServer |version|" -> "GeoServer {{ version }}"
        (r'GeoServer (\d+\.\d+)', r'GeoServer {{ version }}'),
        # "version |release|" -> "version {{ release }}"
        (r'version (\d+\.\d+\.\d+)', r'version {{ release }}'),
        # "(for example 2.28.0)" -> "(for example {{ release }})"
        (r'\(for example \d+\.\d+\.\d+\)', r'(for example {{ release }})'),
        # "example: 2.28.0" -> "example: {{ release }}"
        (r'example: \d+\.\d+\.\d+', r'example: {{ release }}'),
        # "example: 2.28.x" -> "example: {{ version }}.x"
        (r'example: \d+\.\d+\.x', r'example: {{ version }}.x'),
    ]
    
    for pattern, replacement in replacements:
        md_content = re.sub(pattern, replacement, md_content)
    
    if md_content != original_md:
        with open(md_path, 'w', encoding='utf-8') as f:
            f.write(md_content)
        return True
    
    return False

def main():
    print("Fixing missing version/release macros in Markdown files...")
    print("=" * 80)
    
    # Get all RST files with version macros
    result = subprocess.run(
        ["git", "grep", "-l", "-E", r"\|version\||\|release\|", "07fe3b2c7d", "--", "doc/en/user/source/"],
        capture_output=True,
        text=True
    )
    
    if result.returncode != 0:
        print("No RST files found with version macros")
        return 0
    
    rst_files = result.stdout.strip().split('\n')
    rst_files = [f.split(':', 1)[1] for f in rst_files if f and not f.endswith('conf.py')]
    
    print(f"Found {len(rst_files)} RST files to process\n")
    
    fixed_count = 0
    skipped_count = 0
    
    for rst_file in rst_files:
        md_file = convert_rst_path_to_md(rst_file)
        
        if fix_version_macros_in_file(md_file, rst_file):
            print(f"✅ Fixed: {md_file}")
            fixed_count += 1
        else:
            skipped_count += 1
    
    print(f"\n{'=' * 80}")
    print(f"Fixed {fixed_count} files")
    print(f"Skipped {skipped_count} files")
    
    return 0

if __name__ == "__main__":
    sys.exit(main())
