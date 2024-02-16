---
render_macros: true
---

# Installation {: #spatialjson_installation }

## Manual Installation

To download and install the required extensions by hand:

1.  Download the geoserver- {{ release }}-spatialjson-plugin.zip from:

    -   [Community Builds](https://build.geoserver.org/geoserver/main/community-latest/) (GeoServer WebSite)

    It is important to download the version that matches the GeoServer you are running.

2.  Stop the GeoServer application.

3.  Navigate into the **`webapps/geoserver/WEB-INF/lib`** folder.

    These files make up the running GeoServer application.

4.  Unzip the contents of the zip file into the **`lib`** folder.

5.  Restart the Application Server.

After restarting the Application Server the SpatialJSON WFS output format is available and ready to use.
