---
render_macros: true
---

# Installing the GeoServer Web Resource extension {: #web_resource_install }

The **Resource Brower** tool is provided by the web-resource extension is listed on the GeoServer download page.

To install web-resource extension:

1.  From the [GeoServer Download](https://geoserver.org/download) page locate the release used and download: `web-resource`{.interpreted-text role="download_extension"}

    Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example {{ release }} above).

2.  Extract the contents of the archive into the **`WEB-INF/lib`** directory in GeoServer.

    This extension includes two jars.

3.  Restart GeoServer.

4.  To confirm successful installation, navigate to **Tools** page and confirm the availability of **Resource Browser** tool.
