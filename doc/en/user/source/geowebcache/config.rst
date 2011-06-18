.. _gwc_config:

GeoWebCache Configuration
=========================

GeoWebCache is automatically configured to be used with GeoServer with the most common options, with **no setup required**.  All communication between GeoServer and GeoWebCache happens by passing messages inside the JVM.

By default, all layers served by GeoServer will be known to GeoWebCache.  See the :ref:`gwc_demo` page to test the configuration.

.. note::  The ``GEOSERVER_WMS_URL`` parameter in :file:`web.xml`, used in earlier versions of GeoServer, is deprecated and should not be used.

Changing the cache directory
----------------------------

GeoWebCache will automatically store cached tiles in a ``gwc`` directory inside your GeoServer data directory.  To set a different directory, stop GeoServer (if it is running) and add the following code to your GeoServer :file:`web.xml` file (located in the :file:`WEB-INF` directory):

.. code-block:: xml 

   <context-param>
      <param-name>GEOWEBCACHE_CACHE_DIR</param-name>
      <param-value>C:\temp</param-value>
   </context-param>

Change the path inside ``<param-value>`` to the desired cache path (such as :file:`C:\\temp` or :file:`/tmp`).  Restart GeoServer when done.

.. note:: Make sure GeoServer has write access in this directory.

Custom configuration
--------------------

If you need to access more features than the automatic configuration offers, you can create a custom configuration file.  Inside the GeoWebCache cache directory (see above), create a file named :file:`geowebcache.xml`.  Please refer to the `GeoWebCache documentation <http://geowebcache.org/docs>`_ for how to customize this file.  Restart GeoServer for the changes to take effect.  You may also wish to check the logfiles after starting GeoServer to verify that this file has been successfully read.

GeoWebCache with multiple GeoServer instances
---------------------------------------------

For stability reasons, it is not recommended to use the embedded GeoWebCache with multiple GeoServer instances.  If you want configure GeoWebCache as a front-end for multiple instances of GeoServer, we recommend using the `standalone GeoWebCache <http://geowebcache.org>`_.

