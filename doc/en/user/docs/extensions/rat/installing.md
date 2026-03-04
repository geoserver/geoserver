# Installing the RAT module

To install the Raster Attribute Table support:

1.  From the [website download](https://geoserver.org/download) page, locate your release, and download: [rat](https://sourceforge.net/projects/geoserver/files/GeoServer/{{ release }}/extensions/geoserver-{{ release }}-rat-plugin.zip)

    !!! warning

        Make sure to match the version of the extension to the version of GeoServer.

2.  Extract these files and place the JARs in `WEB-INF/lib`.

3.  Perform any configuration required by your servlet container, and then restart.
