.. _netcdf:

NetCDF
======

Adding a NetCDF data store
--------------------------

.. figure:: netcdfcreate.png
   :align: center

   *NetCDF in the list of raster data stores*

Configuring a NetCDF data store
-------------------------------

.. figure:: netcdfconfigure.png
   :align: center

   *Configuring a NetCDF data store*

.. list-table::
   :widths: 20 80

   * - **Option**
     - **Description**
   * - ``Workspace``
     - 
   * - ``Data Source Name``
     - 
   * - ``Description``
     - 
   * - ``Enabled``
     -  
   * - ``URL``
     - 
	 
Supporting Custom NetCDF Coordinate Reference Systems
-----------------------------------------------------
Starting with GeoServer 2.8.x, NetCDF related modules (both NetCDF/GRIB store, imageMosaic store based on NetCDF/GRIB dataset and NetCDF output format) allow to support custom Coordinate Reference Systems and Projections.
As reported in the `NetCDF CF documentation, Grid mappings section <http://cfconventions.org/Data/cf-conventions/cf-conventions-1.6/build/cf-conventions.html#appendix-grid-mappings>`_
a NetCDF CF file may expose gridMapping attributes to describe the underlying projection. 

The GeoTools NetCDF machinery will parse the attributes (if any) contained in the underlying NetCDF dataset to setup an OGC CoordinateReferenceSystem object.
Once created, a CRS lookup will be made to identify a custom EPSG (if any) defined by the user to match that Projection.

User defined NetCDF Coordinate Reference Systems with their custom EPSG need to be provided in :file:`user_projections\\netcdf.projections.properties` file inside your data directory (you have to create that file if missing).  

A sample entry in that property file could look like this:

      9718001=PROJCS["lambert_conformal_conic_1SP", GEOGCS["unknown", DATUM["unknown", SPHEROID["unknown", 6371229.0, 0.0]], PRIMEM["Greenwich", 0.0], UNIT["degree", 0.017453292519943295], AXIS["Geodetic longitude", EAST], AXIS["Geodetic latitude", NORTH]], PROJECTION["Lambert_Conformal_Conic_1SP"], PARAMETER["central_meridian", -95.0], PARAMETER["latitude_of_origin", 25.0], PARAMETER["scale_factor", 1.0], PARAMETER["false_easting", 0.0], PARAMETER["false_northing", 0.0], UNIT["m", 1.0], AXIS["Easting", EAST], AXIS["Northing", NORTH], AUTHORITY["EPSG","9718001"]]

.. note:: Note the "unknown" names for GEOGCS, DATUM and SPHEROID elements. This is how the underlying NetCDF machinery will name custom elements.
.. note:: Note the number that precedes the WKT.  This will determine the EPSG code.  So in this example, the EPSG code is 97180001.
 
You may specify further custom NetCDF EPSG references by adding more lines to that file. 

#. Insert the code WKT for the projection at the end of the file (on a single line or with backslash characters)::
     
      9718002=PROJCS["lambert_conformal_conic_2SP", \
	    GEOGCS["unknown", \
		  DATUM["unknown", \
		    SPHEROID["unknown", 6377397.0, 299.15550239234693]], \
	      PRIMEM["Greenwich", 0.0], \
		  UNIT["degree", 0.017453292519943295], \
		  AXIS["Geodetic longitude", EAST], \
		  AXIS["Geodetic latitude", NORTH]], \
		PROJECTION["Lambert_Conformal_Conic_2SP"], \
		PARAMETER["central_meridian", 13.333333015441895], \
		PARAMETER["latitude_of_origin", 46.0], \
		PARAMETER["standard_parallel_1", 46.0], \
		PARAMETER["standard_parallel_2", 49], \
		PARAMETER["false_easting", 0.0], \
		PARAMETER["false_northing", 0.0], 
		UNIT["m", 1.0], \
		AXIS["Easting", EAST], \
		AXIS["Northing", NORTH], \
		AUTHORITY["EPSG","97180002"]]

#. Save the file.

#. Restart GeoServer.

#. Verify that the CRS has been properly parsed by navigating to the :ref:`srs_list` page in the :ref:`web_admin`.

#. If the projection wasn't listed, examine the logs for any errors.

Specify an external file through system properties
--------------------------------------------------
You may also specify the NetCDF projections definition file by setting a **Java system property** which links to the specified file.
As an instance: :file:`-Dnetcdf.projections.file=/full/path/of/the/customfile.properties`


