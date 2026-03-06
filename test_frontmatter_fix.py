#!/usr/bin/env python3
"""Test script for frontmatter postprocessor"""

import re
import glob
from pathlib import Path

# Pattern to detect mkdocs-macros variables
variable_pattern = r'\{\{\s*(version|release)\s*\}\}'

# Frontmatter to add
frontmatter = "---\nrender_macros: true\n---\n\n"

# Pattern to detect existing frontmatter
existing_frontmatter_pattern = r'^---\n.*?\n---\n'

added_count = 0
skipped_count = 0

docs_dir = Path('test_conversion/sample_docs/docs')

print(f"Scanning {docs_dir} for files with variables...")
print()

for md_file in docs_dir.glob('**/*.md'):
    try:
        with open(md_file, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Check if file contains variables
        variables_found = re.findall(variable_pattern, content)
        if not variables_found:
            continue
        
        print(f"File: {md_file.name}")
        print(f"  Variables found: {set(variables_found)}")
        
        # Check if frontmatter already exists
        if re.match(existing_frontmatter_pattern, content, re.DOTALL):
            # Check if render_macros is already set
            if 'render_macros:' in content[:200]:
                print(f"  Status: Already has render_macros frontmatter - SKIPPED")
                skipped_count += 1
                continue
            else:
                # Has frontmatter but no render_macros - add it
                print(f"  Status: Has frontmatter, adding render_macros")
                content = re.sub(
                    r'^(---\n)',
                    r'\1render_macros: true\n',
                    content,
                    count=1
                )
        else:
            # No frontmatter - add it
            print(f"  Status: No frontmatter, adding full frontmatter block")
            content = frontmatter + content
        
        with open(md_file, 'w', encoding='utf-8') as f:
            f.write(content)
        added_count += 1
        print(f"  ✓ Frontmatter added")
        print()
    
    except Exception as e:
        print(f'ERROR processing {md_file}: {e}')

print()
print(f"Summary: Added frontmatter to {added_count} files (skipped {skipped_count} with existing frontmatter)")
