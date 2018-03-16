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

Rule syntax reference
---------------------

The current implementation of the control flow module reads its rules from a ``controlflow.properties`` property file located in the :ref:`datadir`.

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

Using a header implies some other system is involved in the priority management. This is particulary good when using
a load balancer, as the requests priorities need to be evenly split across cluster elements, control-flow only
has visibility of a single instance. As an example, the priority will be de-facto ignored at the cluster level
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

Where ``<count>`` is the maximum number of requests the ip speficied in ``<ip_addr>`` will execute in parallel.

To reject requests from a list of ip addresses::

  ip.blacklist=<ip_addr1>,<ip_addr2>,...
  
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

Assuming the server we want to protect has 4 cores a sample configuration could be::

  # if a request waits in queue for more than 60 seconds it's not worth executing,
  # the client will  likely have given up by then
  timeout=60
  # don't allow the execution of more than 100 requests total in parallel
  ows.global=100
  # don't allow more than 10 GetMap in parallel
  ows.wms.getmap=10
  # don't allow more than 4 outputs with Excel output as it's memory bound
  ows.wfs.getfeature.application/msexcel=4
  # don't allow a single user to perform more than 6 requests in parallel
  # (6 being the Firefox default concurrency level at the time of writing)
  user=6
  # don't allow the execution of more than 16 tile requests in parallel
  # (assuming a server with 4 cores, GWC empirical tests show that throughput
  # peaks up at 4 x number of cores. Adjust as appropriate to your system)
  ows.gwc=16



