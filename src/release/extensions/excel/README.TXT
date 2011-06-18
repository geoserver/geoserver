Excel WFS output format
-----------------------

This extension allows the user to request the attribute tables
of vector layers via WFS in Excel file format

Installation
-----------------------
1. Stop GeoServer
2. Copy the jar files contained in this zip into geoserver/WEB-INF/lib
3. Restart GeoServer

Usage
-----------------------

Issue a request using &outputFormat=excel, for example:

http://localhost:8080/geoserver/wfs?request=GetFeature&version=1.0.0&typeName=topp:states&outputFormat=excel
