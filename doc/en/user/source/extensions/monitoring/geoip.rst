.. _monitor_geoip:

GeoIP
=====

The monitor extension has the capability to integrate with the 
`MaxMind GeoIP <http://www.maxmind.com/en/geolocation_landing>`_ database 
in order to provide geolocation information about the origin of a request. 
This functionality is not enabled by default. 

.. note::

   At this time only the freely available GeoLite City database is supported. 
   
Enabling GeoIP Lookup
---------------------

In order to enable the GeoIP lookup capabilities

#. Download the `GeoLite City <http://dev.maxmind.com/geoip/geolite>`_ database.
#. Uncompress the file and copy :file:`GeoLiteCity.dat` to the :file:`monitoring`
   directory.
#. Restart GeoServer.










