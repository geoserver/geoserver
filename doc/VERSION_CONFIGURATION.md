# Centralized Version Configuration

## Overview

All GeoServer documentation manuals (user, developer, docguide, and translations) now use a single centralized version configuration file to ensure consistency across all documentation.

## Configuration File

**Location:** `doc/version.py`

This Python module is loaded by mkdocs-macros-plugin and provides version variables to all Markdown files.

## How to Update Version Numbers

When releasing a new version of GeoServer, update the version numbers in **ONE place only**:

1. Open `doc/version.py`
2. Update the `version` and `release` values:
   ```python
   env.variables['version'] = '2.28.x'    # Update this
   env.variables['release'] = '2.28.2'    # Update this
   ```
3. Save the file
4. Rebuild all documentation - the new version will appear everywhere

## Available Variables

The following variables are available in all Markdown files:

- `{{ version }}` - Branch version (e.g., "2.28.x", "3.0")
- `{{ release }}` - Specific release version (e.g., "2.28.2", "3.0.0")
- `{{ geoserver_repo }}` - GitHub repository URL
- `{{ docs_url }}` - Documentation base URL

## Usage in Markdown

Use these variables in any Markdown file:

```markdown
GeoServer {{ version }} is the latest stable release.

Download GeoServer {{ release }} from the website.

Docker image: docker.osgeo.org/geoserver:{{ version }}.x
```

## Technical Details

### How It Works

1. Each `mkdocs.yml` file references the central `version.py` module:
   ```yaml
   plugins:
     - macros:
         module_name: ../../version
   ```

2. When MkDocs builds the documentation, mkdocs-macros-plugin:
   - Loads `doc/version.py`
   - Calls the `define_env()` function
   - Makes the variables available to all Markdown files

3. During rendering, `{{ version }}` and `{{ release }}` are replaced with actual values

### Files That Reference version.py

- `doc/en/user/mkdocs.yml`
- `doc/en/developer/mkdocs.yml`
- `doc/en/docguide/mkdocs.yml`
- `doc/zhCN/user/mkdocs.yml`

All four files use `module_name: ../../version` to reference the same central configuration.

## Benefits

1. **Single Source of Truth** - Update version in one place, applies everywhere
2. **Consistency** - All manuals show the same version numbers
3. **Easy Maintenance** - No need to search/replace across multiple files
4. **Branch-Specific** - Each git branch (2.28.x, 3.0, main) has its own version.py

## Migration Notes

Previously, version numbers were hardcoded in the `extra:` section of each mkdocs.yml file:

```yaml
# OLD WAY (don't use)
extra:
  version: "2.28.x"
  release: "2.28.2"
```

This has been replaced with the centralized `doc/version.py` approach. The `extra:` section no longer contains version/release variables.
