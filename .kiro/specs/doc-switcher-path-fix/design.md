# Doc Switcher Path Fix Bugfix Design

## Overview

The GeoServer documentation switcher uses relative paths (`../user/`, `../developer/`, etc.) in the centralized `doc_switcher.yml` configuration file. These relative paths work correctly at nesting level 1 but fail at deeper levels (level 2+), causing 404 errors when users try to switch documentation types. The fix will replace relative paths with absolute paths dynamically constructed using the MkDocs macros plugin, ensuring correct navigation regardless of page depth. The solution must support both the current multi-branch deployment structure (`/migration/3.0-rst-to-md/en/user/`) and the future mike-based versioned structure (`/latest/en/user/`, `/stable/en/user/`).

## Glossary

- **Bug_Condition (C)**: The condition that triggers the bug - when users navigate to pages at nesting level 2 or deeper and click doc_switcher links
- **Property (P)**: The desired behavior - doc_switcher links should navigate to the correct documentation type regardless of current page depth
- **Preservation**: Existing doc_switcher behavior at level 1 and the centralized configuration approach must remain unchanged
- **doc_switcher.yml**: The centralized YAML configuration file at `doc/themes/geoserver/doc_switcher.yml` that defines navigation links between documentation types
- **version.py**: The macros plugin module at `doc/version.py` that loads doc_switcher.yml and injects it into MkDocs configuration
- **Nesting Level**: The depth of a page in the URL structure (e.g., `/en/user/` is level 1, `/en/user/introduction/` is level 2)
- **site_url**: The MkDocs configuration variable defined in each mkdocs.yml file (e.g., `https://docs.geoserver.org/3.0/en/user/`) that defines the base URL for the documentation site
- **doc_type**: The extra variable in mkdocs.yml that identifies the current documentation type (user, developer, docguide)
- **mike**: A versioning tool for MkDocs that manages multiple documentation versions using `--deploy-prefix` to control deployment paths
- **DOCS_BASE_PATH**: Environment variable (optional) to override the base path for migration branch deployments where site_url doesn't match actual deployment URL

## Bug Details

### Bug Condition

The bug manifests when a user navigates to a page at nesting level 2 or deeper (e.g., `/en/user/introduction/` or `/en/user/introduction/overview/`) and clicks any doc_switcher link. The relative path resolution (`../developer/`) only goes up one directory level, so it resolves from the current page location instead of from the documentation root, resulting in incorrect URLs like `/en/user/developer/` instead of `/en/developer/`.

### site_url Configuration Context

**Current Configuration:**
- `site_url` is defined in each mkdocs.yml file:
  - `doc/en/user/mkdocs.yml`: `site_url: https://docs.geoserver.org/3.0/en/user/`
  - `doc/en/developer/mkdocs.yml`: `site_url: https://docs.geoserver.org/3.0/en/developer/`
  - `doc/en/docguide/mkdocs.yml`: `site_url: https://docs.geoserver.org/3.0/en/docguide/`

**Deployment Scenarios:**

1. **Local Development** (`mkdocs serve`):
   - Uses `site_url` from mkdocs.yml directly
   - Typically serves at `http://localhost:8000/`
   - Relative paths work at level 1 but fail at deeper levels

2. **Migration Branch Testing** (current issue):
   - Actual URL: `https://petersmythe.github.io/geoserver/migration/3.0-rst-to-md/en/user/`
   - Configured site_url: `https://docs.geoserver.org/3.0/en/user/`
   - **Mismatch**: The `/geoserver/migration/3.0-rst-to-md/` prefix is not in site_url
   - This mismatch contributes to path resolution issues

3. **Production with mike** (future):
   - Mike uses `--deploy-prefix` to control deployment paths (e.g., `mike deploy --deploy-prefix "latest/en/user"`)
   - Mike automatically handles path adjustments for versioned deployments
   - Final URL: `https://docs.geoserver.org/latest/en/user/`
   - The `site_url` serves as the base, and mike adds version prefixes

**Formal Specification:**
```
FUNCTION isBugCondition(input)
  INPUT: input of type PageNavigationEvent
  OUTPUT: boolean
  
  RETURN input.currentPageNestingLevel >= 2
         AND input.clickedElement IN doc_switcher_links
         AND doc_switcher_uses_relative_paths()
         AND NOT buttonClickNavigatesToCorrectPath(input.targetDocType)
END FUNCTION
```

### Examples

- **Example 1**: User at `/en/user/introduction/` clicks "Developer Manual" → navigates to `/en/user/developer/` (404) instead of `/en/developer/`
- **Example 2**: User at `/en/user/introduction/overview/` clicks "Developer Manual" → navigates to `/en/user/introduction/developer/` (404) instead of `/en/developer/`
- **Example 3**: User at `/en/developer/core/architecture/` clicks "User Manual" → navigates to `/en/developer/core/user/` (404) instead of `/en/user/`
- **Edge Case**: User at `/en/user/` (level 1) clicks "Developer Manual" → correctly navigates to `/en/developer/` (this currently works)

## Expected Behavior

### Preservation Requirements

**Unchanged Behaviors:**
- Navigation at nesting level 1 must continue to work exactly as before
- The centralized doc_switcher.yml configuration approach must remain unchanged
- The macros plugin loading mechanism in version.py must continue to work
- Theme templates accessing `extra.doc_switcher` must continue to receive the same data structure (array with label, url, type fields)
- The build process (`mkdocs build`, `mkdocs serve`) must continue to work without additional command-line arguments

**Scope:**
All aspects of the doc_switcher functionality that do NOT involve URL path construction should be completely unaffected by this fix. This includes:
- The YAML structure of doc_switcher.yml
- The template rendering logic
- The visual appearance of the doc_switcher UI
- The loading and injection mechanism in version.py

## Hypothesized Root Cause

Based on the bug description and code analysis, the root cause is:

1. **Relative Path Limitation**: The `../` operator in relative paths only goes up one directory level. At nesting level 2+, a single `../` cannot reach the documentation root where sibling documentation types are located.

2. **Static Path Configuration**: The doc_switcher.yml file contains static relative paths that don't adapt to the current page's nesting level. There's no mechanism to dynamically adjust the number of `../` operators based on page depth.

3. **Missing Base URL Context**: The version.py macros plugin loads doc_switcher.yml but doesn't access or use the `site_url` configuration (available via `env.conf['site_url']`) to construct absolute paths.

4. **Multi-Branch Deployment Complexity**: The deployment structure includes branch prefixes (e.g., `/migration/3.0-rst-to-md/`) that further complicate path resolution. For migration branch testing, the actual deployment URL doesn't match the `site_url` configured in mkdocs.yml, requiring awareness of the full deployment context.

## Correctness Properties

Property 1: Bug Condition - Doc Switcher Navigation at Any Depth

_For any_ page navigation event where a user is at nesting level 2 or deeper and clicks a doc_switcher link, the fixed implementation SHALL navigate to the correct documentation type at the documentation root (e.g., `/en/developer/`), preserving any version or branch prefixes in the URL path.

**Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5**

Property 2: Preservation - Level 1 Navigation and Configuration

_For any_ page navigation event at nesting level 1, doc_switcher configuration loading, or template rendering, the fixed implementation SHALL produce exactly the same behavior as the original implementation, preserving the centralized configuration approach and existing data structures.

**Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5**

## Fix Implementation

### Changes Required

Assuming our root cause analysis is correct:

**File**: `doc/version.py`

**Function**: `define_env`

**Specific Changes**:

1. **Extract Base URL from site_url**: Parse the `site_url` configuration to extract the base path that includes version/branch prefixes
   - Example: `https://docs.geoserver.org/3.0/en/user/` → extract `/3.0/en/`
   - Example: `https://petersmythe.github.io/geoserver/migration/3.0-rst-to-md/en/user/` → extract `/geoserver/migration/3.0-rst-to-md/en/`

2. **Construct Absolute Paths**: For each doc_switcher entry, replace the relative path with an absolute path constructed from the base URL
   - Combine extracted base path with the target doc type
   - Example: `/3.0/en/` + `developer/` → `/3.0/en/developer/`

3. **Handle Root-Relative URLs**: Ensure paths start with `/` to make them root-relative (absolute from the domain root)
   - This ensures they work regardless of the current page's nesting level

4. **Preserve URL Structure**: Maintain the trailing slash convention and ensure compatibility with both GitHub Pages and local development

5. **Add Fallback Logic**: If `site_url` is not configured or cannot be parsed, fall back to relative paths with a warning

**File**: `doc/themes/geoserver/doc_switcher.yml`

**Changes**: Update the documentation comments to reflect that paths will be dynamically converted to absolute paths by the macros plugin. The relative paths in the file serve as templates that indicate the target doc type.

### Implementation Approach

The fix will be implemented in the `define_env` function in `doc/version.py` with support for three deployment scenarios:

**Solution Strategy:**
1. **Primary**: Use `env.conf['site_url']` to extract base path (works for local dev and production)
2. **Fallback**: Use environment variable `DOCS_BASE_PATH` for migration branch testing where site_url doesn't match deployment URL
3. **Compatibility**: Ensure solution works with both current deployment and future mike-based versioning

```python
import os
from urllib.parse import urlparse

def define_env(env):
    # ... existing version variables ...
    
    # Load shared doc_switcher configuration
    config_path = Path(__file__).parent / 'themes' / 'geoserver' / 'doc_switcher.yml'
    with open(config_path, 'r') as f:
        config = yaml.safe_load(f)
    
    # Extract base path for absolute URL construction
    # Priority: 1) Environment variable (for migration branch), 2) site_url (for production/local)
    base_path = extract_base_path(env)
    
    # Convert relative paths to absolute paths
    doc_switcher = []
    for entry in config['doc_switcher']:
        absolute_entry = entry.copy()
        if not entry['url'].startswith('http'):
            # Convert relative path to absolute path
            absolute_entry['url'] = construct_absolute_path(base_path, entry['url'], entry['type'])
        doc_switcher.append(absolute_entry)
    
    # Inject doc_switcher into config.extra
    env.conf['extra']['doc_switcher'] = doc_switcher
```

Helper functions to add:

```python
def extract_base_path(env):
    """
    Extract the base path for constructing absolute doc_switcher URLs.
    
    Priority:
    1. DOCS_BASE_PATH environment variable (for migration branch testing)
    2. Parse from site_url in mkdocs.yml (for production and local development)
    
    Examples:
    - site_url: https://docs.geoserver.org/3.0/en/user/ → /3.0/en/
    - site_url: https://petersmythe.github.io/geoserver/migration/3.0-rst-to-md/en/user/
      with DOCS_BASE_PATH=/geoserver/migration/3.0-rst-to-md → /geoserver/migration/3.0-rst-to-md/en/
    - DOCS_BASE_PATH=/geoserver/migration/3.0-rst-to-md → /geoserver/migration/3.0-rst-to-md/en/
    
    Returns:
        str: Base path with leading slash and trailing slash (e.g., '/3.0/en/')
    """
    # Check for environment variable first (migration branch override)
    env_base_path = os.environ.get('DOCS_BASE_PATH', '').strip()
    if env_base_path:
        # Ensure leading slash
        if not env_base_path.startswith('/'):
            env_base_path = '/' + env_base_path
        # Add /en/ if not present (language directory)
        if not env_base_path.endswith('/en/'):
            env_base_path = env_base_path.rstrip('/') + '/en/'
        return env_base_path
    
    # Parse from site_url
    site_url = env.conf.get('site_url', '')
    if not site_url:
        # Fallback to root if no site_url configured
        return '/en/'
    
    # Parse URL and extract path component
    parsed = urlparse(site_url)
    path = parsed.path.rstrip('/')
    
    # Remove current doc type from the end (user, developer, docguide)
    current_doc_type = env.conf.get('extra', {}).get('doc_type', '')
    if current_doc_type and path.endswith('/' + current_doc_type):
        path = path[:-len(current_doc_type)-1]
    
    # Ensure trailing slash
    if not path.endswith('/'):
        path += '/'
    
    return path
    
def construct_absolute_path(base_path, relative_url, target_doc_type):
    """
    Construct an absolute path from base path and relative URL.
    
    Args:
        base_path: Base path extracted from site_url or environment (e.g., '/3.0/en/')
        relative_url: Relative URL from doc_switcher.yml (e.g., '../developer/')
        target_doc_type: Target documentation type (e.g., 'developer', 'user')
    
    Examples:
    - base_path=/3.0/en/, relative_url=../developer/, target=developer → /3.0/en/developer/
    - base_path=/3.0/en/, relative_url=../user/api/, target=swagger → /3.0/en/user/api/
    - base_path=/geoserver/migration/3.0-rst-to-md/en/, relative_url=../developer/ 
      → /geoserver/migration/3.0-rst-to-md/en/developer/
    
    Returns:
        str: Absolute path with leading slash
    """
    # Parse the relative URL to extract the target path
    # Remove ../ prefix (we're constructing from base_path, not navigating relatively)
    target_path = relative_url.replace('../', '')
    
    # Combine base_path with target path
    absolute_path = base_path.rstrip('/') + '/' + target_path.lstrip('/')
    
    # Ensure leading slash
    if not absolute_path.startswith('/'):
        absolute_path = '/' + absolute_path
    
    return absolute_path
```

**GitHub Actions Workflow Changes:**

For migration branch testing, add environment variable to the workflow:

```yaml
- name: Deploy User Manual (Migration Branch)
  working-directory: doc/en/user
  env:
    DOCS_BASE_PATH: /geoserver/migration/3.0-rst-to-md
  run: |
    mkdocs build
```

## Testing Strategy

### Validation Approach

The testing strategy follows a two-phase approach: first, surface counterexamples that demonstrate the bug on unfixed code, then verify the fix works correctly and preserves existing behavior.

### Exploratory Bug Condition Checking

**Goal**: Surface counterexamples that demonstrate the bug BEFORE implementing the fix. Confirm or refute the root cause analysis. If we refute, we will need to re-hypothesize.

**Test Plan**: Build the documentation with the current (unfixed) code and manually test navigation from pages at different nesting levels. Record the actual URLs generated and compare them to expected URLs.

**Test Cases**:
1. **Level 2 Navigation Test**: Navigate to `/en/user/introduction/` and click "Developer Manual" (will fail on unfixed code - navigates to `/en/user/developer/`)
2. **Level 3 Navigation Test**: Navigate to `/en/user/introduction/overview/` and click "Documentation Guide" (will fail on unfixed code - navigates to `/en/user/introduction/docguide/`)
3. **Cross-Type Navigation Test**: Navigate to `/en/developer/core/` and click "User Manual" (will fail on unfixed code - navigates to `/en/developer/user/`)
4. **API Link Test**: Navigate to `/en/docguide/contributing/` and click "Swagger APIs" (will fail on unfixed code - navigates to `/en/docguide/user/api/`)

**Expected Counterexamples**:
- Doc switcher links navigate to incorrect paths that include parent directories from the current page location
- Possible causes: relative path resolution from current page, insufficient `../` operators, missing base URL context

### Fix Checking

**Goal**: Verify that for all inputs where the bug condition holds, the fixed function produces the expected behavior.

**Pseudocode:**
```
FOR ALL page WHERE page.nestingLevel >= 2 DO
  FOR ALL docType IN ['user', 'developer', 'docguide', 'swagger'] DO
    url := getDocSwitcherUrl(page, docType)
    ASSERT url starts with '/' (absolute path)
    ASSERT url contains correct docType at root level
    ASSERT url does NOT contain parent directories from current page
  END FOR
END FOR
```

**Test Plan**: After implementing the fix, build the documentation and verify that doc_switcher URLs are absolute paths that correctly navigate to the target documentation type.

**Test Cases**:
1. **Level 2 Fixed Navigation**: From `/en/user/introduction/`, clicking "Developer Manual" navigates to `/3.0/en/developer/` (or appropriate version path)
2. **Level 3 Fixed Navigation**: From `/en/user/introduction/overview/`, clicking "Documentation Guide" navigates to `/3.0/en/docguide/`
3. **Multi-Branch Fixed Navigation**: In migration branch deployment, URLs include the branch prefix (e.g., `/geoserver/migration/3.0-rst-to-md/en/developer/`)
4. **API Link Fixed**: From any nesting level, clicking "Swagger APIs" navigates to the correct API path

### Preservation Checking

**Goal**: Verify that for all inputs where the bug condition does NOT hold, the fixed function produces the same result as the original function.

**Pseudocode:**
```
FOR ALL page WHERE page.nestingLevel = 1 DO
  FOR ALL docType IN ['user', 'developer', 'docguide', 'swagger'] DO
    url_original := getDocSwitcherUrl_original(page, docType)
    url_fixed := getDocSwitcherUrl_fixed(page, docType)
    ASSERT url_fixed navigates to same destination as url_original
  END FOR
END FOR
```

**Testing Approach**: Property-based testing is recommended for preservation checking because:
- It generates many test cases automatically across the input domain
- It catches edge cases that manual unit tests might miss
- It provides strong guarantees that behavior is unchanged for all non-buggy inputs

**Test Plan**: Observe behavior on UNFIXED code first for level 1 navigation, then verify that the fixed code produces equivalent navigation results (same destination, even if URL format differs from relative to absolute).

**Test Cases**:
1. **Level 1 Navigation Preservation**: Verify clicking doc_switcher links from `/en/user/` navigates to correct destinations
2. **Configuration Loading Preservation**: Verify doc_switcher.yml is still loaded by version.py without errors
3. **Template Data Structure Preservation**: Verify templates receive doc_switcher array with label, url, type fields
4. **Build Process Preservation**: Verify `mkdocs build` and `mkdocs serve` work without additional arguments

### Unit Tests

- Test `extract_base_path` function with various site_url formats (GitHub Pages, multi-branch, local development)
- Test `construct_absolute_path` function with different relative URL patterns
- Test doc_switcher URL generation at different nesting levels
- Test edge cases (missing site_url, malformed URLs, missing doc_type)

### Property-Based Tests

- Generate random page paths at various nesting levels and verify doc_switcher URLs are always absolute
- Generate random site_url configurations and verify base path extraction works correctly
- Test that all doc_switcher URLs start with `/` and contain the correct doc type
- Verify preservation: at level 1, absolute URLs navigate to the same destination as relative URLs would

### Integration Tests

- Build all three documentation types (user, developer, docguide) with the fix
- Test navigation between doc types from pages at levels 1, 2, 3, and 4
- Test in both local development (`mkdocs serve`) and production build (`mkdocs build`)
- Test with multi-branch deployment structure (migration branch)
- Verify version selector and doc_switcher work together correctly
