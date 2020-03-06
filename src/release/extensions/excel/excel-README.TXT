Excel WFS output format
-----------------------

This extension allows the user to request the attribute tables
of vector layers via WFS in Excel file format.

Two output formats are supported:

excel - Excel 97-2003 (.xls) binary format 
excel2007 - Excel 2007 (.xlsx) xml format

The older .xls binary format is smaller in size and compatible with older versions of 
Microsoft Office, but is limited to 65,536 rows and 256 columns.

The newer .xlsx xml format generates large files and is only compatible with newer verions
of Microsoft Office, but can store up to 1,048,576 rows and 16,384 columns.

Both formats should be compatible with all versions of OpenOffice. 

For more information http://docs.geoserver.org/latest/en/user/extensions/excel.html

Installation
-----------------------
1. Stop GeoServer
2. Copy the jar files contained in this zip into geoserver/WEB-INF/lib
3. Restart GeoServer

Usage
-----------------------

Issue a WFS request using &outputFormat=excel, for example:

excel: http://localhost:8080/geoserver/wfs?request=GetFeature&version=1.0.0&typeName=topp:states&outputFormat=excel

excel2007: http://localhost:8080/geoserver/wfs?request=GetFeature&version=1.0.0&typeName=topp:states&outputFormat=excel2007
