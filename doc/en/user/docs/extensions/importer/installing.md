---
render_macros: true
---

# Installing the Importer extension

1.  Visit the [website download](https://geoserver.org/download) page, locate your release, and download: {{ download_extension('importer') }} (nightly {{ download_extension('importer','snapshot') }})

    Verify that the version number in the filename (for example {{ release }} above) corresponds to the version of GeoServer you are running.

2.  Extract the archive and copy the contents into the GeoServer **`WEB-INF/lib`** directory.

3.  Restart GeoServer.

4.  To verify that the extension was installed successfully, open the [Web administration interface](../../webadmin/index.md) and look for an **Import Data** option in the **Data** section on the left-side menu.

    ![](images/importer_link.png)
    *Importer extension successfully installed.*

For additional information please see the section on [Using the Importer extension](using.md).
