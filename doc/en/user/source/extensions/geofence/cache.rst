.. _geofence_cache:

GeoFence Cache REST
===================

The Geofence client cache status can be queried, and the cache cleared (invalidated) through a REST service.

Requests
--------

``/geofence/ruleCache/info``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Retrieve information about the geofence cache status.

.. list-table::
   :header-rows: 1
   :widths: 10 20 20 20

   * - Method
     - Action
     - Parameters
     - Response
   * - GET
     - Retrieve information about the geofence cache status. Per cache (rules, admin rules and users) we retrieve the cache size, hits, misses, load successes, load failures, load times and evictions.
     - ---
     - 200 OK. Text Format.


``/geofence/ruleCache/invalidate``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Invalidate the geofence cache.

.. list-table::
   :header-rows: 1
   :widths: 10 20 20 20

   * - Method
     - Action
     - Parameters
     - Response
   * - PUT
     - Invalidate (clear) the geofence cache
     - ---
     - 200 OK.


