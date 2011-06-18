.. _crs_configure:

Coordinate Reference System Configuration
=========================================

When adding data, GeoServer tries to inspect data headers looking for an EPSG code:

* If the data has a CRS with an explicit EPSG code and the full CRS definition behind the code matches the one in GeoServer, the CRS will be already set for the data.
* If the data has a CRS but no EPSG code, you can use the :guilabel:`Find` option on the :ref:`webadmin_layers` page to make GeoServer perform a lookup operation where the data CRS is compared against every other known CRS. If this succeeds, an EPSG code will be selected. The common case for a CRS that has no EPSG code is shapefiles whose .PRJ file contains a valid WKT string without the EPSG identifiers (as these are optional).

If an EPSG code cannot be found, then either the data has no CRS or it is unknown to GeoServer.  In this case, there are a few options:

* Force the declared CRS, ignoring the native one.  This is the best solution if the native CRS is known to be wrong.
* Reproject from the native to the declared CRS.  This is the best solution if the native CRS is correct, but cannot be matched to an EPSG number.  (An alternative is to add a custom EPSG code that matches exactly the native SRS.  See the section on :ref:`crs_custom` for more information.)

If your data has no native CRS information, the only option is to specify/force an EPSG code.
