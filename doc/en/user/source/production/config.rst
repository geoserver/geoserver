.. _production_config:

Configuration Considerations
============================

Use production logging
----------------------

Logging may visibly affect the performance of your server. High logging levels are often necessary to track down issues, but by default you should run with low levels.  (You can switch the logging levels while GeoServer is running.)  

You can change the logging level in the :ref:`web_admin`.  You'll want to choose the :guilabel:`PRODUCTION` logging configuration.

Set a service strategy
----------------------

A service strategy is the method in which output is served to the client.  This is a balance between proper form (being absolutely sure of reporting errors with the proper OGC codes, etc) and speed (serving output as quickly as possible).  This is a decision to be made based on the function that GeoServer is providing.  You can configure the service strategy by modifying the :file:`web.xml` file of your GeoServer instance.

The possible strategies are:

.. list-table::
   :widths: 20 80

   * - **Strategy**
     - **Description**
   * - ``SPEED``
     - Serves output right away. This is the fastest strategy, but proper OGC errors are usually omitted.
   * - ``BUFFER``
     - Stores the whole result in memory, and then serves it after the output is complete.  This ensures proper OGC error reporting, but delays the response quite a bit and can exhaust memory if the response is large.
   * - ``FILE``
     - Similar to ``BUFFER``, but stores the whole result in a file instead of in memory. Slower than ``BUFFER``, but ensures there won't be memory issues.
   * - ``PARTIAL-BUFFER`` 
     - A balance between ``BUFFER`` and ``SPEED``, this strategy tries to buffer in memory a few KB of response, then serves the full output.

Personalize your server
-----------------------

This is isn't a performance consideration, but is just as important.  In order to make GeoServer as useful as possible, you should customize the server's metadata to your organization.  It may be tempting to skip some of the configuration steps, and leave in the same keywords and abstract as the sample, but this will only confuse potential users.

Suggestions:

* Fill out the WFS, WMS, and WCS Contents sections (this info will be broadcast as part of the capabilities documents)
* Serve your data with your own namespace (and provide a correct URI)
* Remove default layers (such as ``topp:states``)

Configure service limits
------------------------

Make sure clients cannot request an inordinate amount of resources from your server.

In particular:

* Set the maximum amount of features returned by each WFS GetFeature request (this can also be set on a per featuretype basis by modifying the :file:`info.xml` files directly)
* Set the WMS ``request limits`` so that no request will consume too much memory or too much time

Set security
------------

GeoServer includes support for WFS-T (transactions) by default, which lets users modify your data. If you don't want your database modified, you can turn off transactions in the the :ref:`web_admin`. Set the :guilabel:`Service Level` to ``Basic``.

If you'd like some users to be able to modify some but not all of your data, you will have to set up an external security service. An easy way to accomplish this is to run two GeoServer instances and configure them differently, and use authentication to only allow certain users to have access.

For extra security, make sure that the connection to the datastore that is open to all is through a user who has read-only permissions. This will eliminate the possibility of a SQL injection (though GeoServer is generally not vulnerable to that sort of attack).

Cache your data
---------------

Server-side caching of WMS tiles is the best way to increase performance.  In caching, pre-rendered tiles will be saved, eliminating the need for redundant WMS calls.  There are several ways to set up WMS caching for GeoServer.  GeoWebCache is the simplest method, as it comes bundled with GeoServer.  (See the section on :ref:`gwc` for more details.)  Another option is `TileCache <http://tilecache.org>`_.  You can also use a more generic caching system, such as `OSCache <http://www.opensymphony.com/oscache/>`_ (an embedded cache service) or `Squid <http://www.squid-cache.org>`_ (a web cache proxy).

Caching is also possible for WFS layers, in a very limited fashion. For DataStores that don't have a quick way to determine feature counts (i.e. shapefiles), enabling caching can prevent querying a store twice during a single request. To enable caching, set the Java system property ``org.geoserver.wfs.getfeature.cachelimit`` to a positive integer. Any data sets that are smaller than the cache limit will be cached for the duration of a request, which will prevent them from being queried a second time for the feature count. Note that this may adversely affect some types of DataStores, as it bypasses any feature count optimizations that may exist.

Disable the GeoServer web administration interface
--------------------------------------------------

In some circumstances, you might want to completely disable the web administration interface.  There are two ways of doing this:

* Set the Java system property GEOSERVER_CONSOLE_DISABLED to true by adding -DGEOSERVER_CONSOLE_DISABLED=true to your container's JVM options
* Remove all of the gs-web*-.jar files from WEB-INF/lib

X-Frame-Options Policy
----------------------

In order to prevent clickjacking attacks GeoServer defaults to setting the X-Frame-Options HTTP 
header to SAMEORIGIN. This prevents GeoServer from being embedded into an iFrame, which prevents certain
kinds of security vulnerabilities. See the `OWASP Clickjacking entry <https://www.owasp.org/index.php/Clickjacking_Defense_Cheat_Sheet>`_ for details.

If you wish to change this behavior you can do so through the following properties:

* geoserver.xframe.shouldSetPolicy: controls whether the X-Frame-Options filter should be set at all. Default is true.
* geoserver.xframe.policy: controls what the set the X-Frame-Options header to. Default is SAMEORIGIN valid options are DENY, SAMEORIGIN and ALLOW-FROM [uri]

These properties can be set either via Java system property, command line argument (-D), environment
variable or web.xml init parameter.


OWS ServiceException XML mimeType
--------------------------------------------------

By default, OWS Service Exception XML responses have content-type set to application/xml.
In case you want it set to text/xml instead, you need to setup the Java System properties:

* -Dows10.exception.xml.responsetype=text/xml for OWS 1.0.0 version
* -Dows11.exception.xml.responsetype=text/xml for OWS 1.1.0 version
