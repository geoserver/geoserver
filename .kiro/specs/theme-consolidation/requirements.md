# Theme Consolidation Requirements

## Problem Statement

The GeoServer documentation theme files are currently duplicated across multiple locations in the `doc/en/` directory structure, leading to:

1. **Maintenance burden**: Changes must be made in multiple places
2. **Inconsistency risk**: Files can drift out of sync (as happened with extra.css)
3. **Storage waste**: Identical files stored multiple times
4. **Build complexity**: Each documentation type maintains its own copy

## Current State

### Theme File Locations

**Primary theme location:**
- `doc/themes/geoserver/` - Contains the canonical theme with all components

**Duplicated theme location:**
- `doc/en/themes/geoserver/` - Partial duplication (missing partials/)

**Per-documentation duplications:**
- `doc/en/user/docs/stylesheets/extra.css`
- `doc/en/developer/docs/stylesheets/extra.css`
- `doc/en/docguide/docs/stylesheets/extra.css`
- `doc/en/docguide/docs/img/pagelogo.png` (docguide-specific)

### Theme Components

The complete theme consists of:
- `stylesheets/extra.css` - Custom CSS for documentation switcher and mobile navigation
- `img/geoserver-logo.png` - Logo image
- `img/geoserver.ico` - Favicon
- `partials/header.html` - Custom header template
- `partials/header-switcher.html` - Documentation switcher component
- `layout.html` - Base layout template
- `theme.conf` - Theme configuration

### Current mkdocs.yml Configuration

All three documentation types reference the theme:
```yaml
theme:
  name: material
  custom_dir: ../../themes/geoserver  # user, developer, docguide
```

## Goals

1. **Eliminate duplication**: Single source of truth for all theme files
2. **Maintain functionality**: All three documentation types must continue to work
3. **Simplify maintenance**: Changes in one place propagate to all docs
4. **Preserve customization**: Allow doc-specific overrides where needed (e.g., docguide's pagelogo.png)

## Proposed Solution

### Option 1: Use Existing Structure (Recommended)

Keep `doc/themes/geoserver/` as the single source and ensure all mkdocs.yml files reference it correctly.

**Actions:**
1. Remove all duplicated theme files from `doc/en/`
2. Verify mkdocs.yml paths are correct
3. Handle doc-specific files (like docguide's pagelogo.png) through proper override mechanism

**Pros:**
- Minimal changes to existing structure
- Already partially implemented
- Clear separation between theme and content

**Cons:**
- Relative path from `doc/en/{type}/` to `doc/themes/` is `../../themes/geoserver`

### Option 2: Create Common Theme Directory

Create `doc/en/common_theme/` and reference from all three documentation types.

**Actions:**
1. Create `doc/en/common_theme/geoserver/`
2. Move theme files from `doc/themes/geoserver/` to new location
3. Update all mkdocs.yml to reference `../common_theme/geoserver`
4. Remove duplicates

**Pros:**
- Shorter relative paths
- All doc-related files under `doc/en/`
- Clearer that theme is shared

**Cons:**
- Requires restructuring
- `doc/themes/` becomes orphaned or needs removal
- More changes to existing setup

## Constraints

1. **Build process**: Must work with MkDocs Material theme's custom_dir feature
2. **GitHub Pages**: Must deploy correctly to GitHub Pages
3. **Local builds**: Must work for local development
4. **Version branches**: Changes must work across version branches (2.28.x, 3.0, etc.)

## Success Criteria

1. All three documentation types build successfully
2. Documentation switcher works on all three types
3. No duplicate theme files exist
4. Theme changes propagate to all documentation types
5. Doc-specific customizations (if any) still work
6. GitHub Pages deployment succeeds
7. All visual elements render correctly

## Out of Scope

- Changing the theme itself (Material theme)
- Modifying documentation content
- Restructuring documentation organization
- Changes to build scripts (beyond path updates)

## Risks

1. **Build failures**: Incorrect paths could break builds
2. **Deployment issues**: GitHub Pages might not find theme files
3. **Visual regressions**: Missing CSS or images could break appearance
4. **Cross-version conflicts**: Changes might not work across all version branches

## Dependencies

- MkDocs 1.6.1
- MkDocs Material 9.7.4
- Python build environment
- GitHub Pages deployment workflow
