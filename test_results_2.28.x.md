# Test Results for 2.28.x Branch - Task 5.3

## Summary

Tests were re-run on the migration/2.28-x-rst-to-md branch after cherry-picking the download link fix.

## Test 1: Preservation Property Test

**Status**: ✓ PASSED

**Command**: `python run_preservation_test.py`

**Results**:
- Total links that must be preserved: 57
- Internal documentation links: 52
- External reference links: 5
- All non-download links remained unchanged

**Conclusion**: The preservation property holds - no regressions were introduced.

---

## Test 2: Bug Condition Exploration Test

**Status**: ✗ FAILED (Partial Fix Applied)

**Command**: `python test_bug_condition.py`

**Results**:
- Total download links checked: 4
- Links with full filename patterns: 0
- Links still missing full filenames: 4

**Counterexamples**:
1. File: user/docs/styling/css/install.md
   - Link text: `css` (should be `geoserver-{{ release }}-css-plugin.zip`)
   - URL: Correctly updated with full filename

2. File: user/docs/styling/mbstyle/installing.md
   - Link text: `mbstyle` (should be `geoserver-{{ release }}-mbstyle-plugin.zip`)
   - URL: Correctly updated with full filename

3. File: user/docs/community/schemaless-features/install.md
   - Link text: `mongodb-schemaless` (should be `geoserver-{{ snapshot }}-mongodb-schemaless-plugin.zip`)
   - URL: Correctly updated with full filename

4. File: user/docs/extensions/wps-download/index.md
   - Link text: `wps-download` (should be `geoserver-{{ release }}-wps-download-plugin.zip`)
   - URL: Correctly updated with full filename

**Analysis**:
The cherry-pick from the 3.0 branch only partially applied the fix:
- ✓ URLs were correctly updated with full filenames
- ✗ Link TEXT was NOT updated - still shows just plugin names

**Root Cause**:
The 2.28.x and 3.0 branches have different documentation formats:
- 3.0 branch: Uses format `{{ release }} [geoserver-{{ release }}-css-plugin.zip](URL)`
- 2.28.x branch: Uses format `{{ release }} [css](URL)`

The fix scripts expect the link text to already contain the full filename, but on 2.28.x it doesn't.

---

## Test 3: Git Diff Verification

**Command**: `git diff b952b1241b~1 b952b1241b -- doc/`

**Results**:
- Only 1 file was modified in the doc/ directory
- Changes were primarily formatting fixes (list structure)
- No download link text transformations were applied

**Conclusion**: The fix commit on 2.28.x did not update download link text as expected.

---

## Overall Assessment

**EXPECTED OUTCOME**: All tests PASS on 2.28.x branch
**ACTUAL OUTCOME**: Preservation test PASSED, Bug condition test FAILED

**Recommendation**: 
The 2.28.x branch requires additional work to complete the download link fix. A script needs to be created or adapted to transform link text from the current format `[plugin-name]` to the full filename format `[geoserver-{{ macro }}-plugin-name-plugin.zip]`.

**Next Steps**:
1. Create a new script to transform link text on 2.28.x branch
2. Re-run the bug condition exploration test to verify the fix
3. Re-run the preservation test to ensure no regressions
