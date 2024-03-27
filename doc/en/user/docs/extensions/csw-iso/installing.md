# Installing Catalog Services for Web (CSW) - ISO Metadata Profile

To install the CSW ISO extension:

1.  Visit the GeoServer [download](https://geoserver.org/download) page and navigate to the download page for the version of GeoServer your are using. If you do not have the CSW extension yet, get it first. Both the **csw** and **csw-iso** downloads are listed under extensions. The file needed are **`geoserver-*-csw-plugin.zip`** (if necessary) and **`geoserver-*-SNAPSHOT-csw-iso-plugin.zip`**, where `*` matches the version number of GeoServer you are using.
2.  Extract these zip files and place all the JARs in `WEB-INF/lib`.
3.  Perform any configuration required by your servlet container, and then restart.
4.  Verify that the CSW module was installed correctly by going to the Welcome page of the [Web administration interface](../../webadmin/index.md) and seeing that **CSW** is listed in the **Service Capabilities** list. Open the CSW capabilities and search for the text ``gmd:MD_Metadata`` to verify that the ISO metadata profile was installed correctly.
