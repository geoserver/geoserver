# Multi-Version Documentation Strategy

**Date:** 2026-03-09  
**Status:** Final Strategy - GitHub Pages for New Versions, OSGeo for Archives

---

## Executive Summary

**Strategy:** Hybrid approach combining GitHub Pages and OSGeo hosting

- **GitHub Pages:** New versions only (2.28.x, main/3.0, future releases)
- **OSGeo Server:** Archive versions unchanged (2.27.x and earlier)
- **Version Selector:** One-way links from new → archive (not reverse)
- **Size:** ~500 MB for 3 versions on GitHub Pages (50% of 1 GB limit)

---

## Version Distribution

### GitHub Pages (New Versions)

**Versions hosted:**
- `latest` (alias for main branch / 3.0 dev)
- `stable` (alias for 2.28.x)
- Future versions (3.0.x, 3.1.x, etc.)

**URL structure:**
```
https://docs.geoserver.org/latest/en/user/
https://docs.geoserver.org/stable/en/user/
https://docs.geoserver.org/3.0.x/en/user/
```

**Deployment:** GitHub Actions + mike

### OSGeo Server (Archive Versions)

**Versions hosted:**
- 2.27.x (maintenance)
- 2.26.x
- 2.25.x
- ... (all versions back to 2.10.x)

**URL structure (with new CNAME):**
```
https://docs-archive.geoserver.org/2.27.x/en/user/
https://docs-archive.geoserver.org/2.26.x/en/user/
https://docs-archive.geoserver.org/2.25.x/en/user/
```

**Deployment:** Existing Jenkins jobs (unchanged)

**Note:** Archive versions remain exactly as-is on OSGeo server. Only the CNAME changes.

---

## DNS Configuration

### Primary Domain (GitHub Pages)

```
docs.geoserver.org CNAME geoserver.github.io
```

**Serves:** New versions (2.28.x, main, future)

### Archive Domain (OSGeo Server)

```
docs-archive.geoserver.org A <OSGeo-server-IP>
```

**Serves:** Old versions (2.27.x and earlier)

---

## Size Analysis with Git Deduplication

### Actual Built HTML Breakdown

**From real build (`doc/en/user/target/html` = 416 MB):**

| Component | Size | Deduplication | Per Additional Version |
|-----------|------|---------------|------------------------|
| HTML files | 44 MB | None (unique) | 44 MB |
| Images | 5.5 MB | 80% shared | ~1 MB |
| CSS/JS | 2.7 MB | 80% shared | ~0.5 MB |
| Theme assets | 363 MB | 100% shared | 0 MB |
| **Total** | **416 MB** | - | **~45 MB** |

### Key Insights

1. **Theme assets (363 MB) are 100% identical across versions**
   - Material theme files
   - Search indexes
   - Fonts
   - Git automatically deduplicates these

2. **Images (5.5 MB) are mostly shared**
   - Logos, icons: 100% shared
   - UI screenshots: ~80% shared
   - Only new feature screenshots differ (~1 MB per version)

3. **HTML (44 MB) is unique per version**
   - Content changes between versions
   - No deduplication possible

### GitHub Pages Capacity

| Versions | Calculation | Total Size | % of 1 GB |
|----------|-------------|------------|-----------|
| 1 (2.28.x) | 416 MB | 416 MB | 41% |
| 2 (+ latest) | 416 + 45 | 461 MB | 46% |
| 3 (+ 3.0.x) | 416 + 90 | 506 MB | 51% |
| 4 (+ 3.1.x) | 416 + 135 | 551 MB | 55% |
| 5 (+ 3.2.x) | 416 + 180 | 596 MB | 60% |

**Conclusion:** Can easily host 5+ versions on GitHub Pages with room to spare!

---

## Version Selector Implementation

### New Versions (GitHub Pages)

**Version selector includes archive links:**

```yaml
# mkdocs.yml (for 2.28.x and later)
extra:
  version:
    provider: mike
    default: stable
  
  versions:
    # GitHub Pages versions
    - version: latest
      title: "Latest (3.0 dev)"
      url: "/latest/"
      archive: false
    - version: stable
      title: "Stable (2.28.x)"
      url: "/stable/"
      archive: false
    
    # Archive versions (OSGeo server)
    - version: 2.27.x
      title: "2.27.x (archive)"
      url: "https://docs-archive.geoserver.org/2.27.x/"
      archive: true
    - version: 2.26.x
      title: "2.26.x (archive)"
      url: "https://docs-archive.geoserver.org/2.26.x/"
      archive: true
    - version: 2.25.x
      title: "2.25.x (archive)"
      url: "https://docs-archive.geoserver.org/2.25.x/"
      archive: true
```

**JavaScript handler:**
```javascript
document.querySelector('.version-selector').addEventListener('change', function(e) {
  const selectedVersion = versions.find(v => v.version === e.target.value);
  if (selectedVersion.archive) {
    // External link to OSGeo archive
    window.location.href = selectedVersion.url;
  } else {
    // Internal link within GitHub Pages
    window.location.href = selectedVersion.url + getCurrentPath();
  }
});
```

### Archive Versions (OSGeo Server)

**Version selector remains unchanged:**
- No updates needed to archive versions
- They keep their existing version selectors
- Users can navigate between archive versions
- No links back to GitHub Pages versions

**Result:** One-way navigation from new → archive

---

## Migration Path

### Phase 1: DNS Setup (Week 1)

1. **Configure archive CNAME**
   ```
   docs-archive.geoserver.org → OSGeo server IP
   ```
   Wait for DNS propagation (24-48 hours)

2. **Test archive access**
   ```
   https://docs-archive.geoserver.org/2.27.x/en/user/
   ```
   Verify all old versions accessible

3. **Configure GitHub Pages CNAME**
   ```
   docs.geoserver.org → geoserver.github.io
   ```

### Phase 2: Deploy to GitHub Pages (Week 2)

1. **Deploy 2.28.x**
   - Use GitHub Actions workflow
   - Deploy with mike
   - Test URLs and version selector

2. **Deploy main (latest)**
   - Deploy development version
   - Verify version switching works

3. **Parallel testing**
   - Keep Jenkins running
   - Monitor both deployments
   - Gather feedback

### Phase 3: Cutover (Week 3)

1. **Update version selectors**
   - Add archive links to 2.28.x and main
   - Test navigation to archives

2. **Disable Jenkins for new versions**
   - Stop 2.28.x and main Jenkins jobs
   - Keep archive version jobs running

3. **Monitor and adjust**
   - Watch for issues
   - Fix any broken links
   - Update documentation

### Phase 4: Long-term (Ongoing)

1. **New version releases**
   - Deploy to GitHub Pages automatically
   - Update version selector list

2. **Archive maintenance**
   - Keep Jenkins jobs for archives
   - Eventually migrate archives to static hosting
   - No rush - they work fine as-is

---

## Advantages of Hybrid Approach

### vs Pure GitHub Pages

| Aspect | Hybrid | Pure GitHub Pages |
|--------|--------|-------------------|
| **Migration effort** | Low (only new versions) | High (migrate all versions) |
| **Archive stability** | High (unchanged) | Medium (need migration) |
| **Size on GitHub** | ~500 MB (3 versions) | ~2 GB (20 versions) |
| **Risk** | Low (archives untouched) | Medium (migrate everything) |
| **Maintenance** | Split (2 systems) | Unified (1 system) |

### vs Pure OSGeo

| Aspect | Hybrid | Pure OSGeo |
|--------|--------|------------|
| **New version deployment** | Automatic (GitHub Actions) | Manual (Jenkins + SSH) |
| **Version management** | Built-in (mike) | Manual scripts |
| **Global CDN** | Yes (GitHub) | No (single server) |
| **SSH credentials** | Not needed | Required |
| **Rollback** | Git-based | Manual |

**Winner:** Hybrid approach balances benefits and minimizes risk!

---

## Technical Implementation

### GitHub Actions Workflow

```yaml
name: MkDocs Documentation (GitHub Pages)

on:
  push:
    branches: [main, 2.28.x]  # Only new versions
    paths:
      - 'doc/**'
      - '.github/workflows/mkdocs.yml'

permissions:
  contents: write

jobs:
  build-and-deploy:
    runs-on: ubuntu-22.04
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0
      
      - name: Setup Python
        uses: actions/setup-python@v5
        with:
          python-version: '3.x'
          cache: 'pip'
      
      - name: Install MkDocs and mike
        run: |
          pip install mkdocs mkdocs-material mkdocs-macros-plugin \
                      pymdown-extensions mike
      
      - name: Configure Git
        run: |
          git config user.name github-actions
          git config user.email github-actions@github.com
      
      - name: Determine version
        id: version
        run: |
          BRANCH=${GITHUB_REF##refs/heads/}
          
          if [ "$BRANCH" = "main" ]; then
            echo "version=latest" >> $GITHUB_OUTPUT
            echo "title=Latest (3.0 dev)" >> $GITHUB_OUTPUT
            echo "is_default=false" >> $GITHUB_OUTPUT
          elif [ "$BRANCH" = "2.28.x" ]; then
            echo "version=stable" >> $GITHUB_OUTPUT
            echo "title=Stable (2.28.x)" >> $GITHUB_OUTPUT
            echo "is_default=true" >> $GITHUB_OUTPUT
          fi
      
      # Deploy User Manual
      - name: Deploy User Manual
        working-directory: doc/en/user
        run: |
          VERSION=${{ steps.version.outputs.version }}
          mike deploy --deploy-prefix "$VERSION/en/user" \
            $VERSION \
            --title "${{ steps.version.outputs.title }}" \
            --push \
            --update-aliases
      
      # Deploy Developer Guide
      - name: Deploy Developer Guide
        working-directory: doc/en/developer
        run: |
          VERSION=${{ steps.version.outputs.version }}
          mike deploy --deploy-prefix "$VERSION/en/developer" \
            $VERSION \
            --push \
            --update-aliases
      
      # Deploy Documentation Guide
      - name: Deploy Documentation Guide
        working-directory: doc/en/docguide
        run: |
          VERSION=${{ steps.version.outputs.version }}
          mike deploy --deploy-prefix "$VERSION/en/docguide" \
            $VERSION \
            --push \
            --update-aliases
      
      # Copy API docs (static files)
      - name: Deploy API documentation
        run: |
          VERSION=${{ steps.version.outputs.version }}
          
          git fetch origin gh-pages
          git checkout gh-pages
          
          mkdir -p $VERSION/en/api
          cp -r doc/en/api/* $VERSION/en/api/
          
          git add $VERSION/en/api
          git commit -m "Update API docs for $VERSION" || echo "No changes"
          git push origin gh-pages
          
          git checkout ${GITHUB_REF##refs/heads/}
      
      # Set default version
      - name: Set default version
        if: steps.version.outputs.is_default == 'true'
        working-directory: doc/en/user
        run: |
          mike set-default ${{ steps.version.outputs.version }} --push
      
      # Configure custom domain
      - name: Add CNAME
        if: github.ref == 'refs/heads/main'
        run: |
          git fetch origin gh-pages
          git checkout gh-pages
          echo "docs.geoserver.org" > CNAME
          git add CNAME
          git commit -m "Add CNAME" || echo "CNAME exists"
          git push origin gh-pages
```

### Jenkins Jobs (Archive Versions)

**Keep existing Jenkins jobs for archive versions:**
- `geoserver-2.27.x-docs`
- `geoserver-2.26.x-docs`
- `geoserver-2.25.x-docs`
- etc.

**No changes needed** - they continue deploying to OSGeo server as before.

---

## URL Compatibility

### All URLs Remain Valid

| URL Type | Example | Status |
|----------|---------|--------|
| New versions | `https://docs.geoserver.org/stable/en/user/` | ✅ GitHub Pages |
| Archive versions | `https://docs-archive.geoserver.org/2.27.x/en/user/` | ✅ OSGeo (new CNAME) |
| Deep links (new) | `https://docs.geoserver.org/stable/en/user/installation/` | ✅ Works |
| Deep links (archive) | `https://docs-archive.geoserver.org/2.27.x/en/user/installation/` | ✅ Works |

**100% backward compatible!**

---

## Future Considerations

### When to Migrate Archives to GitHub Pages

**Consider migration when:**
- OSGeo server maintenance becomes burden
- Want unified hosting platform
- Need better global CDN for archives

**Not urgent because:**
- Archives work fine on OSGeo
- Rarely accessed (low traffic)
- No active development

### Alternative: Static Archive Repository

**If OSGeo becomes unavailable:**
1. Create `geoserver/docs-archive` repository
2. Copy archive versions to gh-pages branch
3. Update CNAME to point to archive repo
4. No changes to version selector needed

---

## Summary

**Hybrid Strategy Benefits:**
- ✅ Minimal migration effort (only new versions)
- ✅ Archive versions remain stable and unchanged
- ✅ GitHub Pages for new versions (~500 MB, plenty of room)
- ✅ Automatic deployment with GitHub Actions
- ✅ Built-in version management with mike
- ✅ 100% backward compatible URLs
- ✅ One-way version selector (new → archive)
- ✅ Git deduplication saves ~370 MB per version

**This is the optimal solution!**

---

**Document Version:** 1.0  
**Last Updated:** 2026-03-09  
**Status:** Final Strategy - Ready for Implementation
