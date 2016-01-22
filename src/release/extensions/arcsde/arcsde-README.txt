GeoServer ArcSDE Datastore Extension
====================================

This extension adds functionality to GeoServer to allow connection to ArcSDE
instances.

IMPORTANT NOTE:

Due to licensing issues, there are certain files that are not distributed
with this archive that are REQUIRED for it to work!

Files required:

1) jsde_sdk.jar (also known as jsde##_sdk.jar where ## is the version number,
   such as 92 for 9.2)
2) jpe_sdk.jar (also known as jpe##_sdk.jar where ## is the version number,
   such as 92 for 9.2)
3) icu4j_3_2.jar (only needed for ArcSDE 9.2 and newer)

These files can be downloaded from ESRI's website or copied from the ArcSDE
installation media.

To download the JSDE/JPE JAR files from ESRI's website:

1) Navigate to:
   http://support.esri.com/index.cfm?fa=downloads.patchesServicePacks.listPatches&PID=66
2) Find the link to the latest service pack for your version of ArcSDE
3) Scroll down to "Installing this Service Pack" -> "ArcSDE SDK" -> "UNIX"
   (regardless of your target OS)
4) Download any of the target files (but be sure to match 32/64 bit to your OS)
5) Open the archive, and extract the JARs.

To download the icu4j JAR file:

1) Navigate to:
   ftp://ftp.software.ibm.com/software/globalization/icu/icu4j/3.2/
2) Download the file icu4j_3_2.jar.


INSTALLATION

1) Copy all files to the GeoServer library directory.
   In a binary install this is [GEOSERVER_HOME]/server/geoserver/WEB-INF/lib/
   In a WAR install this is [container]/webapps/geoserver/WEB-INF/lib/
2) Restart GeoServer.

COMPATIBILITY

This jar should work with any version of GeoServer based on GeoTools 15.x  
Currently this is anything in 2.9.x. 

For more information see on needed libraries and parameters, see:
http://docs.geoserver.org/latest/en/user/data/database/arcsde.html

For more Geotools related information: 
http://docs.geotools.org/latest/userguide/library/data/arcsde.html

Please report any bugs with jira (https://osgeo-org.atlassian.net/projects/GEOS).

Any other issues can be discussed on the geoserver-users mailing list
(http://lists.sourceforge.net/lists/listinfo/geoserver-users).

