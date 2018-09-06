.. _community_geofence:

Geofence Plugin
===============
`GeoFence <https://github.com/geoserver/geofence/>`_ offers an alternative to the GeoServer :ref:`security` subsystem of GeoServer, allowing far more advanced security configurations, such as rules that combine data and service restrictions. It uses a client-server model, and this plugin only provides the client component. It must connect either to an external Geofence server, or be used in combination with the GeoServer integrated Geofence server  :ref:`community_geofence_server`.

.. toctree::
   :maxdepth: 2

   installing
   configuration
   cache
