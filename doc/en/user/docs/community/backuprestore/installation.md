---
render_macros: true
---

# Installation

## Manual Install

To download and install the required extensions by hand:

1.  Download the geoserver-{{ release }}-backup-restore-plugin.zip from:

    -   [Community Builds](https://build.geoserver.org/geoserver/main/community-latest/) (GeoServer WebSite)

    It is important to download the version that matches the GeoServer you are running.

2.  Stop the GeoServer application.

3.  Navigate into the **`webapps/geoserver/WEB-INF/lib`** folder.

    These files make up the running GeoServer application.

4.  Unzip the contents of the zip file into the **`lib`** folder.

5.  Restart the Application Server.

6.  Login to the Web Administration application. Select **Data** from the naviagion menu. Click **Backup and Restore** and ensure the page is rendered correctly and without errors.

Backup and Restore plugin can be used both via user interface and via HTTP REST interface. For more details please see the next sections.
