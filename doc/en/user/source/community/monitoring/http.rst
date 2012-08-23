.. _monitor_http_api:

Monitor HTTP API
================

The monitor extension provides an API for retrieving request information via a
simple set of HTTP calls.

Examples
--------

All requests as HTML 
^^^^^^^^^^^^^^^^^^^^

The most simple of all calls would be to retrieve information about all requests::

  GET http://localhost:8080/geoserver/rest/monitor/requests.html

All requests as CSV
^^^^^^^^^^^^^^^^^^^
Request information can be returned in CSV format, for easier post-processing::

  GET http://localhost:8080/geoserver/rest/monitor/requests.csv

Request bodies containing newlines are handled with quoted text.  If your CSV reader doesn't handle quoted newlines, it will not work correctly.

All requests as PKZip
^^^^^^^^^^^^^^^^^^^^^
A PKZip archive containing the CSV file above, with all the request bodies and errors as separate files::

  GET http://localhost:8080/geoserver/rest/monitor/requests.zip

All requests as MS Excel
^^^^^^^^^^^^^^^^^^^^^^^^
A Microsoft Excel spreadsheet containing the same information as the CSV file::

  GET http://localhost:8080/geoserver/rest/monitor/requests.xls


Requests during a time period
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
Requests can be filtered by date and time range::

  GET http://localhost:8080/geoserver/rest/monitor/requests.html?from=2010-06-20&to=2010-07-20
  GET http://localhost:8080/geoserver/rest/monitor/requests.html?from=2010-06-20T2:00:00&to=2010-06-20T16:00:00

Request set paging
^^^^^^^^^^^^^^^^^^
Large result sets can be paged over multiple queries::
  
  GET http://localhost:8080/geoserver/rest/monitor/requests.html?count=100
  GET http://localhost:8080/geoserver/rest/monitor/requests.html?count=100&offset=100
  GET http://localhost:8080/geoserver/rest/monitor/requests.html?count=100&offset=200
  GET http://localhost:8080/geoserver/rest/monitor/requests.html?count=100&offset=300
  
Single request
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
An individual request can be retrieved by specifying its ID::

  GET http://<host>:<port>/geoserver/rest/monitor/requests.<format>

Where ``format`` is the representation of the returned result and is one of:

 * html - Representation as an HTML table.
 * csv - Representation as a Comma Separated Value table.

A query for a single request has the structure::

  GET http://<host>:<port>/geoserver/rest/monitor/requests/<id>.<format>

where ``id`` is the numeric identifier of a single request,
and ``format`` specifies the representation of the returned result as one of:

* ``html`` - an HTML table.
* ``csv`` - a Comma Separated Values table.
* ``zip`` - PKZip archive containing CSV as above, plus plain text of errors and request body.
* ``xls`` - Microsoft Excel spreadsheet.

.. note::

   An alternative to specifying the returned representation with the 
   ``format`` extension is to use the http ``Accept`` header and specify 
   the MIME type as one of:
   
    * ``text/html``
    * ``application/csv``
    * ``application/zip``
    * ``application/vnd.ms-excel``

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
     - requests.html?order=startTime;DESC
   * - 
     - requests.html?order=totalTime;ASC


