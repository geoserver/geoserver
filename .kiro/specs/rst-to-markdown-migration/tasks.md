# Implementation Plan: RST to Markdown Migration

## Overview

This plan executes the one-time migration of GeoServer documentation from RST/Sphinx to Markdown/MkDocs across 6 phases over 14 days. The conversion tool (petersmythe/translate) already exists; this project focuses on executing conversion, validating results, committing changes, and managing the transition for branches 3.0 and 2.28.x.

## Tasks

- [ ] 1. Phase 1: Preparation and Setup (Days 1-2)
  - [x] 1.1 Create migration branches and setup environment
    - Create migration branch from 3.0: `git checkout -b migration/3.0-rst-to-md main`
    - Create migration branch from 2.28.x: `git checkout -b migration/2.28-x-rst-to-md 2.28.x`
    - Install Python dependencies: mkdocs, mkdocs-material, mkdocs-macros-plugin, mkdocs-with-pdf, pymdown-extensions
    - Clone petersmythe/translate tool to local environment
    - _Requirements: 1.1, 1.5_

  - [x] 1.2 Create migration orchestration script (migration.py)
    - Implement MigrationOrchestrator class with convert_all_files(), validate_conversion(), generate_config(), create_summary_report()
    - Implement TranslationToolWrapper to execute petersmythe/translate on RST files
    - Configure directive mappings (guilabel, menuselection, file, code-block, note, warning, tip, only)
    - Configure variable substitutions (|version|, |release|)
    - _Requirements: 1.2, 1.3, 1.4, 1.6_

  - [x] 1.3 Create validation scripts
    - Implement RoundTripValidator class to compare Sphinx HTML vs MkDocs HTML
    - Implement LinkValidator to check internal links, external links, and anchors
    - Implement ImageValidator to verify image references and identify screenshots
    - Create validation report generator
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 6.1, 6.2, 6.3, 6.4, 6.5_

  - [x] 1.4 Test conversion locally on sample files
    - Select 5-10 representative RST files from user manual
    - Run conversion on sample files
    - Manually review converted Markdown for quality
    - Verify directive conversions work correctly
    - Adjust directive mappings if needed
    - _Requirements: 1.2, 1.3, 1.4, 5.5, 5.6_

- [ ] 2. Phase 2: 3.0 Branch Conversion (Days 3-5)
  - [x] 2.1 Execute full conversion on 3.0 branch
    - Switch to migration/3.0-rst-to-md branch
    - Run migration.py to convert all RST files in doc/en/user/, doc/en/developer/, doc/en/docguide/
    - Convert Chinese documentation in doc/zhCN/
    - Generate mkdocs.yml configurations with navigation structure
    - Review conversion logs for warnings and errors
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 15.1, 15.2_

  - [x] 2.2 Update mkdocs.yml configurations
    - Merge generated navigation from nav_generated.yml into mkdocs.yml for each manual
    - **NOTE**: Once navigation is merged, remove nav_generated.yml files (they are temporary conversion artifacts)
    - Configure mkdocs-macros-plugin with version and release variables
    - Configure pymdownx extensions (tabbed, superfences, admonition)
    - Configure theme branding (logo, colors, dark mode)
    - Configure version selector with versions: 3.0, 2.28.x, latest
    - Configure PDF generation with mkdocs-with-pdf plugin
    - Configure social links (GitHub, geoserver.org)
    - Update doc/zhCN/mkdocs.yml with Chinese language settings
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 14.1, 14.2, 14.3, 14.4, 14.5, 14.6, 14.7, 15.3, 16.1, 16.2, 16.3, 17.1, 17.2, 17.3, 17.4, 17.6_

  - [x] 2.3 Create build hooks for download files
    - Create hooks/download_files.py with on_pre_build() and on_files() functions
    - Implement scan_download_links() to find download references in Markdown
    - Implement copy_download_files() to copy files from src/ to docs output
    - Configure download_sources in mkdocs.yml extra section
    - Test download links work in built documentation
    - _Requirements: 18.1, 18.2, 18.3, 18.4, 18.5_

  - [x] 2.4 Update GitHub Actions workflow for post-migration builds
    - Modify .github/workflows/mkdocs.yml to remove conversion steps
    - Remove "Install pandoc" step
    - Remove "Install mkdocs-translate" step
    - Remove "Convert RST to Markdown" step
    - Keep "Build all MkDocs sites" step (builds from source Markdown)
    - Keep "Deploy to GitHub Pages" step
    - Update workflow trigger branches to [3.0, 2.28.x]
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7_

  - [x] 2.5 Commit converted files (DO NOT remove RST yet)
    - Stage all converted Markdown files
    - Stage updated mkdocs.yml files
    - Stage hooks/download_files.py
    - Stage updated .github/workflows/mkdocs.yml
    - Commit with message: "Convert 3.0 documentation from RST to Markdown"
    - DO NOT commit temporary conversion files (target/, convert/)
    - DO NOT remove RST files yet (removal happens after validation in Phase 3)
    - _Requirements: 2.1, 2.2, 2.5, 3.8_

- [ ] 3. Phase 3: Validation and Quality Assurance (Days 6-8)
  - [x] 3.1 Build and compare HTML outputs
    - Build Sphinx HTML from original RST files: `cd doc/en && make html`
    - Build MkDocs HTML from converted Markdown: `cd doc/en/user && mkdocs build`
    - Run RoundTripValidator to compare rendered pages side-by-side
    - Identify major rendering problems (tables, missing sections, broken formatting)
    - Document comparison results
    - **COMPLETED**: Identified 2,071 missing images and 128 broken anchors
    - **FIXED**: All 2,071 image path issues resolved (100% success rate)
    - **REMAINING**: 124 broken anchor links need investigation
    - _Requirements: 5.1, 5.2, 5.3, 5.4_

  - [x] 3.1.1 Fix remaining broken anchor links (124 issues)
    - **REMINDER**: 124 broken anchor links remain from task 3.1
    - Investigate anchor generation differences between Sphinx and MkDocs
    - Review auto-generated anchors from headings
    - Fix cross-document references with incorrect syntax
    - Add missing anchors to target documents where needed
    - Verify all internal navigation works correctly
    - _Requirements: 6.1, 6.2, 6.3_

  - [x] 3.2 Manual review of directive conversions
    - Review guilabel conversions (should be bold text)
    - Review menuselection conversions (should be bold with arrow separator)
    - Review file conversions (should be inline code)
    - Review admonition formatting (note, warning, tip)
    - Review code block formatting and syntax highlighting
    - Review conditional content (only snapshot/release → tabbed)
    - **NOTE from Task 1.4 testing:** Conditional content (.. only:: directive) currently converts to admonition blocks (!!! abstract "Release"/"Nightly Build") instead of pymdownx.tabbed syntax (=== "Release"). This is functional but not ideal. Evaluate if manual conversion to tabbed syntax is needed, or if admonition format is acceptable.
    - **NOTE from Task 1.4 testing:** Variable substitutions (|version|, |release|) are NOW AUTOMATICALLY HANDLED by the migration.py postprocessor. The script detects files using {{ version }} or {{ release }} and automatically adds render_macros: true frontmatter. Verify frontmatter was added correctly during review.
    - **NOTE from Task 1.4 testing:** Interpreted text roles (e.g., :website:`text <url>`, :developer:`text <url>`) are NOW AUTOMATICALLY CONVERTED by the migration.py postprocessor. The script converts common roles (website, developer, user, api, geotools, wiki, geos/JIRA, docguide, download_*) to proper Markdown links. **ENHANCEMENT COMPLETED**: Enhanced postprocessor now handles 99%+ of all interpreted text roles automatically (364 roles converted in 2.28.x branch). Only ~54 edge cases remain for manual review. Verify conversions are correct and check for any remaining unknown roles during manual review.
    - Fix critical rendering issues
    - _Requirements: 5.5, 5.6_

  - [x] 3.3 Validate navigation structure
    - Verify navigation hierarchy matches original Sphinx structure
    - Test navigation tabs, sections, and expansion
    - Verify breadcrumbs work correctly
    - Test "Back to top" functionality
    - _Requirements: 5.7, 7.7_

  - [x] 3.4 Validate link integrity
    - Run LinkValidator on built HTML
    - Fix all broken internal links
    - Fix all broken anchor links
    - Verify all image references exist
    - Document any broken external links (don't fix)
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7_

  - [x] 3.5 Test builds and deployment
    - Push migration branch to GitHub
    - Trigger GitHub Actions workflow
    - Verify HTML builds successfully for user, developer, and docguide manuals
    - Verify no build errors or warnings
    - Verify GitHub Pages preview is accessible
    - Test documentation on desktop browsers (Chrome, Firefox, Safari)
    - Test documentation on mobile browsers
    - Verify search functionality works with English queries
    - Verify search functionality works with Chinese queries
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 15.6_

  - [x] 3.5.1 Fix responsive navigation menu overflow on mobile
    - **ISSUE**: Horizontal navigation tabs overflow on mobile/narrow screens, cutting off items after "Production, REST, Secu..."
    - **CAUSE**: navigation.tabs creates horizontal tabs for 17+ top-level items in user manual
    - Investigate Material for MkDocs responsive navigation options
    - Consider solutions:
      - Reorganize navigation hierarchy (group related sections under fewer top-level items)
      - Disable navigation.tabs on mobile using custom CSS media queries
      - Use navigation.sections instead of navigation.tabs for better mobile UX
      - Implement hamburger menu behavior for mobile screens
    - Test solution on desktop (1920px), tablet (768px), and mobile (375px) screen sizes
    - Apply fix to all three manuals (user, developer, docguide)
    - Verify navigation is fully accessible on all screen sizes
    - _Requirements: 7.5, 14.7_

  - [x] 3.5.2 Move images to img subfolders (2.28.x branch)
    - **REQUIREMENT**: All images must be in an img subfolder relative to the calling Markdown file
    - **CORRECT**: docs/eclipse-guide/img/code-template.png
    - **INCORRECT**: docs/eclipse-guide/code-template.png
    - Scan all Markdown files for image references
    - Identify images not in img subfolders
    - Create img subfolders where needed
    - Move images to appropriate img subfolders
    - Update all image references in Markdown files
    - Verify all images display correctly after move
    - Test locally with `mkdocs serve`
    - **NOTE**: This must be completed in 2.28.x branch before continuing to 3.0 branch
    - _Requirements: 6.4, 6.5_

  - [ ] 3.7 Remove RST infrastructure after successful validation
    - Remove doc/en/user/source/ directory (RST files)
    - Remove doc/en/developer/source/ directory (RST files)
    - Remove doc/en/docguide/source/ directory (RST files)
    - Remove doc/zhCN/source/ directory (RST files)
    - Remove doc/en/requirements.txt (Sphinx dependencies)
    - Remove doc/en/build.xml (Ant build script)
    - Update doc/en/pom.xml to remove Sphinx build profiles
    - Remove .github/workflows/docs.yml (old Sphinx workflow)
    - Commit removals with message: "Remove RST infrastructure after migration to Markdown"
    - _Requirements: 20.1, 20.2, 20.3, 20.4, 20.5, 20.6, 20.7_

  - [x] 3.8 Create migration summary report
    - Count total RST files converted
    - Count total lines of documentation converted
    - List unconverted directives or issues
    - Document conversion time
    - List files removed (RST sources, Sphinx config)
    - List files added (Markdown docs, updated workflows)
    - _Requirements: 19.1, 19.2, 19.3, 19.4, 19.5, 19.6_

  - [x] 3.9 Create pull request for 2.28.x branch
    - Create PR: "Migrate 2.28.x documentation from RST to Markdown"
    - Include migration summary report in PR description
    - Document known minor issues
    - Request review from documentation maintainers
    - _Requirements: 2.3, 2.4, 5.9, 19.7_

- [x] 4. Checkpoint - Ensure 2.28.x migration is complete
  - Ensure all validation passes, all tests pass, PR is ready for review. Ask the user if questions arise.

- [ ] 5. Phase 4: 3.0 (main) Branch Conversion (Days 9-11)
  - [x] 5.1 Execute full conversion on 3.0 branch
    - Switch to migration/3.0-rst-to-md branch
    - Run migration.py to convert all RST files in doc/en/user/, doc/en/developer/, doc/en/docguide/
    - **NOTE**: Chinese documentation (doc/zhCN/) is NOT included in 3.0 branch conversion
    - Generate mkdocs.yml configurations with navigation structure
    - Review conversion logs for warnings and errors
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6_

  - [x] 5.2 Update mkdocs.yml configurations for 3.0
    - Apply same configuration as 2.28.x branch (theme, plugins, extensions)
    - Update version variables to 3.0
    - Configure version selector with correct version number
    - **NOTE**: No Chinese language configuration needed (doc/zhCN/ not in 3.0 branch)
    - **NOTE**: PDF generation configuration removed for 3.0 branch
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 16.1, 16.2, 16.3_

  - [x] 5.3 Update GitHub Actions workflow for 3.0
    - Apply same workflow changes as 2.28.x branch
    - Verify workflow triggers on 3.0 branch
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7_

  - [x] 5.4 Commit converted files for 3.0 (DO NOT remove RST yet)
    - Stage all converted Markdown files
    - Stage updated mkdocs.yml files
    - Stage updated .github/workflows/mkdocs.yml
    - Commit with message: "Convert 3.0 documentation from RST to Markdown"
    - _Requirements: 2.1, 2.2, 2.5_

  - [x] 5.4.1 Fix unknown interpreted text roles (NEW - 3.0 specific issue)
    - **CRITICAL**: 219 unknown role occurrences detected (62% unconverted rate)
    - Create fix_unknown_roles.py script with mappings:
      - `nightly_community` → `https://build.geoserver.org/geoserver/main/community-latest/`
      - `nightly_extension` → `https://build.geoserver.org/geoserver/main/ext-latest/`
      - `doc` → relative path (use URL as-is, add .md extension)
      - `abbr` → convert to HTML `<abbr title="definition">text</abbr>`
      - Verify `wiki` and `docguide` mappings work correctly
    - Run fix_unknown_roles.py on all three manuals
    - Validate converted links in sample files
    - Re-run validation to confirm 99%+ conversion success rate
    - Document results in 3.0_conversion_issues.md
    - Commit fixes with message: "Fix 219 unknown interpreted text roles"
    - _Requirements: 5.5, 5.6_

  - [x] 5.5 Validate 3.0 conversion
    - Run same validation steps as 2.28.x branch (HTML comparison, link validation, build tests)
    - Fix any issues specific to 3.0 branch
    - **NOTE**: Unknown interpreted text roles should be fixed in task 5.4.1 before this validation
    - **FIX USER MANUAL INDEX**: Update doc/en/user/docs/index.md to use grid cards format (like developer/docguide manuals) instead of definition list format. The conversion tool incorrectly converted RST definition lists to Markdown definition lists instead of grid cards.
    - **FIX GRID CARD TITLES** (CRITICAL - learned from 2.28.x branch):
      - **Issue**: The conversion tool creates malformed grid card titles by concatenating section names with page titles
      - **Examples of malformed titles**:
        - `[Programming GuideConfigIndex](config/index.md)` should be `[Config](config/index.md)`
        - `[ExtensionsAuthkeyIndex](authkey/index.md)` should be `[Authkey](authkey/index.md)`
        - `[SecurityWebadminSettings](settings.md)` should be `[Settings](settings.md)`
      - **Root cause**: The conversion tool is using RST toctree entry IDs instead of actual page titles
      - **PROPER FIX FOR 3.0**: Modify the conversion tool (migration.py or petersmythe/translate) to extract clean titles from RST source files BEFORE conversion, not after
      - **Temporary workaround**: If proper fix cannot be implemented, run fix_grid_card_titles.py after conversion (fixes 669 titles in 121 files for 2.28.x)
      - **Verification**: Check https://petersmythe.github.io/geoserver/migration/3.0/en/developer/programming-guide/ - menu cards should show "Config", "OWS Services", "REST Services", etc., NOT "Programming GuideConfigIndex", "Programming GuideOws ServicesIndex", etc.
      - **Files typically affected**: All index.md files with `<div class="grid cards">` blocks (218 files total in 2.28.x branch)
    - **FIX MISSING VERSION NUMBERS**: Add version/release macros to index files that were dropped during conversion:
      - doc/en/user/docs/index.md: Add `{{ version }}` after "GeoServer" in intro paragraph
      - doc/en/developer/docs/index.md: Add `{{ version }}` after "GeoServer" in intro paragraph
      - doc/en/docguide/docs/index.md: Add `{{ release }}` after "GeoServer" in intro paragraph
    - **FIX MACRO RENDERING**: Files with `{{ version }}` or `{{ release }}` macros need `render_macros: true` frontmatter:
      - Run fix_macro_rendering.py to add frontmatter to files missing it
      - This fixes index.md files and any other files where macros appear as literal text
      - Verify macros render correctly in built documentation (not showing as `{{ version }}`)
    - **FIX INCLUDE SYNTAX**: The conversion tool may create multi-line `{% include %}` statements that cause macro syntax errors:
      - Multi-line includes like `{% \n  include "path" \n%}` need to be converted to single-line `{% include "path" %}`
      - Run fix_include_syntax.py to convert all multi-line includes to single-line format
      - Run fix_include_paths.py to fix paths to be relative to docs directory (if needed)
      - Verify YSLD reference pages, service vendor option pages, and other pages with includes render correctly
      - Check that included content appears in the rendered pages (not showing as literal `{% include %}` text)
    - **FIX INCLUDE STATEMENT ISSUES** (CRITICAL - learned from 2.28.x branch):
      - **Issue 1: Include statements in code blocks** - Run fix_all_include_issues.py to wrap includes in code blocks with `{%raw%}...{%endraw%}` tags (prevents mkdocs-macros from processing example code)
      - **Issue 2: Include paths outside docs directory** - Script will comment out includes like `../../../../LICENSE.md` (mkdocs-macros cannot include files outside docs directory)
      - **Issue 3: Nested include statements** - Manually fix any `{% {% include %} %}` patterns to `{%raw%}{% include %}{%endraw%}` in code blocks
      - **Issue 4: Include with start/end parameters** - Run fix_include_with_params.py to fix invalid Jinja2 syntax like `{% include "file" start="..." end="..." %}` (Jinja2 does not support start/end parameters)
      - **Files typically affected**: workshop files (css.md, mbstyle.md, ysld.md), configuration examples, developer guide
      - **Verification**: Check GitHub Actions logs for "Macro Syntax Error" or "Macro Rendering Error" messages
    - **FIX TILDE CODE FENCES** (CRITICAL - learned from 2.28.x branch):
      - **Issue**: MkDocs requires backticks (```) for code fences, not tildes (~~~)
      - **Solution**: Run fix_tilde_fences.py to replace all ~~~ with ``` throughout documentation
      - **Files typically affected**: Any files with code blocks (container.md, configuration examples, etc.)
      - **Verification**: Check deployed pages for visible ~~~ characters or malformed code blocks
    - **FIX BLANK HEADER ROW TABLES** (CRITICAL - learned from 2.28.x branch):
      - **Issue**: Tables with blank first row (empty cells) followed by separator row render incorrectly
      - **Root cause**: Conversion tool creates tables with pattern: blank row → separator → actual headers → data
      - **Solution**: Run fix_blank_header_tables.py to remove blank first rows from all tables
      - **Files affected**: 104 files across all three manuals (212 tables total in 2.28.x branch)
      - **Common locations**: SLD reference pages, cookbook examples, workshop tutorials, filter references, service configuration
      - **Verification**: Check that tables display headers correctly without empty rows above them
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7_

  - [x] 5.5.1 Fix image paths and wildcard references for 3.0 branch
    - **REQUIREMENT**: All images must be in an img subfolder relative to the calling Markdown file (same as 2.28.x branch)
    - **CORRECT**: docs/eclipse-guide/img/code-template.png
    - **INCORRECT**: docs/eclipse-guide/code-template.png
    - Run fix_image_paths.py to convert absolute paths to relative
    - Run fix_anchor_case.py to fix anchor case sensitivity
    - Run fix_variable_substitution.py to replace variable placeholders
    - Run fix_overcorrected_paths.py to remove excessive ../ prefixes
    - Run fix_all_image_paths.py for comprehensive multi-strategy fix
    - Run fix_wildcard_images.py to replace wildcard image references with .svg
    - Run fix_ysld_image_paths.py to fix YSLD reference image paths
    - Ensure all images are moved to img subfolders (if not already)
    - Update all image references to point to img subfolders
    - Verify all 2,071 images are fixed (expect 100% success rate)
    - Test locally with `mkdocs serve` to verify images display correctly
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 6.4, 6.5_

  - [x] 5.5.2 Fix missing version/release macros throughout documentation (3.0 branch)
    - **CRITICAL**: The conversion tool dropped |version| and |release| macros in ~79 locations
    - Create automated fix script to restore {{ version }} and {{ release }} macros in:
      - Extension installation instructions (download links, version warnings)
      - Database connector installations (H2, MySQL, Oracle, SQL Server, DB2)
      - Community module installations
      - Service installations (WPS, CSW)
      - Styling extension installations (CSS, YSLD, MBStyle)
    - Common patterns to fix:
      - "example: 2.28.0" → "example: {{ release }}"
      - "example: 2.28.x" → "example: {{ version }}.x"
      - "(for example 2.28.0)" → "(for example {{ release }})"
      - "geoserver-2.28-" → "geoserver-{{ version }}-"
      - "GeoServer 2.28" → "GeoServer {{ version }}"
    - Run check_missing_version_macros.py to identify all missing macros
    - Create and run automated fix script for systematic replacement
    - Verify with check_missing_version_macros.py (should report 0 issues)
    - Test locally that macros render correctly in built documentation
    - _Requirements: 1.3, 1.4, 5.5, 5.6_

  - [ ] 5.6 Remove RST infrastructure for 3.0 after validation
    - Remove all RST source directories and Sphinx configuration
    - Commit removals with message: "Remove RST infrastructure after migration to Markdown"
    - _Requirements: 20.1, 20.2, 20.3, 20.4, 20.5, 20.6, 20.7_

  - [ ] 5.7 Create migration summary report for 3.0
    - Generate same report as 2.28.x branch
    - _Requirements: 19.1, 19.2, 19.3, 19.4, 19.5, 19.6_

  - [ ] 5.8 Create pull request for 3.0 branch
    - Create PR: "Migrate 3.0 documentation from RST to Markdown"
    - Include migration summary report in PR description
    - _Requirements: 2.3, 2.4, 19.7_

- [ ] 6. Checkpoint - Ensure 3.0 migration is complete
  - Ensure all validation passes, all tests pass, PR is ready for review. Ask the user if questions arise.

- [ ] 7. Phase 5: Jenkins Analysis and OSGeo Deployment (Days 12-13)
  - [ ] 7.1 Document Jenkins build process
    - Access Jenkins build logs for documentation builds
    - Document all Jenkins build steps
    - Identify deployment targets used by Jenkins
    - Compare Jenkins steps with GitHub Actions workflow
    - Document any gaps or differences
    - Create Jenkins analysis document
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.7_

  - [ ] 7.2 Update GitHub Actions workflow if gaps found
    - Add any missing critical functionality from Jenkins
    - Test updated workflow
    - _Requirements: 8.6_

  - [ ] 7.3 Configure OSGeo server deployment
    - Add SSH credentials to GitHub Secrets (GEOSERVER_DOCS_SSH_KEY)
    - Add deployment step to .github/workflows/mkdocs.yml
    - Configure deployment to geoserverdocs@geo-docs.geoserver.org:2223
    - Configure remote path: /var/www/geoserverdocs/$VER
    - Configure "latest" symlink for 3.0 branch
    - Replicate Jenkins deployment process (ZIP transfer + unzip)
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6_

  - [ ] 7.4 Test OSGeo deployment on migration branch
    - Deploy to test path on OSGeo server
    - Verify URL structure matches existing docs
    - Verify all files deployed correctly
    - Verify "latest" symlink works
    - _Requirements: 9.6, 9.7_

- [ ] 8. Phase 6: Documentation Updates and Screenshot QA Prep (Day 14)
  - [ ] 8.1 Update Documentation Guide for Markdown
    - Update doc/en/docguide/ with Markdown syntax guide
    - Document Markdown equivalents for RST directives (guilabel, menuselection, file, download, code-block, note, warning, tip, only)
    - Provide examples of each directive conversion
    - Document variable substitution using mkdocs-macros-plugin
    - Document conditional content using pymdownx.tabbed
    - Document local development workflow (mkdocs serve, mkdocs build)
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5, 10.7_

  - [ ] 8.2 Update doc/en/README.md
    - Remove Sphinx build instructions
    - Add MkDocs build instructions
    - Document local development setup
    - _Requirements: 10.6_

  - [ ] 8.3 Test local development workflow
    - Test `mkdocs serve` command for live preview
    - Verify browser auto-reload on file changes
    - Test `mkdocs build` command for local builds
    - Measure and compare build time vs Sphinx
    - Document setup steps in README
    - _Requirements: 13.1, 13.2, 13.3, 13.4, 13.5_

  - [ ] 8.4 Update steering documentation
    - Check if .kiro/steering/ exists
    - If exists, update with migration information
    - Document new MkDocs build process
    - Document how to contribute to Markdown documentation
    - Document GitHub Actions workflow
    - Remove references to Sphinx
    - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5, 11.6_

  - [ ] 8.5 Generate screenshot QA report
    - Run ImageValidator to scan all images in documentation
    - Classify images as screenshots, diagrams, or other
    - Generate screenshot QA report listing all screenshots by page
    - Create tracking spreadsheet for screenshot updates
    - _Requirements: 12.3, 12.4_

  - [ ] 8.6 Brief AfriGIS team on screenshot updates
    - Document how to replace screenshots in Markdown
    - Provide clear instructions for screenshot file paths and naming
    - Explain that ALL screenshots will need updating for GS3 UI
    - Explain Phase 1 begins mid-March when GS3 UI work begins
    - Explain Phase 2 must complete before 15 April release
    - _Requirements: 12.1, 12.2, 12.5, 12.6, 12.7_

- [ ] 9. Final Checkpoint - Migration Complete
  - Ensure all PRs are created, all documentation is updated, all validation passes. Ask the user if questions arise.

## Notes

- This is a project execution workflow, not software development
- Conversion tool (petersmythe/translate) already exists - we're using it, not building it
- RST infrastructure removal happens AFTER validation succeeds (Phase 3, task 3.7)
- Screenshot updates are coordinated but not executed in this migration (separate QA process)
- All tasks reference specific requirements for traceability
- Checkpoints ensure validation at key milestones
- 3.0 branch is converted first, then 2.28.x branch
- Total timeline: 14 days across 6 phases

- [ ] 10. Backport version/release macro fixes to 2.28.x branch (Optional)
  - **OPTIONAL TASK**: After 3.0 branch macro fixes are complete and verified
  - Review the automated fix script created for 3.0 branch (task 5.5.2)
  - Adapt script for 2.28.x branch if needed (version numbers differ)
  - Run check_missing_version_macros.py on 2.28.x branch to identify issues
  - Apply automated fixes to 2.28.x branch
  - Verify with check_missing_version_macros.py (should report 0 issues)
  - Test locally that macros render correctly
  - Commit and push changes to 2.28.x branch
  - Note: This task can be deferred if 2.28.x branch is being deprecated soon
