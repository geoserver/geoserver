# PDF Generation Configuration and Testing Notes

## Task 3.6: Test PDF Generation

### Current Status: NOT WORKING

**Issue**: The mkdocs-with-pdf plugin is **incompatible** with the mkdocs-material theme due to CSS Grid layout issues in WeasyPrint.

**Error**: `TypeError: 'FunctionBlock' object is not subscriptable` when WeasyPrint tries to process Material theme's CSS Grid layouts.

**Time to Failure**: 
- Original: 25+ minutes (processing all pages before failing)
- Optimized: ~1.5 minutes (fails fast on smallest manual)

### How RST/Sphinx Currently Does PDF Generation

The existing RST documentation uses a **completely different approach**:

**Sphinx + LaTeX + pdflatex**:
1. Sphinx converts RST → LaTeX (.tex files)
2. pdflatex compiles LaTeX → PDF
3. Requires full LaTeX installation (MiKTeX on Windows, TeX Live on Linux)
4. Uses custom LaTeX styling (Palatino font, Sonny chapter style)
5. Configured in `conf.py` files with `latex_documents` and `latex_elements`

**Build command**: `sphinx-build -b latex` then `make latexpdf`

This approach:
- ✅ Works reliably for complex documentation
- ✅ Professional PDF output with proper typography
- ✅ Handles large documents well
- ❌ Requires LaTeX installation (~2-4GB)
- ❌ More complex build process

### Why mkdocs-with-pdf Doesn't Work

**mkdocs-with-pdf uses WeasyPrint**:
1. MkDocs converts Markdown → HTML (with Material theme CSS)
2. WeasyPrint renders HTML+CSS → PDF
3. No LaTeX required (uses Python + GTK3 libraries)

**The problem**: WeasyPrint has incomplete CSS Grid support. The Material theme uses CSS Grid extensively for layout, which causes WeasyPrint to crash during PDF generation.

This is a known limitation documented in:
- https://github.com/orzih/mkdocs-with-pdf/issues
- https://github.com/Kozea/WeasyPrint/issues (CSS Grid support)

### Attempted Solutions

1. ✗ Pinned WeasyPrint versions (59.0, 0.9.3) - API incompatibilities
2. ✗ Custom CSS overrides to disable Grid - Applied too late in rendering pipeline
3. ✗ Latest versions - Still hits CSS Grid parsing errors

### Recommended Solutions

#### Option 1: Use a Different Theme for PDF (Recommended)

Create a separate mkdocs.yml for PDF generation that uses a simpler theme:

```yaml
# mkdocs-pdf.yml
site_name: GeoServer User Manual
theme:
  name: readthedocs  # or 'mkdocs' (default theme)

plugins:
  - search
  - with-pdf:
      output_path: pdf/geoserver-user-manual.pdf
```

Build with: `mkdocs build -f mkdocs-pdf.yml`

#### Option 2: Use mkdocs-pdf-export-plugin

This plugin uses a different rendering approach (headless Chrome) that handles Material theme better:

```bash
pip install mkdocs-pdf-export-plugin
```

```yaml
plugins:
  - pdf-export:
      combined: true
      combined_output_path: pdf/manual.pdf
```

#### Option 3: Disable PDF Generation

Since the HTML documentation works perfectly, PDF generation can be considered optional. The plugin is already conditionally enabled via `ENABLE_PDF_EXPORT` environment variable.

### Configuration Completed

The PDF generation plugin configuration is in place in all three mkdocs.yml files:

1. **User Manual** (`doc/en/user/mkdocs.yml`)
2. **Developer Manual** (`doc/en/developer/mkdocs.yml`)  
3. **Documentation Guide** (`doc/en/docguide/mkdocs.yml`)

```yaml
plugins:
  - with-pdf:
      enabled_if_env: ENABLE_PDF_EXPORT
      author: GeoServer Project
      copyright: Copyright © 2024 Open Source Geospatial Foundation
      cover_title: [Manual Name]
      cover_subtitle: Version 3.0
      output_path: pdf/[filename].pdf
```

### Testing Infrastructure

Created fast-fail testing setup:

- `test-pdf-quick.sh` - Version compatibility test (completes in ~47 seconds)
- `docker-compose.pdf-test.yml` - Full PDF build test with:
  - Tests smallest manual first (Documentation Guide)
  - 2-minute timeout to fail fast
  - Only proceeds to larger manuals if first succeeds
  - Fails in ~1.5 minutes instead of 25+ minutes

### Requirements Status

- **17.1**: Configure mkdocs-with-pdf plugin ✅ (configured but not working)
- **17.2**: Generate PDF for User Manual ✗ (blocked by theme incompatibility)
- **17.3**: Generate PDF for Developer Manual ✗ (blocked by theme incompatibility)
- **17.4**: Generate PDF for Documentation Guide ✗ (blocked by theme incompatibility)
- **17.5**: Verify PDF formatting ✗ (cannot test - PDFs don't generate)
- **17.6**: Configure PDF metadata ✅ (configured correctly)

### Recommendation

**Mark this task as blocked** and document the limitation. The HTML documentation is the primary deliverable and works perfectly. PDF generation with Material theme requires either:

1. Switching to a simpler theme for PDF builds (significant work)
2. Using a different PDF plugin (mkdocs-pdf-export-plugin with Chrome)
3. Accepting that PDFs are not available for this documentation

The plugin configuration is correct and will work if/when WeasyPrint adds full CSS Grid support or if the theme is changed.
