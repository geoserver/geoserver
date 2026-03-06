# Implementation Plan

- [x] 1. Write bug condition exploration test
  - **Property 1: Bug Condition** - Download Links Display Incomplete Text
  - **CRITICAL**: This test MUST FAIL on unfixed code - failure confirms the bug exists
  - **DO NOT attempt to fix the test or the code when it fails**
  - **NOTE**: This test encodes the expected behavior - it will validate the fix when it passes after implementation
  - **GOAL**: Surface counterexamples that demonstrate the bug exists in actual documentation files
  - **Scoped PBT Approach**: Manually inspect known affected files to find concrete failing cases
  - Test that documentation files contain links matching pattern `{{ (release|version|snapshot) }} [plugin-name](URL)` where URL contains `/release/`, `/nightly/`, or `/community/` paths
  - Verify link text does NOT contain "geoserver-" prefix or file extensions (.zip, .war)
  - Document specific examples with file paths and line numbers (e.g., "docs/user/extensions/mbstyle.md line 45: '{{ release }} [mbstyle](...)' should be '{{ release }} geoserver-{{ release }}-mbstyle-plugin.zip'")
  - Run test on UNFIXED code
  - **EXPECTED OUTCOME**: Test FAILS with counterexamples (this is correct - it proves the bug exists)
  - Document at least 3-5 concrete counterexamples covering different link types (extensions, community modules, binaries)
  - Mark task complete when test is written, run, and failures are documented
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

- [-] 2. Write preservation property tests (BEFORE implementing fix)
  - **Property 2: Preservation** - Non-Download Link Behavior
  - **IMPORTANT**: Follow observation-first methodology
  - Observe behavior on UNFIXED code for non-buggy inputs (links that don't match bug condition)
  - Identify and document examples of links that should remain unchanged:
    - Internal documentation links (e.g., `[configuration](../config.md)`)
    - External reference links (e.g., `[OGC WMS](https://www.ogc.org/...)`)
    - Links in code blocks
    - Download links already in correct format
  - Write property-based test: for all links where isBugCondition returns false, content remains identical after fix
  - Test approach: Create git commit/backup before fix, use `git diff` after fix to verify only buggy links changed
  - Run tests on UNFIXED code
  - **EXPECTED OUTCOME**: Tests PASS (this confirms baseline behavior to preserve)
  - Mark task complete when tests are written, run, and passing on unfixed code
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

- [ ] 3. Fix for broken download links across documentation

  - [ ] 3.1 Create Python fix script
    - Create `scripts/fix_download_links.py`
    - Implement regex patterns to detect buggy download links matching `{{ (release|version|snapshot) }} [plugin-name](URL)` where URL contains download paths
    - Implement link type classification logic (extensions, community modules, WAR, bin, database connectors)
    - Implement link text reconstruction to generate full filename patterns:
      - Extensions/Community: `geoserver-{{ macro }}-{name}-plugin.zip`
      - WAR binaries: `geoserver-{{ macro }}.war`
      - Bin binaries: `geoserver-{{ macro }}-bin.zip`
    - Implement file processing to scan `docs/` directory recursively for `.md` files
    - Apply fixes only to lines matching bug condition, preserve all other content
    - Preserve URLs unchanged, only modify link text
    - _Bug_Condition: isBugCondition(link) where link.linkText MATCHES_PATTERN '{{ (release|version|snapshot) }} [a-zA-Z0-9-_]+' AND link.url CONTAINS '/release/' OR '/nightly/' OR '/community/' AND NOT link.linkText CONTAINS 'geoserver-' AND NOT link.linkText CONTAINS '.zip' OR '.war'_
    - _Expected_Behavior: fixed_link.linkText CONTAINS 'geoserver-' AND macro_variable AND plugin_name AND appropriate_extension, with URL unchanged_
    - _Preservation: All links where NOT isBugCondition(link) remain completely unchanged_
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 2.1, 2.2, 2.3, 2.4, 2.5, 3.1, 3.2, 3.3, 3.4, 3.5_

  - [ ] 3.2 Add unit tests for fix script
    - Test regex pattern matching for each download link type
    - Test link text reconstruction for extensions, community modules, WAR, and bin downloads
    - Test that non-matching links are ignored by fix logic
    - Test edge cases (links with special characters, multi-line links)
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

  - [ ] 3.3 Run fix script on documentation
    - Activate Python virtual environment: `source .venv/Scripts/activate`
    - Create git commit or backup before running script
    - Execute: `python scripts/fix_download_links.py`
    - Review script output for number of files processed and links fixed
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 2.1, 2.2, 2.3, 2.4, 2.5_

  - [ ] 3.4 Verify bug condition exploration test now passes
    - **Property 1: Expected Behavior** - Download Links Display Full Filenames
    - **IMPORTANT**: Re-run the SAME test from task 1 - do NOT write a new test
    - The test from task 1 encodes the expected behavior
    - When this test passes, it confirms the expected behavior is satisfied
    - Re-examine the same files and line numbers documented in task 1
    - Verify link text now contains full filename pattern with "geoserver-", macro variable, plugin name, and file extension
    - **EXPECTED OUTCOME**: Test PASSES (confirms bug is fixed)
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

  - [ ] 3.5 Verify preservation tests still pass
    - **Property 2: Preservation** - Non-Download Link Behavior
    - **IMPORTANT**: Re-run the SAME tests from task 2 - do NOT write new tests
    - Use `git diff` to examine all changes
    - Verify only buggy download links were modified
    - Verify internal documentation links remain unchanged
    - Verify external reference links remain unchanged
    - Verify code blocks remain unchanged
    - Verify prose content remains identical
    - **EXPECTED OUTCOME**: Tests PASS (confirms no regressions)
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

- [ ] 4. Checkpoint - Ensure all tests pass and documentation builds correctly
  - Build documentation with MkDocs to verify links render correctly
  - Verify macro variables are properly substituted in built documentation
  - Manually test several fixed download links to verify they work
  - Ensure all tests pass, ask the user if questions arise

- [ ] 5. Apply fix to 2.28.x branch
  - [ ] 5.1 Commit the fix on main branch
    - Stage all changes: `git add scripts/fix_download_links.py doc/`
    - Commit with clear message: `git commit -m "Fix broken download links in documentation"`
    - Note the commit hash for cherry-picking
    - _Requirements: All_

  - [ ] 5.2 Cherry-pick fix to 2.28.x branch
    - Switch to 2.28.x branch: `git checkout 2.28.x`
    - Cherry-pick the fix commit: `git cherry-pick <commit-hash>`
    - If cherry-pick succeeds without conflicts, proceed to task 5.3
    - If merge conflicts occur, proceed to task 5.2.1
    - _Requirements: All_

  - [ ] 5.2.1 Resolve merge conflicts (if any)
    - Review conflict markers in affected files
    - Common conflict scenarios:
      - File paths may differ between branches (e.g., different directory structure)
      - Version numbers in examples may differ (3.0.x vs 2.28.x)
      - Some files may exist in one branch but not the other
    - Resolve conflicts by:
      - Keeping the fix logic (full filename patterns in link text)
      - Adapting to 2.28.x file paths and structure
      - Ensuring version macros match 2.28.x conventions
    - Stage resolved files: `git add <resolved-files>`
    - Continue cherry-pick: `git cherry-pick --continue`
    - _Requirements: All_

  - [ ] 5.3 Re-run tests on 2.28.x branch
    - Re-run bug condition exploration test (Task 1) on 2.28.x files
    - Verify link text now contains full filename patterns
    - Re-run preservation tests (Task 2) on 2.28.x files
    - Use `git diff` to verify only download links were modified
    - **EXPECTED OUTCOME**: All tests PASS on 2.28.x branch
    - _Requirements: All_

  - [ ] 5.4 Build and verify 2.28.x documentation
    - Build 2.28.x documentation with MkDocs
    - Verify links render correctly with proper version numbers (2.28.x)
    - Verify macro variables are properly substituted
    - Manually test several fixed download links
    - Compare rendered output with main branch to ensure consistency
    - _Requirements: All_

  - [ ] 5.5 Commit 2.28.x changes (if manual adjustments were needed)
    - If cherry-pick required conflict resolution or manual adjustments, commit those changes
    - Use commit message: `git commit -m "Fix broken download links in 2.28.x documentation"`
    - If cherry-pick was clean, no additional commit needed
    - _Requirements: All_
