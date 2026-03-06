#!/usr/bin/env python3
"""
Replace hardcoded API spec paths with {{ api_url }} variable.
"""

import sys
from pathlib import Path
import re

# Files and their API spec references
files_to_fix = [
    'doc/en/user/docs/community/opensearch-eo/upgrading.md',
    'doc/en/user/docs/community/proxy-base-ext/usage.md',
    'doc/en/user/docs/extensions/metadata/index.md',
    'doc/en/user/docs/extensions/params-extractor/usage.md',
    'doc/en/user/docs/extensions/rat/using.md',
    'doc/en/user/docs/extensions/wps-download/index.md',
    'doc/en/user/docs/styling/sld/working.md',
]

def fix_api_urls():
    """Replace hardcoded API paths with {{ api_url }} variable."""
    fixed_count = 0
    
    # Pattern to match various forms of API paths
    # Matches: ../../../api/1.0.0/filename.yaml or ../../../../api/1.0.0/filename.yaml
    pattern = re.compile(r'(\.\./)+api/1\.0\.0/([a-z\-]+\.yaml)')
    
    for filepath in files_to_fix:
        path = Path(filepath)
        
        if not path.exists():
            print(f"⚠ File not found: {filepath}")
            continue
        
        with open(path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Replace all API path patterns with {{ api_url }}/filename.yaml
        new_content = pattern.sub(r'{{ api_url }}/\2', content)
        
        if new_content != content:
            with open(path, 'w', encoding='utf-8') as f:
                f.write(new_content)
            
            # Count how many replacements were made
            matches = pattern.findall(content)
            print(f"✓ {path.name}: {len(matches)} API reference(s) fixed")
            fixed_count += len(matches)
    
    print(f"\n✓ Fixed {fixed_count} API references in {len(files_to_fix)} files")
    return 0

if __name__ == '__main__':
    sys.exit(fix_api_urls())
