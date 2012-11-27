.. _rest_api_details:

API details
===========

This page contains information on the REST API architecture.

Authentication
--------------

Requests that modify resources (POST, PUT, and DELETE operations) require the client to be authenticated. By default, method of authentication used is Basic authentication. See the :ref:`security` section for how to change the authentication method.

Status codes
------------

An HTTP request uses a status code to relay the outcome of the request to the client. Different status codes are used for various purposes through out this document. These codes are described in detail by the `HTTP specification <http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html>`_.

The most common status codes are listed below, along with their descriptions:

.. list-table::
   :header-rows: 1

   * - Status code
     - Description
     - Notes
   * - 200
     - OK
     - The request was successful
   * - 201
     - Created
     - A new resource was successfully created, such as a new feature type or data store
   * - 403
     - Forbidden
     - Often denotes a permissions mismatch
   * - 404
     - Not Found
     - Endpoint or resource was not at the indicated location
   * - 405
     - Method Not Allowed
     - Often denotes an endpoint accessed with an incorrect operation (for example, a GET request where a PUT/POST is indicated)
   * - 500
     - Internal Server Error
     - Often denotes a syntax error in the request

Formats and representations
---------------------------

A ``format`` specifies how a particular resource should be represented. A format is used:

* In an operation to specify what representation should be returned to the client
* In a POST or PUT operation to specify the representation being sent to the server

In a **GET** operation the format can be specified in two ways.

There are two ways to specify the format for a GET operation. The first option uses the ``Accept`` header. For example, with the header set to ``"Accept: text/xml"`` the resource would be returned as XML. The second option of setting the format is via a file extension. For example, given a resource ``foo``, to request a representation of ``foo`` as XML, the request URI would end with ``/foo.xml``. To request a representation as JSON, the request URI would end with ``/foo.json``. When no format is specified the server will use its own internal format, usually HTML.

In a **POST** or **PUT** operation the format specifies both the representation of the content sent to the server, and the representation of the response returned. The representation of content being sent to the server is specified with the ``Content-type`` header. For example, to send a representation in XML, use ``"Content-type: text/xml"`` or ``"Content-type: application/xml"``. The representation of content being sent to the server is specified with the ``Accept`` header as with the GET request.

The following table defines the ``Content-type`` values for each format: 

.. list-table::
   :header-rows: 1

   * - Format
     - Content-type
   * - XML
     - ``application/xml``
   * - JSON
     - ``application/json``
   * - HTML
     - ``application/html``
   * - SLD
     - ``application/vnd.ogc.sld+xml``
