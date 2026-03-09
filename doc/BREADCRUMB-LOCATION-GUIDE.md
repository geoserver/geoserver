# Where to Find Breadcrumb Navigation

## Quick Access

**Local Server is Running:**
- URL: http://127.0.0.1:8001/3.0/en/user/

## Where Breadcrumbs Appear

### ✓ Breadcrumbs WILL appear on nested pages:

1. **Navigate to a nested page** (breadcrumbs show the path):
   - http://127.0.0.1:8001/3.0/en/user/styling/sld/cookbook/points/
   - http://127.0.0.1:8001/3.0/en/user/services/wms/reference/
   - http://127.0.0.1:8001/3.0/en/user/data/database/postgis/
   - http://127.0.0.1:8001/3.0/en/user/extensions/monitoring/installation/

### ✗ Breadcrumbs will NOT appear on:

1. **Root/Index pages** (by Material for MkDocs design):
   - http://127.0.0.1:8001/3.0/en/user/ (home page)
   - Top-level section index pages

## Visual Location

```
┌─────────────────────────────────────────────────────────┐
│ [Logo] GeoServer User Manual ▼         [Search] [Edit] │ ← Header
├─────────────────────────────────────────────────────────┤
│                                                         │
│ GeoServer User Manual › Styling › SLD › Cookbook › Points  ← BREADCRUMBS HERE
│                                                         │
│ # Points                                                │ ← Page Title
│                                                         │
│ Page content starts here...                            │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

## Exact Position

Breadcrumbs appear:
- **Below the header** (logo, title, search bar)
- **Above the page title** (H1 heading)
- **In the main content area** (not in sidebar)

## Testing Steps

1. **Open the local server:**
   ```
   http://127.0.0.1:8001/3.0/en/user/
   ```

2. **Click on a nested section** in the left sidebar:
   - Click "Styling" → "SLD" → "Cookbook" → "Points"

3. **Look for breadcrumbs** at the top of the page content:
   - You should see: `GeoServer User Manual › Styling › SLD › Cookbook › Points`
   - Each item (except the last) is clickable

4. **Try other nested pages:**
   - Services → WMS → Reference
   - Data → Database → PostGIS
   - Extensions → Monitoring → Installation

## Styling Details

The breadcrumbs use:
- **Separator:** `›` (right-pointing angle quotation mark)
- **Font size:** 0.85rem (smaller than body text)
- **Color:** Light gray for links, darker for current page
- **Hover effect:** Links turn blue and underline on hover

## Troubleshooting

### "I still don't see breadcrumbs"

1. **Check you're on a nested page:**
   - Breadcrumbs only show on pages with parent sections
   - Root pages and top-level pages may not show breadcrumbs

2. **Clear browser cache:**
   ```bash
   Ctrl+Shift+R (Windows/Linux)
   Cmd+Shift+R (Mac)
   ```

3. **Verify the feature is enabled:**
   - Check `doc/en/user/mkdocs.yml` contains:
     ```yaml
     features:
       - navigation.path
     ```

4. **Rebuild the documentation:**
   ```bash
   cd doc/en/user
   source ../../../.venv/Scripts/activate
   mkdocs build --clean
   ```

### "Breadcrumbs look wrong"

1. **Check custom CSS is loaded:**
   - Open browser DevTools (F12)
   - Look for `.md-path` styles in the Styles panel
   - Verify `extra.css` is loaded

2. **Verify Material for MkDocs version:**
   ```bash
   pip show mkdocs-material
   # Should be 9.7.1 or higher
   ```

## Example Pages with Breadcrumbs

Here are guaranteed pages that will show breadcrumbs:

| Page | Breadcrumb Path |
|------|----------------|
| `/styling/sld/cookbook/points/` | GeoServer User Manual › Styling › SLD › Cookbook › Points |
| `/services/wms/reference/` | GeoServer User Manual › Services › WMS › Reference |
| `/data/database/postgis/` | GeoServer User Manual › Data › Database › PostGIS |
| `/extensions/monitoring/installation/` | GeoServer User Manual › Extensions › Monitoring › Installation |
| `/security/webadmin/auth/` | GeoServer User Manual › Security › Webadmin › Auth |

## Screenshot Locations

If you need to verify visually, breadcrumbs appear:
- **Desktop:** Full width below header, above page title
- **Tablet:** Same position, with text truncation if needed
- **Mobile:** Same position, smaller font, more aggressive truncation

## HTML Structure

If you want to inspect the HTML:

```html
<nav class="md-path" aria-label="Navigation">
  <ol class="md-path__list">
    <li class="md-path__item">
      <a href="..." class="md-path__link">GeoServer User Manual</a>
    </li>
    <li class="md-path__item">
      <a href="..." class="md-path__link">Styling</a>
    </li>
    <li class="md-path__item">
      <a href="..." class="md-path__link">SLD</a>
    </li>
    <li class="md-path__item">
      <a href="..." class="md-path__link">Cookbook</a>
    </li>
    <li class="md-path__item">
      <a href="..." class="md-path__link">Points</a>
    </li>
  </ol>
</nav>
```

## Need Help?

If you still can't find the breadcrumbs:
1. Share the URL you're viewing
2. Share a screenshot of the page
3. Check browser console for errors (F12 → Console tab)
