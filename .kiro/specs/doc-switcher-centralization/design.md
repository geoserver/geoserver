# Design Document: Doc Switcher Centralization

## Overview

This feature centralizes the `doc_switcher` configuration from three separate MkDocs configuration files into a single shared YAML file. Currently, the user manual, developer manual, and documentation guide each maintain identical copies of the doc_switcher navigation menu, creating maintenance overhead and potential inconsistencies.

The solution leverages the existing MkDocs macros plugin infrastructure to load a shared YAML configuration file at build time and inject it into each documentation site's configuration. This approach eliminates duplication while maintaining backward compatibility with existing templates and build processes.

### Goals

- Eliminate duplication of doc_switcher configuration across three mkdocs.yml files
- Maintain backward compatibility with existing templates and build processes
- Provide clear error messages for configuration issues
- Require no changes to CI/CD pipelines or template files

### Non-Goals

- Centralizing other configuration values beyond doc_switcher
- Modifying the doc_switcher data structure or template rendering
- Changing the build process or adding new build commands

## Architecture

### High-Level Design

The architecture consists of three main components:

1. **Shared Configuration File**: A YAML file at `doc/themes/geoserver/doc_switcher.yml` containing the doc_switcher array
2. **Macros Plugin Extension**: Python code in `doc/version.py` that loads and validates the shared configuration
3. **MkDocs Configuration Files**: Three mkdocs.yml files that reference the shared configuration through the macros plugin

```
┌─────────────────────────────────────────────────────────────┐
│                    MkDocs Build Process                      │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│              Macros Plugin (version.py)                      │
│  ┌────────────────────────────────────────────────────┐    │
│  │ 1. Load doc_switcher.yml                           │    │
│  │ 2. Parse YAML                                      │    │
│  │ 3. Validate structure and fields                   │    │
│  │ 4. Inject into env.variables['doc_switcher']      │    │
│  └────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                  MkDocs Configuration                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ user/        │  │ developer/   │  │ docguide/    │     │
│  │ mkdocs.yml   │  │ mkdocs.yml   │  │ mkdocs.yml   │     │
│  │              │  │              │  │              │     │
│  │ extra:       │  │ extra:       │  │ extra:       │     │
│  │   doc_type   │  │   doc_type   │  │   doc_type   │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    Template Rendering                        │
│  Templates access:                                           │
│  - extra.doc_switcher (from shared config)                  │
│  - extra.doc_type (from local config)                       │
└─────────────────────────────────────────────────────────────┘
```

### Design Decisions

**Decision 1: Use Macros Plugin Instead of YAML Anchors**
- Rationale: MkDocs doesn't support YAML includes or anchors across files. The macros plugin is already configured and provides a Python-based extension mechanism.
- Trade-off: Requires Python code but provides validation and error handling capabilities.

**Decision 2: Place Shared Config in Theme Directory**
- Rationale: The theme directory (`doc/themes/geoserver/`) is already shared across all three documentation manuals and is referenced via `custom_dir`.
- Trade-off: Couples configuration to theme location, but this is already an established pattern.

**Decision 3: Keep doc_type in Local Config Files**
- Rationale: Each documentation manual needs a unique identifier that shouldn't be shared.
- Trade-off: Partial centralization, but maintains necessary per-manual configuration.

**Decision 4: Validate Configuration at Load Time**
- Rationale: Early validation provides clear error messages before the build process begins.
- Trade-off: Adds processing overhead, but improves developer experience.

## Components and Interfaces

### Component 1: Shared Configuration File

**Location**: `doc/themes/geoserver/doc_switcher.yml`

**Structure**:
```yaml
# Shared doc_switcher configuration for all GeoServer documentation manuals
# This file is loaded by the macros plugin (version.py) and injected into
# the 'extra' section of each mkdocs.yml configuration.
#
# DO NOT modify the individual mkdocs.yml files - update this file instead.

doc_switcher:
  - label: "User Manual"
    url: "../user/"
    type: "user"
  - label: "Developer Manual"
    url: "../developer/"
    type: "developer"
  - label: "Documentation Guide"
    url: "../docguide/"
    type: "docguide"
  - label: "Swagger APIs"
    url: "../user/api/"
    type: "swagger"
```

**Responsibilities**:
- Store the canonical doc_switcher configuration
- Provide clear documentation through comments
- Use valid YAML syntax

### Component 2: Macros Plugin Extension

**Location**: `doc/version.py`

**Interface**:
```python
def define_env(env):
    """
    Define macros and variables for mkdocs-macros-plugin.
    
    This function is called by mkdocs-macros-plugin during the build process.
    It loads the shared doc_switcher configuration and makes it available
    to all templates.
    """
    # Existing version configuration...
    
    # Load shared doc_switcher configuration
    import yaml
    from pathlib import Path
    
    config_path = Path(__file__).parent / 'themes' / 'geoserver' / 'doc_switcher.yml'
    with open(config_path, 'r') as f:
        config = yaml.safe_load(f)
    
    env.variables['doc_switcher'] = config['doc_switcher']
```

**Responsibilities**:
- Load and parse the YAML file
- Inject the configuration into the MkDocs environment
- Let YAML parser handle syntax errors (no custom validation needed)

**Path Resolution Logic**:
The macros plugin is invoked from three different locations, but `doc/version.py` is always at the same location relative to the shared config:
- `doc/version.py` → shared config at `themes/geoserver/doc_switcher.yml`

Simple relative path from the version.py file location.

### Component 3: MkDocs Configuration Files

**Modified Files**:
- `doc/en/user/mkdocs.yml`
- `doc/en/developer/mkdocs.yml`
- `doc/en/docguide/mkdocs.yml`

**Changes**:
Each file will have its `extra.doc_switcher` array removed and replaced with a comment:

```yaml
extra:
  doc_type: "user"  # Remains in local config
  # doc_switcher is loaded from shared config by macros plugin (see version.py)
  version_selector:
    # ... existing version_selector config ...
```

**Responsibilities**:
- Maintain the doc_type field specific to each manual
- Reference the macros plugin for shared configuration
- Preserve all other existing configuration values

## Data Models

### Doc Switcher Entry

```python
{
    "label": str,      # Display name (e.g., "User Manual")
    "url": str,        # Relative URL (e.g., "../user/")
    "type": str        # Type identifier: "user", "developer", "docguide", or "swagger"
}
```

### Doc Switcher Configuration

```python
{
    "doc_switcher": [
        DocSwitcherEntry,
        DocSwitcherEntry,
        ...
    ]
}
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do.*

### Property 1: Configuration Loading

*For any* MkDocs configuration file (user, developer, docguide), when the macros plugin processes it, the system must successfully load the shared doc_switcher.yml file and make it accessible to templates as `extra.doc_switcher`.

**Validates: Requirements 2.1, 2.2, 2.5, 3.3, 4.1, 4.2**

**Testing Approach**: Manual verification by building all three manuals and checking that doc_switcher renders correctly.

### Example-Based Tests

**Example 1: Successful Build**
- Build all three documentation manuals (user, developer, docguide)
- Verify builds complete without errors
- Verify doc_switcher appears in rendered HTML

**Example 2: Config Files Updated**
- Verify all three mkdocs.yml files have doc_switcher arrays removed
- Verify doc_type remains in each file

**Example 3: Template Compatibility**
- Verify existing templates work without modification
- Verify doc_switcher navigation renders correctly

## Error Handling

### Error Scenarios and Responses

**Scenario 1: Shared Configuration File Not Found**
- Python will raise `FileNotFoundError` with the file path
- MkDocs build will fail with clear error message

**Scenario 2: Invalid YAML Syntax**
- PyYAML will raise `yaml.YAMLError` with syntax details
- MkDocs build will fail with clear error message

**Scenario 3: Missing 'doc_switcher' Key**
- Python will raise `KeyError: 'doc_switcher'`
- MkDocs build will fail

### Error Handling Strategy

1. **No Custom Validation**: Let Python and YAML parser handle errors naturally
2. **Fail Fast**: Errors occur during plugin initialization, before build starts
3. **Standard Exceptions**: Use built-in Python exceptions (FileNotFoundError, KeyError, yaml.YAMLError)

### Recovery Mechanisms

- **No Automatic Recovery**: Configuration errors require manual intervention
- **Build Failure**: Any error causes the build to fail immediately

## Testing Strategy

### Testing Approach

This is a simple configuration change with minimal logic. Testing focuses on verifying the integration works correctly.

### Manual Testing

**Primary Testing Method**: Manual verification that builds work and doc_switcher renders correctly.

1. **Build Test**: Run `mkdocs build` for each manual (user, developer, docguide)
2. **Serve Test**: Run `mkdocs serve` for each manual and verify doc_switcher appears
3. **Navigation Test**: Click each doc_switcher link and verify navigation works
4. **Visual Test**: Compare rendered HTML before and after to ensure consistency

### Optional Unit Tests

If desired, simple unit tests can verify:
- The shared YAML file can be loaded
- The loaded data has the expected structure
- Each mkdocs.yml file has been updated correctly

### Test Organization

```
tests/
└── integration/
    ├── test_user_build.py       # Test user manual builds
    ├── test_developer_build.py  # Test developer manual builds
    └── test_docguide_build.py   # Test docguide manual builds
```

### Testing Guidelines

1. **Keep It Simple**: This is a straightforward config change, don't over-test
2. **Focus on Integration**: Verify the three manuals build and render correctly
3. **Manual Verification**: Visual inspection is the most important test

## Implementation Plan

### Phase 1: Create Shared Configuration

1. Create `doc/themes/geoserver/doc_switcher.yml` with the doc_switcher array
2. Add comprehensive comments explaining the file's purpose
3. Validate YAML syntax

### Phase 2: Extend Macros Plugin

1. Add simple loading code to `doc/version.py`
2. Add inline comments explaining the implementation

### Phase 3: Update Configuration Files

1. Remove `doc_switcher` array from `doc/en/user/mkdocs.yml`
2. Remove `doc_switcher` array from `doc/en/developer/mkdocs.yml`
3. Remove `doc_switcher` array from `doc/en/docguide/mkdocs.yml`
4. Add comments in each file explaining that doc_switcher is loaded by the macros plugin
5. Verify that `doc_type` remains in each file

### Phase 4: Testing

1. Run `mkdocs build` for each manual
2. Run `mkdocs serve` for each manual and verify doc_switcher appears
3. Verify navigation works correctly
4. Compare HTML output before and after

### Phase 5: Documentation

1. Add comments to the shared config file
2. Add comments to the updated mkdocs.yml files
3. Add comments to version.py explaining the loading logic

## Dependencies

### External Dependencies

- **PyYAML**: Required for parsing the shared YAML configuration file
  - Already a dependency of MkDocs
  - No additional installation required

- **mkdocs-macros-plugin**: Required for the plugin extension mechanism
  - Already configured in all three mkdocs.yml files
  - No additional installation required

### Internal Dependencies

- **MkDocs Configuration**: The solution depends on the existing MkDocs configuration structure
- **Theme Directory**: The solution depends on the existing theme directory structure at `doc/themes/geoserver/`
- **Macros Plugin Module**: The solution extends the existing `doc/version.py` module

### Version Compatibility

- **MkDocs**: Compatible with current version (no version-specific features used)
- **mkdocs-macros-plugin**: Compatible with current version (uses standard `define_env` hook)
- **Python**: Requires Python 3.6+ (for f-strings and type hints)

## Risks and Mitigations

### Risk 1: Path Resolution Issues

**Description**: The relative path from mkdocs.yml to doc_switcher.yml might not resolve correctly in all environments.

**Likelihood**: Low
**Impact**: High (build failures)

**Mitigation**:
- Use MkDocs' config file path to resolve relative paths
- Test in multiple environments (local, CI/CD)
- Provide clear error messages if file cannot be found

### Risk 2: Breaking Template Compatibility

**Description**: Changes to how doc_switcher is exposed might break existing templates.

**Likelihood**: Low
**Impact**: High (broken documentation sites)

**Mitigation**:
- Maintain the same variable name (`extra.doc_switcher`)
- Test template rendering before and after changes
- Compare HTML output to ensure consistency

### Risk 3: Build Performance Impact

**Description**: Loading the shared config on every build might slow down the build process.

**Likelihood**: Very Low
**Impact**: Negligible

**Mitigation**:
- The config file is tiny (< 1KB)
- Loading and parsing is extremely fast
- No performance impact expected

### Risk 4: Typos in Shared Config

**Description**: A typo in the shared config affects all three manuals.

**Likelihood**: Low
**Impact**: Medium (all docs broken)

**Mitigation**:
- Test builds locally before committing
- CI/CD will catch errors before deployment
- Centralization actually reduces this risk (one file vs three)

## Future Enhancements

### Enhancement 1: Additional Shared Configuration

Once this pattern is proven successful, other duplicated configuration values could be centralized:
- Social links
- Theme colors
- Common navigation elements

### Enhancement 2: Configuration Validation Tool

A standalone CLI tool could be created to validate the shared configuration without running a full build:
```bash
python validate_config.py doc/themes/geoserver/doc_switcher.yml
```

### Enhancement 3: Dynamic Doc Switcher

The doc_switcher could be enhanced to automatically detect available documentation types rather than requiring manual configuration.

### Enhancement 4: Multi-Language Support

If GeoServer documentation expands to multiple languages, the shared configuration pattern could be extended to support language-specific doc_switcher configurations.

### Enhancement 5: Configuration Schema

A JSON Schema or similar could be created to provide IDE autocomplete and validation for the shared configuration file.

## Appendix

### File Locations Reference

```
doc/
├── themes/
│   └── geoserver/
│       ├── doc_switcher.yml          # NEW: Shared configuration
│       └── ...                        # Existing theme files
├── en/
│   ├── user/
│   │   └── mkdocs.yml                # MODIFIED: Remove doc_switcher array
│   ├── developer/
│   │   └── mkdocs.yml                # MODIFIED: Remove doc_switcher array
│   └── docguide/
│       └── mkdocs.yml                # MODIFIED: Remove doc_switcher array
└── version.py                         # MODIFIED: Add loading logic
```

### Current Doc Switcher Configuration

For reference, the current doc_switcher configuration that appears in all three mkdocs.yml files:

```yaml
doc_switcher:
  - label: "User Manual"
    url: "../user/"
    type: "user"
  - label: "Developer Manual"
    url: "../developer/"
    type: "developer"
  - label: "Documentation Guide"
    url: "../docguide/"
    type: "docguide"
  - label: "Swagger APIs"
    url: "../user/api/"
    type: "swagger"
```

### Macros Plugin Configuration

Current configuration in each mkdocs.yml file:

```yaml
plugins:
  - search:
      lang: en
  - macros:
      render_by_default: true
      include_dir: docs
      module_name: ../../version
```

This configuration tells the macros plugin to load `doc/version.py` and call its `define_env()` function during the build process.
