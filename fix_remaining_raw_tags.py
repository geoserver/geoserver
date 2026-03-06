#!/usr/bin/env python3
"""
Fix remaining include statements that need {%raw%} tags.
"""

import re
from pathlib import Path

def fix_file(file_path):
    """Fix include statements in a single file."""
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original_content = content
    
    # Fix import.json includes
    content = re.sub(
        r'(```json\s*\n\s*)(\{% include "\./files/import\.json" %\})(\s*\n\s*```)',
        r'\1{%raw%}\2{%endraw%}\3',
        content
    )
    
    if content != original_content:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)
        return True
    return False

def main():
    """Process specific files."""
    files_to_fix = [
        Path('doc/en/user/docs/extensions/importer/rest_examples.md'),
    ]
    
    fixed_files = []
    
    for file_path in files_to_fix:
        if file_path.exists() and fix_file(file_path):
            fixed_files.append(file_path)
            print(f'Fixed: {file_path}')
    
    print(f'\nTotal files fixed: {len(fixed_files)}')

if __name__ == '__main__':
    main()
