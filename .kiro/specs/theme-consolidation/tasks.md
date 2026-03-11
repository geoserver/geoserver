# Theme Consolidation Tasks

## Overview

Consolidate duplicated GeoServer documentation theme files into a single source of truth to eliminate maintenance burden and prevent inconsistencies.

## Task Breakdown

### Phase 1: Analysis and Verification

- [x] 1.1 Audit All Theme Files
  - Priority: high
  - Estimated effort: 30 minutes
  - Description: Create a complete inventory of all theme-related files and their locations
  - Steps:
    1. List all files in `doc/themes/geoserver/`
    2. List all files in `doc/en/themes/geoserver/`
    3. List all theme files in `doc/en/{user,developer,docguide}/docs/`
    4. Compare file contents using checksums to identify true duplicates
    5. Identify any doc-specific customizations
    6. Document findings in a comparison matrix
  - Acceptance Criteria:
    - Complete file inventory created
    - Duplicate files identified with checksums
    - Doc-specific files documented
    - Comparison matrix shows which files are identical

- [x] 1.2 Verify Current mkdocs.yml Paths
  - Priority: high
  - Estimated effort: 15 minutes
  - Description: Verify that all mkdocs.yml files have correct theme paths
  - Steps:
    1. Check `doc/en/user/mkdocs.yml` - should have `custom_dir: ../../themes/geoserver`
    2. Check `doc/en/developer/mkdocs.yml` - should have `custom_dir: ../../themes/geoserver`
    3. Check `doc/en/docguide/mkdocs.yml` - should have `custom_dir: ../../themes/geoserver`
    4. Verify paths resolve correctly from each location
    5. Document any discrepancies
  - Acceptance Criteria:
    - All three mkdocs.yml files verified
    - Path correctness confirmed
    - Any issues documented

- [x] 1.3 Test Current Build Process
  - Priority: high
  - Estimated effort: 30 minutes
  - Description: Establish baseline by building all three documentation types and verifying they work correctly
  - Steps:
    1. Build user manual: `cd doc/en/user && mkdocs build`
    2. Build developer manual: `cd doc/en/developer && mkdocs build`
    3. Build docguide: `cd doc/en/docguide && mkdocs build`
    4. Verify all builds succeed
    5. Check that documentation switcher appears in all three
    6. Verify all theme elements render correctly
    7. Take screenshots for comparison
  - Acceptance Criteria:
    - All three builds complete successfully
    - Documentation switcher visible and functional
    - All theme elements present
    - Baseline screenshots captured

### Phase 2: Consolidation Implementation

- [x] 2.1 Remove Duplicate extra.css Files
  - Priority: high
  - Estimated effort: 15 minutes
  - Description: Remove the duplicate extra.css files from individual documentation directories
  - Steps:
    1. Verify `doc/themes/geoserver/stylesheets/extra.css` is the canonical version
    2. Delete `doc/en/user/docs/stylesheets/extra.css`
    3. Delete `doc/en/developer/docs/stylesheets/extra.css`
    4. Delete `doc/en/docguide/docs/stylesheets/extra.css`
    5. Commit changes with descriptive message
  - Acceptance Criteria:
    - Three duplicate files removed
    - Canonical file remains at `doc/themes/geoserver/stylesheets/extra.css`
    - Changes committed to git

- [x] 2.2 Evaluate doc/en/themes/geoserver/
  - Priority: medium
  - Estimated effort: 20 minutes
  - Description: Determine if `doc/en/themes/geoserver/` is needed or should be removed
  - Steps:
    1. Compare contents with `doc/themes/geoserver/`
    2. Check if any mkdocs.yml references this location
    3. Search for any build scripts that reference this path
    4. If unused, remove the directory
    5. If used, document why and ensure it's not duplicating
  - Acceptance Criteria:
    - Usage of `doc/en/themes/geoserver/` determined
    - Decision made: keep or remove
    - If removed, directory deleted and committed
    - If kept, purpose documented

- [x] 2.3 Handle Doc-Specific Customizations
  - Priority: medium
  - Estimated effort: 30 minutes
  - Description: Properly handle any documentation-specific theme files (like docguide's pagelogo.png)
  - Steps:
    1. Identify all doc-specific files (e.g., `doc/en/docguide/docs/img/pagelogo.png`)
    2. Determine if they should remain doc-specific or be generalized
    3. If doc-specific, ensure they're in the correct location for MkDocs overrides
    4. If generalizable, move to shared theme
    5. Document the override mechanism
    6. Update any references in markdown files
  - Acceptance Criteria:
    - All doc-specific files identified
    - Decision made for each file
    - Files moved to appropriate locations
    - Override mechanism documented
    - References updated

### Phase 3: Testing and Verification

- [x] 3.1 Build and Test All Documentation Types
  - Priority: high
  - Estimated effort: 45 minutes
  - Description: Build all three documentation types and verify everything works correctly
  - Steps:
    1. Clean all previous builds: `rm -rf doc/en/*/target`
    2. Build user manual: `cd doc/en/user && mkdocs build`
    3. Build developer manual: `cd doc/en/developer && mkdocs build`
    4. Build docguide: `cd doc/en/docguide && mkdocs build`
    5. Verify all builds succeed without errors
    6. Check build output for any warnings
    7. Verify file sizes are reasonable (no bloat from duplicates)
  - Acceptance Criteria:
    - All three builds complete successfully
    - No build errors or warnings
    - Build output sizes are reasonable
    - No missing file errors

- [x] 3.2 Visual Regression Testing
  - Priority: high
  - Estimated effort: 30 minutes
  - Description: Verify that all visual elements render correctly after consolidation
  - Steps:
    1. Open each built documentation in a browser
    2. Check documentation switcher appears and works
    3. Verify logo and favicon display correctly
    4. Check custom CSS is applied (mobile navigation, dropdown styling)
    5. Test on different screen sizes (desktop, tablet, mobile)
    6. Compare with baseline screenshots from Task 1.3
    7. Test dropdown hover behavior (0.8s delay)
    8. Verify dark mode toggle works
  - Acceptance Criteria:
    - Documentation switcher visible and functional on all three types
    - Logo and favicon display correctly
    - Custom CSS applied correctly
    - Responsive behavior works
    - No visual regressions from baseline
    - Dark mode works

- [ ] 3.3 Test Local Development Workflow
  - Priority: medium
  - Estimated effort: 20 minutes
  - Description: Verify that local development with `mkdocs serve` works correctly
  - Steps:
    1. Start dev server for user manual: `cd doc/en/user && mkdocs serve`
    2. Verify hot reload works when editing theme files
    3. Stop server and test developer manual
    4. Stop server and test docguide
    5. Verify theme changes propagate to all docs
    6. Test editing `doc/themes/geoserver/stylesheets/extra.css` and verify it updates
  - Acceptance Criteria:
    - Dev server starts successfully for all three types
    - Hot reload works for theme changes
    - Theme changes visible in all documentation types
    - No file watching errors

### Phase 4: Documentation and Cleanup

- [ ] 4.1 Update Build Documentation
  - Priority: medium
  - Estimated effort: 30 minutes
  - Description: Document the theme structure and build process
  - Steps:
    1. Create or update `doc/themes/geoserver/README.md`
    2. Document theme structure and components
    3. Explain how theme is shared across documentation types
    4. Document how to make theme changes
    5. Explain override mechanism for doc-specific customizations
    6. Add troubleshooting section
    7. Update main documentation build guide if needed
  - Acceptance Criteria:
    - README.md created/updated in theme directory
    - Theme structure documented
    - Change process documented
    - Override mechanism explained
    - Troubleshooting guide included

- [ ] 4.2 Create Verification Script
  - Priority: low
  - Estimated effort: 45 minutes
  - Description: Create a script to verify theme files are not duplicated
  - Steps:
    1. Create `doc/verify_theme_consolidation.sh` or `.py`
    2. Script should check for duplicate theme files
    3. Script should verify mkdocs.yml paths are correct
    4. Script should report any issues
    5. Add script to CI/CD if applicable
    6. Document how to run the script
  - Acceptance Criteria:
    - Verification script created
    - Script detects duplicate theme files
    - Script verifies mkdocs.yml paths
    - Script provides clear error messages
    - Script documented

- [ ] 4.3 Test on GitHub Pages
  - Priority: high
  - Estimated effort: 30 minutes
  - Description: Verify that GitHub Pages deployment works correctly with consolidated theme
  - Steps:
    1. Push changes to a test branch
    2. Trigger GitHub Pages build
    3. Wait for deployment to complete
    4. Visit deployed documentation URLs
    5. Verify all three documentation types are accessible
    6. Test documentation switcher on deployed site
    7. Check browser console for any errors
    8. Verify all assets load correctly (CSS, images)
  - Acceptance Criteria:
    - GitHub Pages build succeeds
    - All three documentation types deploy correctly
    - Documentation switcher works on deployed site
    - No console errors
    - All assets load correctly

### Phase 5: Finalization

- [ ] 5.1 Code Review and Testing
  - Priority: high
  - Estimated effort: 30 minutes
  - Description: Final review and testing before merging
  - Steps:
    1. Review all changes in git diff
    2. Verify no unintended files were deleted
    3. Check commit messages are descriptive
    4. Run full build test one more time
    5. Test on a clean checkout to ensure no local dependencies
    6. Get peer review if available
  - Acceptance Criteria:
    - All changes reviewed
    - No unintended deletions
    - Commit messages clear
    - Full build test passes
    - Clean checkout test passes

- [ ] 5.2 Merge and Monitor
  - Priority: high
  - Estimated effort: 20 minutes
  - Description: Merge changes and monitor for issues
  - Steps:
    1. Merge to main branch (or appropriate version branch)
    2. Monitor GitHub Pages deployment
    3. Check deployed site immediately after deployment
    4. Monitor for any issue reports
    5. Be ready to rollback if critical issues found
  - Acceptance Criteria:
    - Changes merged successfully
    - GitHub Pages deployment succeeds
    - Deployed site works correctly
    - No critical issues reported

## Summary

**Total estimated effort**: ~6 hours

**Critical path**:
1. Phase 1: Analysis (1.25 hours)
2. Phase 2: Implementation (1 hour)
3. Phase 3: Testing (1.5 hours)
4. Phase 4: Documentation (1.75 hours)
5. Phase 5: Finalization (0.75 hours)

**Key risks**:
- Build failures due to incorrect paths
- GitHub Pages deployment issues
- Visual regressions
- Missing theme files breaking appearance

**Rollback plan**:
- Keep backup of current state before starting
- Use git to revert changes if critical issues found
- Have previous working commit hash documented
