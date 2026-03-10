---
**GSIP 240 - GitHub Actions for Documentation Deployment**

---

## Overview

Migrate GeoServer documentation builds from Jenkins to GitHub Actions following the approved GSIP 221 (MkDocs Migration). Jenkins can continue building after RST removal with additional reconfiguration work, but GitHub Actions provides superior maintainability through version-controlled configuration and eliminates SSH credential management.

**Key Benefits:**
- **Version-controlled configuration:** YAML in Git (reviewable, auditable) vs Jenkins UI (manual)
- **No credential management:** GitHub tokens (automatic) vs SSH keys (manual rotation)
- **Faster deployment:** Immediate trigger (<10s) vs polling (0-5 min delay)
- **Better maintenance:** Git-tracked changes vs manual Jenkins UI edits

---

## Proposed By

Peter Smythe  
**Date:** 2026-03-10

---

## Assigned to Release

This proposal applies to:
- **GeoServer 3.0** (main branch) - New MkDocs documentation
- **GeoServer 2.28.x** - New MkDocs documentation
- **Archive versions (2.27.x and earlier)** - Remain on OSGeo server (frozen, no updates)

---

## State

- [x] Under Discussion
- ☐ In Progress
- ☐ Completed
- ☐ Rejected
- ☐ Deferred

---

## Motivation

Following [GSIP 221](GSIP-221)'s approval to migrate from RST/Sphinx to Markdown/MkDocs, we must update our documentation build infrastructure. While Jenkins could be reconfigured to build MkDocs documentation, this would require:

1. **Manual reconfiguration** of Jenkins jobs through UI (not version-controlled)
2. **Continued SSH credential management** for OSGeo server deployment
3. **No audit trail** for deployment configuration changes
4. **Polling-based triggers** (5-minute delay) instead of immediate push events

GitHub Actions addresses all these issues while providing a modern, maintainable deployment pipeline with configuration tracked in Git alongside the documentation source.

---

## Proposal

Migrate documentation builds from Jenkins to GitHub Actions with the following changes:

### Build Infrastructure

| Component | Current (Jenkins) | Proposed (GitHub Actions) |
|-----------|-------------------|---------------------------|
| **Configuration** | Jenkins UI (manual) | YAML in Git (version-controlled) |
| **Credentials** | SSH keys (manual rotation) | GitHub tokens (automatic) |
| **Trigger** | SCM polling (5 min) | Push events (immediate) |
| **Hosting (new versions)** | OSGeo server | GitHub Pages |
| **Hosting (archives)** | OSGeo server | OSGeo server (unchanged) |

### Primary Benefits

#### 1. Version-Controlled Configuration

### 1. Version-Controlled Configuration (Git-Tracked YAML)

**Current (Jenkins):**
- Configuration stored in Jenkins UI (not in Git)
- Changes made through web interface
- No review process for deployment changes
- No audit trail of who changed what
- Cannot see configuration history
- Cannot revert configuration changes easily

**Proposed (GitHub Actions):**
- Configuration in `.github/workflows/docs-deploy.yml` (tracked in Git)
- Changes reviewed through pull requests
- Full audit trail in Git history
- Can see exactly what changed and when
- Easy rollback: `git revert` to undo changes
- Configuration changes go through same review as code

**Example workflow change:**
```yaml
# .github/workflows/docs-deploy.yml
name: Deploy Documentation

on:
  push:
    branches: [main, 2.28.x]
    paths: ['doc/**']

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Deploy with mike
        run: mike deploy stable --push
```

**Benefits:**
- ✅ **Reviewable** - Changes go through PR review process
- ✅ **Auditable** - Git log shows who changed what and why
- ✅ **Revertable** - Easy to undo bad changes
- ✅ **Documentable** - Commit messages explain changes
- ✅ **Testable** - Can test workflow changes on branches

#### 2. No SSH Credential Management

**Current (Jenkins):**
- Requires SSH private key stored in Jenkins
- Credential ID: "geotools" (shared across jobs)
- Manual key rotation when needed
- Risk of key exposure if Jenkins compromised
- Requires OSGeo server access for deployment
- Port 2223 (non-standard) must be accessible

**Proposed (GitHub Actions):**
- Uses GitHub's built-in authentication (automatic)
- No SSH keys to manage
- No credentials to rotate
- No server access required
- Deploys to GitHub Pages (built-in)
- GitHub manages all security

**Benefits:**
- ✅ **Zero credential management** - GitHub handles authentication
- ✅ **Better security** - No SSH keys to leak
- ✅ **Easier onboarding** - New maintainers don't need server access
- ✅ **No key rotation** - GitHub manages token lifecycle

#### 3. Easier Maintenance and Troubleshooting

**Current (Jenkins):**
- Must log into Jenkins UI to view/edit jobs
- Configuration scattered across multiple screens
- Build logs in Jenkins (separate from code)
- Requires Jenkins admin access to modify
- Changes not tracked in version control
- Difficult to reproduce builds locally

**Proposed (GitHub Actions):**
- Configuration in repository (visible to all)
- Single YAML file contains entire workflow
- Build logs in GitHub Actions (same place as code)
- Any maintainer can propose changes via PR
- All changes tracked in Git
- Can test workflows locally with `act` tool

**Benefits:**
- ✅ **Transparent** - Everyone can see configuration
- ✅ **Accessible** - No special access needed to view
- ✅ **Collaborative** - Changes reviewed by team
- ✅ **Reproducible** - Workflow defined in code

---

## Current State: Jenkins Documentation Builds

### Jenkins Configuration

**Active Jenkins Jobs:**
- `geoserver-main-docs` (main branch → latest)
- `geoserver-2.28.x-docs` (2.28.x → stable)
- `geoserver-2.27.x-docs` (2.27.x)
- `geoserver-2.26.x-docs` (2.26.x)

**Build Process:**
1. **Trigger:** Polls GitHub every 5 minutes for changes in `doc/` directory
2. **Build:** Maven + Ant + Sphinx converts RST files to HTML (1-2 minutes)
3. **Deploy:** SSH + ZIP transfer to OSGeo server at `geo-docs.geoserver.org:2223`
4. **Location:** `/var/www/geoserverdocs/$VERSION/en/{user,developer,docguide,api}/`

**Current URLs:**
```
https://docs.geoserver.org/latest/en/user/
https://docs.geoserver.org/stable/en/user/
https://docs.geoserver.org/2.28.x/en/user/
https://docs.geoserver.org/2.27.x/en/user/
```

### Jenkins Can Continue (With Additional Work)

**Important clarification:** Jenkins CAN continue building documentation after RST removal, but requires:

1. **Reconfigure build jobs** from Sphinx to MkDocs (manual UI changes)
2. **Update Maven/Ant scripts** to call MkDocs instead of Sphinx
3. **Test new build process** across all active branches
4. **Maintain SSH credentials** for OSGeo deployment
5. **Continue manual configuration management** (no version control)

**Why GitHub Actions is better:**
- Configuration changes tracked in Git (reviewable, auditable)
- No SSH credential management needed
- Immediate deployment triggers (no polling delay)
- Easier for new maintainers (no Jenkins access required)

---

## Proposed State: GitHub Actions + GitHub Pages

### GitHub Actions Workflow

**Workflow Trigger:**
- **Event:** Push to `main` or `2.28.x` branches
- **Path filter:** Changes to `doc/**` or `.github/workflows/docs-deploy.yml`
- **Delay:** Immediate (<10 seconds) - no polling

**Build Process:**
1. **Trigger:** GitHub webhook fires immediately on commit
2. **Build:** MkDocs builds all three manuals from **Markdown files** (2-3 minutes)
3. **Deploy:** mike deploys to gh-pages branch (built-in version management)
4. **Publish:** GitHub Pages serves from gh-pages branch
5. **CDN:** GitHub's global CDN distributes content worldwide

**Deployment Commands (in version-controlled YAML):**
```yaml
# .github/workflows/docs-deploy.yml
# Example for 2.28.x branch
- name: Deploy User Manual
  run: mike deploy --deploy-prefix "2.28.x/en/user" 2.28.x --push

- name: Deploy Developer Guide
  run: mike deploy --deploy-prefix "2.28.x/en/developer" 2.28.x --push

- name: Deploy Documentation Guide
  run: mike deploy --deploy-prefix "2.28.x/en/docguide" 2.28.x --push
```

### Hybrid Hosting Strategy

**GitHub Pages (New Versions - 2.28.x and later):**
- **Versions:** 2.28.x (stable), main (latest), future releases
- **URL:** `https://docs.geoserver.org/` (CNAME to `geoserver.github.io`)
- **Deployment:** Automatic via GitHub Actions
- **Source:** Markdown files in repository

**OSGeo Server (Archive Versions - 2.27.x and earlier):**
- **Versions:** 2.27.x, 2.26.x, 2.25.x, ... (all older versions)
- **URL:** `https://docs-archive.geoserver.org/` (new CNAME to OSGeo server)
- **Deployment:** None (frozen, no updates)
- **Status:** Remain exactly as-is (HTML already built)

**Version Selector:**
- New versions can link to archives (one-way navigation)
- Archive versions keep existing selectors (no updates needed)

---

## GitHub Pages 1 GB Limit: Not a Concern

### How Git Deduplication Works

GitHub Pages has a 1 GB limit for the gh-pages branch, but **Git automatically deduplicates identical files across versions**.

**Size breakdown per version (from actual build data - see size-analysis.md):**

| Component | First Version | Each Additional Version | Deduplication |
|-----------|---------------|-------------------------|---------------|
| HTML + Other content | 398 MB | ~20 MB | 95% shared (structure, common pages) |
| Images | 5.5 MB | ~1 MB | 80% shared (logos, diagrams) |
| CSS/JS | 2.7 MB | ~0.5 MB | 80% shared (minor changes) |
| Material theme assets | 2.5 MB | **0 MB** | **100% shared (identical)** |
| Search index | 4.5 MB | 4.5 MB | None (unique, but gzipped to 0.5 MB) |
| Tutorial data | 4.8 MB | **0 MB** | **100% shared (identical)** |
| **Total** | **416 MB** | **~26 MB** | **~390 MB saved per version** |

**Key insight:** Material theme assets, tutorial data files, most images, and 95% of HTML content are identical or very similar across versions. Git deduplication stores these only once, saving ~390 MB per additional version.

### Capacity Calculation

| Versions Hosted | Calculation | Total Size | % of 1 GB Limit |
|-----------------|-------------|------------|-----------------|
| 1 (2.28.x) | 416 MB | 416 MB | 41% |
| 2 (+ 3.0/main) | 416 + 26 | 442 MB | 44% |
| 3 (+ 3.1.x) | 416 + 52 | 468 MB | 47% |
| 4 (+ 3.2.x) | 416 + 78 | 494 MB | 49% |
| 5 (+ 3.3.x) | 416 + 104 | 520 MB | 52% |
| 6 (+ 3.4.x) | 416 + 130 | 546 MB | 55% |
| 7 (+ 3.5.x) | 416 + 156 | 572 MB | 57% |
| 8 (+ 3.6.x) | 416 + 182 | 598 MB | 60% |
| 9 (+ 3.7.x) | 416 + 208 | 624 MB | 62% |
| 10 (+ 3.8.x) | 416 + 234 | 650 MB | 65% |
| 15 (+ 5 more) | 416 + 364 | 780 MB | 78% |
| 20 (+ 5 more) | 416 + 494 | 910 MB | 91% |

**Conclusion:** Can host **20+ versions** on GitHub Pages with room to spare.

### Realistic Hosting Plan

**GitHub Pages (all active and future versions):**
- Latest (main branch / 3.0 dev)
- Stable (current release: 2.28.x initially, then 3.0, 3.1.x, etc.)
- Maint (e.g., 3.0.x when 3.1.x is stable)
- All future releases (3.2.x, 3.3.x, 3.4.x, ...)
- **Total: All new versions starting from 2.28.x = ~442-650 MB for 2-10 versions**

**OSGeo Archive (pre-migration versions only):**
- 2.27.x and earlier (all versions before MkDocs migration)
- Frozen, no updates needed
- Unlimited space on OSGeo

**Example after 3.1.x release:**
- GitHub Pages: latest (main), stable (3.1.x), maint (3.0.x), 2.28.x
- OSGeo Archive: 2.27.x, 2.26.x, 2.25.x, ...

**Important:** 2.28.x and all future versions stay on GitHub Pages permanently. Only pre-migration versions (2.27.x and earlier) remain on OSGeo.

---

## Backwards Compatibility

### URL Compatibility

All existing URLs continue to work with the hybrid hosting strategy:

**New versions (GitHub Pages):**
```
https://docs.geoserver.org/latest/en/user/      ✅ Works (3.0/main)
https://docs.geoserver.org/stable/en/user/      ✅ Works (2.28.x)
https://docs.geoserver.org/3.0/en/user/         ✅ Works (same as latest)
```

**Archive versions (OSGeo with new CNAME):**
```
https://docs-archive.geoserver.org/2.27.x/en/user/  ✅ Works
https://docs-archive.geoserver.org/2.26.x/en/user/  ✅ Works
```

**Deep links preserved:**
```
https://docs.geoserver.org/stable/en/user/installation/  ✅ Works
```

**100% backward compatible** - No broken links!

### Archive Version Handling

**Archive versions (2.27.x and earlier):**
- Remain on OSGeo server (no migration needed)
- Jenkins jobs disabled (no more builds)
- HTML already built and frozen
- Accessible via new CNAME: `docs-archive.geoserver.org`
- No updates needed (stable, end-of-life versions)

**Why this approach:**
- Minimal migration effort (only new versions)
- Archive stability (unchanged)
- No risk to existing documentation
- Saves GitHub Pages space for active versions

---

## Implementation Plan

### Timeline: 1 Week Maximum

**Why no parallel running?**
- Clean cutover is simpler and faster
- Jenkins can be re-enabled if issues arise
- Test thoroughly on migration branch first

### Day 1-2: Setup and Testing

1. **Create GitHub Actions workflow**
   - Write `.github/workflows/docs-deploy.yml`
   - Configure triggers, build steps, mike deployments
   - Test on migration branch

2. **Configure GitHub Pages**
   - Enable GitHub Pages on gh-pages branch
   - Add CNAME file for custom domain

3. **Test deployment**
   - Deploy to migration branch
   - Verify all three manuals build correctly
   - Check URLs and navigation

### Day 3-4: DNS Configuration

1. **Coordinate with infrastructure team**
   - Update `docs.geoserver.org` → `geoserver.github.io` (GitHub Pages)
   - Create `docs-archive.geoserver.org` → OSGeo server IP (archives)
   - Wait for DNS propagation (24-48 hours)

2. **Verify DNS changes**
   - Test `docs.geoserver.org` resolves to GitHub Pages
   - Test `docs-archive.geoserver.org` resolves to OSGeo
   - Enable HTTPS on GitHub Pages

### Day 5-7: Production Cutover

1. **Merge workflow to main and 2.28.x branches**
   - GitHub Actions starts building automatically
   - Documentation deploys to GitHub Pages

2. **Disable Jenkins jobs**
   - Disable `geoserver-main-docs`
   - Disable `geoserver-2.28.x-docs`
   - Disable archive version jobs (2.27.x, 2.26.x, etc.)

3. **Verify production**
   - Check `docs.geoserver.org` serves from GitHub Pages
   - Test all URLs work correctly
   - Verify version selector works
   - Monitor for any issues

4. **Update documentation**
   - Update README.md with new build process
   - Document GitHub Actions deployment
   - Announce completion to community

### Rollback Plan (If Needed)

**If issues arise during cutover:**
1. Revert DNS changes (point back to OSGeo)
2. Re-enable Jenkins jobs temporarily
3. Fix issues with GitHub Actions workflow
4. Retry cutover when ready

---

## Feedback

### Q: Why not keep everything on OSGeo?

**A:** GitHub Actions + GitHub Pages provides:
- Version-controlled configuration (reviewable, auditable)
- No SSH credential management
- Easier maintenance (GitHub manages infrastructure)
- Faster deployments (immediate trigger vs 5-minute polling)
- Global CDN (faster access worldwide)
- Free hosting with automatic HTTPS

### Q: What happens to old documentation versions?

**A:** Archive versions (2.27.x and earlier) remain on OSGeo server. Jenkins jobs are disabled (no more builds needed). HTML is already built and frozen. Accessible via `docs-archive.geoserver.org`.

### Q: What if GitHub Pages has issues?

**A:** We can quickly rollback:
1. Revert DNS changes (point back to OSGeo)
2. Re-enable Jenkins jobs temporarily
3. Fix issues with GitHub Actions workflow
4. Retry cutover when ready

### Q: Will this break existing links?

**A:** No. All URLs remain the same:
- `docs.geoserver.org` continues to work (CNAME to GitHub Pages)
- `docs-archive.geoserver.org` serves old versions (CNAME to OSGeo)
- Deep links preserved with same URL structure

### Q: What about the 1 GB GitHub Pages limit?

**A:** Not a concern:
- Git automatically deduplicates identical files (theme assets, tutorial data, 95% of HTML)
- First version: 416 MB, each additional version: ~26 MB
- Can host 20+ versions (910 MB for 20 versions = 91% of limit)
- All new versions (2.28.x and later) stay on GitHub Pages permanently
- Archive versions (2.27.x and earlier) remain on OSGeo (unlimited space)

### Q: How long will the migration take?

**A:** 1 week maximum:
- Days 1-2: Setup and testing
- Days 3-4: DNS configuration
- Days 5-7: Production cutover

### Q: Who needs to approve DNS changes?

**A:** Infrastructure team needs to:
- Update `docs.geoserver.org` CNAME to `geoserver.github.io`
- Create `docs-archive.geoserver.org` CNAME to OSGeo server
- Coordinate timing for DNS propagation

### Q: Can Jenkins continue building after RST removal?

**A:** Yes, but requires additional reconfiguration work:
- Reconfigure Jenkins jobs from Sphinx to MkDocs (manual UI changes)
- Update Maven/Ant scripts to call MkDocs instead of Sphinx
- Test new build process across all active branches
- Continue maintaining SSH credentials for OSGeo deployment
- No version control for configuration changes

GitHub Actions is recommended because it provides version-controlled configuration, eliminates SSH credential management, and offers easier maintenance.

---

## Voting

**Project Steering Committee:**

- Alessio Fabiani:
- Andrea Aime:
- Ian Turton:
- Jody Garnett:
- Jukka Rahkonen:
- Kevin Smith:
- Simone Giannecchini:
- Torben Barsballe:
- Nuno Oliveira:
- Peter Smythe: +1 (proposing)

**Community Support:**

- 

---

## Links

- **GSIP 221 (MkDocs Migration):** https://github.com/geoserver/geoserver/wiki/GSIP-221


---

**Status:** Awaiting PSC approval  
**Last Updated:** March 10, 2026
