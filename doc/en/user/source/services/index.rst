.. _services:

Services
========

GeoServer serves data using standard protocols established by the `Open Geospatial Consortium <http://www.opengeospatial.org>`_:  

- The **Web Map Service** (WMS) supports requests for map images (and other formats) generated from geographical data.  
- The **Web Feature Service** (WFS) supports requests for geographical feature data (with vector geometry and attributes).  
- The **Web Coverage Service** (WCS) supports requests for coverage data (rasters).  

These services are the primary way that GeoServer supplies geospatial information.

.. toctree::
   :maxdepth: 1

   wms/index
   wfs/index
   wcs/index
   wps/index
   csw/index