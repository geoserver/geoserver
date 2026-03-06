#!/usr/bin/env python3
"""
Fix tables with blank first row (empty cells) followed by separator row.
These render incorrectly in MkDocs.

Pattern to fix:
|   |   |   |
|---|---|---|
| Header1 | Header2 | Header3 |
| Data1 | Data2 | Data3 |

Should become:
| Header1 | Header2 | Header3 |
|---------|---------|---------|
| Data1 | Data2 | Data3 |
"""

import os
import re
from pathlib import Path

def is_blank_row(line):
    """Check if a table row contains only empty cells."""
    # Remove leading/trailing pipes and whitespace
    content = line.strip().strip('|').strip()
    # Split by pipe and check if all cells are empty
    cells = [cell.strip() for cell in content.split('|')]
    return all(not cell for cell in cells)

def is_separator_row(line):
    """Check if a line is a table separator row."""
    # Table separator contains only |, -, :, and whitespace
    return bool(re.match(r'^\s*\|[\s\-:|]+\|\s*$', line))

def fix_blank_header_tables(content):
    """Fix tables with blank first row by moving the real headers to position 1."""
    lines = content.split('\n')
    fixed_lines = []
    i = 0
    fixes = 0
    
    while i < len(lines):
        line = lines[i]
        
        # Check if this is a blank table row
        if line.strip().startswith('|') and is_blank_row(line):
            # Check if next line is a separator and there's a 3rd row with real headers
            if (i + 2 < len(lines) and 
                is_separator_row(lines[i + 1]) and 
                lines[i + 2].strip().startswith('|')):
                # This is the pattern: blank row -> separator -> real headers
                # Move real headers to position 1, keep separator as position 2
                real_headers = lines[i + 2]
                separator = lines[i + 1]
                
                fixed_lines.append(real_headers)  # Real headers become row 1
                fixed_lines.append(separator)      # Separator stays as row 2
                
                fixes += 1
                i += 3  # Skip all 3 lines (blank, separator, headers)
                continue
        
        fixed_lines.append(line)
        i += 1
    
    return '\n'.join(fixed_lines), fixes

def process_file(filepath):
    """Process a single markdown file."""
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
        
        fixed_content, fixes = fix_blank_header_tables(content)
        
        if fixes > 0:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(fixed_content)
            return fixes
        
        return 0
    except Exception as e:
        print(f"Error processing {filepath}: {e}")
        return 0

def main():
    """Process all markdown files in documentation directories."""
    base_dirs = [
        'doc/en/user/docs',
        'doc/en/developer/docs',
        'doc/en/docguide/docs'
    ]
    
    total_fixes = 0
    fixed_files = []
    
    for base_dir in base_dirs:
        if not os.path.exists(base_dir):
            continue
        
        print(f"\nSearching in {base_dir}...")
        
        for root, dirs, files in os.walk(base_dir):
            for file in files:
                if file.endswith('.md'):
                    filepath = os.path.join(root, file)
                    fixes = process_file(filepath)
                    if fixes > 0:
                        total_fixes += fixes
                        fixed_files.append((filepath, fixes))
                        print(f"  Fixed: {filepath} ({fixes} blank rows removed)")
    
    print(f"\n{'='*60}")
    print(f"Total: Removed {total_fixes} blank header rows from {len(fixed_files)} files")
    
    if fixed_files:
        print("\nFixed files:")
        for filepath, fixes in fixed_files:
            print(f"  - {filepath} ({fixes} rows)")

if __name__ == '__main__':
    main()
