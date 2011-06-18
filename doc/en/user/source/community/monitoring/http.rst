.. _monitor_http_api:

Monitor HTTP API
================

The monitor extension provides an API for retrieving request information via a
simple set of HTTP calls.

The most simple of all calls would be to retrieve information about all requests::

  GET http://localhost:8080/geoserver/rest/monitor/requests.html

This would return an HTML document containing information about 
all requests. The general structure of a query for a set of requests is::

  GET http://<host>:<port>/geoserver/rest/monitor/requests.<format>

Where ``format`` is the representation of the returned result and is one of:

 * html - Representation as an HTML table.
 * csv - Representation as a Comma Separated Value table.

A query for a single request has the structure::

  GET http://<host>:<port>/geoserver/rest/monitor/requests/<id>.<format>

Where ``id`` is the numeric identifier of a single request and ``format`` 
is as described above.

.. note::

   An alternative to specifying the returned representation with the 
   ``format`` extension is to use the http ``Accept`` header and specify 
   one of the MIME types:

     * text/html
     * application/csv

   See the `HTTP specification <http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html>`_
   for more information about the ``Accept`` header.


API Reference
-------------

There are numerous parameters available that can be used to filter what request
information is returned and how it is structured. This section contains a 
comprehensive list of all parameters. See the examples section for a set of 
examples of applying these parameters.

count
^^^^^

Specifies how many records should be returned.

.. list-table::
   :header-rows: 1
   :widths: 30 60

   * - Syntax
     - Example
   * - count=<integer>
     - requests.html?count=100

offset
^^^^^^

Specifies where in the result set records should be returned from.

.. list-table::
   :header-rows: 1
   :widths: 30 60

   * - Syntax
     - Example
   * - offset=<integer>
     - requests.html?count=100&offset=500

live
^^^^

Specifies that only live (currently executing) requests be returned.

.. list-table::
   :header-rows: 1
   :widths: 30 60

   * - Syntax
     - Example
   * - live=<yes|no|true|false>
     - requests.html?live=yes
  
This parameter relies on a :ref:`monitor_mode` being used that maintains real time 
request information (either **live** or **mixed**).

from
^^^^

Specifies an inclusive lower bound on the timestamp for the start of a request.

.. list-table::
   :header-rows: 1
   :widths: 30 60

   * - Syntax
     - Example
   * - from=<timestamp>
     - requests.html?from=2010-07-23T16:16:44

to
^^

Specifies an inclusive upper bound on the timestamp for the start of a request.

.. list-table::
   :header-rows: 1
   :widths: 30 60

   * - Syntax
     - Example
   * - to=<timestamp>
     - requests.html?to=2010-07-24T00:00:00

order
^^^^^

Specifies which attribute of a request to sort by.

.. list-table::
   :header-rows: 1
   :widths: 30 60

   * - Syntax
     - Example
   * - order=<attribute>[;<ASC|DESC>]
     - requests.html?order=path
   * - 
     - requests.html?order=startTime:DESC
   * - 
     - requests.html?order=totalTime:ASC


Examples
--------

All requests as HTML 
^^^^^^^^^^^^^^^^^^^^

::  
 
  GET http://localhost:8080/geoserver/rest/monitor/requests.html

All requests as CSV
^^^^^^^^^^^^^^^^^^^

::

  GET http://localhost:8080/geoserver/rest/monitor/requests.csv

Requests over a time period
^^^^^^^^^^^^^^^^^^^^^^^^^^^

::
  
  GET http://localhost:8080/geoserver/rest/monitor/requests.html?from=2010-06-20&to2010-07-20

Requests paged over multiple queries
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

::
  
  GET http://localhost:8080/geoserver/rest/monitor/requests.html?count=100
  GET http://localhost:8080/geoserver/rest/monitor/requests.html?count=100&offset=100
  GET http://localhost:8080/geoserver/rest/monitor/requests.html?count=100&offset=200
  GET http://localhost:8080/geoserver/rest/monitor/requests.html?count=100&offset=300
  

