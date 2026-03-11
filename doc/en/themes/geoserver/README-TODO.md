# TEMPORARY DIRECTORY - TO BE REMOVED

**Status:** This directory is temporarily restored for Sphinx build compatibility.

## Why This Directory Exists

This directory was removed during theme consolidation (commit 80feea3f81) because:
- It's a duplicate of `doc/themes/geoserver/` (the canonical MkDocs theme)
- It's incomplete (missing critical MkDocs components)
- It was causing confusion about which theme is canonical

However, it has been **temporarily restored** because:
- The CI workflow (`.github/workflows/docs.yml`) still runs Sphinx builds
- Sphinx `conf.py` files reference this theme location: `html_theme_path = ['../../themes']`
- The Sphinx build fails without this directory

## What Needs to Happen

**This directory MUST be removed** as part of the RST infrastructure cleanup in task 3.7 / 5.6:

1. Remove all RST source directories (`doc/en/*/source/`)
2. Remove Sphinx configuration files (`conf.py`, `requirements.txt`)
3. Remove Sphinx build scripts (`doc/en/build.xml`, `doc/en/pom.xml` Sphinx profiles)
4. Remove `.github/workflows/docs.yml` (old Sphinx workflow)
5. **Remove this directory** (`doc/en/themes/geoserver/`)

## Timeline

- **Now:** Directory restored temporarily for Sphinx build compatibility
- **After validation:** Remove this directory along with all RST infrastructure (Phase 3, task 3.7 for 2.28.x; Phase 4, task 5.6 for 3.0)

## Canonical Theme Location

The **canonical theme** for MkDocs is at:
- `doc/themes/geoserver/` (complete, maintained, used by MkDocs builds)

This directory (`doc/en/themes/geoserver/`) is:
- Incomplete (missing `img/`, `partials/`, `stylesheets/`, `overrides/`)
- Only used by legacy Sphinx builds
- Will be removed when Sphinx is fully deprecated

---

**DO NOT** make changes to this directory. All theme changes should be made to `doc/themes/geoserver/`.
