---
render_macros: true
---

# Installing the Proxy Base extension {: #proxy_base_ext_install }

The Proxy Base extension is listed among the other extension downloads on the GeoServer download page.

The installation process is similar to other GeoServer extensions:

1.  Visit the [website download](https://geoserver.org/download) page, locate your release, and download: `proxy-base-ext`{.interpreted-text role="download_extension"}

    Verify that the version number in the filename (for example {{ release }} above) corresponds to the version of GeoServer you are running.

2.  Extract the contents of the archive into the **`WEB-INF/lib`** directory in GeoServer. Make sure not to create any sub-directories during the extraction process.

3.  Restart GeoServer.

On successful installation, a new Proxy Base Extension entry will appear in the left menu, under "Settings".

![](images/proxy_base_settings.png)
*The Proxy Base Extension menu entry*
