.. _geofence_configuration:

GeoFence Admin GUI
==================

The GeoFence Admin Page is a component of the GeoServer web interface. You can access it from the GeoServer web interface by clicking the :guilabel:`GeoFence` link, found on the left side of the screen after logging in.

.. figure:: images/configuration.png
   :align: center

General Settings
----------------
Configure the following settings here:

- Geoserver instance name: the name under which this geoserver is known by the geofence server. This useful for when you use an external geofence server with multiple geoserver servers.

- GeoServer services URL: this is how geoserver knows how to connect to the external geofence server. When using an internal geofence server, this is not configurable. For example "http://localhost:9191/geofence/remoting/RuleReader" for an external geofence server on localhost.

Options
-------

Configure the following settings here:

- Allow remote and inline layers in SLD

- Authenticated users can write

- Use GeoServer roles to get authorizations

- Comma delimited list of mutually exclusive roles for authorization

Cache
-----

Configure the following settings here:

- Size of the rule cache (amount of entries)

- Cache refresh interval (ms)

- Cache expire interval (ms)

Collected data about the cache can be retrieved here. Per cache (rules, admin rules and users) we retrieve the cache size, hits, misses, load successes, load failures, load times and evictions. The cache can be manually invalidated (cleared).
