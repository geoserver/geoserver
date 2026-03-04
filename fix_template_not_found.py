#!/usr/bin/env python3
"""
Fix TemplateNotFound errors by wrapping include statements in {%raw%}...{%endraw%} tags.
These are includes that reference files outside the docs directory.
"""

import re
from pathlib import Path

# List of include patterns that cause TemplateNotFound errors
TEMPLATE_NOT_FOUND_PATTERNS = [
    r'\{% include "\./src/modisvi/indexer\.properties" %\}',
    r'\{% include "\./dynamic_map_layer/layers-dynamicmaplayer\.html" %\}',
    r'\{% include "\./feature_layer/layers-featurelayer-fema\.html" %\}',
    r'\{% include "\./feature_table/featuretable\.html" %\}',
    r'\{% include "\./controlflow\.properties" %\}',
    r'\{% include "\./files/postgis\.json" %\}',
    r'\{% include "\./files/background\.sld" %\}',
    r'\{% include "\.\./files/airports2\.sld" %\}',
    r'\{% include "\.\./files/sprites\.json" %\}',
    r'\{% include "\./artifacts/polygon_simplepolygon\.ysld" %\}',
    r'\{% include "\./src/indexerWithElevation\.properties" %\}',
    r'\{% include "\./src/datastore\.properties" %\}',
    r'\{% include "\./hello/pom\.xml" %\}',
]

def fix_template_not_found(file_path):
    """Fix TemplateNotFound errors in a single file."""
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original_content = content
    
    for pattern in TEMPLATE_NOT_FOUND_PATTERNS:
        # Check if the include is already wrapped in {%raw%}...{%endraw%}
        # If not, wrap it
        def replace_include(match):
            include_stmt = match.group(0)
            # Check if already wrapped
            if '{%raw%}' in content[max(0, match.start()-20):match.start()]:
                return include_stmt
            return f'{{%raw%}}{include_stmt}{{%endraw%}}'
        
        content = re.sub(pattern, replace_include, content)
    
    # Also fix the malformed {%raw%}{%endraw%}{% include pattern
    content = re.sub(r'\{%raw%\}\{%endraw%\}(\{% include [^%]+%\})', 
                     r'{%raw%}\1{%endraw%}', content)
    
    if content != original_content:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)
        return True
    return False

def main():
    """Process all markdown files in doc directories."""
    doc_dirs = [
        Path('doc/en/user/docs'),
        Path('doc/en/developer/docs'),
        Path('doc/en/docguide/docs')
    ]
    
    fixed_files = []
    
    for doc_dir in doc_dirs:
        if not doc_dir.exists():
            continue
        
        for md_file in doc_dir.rglob('*.md'):
            if fix_template_not_found(md_file):
                fixed_files.append(md_file)
                print(f'Fixed: {md_file}')
    
    print(f'\nTotal files fixed: {len(fixed_files)}')
    
    if fixed_files:
        print('\nFixed files:')
        for f in fixed_files:
            print(f'  - {f}')

if __name__ == '__main__':
    main()
