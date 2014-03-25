.. _community_geopkg:

GeoPackage Extension
====================
This plugin brings in the ability to read and write GeoPackage files in GeoServer. 
`GeoPackage <http://www.opengeospatial.org/projects/groups/geopackageswg/>`_ is an SQLite based standard format that is able to hold multiple vector and raster data layers in a single file.

GeoPackage files can be used both as Vector Data Stores as well as Raster Data Stores (so that both kinds of layers can published).

GeoPackage can be used as an output format for WFS :ref:`wfs_getfeature` (creating one vector data layer) as well as WMS :ref:`wms_getmap` (creating one raster data layer). 
The GeoServer GeoPackage extension also allows to create a completely custom made GeoPackage with multiple layers, using the GeoPackage process.

.. toctree::
   :maxdepth: 2

   installing
   input   
   output
