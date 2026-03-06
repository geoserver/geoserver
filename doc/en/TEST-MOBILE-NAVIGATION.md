# Mobile Navigation Testing Guide

## Quick Test Instructions

### Prerequisites
```bash
# Ensure you're in the workspace root
cd /path/to/geoserver

# Activate Python virtual environment
source .venv/Scripts/activate
```

### Test User Manual
```bash
cd doc/en/user
mkdocs serve
```
Open browser to http://localhost:8000

### Test Developer Manual
```bash
cd doc/en/developer
mkdocs serve
```
Open browser to http://localhost:8000

### Test Documentation Guide
```bash
cd doc/en/docguide
mkdocs serve
```
Open browser to http://localhost:8000

## Testing Checklist

### Desktop Testing (1920px width)
- [ ] Open browser DevTools (F12)
- [ ] Set viewport to 1920px width
- [ ] Verify horizontal navigation tabs are visible
- [ ] Verify all tabs are accessible (may need to scroll horizontally)
- [ ] Verify tabs have a subtle scrollbar if they overflow
- [ ] Verify clicking tabs navigates correctly

### Tablet Testing (768px width)
- [ ] Set viewport to 768px width
- [ ] Verify tabs either wrap to multiple rows OR scroll horizontally
- [ ] Verify all navigation items are accessible
- [ ] Verify no items are cut off
- [ ] Verify touch scrolling works (if testing on actual tablet)

### Mobile Testing (375px width)
- [ ] Set viewport to 375px width
- [ ] Verify horizontal tabs are HIDDEN
- [ ] Verify hamburger menu icon is visible in header
- [ ] Click hamburger menu icon
- [ ] Verify navigation drawer opens
- [ ] Verify all navigation items are visible in drawer
- [ ] Verify navigation items have good touch targets (not too small)
- [ ] Verify nested navigation expands/collapses correctly
- [ ] Verify clicking navigation items works correctly

## Browser Testing Matrix

Test on the following browsers:

### Desktop Browsers
- [ ] Chrome (Windows/Mac/Linux)
- [ ] Firefox (Windows/Mac/Linux)
- [ ] Safari (Mac)
- [ ] Edge (Windows)

### Mobile Browsers
- [ ] Chrome Mobile (Android)
- [ ] Safari Mobile (iOS)
- [ ] Firefox Mobile (Android)

## Expected Results

### ✅ Success Criteria
1. **Mobile (< 768px)**: Tabs hidden, hamburger menu provides full navigation access
2. **Tablet (768px - 1220px)**: Tabs visible with wrapping or horizontal scroll
3. **Desktop (> 1220px)**: Tabs visible in single row with horizontal scroll if needed
4. **All sizes**: All navigation items are accessible
5. **All sizes**: Navigation is usable and intuitive

### ❌ Failure Indicators
1. Navigation items cut off or inaccessible
2. Horizontal overflow without scrolling
3. Hamburger menu not working on mobile
4. Tabs visible on mobile (should be hidden)
5. Poor touch targets on mobile (too small to tap)

## Troubleshooting

### Issue: CSS not loading
**Symptom**: Navigation looks the same on all screen sizes
**Solution**: 
1. Check that `extra_css` is configured in mkdocs.yml
2. Verify `docs/stylesheets/extra.css` exists
3. Clear browser cache and reload
4. Check browser console for CSS loading errors

### Issue: Tabs still overflow on mobile
**Symptom**: Horizontal tabs visible on mobile, items cut off
**Solution**:
1. Verify viewport meta tag is present in HTML
2. Check that CSS media queries are being applied (use DevTools)
3. Verify no conflicting CSS from other sources

### Issue: Hamburger menu not appearing
**Symptom**: No way to access navigation on mobile
**Solution**:
1. Verify Material for MkDocs theme is configured correctly
2. Check that navigation features are enabled in mkdocs.yml
3. Verify no JavaScript errors in browser console

## Manual Testing with Browser DevTools

### Chrome/Edge DevTools
1. Press F12 to open DevTools
2. Press Ctrl+Shift+M to toggle device toolbar
3. Select device preset (e.g., "iPhone SE", "iPad", "Responsive")
4. Or manually set width using the width input field
5. Test navigation at different widths

### Firefox DevTools
1. Press F12 to open DevTools
2. Click the "Responsive Design Mode" icon (or Ctrl+Shift+M)
3. Select device preset or set custom dimensions
4. Test navigation at different widths

### Safari DevTools
1. Enable Developer menu: Safari > Preferences > Advanced > Show Develop menu
2. Develop > Enter Responsive Design Mode
3. Select device preset or set custom dimensions
4. Test navigation at different widths

## Automated Testing (Optional)

For CI/CD integration, consider:
1. Visual regression testing with Percy or Chromatic
2. Responsive screenshot testing with BackstopJS
3. Accessibility testing with axe-core or Lighthouse

## Reporting Issues

If you find navigation issues, report with:
1. Screen size where issue occurs
2. Browser and version
3. Screenshot or screen recording
4. Steps to reproduce
5. Expected vs actual behavior
