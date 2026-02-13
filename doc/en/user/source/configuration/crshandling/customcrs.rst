.. _crs_custom:

Custom CRS Definitions
======================

Add a custom CRS in the EPSG authority
--------------------------------------

This example shows how to add a custom projection in GeoServer.

#. The projection parameters need to be provided as a WKT (well known text) definition.  The code sample below is just an example::

      PROJCS["NAD83 / Austin",
        GEOGCS["NAD83",
          DATUM["North_American_Datum_1983",
            SPHEROID["GRS 1980", 6378137.0, 298.257222101],
            TOWGS84[0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0]],
          PRIMEM["Greenwich", 0.0],
          UNIT["degree", 0.017453292519943295],
          AXIS["Lon", EAST],
          AXIS["Lat", NORTH]],
        PROJECTION["Lambert_Conformal_Conic_2SP"],
        PARAMETER["central_meridian", -100.333333333333],
        PARAMETER["latitude_of_origin", 29.6666666666667],
        PARAMETER["standard_parallel_1", 31.883333333333297],
        PARAMETER["false_easting", 2296583.333333],
        PARAMETER["false_northing", 9842500.0],
        PARAMETER["standard_parallel_2", 30.1166666666667],
        UNIT["m", 1.0],
        AXIS["x", EAST],
        AXIS["y", NORTH],
        AUTHORITY["EPSG","100002"]]

   .. note:: This code sample has been formatted for readability.  The information will need to be provided on a single line instead, or with backslash characters at the end of every line (except the last one).

#. Go into the :file:`user_projections` directory inside your data directory, and open the :file:`epsg.properties` file.  If this file doesn't exist, you can create it.

#. Insert the code WKT for the projection at the end of the file (on a single line or with backslash characters)::

      100002=PROJCS["NAD83 / Austin", \
        GEOGCS["NAD83", \
          DATUM["North_American_Datum_1983", \
            SPHEROID["GRS 1980", 6378137.0, 298.257222101], \
            TOWGS84[0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0]], \
          PRIMEM["Greenwich", 0.0], \
          UNIT["degree", 0.017453292519943295], \
          AXIS["Lon", EAST], \
          AXIS["Lat", NORTH]], \
        PROJECTION["Lambert_Conformal_Conic_2SP"], \
        PARAMETER["central_meridian", -100.333333333333], \
        PARAMETER["latitude_of_origin", 29.6666666666667], \
        PARAMETER["standard_parallel_1", 31.883333333333297], \
        PARAMETER["false_easting", 2296583.333333], \
        PARAMETER["false_northing", 9842500.0], \
        PARAMETER["standard_parallel_2", 30.1166666666667], \
        UNIT["m", 1.0], \
        AXIS["x", EAST], \
        AXIS["y", NORTH], \
        AUTHORITY["EPSG","100002"]]

.. note:: Note the number that precedes the WKT.  This will determine the EPSG code.  So in this example, the EPSG code is 100002.

#. Save the file.

#. Restart GeoServer.

#. Verify that the CRS has been properly parsed by navigating to the :ref:`srs_list` page in the :ref:`web_admin`.

#. If the projection wasn't listed, examine the logs for any errors.

Override an official EPSG code
------------------------------

In some situations it is necessary to override an official EPSG code with a custom definition.  A common case is the need to change the TOWGS84 parameters in order to get better reprojection accuracy in specific areas.

The GeoServer referencing subsystem checks the existence of another property file, :file:`epsg_overrides.properties`, whose format is the same as :file:`epsg.properties`. Any definition contained in :file:`epsg_overrides.properties` will **override** the EPSG code, while definitions stored in :file:`epsg.proeprties` can only **add** to the database.

Special care must be taken when overriding the Datum parameters, in particular the **TOWGS84** parameters. To make sure the override parameters are actually used the code of the Datum must be removed, otherwise the referencing subsystem will keep on reading the official database in search of the best Datum shift method (grid, 7 or 5 parameters transformation, plain affine transform).

For example, if you need to override the official **TOWGS84** parameters of EPSG:23031::

 PROJCS["ED50 / UTM zone 31N", 
   GEOGCS["ED50", 
     DATUM["European Datum 1950", 
       SPHEROID["International 1924", 6378388.0, 297.0, AUTHORITY["EPSG","7022"]], 
       TOWGS84[-157.89, -17.16, -78.41, 2.118, 2.697, -1.434, -1.1097046576093785],
       AUTHORITY["EPSG","6230"]], 
     PRIMEM["Greenwich", 0.0, AUTHORITY["EPSG","8901"]], 
     UNIT["degree", 0.017453292519943295], 
     AXIS["Geodetic longitude", EAST], 
     AXIS["Geodetic latitude", NORTH], 
     AUTHORITY["EPSG","4230"]], 
  PROJECTION["Transverse_Mercator"], 
   PARAMETER["central_meridian", 3.0], 
   PARAMETER["latitude_of_origin", 0.0], 
   PARAMETER["scale_factor", 0.9996], 
   PARAMETER["false_easting", 500000.0], 
   PARAMETER["false_northing", 0.0], 
   UNIT["m", 1.0], 
   AXIS["Easting", EAST], 
   AXIS["Northing", NORTH], 
   AUTHORITY["EPSG","23031"]]
   
You should write the following (in a single line, here it's reported formatted over multiple lines for readability)::
  
  23031=
   PROJCS["ED50 / UTM zone 31N", 
     GEOGCS["ED50", 
       DATUM["European Datum 1950", 
         SPHEROID["International 1924", 6378388.0, 297.0, AUTHORITY["EPSG","7022"]], 
         TOWGS84[-136.65549, -141.4658, -167.29848, 2.093088, 0.001405, 0.107709, 11.54611], 
         AUTHORITY["EPSG","6230"]], 
       PRIMEM["Greenwich", 0.0, AUTHORITY["EPSG","8901"]], 
       UNIT["degree", 0.017453292519943295], 
       AXIS["Geodetic longitude", EAST], 
       AXIS["Geodetic latitude", NORTH]], 
    PROJECTION["Transverse_Mercator"], 
     PARAMETER["central_meridian", 3.0], 
     PARAMETER["latitude_of_origin", 0.0], 
     PARAMETER["scale_factor", 0.9996], 
     PARAMETER["false_easting", 500000.0], 
     PARAMETER["false_northing", 0.0], 
     UNIT["m", 1.0], 
     AXIS["Easting", EAST], 
     AXIS["Northing", NORTH], 
     AUTHORITY["EPSG","23031"]]

The definition has been changed in two places, the **TOWGS84** parameters, and the Datum code, ``AUTHORITY["EPSG","4230"]``, has been removed.

Configuring a custom CRS authority
----------------------------------

GeoServer also allows to configure a custom authority for your own CRS definitions.
This is useful when you have a set of custom projections that you want to manage separately from the EPSG database.

The process to declare a custom authority requires two steps:

#. Create a new :file:`user_projections/authorities.properties` properties file to declare the custom authorities, one row for each, following the ``prefix=title`` pattern, e.g.::

    MyCustomAuthority=My own CRS authority

#. Create a property file with the WKT definitions, similar to ``epsg.properties``, but named after the custom authority prefix, e.g.::

    user_projections/MyCustomAuthority.properties

.. note:: Mind the capitalization of the file name, it must match the authority prefix declared in the ``authorities.properties`` file.

The WKT definitions are still using a simple ``code=WKT`` pattern, for example ``MyCustomAuthority.properties`` will contain something like this::

    1=PROJCS["My Custom Projection", ... ]
    2=GEOGCS["My Custom Geographic CRS", ... ]

.. note:: In order to support axis order flipping for different versions of WMS/WFS, the WKT definitions in the above
          file should be provided in the geographic axis order, e.g., latitude/longitude, and GeoServer will take care
          of generating the longitude/latitude version automatically.

If the above files are modified at runtime, GeoServer won't notice the changes right away.
To force a reload of the custom authority definitions, issue a "reset" of the configuration,
e.g., from the UI go into "Server Status" and click on the "Reset" button.