.. _rest_api_logging:

Logging settings
================

Allows access to GeoServer's logging settings.

``/logging[.<format>]``
-----------------------

Controls logging settings.

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status code
     - Formats
     - Default Format
   * - GET
     - List logging settings
     - 200
     - HTML, XML, JSON
     - HTML
   * - POST
     - 
     - 405
     - 
     - 
   * - PUT
     - Update logging settings
     - 200
     - XML, JSON
     -
   * - DELETE
     -
     - 405
     -
     -