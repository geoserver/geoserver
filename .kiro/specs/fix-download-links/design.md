# Fix Download Links Bugfix Design

## Overview

This bugfix addresses broken download links across 50+ GeoServer documentation files that resulted from the RST/Sphinx to Markdown/MkDocs migration. The bug manifests as incomplete link text (e.g., "3.0.0 mbstyle") instead of properly formatted download links with full filenames (e.g., "2.28.0 geoserver-2.28.0-mbstyle-plugin.zip"). The fix will involve creating an automated Python script to systematically correct the link text format across all affected files while preserving macro variable substitution and leaving non-download links unchanged.

## Glossary

- **Bug_Condition (C)**: The condition that triggers the bug - when download links use incomplete text format like `{{ release }} [plugin-name](URL)` instead of including the full filename
- **Property (P)**: The desired behavior when download links are rendered - they should display as "{{ release }} geoserver-{{ release }}-{plugin-name}-plugin.zip" with full filename visible
- **Preservation**: Existing non-download links, internal documentation links, external reference links, macro substitution in non-link contexts, and code blocks that must remain unchanged by the fix
- **Macro Variables**: Template variables like `{{ release }}`, `{{ version }}`, and `{{ snapshot }}` that get substituted with actual version numbers at build time
- **Download Link Types**: Extensions (.zip plugins), community modules (.zip plugins), binary downloads (.war, .bin.zip), and database connectors
- **Link Text**: The visible text portion of a Markdown link in the format `[link text](URL)`

## Bug Details

### Bug Condition

The bug manifests when a documentation file contains download links with incomplete link text that only includes the macro variable and plugin/module name, but omits the full filename pattern. The Markdown rendering engine displays this incomplete text to users, making it unclear what file will be downloaded.

**Formal Specification:**
```
FUNCTION isBugCondition(input)
  INPUT: input of type MarkdownLink
  OUTPUT: boolean
  
  RETURN input.linkText MATCHES_PATTERN '{{ (release|version|snapshot) }} [a-zA-Z0-9-_]+'
         AND input.url CONTAINS '/release/' OR '/nightly/' OR '/community/'
         AND NOT input.linkText CONTAINS 'geoserver-'
         AND NOT input.linkText CONTAINS '.zip' OR '.war'
END FUNCTION
```

### Examples

- **Extension Link**: Current `{{ release }} [mbstyle](https://...)` renders as "3.0.0 mbstyle" → Expected "3.0.0 geoserver-3.0.0-mbstyle-plugin.zip"
- **Nightly Extension**: Current `{{ snapshot }} [css](https://...)` renders as "2.28-SNAPSHOT css" → Expected "2.28-SNAPSHOT geoserver-2.28-SNAPSHOT-css-plugin.zip"
- **Community Module**: Current `{{ release }} [wps-download](https://...)` renders as "3.0.0 wps-download" → Expected "3.0.0 geoserver-3.0.0-wps-download-plugin.zip"
- **Binary WAR**: Current `{{ release }} [war](https://...)` renders as "3.0.0 war" → Expected "3.0.0 geoserver-3.0.0.war"
- **Binary Bin**: Current `{{ release }} [bin](https://...)` renders as "3.0.0 bin" → Expected "3.0.0 geoserver-3.0.0-bin.zip"

## Expected Behavior

### Preservation Requirements

**Unchanged Behaviors:**
- Regular Markdown links (non-download) must continue to render with their existing link text format
- Internal documentation links must continue to work correctly without modification
- External reference links (non-download) must continue to render correctly
- Macro variable substitution in non-link contexts must continue to work
- Code blocks containing download examples must remain as code without link processing

**Scope:**
All links that do NOT point to download URLs (containing `/release/`, `/nightly/`, or `/community/` paths) should be completely unaffected by this fix. This includes:
- Navigation links between documentation pages
- External reference links to other websites
- Anchor links within the same page
- Links in code blocks or inline code
- Any link that already has the correct full filename format

## Hypothesized Root Cause

Based on the bug description and migration context, the most likely issues are:

1. **Incomplete RST Conversion**: The RST-to-Markdown conversion script did not properly handle the `:download_extension:`, `:nightly_extension:`, `:download_community:`, and similar interpreted text roles, resulting in simplified link text that only captured the plugin name without the full filename pattern.

2. **Missing Filename Pattern Logic**: The conversion script may have extracted the plugin/module name from the RST role but failed to reconstruct the complete filename pattern (e.g., `geoserver-{{ release }}-{name}-plugin.zip`) that was implicit in the original RST roles.

3. **Macro Variable Placement**: The macro variables were correctly preserved but placed only at the beginning of the link text, without being repeated in the filename portion where they're needed for the full path.

4. **Link Type Differentiation**: The conversion may not have distinguished between different download types (extensions vs. community modules vs. binaries) which require slightly different filename patterns (.war vs. .zip, with or without "-plugin" suffix).

## Correctness Properties

Property 1: Bug Condition - Download Links Display Full Filenames

_For any_ Markdown link where the bug condition holds (link text matches incomplete pattern and URL points to download location), the fixed documentation SHALL display the link text with the complete filename pattern including "geoserver-", the macro variable, the plugin/module name, and the appropriate file extension (.zip, .war, etc.).

**Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5**

Property 2: Preservation - Non-Download Link Behavior

_For any_ Markdown link where the bug condition does NOT hold (regular links, internal links, external non-download links, or links already in correct format), the fixed documentation SHALL preserve exactly the same link text and URL as before the fix, maintaining all existing non-download link functionality.

**Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5**

## Fix Implementation

### Changes Required

Assuming our root cause analysis is correct:

**Approach**: Create a Python script to systematically fix all affected download links across the documentation files.

**Script Location**: `scripts/fix_download_links.py` (new file)

**Specific Changes**:

1. **Pattern Detection**: Implement regex patterns to identify buggy download links
   - Match `{{ release }} [plugin-name](URL)` patterns
   - Match `{{ version }} [plugin-name](URL)` patterns
   - Match `{{ snapshot }} [plugin-name](URL)` patterns
   - Verify URL contains download paths (`/release/`, `/nightly/`, `/community/`)

2. **Link Type Classification**: Determine the correct filename pattern based on context
   - Extensions: `geoserver-{{ macro }}-{name}-plugin.zip`
   - Community modules: `geoserver-{{ macro }}-{name}-plugin.zip`
   - WAR binaries: `geoserver-{{ macro }}.war`
   - Bin binaries: `geoserver-{{ macro }}-bin.zip`
   - Database connectors: `geoserver-{{ macro }}-{name}-plugin.zip`

3. **Link Text Reconstruction**: Replace incomplete link text with full filename pattern
   - Extract the plugin/module name from current link text
   - Extract the macro variable (release, version, or snapshot)
   - Construct the appropriate full filename based on link type
   - Preserve the URL unchanged

4. **File Processing**: Iterate through all affected documentation files
   - Scan `docs/` directory recursively for `.md` files
   - Apply fixes only to lines matching the bug condition
   - Preserve all other content unchanged
   - Create backup or use git for safety

5. **Validation**: Verify fixes don't break existing functionality
   - Ensure macro variables are still properly formatted for MkDocs processing
   - Verify URLs remain unchanged
   - Check that non-download links are untouched
   - Test that code blocks are not modified

### Implementation Strategy

The Python script will:
1. Use regex to find all Markdown links in each file
2. For each link, check if it matches the bug condition
3. If buggy, extract the macro variable and plugin name
4. Determine the link type from the URL path
5. Reconstruct the link text with the full filename pattern
6. Replace the old link with the corrected version
7. Write the updated content back to the file

## Testing Strategy

### Validation Approach

The testing strategy follows a two-phase approach: first, verify the bug exists in the current documentation by examining actual files, then verify the fix script correctly transforms buggy links while preserving all other content.

### Exploratory Bug Condition Checking

**Goal**: Surface concrete examples that demonstrate the bug BEFORE implementing the fix. Confirm the root cause analysis by examining actual documentation files.

**Test Plan**: Manually inspect several documentation files known to contain download links. Verify that they exhibit the incomplete link text pattern. Document specific examples with file paths and line numbers.

**Test Cases**:
1. **Extension Link Test**: Find a file with extension download links (e.g., `docs/user/extensions/mbstyle.md`) and verify it shows "{{ release }} [mbstyle]" instead of full filename (will demonstrate bug)
2. **Community Module Test**: Find a file with community module links and verify incomplete link text (will demonstrate bug)
3. **Binary Download Test**: Find a file with WAR/bin download links and verify incomplete link text (will demonstrate bug)
4. **Multiple Link Types**: Find a file with multiple download link types and verify all are affected (will demonstrate bug scope)

**Expected Counterexamples**:
- Link text displays only "3.0.0 mbstyle" instead of "3.0.0 geoserver-3.0.0-mbstyle-plugin.zip"
- Users cannot see what file they're downloading from the link text alone
- Pattern is consistent across 50+ files, confirming systematic conversion issue

### Fix Checking

**Goal**: Verify that for all links where the bug condition holds, the fixed documentation produces the expected full filename in the link text.

**Pseudocode:**
```
FOR ALL link IN documentation_files WHERE isBugCondition(link) DO
  fixed_link := fix_download_link(link)
  ASSERT fixed_link.linkText CONTAINS 'geoserver-'
  ASSERT fixed_link.linkText CONTAINS macro_variable
  ASSERT fixed_link.linkText CONTAINS plugin_name
  ASSERT fixed_link.linkText CONTAINS appropriate_extension
  ASSERT fixed_link.url = link.url  // URL unchanged
END FOR
```

### Preservation Checking

**Goal**: Verify that for all links where the bug condition does NOT hold, the fixed documentation produces exactly the same link text and URL as the original.

**Pseudocode:**
```
FOR ALL link IN documentation_files WHERE NOT isBugCondition(link) DO
  ASSERT original_content(link) = fixed_content(link)
END FOR
```

**Testing Approach**: Property-based testing principles apply here - we want to verify that the vast majority of documentation content (non-download links, prose, code blocks, etc.) remains completely unchanged. Manual inspection of a representative sample combined with diff tools will provide confidence.

**Test Plan**: Before running the fix script, create a git commit or backup. After running the script, use `git diff` to examine all changes and verify that only buggy download links were modified.

**Test Cases**:
1. **Regular Link Preservation**: Verify that internal documentation links (e.g., `[configuration](../config.md)`) remain unchanged
2. **External Link Preservation**: Verify that external reference links (e.g., `[OGC WMS](https://www.ogc.org/...)`) remain unchanged
3. **Code Block Preservation**: Verify that code blocks containing download URLs are not modified
4. **Prose Preservation**: Verify that all non-link text content remains identical
5. **Already-Correct Links**: Verify that any download links already in correct format are not modified

### Unit Tests

- Test regex pattern matching for each download link type
- Test link text reconstruction for extensions, community modules, WAR, and bin downloads
- Test that non-matching links are ignored by the fix logic
- Test edge cases (links with special characters, multi-line links, etc.)

### Property-Based Tests

- Generate random documentation content with various link types and verify only buggy download links are modified
- Generate random plugin names and verify correct filename pattern construction
- Test across many file samples to ensure consistent behavior

### Integration Tests

- Run the fix script on a test copy of the documentation
- Use `git diff` to verify only expected changes occurred
- Build the documentation with MkDocs and verify links render correctly
- Manually click several fixed download links to verify they work
- Verify macro variables are properly substituted in the built documentation

