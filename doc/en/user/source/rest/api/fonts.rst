.. _rest_api_fonts:

Fonts 
=====

This operation provides the list of ``fonts`` available in GeoServer. It can be useful to use this operation to verify if a ``font`` used in a SLD file is available before uploading the SLD.

``/fonts[.<format>]``
---------------------

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status code
     - Formats
     - Default Format
   * - GET
     - Return the fonts available in GeoServer
     - 200
     - XML, JSON
     - XML
   * - POST
     -
     - 405
     - 
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

