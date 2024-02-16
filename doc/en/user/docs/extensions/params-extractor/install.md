---
render_macros: true
---

# Installing the Parameter Extractor extension {: #params_extractor_install }

The Parameter Extractor extension is listed among the other extension downloads on the GeoServer download page.

The installation process is similar to other GeoServer extensions:

1.  Visit the [website download](https://geoserver.org/download) page, locate your release, and download: `params-extractor`{.interpreted-text role="download_extension"}

    Verify that the version number in the filename (for example {{ release }} above) corresponds to the version of GeoServer you are running.

2.  Extract the contents of the archive into the **`WEB-INF/lib`** directory in GeoServer. Make sure you do not create any sub-directories during the extraction process.

3.  Restart GeoServer.

If installation was successful, you will see a new Params-Extractor entry in the left menu, under "Settings".

![](images/menu.png)
*The Parameter Extractor menu entry*
