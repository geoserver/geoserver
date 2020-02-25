
Maven Quickstart
================

This guide is designed to get developers up and running as quick as possible. For a more comprehensive guide see the :ref:`maven_guide`.

.. include:: checkout.txt

Command line build
------------------

#. Change directory to the root of the source tree and execute the maven build
command::

  cd geoserver/src
  mvn install -DskipTests -T 2C

  This will result in significant downloading of dependencies on the first build.

#. A successful build will result in output that ends with something like the following::

     [INFO] ------------------------------------------------------------------------
     [INFO] Reactor Summary:
     [INFO] 
     [INFO] GeoServer 2.15-SNAPSHOT ............................ SUCCESS [  3.735 s]
     [INFO] Core Platform Module ............................... SUCCESS [  1.926 s]
     [INFO] Open Web Service Module ............................ SUCCESS [  1.079 s]
     [INFO] Main Module ........................................ SUCCESS [  7.371 s]
     [INFO] GeoServer Security Modules ......................... SUCCESS [  0.172 s]
     [INFO] GeoServer Security Tests Module .................... SUCCESS [  2.040 s]
     [INFO] GeoServer JDBC Security Module ..................... SUCCESS [  0.904 s]
     [INFO] GeoServer LDAP Security Module ..................... SUCCESS [  1.823 s]
     [INFO] Web Coverage Service Module ........................ SUCCESS [  0.812 s]
     [INFO] Web Coverage Service 1.0 Module .................... SUCCESS [  1.432 s]
     [INFO] Web Coverage Service 1.1 Module .................... SUCCESS [  2.370 s]
     [INFO] Web Coverage Service 2.0 Module .................... SUCCESS [  0.970 s]
     [INFO] Web Feature Service Module ......................... SUCCESS [  3.658 s]
     [INFO] Web Map Service Module ............................. SUCCESS [  1.899 s]
     [INFO] KML support for GeoServer .......................... SUCCESS [  0.757 s]
     [INFO] gs-rest ............................................ SUCCESS [  1.977 s]
     [INFO] GeoWebCache (GWC) Module ........................... SUCCESS [  1.059 s]
     [INFO] gs-restconfig ...................................... SUCCESS [  1.448 s]
     [INFO] gs-restconfig-wcs .................................. SUCCESS [  0.218 s]
     [INFO] gs-restconfig-wfs .................................. SUCCESS [  0.229 s]
     [INFO] gs-restconfig-wms .................................. SUCCESS [  0.333 s]
     [INFO] GeoServer Web Modules .............................. SUCCESS [  0.055 s]
     [INFO] Core UI Module ..................................... SUCCESS [  6.905 s]
     [INFO] WMS UI Module ...................................... SUCCESS [  0.884 s]
     [INFO] GWC UI Module ...................................... SUCCESS [  1.086 s]
     [INFO] WFS UI Module ...................................... SUCCESS [  0.351 s]
     [INFO] Demos Module ....................................... SUCCESS [  0.744 s]
     [INFO] WCS UI Module ...................................... SUCCESS [  0.261 s]
     [INFO] Security UI Modules ................................ SUCCESS [  0.115 s]
     [INFO] Security UI Core Module ............................ SUCCESS [  1.241 s]
     [INFO] Security UI JDBC Module ............................ SUCCESS [  0.239 s]
     [INFO] Security UI LDAP Module ............................ SUCCESS [  0.209 s]
     [INFO] REST UI Module ..................................... SUCCESS [  0.204 s]
     [INFO] GeoServer Web Application .......................... SUCCESS [  3.266 s]
     [INFO] Community Space .................................... SUCCESS [  0.054 s]
     [INFO] GeoServer Extensions 2.15-SNAPSHOT ................. SUCCESS [  0.055 s]
     [INFO] ------------------------------------------------------------------------
     [INFO] BUILD SUCCESS
     [INFO] ------------------------------------------------------------------------
     [INFO] Total time: 26.451 s (Wall Clock)
     [INFO] Finished at: 2018-08-27T15:52:42+03:00
     [INFO] ------------------------------------------------------------------------

#. Navigate to the web-app folder::
   
     cd web/app

#. Run GeoServer::
     
     mvn jetty:run 
     
#. Use the browser to open:
   
   http://localhost:8080/geoserver/