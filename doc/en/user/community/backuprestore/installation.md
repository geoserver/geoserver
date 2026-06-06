---
render_macros: true
---

# Installation

!!! warning
    The module build must match your exact GeoServer version. A mismatched build will fail to load. Log in and check **About & Status > About GeoServer > Build Information** to determine the exact version in use.

1. Visit the [download page](https://geoserver.org/download/), switch to the **Development** tab, and locate the nightly build matching your GeoServer version.

2. Follow the **Community Modules** link and download `geoserver-{{ release }}-backup-restore-plugin.zip`.

   The website lists active nightly builds to provide feedback to developers. You may also [browse the build server](https://build.geoserver.org/geoserver/main/community-latest/) for the latest `main` community builds, or other branches under [https://build.geoserver.org/geoserver/](https://build.geoserver.org/geoserver/).

3. Stop GeoServer.

4. Extract the contents of the archive into `WEB-INF/lib` of your GeoServer installation.

5. Restart GeoServer.

## Verification

After restart, log in to the web admin interface and confirm that **Backup and Restore** appears under the **Data** section of the navigation menu. Open it and check that the page renders without errors.

The module can be driven from both the web interface and the REST API — see [usage via the web interface](usagegui.md) and [usage via the REST API](usagerest.md).
