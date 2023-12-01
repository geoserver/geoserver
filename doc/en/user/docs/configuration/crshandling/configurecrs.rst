.. _crs_configure:

Coordinate Reference System Configuration
=========================================

When adding data, GeoServer tries to inspect data headers looking for an EPSG code:

* If the data has a CRS with an explicit EPSG code and the full CRS definition behind the code matches the one in GeoServer, the CRS will be already set for the data.
* If the data has a CRS but no EPSG code, you can use the :guilabel:`Find` option on the :ref:`data_webadmin_layers` page to make GeoServer perform a lookup operation where the data CRS is compared against every other known CRS. If this succeeds, an EPSG code will be selected. The common case for a CRS that has no EPSG code is shapefiles whose .PRJ file contains a valid WKT string without the EPSG identifiers (as these are optional).

If an EPSG code cannot be found, then either the data has no CRS or it is unknown to GeoServer.  In this case, there are a few options:

* Force the declared CRS, ignoring the native one.  This is the best solution if the native CRS is known to be wrong.
* Reproject from the native to the declared CRS.  This is the best solution if the native CRS is correct, but cannot be matched to an EPSG number.  (An alternative is to add a custom EPSG code that matches exactly the native SRS.  See the section on :ref:`crs_custom` for more information.)

If your data has no native CRS information, the only option is to specify/force an EPSG code.

Increasing Comparison Tolerance
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
Decimal numbers comparisons are made using a comparison tolerance. This means, as an instance, that an ellipsoid's semi_major axis
equals a candidate EPSG's ellipsoid semi_major axis only if their difference is within that tolerance.
Default value is 10^-9 although it can be changed by setting a COMPARISON_TOLERANCE Java System property to your container's JVM to specify a different value.

.. warning::

	The default value should be changed only if you are aware of use cases which require a wider tolerance.
	Don't change it unless really needed (See the following example).
	
Example
.......	
	
* Your sample dataset is known to be a LambertConformalConic projection and the related EPSG code defines latitude_of_origin value = 25.0.
* The coverageStore plugin is exposing raster projection details through a third party library which provides projection parameter definitions as float numbers. 
* Due to the underlying math computations occurring in that third party library, the exposed projection parameters are subject to some accuracy loss, so that the provided latitude_of_origin is something like 25.0000012 whilst all the other params match the EPSG definition.
* You notice that the native CRS isn't properly recognized as the expected EPSG due to that small difference in latitude_of_origin

In that case you could consider increasing a bit the tolerance.
