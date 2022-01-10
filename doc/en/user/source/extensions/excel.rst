.. _excel_extension:

Excel WFS Output Format
=======================

The GeoServer Excel plugin adds the ability to output WFS responses in either Excel 97-2003 (``.xls``) or Excel 2007 (``.xlsx``) formats.

Installation
------------

   1. Download the Excel plugin for your version of GeoServer from the `download page <http://geoserver.org/download>`_.
   2. Unzip the archive into the WEB-INF/lib directory of the GeoServer installation.
   3. Restart GeoServer.

Usage
-----

When making a WFS request, set the ``outputFormat`` to ``excel`` (for Excel 97-2003) or ``excel2007`` (for Excel 2007).

Examples
--------

Excel 97-2003 GET:
  http://localhost:8080/geoserver/wfs?request=GetFeature&version=1.1.0&typeName=topp:states&outputFormat=excel

Excel 2007 GET:
  http://localhost:8080/geoserver/wfs?request=GetFeature&version=1.1.0&typeName=topp:states&outputFormat=excel2007

**Excel 97-2003 POST**::

  <wfs:GetFeature service="WFS" version="1.1.0"
    outputFormat="excel"
    xmlns:topp="http://www.openplans.org/topp"
    xmlns:wfs="http://www.opengis.net/wfs"
    xmlns:ogc="http://www.opengis.net/ogc"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.opengis.net/wfs
                        http://schemas.opengis.net/wfs/1.1.0/wfs.xsd">
    <wfs:Query typeName="topp:states" />
  </wfs:GetFeature>

Limitations
-----------

Excel 97-2003 files are stored in a binary format and are thus space-efficient, but have inherent size limitations (65,526 rows per sheet; 256 columns per sheet). 

Excel 2007 files are XML-based, and have much higher limits (1,048,576 rows per sheet; 16,384 columns per sheet). 
However, because they are text files Excel 2007 files are usually larger than Excel 97-2003 files.

If the number of rows in a sheet or characters in a cell exceeds the limits of the chosen Excel file format, warning text is inserted to indicate the truncation.
