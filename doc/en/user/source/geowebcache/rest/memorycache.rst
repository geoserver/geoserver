.. _gwc_rest_memorycache:

In-memory tile cache
====================

The GeoWebCache REST API allows to view and replace the settings of the :ref:`in-memory tile cache <gwc_webadmin_defaults_memorycache>`, the same ones shown on the **Caching Defaults** page.

Operations
----------

URL: ``/gwc/rest/memorycache.<format>``

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Return Code
     - Formats
   * - GET
     - Return the saved settings
     - 200
     - XML, JSON
   * - POST
     - 
     - 405
     - 
   * - PUT
     - Replace the settings
     - 200, 400
     - XML, JSON
   * - DELETE
     - 
     - 405
     - 

GET returns the settings as saved: a value missing from the response takes its default.

PUT replaces all the settings, **a value left out goes back to its default**, it does not keep its current value. Changing the settings empties the cache, while sending the same settings again leaves it as it is. Settings the cache could not start with, a ``timeToLive`` below 1 or a ``backend`` class that is not installed, are rejected with a 400 and not saved.

The ``backend`` element takes the same ``class`` and settings as in ``geowebcache.xml``, for example ``hazelcastClientKeyValueStore`` for the client backend of the :ref:`GWC Hazelcast tile cache plugin <community_gwc_hazelcast>` when it is installed. Saving does not connect to the backend: it is reached the next time the cache is used, and an unreachable one is then reported in the log.

As for a change on the **Caching Defaults** page, the other nodes of a cluster only see the new settings after a reload or a restart.

The examples below use the `cURL <http://curl.haxx.se/>`_ tool, though the examples apply to any HTTP-capable tool or library.

Retrieving the current settings
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. code-block:: console

   curl -u admin:geoserver http://localhost:8080/geoserver/gwc/rest/memorycache.xml

Replacing the settings
~~~~~~~~~~~~~~~~~~~~~~

Given a ``memorycache.xml`` file as the following:

.. code-block:: xml

   <memoryCache>
     <enabled>true</enabled>
     <timeToLive>120</timeToLive>
     <backend class="heapKeyValueStore">
       <maxSizeMB>256</maxSizeMB>
     </backend>
   </memoryCache>

.. code-block:: console

   curl -u admin:geoserver -XPUT -H "Content-type: text/xml" -d @memorycache.xml http://localhost:8080/geoserver/gwc/rest/memorycache.xml

This turns the cache on, with tiles kept 120 seconds in up to 256 MB of heap on each node, and ``passthrough`` and ``statsInterval`` back to their defaults.
