.. _monitor_query_api:

Monitor Query API
=================

The monitor extension provides a simple HTTP-based API for querying request information.
It allows retrieving individual request records or sets of request records, in either HTML or CSV format.
Records can be filtered by time range and the result set sorted by any field.  
Large result sets can be paged over multiple queries.

Examples
--------
The following examples show the syntax for common Monitoring queries.

All requests as HTML 
^^^^^^^^^^^^^^^^^^^^
The simplest query is to retrieve an HTML document containing information
about all requests::
 
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

  GET http://localhost:8080/geoserver/rest/monitor/requests/12345.html
  

  
API Reference
-------------

There are two kinds of query: one for single requests, and one for sets of requests. 

Single Request Query
^^^^^^^^^^^^^^^^^^^^

A query for a single request record has the structure::

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


Request Set Query
^^^^^^^^^^^^^^^^^

The structure of a query for a set of requests is::

  GET http://<host>:<port>/geoserver/rest/monitor/requests.<format>[?parameter{&parameter}]

where ``format`` is as described above, 
and ``parameter`` is one or more of the parameters listed below.

The request set query accepts various parameters 
that control what requests are returned and how they are sorted. 
The available parameters are: 

count Parameter
^^^^^^^^^^^^^^^

Specifies how many records should be returned.

.. list-table::
   :header-rows: 1
   :widths: 40 60

   * - Syntax
     - Example
   * - ``count=<integer>``
     - requests.html?count=100

offset Parameter
^^^^^^^^^^^^^^^^

Specifies where in the result set records should be returned from.

.. list-table::
   :header-rows: 1
   :widths: 40 60

   * - Syntax
     - Example
   * - ``offset=<integer>``
     - requests.html?count=100&offset=500

live Parameter
^^^^^^^^^^^^^^

Specifies that only live (currently executing) requests be returned.

.. list-table::
   :header-rows: 1
   :widths: 40 60

   * - Syntax
     - Example
   * - ``live=<yes|no|true|false>``
     - requests.html?live=yes
  
This parameter relies on a :ref:`monitor_mode` being used that maintains real time 
request information (either **live** or **mixed**).

from Parameter
^^^^^^^^^^^^^^

Specifies an inclusive lower bound on the timestamp for the start of a request.
The timestamp can be specified to any desired precision.

.. list-table::
   :header-rows: 1
   :widths: 40 60

   * - Syntax
     - Example
   * - ``from=<timestamp>``
     - requests.html?from=2010-07-23T16:16:44
   * - 
     - requests.html?from=2010-07-23

to Parameter
^^^^^^^^^^^^^

Specifies an inclusive upper bound on the timestamp for the start of a request.
The timestamp can be specified to any desired precision.

.. list-table::
   :header-rows: 1
   :widths: 40 60

   * - Syntax
     - Example
   * - ``to=<timestamp>``
     - requests.html?to=2010-07-24T00:00:00
   * - 
     - requests.html?to=2010-07-24

order Parameter
^^^^^^^^^^^^^^^

Specifies which request attribute to sort by, and optionally specifies the sort direction.

.. list-table::
   :header-rows: 1
   :widths: 40 60

   * - Syntax
     - Example
   * - ``order=<attribute>[;<ASC|DESC>]``
     - requests.html?order=path
   * - 
     - requests.html?order=startTime;DESC
   * - 
     - requests.html?order=totalTime;ASC




