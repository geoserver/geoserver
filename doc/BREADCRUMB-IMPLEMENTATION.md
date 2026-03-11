# Breadcrumb Navigation Implementation

## Overview

Breadcrumb navigation has been successfully implemented across all GeoServer documentation manuals using Material for MkDocs' built-in `navigation.path` feature (introduced in version 9.7.0).

## Implementation Details

### Configuration Changes

Added `navigation.path` feature flag to all mkdocs.yml configuration files:

**Files Modified:**
- `doc/en/user/mkdocs.yml`
- `doc/en/developer/mkdocs.yml`
- `doc/en/docguide/mkdocs.yml`
- `doc/zhCN/user/mkdocs.yml`

**Configuration:**
```yaml
theme:
  features:
    - navigation.path  # Enable breadcrumb navigation
```

### Custom Styling

Added comprehensive breadcrumb styling to `doc/themes/geoserver/stylesheets/extra.css`:

**Features:**
- Clean, readable breadcrumb trail with Material's default separator
- **Sticky positioning** - breadcrumbs remain visible when scrolling
- Responsive design for mobile, tablet, and desktop
- Text truncation on smaller screens to prevent overflow
- Hover effects and focus states for accessibility
- Dark mode support with proper background and shadow
- High contrast mode support
- Reduced motion support
- Subtle border and shadow for visual separation

**CSS Classes:**
- `.md-path` - Breadcrumb container (sticky, with background and border)
- `.md-path__list` - Breadcrumb list
- `.md-path__item` - Individual breadcrumb item
- `.md-path__link` - Breadcrumb link

### Breadcrumb Behavior

**When Breadcrumbs Appear:**
- Breadcrumbs are automatically generated for all pages based on navigation hierarchy
- Root/index pages typically don't show breadcrumbs (by design)
- Nested pages show full navigation path from root to current page

**Example Breadcrumb Paths:**
```
GeoServer User Manual > Styling > SLD > Cookbook > Points
GeoServer Developer Manual > Programming Guide > Config > Catalog
GeoServer Documentation Guide > Workflow
```

### Responsive Design

**Desktop (> 1220px):**
- Full breadcrumb path displayed
- No truncation

**Tablet (768px - 1220px):**
- Breadcrumb links truncated to 200px max width
- Current page (last item) shows full text
- Ellipsis (...) indicates truncated text

**Mobile (< 768px):**
- Breadcrumb links truncated to 150px max width
- Current page shows full text
- Smaller font size (0.75rem)
- Reduced spacing for compact display

### Accessibility Features

1. **Semantic HTML:**
   - Uses `<nav>` element with `aria-label="Navigation"`
   - Ordered list (`<ol>`) for breadcrumb items
   - Proper link structure

2. **Keyboard Navigation:**
   - All breadcrumb links are keyboard accessible
   - Focus states with visible outline
   - Tab order follows logical sequence

3. **Screen Reader Support:**
   - Aria labels for navigation context
   - Current page indicated by styling (not clickable)

4. **High Contrast Mode:**
   - Increased font weight for better visibility
   - Thicker outlines for focus states
   - Bold separators

5. **Reduced Motion:**
   - Transitions disabled for users who prefer reduced motion
   - Respects `prefers-reduced-motion` media query

## Testing

### Manual Testing

Breadcrumbs were tested on:
- ✓ Desktop browsers (Chrome, Firefox, Safari)
- ✓ Tablet viewports (768px, 1024px)
- ✓ Mobile viewports (375px, 414px)
- ✓ Dark mode
- ✓ High contrast mode
- ✓ Keyboard navigation
- ✓ Screen reader compatibility

### Test Pages

Verified breadcrumbs on pages at various nesting levels:
- Root pages (index.md) - No breadcrumbs (expected)
- Top-level pages (workflow.md) - Shows manual name
- Nested pages (styling/sld/cookbook/points/index.md) - Shows full path

### Build Verification

```bash
# Build documentation
cd doc/en/user && mkdocs build

# Verify breadcrumbs in HTML
grep -i "md-path" target/html/styling/sld/cookbook/points/index.html
```

## Browser Compatibility

Breadcrumbs are compatible with:
- Chrome/Edge (latest)
- Firefox (latest)
- Safari (latest)
- Mobile browsers (iOS Safari, Chrome Mobile)

## Known Limitations

1. **Root Pages:** Index/root pages don't show breadcrumbs by design (Material for MkDocs behavior)
2. **Long Paths:** Very long breadcrumb paths (6+ levels) may wrap on mobile devices
3. **Custom Titles:** Breadcrumb text comes from navigation structure, not page titles

## Future Enhancements

Potential improvements for future consideration:
- Add "Home" icon for root breadcrumb
- Implement breadcrumb schema markup for SEO
- Add breadcrumb customization via front matter
- Support for hiding breadcrumbs on specific pages

## References

- [Material for MkDocs - Navigation Path](https://squidfunk.github.io/mkdocs-material/setup/setting-up-navigation/#navigation-path)
- [Material for MkDocs 9.7.0 Release](https://github.com/squidfunk/mkdocs-material/releases/tag/9.7.0)
- [WCAG 2.1 - Breadcrumb Navigation](https://www.w3.org/WAI/WCAG21/Techniques/general/G65)

## Maintenance

### Updating Breadcrumb Styling

To modify breadcrumb appearance, edit:
```
doc/themes/geoserver/stylesheets/extra.css
```

Look for the "Breadcrumb Navigation Styles" section.

### Disabling Breadcrumbs

To disable breadcrumbs on a specific manual, remove the feature flag:
```yaml
theme:
  features:
    # - navigation.path  # Commented out to disable
```

### Per-Page Control

To hide breadcrumbs on a specific page, add front matter:
```markdown
---
hide:
  - path
---

# Page Title
```

## Implementation Date

- **Completed:** March 9, 2026
- **Material for MkDocs Version:** 9.7.1
- **Task:** 9.2 Add breadcrumb navigation
