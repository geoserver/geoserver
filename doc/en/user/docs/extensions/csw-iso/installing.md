---
render_macros: true
---


# Installing Catalog Services for Web (CSW) - ISO Metadata Profile

To install the CSW ISO extension:

1.  Login, and navigate to **About & Status > About GeoServer** and check **Build Information** to determine the exact version of GeoServer you are running.

2.  Visit the [website download](https://geoserver.org/download) page, change the **Archive** tab, and locate your release.

    If you do not have the CSW extension yet, get it first. From the list of **OGC Services** extensions download **CSW**.

    - {{ release }} example: [csw](https://sourceforge.net/projects/geoserver/files/GeoServer/{{ release }}/extensions/geoserver-{{ release }}-csw-plugin.zip)
    - {{ snapshot }} example: [csw](https://build.geoserver.org/geoserver/main/ext-latest/geoserver-{{ snapshot }}-csw-plugin.zip)

    From the list of **OGC Services** extensions download **CSW ISO Metadata Profile**.

    - {{ release }} example: [csw-iso](https://sourceforge.net/projects/geoserver/files/GeoServer/{{ release }}/extensions/geoserver-{{ release }}-csw-iso-plugin.zip)
    - {{ snapshot }} example: [csw-iso](https://build.geoserver.org/geoserver/main/ext-latest/geoserver-{{ snapshot }}-csw-iso-plugin.zip)

    Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example {{ release }} above).

3.  Extract these zip files and place all the JARs in `WEB-INF/lib`.

4.  Perform any configuration required by your servlet container, and then restart.

5.  Verify that the CSW module was installed correctly by going to the Welcome page of the [Web administration interface](../../webadmin/index.md) and seeing that **CSW** is listed in the **Service Capabilities** list.

    Open the CSW capabilities and search for the text ``gmd:MD_Metadata`` to verify that the ISO metadata profile was installed correctly.
