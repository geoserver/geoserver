.. _iauwkt.usage:

Using IAU authority
-------------------

Support for the IAU authority required deep changes to the GeoServer code base, as the 
assumption that the only possible authority is EPSG was widespread.

At the time of writing, the following modules support IAU:

* GeoTIFF reading and writing (e.g. WCS output)
* Shapefile and GeoPackage reading and writing (e.g. WFS output)
* PostGIS reading (the ``spatial_ref_sys`` table must contain the IAU codes and definitions)
* Basic functionality of WMS, WFS, WCS, WPS, OGC API - Features and OGC API - Maps.
* GML and GeoJSON outputs, in various versions
* The importer module should be able to successfully handle input data in IAU CRSs

Other functionality might not be ready, the code base will be improved and generalized
as contributions and funding allow.
