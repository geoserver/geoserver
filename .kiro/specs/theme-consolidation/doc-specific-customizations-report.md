# Doc-Specific Customizations Report

**Date:** March 5, 2025  
**Task:** 2.3 Handle Doc-Specific Customizations  
**Spec:** theme-consolidation

## Executive Summary

This report documents all doc-specific customization files in the GeoServer documentation and explains the MkDocs override mechanism that allows individual documentation types to have custom assets while sharing a common theme.

**Key Findings:**
- ✅ 3 doc-specific image files identified in docguide
- ✅ All files are legitimate content images, not theme overrides
- ✅ Files are correctly located in `docs/` directory
- ✅ MkDocs automatically handles doc-specific files without special configuration
- ✅ No action required - current setup is optimal

## Doc-Specific Files Inventory

### Documentation Guide (docguide)

**Location:** `doc/en/docguide/docs/`

| File | Size | Purpose | Referenced In | Status |
|------|------|---------|---------------|--------|
| `pagelogo.png` | 16,202 bytes | Example image for documentation tutorial | `sphinx.md` | ✅ Keep |
| `github_edit1.png` | Unknown | Screenshot for GitHub editing workflow | `quickfix.md` | ✅ Keep |
| `github_edit2.png` | Unknown | Screenshot for GitHub editing workflow | Not found in current docs | ✅ Keep |

### User Manual

**Location:** `doc/en/user/docs/`

- ✅ **No doc-specific theme files** - only content images in subdirectories

### Developer Manual

**Location:** `doc/en/developer/docs/`

- ✅ **No doc-specific theme files** - only content images in subdirectories

## File Analysis

### 1. pagelogo.png

**Purpose:** Educational example in documentation guide

**Referenced in:** `doc/en/docguide/docs/sphinx.md`

```markdown
![](pagelogo.png)
*The GeoServer logo as shown on the homepage.*
```

**Context:** This image is used as an example in the documentation guide to demonstrate how to include images in documentation. It shows the GeoServer logo and is accompanied by a caption explaining proper image usage.

**Decision:** ✅ **Keep as doc-specific file**

**Rationale:**
- This is content, not a theme asset
- Specific to the documentation guide's tutorial content
- Not intended to be shared across other documentation types
- Correctly located in `docs/` directory (not theme directory)

### 2. github_edit1.png

**Purpose:** Screenshot for GitHub editing workflow tutorial

**Referenced in:** `doc/en/docguide/docs/quickfix.md`

```markdown
![](github_edit1.png)
*GitHub Preview of style.rst page*
```

**Context:** This screenshot demonstrates the GitHub web interface for editing documentation files. It's part of the "Quick Fix" tutorial that teaches contributors how to make simple edits via GitHub's web UI.

**Decision:** ✅ **Keep as doc-specific file**

**Rationale:**
- Tutorial content specific to docguide
- Not a theme asset
- Correctly located in `docs/` directory

### 3. github_edit2.png

**Purpose:** Additional screenshot for GitHub editing workflow

**Status:** File exists but reference not found in current markdown files (may be orphaned or referenced in converted content)

**Decision:** ✅ **Keep as doc-specific file**

**Rationale:**
- Part of the same tutorial series
- Low risk to keep (small file)
- May be referenced in content not yet migrated

## MkDocs Override Mechanism

### How MkDocs Handles Doc-Specific Files

MkDocs uses a **layered file resolution system** with the following priority:

1. **Highest Priority:** Files in `docs_dir` (e.g., `doc/en/docguide/docs/`)
2. **Medium Priority:** Files in `theme.custom_dir` (e.g., `doc/themes/geoserver/`)
3. **Lowest Priority:** Files in the base theme (e.g., Material theme)

### File Resolution Example

When MkDocs encounters a reference like `![](pagelogo.png)`:

```
1. Check: doc/en/docguide/docs/pagelogo.png ✅ FOUND - Use this
2. Check: doc/themes/geoserver/pagelogo.png (skipped - already found)
3. Check: Material theme/pagelogo.png (skipped - already found)
```

When MkDocs encounters a theme asset like `img/geoserver-logo.png`:

```
1. Check: doc/en/docguide/docs/img/geoserver-logo.png ❌ Not found
2. Check: doc/themes/geoserver/img/geoserver-logo.png ✅ FOUND - Use this
3. Check: Material theme/img/geoserver-logo.png (skipped - already found)
```

### Configuration

**Current mkdocs.yml configuration:**

```yaml
docs_dir: docs                          # Content and doc-specific files
site_dir: target/html                   # Build output
theme:
  name: material                        # Base theme
  custom_dir: ../../themes/geoserver    # Shared theme customizations
```

This configuration enables:
- ✅ Shared theme assets from `doc/themes/geoserver/`
- ✅ Doc-specific files from `docs/` directory
- ✅ Automatic override without special configuration

## Override Mechanism Documentation

### For Theme Assets (Shared)

**Location:** `doc/themes/geoserver/`

**Purpose:** Assets shared across all documentation types

**Examples:**
- `img/geoserver-logo.png` - Site logo
- `img/geoserver.ico` - Favicon
- `stylesheets/extra.css` - Custom CSS
- `partials/header.html` - Custom header template
- `partials/header-switcher.html` - Documentation switcher

**How to modify:**
1. Edit files in `doc/themes/geoserver/`
2. Changes automatically apply to all documentation types
3. Test all three documentation types after changes

### For Doc-Specific Content (Per-Documentation)

**Location:** `doc/en/{type}/docs/`

**Purpose:** Content and assets specific to one documentation type

**Examples:**
- `doc/en/docguide/docs/pagelogo.png` - Tutorial example image
- `doc/en/docguide/docs/github_edit1.png` - Tutorial screenshot
- Any markdown files and their associated images

**How to add doc-specific files:**
1. Place file in `doc/en/{type}/docs/` directory
2. Reference in markdown using relative path: `![](filename.png)`
3. File automatically overrides any theme file with the same name
4. No configuration changes needed

### Override Priority Rules

**Rule 1:** Content directory (`docs/`) always wins over theme directory

**Rule 2:** Theme directory (`custom_dir`) always wins over base theme

**Rule 3:** Files with the same name in `docs/` will shadow theme files

**Example Scenario:**

If you place `img/geoserver-logo.png` in `doc/en/docguide/docs/img/`:
- Docguide will use the doc-specific logo
- User manual and developer manual will use the shared theme logo
- No configuration changes needed

## Recommendations

### 1. Current Setup is Optimal ✅

**No changes needed** - The current file organization is correct:
- Doc-specific content images are in `docs/` directories
- Shared theme assets are in `doc/themes/geoserver/`
- MkDocs automatically handles the override mechanism

### 2. Documentation for Future Maintainers

**Action:** Create a README in the theme directory explaining the override mechanism

**Location:** `doc/themes/geoserver/README.md`

**Content:** Should explain:
- What files are in the shared theme
- How to modify shared theme assets
- How to add doc-specific overrides
- Testing procedures after changes

### 3. Naming Convention

**Recommendation:** Use descriptive names for doc-specific files to avoid accidental shadowing

**Good examples:**
- ✅ `pagelogo.png` - Clearly a content image
- ✅ `github_edit1.png` - Clearly a tutorial screenshot
- ✅ `workflow-diagram.png` - Clearly content

**Avoid:**
- ❌ `logo.png` - Could be confused with theme logo
- ❌ `extra.css` - Would shadow theme CSS
- ❌ `header.html` - Would shadow theme template

## Testing Verification

### Build Test Results

From Task 1.3 baseline build:

| Documentation | pagelogo.png in Output | Status |
|--------------|------------------------|--------|
| User Manual | ❌ Not present (expected) | ✅ Correct |
| Developer Manual | ❌ Not present (expected) | ✅ Correct |
| Documentation Guide | ✅ Present at `target/html/pagelogo.png` | ✅ Correct |

**Verification:** Doc-specific files are correctly isolated to their respective documentation types.

### Reference Verification

All references to doc-specific files are valid:

- ✅ `pagelogo.png` referenced in `sphinx.md` - file exists
- ✅ `github_edit1.png` referenced in `quickfix.md` - file exists
- ⚠️ `github_edit2.png` not referenced in current docs - file exists (orphaned?)

## Conclusion

### Summary

All doc-specific customization files have been identified and analyzed:

1. **3 doc-specific files** found in docguide
2. **All files are content images**, not theme overrides
3. **Files are correctly located** in `docs/` directory
4. **MkDocs override mechanism** works automatically
5. **No configuration changes** required

### Actions Taken

- ✅ Identified all doc-specific files
- ✅ Analyzed purpose and usage of each file
- ✅ Verified correct location and references
- ✅ Documented MkDocs override mechanism
- ✅ Confirmed current setup is optimal

### Actions Required

- ⏭️ Create `doc/themes/geoserver/README.md` (Task 4.1)
- ⏭️ Consider removing orphaned `github_edit2.png` if confirmed unused

### No Changes Needed

- ✅ Doc-specific files are in correct locations
- ✅ Override mechanism works automatically
- ✅ No risk of theme file conflicts
- ✅ Current setup follows MkDocs best practices

---

**Task Status:** ✅ Complete  
**Next Task:** 3.1 Build and Test All Documentation Types

