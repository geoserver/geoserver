# Automated Postprocessors - Implementation Summary

## Overview

Implemented two automated postprocessors in `migration.py` that handle previously manual tasks during the conversion pipeline:
1. **Interpreted text roles** → Markdown links
2. **Variable frontmatter** → Automatic frontmatter addition

## Implementation Details

### Location
**File:** `migration.py`  
**Class:** `TranslationToolWrapper`  
**Methods:** 
- `run_interpreted_text_postprocess()` - Step 6
- `run_frontmatter_postprocess()` - Step 7

### Pipeline Integration
The postprocessors run after standard conversion:
1. Init
2. Scan
3. Migrate
4. Nav generation
5. Standard postprocessing
6. **Interpreted text conversion** ← NEW
7. **Frontmatter addition** ← NEW

---

## Postprocessor 1: Interpreted Text Roles

### Conversion Logic

#### Pattern Matching
```python
pattern = r'`([^`]+) <([^>]+)>`\{\.interpreted-text role="([^"]+)"\}'
```

Matches Sphinx interpreted text roles in the format:
```markdown
`text <url>`{.interpreted-text role="rolename"}
```

#### Role Mappings
```python
role_mappings = {
    'website': 'https://geoserver.org/',
    'developer': 'https://docs.geoserver.org/latest/en/developer/',
    'user': '../user/',  # Relative path for user guide
}
```

#### Conversion Examples

| Input (Sphinx) | Output (Markdown) |
|----------------|-------------------|
| `` `Nightly <release/main>`{.interpreted-text role="website"} `` | `[Nightly](https://geoserver.org/release/main)` |
| `` `community modules <policies/community-modules.html>`{.interpreted-text role="developer"} `` | `[community modules](https://docs.geoserver.org/latest/en/developer/policies/community-modules.html)` |
| `` `security <security/index>`{.interpreted-text role="user"} `` | `[security](../user/security/index)` |

### Error Handling

- **Unknown roles:** Preserved as-is with warning message
- **File processing errors:** Logged but don't stop conversion
- **Non-critical:** Failures don't abort the migration

### Output

The postprocessors report:
```
Step 6: Converting interpreted text roles to Markdown links...
[OK] Converted X interpreted text roles in Y files

Step 7: Adding frontmatter to files with variables...
[OK] Added frontmatter to X files (skipped Y with existing frontmatter)
```

---

## Postprocessor 2: Variable Frontmatter

### Purpose
Automatically add `render_macros: true` frontmatter to files that use mkdocs-macros variables (`{{ version }}` or `{{ release }}`).

### Detection Logic

```python
# Pattern to detect mkdocs-macros variables
variable_pattern = r'\{\{\s*(version|release)\s*\}\}'
```

Detects:
- `{{ version }}`
- `{{ release }}`
- `{{version}}` (no spaces)
- `{{ version }}` (extra spaces)

### Frontmatter Handling

**Case 1: No existing frontmatter**
```markdown
# Before
# Installation Guide
Download GeoServer {{ version }}...

# After
---
render_macros: true
---

# Installation Guide
Download GeoServer {{ version }}...
```

**Case 2: Existing frontmatter without render_macros**
```markdown
# Before
---
title: Installation
---
Download GeoServer {{ version }}...

# After
---
render_macros: true
title: Installation
---
Download GeoServer {{ version }}...
```

**Case 3: Already has render_macros**
```markdown
# Before & After (no change)
---
render_macros: true
---
Download GeoServer {{ version }}...
```

### Test Results

**Test on docker.md (already has frontmatter):**
```
File: docker.md
  Variables found: {'version', 'release'}
  Status: Already has render_macros frontmatter - SKIPPED
```

**Test on file without frontmatter:**
```
File: test_no_frontmatter.md
  Variables found: {'release', 'version'}
  Status: No frontmatter, adding full frontmatter block
  ✓ Frontmatter added
```

---

## Combined Testing

### Test Scripts

1. **test_interpreted_text_fix.py** - Tests interpreted text conversion
2. **test_frontmatter_fix.py** - Tests frontmatter addition

Both scripts can be run standalone for verification.

### Test Results

**Sample conversion on docker.md:**
```
Converting: `Nightly <release/main>` (role=website)
       To: [Nightly](https://geoserver.org/release/main)

Converting: `community modules <policies/community-modules.html>` (role=developer)
       To: [community modules](https://docs.geoserver.org/latest/en/developer/policies/community-modules.html)

✓ Processed: docker.md (2 conversions)

Summary: Converted 2 interpreted text roles in 1 files
```

### Verification

Before conversion:
```markdown
`Nightly <release/main>`{.interpreted-text role="website"}
```

After conversion:
```markdown
[Nightly](https://geoserver.org/release/main)
```

## Benefits

### Automated
- No manual search-and-replace needed
- Consistent conversion across all files
- Reduces manual review time significantly

### Accurate
- Uses regex pattern matching
- Handles edge cases (existing frontmatter, spacing variations)
- Preserves unknown roles/patterns for manual review

### Integrated
- Runs automatically in migration pipeline
- No separate script execution needed
- Part of standard conversion workflow

## Impact on Manual Review

### Before Implementation
Manual review required:
1. **Interpreted text:** Search, identify role, construct URL, replace (~2 hours)
2. **Variable frontmatter:** Find files with variables, add frontmatter (~1 hour)
3. Verify conversions
4. Test links

**Estimated time:** 5-8 hours

### After Implementation
Manual review only needs to:
1. Verify interpreted text conversions are correct
2. Verify frontmatter was added correctly
3. Test a few sample links
4. Check for unknown roles (warnings)

**Estimated time:** 2-3 hours

### Time Savings
**~3-5 hours per branch** (3.0 and 2.28.x)  
**Total savings: ~6-10 hours**

## Configuration

### Adding New Roles

To add support for additional interpreted text roles, update the `role_mappings` dictionary in `migration.py`:

```python
role_mappings = {
    'website': 'https://geoserver.org/',
    'developer': 'https://docs.geoserver.org/latest/en/developer/',
    'user': '../user/',
    'newrole': 'https://example.com/',  # Add new role here
}
```

### Customizing URLs

Role URLs can be customized per branch or environment by modifying the mappings before conversion.

## Known Limitations

### Relative Paths
The `user` role uses relative paths (`../user/`). This works for:
- Developer guide → User guide
- Documentation guide → User guide

But may need adjustment for:
- User guide → User guide (same directory)
- Cross-manual references

### Complex Roles
Does not handle:
- Nested roles
- Roles with special formatting
- Custom role extensions beyond the three common types

These will be preserved with warnings for manual review.

## Future Enhancements

### Potential Improvements
1. **Smart path resolution:** Detect source manual and adjust relative paths
2. **Role validation:** Check if target URLs exist
3. **Statistics tracking:** Count conversions per role type
4. **Configuration file:** External role mappings configuration

### Not Planned
- Support for all possible Sphinx roles (too many edge cases)
- Dynamic URL resolution (requires Sphinx configuration parsing)
- Bidirectional conversion (Markdown → RST)

## Documentation Updates

### Updated Files
1. **tasks.md** - Task 3.2 note updated to reflect automation
2. **INTERPRETED_TEXT_ROLES_EXPLAINED.md** - Reference document (still useful for understanding)
3. **MANUAL_REVIEW_CHECKLIST.md** - Reduced priority for interpreted text roles

### Task 3.2 Note
Changed from:
> "may not convert fully... Review and manually convert"

To:
> "are NOW AUTOMATICALLY CONVERTED... Verify conversions are correct"

## Conclusion

The interpreted text roles postprocessor successfully automates a previously manual task, saving significant time during the migration process while maintaining accuracy and providing clear feedback on any issues that require manual attention.

**Status:** ✓ Implemented and tested  
**Integration:** ✓ Added to migration.py pipeline  
**Documentation:** ✓ Updated  
**Testing:** ✓ Verified on sample files  
**Ready for:** Full conversion on 3.0 and 2.28.x branches
