.. _community_geofence_server:

Geofence Internal Server
========================
This plugin runs a `GeoFence <https://github.com/geoserver/geofence/>`_ server integrated internally in GeoServer. 
Geofence allows far more advanced security configurations than the default GeoServer :ref:`security` substem, such as rules that combine data and service restrictions.

In the integrated version, the users and roles service configured in geoserver are associated with the geofence rule database. At this time this community plugin does not provide all configuration possibilities yet that the stand-alone geofence provides. 
The integrated geofence server can be configured using its WebGUI page or REST configuration.

.. toctree::
   :maxdepth: 2

   installing
   gui
   rest
   rest-userrole
   tutorial
   migration
