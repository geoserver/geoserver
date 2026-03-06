# Download Hook - 10 Real Examples

This document shows 10 real examples of download links in the GeoServer documentation and how the `download_files.py` hook handles them.

---

## Example 1: Shapefile Tutorial Data

**Location:** `doc/en/user/docs/gettingstarted/shapefile-quickstart/index.md`

**Markdown:**
```markdown
1. Download the file [nyc_roads.zip](nyc_roads.zip). This archive contains a shapefile 
   of roads from New York City that will be used in this tutorial.
```

**How the hook handles it:**
- **Scans:** Detects link to `nyc_roads.zip` in the markdown file
- **Source:** `doc/en/user/docs/gettingstarted/shapefile-quickstart/nyc_roads.zip`
- **Destination:** `target/html/gettingstarted/shapefile-quickstart/nyc_roads.zip`
- **Result:** ✅ File copied, download link works in built docs

---

## Example 2: PostGIS Tutorial Data

**Location:** `doc/en/user/docs/gettingstarted/postgis-quickstart/index.md`

**Markdown:**
```markdown
1. Download the file [nyc_buildings.zip](nyc_buildings.zip). It contains a PostGIS dump 
   of a dataset of buildings from New York City.
```

**How the hook handles it:**
- **Scans:** Detects link to `nyc_buildings.zip`
- **Source:** `doc/en/user/docs/gettingstarted/postgis-quickstart/nyc_buildings.zip`
- **Destination:** `target/html/gettingstarted/postgis-quickstart/nyc_buildings.zip`
- **Result:** ✅ File copied, download link works

---

## Example 3: SLD Cookbook Examples

**Location:** `doc/en/user/docs/styling/sld/cookbook/points.md`

**Markdown:**
```markdown
The [points layer](../../sld/cookbook/artifacts/sld_cookbook_point.zip) used for 
the examples below contains name and population information...

[Download the points shapefile](../../sld/cookbook/artifacts/sld_cookbook_point.zip)
```

**How the hook handles it:**
- **Scans:** Detects relative path `../../sld/cookbook/artifacts/sld_cookbook_point.zip`
- **Source:** `doc/en/user/docs/styling/sld/cookbook/artifacts/sld_cookbook_point.zip`
- **Destination:** `target/html/styling/sld/cookbook/artifacts/sld_cookbook_point.zip`
- **Result:** ✅ Relative paths resolved correctly, file copied

---

## Example 4: Style Files (SLD)

**Location:** `doc/en/user/docs/styling/sld/cookbook/points.md`

**Markdown:**
```markdown
[Download the "Simple point" SLD](artifacts/point_simplepoint.sld)
```

**How the hook handles it:**
- **Scans:** Detects `.sld` file extension
- **Source:** `doc/en/user/docs/styling/sld/cookbook/artifacts/point_simplepoint.sld`
- **Destination:** `target/html/styling/sld/cookbook/artifacts/point_simplepoint.sld`
- **Result:** ✅ SLD style files automatically handled

---

## Example 5: Configuration Properties Files

**Location:** `doc/en/user/docs/extensions/controlflow/index.md`

**Markdown:**
```markdown
Download the example [controlflow.properties](controlflow.properties) file.
```

**How the hook handles it:**
- **Scans:** Detects `.properties` file extension
- **Source:** `doc/en/user/docs/extensions/controlflow/controlflow.properties`
- **Destination:** `target/html/extensions/controlflow/controlflow.properties`
- **Result:** ✅ Configuration files automatically handled

---

## Example 6: XML Configuration Files

**Location:** `doc/en/user/docs/data/app-schema/tutorial.md`

**Markdown:**
```markdown
Download the mapping file: [MappedFeature_MappingFile.xml](MappedFeature_MappingFile.xml)
```

**How the hook handles it:**
- **Scans:** Detects `.xml` file extension
- **Source:** `doc/en/user/docs/data/app-schema/MappedFeature_MappingFile.xml`
- **Destination:** `target/html/data/app-schema/MappedFeature_MappingFile.xml`
- **Result:** ✅ XML files automatically handled

---

## Example 7: Logging Configuration

**Location:** `doc/en/user/docs/configuration/logging.md`

**Markdown:**
```markdown
- [DEFAULT_LOGGING](../../../../src/main/src/main/resources/DEFAULT_LOGGING.xml)
- [VERBOSE_LOGGING](../../../../src/main/src/main/resources/VERBOSE_LOGGING.xml)
```

**How the hook handles it:**
- **Scans:** Detects complex relative path with multiple `../` segments
- **Source:** `doc/en/user/docs/configuration/../../../../src/main/src/main/resources/DEFAULT_LOGGING.xml`
- **Resolves to:** `src/main/src/main/resources/DEFAULT_LOGGING.xml`
- **Result:** ⚠️ File not in docs directory - logged as warning (expected for source code files)

---

## Example 8: Tutorial Metadata Files

**Location:** `doc/en/user/docs/tutorials/metadata/index.md`

**Markdown:**
```markdown
UI configuration [metadata-ui.yaml](files/metadata-ui.yaml)

Translate keys to labels [metadata.properties](files/metadata.properties)

Map metadata attributes to xml [MD_Metadata.properties](files/MD_Metadata.properties)
```

**How the hook handles it:**
- **Scans:** Detects multiple files in subdirectory
- **Source:** `doc/en/user/docs/tutorials/metadata/files/metadata-ui.yaml`
- **Destination:** `target/html/tutorials/metadata/files/metadata-ui.yaml`
- **Result:** ✅ All three files copied with directory structure preserved

---

## Example 9: ImageMosaic Tutorial Dataset

**Location:** `doc/en/user/docs/tutorials/imagemosaic_timeseries/imagemosaic_timeseries.md`

**Markdown:**
```markdown
The dataset used in the tutorial can be downloaded [Here](snowLZWdataset.zip). 
It contains 3 image files and a .sld file.
```

**How the hook handles it:**
- **Scans:** Detects link with descriptive text "Here"
- **Source:** `doc/en/user/docs/tutorials/imagemosaic_timeseries/snowLZWdataset.zip`
- **Destination:** `target/html/tutorials/imagemosaic_timeseries/snowLZWdataset.zip`
- **Result:** ✅ Link text doesn't matter, file path is what counts

---

## Example 10: External URLs (Skipped)

**Location:** `doc/en/user/docs/styling/workshop/setup/data.md`

**Markdown:**
```markdown
- [styling-workshop-vector.zip](http://echobase.boundlessgeo.com/~jgarnett/GeoServerStyling/styling-workshop-vector.zip)
- [styling-workshop-raster.zip](styling-workshop-raster.zip)
```

**How the hook handles it:**
- **First link:** Starts with `http://` - **SKIPPED** (external URL, not copied)
- **Second link:** Relative path - **PROCESSED** normally
- **Result:** ✅ External URLs ignored, local files handled

---

## Summary Table

| Example | File Type | Path Type | Status |
|---------|-----------|-----------|--------|
| 1. nyc_roads.zip | Archive | Same directory | ✅ Copied |
| 2. nyc_buildings.zip | Archive | Same directory | ✅ Copied |
| 3. sld_cookbook_point.zip | Archive | Relative path (../..) | ✅ Copied |
| 4. point_simplepoint.sld | Style | Subdirectory | ✅ Copied |
| 5. controlflow.properties | Config | Same directory | ✅ Copied |
| 6. MappedFeature_MappingFile.xml | Config | Same directory | ✅ Copied |
| 7. DEFAULT_LOGGING.xml | Config | Source code path | ⚠️ Warning |
| 8. metadata-ui.yaml | Config | Subdirectory | ✅ Copied |
| 9. snowLZWdataset.zip | Archive | Same directory | ✅ Copied |
| 10. External URL | Archive | HTTP URL | ⏭️ Skipped |

---

## Build Output Example

When you run `mkdocs build --verbose`, you'll see:

```
INFO    -  Scanning for download links in documentation...
INFO    -  Found 210 download file references
INFO    -  Copying 210 download files to output directory...
DEBUG   -  Copied: docs/gettingstarted/shapefile-quickstart/nyc_roads.zip -> target/html/gettingstarted/shapefile-quickstart/nyc_roads.zip
DEBUG   -  Copied: docs/gettingstarted/postgis-quickstart/nyc_buildings.zip -> target/html/gettingstarted/postgis-quickstart/nyc_buildings.zip
DEBUG   -  Copied: docs/styling/sld/cookbook/artifacts/point_simplepoint.sld -> target/html/styling/sld/cookbook/artifacts/point_simplepoint.sld
WARNING -  Download file not found: docs/configuration/../../../../src/main/src/main/resources/DEFAULT_LOGGING.xml
INFO    -  Successfully copied 133 download files
```

---

## Key Takeaways

1. **Automatic Detection**: No special markup needed - just use standard Markdown links
2. **Relative Paths**: The hook correctly resolves `../` and subdirectory paths
3. **Multiple File Types**: Handles .zip, .xml, .properties, .sld, .yaml, .json, etc.
4. **External URLs**: Automatically skips http:// and https:// links
5. **Directory Structure**: Preserves the same folder structure in output
6. **Missing Files**: Logs warnings but doesn't fail the build
7. **No Configuration**: Works automatically once added to mkdocs.yml

The hook makes download files "just work" without any manual intervention!
