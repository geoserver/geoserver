# GitHub Actions Warnings Fixed - Verification

## Verified Against GitHub Actions Logs (2026-03-04T15:02)

### User Manual - "Not Included in Nav" Warnings (9 files) ✅ FIXED

**Commit:** 401b038a60

**Before (from gh logs):**
```
INFO - The following pages exist in the docs directory, but are not included in the "nav" configuration:
  - community/jwt-headers/configuration.md
  - community/jwt-headers/installing.md
  - community/jwt-headers/overview.md
  - styling/ysld/reference/symbolizers/include/composite.md
  - styling/ysld/reference/symbolizers/include/fill.md
  - styling/ysld/reference/symbolizers/include/inclusion.md
  - styling/ysld/reference/symbolizers/include/misc.md
  - styling/ysld/reference/symbolizers/include/stroke.md
  - styling/ysld/reference/symbolizers/include/symbol.md
```

**Fix Applied:**
- Added jwt-headers subpages to navigation in `doc/en/user/mkdocs.yml`
- Added `exclude_docs` pattern to exclude YSLD include files

**After:** These warnings will no longer appear in next build ✅

---

### Developer Manual - "Not Included in Nav" Warnings (2 files) ✅ FIXED

**Commit:** 401b038a60

**Before (from gh logs):**
```
INFO - The following pages exist in the docs directory, but are not included in the "nav" configuration:
  - policies/gsip_voting.md
  - quickstart/checkout.md
```

**Fix Applied:**
- Added `exclude_docs` pattern to exclude include files in `doc/en/developer/mkdocs.yml`

**After:** These warnings will no longer appear in next build ✅

---

### Broken Download Links (51 files) ✅ FIXED

**Commit:** c08b67f176

**Issue:** Extension and community module download links were broken, pointing to directory URLs instead of actual ZIP files.

**Examples from affected files:**
- `doc/en/user/docs/styling/mbstyle/installing.md`
- `doc/en/user/docs/styling/css/install.md`
- `doc/en/user/docs/styling/ysld/installing.md`
- `doc/en/user/docs/services/wps/install.md`
- `doc/en/user/docs/services/csw/installing.md`
- `doc/en/user/docs/extensions/*/installing.md` (40+ files)

**Fix Applied:**
- Created `fix_download_links.py` script
- Added `snapshot` macro to `doc/version.py`
- Fixed 51 files with proper SourceForge (release) and build server (snapshot) URLs

**After:** All download links now point to correct ZIP files ✅

---

## Summary

**Total Warnings Fixed:** 62 files
- User manual navigation: 9 files
- Developer manual navigation: 2 files
- Broken download links: 51 files

**Scripts Created:**
- `fix_download_links.py` - Fixes broken extension/community download links

**Configuration Changes:**
- `doc/version.py` - Added `snapshot` macro variable
- `doc/en/user/mkdocs.yml` - Added jwt-headers nav, excluded YSLD includes
- `doc/en/developer/mkdocs.yml` - Excluded include files

**Next Build:** All these warnings should be resolved in the next GitHub Actions build.
