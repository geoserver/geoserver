.. _rest_api_services:

OWS Services
============

GeoServer includes several types of OGC services like WCS, WFS and WMS, commonly referred to as "OWS" services. These services can be global for the whole GeoServer instance or local to a particular workspace. In this last case, they are called :ref:`virtual services <virtual_services>`.

``/services/wcs/settings[.<format>]``
-------------------------------------

Controls Web Coverage Service settings.

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status code
     - Formats
     - Default Format
   * - GET
     - Return global WCS settings
     - 200
     - XML, JSON
     - HTML
   * - POST
     -
     - 405
     - 
     - 
   * - PUT
     - Modify global WCS settings
     - 200
     - 
     - 
   * - DELETE
     -
     - 405
     - 
     - 


``/services/wcs/workspaces/<ws>/settings[.<format>]``
------------------------------------------------------

Controls Web Coverage Service settings for a given workspace.

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status code
     - Formats
     - Default Format
   * - GET
     - Return WCS settings for workspace ``ws``
     - 200
     - HTML, XML, JSON
     - HTML
   * - POST
     - 
     - 405
     -
     -
   * - PUT
     - Create or modify WCS settings for workspace ``ws``
     - 200
     - XML,JSON
     - 
   * - DELETE
     - Delete WCS settings for workspace ``ws``
     - 200
     -
     -


``/services/wfs/settings[.<format>]``
-------------------------------------

Controls Web Feature Service settings.

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status code
     - Formats
     - Default Format
   * - GET
     - Return global WFS settings
     - 200
     - HTML, XML, JSON
     - HTML
   * - POST
     - 
     - 405
     -
     -
   * - PUT
     - Modify global WFS settings
     - 200
     - XML,JSON
     - 
   * - DELETE
     - 
     - 405
     -
     -


``/services/wfs/workspaces/<ws>/settings[.<format>]``
------------------------------------------------------

Controls Web Feature Service settings for a given workspace.

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status code
     - Formats
     - Default Format
   * - GET
     - Return WFS settings for workspace ``ws``
     - 200
     - HTML, XML, JSON
     - HTML
   * - POST
     - 
     - 405
     -
     -
   * - PUT
     - Modify WFS settings for workspace ``ws``
     - 200
     - XML,JSON
     - 
   * - DELETE
     - Delete WFS settings for workspace ``ws``
     - 200
     -
     -


``/services/wms/settings[.<format>]``
-------------------------------------

Controls Web Map Service settings.


.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status code
     - Formats
     - Default Format
   * - GET
     - Return global WMS settings
     - 200
     - HTML, XML, JSON
     - HTML
   * - POST
     - 
     - 405
     -
     -
   * - PUT
     - Modify global WMS settings
     - 200
     - XML,JSON
     - 
   * - DELETE
     - 
     - 405
     -
     -


``/services/wms/workspaces/<ws>/settings[.<format>]``
------------------------------------------------------

Controls Web Map Service settings for a given workspace.

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status code
     - Formats
     - Default Format
   * - GET
     - Return WMS settings for workspace ``ws``
     - 200
     - HTML, XML, JSON
     - HTML
   * - POST
     - 
     - 405
     -
     -
   * - PUT
     - Modify WMS settings for workspace ``ws``
     - 200
     - XML,JSON
     - 
   * - DELETE
     - Delete WMS settings for workspace ``ws``
     - 200
     -
     -

``/services/wmts/settings[.<format>]``
--------------------------------------

Controls Web Map Tile Service settings.


.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status code
     - Formats
     - Default Format
   * - GET
     - Return global WMTS settings
     - 200
     - HTML, XML, JSON
     - HTML
   * - POST
     - 
     - 405
     -
     -
   * - PUT
     - Modify global WMTS settings
     - 200
     - XML,JSON
     - 
   * - DELETE
     - 
     - 405
     -
     -


``/services/wmts/workspaces/<ws>/settings[.<format>]``
-------------------------------------------------------

Controls Web Map Tile Service settings for a given workspace.

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status code
     - Formats
     - Default Format
   * - GET
     - Return WMTS settings for workspace ``ws``
     - 200
     - HTML, XML, JSON
     - HTML
   * - POST
     - 
     - 405
     -
     -
   * - PUT
     - Modify WMTS settings for workspace ``ws``
     - 200
     - XML,JSON
     - 
   * - DELETE
     - Delete WMTS settings for workspace ``ws``
     - 200
     -
     -

.. todo:: WPS?
