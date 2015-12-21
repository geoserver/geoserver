.. _production_container:

Container Considerations
========================

Java web containers such as `Tomcat <http://tomcat.apache.org>`_ or `Jetty <http://www.mortbay.org/jetty/>`_ ship with configurations that allow for fast startup, but don't always deliver the best performance.

Optimize your JVM
-----------------

Set the following performance settings in the Java virtual machine (JVM) for your container.  These settings are not specific to any container.

.. list-table::
   :widths: 20 80

   * - **Option**
     - **Description**
   * - ``-server``
     - Enables the server Java Virtual Machine (JVM), which compiles bytecode much earlier and with stronger optimizations.  Startup and initial calls will be slower due to "just-in-time" (JIT) compilation taking longer, but subsequent calls will be faster.
   * - ``-Xmx256M -Xms48m``
     - Allocates extra memory to your server.  By default, JVM will use only 64MB of heap. If you're serving just vector data, you'll be streaming, so having more memory won't increase performance.  If you're serving coverages, however, JAI will use a disk cache. ``-Xmx256M`` allocates 256MB of memory to GeoServer (use more if you have excess memory).  It is also a good idea to configure the JAI tile cache size (see the Server Config page in the :ref:`web_admin` section) so that it uses 75% of the heap (0.75). ``-Xms48m`` will tell the virtual machine to grab a 48MB heap on startup, which will make heap management more stable during heavy load serving.
   * - ``-XX:SoftRefLRUPolicyMSPerMB=36000``
     - Increases the lifetime of "soft references" in GeoServer.  GeoServer uses soft references to cache datastore references and other similar requests.  Making them live longer will increase the effectiveness of the cache.
   * - ``-XX:+UseParallelGC``
     - Enables the throughput garbage collector.

For more information about JVM configuration, see the article `Performance tuning garbage collection in Java <http://www.petefreitag.com/articles/gctuning/>`_.
