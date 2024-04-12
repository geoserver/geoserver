.. _rest_api_urlchecks:

URL Checks
==========

An ``URL External Access Check`` is the check performed on user provided URLs that GeoServer will use to access remote resources.

``/urlchecks[.<format>]``
--------------------------

Returns all url checks.

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status code
     - Formats
     - Default Format
   * - GET
     - List all url checks
     - 200
     - HTML, XML, JSON
     - JSON
   * - POST
     - Create a new url check
     - 201 with ``Location`` header
     - XML, JSON
     -
   * - PUT
     -
     - 405
     -
     -
   * - DELETE
     -
     - 405
     -
     -

``/urlchecks/<uc>[.<format>]``
-------------------------------

Returns a specific url check.

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status code
     - Formats
     - Default Format
     - Parameters
   * - GET
     - Return url check ``uc``
     - 200
     - HTML, XML, JSON
     - JSON
     -
   * - POST
     -
     - 405
     -
     -
     -
   * - PUT
     - 200
     - Modify url check ``uc``
     - XML, JSON
     -
     -
   * - DELETE
     - 200
     - Delete url check ``uc``
     - XML, JSON
     -
     -

Exceptions
~~~~~~~~~~

.. list-table::
   :header-rows: 1

   * - Exception
     - Status code
   * - POST or PUT for a url check missing required fields
     - 401
   * - GET or DELETE for a url check that does not exist
     - 404
   * - POST for a url check that already exists
     - 409
