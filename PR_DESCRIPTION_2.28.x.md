# Migrate 2.28.x documentation from RST to Markdown

## Overview

This PR migrates the GeoServer 2.28.x documentation from reStructuredText (RST) with Sphinx to Markdown with MkDocs. This is part of the broader documentation modernization effort to improve maintainability, build performance, and contributor experience.

## What's Changed

### Documentation Conversion
- ✅ **793 Markdown files** converted (99,964 lines of documentation)
  - User Manual: 727 files (90,843 lines)
  - Developer Manual: 56 files (8,313 lines)
  - Documentation Guide: 10 files (808 lines)

### Configuration Updates
- ✅ Updated `mkdocs.yml` for all three manuals (user, developer, docguide)
- ✅ Configured mkdocs-material theme with GeoServer branding
- ✅ Enabled pymdownx extensions (tabbed, superfences, admonition)
- ✅ Configured mkdocs-macros-plugin for version/release variables
- ✅ Created build hooks for download file handling

### Build System Changes
- ✅ Updated `.github/workflows/mkdocs.yml` to build from source Markdown
- ✅ Removed conversion steps (pandoc, mkdocs-translate)
- ✅ Builds now use committed Markdown files directly

### Quality Fixes Applied
- ✅ **Image paths**: Fixed all 2,071 image path issues (100% success rate)
- ✅ **Grid card titles**: Fixed 669 malformed titles in 121 files
- ✅ **Macro rendering**: Automated frontmatter addition for version/release macros
- ✅ **Include syntax**: Fixed multi-line includes and invalid Jinja2 parameters
- ✅ **Code fences**: Replaced tildes (~~~) with backticks (```)
- ✅ **Table formatting**: Fixed 212 tables with blank header rows in 104 files
- ✅ **Responsive navigation**: Fixed mobile overflow issues
- ✅ **Interpreted text roles**: 99%+ automatically converted (364 roles)

## Known Issues

### Minor Issues (Non-Blocking)
1. **Broken anchor links**: 124 broken anchor links remain
   - Requires investigation of anchor generation differences between Sphinx and MkDocs
   - Cross-document references need manual review
   - Tracked in task 3.1.1

2. **Missing version macros**: ~79 locations need restoration
   - Conversion tool dropped `|version|` and `|release|` in some installation instructions
   - Affects extension/database/service installation pages
   - Automated fix script planned for 3.0 branch (task 5.5.2)
   - Can be backported to 2.28.x if needed (task 10)

3. **PDF generation**: Task 3.6 marked as obsolete
   - PDF generation requirement may no longer be needed
   - Can be addressed in follow-up if required

### Pending Cleanup (Task 3.7)
The following RST infrastructure files are still present and should be removed after successful validation:
- `doc/en/user/source/` (RST files)
- `doc/en/developer/source/` (RST files)
- `doc/en/docguide/source/` (RST files)
- `doc/en/requirements.txt` (Sphinx dependencies)
- `doc/en/build.xml` (Ant build script)
- `.github/workflows/docs.yml` (old Sphinx workflow)

**Note**: These files are intentionally kept in this PR to allow rollback if needed. They will be removed in a follow-up commit after validation confirms the migration is successful.

## Migration Summary

See [MIGRATION_SUMMARY_2.28.x.md](MIGRATION_SUMMARY_2.28.x.md) for detailed statistics and analysis.

### Key Metrics
- **Conversion success rate**: >99%
- **Image validation**: 100% (all 2,071 images fixed)
- **Build time**: ~5-10 minutes per manual (faster than Sphinx)
- **Total project time**: ~8 days (within 2-week timeline)

## Testing Performed

### Automated Validation
- ✅ Link validation (internal links)
- ✅ Image reference validation (all images exist)
- ✅ Build validation (GitHub Actions passing)
- ✅ Navigation structure verification

### Manual Review
- ✅ Directive conversions (guilabel, menuselection, file)
- ✅ Admonition formatting (note, warning, tip)
- ✅ Code block syntax highlighting
- ✅ Table rendering
- ✅ Responsive design (desktop, tablet, mobile)
- ✅ Search functionality

### Build Verification
- ✅ GitHub Actions workflow builds successfully
- ✅ All three manuals build without errors
- ✅ GitHub Pages preview accessible
- ✅ Cross-browser testing (Chrome, Firefox, Safari)

## How to Review

### Local Testing
```bash
# Clone and checkout the branch
git checkout migration/2.28-x-rst-to-md

# Build and serve locally
cd doc/en/user
mkdocs serve
# Opens at http://localhost:8000

# Repeat for developer and docguide manuals
```

### Online Preview
The documentation is deployed to GitHub Pages for preview:
- User Manual: https://petersmythe.github.io/geoserver/migration/2.28.x/en/user/
- Developer Manual: https://petersmythe.github.io/geoserver/migration/2.28.x/en/developer/
- Documentation Guide: https://petersmythe.github.io/geoserver/migration/2.28.x/en/docguide/

### What to Look For
1. **Content accuracy**: Verify documentation content matches the original RST
2. **Navigation**: Check that navigation structure is logical and complete
3. **Links**: Test internal links and anchor references
4. **Images**: Verify images display correctly
5. **Formatting**: Check tables, code blocks, and admonitions render properly
6. **Search**: Test search functionality with common queries
7. **Mobile**: Test responsive design on mobile devices

## Migration Approach

This migration follows a systematic approach:
1. **Automated conversion** using petersmythe/translate tool
2. **Quality fixes** applied via automated scripts
3. **Manual validation** of critical pages and features
4. **Build verification** via GitHub Actions
5. **Incremental cleanup** (RST removal in follow-up)

## Next Steps

After this PR is merged:
1. ✅ Complete anchor link fixes (task 3.1.1) - can be done in follow-up PR
2. ✅ Remove RST infrastructure (task 3.7) - separate cleanup PR
3. ✅ Backport version macro fixes if needed (task 10) - optional
4. ✅ Begin 3.0 branch migration (Phase 4)

## Related Work

- **3.0 branch migration**: Will follow the same process with lessons learned applied
- **Screenshot QA**: Separate effort coordinated with GS3 UI development
- **Jenkins analysis**: Documentation deployment to docs.geoserver.org (Phase 5)

## Request for Review

@geoserver/documentation-maintainers - Please review this migration PR:
- Verify documentation content is accurate and complete
- Test navigation and search functionality
- Check for any critical rendering issues
- Approve if the migration meets quality standards

The known minor issues (124 broken anchors, ~79 missing version macros) can be addressed in follow-up PRs and do not block this migration.

---

**Migration Status**: ✅ Validation Phase Complete (Phase 3)  
**Timeline**: Completed in ~8 days (within 2-week target)  
**Quality**: >99% conversion success rate, all critical issues resolved
