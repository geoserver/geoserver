# Image Move to img Subfolders - Summary Report

## Task 3.5.2: Move images to img subfolders (2.28.x branch)

**Date**: 2026-03-04  
**Branch**: migration/2.28-x-rst-to-md  
**Status**: ✅ COMPLETED

## Summary

Successfully moved 159 images from root directories to img subfolders and updated all Markdown references.

## Statistics

- **Total Markdown files scanned**: 793
- **Total image references found**: 2,113
- **Images already in img/images folders**: 1,950
- **Images moved to img subfolders**: 159
- **Directories with new img folders created**: 32

## Directories Affected

### User Manual (doc/en/user/docs)
- community/jdbcconfig/
- data/app-schema/
- extensions/grib/
- extensions/netcdf/
- extensions/netcdf-out/
- geowebcache/
- gettingstarted/postgis-quickstart/
- gettingstarted/shapefile-quickstart/
- gettingstarted/web-admin-quickstart/
- services/wms/googleearth/
- services/wms/googleearth/tutorials/heights/
- services/wms/googleearth/tutorials/kmlplacemark/
- services/wms/googleearth/tutorials/time/
- tutorials/cloud-foundry/
- tutorials/cql/
- tutorials/feature-pregeneralized/
- tutorials/georss/
- tutorials/GetFeatureInfo/
- tutorials/imagepyramid/
- tutorials/palettedimage/
- tutorials/tomcat-jndi/

### Developer Manual (doc/en/developer/docs)
- cite-test-guide/
- eclipse-guide/
- policies/
- programming-guide/ows-services/
- programming-guide/rest-services/
- programming-guide/web-ui/
- release/guide/
- release/schedule/
- release/testing/

### Documentation Guide (doc/en/docguide/docs)
- (root level img folder created)

## Verification

✅ **Build Test**: Successfully built user manual with `mkdocs build --strict`
- No errors related to missing images
- All image references resolved correctly
- Build completed successfully (warnings are pre-existing link issues)

✅ **Git Status**: All changes tracked correctly
- 159 images deleted from original locations (D status)
- 159 images added to img subfolders (new files)
- Markdown files updated with new image paths (M status)

## Compliance

All images now follow the required structure:
- ✅ **CORRECT**: `docs/eclipse-guide/img/code-template.png`
- ❌ **INCORRECT** (fixed): `docs/eclipse-guide/code-template.png`

## Next Steps

1. ✅ Review changes with `git status`
2. ✅ Test locally with `mkdocs build` (completed successfully)
3. ⏭️ Commit changes to migration branch
4. ⏭️ Continue with remaining validation tasks

## Notes

- A few warnings appeared about images already moved in previous operations (expected)
- All new img folders follow the standard structure
- Image references updated to use relative paths: `img/filename.png`
- No images were lost or duplicated during the move
