# Include Statement Issues - Fix Summary

## Problem

After previous fixes for macro syntax errors, the production/container page and other pages still showed "Macro Syntax Error" messages. Investigation of GitHub Actions logs revealed multiple types of include statement issues.

## Root Causes Identified

### 1. Include Statements in Code Blocks
**Issue**: Include statements inside code blocks (~~~properties, ```markdown) were being processed by mkdocs-macros instead of being displayed as literal example code.

**Example**:
```markdown
~~~properties
{% include "extensions/controlflow/controlflow.properties" %}
~~~
```

**Impact**: These were meant to show users example configuration syntax, not actually include files.

### 2. Nested Include Statements
**Issue**: Malformed nested `{% {% include %} %}` statements from conversion tool.

**Example**:
```markdown
{% 
  {% include "extensions/controlflow/controlflow.properties" %}
%}
```

**Impact**: Invalid Jinja2 syntax causing macro syntax errors.

### 3. Include Paths Outside Docs Directory
**Issue**: Include statements trying to reference files outside the docs directory (e.g., `../../../../LICENSE.md`).

**Impact**: mkdocs-macros cannot include files outside the configured docs directory, causing TemplateNotFound errors.

## Solutions Implemented

### Fix 1: Escape Code Block Includes
**Script**: `fix_all_include_issues.py`

Wrapped include statements in code blocks with `{%raw%}...{%endraw%}` tags to prevent processing:

```markdown
~~~properties
{%raw%}{% include "extensions/controlflow/controlflow.properties" %}{%endraw%}
~~~
```

**Files Fixed**: 15 files
- `configuration/logging.md`
- `community/cog/mosaic.md`
- `community/gsr/dynamicMapLayer.md`
- `community/gsr/featureLayer.md`
- `community/gsr/featureTable.md`
- `extensions/controlflow/index.md`
- `services/wps/processes/gs.md`
- `styling/workshop/css/css.md`
- `styling/workshop/mbstyle/mbstyle.md`
- `styling/workshop/ysld/ysld.md`
- `styling/ysld/cookbook/polygons.md`
- `tutorials/imagemosaic_timeseries/imagemosaic_time-elevationseries.md`
- `tutorials/imagemosaic_timeseries/imagemosaic_timeseries.md`
- `developer/docs/qa-guide/index.md`
- `developer/docs/programming-guide/ows-services/implementing.md`

### Fix 2: Comment Out External Includes
**Script**: `fix_all_include_issues.py`

Replaced include statements that reference files outside docs directory with HTML comments:

```markdown
<!-- Include path goes outside docs directory: ../../../../LICENSE.md -->
<!-- TODO: Copy file to docs directory or use alternative approach -->
```

**Files Fixed**: 5 files
- `introduction/license.md` (LICENSE.md)
- `production/container.md` (LICENSE.md)
- `services/wps/processes/gs.md` (LICENSE.md)
- `developer/docs/policies/community-modules.md` (LICENSE.md)
- `developer/docs/qa-guide/index.md` (LICENSE.md)

### Fix 3: Manual Fix for Nested Include
**File**: `configuration/tools/resource/examples.md`

Manually fixed the nested include statement:

**Before**:
```markdown
> {% 
>   {% include "extensions/controlflow/controlflow.properties" %}
> %}
```

**After**:
```markdown
> {%raw%}{% include "extensions/controlflow/controlflow.properties" %}{%endraw%}
```

## Total Impact

- **18 files fixed** across user, developer, and docguide manuals
- **3 types of issues** resolved
- **All macro syntax errors** related to include statements should now be resolved

## Verification

Once GitHub Actions completes (~5-10 minutes), verify:
- https://petersmythe.github.io/geoserver/migration/2.28-x-rst-to-md/en/user/production/container/
- https://petersmythe.github.io/geoserver/migration/2.28-x-rst-to-md/en/user/introduction/license/
- https://petersmythe.github.io/geoserver/migration/2.28-x-rst-to-md/en/user/extensions/controlflow/

All pages should render correctly without "Macro Syntax Error" messages.

## Scripts Created

1. `fix_malformed_includes.py` - Initial attempt to fix malformed includes
2. `fix_all_include_issues.py` - Comprehensive fix for all include-related issues

## Commit

- **Commit**: da4ee8aef8
- **Message**: "Fix include statement issues: escape code block examples and comment out external paths"
- **Branch**: migration/2.28-x-rst-to-md
- **Pushed**: Yes

## Notes for 3.0 Branch

These same fixes will need to be applied to the 3.0 branch during Phase 4 (task 5.5).

## Future Improvements

For files that need to include LICENSE.md:
1. Copy LICENSE.md to `doc/en/user/docs/` directory
2. Update include paths to reference the local copy
3. Or use a different approach (e.g., link to GitHub raw file)
