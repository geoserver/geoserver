# Jenkins Analysis Summary

**Date:** 2026-03-09  
**Analyst:** Migration Team  
**Data Source:** Live Jenkins API + Configuration Files

---

## Executive Summary

Comprehensive analysis of GeoServer's Jenkins documentation build system reveals a highly optimized process with **1-2 minute build times**. All critical functionality can be replicated in GitHub Actions with comparable performance.

---

## Key Discoveries

### 1. Build Performance (Actual vs Estimated)

| Metric | Initial Estimate | Actual (from Jenkins) | Difference |
|--------|------------------|----------------------|------------|
| Average build time | 5-10 minutes | **1m 24s** | 73% faster than estimated |
| Typical range | 5-10 minutes | **1-2 minutes** | Much faster |
| Deployment included | No | **Yes** | Already optimized |

**Conclusion:** Jenkins is already highly optimized. GitHub Actions will have similar performance (2-3 min), not faster as initially estimated.

### 2. Trigger Mechanism (CRITICAL FINDING)

**Jenkins Uses SCM Polling, NOT Webhooks:**
- Polls GitHub every 5 minutes (`H/5 * * * *`)
- Hash-based distribution prevents thundering herd
- Only checks `doc/` directory for changes
- Delay: 0-5 minutes after commit
- **Total commit-to-deployment: 6-7 minutes**

**Why This Matters:**
- GitHub Actions uses push events (webhooks) = **immediate trigger**
- Eliminates 0-5 minute polling delay
- **Documentation updates will be 5 minutes faster** with GitHub Actions
- Event-driven vs polling = better resource utilization

**Evidence:**
- All builds show "Started by an SCM change" (polling indicator)
- No webhook configuration in Jenkins XML
- `ignorePostCommitHooks=false` but no webhook evidence

### 2. Active Documentation Jobs

**4 active jobs** building documentation continuously:
- `geoserver-main-docs` (main branch → latest)
- `geoserver-2.28.x-docs` (2.28.x → stable)
- `geoserver-2.27.x-docs` (2.27.x)
- `geoserver-2.26.x-docs` (2.26.x)

**3 disabled jobs** for end-of-life versions:
- 2.25.x, 2.24.x, 2.23.x

### 3. Build Process Details

**8-step process:**
1. Install Python dependencies (Sphinx)
2. Build with Maven (API docs + Sphinx invocation)
3. Build English docs (user, developer, docguide)
4. Build Chinese docs (main branch only)
5. Stage files locally
6. Deploy to OSGeo server via SSH + ZIP
7. Update symlinks (latest/stable)
8. Deploy Chinese docs (main branch only)

**Total time:** 1-2 minutes for complete build + deployment

### 4. Deployment Configuration

**Target Server:**
- Host: `geoserverdocs@geo-docs.geoserver.org`
- Port: `2223` (non-standard SSH)
- Path: `/var/www/geoserverdocs/$VER/`
- Method: ZIP transfer + remote unzip

**Symlinks:**
- Main branch → `/var/www/geoserverdocs/latest`
- 2.28.x branch → `/var/www/geoserverdocs/stable`

**Security:**
- SSH key authentication (credential ID: "geotools")
- No password authentication
- StrictHostKeyChecking disabled for automation

### 5. Git Configuration Optimizations

**Performance optimizations:**
- Shallow clone (depth=3)
- No tags fetched
- Shared object cache: `/var/lib/jenkins/workspace/geoserver-main/.git`
- Path restriction: `doc/.*` (only trigger on doc changes)

**Trigger:**
- **Type:** SCM Polling (NOT webhooks)
- **Schedule:** `H/5 * * * *` (every 5 minutes with hash distribution)
- **Delay:** 0-5 minutes after commit
- **Total commit-to-deployment:** 6-7 minutes (5 min polling + 1-2 min build)

**Why Polling Instead of Webhooks:**
- Likely firewall/security restrictions
- Jenkins server may not be publicly accessible
- Polling is more reliable (no missed events)
- All builds show "Started by an SCM change" (polling indicator)

### 6. Build Concurrency Control

**Throttling:**
- Category: "Documentation"
- Max concurrent: **1 job at a time**
- Prevents resource contention on build server
- Ensures stable build performance

### 7. Chinese Documentation

**Only built for main branch:**
- Not built for maintenance branches (2.28.x, 2.27.x, etc.)
- Separate Maven build: `doc/zhCN/pom.xml`
- Separate deployment: `zhCN.zip` → `/var/www/geoserverdocs/$VER/zhCN/`
- No symlink management

---

## Gap Analysis Results

### Critical Gaps Identified

| Feature | Jenkins | Current GitHub Actions | Status |
|---------|---------|------------------------|--------|
| Build Documentation | ✅ Sphinx | ✅ MkDocs | ✅ Equivalent |
| API Documentation | ✅ Swagger | ❌ Missing | ⚠️ **GAP** |
| Deploy to OSGeo | ✅ SSH + ZIP | ❌ GitHub Pages only | ⚠️ **GAP** |
| Version Directories | ✅ Yes | ❌ Missing | ⚠️ **GAP** |
| Symlink Management | ✅ Yes | ❌ Missing | ⚠️ **GAP** |
| Chinese Docs | ✅ Yes | ⚠️ Not converted | ⚠️ **GAP** |
| Build Time | ✅ 1-2 min | ⚠️ 2-3 min (est) | ✅ Acceptable |

### Non-Critical Differences

| Feature | Jenkins | GitHub Actions | Impact |
|---------|---------|----------------|--------|
| Trigger | SCM polling (5 min) | Push-based (immediate) | **Positive (5 min faster)** |
| Trigger Delay | 0-5 minutes | <10 seconds | **Positive** |
| Build Tool | Sphinx | MkDocs | Neutral (equivalent) |
| Concurrency | Throttled (1 job) | Unlimited | Positive (parallel builds) |
| Workspace | Shared | Ephemeral | Neutral |
| Polling Overhead | Yes (every 5 min) | No (event-driven) | **Positive** |

**Key Improvement:** GitHub Actions eliminates the 0-5 minute polling delay, making documentation updates **5 minutes faster** on average.

---

## Recommendations

### 1. Update GitHub Actions Workflow

**Add missing functionality:**
- ✅ API documentation build (Maven + Swagger Codegen)
- ✅ OSGeo server deployment (SSH + ZIP transfer)
- ✅ Version directory management
- ✅ Symlink management (latest/stable)
- ⚠️ Chinese documentation (defer to separate task)

**Configuration required:**
- GitHub Secret: `GEOSERVER_DOCS_SSH_KEY`
- SSH key for `geoserverdocs@geo-docs.geoserver.org`
- Port 2223 access

### 2. Performance Expectations

**Realistic estimates:**
- GitHub Actions: **2-3 minutes** (build time)
- Jenkins: **1-2 minutes** (build time)
- Build time difference: **~1 minute slower**

**BUT: Total time to deployment is FASTER with GitHub Actions:**
- Jenkins: 0-5 min (polling) + 1-2 min (build) = **1-7 minutes total**
- GitHub Actions: <10s (trigger) + 2-3 min (build) = **2-3 minutes total**
- **Average improvement: 3-4 minutes faster** (eliminates polling delay)

**Why slower build but faster deployment?**
- Ephemeral runners (no shared object cache)
- Fresh Python environment each build
- No workspace reuse
- **BUT:** Immediate trigger eliminates 0-5 minute polling delay

**Why acceptable?**
- Push-based triggers (no polling delay)
- Better integration with GitHub
- Easier maintenance
- Version-controlled configuration
- **Net result: Faster documentation updates**

### 3. Migration Strategy

**Phase 1: Add missing functionality**
- Implement API documentation build
- Add OSGeo deployment step
- Test on migration branch

**Phase 2: Parallel running**
- Run both Jenkins and GitHub Actions for 1-2 weeks
- Compare outputs for consistency
- Monitor performance

**Phase 3: Cutover**
- Disable Jenkins jobs
- Make GitHub Actions primary
- Keep Jenkins as backup for 1 month

### 4. Risk Mitigation

**Low risk:**
- Deployment method identical (SSH + ZIP)
- Same target server and paths
- Well-tested deployment script

**Medium risk:**
- API documentation integration (new Maven step)
- SSH credentials management (GitHub Secrets vs Jenkins)

**Mitigation:**
- Test on migration branch first
- Deploy to test path initially
- Maintain Jenkins as backup

---

## Conclusion

Jenkins documentation builds are **already highly optimized** at 1-2 minutes. GitHub Actions will have **similar performance** (2-3 minutes), not faster as initially estimated. However, the migration is still recommended for:

✅ **Better automation** (push-based vs polling)  
✅ **Easier maintenance** (YAML vs Jenkins UI)  
✅ **Version control** (workflow changes in Git)  
✅ **Better integration** (GitHub-native)  
✅ **No resource contention** (dedicated runners)

**Recommendation:** Proceed with migration using the updated GitHub Actions configuration in the main jenkins-analysis.md document.

---

## Data Sources

1. **Jenkins API:** Live job configurations and build data
2. **Jenkins XML:** Raw configuration files (config.xml)
3. **Build logs:** Last successful builds for each job
4. **Git repository:** pom.xml, build.xml, mkdocs.yml files

**Analysis tool:** `analyze_jenkins_docs.py` (Python script using Jenkins REST API)

**Verification:** All data cross-referenced with multiple sources for accuracy.
