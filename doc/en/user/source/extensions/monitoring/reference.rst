.. _monitor_reference:

Data Reference
==============

The following is a list of all the attributes of a request that are captured by 
the monitor extension.

General
-------

.. list-table::
   :widths: 20 65 15
   :header-rows: 1

   * - Attribute
     - Description
     - Type
   * - ID
     - Numeric identifier of the request. Every request is assigned an identifier upon 
       its creation.
     - Numeric
   * - Status
     - Status of the request. See :ref:`notes <status>` below.
     - String
   * - Category
     - The type of request being made, for example an OGC service request, a REST call, etc... 
       See :ref:`notes <category>` below.
     - String
   * - Start time
     - The time of the start of the request.
     - Timestamp
   * - End time
     - The time of the completion of the request.
     - Timestamp
   * - Total time
     - The total time spent handling the request, measured in milliseconds, equal to 
       the end time - start time.
     - Numeric
   * - Error message
     - The exception message if the request failed or resulted in an error.
     - String
   * - Error
     - The raw exception if the message failed or resulted in an error.
     - Text blob


.. _status:

Status
^^^^^^

The status of a request changes over it's life cycle and may have one of the 
following values:


* ``WAITING`` - The request has been received by the server, but is queued and not yet 
  being actively handled.
* ``RUNNING`` - The request is in the process of being handled by the server.
* ``FINISHED`` - The request has been completed and finished normally.
* ``FAILED`` - The request has been completed but resulted in an error.
* ``CANCELLED`` - The request was cancelled before it could complete.
* ``INTERRUPTED`` - The request was interrupted before it could complete.
 
.. _category:

Category
^^^^^^^^

Requests are grouped into categories that describe the nature or type of the request. The 
following are the list of all categories:

* ``OWS`` - The request is an OGC service request.
* ``REST`` - The request is a REST service request.
* ``OTHER`` - All other requests.

HTTP
----

The following attributes are all HTTP related.

.. list-table::
   :widths: 20 65 15
   :header-rows: 1
   
   * - Attribute
     - Description
     - Type
   * - HTTP method
     - The HTTP method, one of ``GET``, ``POST``, ``PUT``, or ``DELETE``
     - String
   * - Remote address
     - The IP address of the client from which the request originated.
     - String
   * - Remote host
     - The hostname corresponding to the remote address, obtained via reverse DNS lookup.
     - String
   * - Host
     - The hostname of the server handling the request, from the point of view of the client. 
     - String
   * - Internal host
     - The hostname of the server handling request, from the point of view of the local network.
       Availability depends on host and network configuration.
     - String
   * - Path
     - The path component of the request URL, for example: "/wms", "/rest/workspaces.xml", etc...
     - String
   * - Query string
     - The query string component of the request URL. Typically only present when the HTTP method is GET.
     - String
   * - Body
     - The body content of the request. Typically only present when the HTTP method is PUT or POST.
     - Binary blob
   * - Body content length
     - The total number of bytes comprising the body of the request. Typically only present when the
       HTTP method is PUT or POST.
     - Numeric
   * - Body content type
     - The mime type of the body content of the request, for example: "application/json", 
       "text/xml; subtype=gml/3.2", etc... Typically only present when the HTTP method is PUT or POST.
     - String
   * - Response status
     - The HTTP response code, for example: 200, 401, etc...
     - Numeric
   * - Response length
     - The total number of bytes comprising the response to the request.
     - Numeric
   * - Response content type
     - The mime type of the response to the request.
     - String
   * - Remote user
     - The username specified parsed of the request. Only available when request included credentials 
       for authentication.
     - String
   * - Remote user agent
     - The value of the ``User-Agent`` HTTP header.
     - String
   * - Http referrer
     - The value of the ``Referer`` HTTP header. 
     - String

OWS/OGC 
-------

The following attributes are OGC service specific.

.. list-table::
   :widths: 20 65 15
   :header-rows: 1

   * - Attribute
     - Description
     - Type
   * - Service
     - The OGC service identifier, for example: "WMS", "WFS", etc...
     - String
   * - Operation
     - The OGC operation name, for example: "GetMap", "GetFeature", etc...
     - String
   * - Sub operation
     - The ogc sub operation (if it applies). For instance when the operation is a WFS Transaction
       the sub operation may be one of "Insert", "Update", etc...
     - String
   * - OWS/OGC Version
     - The OGC service version, for example with WFS the version may be "1.0.0", "1.1.0", etc...
     - String
   * - Resources
     - Names of resources (layers, processes, etc...) specified as part of the request.
     - List of String
   * - Bounding box
     - The bounding box specified as part of the request. In some cases this is not possible to 
       obtain this reliable, an example being a complex WFS query with a nested "BBOX" filter.
     - List of Numeric


GeoIP
-----

The following attributes are specific to GeoIP look ups and are not captured out of the box. See 
:ref:`monitor_geoip` for more details.

.. list-table::
   :widths: 20 65 15
   :header-rows: 1

   * - Attribute
     - Description
     - Type
   * - Remote country
     - Name of the country of the client from which the request originated.
     - String
   * - Remote city
     - Name of the city from which the request originated.
     - String
   * - Remote lat
     - The latitude from which the request originated.
     - Numeric
   * - Remote lon
     - The longitude from which the request originated.
     - Numeric
   
GWC 
---

The following attributes are specific to tile cached requests.

.. list-table::
:widths: 20 65 15
   :header-rows: 1

      * - Attribute
        - Description
        - Type
      * - CacheResult
        - "HIT" or "MISS" (can be empty if GWC was not involved)
        - String
      * - MissReason
        - A description of why the cache was not used. Available only on requests hitting a cached layer on direct WMS integration,
          applies to cases where the request was not forwarded to GWC, for example "no parameter filter exists for FEATUREID",
          "request does not align to grid(s) "EPSG:4326" or "not a tile layer". Will be missing for
          any request not hitting the direct integration (e.g., direct WMTS requests, for example)
        - String
