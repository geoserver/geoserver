.. _crs_manual_epsg:

Manually editing the EPSG database
==================================

.. warning:: These instructions are very advanced, and are here mainly for the curious who want to know details about the EPSG database subsystem.

To define a custom projection, edit the EPSG.sql file, which is used to create the cached EPSG database.

#. Navigate to the :file:`WEB-INF/lib` directory

#. Uncompress the :file:`gt2-epsg-h.jar` file.  On Linux, the command is::

      jar xvf gt2-epsg-h.jar

#. Open :file:`org/geotools/referencing/factory/epsg/EPSG.sql` with a text editor.  To add a custom projection, these entries are essential:
   
   #. An entry in the EPSG_COORDINATEREFERENCESYSTEM table::

      (41111,'WGC 84 / WRF Lambert',1324,'projected',4400,NULL,4326,20000,NULL,NULL,'US Nat. scale mapping.','Entered by Alex Petkov','Missoula Firelab WRF','WRF','2000-10-19','',1,0),

      where: 

         * **1324** is the EPSG_AREA code that describes the area covered by my projection
         * **4400** is the EPSG_COORDINATESYSTEM code for my projection
         * **20000** is the EPSG_COORDOPERATIONPARAMVALUE key for the array that contains my projection parameters

   #. An entry in the EPSG_COORDOPERATIONPARAMVALUE table::
      
      (20000,9802,8821,40,'',9102),    //latitude of origin
      (20000,9802,8822,-97.0,'',9102), //central meridian
      (20000,9802,8823,33,'',9110),    //st parallel 1
      (20000,9802,8824,45,'',9110),    //st parallel 2
      (20000,9802,8826,0.0,'',9001),   //false easting
      (20000,9802,8827,0.0,'',9001)    //false northing

      where:

         * **9802** is the EPSG_COORDOPERATIONMETHOD key for the Lambert Conic Conformal (2SP) formula

   #. An entry in the EPSG_COORDOPERATION table:

      (20000,'WRF Lambert','conversion',NULL,NULL,'',NULL,1324,'Used for weather forecasting.',0.0,9802,NULL,NULL,'Used with the WRF-Chem model for weather forecasting','Firelab in Missoula, MT','EPSG','2005-11-23','2005.01',1,0)

      where:

         * **1324** is the EPSG_AREA code that describes the area covered by my projection
         * **9802** is the EPSG_COORDOPERATIONMETHOD key for the Lambert Conic Conformal (2SP) formula

.. note:: Observe the commas. If you enter a line that is at the end of an INSERT statement, the comma is omitted (make sure the row before that has a comma at the end). Otherwise, add a comma at the end of your entry.

#. After all edits, save the file and exit.

#. Compress the gt2-epsg-h.jar file.  On Linux, the command is::

      jar -Mcvf gt2-epsg-h.jar META-INF org

#. Remove the cached copy of the EPSG database, so that can be recreated. On Linux, the command is::

      rm -rf /tmp/Geotools/Databases/HSQL

#. Restart GeoServer.

The new projection will be successfully parsed. Verify that the CRS has been properly parsed by navigating to the :ref:`srs_list` page in the :ref:`web_admin`.
