# Download Link Fix Scripts - Execution Guide for 2.28.x Branch

This document lists all fix scripts that were created to address broken download links in the GeoServer documentation after the RST/Sphinx to Markdown/MkDocs migration.

## Overview

These scripts fix various issues with download links across 50+ documentation files. They must be run in sequence on the `migration/2.28.x` branch after cherry-picking the initial commit.

## Prerequisites

Before running any scripts:
```bash
# Activate Python virtual environment
source .venv/Scripts/activate

# Ensure you're on the correct branch
git checkout migration/2.28.x

# Create a backup commit before running scripts
git add -A
git commit -m "Backup before applying download link fixes"
```

## Scripts to Run (in order)

### 1. fix_download_links.py (Main Fix Script)

**Purpose**: Fixes the core bug - incomplete link text in download links

**What it fixes**:
- Transforms `{{ release }} [mbstyle](URL)` → `{{ release }} [geoserver-{{ release }}-mbstyle-plugin.zip](URL)`
- Handles extensions, community modules, WAR binaries, and bin downloads
- Only modifies links matching the bug condition (missing "geoserver-" prefix and file extensions)
- Preserves all non-download links unchanged

**Run command**:
```bash
python scripts/fix_download_links.py
```

**Expected output**: Reports number of files processed, files modified, and links fixed

---

### 2. fix_version_to_snapshot.py

**Purpose**: Replaces `{{ version }}` macro with `{{ snapshot }}` in download links

**What it fixes**:
- Pattern: `{{ version }} [geoserver-{{ version }}-...-plugin.zip](URL)`
- Becomes: `{{ snapshot }} [geoserver-{{ snapshot }}-...-plugin.zip](URL)`
- Fixes link text, macro prefix, and URLs

**Affected files** (20 files):
- doc/en/user/docs/configuration/tools/resource/install.md
- doc/en/user/docs/data/database/mysql.md
- doc/en/user/docs/data/database/oracle.md
- doc/en/user/docs/data/database/sqlserver.md
- doc/en/user/docs/data/raster/arcgrid.md
- doc/en/user/docs/data/raster/gdal.md
- doc/en/user/docs/data/raster/imagepyramid.md
- doc/en/user/docs/data/raster/worldimage.md
- doc/en/user/docs/data/vector/featurepregen.md
- doc/en/user/docs/extensions/arcgrid/index.md
- doc/en/user/docs/extensions/geofence-server/installing.md
- doc/en/user/docs/extensions/image/index.md
- doc/en/user/docs/services/csw/installing.md
- doc/en/user/docs/services/features/install.md
- doc/en/user/docs/services/wcs/install.md
- doc/en/user/docs/services/wps/install.md
- doc/en/user/docs/styling/css/install.md
- doc/en/user/docs/styling/mbstyle/installing.md
- doc/en/user/docs/styling/workshop/setup/install.md
- doc/en/user/docs/styling/ysld/installing.md

**Run command**:
```bash
python scripts/fix_version_to_snapshot.py
```

---

### 3. fix_community_snapshot_pattern.py

**Purpose**: Ensures consistency in community module snapshot URLs

**What it fixes**:
- Pattern: `geoserver-{{ version }}-SNAPSHOT-`
- Becomes: `geoserver-{{ snapshot }}-`

**Scope**: All markdown files in `doc/en/user/docs/community/`

**Run command**:
```bash
python scripts/fix_community_snapshot_pattern.py
```

---

### 4. fix_example_text_version.py

**Purpose**: Fixes example text in warnings to match actual download links

**What it fixes**:
- Pattern: `for example geoserver-{{ version }}-`
- Becomes: `for example geoserver-{{ snapshot }}-`

**Scope**: All markdown files in `doc/en/user/docs/community/`

**Run command**:
```bash
python scripts/fix_example_text_version.py
```

---

### 5. fix_remaining_version_macros.py

**Purpose**: Fixes remaining `{{ version }}` macros in download link contexts

**What it fixes**:
- Pattern: `{{ version }} example:`
- Becomes: `{{ snapshot }} example:`

**Scope**: All markdown files in `doc/en/user/docs/`

**Run command**:
```bash
python scripts/fix_remaining_version_macros.py
```

---

### 6. fix_snapshot_url_path.py

**Purpose**: Corrects snapshot build URL paths

**What it fixes**:
- Pattern: `build.geoserver.org/geoserver/main/extensions/`
- Becomes: `build.geoserver.org/geoserver/main/ext-latest/`

**Reason**: Snapshot builds are located at `/ext-latest/`, not `/extensions/`

**Scope**: All markdown files in `doc/en/user/docs/`

**Run command**:
```bash
python scripts/fix_snapshot_url_path.py
```

---

## Complete Execution Sequence

Run all scripts in order:

```bash
# Activate venv
source .venv/Scripts/activate

# Run all fix scripts in sequence
python scripts/fix_download_links.py
python scripts/fix_version_to_snapshot.py
python scripts/fix_community_snapshot_pattern.py
python scripts/fix_example_text_version.py
python scripts/fix_remaining_version_macros.py
python scripts/fix_snapshot_url_path.py
```

## Verification After Running Scripts

After running all scripts:

1. **Check git diff**:
   ```bash
   git diff --stat
   ```

2. **Review changes**:
   ```bash
   git diff doc/
   ```

3. **Verify only download links were modified** (preservation property)

4. **Build documentation** to verify links render correctly:
   ```bash
   # Build command depends on your setup
   mkdocs build
   ```

5. **Test sample download links** manually

## Commit Changes

After verification:

```bash
git add scripts/ doc/
git commit -m "Fix broken download links in 2.28.x documentation

Applied all download link fixes:
- Fixed incomplete link text (main fix)
- Replaced {{ version }} with {{ snapshot }} in download links
- Fixed community module snapshot patterns
- Fixed example text in warnings
- Fixed remaining version macros
- Corrected snapshot URL paths"
```

## Notes for 2.28.x Branch

- File paths may differ between 3.0 and 2.28.x branches
- Version numbers in examples will differ (3.0.x vs 2.28.x)
- Some files may exist in one branch but not the other
- If scripts report "file not found", this is expected for branch differences
- Focus on verifying that files that DO exist are fixed correctly

## Troubleshooting

**If a script fails**:
1. Check that you're in the workspace root directory
2. Verify the virtual environment is activated
3. Check that the target files exist in the 2.28.x branch
4. Review the script output for specific error messages

**If merge conflicts occur during cherry-pick**:
1. Resolve conflicts manually
2. Run the scripts on the resolved files
3. Verify changes with `git diff`
4. Continue with `git cherry-pick --continue`
