# Solution: Broken Links and Images in RST to Markdown Migration

**Date:** March 2, 2026  
**Issue:** 2,199 broken references (128 anchor links + 2,071 images)

## Root Cause Analysis

### Problem 1: Missing Images (2,071 references)

**Root Cause:** Images exist in both `source/` and `docs/` directories, but MkDocs is looking for them relative to the wrong base path.

**Evidence:**
- Images exist: `doc/en/user/docs/data/webadmin/img/workspace_services.png` ✓
- Markdown references: `![](data/webadmin/img/workspace_services.png)`
- File location: `doc/en/user/docs/configuration/virtual-services.md`
- MkDocs looks for: `doc/en/user/docs/configuration/data/webadmin/img/workspace_services.png` ✗

**Issue:** The image path is relative to the docs root, but the markdown file is in a subdirectory. MkDocs interprets relative paths from the file's location, not the docs root.

### Problem 2: Broken Anchor Links (128 references)

**Root Cause:** Three distinct issues:

1. **Case sensitivity mismatch** - Markdown generates lowercase anchors, but links use mixed case
   - Link: `[FAQ](#faq)` 
   - Anchor: `## FAQ {: #FAQ }` (should be `{: #faq }`)

2. **Unconverted RST cross-references** - Still using RST syntax
   - Example: `#../../data/app-schema/index.md` (should be `../../data/app-schema/index.md#section`)

3. **Unprocessed variable substitution placeholders**
   - Example: `[|data_directory_win|](##SUBST##|data_directory_win|)`
   - Should be: `[C:\ProgramData\GeoServer\Data](C:\ProgramData\GeoServer\Data)` or similar

## Solution Strategy

### Solution 1: Fix Image Paths (CRITICAL)

**Approach:** Convert absolute-from-docs-root paths to relative paths

**Implementation Options:**

#### Option A: Fix Markdown Files (Recommended)
Run a script to update all image references to use correct relative paths.

```python
#!/usr/bin/env python3
"""Fix image paths in converted Markdown files"""

import os
import re
from pathlib import Path

def fix_image_paths(docs_dir: Path):
    """Fix image paths to be relative to file location"""
    
    for md_file in docs_dir.rglob("*.md"):
        content = md_file.read_text(encoding='utf-8')
        original_content = content
        
        # Find all image references
        # Pattern: ![alt](path/to/image.png)
        def fix_path(match):
            alt_text = match.group(1)
            img_path = match.group(2)
            
            # Skip external URLs
            if img_path.startswith(('http://', 'https://', '//')):
                return match.group(0)
            
            # Skip already relative paths (starting with ./ or ../)
            if img_path.startswith(('./', '../')):
                return match.group(0)
            
            # Calculate relative path from file to docs root
            file_depth = len(md_file.relative_to(docs_dir).parts) - 1
            relative_prefix = '../' * file_depth if file_depth > 0 else './'
            
            # Construct new path
            new_path = relative_prefix + img_path
            
            return f'![{alt_text}]({new_path})'
        
        # Replace all image references
        content = re.sub(r'!\[(.*?)\]\(([^)]+)\)', fix_path, content)
        
        # Write back if changed
        if content != original_content:
            md_file.write_text(content, encoding='utf-8')
            print(f"Fixed: {md_file.relative_to(docs_dir)}")

if __name__ == "__main__":
    docs_dir = Path("doc/en/user/docs")
    fix_image_paths(docs_dir)
    
    # Also fix developer and docguide
    for doc_type in ["developer", "docguide"]:
        docs_dir = Path(f"doc/en/{doc_type}/docs")
        if docs_dir.exists():
            fix_image_paths(docs_dir)
```

#### Option B: Configure MkDocs (Alternative)
Add a custom plugin to handle absolute-from-docs-root paths. Less reliable.

### Solution 2: Fix Anchor Links

#### Fix 2A: Case Sensitivity (Automated)

```python
#!/usr/bin/env python3
"""Fix anchor case sensitivity in Markdown files"""

import re
from pathlib import Path

def fix_anchor_case(docs_dir: Path):
    """Convert explicit anchor IDs to lowercase"""
    
    for md_file in docs_dir.rglob("*.md"):
        content = md_file.read_text(encoding='utf-8')
        original_content = content
        
        # Pattern: ## Heading {: #AnchorID }
        def lowercase_anchor(match):
            heading = match.group(1)
            anchor_id = match.group(2).lower()
            return f'{heading} {{: #{anchor_id} }}'
        
        content = re.sub(r'(##+ .+?) \{: #([A-Za-z0-9_-]+) \}', lowercase_anchor, content)
        
        if content != original_content:
            md_file.write_text(content, encoding='utf-8')
            print(f"Fixed anchors: {md_file.relative_to(docs_dir)}")

if __name__ == "__main__":
    for doc_type in ["user", "developer", "docguide"]:
        docs_dir = Path(f"doc/en/{doc_type}/docs")
        if docs_dir.exists():
            fix_anchor_case(docs_dir)
```

#### Fix 2B: RST Cross-References (Manual Review Required)

Examples to fix:
- `#../../data/app-schema/index.md` → `../../data/app-schema/index.md#section-name`
- `#../rest/index.md` → `../rest/index.md#section-name`

These need manual review to determine the correct target section.

#### Fix 2C: Variable Substitution Placeholders

**Issue:** The migration script has `VARIABLE_SUBSTITUTIONS` defined but doesn't handle platform-specific paths.

**Current code:**
```python
VARIABLE_SUBSTITUTIONS = {
    "|version|": "{{ version }}",
    "|release|": "{{ release }}",
}
```

**Missing substitutions:**
```python
VARIABLE_SUBSTITUTIONS = {
    "|version|": "{{ version }}",
    "|release|": "{{ release }}",
    "|data_directory_win|": "C:\\ProgramData\\GeoServer\\Data",
    "|data_directory_linux|": "/var/lib/geoserver_data",
    "|data_directory_mac|": "/Users/username/Library/Application Support/GeoServer/data_dir",
}
```

**Fix:** Update migration.py and re-run postprocessing on affected files.

## Implementation Plan

### Step 1: Fix Image Paths (CRITICAL - Do First)

```bash
# Create and run the fix_image_paths.py script
python3 fix_image_paths.py

# Verify fixes
cd doc/en/user
mkdocs build --strict 2>&1 | grep -i "image"
```

### Step 2: Fix Anchor Case Sensitivity

```bash
# Create and run the fix_anchor_case.py script
python3 fix_anchor_case.py

# Verify fixes by checking specific files
grep -n "{: #" doc/en/user/docs/community/elasticsearch/index.md
```

### Step 3: Fix Variable Substitution

```bash
# Update migration.py with missing substitutions
# Re-run postprocessing on affected files
python3 -c "
from pathlib import Path
import re

# Define substitutions
subs = {
    '##SUBST##|data_directory_win|': 'C:\\\\ProgramData\\\\GeoServer\\\\Data',
    '##SUBST##|data_directory_linux|': '/var/lib/geoserver_data',
    '##SUBST##|data_directory_mac|': '/Users/username/Library/Application Support/GeoServer/data_dir',
}

# Fix files
for md_file in Path('doc/en/user/docs').rglob('*.md'):
    content = md_file.read_text(encoding='utf-8')
    original = content
    for old, new in subs.items():
        content = content.replace(old, new)
    if content != original:
        md_file.write_text(content, encoding='utf-8')
        print(f'Fixed: {md_file}')
"
```

### Step 4: Manual Review of RST Cross-References

Files to review:
- `doc/en/user/docs/community/jdbcstore/configuration/index.md`
- `doc/en/user/docs/datadirectory/structure/index.md`

Fix pattern: `#../../path/to/file.md` → `../../path/to/file.md#anchor-name`

### Step 5: Validate Fixes

```bash
# Rebuild and check for errors
cd doc/en/user
mkdocs build --strict

# Run validation script
python3 scripts/validate_links.py doc/en/user/target/html
python3 scripts/validate_images.py doc/en/user/docs
```

## Expected Results

After implementing all fixes:
- **Missing Images:** 2,071 → 0 (100% fixed by relative path conversion)
- **Broken Anchors:** 128 → ~10-20 (most fixed automatically, some need manual review)
- **Build Status:** Clean build with no warnings

## Priority

1. **CRITICAL:** Fix image paths (blocks all visual content)
2. **HIGH:** Fix anchor case sensitivity (breaks navigation)
3. **MEDIUM:** Fix variable substitution (affects specific pages)
4. **LOW:** Manual RST cross-reference review (affects ~5 pages)

## Next Steps

1. Create `fix_image_paths.py` script
2. Run on all documentation types (user, developer, docguide)
3. Create `fix_anchor_case.py` script
4. Run on all documentation types
5. Fix variable substitution placeholders
6. Rebuild and validate
7. Commit fixes to migration branch
