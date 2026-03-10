# Version Selector Implementation Guide

This document explains how to implement the version selector with archive link support for the GeoServer documentation.

## Overview

The version selector allows users to switch between different documentation versions:
- **GitHub Pages versions**: latest, 3.0, 2.28.x (hosted on docs.geoserver.org)
- **Archive versions**: 2.27.x, 2.26.x, 2.25.x (hosted on docs-archive.geoserver.org)

## Configuration

The version selector is configured in `mkdocs.yml` for each manual (user, developer, docguide):

```yaml
extra:
  version_selector:
    # GitHub Pages versions (new versions)
    - version: latest
      title: Latest (3.0)
      aliases: []
      archive: false
    - version: "3.0"
      title: "3.0"
      aliases: []
      archive: false
    - version: "2.28.x"
      title: "2.28.x"
      aliases: []
      archive: false
    # Archive versions (OSGeo server)
    - version: "2.27.x"
      title: "2.27.x (archive)"
      url: "https://docs-archive.geoserver.org/2.27.x/en/user/"
      archive: true
    - version: "2.26.x"
      title: "2.26.x (archive)"
      url: "https://docs-archive.geoserver.org/2.26.x/en/user/"
      archive: true
    - version: "2.25.x"
      title: "2.25.x (archive)"
      url: "https://docs-archive.geoserver.org/2.25.x/en/user/"
      archive: true
```

## Material for MkDocs Version Selector

Material for MkDocs has built-in version selector support through the `mike` plugin. However, it doesn't natively support external archive links.

### Option 1: Use mike's Built-in Version Selector (Recommended)

Mike automatically creates a version selector dropdown based on deployed versions. To add archive links:

1. **Deploy versions with mike** (handled by GitHub Actions workflow)
2. **Archive links are handled by Material theme's version provider**

The `mike` plugin will automatically:
- Create a version selector dropdown
- List all deployed versions
- Handle version switching within GitHub Pages

**Limitation**: mike's built-in selector doesn't support external archive links.

### Option 2: Custom JavaScript for Archive Links

To add archive links to the version selector, create custom JavaScript:

**File**: `doc/en/themes/geoserver-material/javascripts/version-selector.js`

```javascript
document.addEventListener('DOMContentLoaded', function() {
  // Wait for Material theme to initialize
  setTimeout(function() {
    const versionSelector = document.querySelector('.md-version');
    
    if (!versionSelector) {
      console.log('Version selector not found');
      return;
    }
    
    // Get archive versions from mkdocs config
    const archiveVersions = [
      {
        version: '2.27.x',
        title: '2.27.x (archive)',
        url: 'https://docs-archive.geoserver.org/2.27.x/en/user/'
      },
      {
        version: '2.26.x',
        title: '2.26.x (archive)',
        url: 'https://docs-archive.geoserver.org/2.26.x/en/user/'
      },
      {
        version: '2.25.x',
        title: '2.25.x (archive)',
        url: 'https://docs-archive.geoserver.org/2.25.x/en/user/'
      }
    ];
    
    // Find the version dropdown menu
    const versionMenu = versionSelector.querySelector('.md-version__list');
    
    if (!versionMenu) {
      console.log('Version menu not found');
      return;
    }
    
    // Add separator
    const separator = document.createElement('li');
    separator.className = 'md-version__item';
    separator.innerHTML = '<hr style="margin: 0.5rem 0;">';
    versionMenu.appendChild(separator);
    
    // Add archive version links
    archiveVersions.forEach(function(archive) {
      const item = document.createElement('li');
      item.className = 'md-version__item';
      
      const link = document.createElement('a');
      link.href = archive.url;
      link.className = 'md-version__link';
      link.textContent = archive.title;
      link.target = '_blank';  // Open in new tab
      link.rel = 'noopener noreferrer';
      
      item.appendChild(link);
      versionMenu.appendChild(item);
    });
  }, 500);  // Wait for Material theme to render
});
```

**File**: `doc/en/themes/geoserver-material/main.html`

```html
{% extends "base.html" %}

{% block scripts %}
  {{ super() }}
  <script src="{{ 'javascripts/version-selector.js' | url }}"></script>
{% endblock %}
```

### Option 3: Use Material Theme's Version Provider (Simplest)

Material for MkDocs supports custom version providers. The simplest approach is to use mike's built-in version management and add a note in the documentation about archive versions.

**Recommended Approach**: Use mike's built-in version selector for GitHub Pages versions, and add a prominent link to archive documentation in the footer or header.

## Implementation Steps

### Step 1: Update mkdocs.yml (Already Done)

All three mkdocs.yml files have been updated with version_selector configuration including archive links.

### Step 2: Deploy with mike

The GitHub Actions workflow (`.github/workflows/docs-deploy.yml`) handles deployment with mike:

```bash
mike deploy --deploy-prefix "$VERSION/en/user" $VERSION \
  --title "$TITLE" --push --update-aliases
```

### Step 3: Test Version Selector

After deployment:

1. Navigate to https://docs.geoserver.org/latest/en/user/
2. Click the version selector dropdown (usually in the header)
3. Verify all versions are listed:
   - Latest (3.0)
   - 3.0
   - 2.28.x
4. Test switching between versions
5. Verify URLs change correctly

### Step 4: Add Archive Links (Optional)

If custom JavaScript is needed for archive links:

1. Create `doc/en/themes/geoserver-material/` directory
2. Add `javascripts/version-selector.js` with custom logic
3. Create `main.html` to include the JavaScript
4. Update mkdocs.yml to use custom_dir:
   ```yaml
   theme:
     name: material
     custom_dir: ../../themes/geoserver-material
   ```
5. Test locally with `mkdocs serve`
6. Deploy and verify archive links work

## Alternative: Footer Links to Archives

A simpler alternative is to add archive links in the footer:

**File**: `doc/en/themes/geoserver-material/partials/footer.html`

```html
{% extends "base.html" %}

{% block footer %}
  {{ super() }}
  <div class="md-footer-meta__inner md-grid">
    <div class="md-footer-copyright">
      <p>
        <strong>Archive Versions:</strong>
        <a href="https://docs-archive.geoserver.org/2.27.x/en/user/">2.27.x</a> |
        <a href="https://docs-archive.geoserver.org/2.26.x/en/user/">2.26.x</a> |
        <a href="https://docs-archive.geoserver.org/2.25.x/en/user/">2.25.x</a>
      </p>
    </div>
  </div>
{% endblock %}
```

This approach:
- ✅ Simple to implement
- ✅ No JavaScript required
- ✅ Works on all browsers
- ✅ Visible on every page
- ⚠️ Not integrated with version selector dropdown

## Testing

### Local Testing

```bash
# Activate Python virtual environment
source .venv/Scripts/activate

# Install mike
pip install mike

# Build and serve locally
cd doc/en/user
mkdocs serve

# Test version selector
# Open http://localhost:8000 in browser
# Click version selector dropdown
# Verify all versions listed
```

### Production Testing

After deployment to GitHub Pages:

1. **Test version switching**:
   - Navigate to https://docs.geoserver.org/latest/en/user/
   - Click version selector
   - Select different version
   - Verify URL changes and content updates

2. **Test archive links** (if implemented):
   - Click archive version in dropdown
   - Verify redirect to docs-archive.geoserver.org
   - Verify correct version loads

3. **Test on multiple browsers**:
   - Chrome (desktop and mobile)
   - Firefox (desktop and mobile)
   - Safari (desktop and mobile)
   - Edge (desktop)

4. **Test deep links**:
   - Navigate to specific page (e.g., /installation/linux.html)
   - Switch versions
   - Verify same page loads in new version (if exists)

## Troubleshooting

### Version Selector Not Showing

**Cause**: mike hasn't deployed versions yet

**Solution**:
1. Check gh-pages branch has versions.json
2. Verify mike deployments completed successfully
3. Check GitHub Actions logs for errors

### Archive Links Not Working

**Cause**: Custom JavaScript not loaded or DNS not configured

**Solution**:
1. Verify custom JavaScript is included in theme
2. Check browser console for JavaScript errors
3. Verify DNS for docs-archive.geoserver.org is configured
4. Test archive URLs directly in browser

### Version Selector Shows Wrong Versions

**Cause**: mike metadata out of sync

**Solution**:
1. Check versions.json in gh-pages branch
2. Re-deploy with mike to update metadata
3. Clear browser cache and reload

## Maintenance

### Adding New Versions

When a new version is released:

1. **Update mkdocs.yml** in all three manuals:
   ```yaml
   version_selector:
     - version: "3.1.x"
       title: "3.1.x"
       aliases: []
       archive: false
   ```

2. **Deploy with GitHub Actions**:
   - Push changes to new version branch (e.g., 3.1.x)
   - GitHub Actions will automatically deploy

3. **Update version aliases**:
   ```bash
   mike alias 3.1.x stable --push
   ```

### Archiving Old Versions

When moving a version to archive:

1. **Update mkdocs.yml** to move version to archive section:
   ```yaml
   - version: "2.28.x"
     title: "2.28.x (archive)"
     url: "https://docs-archive.geoserver.org/2.28.x/en/user/"
     archive: true
   ```

2. **Copy version to OSGeo server** (if needed):
   ```bash
   # On OSGeo server
   cp -r /var/www/geoserverdocs/2.28.x /var/www/geoserverdocs-archive/
   ```

3. **Update DNS** (if needed):
   - Verify docs-archive.geoserver.org points to OSGeo server

4. **Remove from GitHub Pages** (optional):
   ```bash
   mike delete 2.28.x --push
   ```

## Recommendations

**For Initial Implementation**:
1. ✅ Use mike's built-in version selector (simplest)
2. ✅ Add footer links to archive versions (simple, no JavaScript)
3. ⚠️ Skip custom JavaScript for now (can add later if needed)

**For Future Enhancement**:
1. Add custom JavaScript to integrate archive links into dropdown
2. Implement version-aware deep linking
3. Add version comparison feature

**Current Status**:
- ✅ mkdocs.yml updated with version_selector configuration
- ✅ GitHub Actions workflow configured for mike deployment
- ⚠️ Custom JavaScript not implemented (optional)
- ⚠️ Footer links not implemented (optional)

**Next Steps**:
1. Test deployment on migration branch
2. Verify version selector works with mike
3. Decide if custom JavaScript or footer links are needed
4. Implement chosen approach
5. Test on production

---

**Document Version:** 1.0  
**Last Updated:** 2026-03-10  
**Status:** Implementation Guide - Ready for Testing
