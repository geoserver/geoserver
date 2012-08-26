.. _geowebcache:

Caching with GeoWebCache
========================

.. image:: geowebcache.png

GeoWebCache is a tiling server.  It runs as a proxy between a map client and map server, caching (storing) tiles as they are requested, eliminating redundant request processing and thus saving large amounts of processing time.  GeoWebCache is integrated with GeoServer, though it is also available as a standalone product for use with other map servers.

This section will discuss the version of GeoWebCache integrated with GeoServer.  For information about the standalone product, please see the `GeoWebCache homepage <http://geowebcache.org>`_.

.. toctree::
   :maxdepth: 2

   using
   config
   seeding
   responseheaders
   rest/index
   troubleshooting
