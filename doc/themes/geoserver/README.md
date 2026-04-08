# GeoServer Documentation Theme

This directory contains the shared theme customizations for all GeoServer documentation types (User Manual, Developer Manual, and Documentation Guide).

## Theme Structure

```
doc/themes/geoserver/
├── img/
│   ├── geoserver-logo.png    # Site logo (used in header)
│   └── geoserver.ico          # Favicon
├── partials/
│   ├── header.html            # Custom header template
│   └── header-switcher.html   # Documentation switcher component
├── stylesheets/
│   └── extra.css              # Custom CSS for all documentation
├── overrides/
│   └── partials/              # (empty - reserved for future overrides)
├── static/                    # Legacy Sphinx theme files (not used by MkDocs)
├── layout.html                # Legacy Sphinx layout (not used by MkDocs)
├── theme.conf                 # Legacy Sphinx config (not used by MkDocs)
└── README.md                  # This file
```

## How It Works

### MkDocs Configuration

All three documentation types reference this shared theme in their `mkdocs.yml`:

```yaml
theme:
  name: material
  custom_dir: ../../themes/geoserver
  logo: img/geoserver-logo.png
  favicon: img/geoserver.ico
```

### File Resolution Priority

MkDocs uses a layered approach to find files:

1. **Highest Priority:** Documentation-specific files in `doc/en/{type}/docs/`
2. **Medium Priority:** Shared theme files in `doc/themes/geoserver/` (this directory)
3. **Lowest Priority:** Base Material theme files

**Example:** When rendering a page, MkDocs looks for `extra.css` in this order:
1. `doc/en/user/docs/stylesheets/extra.css` (doc-specific override)
2. `doc/themes/geoserver/stylesheets/extra.css` (shared theme - **used**)
3. Material theme's default CSS (base theme)

## Making Changes

### Modifying Shared Theme Assets

To change assets that affect **all documentation types**:

1. Edit files in this directory (`doc/themes/geoserver/`)
2. Test all three documentation types:
   ```bash
   # User Manual
   cd doc/en/user
   source ../../../.venv/Scripts/activate
   mkdocs build
   
   # Developer Manual
   cd ../developer
   mkdocs build
   
   # Documentation Guide
   cd ../docguide
   mkdocs build
   ```
3. Verify changes appear in all three builds
4. Commit changes

### Adding Doc-Specific Overrides

To customize assets for **one documentation type only**:

1. Place the file in `doc/en/{type}/docs/` directory
2. Use the same relative path structure as the theme
3. The doc-specific file will automatically override the shared theme file

**Example:** To give docguide a custom logo:
```bash
# Create doc-specific logo
cp custom-logo.png doc/en/docguide/docs/img/geoserver-logo.png

# Result:
# - Docguide uses: doc/en/docguide/docs/img/geoserver-logo.png
# - User/Developer use: doc/themes/geoserver/img/geoserver-logo.png
```

**No configuration changes needed** - MkDocs handles this automatically.

## Theme Components

### 1. Custom CSS (`stylesheets/extra.css`)

**Purpose:** Provides custom styling for:
- Documentation switcher dropdown
- Mobile navigation improvements
- Custom color schemes
- Responsive design adjustments

**Key Features:**
- Documentation switcher with 0.8s hover delay
- Active state indicators
- Mobile-friendly navigation
- Dark mode support

### 2. Header Templates (`partials/`)

**header.html:** Custom header template that includes the documentation switcher

**header-switcher.html:** Dropdown component that allows switching between:
- User Manual
- Developer Manual
- Documentation Guide
- Swagger APIs

**Configuration:** Uses `extra.doc_type` from `mkdocs.yml` to highlight the active documentation type.

### 3. Images (`img/`)

**geoserver-logo.png:** Site logo displayed in the header (12,580 bytes)

**geoserver.ico:** Favicon displayed in browser tabs (15,086 bytes)

## Documentation Switcher

The documentation switcher is configured in each `mkdocs.yml`:

```yaml
extra:
  doc_type: "user"  # or "developer" or "docguide"
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

The switcher automatically highlights the current documentation type based on `doc_type`.

## Legacy Files

The following files are from the old Sphinx-based documentation system and are **not used by MkDocs**:

- `static/` - Sphinx static assets (Blueprint CSS, chrome images)
- `layout.html` - Sphinx layout template
- `theme.conf` - Sphinx theme configuration

These files are kept for historical reference but can be removed if no longer needed.

## Testing

### Local Development

Test changes with live reload:

```bash
cd doc/en/user
source ../../../.venv/Scripts/activate
mkdocs serve
# Visit http://127.0.0.1:8000
```

### Build All Documentation

```bash
# Activate Python virtual environment
source .venv/Scripts/activate

# Build all three types
cd doc/en/user && mkdocs build && cd -
cd doc/en/developer && mkdocs build && cd -
cd doc/en/docguide && mkdocs build && cd -
```

### Visual Verification Checklist

After making changes, verify:

- [ ] Documentation switcher appears in header
- [ ] Logo and favicon display correctly
- [ ] Custom CSS is applied (check dropdown styling)
- [ ] Mobile navigation works
- [ ] Dark mode toggle works
- [ ] All three documentation types build successfully
- [ ] No console errors in browser

## Troubleshooting

### Theme files not loading

**Problem:** Changes to theme files don't appear in build

**Solution:**
1. Clear the build directory: `rm -rf target/html`
2. Rebuild: `mkdocs build`
3. Check `mkdocs.yml` has correct `custom_dir: ../../themes/geoserver`

### Documentation switcher not working

**Problem:** Dropdown doesn't appear or links are broken

**Solution:**
1. Verify `extra.doc_type` is set in `mkdocs.yml`
2. Check `extra.doc_switcher` configuration
3. Verify `partials/header-switcher.html` exists
4. Check browser console for JavaScript errors

### Doc-specific override not working

**Problem:** Doc-specific file is ignored

**Solution:**
1. Verify file is in `doc/en/{type}/docs/` directory
2. Check file path matches theme structure exactly
3. Clear build directory and rebuild
4. Verify `docs_dir: docs` in `mkdocs.yml`

## Best Practices

### DO:
- ✅ Test all three documentation types after theme changes
- ✅ Use descriptive commit messages for theme changes
- ✅ Keep theme files minimal and focused
- ✅ Document any new theme components
- ✅ Use doc-specific overrides sparingly

### DON'T:
- ❌ Duplicate theme files in documentation directories
- ❌ Modify theme files without testing all docs
- ❌ Use doc-specific overrides for shared assets
- ❌ Commit build artifacts (`target/` directories)
- ❌ Break the documentation switcher functionality

## Related Documentation

- [MkDocs Material Theme Documentation](https://squidfunk.github.io/mkdocs-material/)
- [MkDocs Custom Themes](https://www.mkdocs.org/user-guide/custom-themes/)
- [GeoServer Documentation Guide](../en/docguide/)

## Maintenance

**Last Updated:** March 5, 2025  
**MkDocs Version:** 1.6.1  
**Material Theme Version:** 9.7.4

For questions or issues, refer to the GeoServer documentation team.
