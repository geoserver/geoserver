# HTML Output Comparison Results - Task 3.1

**Date:** March 2, 2026  
**Branch:** migration/2.28-x-rst-to-md  
**Documentation:** User Manual

## Executive Summary

Successfully built and compared HTML outputs from both Sphinx (RST) and MkDocs (Markdown) for the GeoServer User Manual. The RoundTripValidator identified rendering issues that need attention before finalizing the migration.

## Build Status

### Sphinx HTML Build ✓
- **Source:** `doc/en/user/source/` (RST files)
- **Output:** `doc/en/user/target/user/html/`
- **Status:** SUCCESS
- **Build Time:** ~30 seconds
- **Command:** `sphinx-build -D release=2.28-SNAPSHOT -q -W --keep-going -b html`

### MkDocs HTML Build ✓
- **Source:** `doc/en/user/docs/` (Markdown files)
- **Output:** `doc/en/user/target/html/`
- **Status:** SUCCESS (with warnings)
- **Build Time:** ~70 seconds
- **Command:** `mkdocs build`
- **Note:** Custom theme directory issue resolved by commenting out `custom_dir` temporarily

## Validation Results

### Content Comparison ✓
- **Total Files Compared:** Sphinx and MkDocs HTML outputs
- **Content Issues:** 0
- **Status:** PASSED

The HTML content structure and completeness match between Sphinx and MkDocs outputs. No major rendering problems with tables, missing sections, or broken formatting detected in the core content.

### Broken Anchors ⚠️
- **Count:** 128 broken anchor links
- **Severity:** WARNING
- **Impact:** Internal page navigation may not work correctly

**Sample Issues:**
- `community/elasticsearch/index.html` - Missing anchor `#faq`
- `community/geopkg/index.html` - Missing anchor `#geopkgoutput`
- `datadirectory/setting.md` - Malformed anchors with `##SUBST##` placeholders
- `services/wcs/reference.md` - Missing anchors for `#getcapabilities`, `#describecoverage`, `#getcoverage`

**Root Causes:**
1. Anchor generation differences between Sphinx and MkDocs
2. Unconverted RST cross-references (e.g., `#../../data/app-schema/index.md`)
3. Variable substitution placeholders not processed (e.g., `##SUBST##|data_directory_win|`)

### Missing Images ❌
- **Count:** 2,071 broken image references
- **Severity:** ERROR
- **Impact:** Images will not display in rendered documentation

**Sample Missing Images:**
- `data/webadmin/img/workspace_services.png`
- `data/webadmin/img/workspace_wms_settings.png`
- `configuration/img/server_status.png`
- Wildcard references: `img/feature-style.*`, `img/composite-source.*`

**Root Causes:**
1. Image paths not correctly converted from RST to Markdown
2. Images may still be in RST source directory structure
3. Wildcard image references (`.*`) not resolved

### Code Blocks ✓
- **Issues:** 0
- **Status:** PASSED

Code blocks are rendering correctly with proper syntax highlighting.

### External Links ✓
- **Issues:** 0
- **Status:** PASSED

External links are well-formed and accessible.

## Screenshot QA Analysis

### Summary
- **Total Images:** 4,072
- **Screenshots:** 1,604 (39.4%)
- **Diagrams:** 2,468 (60.6%)
- **Flagged for Update:** 1,604 screenshots

### Screenshot Distribution
Screenshots are distributed across all documentation sections:
- Community modules (backuprestore, elasticsearch, features-templating, etc.)
- Data management (webadmin, database, raster, vector)
- Styling (SLD, CSS, YSLD, MBStyle)
- Services (WMS, WFS, WCS, WMTS, WPS)
- Security and configuration
- Extensions (monitoring, importer, geofence, etc.)

### Recommendations
1. **Phase 1 (Initial QA):** Review screenshot inventory and prioritize critical UI screenshots
2. **Phase 2 (Final QA):** Update all screenshots after GeoServer 3.0 UI completion
3. **Coordination:** Share screenshot QA report with AfriGIS team

## Major Rendering Problems Identified

### 1. Broken Anchor Links (128 issues)
**Problem:** Internal page navigation broken due to anchor mismatches  
**Priority:** HIGH  
**Action Required:** Fix anchor generation and cross-reference conversion

### 2. Missing Images (2,071 issues)
**Problem:** Over 50% of images not found in expected locations  
**Priority:** CRITICAL  
**Action Required:** 
- Verify image file locations
- Fix image path conversion in Markdown files
- Resolve wildcard image references

### 3. Variable Substitution Placeholders
**Problem:** Unconverted `##SUBST##` placeholders in anchor links  
**Priority:** MEDIUM  
**Action Required:** Ensure variable substitution postprocessor runs correctly

## Comparison with Requirements

### Requirement 5.1 ✓
**Build HTML from original RST using Sphinx**  
Status: COMPLETE - Sphinx HTML built successfully

### Requirement 5.2 ✓
**Build HTML from converted Markdown using MkDocs**  
Status: COMPLETE - MkDocs HTML built successfully (with warnings)

### Requirement 5.3 ✓
**Compare each rendered HTML page side-by-side**  
Status: COMPLETE - RoundTripValidator compared all pages

### Requirement 5.4 ⚠️
**Identify major rendering problems**  
Status: COMPLETE - Identified 2,199 issues (128 anchors + 2,071 images)

## Next Steps (Task 3.2 - Manual Review)

1. **Review Directive Conversions**
   - guilabel → bold text
   - menuselection → bold with arrow separator
   - file → inline code
   - Admonitions (note, warning, tip)
   - Code blocks with syntax highlighting

2. **Fix Critical Issues**
   - Resolve 2,071 missing image references
   - Fix 128 broken anchor links
   - Address variable substitution placeholders

3. **Navigation Structure Validation (Task 3.3)**
   - Verify navigation hierarchy
   - Test breadcrumbs and "Back to top"
   - Check navigation tabs and sections

4. **Link Integrity Validation (Task 3.4)**
   - Fix all broken internal links
   - Fix all broken anchor links
   - Document external link issues

## Files Generated

1. **user_validation_report.md** - Detailed validation report with all issues
2. **user_screenshot_qa.md** - Screenshot inventory for QA team
3. **html_comparison_results.md** - This summary document

## Conclusion

The HTML comparison reveals that the core content structure is preserved correctly, but significant work is needed to fix image references and anchor links before the migration can be considered complete. The validation process successfully identified these issues, allowing for targeted fixes in subsequent tasks.

**Overall Assessment:** Conversion quality is good for content, but image and anchor handling needs improvement.

**Recommendation:** Proceed to Task 3.2 (Manual Review) to assess directive conversions, then return to fix the identified image and anchor issues before moving forward with the migration.
