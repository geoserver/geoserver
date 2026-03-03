# Include Statement Issues - Complete Fix Summary

## Problem

After previous fixes for macro syntax errors, the production/container page and other pages still showed "Macro Syntax Error" messages. Investigation of GitHub Actions logs revealed **4 types** of include statement issues.

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

### 4. Include Statements with start/end Parameters
**Issue**: Sphinx literalinclude directives with start-after/end-before were incorrectly converted to Jinja2 include with start/end parameters.

**Example**:
```markdown
{% include "../../../../src/web/app/src/main/webapp/WEB-INF/web.xml" start="<!-- Uncomment following filter-mapping to enable CORS" end="-->" %}
```

**Impact**: Jinja2 include does NOT support start/end parameters, causing "expected token 'end of statement block', got 'start'" errors.

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

### Fix 3: Manual Fix for Nested Includes
**Files Fixed Manually**: 4 files
- `configuration/tools/resource/examples.md`
- `developer/docs/programming-guide/web-ui/overview.md`
- `styling/workshop/ysld/ysld.md`
- `styling/workshop/mbstyle/mbstyle.md`

Manually fixed nested include statements:

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

### Fix 4: Fix Include with start/end Parameters
**Script**: `fix_include_with_params.py`

Replaced invalid Jinja2 syntax with comments:

**Before**:
```markdown
{% include "../../../../src/web/app/src/main/webapp/WEB-INF/web.xml" start="<!-- Uncomment following filter-mapping to enable CORS" end="-->" %}
```

**After**:
```markdown
<!-- Include with start/end not supported: ../../../../src/web/app/src/main/webapp/WEB-INF/web.xml -->
<!-- Extract: from "<!-- Uncomment following filter-mapping to enable CORS" to "-->" -->
<!-- TODO: Copy relevant section to docs directory -->
```

**Files Fixed**: 2 files
- `production/container.md` ⭐ (the originally reported page!)
- `developer/docs/qa-guide/index.md`

## Total Impact

- **22 files fixed** across user, developer, and docguide manuals
- **4 types of issues** resolved
- **All macro syntax errors** related to include statements should now be resolved

## Verification

Once GitHub Actions completes (~5-10 minutes), verify:
- https://petersmythe.github.io/geoserver/migration/2.28-x-rst-to-md/en/user/production/container/ ⭐
- https://petersmythe.github.io/geoserver/migration/2.28-x-rst-to-md/en/user/introduction/license/
- https://petersmythe.github.io/geoserver/migration/2.28-x-rst-to-md/en/user/extensions/controlflow/

All pages should render correctly without "Macro Syntax Error" messages.

## Scripts Created

1. `fix_malformed_includes.py` - Initial attempt to fix malformed includes
2. `fix_all_include_issues.py` - Comprehensive fix for code block includes and external paths
3. `fix_include_with_params.py` - Fix includes with invalid start/end parameters
4. `fix_incomplete_includes.py` - Fix incomplete includes and malformed {%raw%} tags
5. `fix_anchor_syntax.py` - Fix {#anchor} syntax that conflicts with Jinja2

## Commits

- **Commit 1**: da4ee8aef8 - "Fix include statement issues: escape code block examples and comment out external paths"
- **Commit 2**: 217b69dc01 - "Fix include statements with start/end parameters and remaining nested includes"
- **Commit 3**: a3a89e3ef0 - "Update tasks.md and summary with complete include fix documentation"
- **Commit 4**: b23d88600f - "Fix remaining macro errors: incomplete includes, anchor syntax, and add missing XML content"
- **Branch**: migration/2.28-x-rst-to-md
- **Pushed**: Yes

## Notes for 3.0 Branch

These same fixes will need to be applied to the 3.0 branch during Phase 4 (task 5.5). The task has been updated with comprehensive instructions including:
- Run fix_all_include_issues.py for code block includes and external paths
- Run fix_include_with_params.py for start/end parameter issues
- Manually fix any remaining nested include statements
- Verify GitHub Actions logs for any remaining errors

## Future Improvements

For files that need to include LICENSE.md or other external files:
1. Copy the files to appropriate locations within `doc/en/user/docs/` directory
2. Update include paths to reference the local copies
3. Or use a different approach (e.g., link to GitHub raw file, or copy content directly into the markdown)

## Complete Fix Chain

This issue required **SEVEN separate fix types**:
1. Multi-line include syntax → single-line (10 files) - Previous fix
2. Macro rendering for {{ version }} (3 files) - Previous fix
3. Include frontmatter for {% include %} (36 files) - Previous fix
4. **Code block includes** → wrapped in {%raw%} (15 files) ⭐ FIX #1
5. **External path includes** → commented out (5 files) ⭐ FIX #2
6. **Nested includes** → fixed manually (4 files) ⭐ FIX #3
7. **Include with start/end** → commented out (2 files) ⭐ FIX #4
8. **Incomplete includes** → fixed malformed {%raw%} tags (13 files) ⭐ FIX #5
9. **Anchor syntax {#anchor}** → wrapped in {%raw%} (3 files) ⭐ FIX #6
10. **Missing XML content** → manually added from source (1 file) ⭐ FIX #7
