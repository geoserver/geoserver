# Design Document: RST to Markdown Migration

## Overview

This design document specifies the execution plan for migrating GeoServer documentation from reStructuredText (RST) with Sphinx to Markdown with MkDocs. This is a **one-time migration project** targeting GeoServer 3.0 and 2.28.x branches. The conversion tooling already exists (petersmythe/translate and .github/workflows/mkdocs.yml), and this project focuses on executing the migration, validating results, and managing the transition.

### Goals

- Execute one-time RST to Markdown conversion on 3.0 and 2.28.x branches
- Commit converted Markdown files to repository
- Remove RST source files and Sphinx build infrastructure
- Update GitHub Actions workflow to build from source MD (remove conversion step)
- Validate conversion quality and fix issues
- Coordinate screenshot QA with GS3 UI development timeline
- Complete migration within 2-week timeline

### Non-Goals

- Building new conversion software (petersmythe/translate already exists)
- Migrating other branches beyond 3.0 and 2.28.x
- Rewriting documentation content
- Changing documentation structure or organization

### Key Design Decisions

1. **Use Existing Tooling**: Leverage existing .github/workflows/mkdocs.yml conversion process
2. **Branch-by-Branch Execution**: Convert 3.0 first, validate, then 2.28.x
3. **Commit Strategy**: Single PR per branch with all converted files
4. **Validation Approach**: Manual review + automated link checking
5. **Screenshot QA**: Two-phase approach aligned with GS3 UI timeline

## Current State Analysis

### Existing Infrastructure

**Conversion Tool**: petersmythe/translate (https://github.com/petersmythe/translate)
- Installed via: `pip install git+https://github.com/petersmythe/translate.git`
- Commands: `mkdocs_translate init`, `scan`, `migrate`, `nav`
- Handles directive mapping, link conversion, navigation generation

**GitHub Actions Workflow**: `.github/workflows/mkdocs.yml`
- Currently converts RST to MD on-the-fly for preview
- Steps:
  1. Install pandoc 3.5
  2. Setup Python virtualenv
  3. Install mkdocs-translate
  4. Convert RST to Markdown (init, scan, migrate, nav)
  5. Build MkDocs sites
  6. Deploy to GitHub Pages for preview
- Processes: doc/en/docguide, doc/en/developer, doc/en/user

**MkDocs Configuration**: mkdocs.yml files already exist
- doc/en/user/mkdocs.yml
- doc/en/developer/mkdocs.yml
- doc/en/docguide/mkdocs.yml

**Sphinx Infrastructure** (to be removed):
- .github/workflows/docs.yml (Sphinx build)
- doc/en/requirements.txt (Sphinx dependencies)
- doc/en/build.xml (Ant build script)
- doc/en/pom.xml (Maven configuration)
- doc/en/*/source/ directories (RST files)

### Target Branches

**3.0 Branch**:
- Primary target for GeoServer 3.0 release (15 April)
- Will receive new GS3 UI screenshots after UI completion
- Convert first, validate thoroughly

**2.28.x Branch**:
- Maintenance branch
- Convert second after 3.0 validation
- Screenshots remain unchanged (existing UI)

## Migration Execution Plan

### Phase 1: Preparation (Days 1-2)

**Objective**: Set up migration environment and validate tooling

**Activities**:

1. **Create Migration Branch**
   - Branch from 3.0: `migration/3.0-rst-to-md`
   - Branch from 2.28.x: `migration/2.28.x-rst-to-md`

2. **Test Conversion Locally**
   - Clone repository
   - Run existing mkdocs.yml workflow steps manually
   - Verify conversion produces valid Markdown
   - Document any issues or warnings

3. **Prepare Validation Scripts**
   - Link checker script
   - Image reference validator
   - Screenshot inventory script

4. **Document Current State**
   - Count RST files per documentation type
   - List Sphinx-specific files to remove
   - Identify custom Sphinx extensions in use

**Deliverables**:
- Migration branches created
- Local conversion tested and documented
- Validation scripts ready
- Current state inventory complete

### Phase 2: 3.0 Branch Conversion (Days 3-5)

**Objective**: Execute conversion on 3.0 branch and commit results

**Activities**:

1. **Execute Conversion**
   ```bash
   # For each doc type: user, developer, docguide
   cd doc/en/user
   
   # Run conversion steps (from mkdocs.yml workflow)
   mkdocs_translate init
   mkdocs_translate scan
   mkdocs_translate migrate
   mkdocs_translate nav > nav_generated.yml
   
   # Merge generated nav into mkdocs.yml
   # Review and commit converted docs/ directory
   ```

2. **Update mkdocs.yml Files**
   - Merge generated navigation from nav_generated.yml
   - Configure mkdocs-macros-plugin for variables
   - Add pymdownx extensions for tabs, admonitions
   - Configure theme and branding

3. **Remove RST Infrastructure**
   - Delete doc/en/*/source/ directories
   - Remove doc/en/requirements.txt (Sphinx deps)
   - Remove doc/en/build.xml
   - Update doc/en/pom.xml (remove Sphinx profiles)

4. **Update GitHub Actions**
   - Modify .github/workflows/mkdocs.yml:
     - Remove "Convert RST to Markdown" step
     - Remove pandoc installation
     - Remove mkdocs-translate installation
     - Keep "Build all MkDocs sites" step (builds from source MD)
   - Remove .github/workflows/docs.yml (Sphinx workflow)

5. **Commit Changes**
   - Commit all converted MD files
   - Commit updated mkdocs.yml files
   - Commit removed RST files
   - Commit updated workflows
   - Create PR: "Migrate 3.0 documentation from RST to Markdown"

**Deliverables**:
- Converted Markdown files in doc/en/*/docs/
- Updated mkdocs.yml with navigation
- RST files and Sphinx infrastructure removed
- GitHub Actions workflow updated
- PR created for review

### Phase 3: Validation and Fixes (Days 6-8)

**Objective**: Validate conversion quality and fix issues

**Activities**:

1. **Automated Validation**
   - Run link checker on built HTML
   - Validate all image references
   - Check for broken anchors
   - Verify code blocks render correctly

2. **Manual Review**
   - Review directive conversions (guilabel, menuselection, file)
   - Check admonition formatting (note, warning, tip)
   - Verify tables render correctly
   - Test search functionality
   - Review navigation structure

3. **Fix Issues**
   - Fix broken links
   - Correct malformed tables
   - Adjust directive formatting
   - Update navigation if needed

4. **Build Verification**
   - Trigger GitHub Actions workflow
   - Verify HTML builds successfully
   - Check GitHub Pages preview
   - Test on mobile/tablet

5. **Documentation Updates**
   - Update doc/en/README.md (remove Sphinx instructions)
   - Update doc/en/docguide/ (Markdown syntax guide)
   - Document Markdown equivalents for RST directives

**Deliverables**:
- Validation report with issues identified
- All critical issues fixed
- Documentation guide updated for Markdown
- Build passing on GitHub Actions

### Phase 4: 2.28.x Branch Conversion (Days 9-11)

**Objective**: Execute conversion on 2.28.x branch

**Activities**:

1. **Execute Conversion**
   - Repeat Phase 2 steps for 2.28.x branch
   - Use lessons learned from 3.0 conversion
   - Apply same fixes proactively

2. **Validation**
   - Run automated validation
   - Manual spot-check (less thorough than 3.0)
   - Fix critical issues only

3. **Commit and PR**
   - Create PR: "Migrate 2.28.x documentation from RST to Markdown"

**Deliverables**:
- 2.28.x branch converted to Markdown
- PR created for review

### Phase 5: Jenkins Analysis (Days 12-13)

**Objective**: Document Jenkins build process and ensure GitHub Actions equivalence

**Activities**:

1. **Analyze Jenkins Workflow**
   - Access Jenkins build logs
   - Document build steps
   - Identify deployment targets
   - Note any custom scripts or plugins

2. **Compare with GitHub Actions**
   - Map Jenkins steps to GitHub Actions
   - Identify gaps or differences
   - Document deployment process

3. **Update GitHub Actions if Needed**
   - Add missing functionality
   - Configure deployment to docs.geoserver.org
   - Set up version selector

**Deliverables**:
- Jenkins workflow documentation
- GitHub Actions comparison report
- Updated workflow if needed

    versions:
      - version: latest
        title: Latest (3.0)
        aliases: []
      - version: "3.0"

1. **Generate Screenshot Inventory**
   - Scan all Markdown files for images
   - Classify as screenshot vs diagram
   - Generate report by page

2. **Coordinate with AfriGIS**
   - Share screenshot inventory
   - Explain QA process
   - Set expectations for Phase 1 (initial) and Phase 2 (final)

3. **Create Screenshot Update Workflow**
   - Document how to replace screenshots
   - Create checklist for QA team
   - Set up tracking spreadsheet

**Deliverables**:
- Screenshot inventory report
- QA workflow documentation
- AfriGIS team briefed

## Post-Migration Workflow

### GitHub Actions Build (Post-Migration)

After migration, the workflow builds from source Markdown:

```yaml
name: Build Documentation

on:
  push:
    branches: [3.0, 2.28.x]
    paths:
      - 'doc/**'
  pull_request:
    paths:
      - 'doc/**'

jobs:
  build:
    runs-on: ubuntu-22.04
    steps:
      - uses: actions/checkout@v4
      
      - name: Setup Python
on:
  push:
    branches: [3.0, 2.28.x]
    paths:
      - 'doc/**'stall MkDocs dependencies
        run: |
          pip install mkdocs mkdocs-material mkdocs-macros-plugin
          
      - name: Build User Manual
        run: |
          cd doc/en/user
          mkdocs build --strict
          
      - name: Build Developer Manual
        run: |
          cd doc/en/developer
          mkdocs build --strict
          
      - name: Build Documentation Guide
        run: |
          cd doc/en/docguide
          mkdocs build --strict
          
      - name: Deploy to GitHub Pages
        if: github.ref == 'refs/heads/3.0' || github.ref == 'refs/heads/2.28.x'
        uses: peaceiris/actions-gh-pages@v3
        with:
          github_token: ${{ secrets.GITHUB_TOKEN }}
          publish_dir: ./gh-pages-output
```

**Key Changes from Current Workflow**:
- ❌ Remove: Install pandoc
- ❌ Remove: Install mkdocs-translate
- ❌ Remove: "Convert RST to Markdown" step
- ✅ Keep: Build MkDocs sites
- ✅ Keep: Deploy to GitHub Pages

### Documentation Authoring (Post-Migration)

**Local Development**:
```bash
cd doc/en/user
mkdocs serve
# Opens browser with live preview at http://localhost:8000
```

**Editing Workflow**:
1. Edit Markdown files in docs/ directory
2. Preview changes with `mkdocs serve`
3. Commit changes
4. Push to branch
      - name: Deploy to GitHub Pages
        if: github.ref == 'refs/heads/3.0' || github.ref == 'refs/heads/2.28.x'
        uses: peaceiris/actions-gh-pages@v3ocguide):

| RST Directive | Markdown Equivalent |
|--------------|---------------------|
| `:guilabel:`Label`` | `**Label**` |
| `:menuselection:`Menu --> Item`` | `**Menu → Item**` |
| `:file:`path/to/file`` | `` `path/to/file` `` |
| `.. note::` | `!!! note` |
| `.. warning::` | `!!! warning` |
| `.. code-block:: python` | ` ```python ` |
| `\|version\|` | `{{ version }}` |

## Screenshot QA Process

### Phase 1: Initial QA (Mid-March)

**Timing**: After GS3 UI work begins

**Objective**: Identify all screenshots that need updating

**Activities**:
1. AfriGIS team reviews screenshot inventory
2. Mark screenshots as "needs update" or "OK"
3. Prioritize critical screenshots
4. Create tracking spreadsheet

**Deliverables**:
- Screenshot QA tracking spreadsheet
- Priority list for updates

### Phase 2: Final QA (After GS3 UI Completion)

**Timing**: As soon as GS3 UI is completed, before 15 April release

**Objective**: Replace all outdated screenshots with new GS3 UI

**Activities**:
1. AfriGIS team captures new screenshots
2. Replace images in doc/en/*/docs/images/
3. Verify image references in Markdown
4. Commit updated screenshots
5. Review and merge

**Deliverables**:
- All screenshots updated for GS3 UI
- Changes committed and merged before 15 April

## Validation Strategy

### Automated Validation

**Link Validation**:
```python
# scripts/validate_links.py
def validate_internal_links(site_dir):
    """Check all internal links resolve"""
    # Parse HTML files
    # Extract all <a href> links
    # Verify target files exist
    # Report broken links

def validate_anchors(site_dir):
    """Check section anchors exist"""
    # Extract anchor links (#section)
    # Verify target sections exist
    # Report broken anchors
```

**Image Validation**:
```python
# scripts/validate_images.py
def validate_image_references(docs_dir):
    """Check all image references exist"""
    # Parse Markdown files
    # Extract image references
    # Verify image files exist
    # Report missing images

def generate_screenshot_inventory(docs_dir):
    """Generate screenshot inventory"""
    # Find all images
    # Classify as screenshot/diagram
    # Generate report by page
```

### Manual Validation Checklist

**Content Review**:
- [ ] All pages render correctly
- [ ] Navigation structure is logical
- [ ] Search returns relevant results
- [ ] Code blocks have syntax highlighting
- [ ] Tables are formatted correctly
- [ ] Admonitions (note, warning, tip) display properly

**Directive Conversion**:
- [ ] guilabel formatting is consistent
- [ ] menuselection uses correct separator
- [ ] file paths are monospaced
- [ ] download links work
- [ ] Variables (version, release) resolve correctly

**Cross-Browser Testing**:
- [ ] Chrome/Edge
- [ ] Firefox
- [ ] Safari
- [ ] Mobile browsers

## Risk Mitigation

### Risk 1: Conversion Quality Issues

**Risk**: Automated conversion produces malformed Markdown

**Mitigation**:
- Test conversion locally before committing
- Manual review of critical pages
- Automated validation scripts
- Keep RST files in git history for reference

**Rollback**: Revert PR and fix issues before re-attempting

### Risk 2: Timeline Overrun

**Risk**: Conversion takes longer than 2 weeks

**Mitigation**:
- Start with 3.0 branch (highest priority)
- Parallelize validation and 2.28.x conversion
- Focus on critical issues only
- Defer non-critical fixes to follow-up PRs

**Contingency**: Complete 3.0 first, defer 2.28.x if needed

### Risk 3: Screenshot QA Delays

**Risk**: GS3 UI completion delays screenshot updates

**Mitigation**:
- Screenshot QA is independent of migration
- Can update screenshots after migration complete
- Phase 1 QA can start during migration
- Clear communication with AfriGIS team

**Contingency**: Release with old screenshots, update in patch release

### Risk 4: Build Failures

**Risk**: GitHub Actions workflow fails after migration

**Mitigation**:
- Test workflow on migration branch before merging
- Keep preview workflow running during migration
- Validate builds locally before pushing
- Monitor build logs closely

**Rollback**: Revert workflow changes, fix issues

## Success Criteria

### Migration Complete When:

1. ✅ All RST files converted to Markdown on 3.0 and 2.28.x
2. ✅ Converted files committed to repository
3. ✅ RST source files and Sphinx infrastructure removed
4. ✅ GitHub Actions workflow updated (no conversion step)
5. ✅ Builds passing on GitHub Actions
6. ✅ Documentation accessible on GitHub Pages
7. ✅ Link validation passing (no broken links)
8. ✅ Image validation passing (no missing images)
9. ✅ Documentation guide updated for Markdown
10. ✅ Jenkins workflow documented
11. ✅ Screenshot inventory generated
12. ✅ AfriGIS team briefed on QA process

### Quality Metrics:

- **Conversion Success Rate**: >99% of files convert without errors
- **Link Validation**: 0 broken internal links
- **Image Validation**: 0 missing images
- **Build Time**: <5 minutes per documentation type
- **Search Quality**: Relevant results for common queries

## Timeline Summary

| Phase | Days | Activities | Deliverables |
|-------|------|-----------|--------------|
| 1. Preparation | 1-2 | Setup, testing, validation scripts | Migration branches, scripts ready |
| 2. 3.0 Conversion | 3-5 | Convert, commit, update workflows | 3.0 PR created |
| 3. Validation | 6-8 | Validate, fix issues, update docs | Issues fixed, build passing |
| 4. 2.28.x Conversion | 9-11 | Convert, validate, commit | 2.28.x PR created |
| 5. Jenkins Analysis | 12-13 | Document Jenkins, compare workflows | Jenkins documentation |
| 6. Screenshot QA Prep | 14 | Generate inventory, brief AfriGIS | Screenshot inventory, QA workflow |

**Total Duration**: 14 days (2 weeks)

**Critical Path**: 3.0 conversion → validation → 2.28.x conversion

**Parallel Activities**: Jenkins analysis can happen during validation phase


## Jenkins Build Process Analysis

### Current Jenkins Workflow

The Jenkins job at https://build.geoserver.org/job/geoserver-main-docs/ performs the following:

**1. Build Documentation**
- Maven builds API documentation
- Ant calls Sphinx to build user/developer/docguide HTML
- Copies static index.html

**2. Stage Files Locally**
```bash
VER=${GIT_BRANCH##origin/}  # Extract branch name (e.g., "main", "2.28.x")
STAGE=target/$VER/en

# Stage structure:
# target/$VER/en/
#   ├── api/
#   ├── user/html/
#   │   └── api/
#   ├── developer/html/
#   └── docguide/html/
```

**3. Deploy to OSGeo Server**
- **Server**: `geoserverdocs@geo-docs.geoserver.org:2223`
- **Remote path**: `/var/www/geoserverdocs/$VER/`
- **Process**:
  1. Create remote directory if needed
  2. Copy index.html to parent directory (main branch only)
  3. Zip staged files: `en.zip`
  4. SCP zip to remote server
  5. SSH to remote, unzip to `/var/www/geoserverdocs/$VER/en/`
  6. Create/update symlink: `/var/www/geoserverdocs/latest` → `/var/www/geoserverdocs/$VER`

**4. URL Structure**
- Published to: `https://docs.geoserver.org/$VER/en/user`
- Latest symlink: `https://docs.geoserver.org/latest/en/user`

### GitHub Actions Deployment Strategy

**Recommended Approach**: Continue using OSGeo server (Option A)

**Configuration Steps**:

1. **Add SSH Credentials to GitHub Secrets**
   - Secret name: `GEOSERVER_DOCS_SSH_KEY`
   - Value: Private SSH key for geoserverdocs@geo-docs.geoserver.org

2. **Update GitHub Actions Workflow**

```yaml
- name: Deploy to docs.geoserver.org
  if: github.ref == 'refs/heads/3.0' || github.ref == 'refs/heads/2.28.x'
  env:
    SSH_KEY: ${{ secrets.GEOSERVER_DOCS_SSH_KEY }}
  run: |
    # Setup SSH
    mkdir -p ~/.ssh
    echo "$SSH_KEY" > ~/.ssh/id_rsa
    chmod 600 ~/.ssh/id_rsa
    ssh-keyscan -p 2223 geo-docs.geoserver.org >> ~/.ssh/known_hosts
    
    # Extract branch version
    VER=${GITHUB_REF##refs/heads/}
    REMOTE=geoserverdocs@geo-docs.geoserver.org
    REMOTE_PATH=/var/www/geoserverdocs/$VER
    
    # Create staging directory matching Jenkins structure
    mkdir -p target/$VER/en
    cp -r gh-pages-output/en/user target/$VER/en/user
    cp -r gh-pages-output/en/developer target/$VER/en/developer
    cp -r gh-pages-output/en/docguide target/$VER/en/docguide
    cp -r api target/$VER/en/api
    
    # Create remote directory
    ssh -p 2223 $REMOTE "mkdir -p $REMOTE_PATH"
    
    # Copy index.html (3.0 branch only)
    if [ "$VER" = "3.0" ]; then
      scp -P 2223 doc/en/index.html $REMOTE:$REMOTE_PATH/../
    fi
    
    # Zip and transfer
    cd target/$VER
    zip -q -r en.zip en/
    scp -P 2223 en.zip $REMOTE:$REMOTE_PATH/
    
    # Unzip on remote
    ssh -p 2223 $REMOTE "cd $REMOTE_PATH && rm -rf en && unzip -q en.zip"
    
    # Update latest symlink (3.0 only)
    if [ "$VER" = "3.0" ]; then
      ssh -p 2223 $REMOTE "rm -f /var/www/geoserverdocs/latest && ln -s $REMOTE_PATH /var/www/geoserverdocs/latest"
    fi
    
    echo "Documentation published to https://docs.geoserver.org/$VER/en/user"
```

**Key Differences from Jenkins**:
- MkDocs output structure vs Sphinx output structure
- GitHub Actions uses `gh-pages-output/` instead of `target/*/html/`
- Need to adapt directory copying to match expected structure

**Testing Strategy**:
1. Test deployment on migration branch first
2. Verify URL structure matches existing docs
3. Check symlink behavior for "latest"
4. Validate all documentation types accessible

### Deployment Comparison

| Aspect | Jenkins | GitHub Actions (Post-Migration) |
|--------|---------|--------------------------------|
| Build Tool | Sphinx (Ant + Maven) | MkDocs |
| Output Dir | target/*/html/ | gh-pages-output/ |
| Deployment | SSH + ZIP transfer | SSH + ZIP transfer (same) |
| Server | geo-docs.geoserver.org:2223 | geo-docs.geoserver.org:2223 (same) |
| Remote Path | /var/www/geoserverdocs/$VER | /var/www/geoserverdocs/$VER (same) |
| Latest Symlink | Yes (main branch) | Yes (3.0 branch) |
| URL Structure | docs.geoserver.org/$VER/en/ | docs.geoserver.org/$VER/en/ (same) |
