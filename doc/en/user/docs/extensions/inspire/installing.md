---
render_macros: true
---

# Installing the INSPIRE extension

1.  Visit the [website download](https://geoserver.org/download) page, locate your release, and download: {{ download_extension('inspire') }} (nightly {{ download_extension('inspire','snapshot') }})

    Verify that the version number in the filename (for example {{ release }} above) corresponds to the version of GeoServer you are running.

2.  Extract the archive and copy the contents into the GeoSever **`WEB-INF/lib`** directory.

3.  Restart GeoServer.

To verify that the extension was installed successfully, please see the next section on [Using the INSPIRE extension](using.md).
