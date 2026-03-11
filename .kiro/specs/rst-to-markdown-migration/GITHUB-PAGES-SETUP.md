# GitHub Pages Configuration Guide

This document provides step-by-step instructions for configuring GitHub Pages for the GeoServer documentation deployment.

## Prerequisites

- Repository admin access to geoserver/geoserver
- DNS configuration access (coordinate with infrastructure team)
- GitHub Actions workflow deployed (`.github/workflows/docs-deploy.yml`)

---

## Step 1: Enable GitHub Pages

1. Navigate to the repository on GitHub: https://github.com/geoserver/geoserver
2. Go to **Settings** → **Pages** (in the left sidebar)
3. Under **Source**, select:
   - **Deploy from a branch**
   - Branch: **gh-pages**
   - Folder: **/ (root)**
4. Click **Save**

**Expected Result:** GitHub Pages will be enabled and start building from the gh-pages branch.

---

## Step 2: Configure Custom Domain

### 2.1 Add CNAME Record

The GitHub Actions workflow automatically creates a CNAME file in the gh-pages branch with:
```
docs.geoserver.org
```

This happens on every push to the main branch.

### 2.2 Configure DNS (Coordinate with Infrastructure Team)

**Primary Domain (GitHub Pages):**
```
docs.geoserver.org CNAME geoserver.github.io
```

**Archive Domain (OSGeo Server):**
```
docs-archive.geoserver.org A <OSGeo-server-IP>
```

**DNS Configuration Steps:**
1. Contact OSGeo infrastructure team or DNS administrator
2. Request DNS changes:
   - Add CNAME record: `docs.geoserver.org` → `geoserver.github.io`
   - Add A record or CNAME: `docs-archive.geoserver.org` → OSGeo server IP
3. Wait for DNS propagation (24-48 hours)

### 2.3 Verify DNS Propagation

**Check DNS resolution:**
```bash
# Check primary domain
nslookup docs.geoserver.org

# Expected output:
# docs.geoserver.org canonical name = geoserver.github.io
# geoserver.github.io has address <GitHub-IP>

# Check archive domain
nslookup docs-archive.geoserver.org

# Expected output:
# docs-archive.geoserver.org has address <OSGeo-server-IP>
```

**Alternative check using dig:**
```bash
dig docs.geoserver.org +short
dig docs-archive.geoserver.org +short
```

---

## Step 3: Configure Custom Domain in GitHub

1. Go to **Settings** → **Pages**
2. Under **Custom domain**, enter: `docs.geoserver.org`
3. Click **Save**
4. Wait for DNS check to complete (may take a few minutes)

**Expected Result:** GitHub will verify the DNS configuration and show a green checkmark.

---

## Step 4: Enable HTTPS

1. After DNS verification completes, the **Enforce HTTPS** checkbox will become available
2. Check the **Enforce HTTPS** box
3. Wait for SSL certificate to be issued (usually 5-10 minutes)

**Expected Result:** 
- HTTPS certificate issued by Let's Encrypt
- All HTTP requests automatically redirect to HTTPS
- Green padlock in browser address bar

---

## Step 5: Verify Deployment

### 5.1 Check GitHub Pages URL

Before custom domain is active, verify deployment at:
```
https://geoserver.github.io/geoserver/latest/en/user/
https://geoserver.github.io/geoserver/stable/en/user/
```

### 5.2 Check Custom Domain URLs

After DNS propagation and HTTPS setup:
```
https://docs.geoserver.org/latest/en/user/
https://docs.geoserver.org/stable/en/user/
https://docs.geoserver.org/latest/en/developer/
https://docs.geoserver.org/latest/en/docguide/
https://docs.geoserver.org/latest/en/api/
```

### 5.3 Test Version Selector

1. Navigate to any documentation page
2. Click the version selector dropdown
3. Verify all versions are listed:
   - Latest (3.0 dev)
   - Stable (2.28.x)
   - Archive versions (2.27.x, 2.26.x, etc.)
4. Test switching between versions
5. Test clicking archive links (should redirect to docs-archive.geoserver.org)

### 5.4 Test on Multiple Browsers

Test documentation on:
- Chrome (desktop and mobile)
- Firefox (desktop and mobile)
- Safari (desktop and mobile)
- Edge (desktop)

**Check:**
- All pages load correctly
- Images display properly
- Navigation works
- Search functionality works
- Version selector works
- Mobile responsive design works

---

## Step 6: Monitor Initial Deployment

### 6.1 Check GitHub Actions Logs

1. Go to **Actions** tab in GitHub repository
2. Find the latest "Documentation Deployment" workflow run
3. Review logs for each step:
   - User Manual deployment
   - Developer Guide deployment
   - Documentation Guide deployment
   - API documentation deployment
   - CNAME configuration

**Look for:**
- ✅ All steps completed successfully
- ✅ No error messages
- ✅ mike deployments pushed to gh-pages
- ✅ API docs copied successfully

### 6.2 Check gh-pages Branch

1. Switch to gh-pages branch in GitHub
2. Verify directory structure:
   ```
   latest/en/user/
   latest/en/developer/
   latest/en/docguide/
   latest/en/api/
   stable/en/user/
   stable/en/developer/
   stable/en/docguide/
   stable/en/api/
   versions.json
   CNAME
   ```

### 6.3 Monitor Page Load Times

Use browser developer tools to check:
- Initial page load time (should be < 2 seconds)
- Asset loading (CSS, JS, images)
- CDN performance (GitHub Pages uses global CDN)

---

## Troubleshooting

### Issue: DNS Not Resolving

**Symptoms:** `nslookup docs.geoserver.org` returns NXDOMAIN

**Solution:**
1. Verify DNS records were added correctly
2. Wait longer for DNS propagation (can take up to 48 hours)
3. Clear local DNS cache: `ipconfig /flushdns` (Windows) or `sudo dscacheutil -flushcache` (Mac)
4. Try different DNS server: `nslookup docs.geoserver.org 8.8.8.8`

### Issue: HTTPS Certificate Not Issued

**Symptoms:** "Enforce HTTPS" checkbox is disabled or shows error

**Solution:**
1. Verify DNS is resolving correctly
2. Verify CNAME file exists in gh-pages branch
3. Wait 10-15 minutes for certificate issuance
4. If still failing, remove and re-add custom domain in GitHub Pages settings

### Issue: 404 Errors on Documentation Pages

**Symptoms:** Main page loads but subpages return 404

**Solution:**
1. Check gh-pages branch has correct directory structure
2. Verify mike deployments completed successfully
3. Check GitHub Actions logs for deployment errors
4. Verify `--deploy-prefix` is correct in workflow

### Issue: Version Selector Not Working

**Symptoms:** Version dropdown doesn't show all versions or links are broken

**Solution:**
1. Check `versions.json` exists in gh-pages branch
2. Verify mike deployments included version metadata
3. Check mkdocs.yml has correct version configuration
4. Clear browser cache and reload

### Issue: API Documentation Missing

**Symptoms:** API docs return 404 or are not accessible

**Solution:**
1. Check Maven build step completed successfully in GitHub Actions
2. Verify API docs were copied to gh-pages branch
3. Check directory exists: `<version>/en/api/`
4. Verify API docs exist in source: `doc/en/api/`

---

## Rollback Procedure

If issues arise and you need to rollback:

### Option 1: Revert to Jenkins Deployment

1. Re-enable Jenkins jobs:
   - `geoserver-main-docs`
   - `geoserver-2.28.x-docs`
2. Update DNS to point back to OSGeo server:
   ```
   docs.geoserver.org A <OSGeo-server-IP>
   ```
3. Wait for DNS propagation
4. Disable GitHub Pages in repository settings

### Option 2: Rollback gh-pages Branch

1. Find last known good commit in gh-pages branch
2. Reset gh-pages to that commit:
   ```bash
   git checkout gh-pages
   git reset --hard <commit-hash>
   git push --force origin gh-pages
   ```
3. GitHub Pages will rebuild from the reverted state

---

## Post-Configuration Checklist

- [ ] GitHub Pages enabled on gh-pages branch
- [ ] Custom domain configured: docs.geoserver.org
- [ ] DNS records updated (primary and archive domains)
- [ ] DNS propagation verified
- [ ] HTTPS enabled and certificate issued
- [ ] All documentation URLs accessible
- [ ] Version selector working correctly
- [ ] Archive links working (docs-archive.geoserver.org)
- [ ] Mobile responsive design verified
- [ ] Search functionality working
- [ ] GitHub Actions workflow running successfully
- [ ] Monitoring in place for future deployments

---

## Maintenance

### Regular Checks

**Weekly:**
- Monitor GitHub Actions workflow runs
- Check for deployment failures
- Verify documentation is up-to-date

**Monthly:**
- Review gh-pages branch size (should stay under 1 GB)
- Check SSL certificate renewal (automatic, but verify)
- Review version selector configuration

**After Each Release:**
- Verify new version deployed correctly
- Update version selector if needed
- Test all version links

### Updating Version Selector

When adding a new version or archiving an old version:

1. Update `mkdocs.yml` in all three manuals (user, developer, docguide)
2. Add new version to `extra.versions` list
3. Commit and push changes
4. GitHub Actions will automatically deploy updated configuration

**Example:**
```yaml
extra:
  versions:
    - version: latest
      title: "Latest (3.1 dev)"
      url: "/latest/"
      archive: false
    - version: stable
      title: "Stable (3.0.x)"
      url: "/stable/"
      archive: false
    - version: 2.28.x
      title: "2.28.x"
      url: "/2.28.x/"
      archive: false
    - version: 2.27.x
      title: "2.27.x (archive)"
      url: "https://docs-archive.geoserver.org/2.27.x/"
      archive: true
```

---

## Contact Information

**For DNS Issues:**
- Contact: OSGeo Infrastructure Team
- Email: sysadmin@osgeo.org

**For GitHub Pages Issues:**
- GitHub Support: https://support.github.com
- GitHub Pages Documentation: https://docs.github.com/en/pages

**For GeoServer Documentation Issues:**
- Mailing List: geoserver-devel@lists.sourceforge.net
- GitHub Issues: https://github.com/geoserver/geoserver/issues

---

**Document Version:** 1.0  
**Last Updated:** 2026-03-10  
**Status:** Ready for Implementation
