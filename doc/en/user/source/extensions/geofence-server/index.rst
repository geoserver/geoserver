.. _community_geofence_server:

Geofence Internal Server
========================
This plugin runs a `GeoFence <https://github.com/geoserver/geofence/>`_ server integrated internally in GeoServer. 
Geofence allows far more advanced security configurations than the default GeoServer :ref:`security` subsystem, such as rules that combine data and service restrictions.

In the integrated version, the users and roles service configured in geoserver are associated with the geofence rule database. The integrated geofence server can be configured using its WebGUI page or REST configuration.

.. toctree::
   :maxdepth: 2

   installing
   gui
   rest
   rest-adminrule
   tutorial
   migration
