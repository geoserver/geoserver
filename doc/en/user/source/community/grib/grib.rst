.. _community_grib:

GRIB format
===========

Installing the GeoServer GRIB format extension
----------------------------------------------

 #. Download the extension from the `nightly GeoServer community module builds <http://ares.opengeo.org/geoserver/master/community-latest/>`_.

    .. warning:: Make sure the version of the extension matches the version of the GeoServer instance!

 #. Extract the contents of the archive into the ``WEB-INF/lib`` directory of the GeoServer installation.

Configuring GRIB dataset
------------------------
For configuring a GRIB dataset the user must go to :guilabel:`Stores --> Add New Store --> GRIB`.

.. note:: Note that internally the GRIB extension uses the NetCDF reader, which supports also GRIB data.
 
 
Current limitations
-------------------

* Only WGS84 output CRS is supported
* Input coverages/slices should share the same bounding box (lon/lat coordinates are the same for the whole ND cube)
