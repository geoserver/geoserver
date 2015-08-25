.. _rest_api_stylegenerator:

Style Generator
===============

The Style Generator endpoint is used for generating default styles. Styles generated using this endpoint may be added to geoserver using the :ref:`styles <rest_api_styles>` endpoint.

``/stylegenerator[.<format>]``
------------------------------

Generates generic styles. Equivalent to ``/stylegenerator/generic[.<format>]``.

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status code
     - Formats
     - Default Format
     - Parameters
   * - GET
     - Generate a generic style.
     - 200
     - SLD
     - SLD
     -
   * - POST
     - 
     - 405
     - 
     - 
     - 
   * - PUT
     - 
     - 405
     - 
     - 
     -
   * - DELETE
     - 
     - 405
     -
     -
     - 

Other extensions (such as :ref:`css <extensions_css>`) add support for 
additional formats. 

``/stylegenerator/<type>[.<format>]``
-------------------------------------

Generates a style for a given geometry type.

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status code
     - Formats
     - Default Format
     - Parameters
   * - GET
     - Generate a style of the specified ``type``
     - 200
     - SLD
     - SLD
     - 
   * - POST
     - 
     - 405
     -
     -
     - 
   * - PUT
     - 
     - 405
     - 
     -
     - 
   * - DELETE
     - 
     - 405
     -
     -
     - 

Other extensions (such as :ref:`css <extensions_css>`) add support for 
additional formats. 

Exceptions
~~~~~~~~~~

.. list-table::
   :header-rows: 1

   * - Exception
     - Status code
   * - GET for a style ``type`` that does not exist
     - 404

Style Types
~~~~~~~~~~~

There are five valid values for ``type``, which determine the content of the generated style (not case sensitive):

.. list-table::
   :header-rows: 1

   * - Value
     - Description
   * - ``Point``
     - Style for a point layer
   * - ``Line``
     - Style for a line layer
   * - ``Polygon``
     - Style for a polygon layer
   * - ``Raster``
     - Style for a raster layer
   * - ``Generic``
     - Style for an unknown layer. Combines the behavior of the other four types.



