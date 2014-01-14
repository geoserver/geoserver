.. _rest_api_global:

Global settings
===============

Allows access to GeoServer's global settings.

``/settings[.<format>]``
------------------------

Controls all global settings.

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status code
     - Formats
     - Default Format
   * - GET
     - List all global settings
     - 200
     - HTML, XML, JSON
     - HTML
   * - POST
     - 
     - 405
     - 
     - 
   * - PUT
     - Update global settings
     - 200
     - XML, JSON
     -
   * - DELETE
     -
     - 405
     -
     -


``/settings/contact[.<format>]``
--------------------------------

Controls global contact information only.

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status code
     - Formats
     - Default Format
   * - GET
     - List global contact information
     - 200
     - HTML, XML, JSON
     - HTML
   * - POST
     - 
     - 405
     - 
     - 
   * - PUT
     - Update global contact
     - 200
     - XML, JSON
     -
   * - DELETE
     -
     - 405
     -
     -

