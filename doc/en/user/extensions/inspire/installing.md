---
render_macros: true
---


# Installing the INSPIRE extension

1.  Login, and navigate to **About & Status > About GeoServer** and check **Build Information** to determine the exact version of GeoServer you are running.

2.  Visit the [website download](https://geoserver.org/download) page, change the **Archive** tab, and locate your release.

    From the list of **Other** extensions download **INSPIRE**.

    - {{ release }} example: [inspire](https://sourceforge.net/projects/geoserver/files/GeoServer/{{ release }}/extensions/geoserver-{{ release }}-inspire-plugin.zip)
    - {{ snapshot }} example: [inspire](https://build.geoserver.org/geoserver/main/ext-latest/geoserver-{{ snapshot }}-inspire-plugin.zip)

    Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example {{ release }} above).

3.  Extract the archive and copy the contents into the GeoSever **`WEB-INF/lib`** directory.

4.  Restart GeoServer.

To verify that the extension was installed successfully, please see the next section on [Using the INSPIRE extension](using.md).
