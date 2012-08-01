.. _control_flow:

Control flow module
===================

The ``control-flow`` module for GeoServer allows the administrator to control the amount of concurrent requests actually executing inside the server.
This kind of control is useful for a number of reasons:

*  *Performance*: tests show that, with local data sources, the maximum throughput in `GetMap` requests is achieved when allowing at most 2 times the number of CPU cores requests to run in parallel.
*  *Resource control*: requests such as `GetMap` can use a significant amount of memory. The :ref:`WMS request limits<wms_configuration_limits>` allow to control the amount of memory used per request, but an ``OutOfMemoryError`` is still possible if too many requests run in parallel. By controlling also the amount of requests executing it's possible to limit the total amount of memory used below the memory that was actually given to the Java Virtual Machine.
*  *Fairness*: a single user should not be able to overwhelm the server with a lot of requests, leaving other users with tiny slices of the overall processing power.

The control flow method does not normally reject requests, it just queues up those in excess and executes them late. However, it's possible to configure the module to reject requests that have been waited in queue for too long.

Rule syntax reference
---------------------

The current implementation of the control flow module reads its rules from a ``controlflow.properties`` property file located in the :ref:`GeoServer data directory <data_directory>`.

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

Per user control
................

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

Timeout
.......

A request timeout is specified with the following syntax::

   timeout=<seconds>

where ``<seconds>`` is the number of seconds a request can stay queued waiting for execution. If the request does not enter execution before the timeout expires it will be rejected.

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



