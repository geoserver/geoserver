#!/usr/bin/env python3
"""Test script for interpreted text roles postprocessor"""

import re
import glob
from pathlib import Path

# Role mappings for GeoServer documentation
role_mappings = {
    'website': 'https://geoserver.org/',
    'developer': 'https://docs.geoserver.org/latest/en/developer/',
    'user': '../user/',
}

# Pattern: `text <url>`{.interpreted-text role="rolename"}
pattern = r'`([^`]+) <([^>]+)>`\{\.interpreted-text role="([^"]+)"\}'

converted_count = 0
file_count = 0

docs_dir = Path('test_conversion/sample_docs/docs')

print(f"Scanning {docs_dir} for interpreted text roles...")
print()

for md_file in docs_dir.glob('**/*.md'):
    try:
        with open(md_file, 'r', encoding='utf-8') as f:
            content = f.read()
        
        original_content = content
        file_converted = [0]  # Use list to allow modification in nested function
        
        def replace_role(match):
            text = match.group(1)
            url = match.group(2)
            role = match.group(3)
            
            if role in role_mappings:
                full_url = role_mappings[role] + url
                file_converted[0] += 1
                print(f"  Converting: `{text} <{url}>` (role={role})")
                print(f"         To: [{text}]({full_url})")
                return f'[{text}]({full_url})'
            else:
                print(f"  WARNING: Unknown role '{role}' in {md_file.name}")
                return match.group(0)
        
        content = re.sub(pattern, replace_role, content)
        
        if content != original_content:
            with open(md_file, 'w', encoding='utf-8') as f:
                f.write(content)
            file_count += 1
            converted_count += file_converted[0]
            print(f"✓ Processed: {md_file.name} ({file_converted[0]} conversions)")
            print()
    
    except Exception as e:
        print(f'ERROR processing {md_file}: {e}')

print()
print(f"Summary: Converted {converted_count} interpreted text roles in {file_count} files")
