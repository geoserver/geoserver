.. _rest_api_reset:

Resource reset 
==============

Resets all store, raster, and schema caches. This operation is used to force GeoServer to drop all caches and store connections and reconnect to each of them the next time they are needed by a request. This is useful in case the stores themselves cache some information about the data structures they manage that may have changed in the meantime.

``/reset``
----------

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status code
     - Formats
     - Default Format
   * - GET
     -
     - 405
     - 
     - 
   * - POST
     - Reset all store, raster, and schema caches
     - 200
     - 
     - 
   * - PUT
     - Reset all store, raster, and schema caches
     - 200
     - 
     - 
   * - DELETE
     -
     - 405
     -
     -

