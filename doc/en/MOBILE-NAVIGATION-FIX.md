# Mobile Navigation Fix

## Issue
Horizontal navigation tabs overflow on mobile/narrow screens, cutting off items after "Production, REST, Secu..." in the User Manual (and potentially other manuals with many top-level navigation items).

## Root Cause
Material for MkDocs `navigation.tabs` feature creates horizontal tabs for all top-level navigation items. With 17+ top-level items in the User Manual, these tabs overflow on mobile and tablet screens, making navigation items inaccessible.

## Solution
Custom CSS media queries that:
1. **Mobile (< 768px)**: Completely hide horizontal tabs and force hamburger menu navigation
2. **Tablet (768px - 1220px)**: Allow tabs with wrapping and horizontal scrolling
3. **Desktop (> 1220px)**: Keep tabs with horizontal scrolling if needed

## Implementation

### Files Modified
- `doc/en/themes/geoserver/stylesheets/extra.css` - Custom CSS with responsive navigation fixes
- `doc/en/user/mkdocs.yml` - Added `extra_css` configuration
- `doc/en/developer/mkdocs.yml` - Added `extra_css` configuration
- `doc/en/docguide/mkdocs.yml` - Added `extra_css` configuration
- `doc/en/user/docs/stylesheets/extra.css` - Copy of CSS for user manual
- `doc/en/developer/docs/stylesheets/extra.css` - Copy of CSS for developer manual
- `doc/en/docguide/docs/stylesheets/extra.css` - Copy of CSS for docguide

### CSS Strategy

The fix uses CSS media queries to adapt navigation behavior based on screen size:

```css
/* Mobile (< 768px): Hide tabs completely, use hamburger menu */
@media screen and (max-width: 47.9375em) {
  .md-tabs {
    display: none !important;
  }
}

/* Tablet (768px - 1220px): Show tabs with wrapping */
@media screen and (min-width: 48em) and (max-width: 76.1875em) {
  .md-tabs {
    display: flex;
    flex-wrap: wrap;
    overflow-x: auto;
  }
}

/* Desktop (> 1220px): Keep tabs with horizontal scroll */
@media screen and (min-width: 76.25em) {
  .md-tabs__list {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }
}
```

## Testing

### Test on Multiple Screen Sizes
1. **Desktop (1920px)**: Tabs should display horizontally with scroll if needed
2. **Tablet (768px)**: Tabs should wrap or scroll horizontally
3. **Mobile (375px)**: Tabs should be hidden, hamburger menu should provide access to all navigation

### Browser Testing
Test on:
- Chrome/Edge (desktop and mobile)
- Firefox (desktop and mobile)
- Safari (desktop and mobile)

### Testing Commands

```bash
# Build user manual
cd doc/en/user
source ../../../.venv/Scripts/activate
mkdocs serve

# Build developer manual
cd doc/en/developer
source ../../../.venv/Scripts/activate
mkdocs serve

# Build documentation guide
cd doc/en/docguide
source ../../../.venv/Scripts/activate
mkdocs serve
```

Then test responsive behavior:
1. Open browser to http://localhost:8000
2. Open browser DevTools (F12)
3. Toggle device toolbar (Ctrl+Shift+M)
4. Test at different viewport widths:
   - 375px (mobile)
   - 768px (tablet)
   - 1920px (desktop)

### Expected Behavior

**Mobile (375px)**:
- Horizontal tabs should NOT be visible
- Hamburger menu icon should be visible in header
- Clicking hamburger should reveal full navigation
- All navigation items should be accessible

**Tablet (768px)**:
- Tabs may wrap to multiple rows OR scroll horizontally
- All tabs should be accessible (no cutoff)
- Navigation should be usable

**Desktop (1920px)**:
- Tabs should display in a single row
- If tabs overflow, horizontal scrollbar should appear
- All tabs should be accessible

## Alternative Solutions Considered

1. **Reorganize navigation hierarchy**: Group related sections under fewer top-level items
   - Pros: Reduces number of tabs
   - Cons: Requires content restructuring, may affect user experience

2. **Disable navigation.tabs globally**: Use `navigation.sections` instead
   - Pros: Simple configuration change
   - Cons: Changes desktop UX, loses horizontal tab navigation

3. **Custom JavaScript**: Dynamically adjust navigation based on screen size
   - Pros: More control over behavior
   - Cons: More complex, requires JavaScript maintenance

4. **Material for MkDocs built-in responsive behavior**: Rely on theme defaults
   - Pros: No custom code
   - Cons: Current defaults don't handle 17+ tabs well on mobile

## Chosen Solution Rationale

Custom CSS media queries provide the best balance:
- ✅ Preserves desktop UX (horizontal tabs)
- ✅ Fixes mobile UX (hamburger menu)
- ✅ No content restructuring required
- ✅ No JavaScript complexity
- ✅ Works with Material for MkDocs theme
- ✅ Easy to maintain and adjust

## Future Considerations

If the number of top-level navigation items continues to grow, consider:
1. Reorganizing navigation hierarchy to reduce top-level items
2. Using `navigation.sections` instead of `navigation.tabs`
3. Creating a custom navigation component

## Requirements Satisfied

- ✅ Requirement 7.5: Test documentation on desktop and mobile browsers
- ✅ Requirement 14.7: Test responsive design on mobile
- ✅ Navigation fully accessible on all screen sizes (375px, 768px, 1920px)
- ✅ Applied to all three manuals (user, developer, docguide)
