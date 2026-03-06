# Macro Rendering Fix Summary

## Issue Identified
On the deployed GitHub Pages, macro variables like `{{ version }}` and `{{ release }}` were appearing as literal text instead of being rendered with their actual values (e.g., "3.0", "3.0.0").

### Example
- **Expected**: "GeoServer 3.0 is an open source..."
- **Actual**: "GeoServer {{ version }} is an open source..."

### Affected Pages
- User Manual index: https://petersmythe.github.io/geoserver/migration/2.28-x-rst-to-md/en/user/
- Developer Manual index: https://petersmythe.github.io/geoserver/migration/2.28-x-rst-to-md/en/developer/
- Documentation Guide index: https://petersmythe.github.io/geoserver/migration/2.28-x-rst-to-md/en/docguide/

## Root Cause
Files containing `{{ version }}` or `{{ release }}` macros need YAML frontmatter with `render_macros: true` to tell mkdocs-macros plugin to process the macros in that file.

Without this frontmatter, the macros are treated as literal text and not replaced with their values.

## Solution Implemented

### 1. Created Automated Fix Script
**File**: `fix_macro_rendering.py`

This Python script:
- Scans all Markdown files in documentation directories
- Detects files containing `{{ version }}` or `{{ release }}` macros
- Checks if file already has `render_macros: true` in frontmatter
- Adds frontmatter if missing, or updates existing frontmatter
- Reports which files were fixed

### 2. Fixed Index Files
Applied the fix to three index files:
- `doc/en/user/docs/index.md`
- `doc/en/developer/docs/index.md`
- `doc/en/docguide/docs/index.md`

Each file now has:
```yaml
---
render_macros: true
---
```

### 3. Updated Tasks for 3.0 Branch
Added notes to `.kiro/specs/rst-to-markdown-migration/tasks.md` for task 5.5:
- Document the macro rendering fix needed for 3.0 branch
- Clarify multi-line include syntax fix
- Provide clear instructions for running fix_macro_rendering.py

## Files Modified

### Created
1. `fix_macro_rendering.py` - Automated fix script

### Modified
1. `doc/en/user/docs/index.md` - Added render_macros frontmatter
2. `doc/en/developer/docs/index.md` - Added render_macros frontmatter
3. `doc/en/docguide/docs/index.md` - Added render_macros frontmatter
4. `.kiro/specs/rst-to-markdown-migration/tasks.md` - Updated with fix notes

## How the Fix Works

### Before Fix
```markdown
# GeoServer User Manual

GeoServer {{ version }} is an open source...
```

Result: "GeoServer {{ version }} is an open source..."

### After Fix
```markdown
---
render_macros: true
---

# GeoServer User Manual

GeoServer {{ version }} is an open source...
```

Result: "GeoServer 3.0 is an open source..."

## Verification

### Local Testing
```bash
cd doc/en/user
source ../../../.venv/Scripts/activate
mkdocs serve
```

Open http://localhost:8000 and verify:
- Index page shows "GeoServer 3.0" (not "GeoServer {{ version }}")
- All macro variables are rendered correctly

### Deployed Testing
After GitHub Actions completes:
1. Visit: https://petersmythe.github.io/geoserver/migration/2.28-x-rst-to-md/en/user/
2. Verify index page shows "GeoServer 3.0" (not literal macros)
3. Check developer and docguide manuals similarly

## Other Files with Macros

The grep search found many other files with `{{ version }}` and `{{ release }}` macros:
- Installation pages (docker.md, linux.md, win_binary.md, etc.)
- Extension installation pages
- Service installation pages
- Styling extension pages

**These files already have `render_macros: true` frontmatter** from previous fixes, so they don't need the fix script run on them.

## For 3.0 Branch Migration

When migrating the 3.0 branch, run the fix script:

```bash
# Activate virtual environment
source .venv/Scripts/activate

# Run the fix script
python fix_macro_rendering.py

# Review changes
git diff

# Commit if fixes were applied
git add doc/en/*/docs/index.md
git commit -m "Fix macro rendering in index files"
```

## Related Issues

### Multi-line Include Syntax
The `fix_include_syntax.py` script handles a related issue where multi-line `{% include %}` statements cause macro syntax errors:

**Problem**:
```markdown
{% 
  include "path/to/file.md"
  start="..."
%}
```

**Solution**:
```markdown
{% include "path/to/file.md" start="..." %}
```

This is a separate issue but uses similar frontmatter requirements.

## Commit Details

- **Commit**: 5ca573a9fb
- **Branch**: migration/2.28-x-rst-to-md
- **Message**: "Fix macro rendering in index files"
- **Files Changed**: 2 files (111 insertions, 3 deletions)

## Status
✅ **COMPLETED** - Fix applied, committed, and pushed to GitHub
