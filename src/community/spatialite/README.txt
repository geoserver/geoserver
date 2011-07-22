SpatiaLiteOutputFormat WFS Output for GeoServer 2.2.x

USAGE
---------------------------

The SpatiaLiteOutpuFormat WFS Output adds support for one additional output format for 
WFS GetFeature requests. The new format, Spatialite is associated to the 
"application/x-sqlite3" mime type.
They produce a standard Spatialite db file.

Spatialite is a Spatial extension for Sqlite. the SpatiaLite extension enables 
SQLite to support spatial data [aka GEOMETRY], in a way conformant to OpenGis 
specifications.
SpatiaLiteOutputFormat works through a JDBC driver for Sqlite, loads the
appropriate Spatialite library and begins to execute the appropriate querys
to create the tables inside the an .sqlite file.

Request Example.
http://localhost:8080/geoserver/wfs?request=GetFeature&version=1.0.0&typeName=topp:states&outputFormat=spatialite
 
GET requests format_options
===========================
The following format_options are supported:
1) FILENAME: name to be assigned to the Spatialite db file. If specified,
must contain a name. By default a standard name (first layer name )will be assigned to the Spatialite db file.

PREREQUISITES
---------------------------

1) JAVA SDK
Download and install the Java SDK.

Create an environment variable called JAVA_HOME and point it to your Java SDK 
directory.
Then modify the PATH variable and add: ;%JAVA_HOME%/bin
Apply the changes.

2) Maven (2.2.1)
Windows: http://www.apache.org/dyn/closer.cgi/maven/binaries/maven-2.2.1.exe
Linux: http://www.apache.org/dyn/closer.cgi/maven/binaries/maven-2.2.1.zip


If you are using Linux, execute the following commands:
export M2_HOME=/usr/java/maven-2.0.4
export PATH=$PATH:$M2_HOME/bin

3)PROJ and GEOS
NOTE: This is only if you are Linux user

execute the following commands:
sudo apt-get install lib-proj-dev lib-geos-dev


4) Build Source Code
Go to the command line and navigate to the root of the source tree.
Execute the command:

mvn clean install

mvn eclipse:eclipse

If it fails, just try again. It trys to download jars and some might not be 
available at that time. So just keep trying.

Note: Working on Windows 32 bits, Linux 32 and 64 bits, Mac OSx 32 and 64, Mac PPC 32 and 64
