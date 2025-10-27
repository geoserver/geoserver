.. _control_flow:

Control flow module
===================

The ``control-flow`` module for GeoServer allows the administrator to control the amount of concurrent requests actually executing inside the server,
as well as giving an opportunity to slow down users making too many requests.
This kind of control is useful for a number of reasons:

*  *Performance*: tests show that, with local data sources, the maximum throughput in `GetMap` requests is achieved when allowing at most 2 times the number of CPU cores requests to run in parallel.
*  *Resource control*: requests such as `GetMap` can use a significant amount of memory. The :ref:`WMS request limits<wms_configuration_limits>` allow to control the amount of memory used per request, but an ``OutOfMemoryError`` is still possible if too many requests run in parallel. By controlling also the amount of requests executing it's possible to limit the total amount of memory used below the memory that was actually given to the Java Virtual Machine.
*  *Fairness*: a single user should not be able to overwhelm the server with a lot of requests, leaving other users with tiny slices of the overall processing power.

The control flow method does not normally reject requests, it just queues up those in excess and executes them late. However, it's possible to configure the module to reject requests that have been waited in queue for too long.

Installation
------------

#. Login, and navigate to :menuselection:`About & Status > About GeoServer` and check **Build Information**
   to determine the exact version of GeoServer you are running.

#. Visit the :website:`website download <download>` page, change the **Archive** tab,
   and locate your release.
   
   From the list of **Miscellaneous** extensions download **Control Flow**.

   * |release| example: :download_extension:`control-flow`
   * |version| example: :nightly_extension:`control-flow`

   Verify that the version number in the filename corresponds to the version of GeoServer you are running (for example |release| above).

#. Extract the files in this archive to the :file:`WEB-INF/lib` directory of your GeoServer installation.

#. Restart GeoServer
 
Rule syntax reference
---------------------

The current implementation of the control flow module reads its rules from a :file:`controlflow.properties` property file located in the :ref:`datadir`.

Total OWS request count
.......................

The global number of OWS requests executing in parallel can be specified with::

   ows.global=<count>

Every request in excess will be queued and executed when other requests complete leaving some free execution slot.

Per request control
...................

A per request type control can be demanded using the following syntax::

   ows.<service>[.<request>[.<outputFormat>]]=<count>

Where:

* ``<service>`` is the OWS service in question (at the time of writing can be ``wms``, ``wfs``, ``wcs``)
* ``<request>``, optional, is the request type. For example, for the ``wms`` service it can be ``GetMap``, ``GetFeatureInfo``, ``DescribeLayer``, ``GetLegendGraphics``, ``GetCapabilities``
* ``<outputFormat>``, optional, is the output format of the request. For example, for the ``wms`` ``GetMap`` request it could be ``image/png``, ``image/gif`` and so on

A few examples::

  # don't allow more than 16 WCS requests in parallel
  ows.wcs=16
  # don't allow more than 8 GetMap requests in parallel
  ows.wms.getmap=8
  # don't allow more than 2 WFS GetFeature requests with Excel output format
  ows.wfs.getfeature.application/msexcel=2
  
Request priority support
........................

Requests controlled by "ows.*" controllers above can be also executed in priority order, in case there are too many
the request will block and wait, and will we awoken in priority order (highest to lowest).

Currently the only way to specific a priority for a request is to add it to a request HTTP header::

  ows.priority.http=<headerName>,<defaultPriority>
  
The header "headerName" will contain a number defining the priority for the request, the default priority is used
as a fallback if/when the header is not found.

Using a header implies some other system is involved in the priority management. This is particularly good when using
a load balancer, as the requests priorities need to be evenly split across cluster elements, control-flow only
has visibility of a single instance. As an example, the priority will be de facto ignored at the cluster level
if there are two nodes, and for whatever chance or design, the high priority requests end up converging on the same cluster node.

Per user concurrency control
............................

There are two mechanisms to identify user requests. The first one is cookie based, so it will work fine for browsers but not as much for other kinds of clients. The second one is ip based, which works for any type of client but that can limit all the users sitting behind the same router

This avoids a single user (as identified by a cookie) to make too many requests in parallel::

  user=<count>

Where ``<count>`` is the maximum number of requests a single user can execute in parallel.


The following avoids a single ip address from making too many requests in parallel::

  ip=<count>

Where ``<count>`` is the maximum number of requests a single ip address can execute in parallel.

It is also possible to make this a bit more specific and throttle a single ip address instead by using the following::

  ip.<ip_addr>=<count>

Where ``<count>`` is the maximum number of requests the ip specified in ``<ip_addr>`` will execute in parallel.

To reject requests from a list of ip addresses::

  ip.blacklist=<ip_addr1>,<ip_addr2>,...

When a count is set to limit parallel requests, the HTTP response will include a header informing the user of the limit::

    X-Concurrent-Limit-<ctx>: 10
    X-Concurrent-Requests-<ctx>: 9

where ``<ctx>`` can be either ``user`` or ``ip`` depending on the rule that triggered the limit.

Per user rate control
.....................

The rate control rules allow to setup the maximum number of requests per unit of time, based either
on a cookie or IP address. These rules look as follows (see "Per user concurrency control" for the meaning of "user" and "ip")::

  user.ows[.<service>[.<request>[.<outputFormat>]]]=<requests>/<unit>[;<delay>s]
  ip.ows[.<service>[.<request>[.<outputFormat>]]]=<requests>/<unit>[;<delay>s]
  
Where:

* ``<service>`` is the OWS service in question (at the time of writing can be ``wms``, ``wfs``, ``wcs``)
* ``<request>``, optional, is the request type. For example, for the ``wms`` service it can be ``GetMap``, ``GetFeatureInfo``, ``DescribeLayer``, ``GetLegendGraphics``, ``GetCapabilities``
* ``<outputFormat>``, optional, is the output format of the request. For example, for the ``wms`` ``GetMap`` request it could be ``image/png``, ``image/gif`` and so on
* ``<requests>`` is the number of requests in the unit of time
*  ``<unit>`` is the unit of time, can be "s", "m", "h", "d" (second, minute, hour and day respectively).
*  ``<delay>`` is an optional the delay applied to the requests that exceed the maximum number of requests in the current time slot. If not specified, once the limit is exceeded a immediate failure response with HTTP code 429 ("Too many requests") will be sent back to the caller.

The following rule will allow 1000 WPS Execute requests a day, and delay each one in excess by 30 seconds::

   user.ows.wps.execute=1000/d;30s
   
The following rule will instead allow up to 30 GetMap requests a second, but will immediately fail any request exceeding the cap::

   user.ows.wms.getmap=30/s
   
In both cases headers informing the user of the request rate control will be added to the HTTP response. For example::

    X-Rate-Limit-Context: Any OGC request
    X-Rate-Limit-Limit: 10
    X-Rate-Limit-Remaining: 9
    X-Rate-Limit-Reset: 1103919616
    X-Rate-Limit-Action: Delay excess requests 1000ms
    
In case several rate control rules apply to a single request, a batch of headers will be added to the
response for each of them, it is thus advised to avoid adding too many of these rules in parallel

Where:

* ``X-Rate-Limit-Context`` is the type of request being subject to control
* ``X-Rate-Limit-Limit`` is the total amount of requests allowed in the control interval
* ``X-Rate-Limit-Remaining`` is the number of remaining requests allowed before the rate control kicks in
* ``X-Rate-Limit-Reset`` is the Unix epoch at which the new control interval will begin
* ``X-Rate-Limit-Action`` specifies what action is taken on requests exceeding the rate control 

Timeout
.......

A request timeout is specified with the following syntax::

   timeout=<seconds>

where ``<seconds>`` is the number of seconds a request can stay queued waiting for execution. If the request does not enter execution before the timeout expires it will be rejected.

Throttling tile requests (WMS-C, TMS, WMTS)
-------------------------------------------
GeoWebCache contributes three cached tiles services to GeoServer: WMS-C, TMS, and WMTS. It is also possible to use the
Control flow module to throttle them, by adding the following rule to the configuration file::

   ows.gwc=<count>

Where ``<count>`` is the maximum number of concurrent tile requests that will be delivered by GeoWebCache at any given time.

Note also that tile request are sensitive to the other rules (user based, ip based, timeout, etc).

A complete example
------------------

Assuming the server we want to protect has 4 cores a sample configuration could be:

.. literalinclude:: controlflow.properties
   :language: properties

Debugging control flow
----------------------

The control flow module logs its activity to the GeoServer log file, with a few logs per request
at INFO level, and more logs at FINE level, for each flow controller.
The following logging configuration file enables both levels, specifically for control-flow:

.. literalinclude:: CONTROL_FLOW_LOGGING.xml
   :language: xml

An example output, filtered on a the single thread ``http-nio-8080-exec-8`` and a single WFS 1.0.0 GetFeature request follows::

    19 018 15:18:31 'http-nio-8080-exec-8' INFO   [geoserver.flow] - Request [WFS 1.0.0 GetFeature] starting, processing through flow controllers
    19 018 15:18:31 'http-nio-8080-exec-8' DEBUG  [geoserver.flow] - Request [WFS 1.0.0 GetFeature] enter RateFlowController [wps, action=Delay excess requests 10000ms]
    19 018 15:18:31 'http-nio-8080-exec-8' DEBUG  [geoserver.flow] - Request [WFS 1.0.0 GetFeature] exit  RateFlowController [wps, action=Delay excess requests 10000ms]
    19 018 15:18:31 'http-nio-8080-exec-8' DEBUG  [geoserver.flow] - Request [WFS 1.0.0 GetFeature] enter RateFlowController [wms, action=Delay excess requests 1000ms]
    19 018 15:18:31 'http-nio-8080-exec-8' DEBUG  [geoserver.flow] - Request [WFS 1.0.0 GetFeature] exit  RateFlowController [wms, action=Delay excess requests 1000ms]
    19 018 15:18:31 'http-nio-8080-exec-8' DEBUG  [geoserver.flow] - Request [WFS 1.0.0 GetFeature] enter BasicOWSController(wcs.getcoverage,SimpleBlocker(1))/0
    19 018 15:18:31 'http-nio-8080-exec-8' DEBUG  [geoserver.flow] - Request [WFS 1.0.0 GetFeature] exit  BasicOWSController(wcs.getcoverage,SimpleBlocker(1))/0
    19 018 15:18:31 'http-nio-8080-exec-8' DEBUG  [geoserver.flow] - Request [WFS 1.0.0 GetFeature] enter IpFlowController(3)
    19 018 15:18:31 'http-nio-8080-exec-8' DEBUG  [flow.controller] - X-Forwarded-For: 185.230.235.34, 10.12.81.32 -> 185.230.235.34
    19 018 15:18:44 'http-nio-8080-exec-8' DEBUG  [flow.controller] - IpFlowController(3) 185.230.235.34, concurrent requests: 3
    19 018 15:18:44 'http-nio-8080-exec-8' DEBUG  [flow.controller] - IpFlowController(3,185.230.235.34) queue size 3
    19 018 15:18:44 'http-nio-8080-exec-8' DEBUG  [geoserver.flow] - Request [WFS 1.0.0 GetFeature] exit  IpFlowController(3)
    19 018 15:18:44 'http-nio-8080-exec-8' DEBUG  [geoserver.flow] - Request [WFS 1.0.0 GetFeature] enter BasicOWSController(wps.execute,SimpleBlocker(4))/0
    19 018 15:18:44 'http-nio-8080-exec-8' DEBUG  [geoserver.flow] - Request [WFS 1.0.0 GetFeature] exit  BasicOWSController(wps.execute,SimpleBlocker(4))/0
    19 018 15:18:44 'http-nio-8080-exec-8' DEBUG  [geoserver.flow] - Request [WFS 1.0.0 GetFeature] enter BasicOWSController(wms.getmap,SimpleBlocker(6))/0
    19 018 15:18:44 'http-nio-8080-exec-8' DEBUG  [geoserver.flow] - Request [WFS 1.0.0 GetFeature] exit  BasicOWSController(wms.getmap,SimpleBlocker(6))/0
    19 018 15:18:44 'http-nio-8080-exec-8' DEBUG  [geoserver.flow] - Request [WFS 1.0.0 GetFeature] enter BasicOWSController(wfs.getfeature,SimpleBlocker(8))/2
    19 018 15:18:44 'http-nio-8080-exec-8' DEBUG  [geoserver.flow] - Request [WFS 1.0.0 GetFeature] exit  BasicOWSController(wfs.getfeature,SimpleBlocker(8))/3
    19 018 15:18:44 'http-nio-8080-exec-8' DEBUG  [geoserver.flow] - Request [WFS 1.0.0 GetFeature] enter GlobalFlowController(SimpleBlocker(50))/2
    19 018 15:18:44 'http-nio-8080-exec-8' DEBUG  [geoserver.flow] - Request [WFS 1.0.0 GetFeature] exit  GlobalFlowController(SimpleBlocker(50))/3
    19 018 15:18:44 'http-nio-8080-exec-8' INFO   [geoserver.flow] - Request control-flow performed, running requests: 3, blocked requests: 12

In this example:

* A RateFlowController imposes a delay of 10 seconds on WPS, and 1 second on WMS requests, when the configured limit is exceeded.
* A WCS concurrency control has a limit of 1, but there are 0 WCS request executing.
* A concurrency limit by IP address is set to 3, and there are 3 requests executing from a single IP (with details on how the IP address has been extracted from the X-Forwarded-For header).
* A concurrency control on wps.execute with 4 requests is in place, with none executing.
* A concurrency control on wms.getmap with 6 requests is in place, with none executing.
* A concurrency control on wfs.getfeature with 8 requests is in place, with 3 executing.
* A global concurrency control with 50 requests is in place, with 3 executing.
* In summary, 3 requests are executing, and 12 are blocked. From this situation, they are queued on the single IP flow controller.
* Looking at the timings, this request has been delayed for about 13 seconds on the single IP flow controller.