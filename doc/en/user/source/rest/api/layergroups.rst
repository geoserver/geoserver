.. _rest_api_layergroups:

Layer groups
============

A ``layer group`` is a grouping of layers and styles that can be accessed as a single layer in a WMS GetMap request. A layer group is sometimes referred to as a "base map".

``/layergroups[.<format>]``
---------------------------

Controls all layer groups.

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status code
     - Formats
     - Default Format
   * - GET
     - Return all layer groups
     - 200
     - HTML, XML, JSON
     - HTML
   * - POST
     - Add a new layer group
     - 201, with ``Location`` header
     - XML,JSON
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


``/layergroups/<lg>[.<format>]``
--------------------------------

Controls a particular layer group.

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status code
     - Formats
     - Default Format
   * - GET
     - Return layer group ``lg``
     - 200
     - HTML, XML, JSON
     - HTML
   * - POST
     - 
     - 405
     -
     -
   * - PUT
     - Modify layer group ``lg``
     - 200
     - XML,JSON
     - 
   * - DELETE
     - Delete layer group ``lg``
     - 200
     -
     -

Exceptions
~~~~~~~~~~

.. list-table::
   :header-rows: 1

   * - Exception
     - Status code
   * - GET for a layer group that does not exist
     - 404
   * - POST that specifies layer group with no layers
     - 400
   * - PUT that changes name of layer group
     - 403

 
``/workspaces/<ws>/layergroups[.<format>]``
-------------------------------------------

Controls all layer groups in a given workspace.
 
.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status code
     - Formats
     - Default Format
   * - GET
     - Return all layer groups within workspace ``ws``
     - 200
     - HTML, XML, JSON
     - HTML
   * - POST
     - Add a new layer group within workspace ``ws``
     - 201, with ``Location`` header
     - XML,JSON
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


``/workspaces/<ws>/layergroups/<lg>[.<format>]``
------------------------------------------------

Controls a particular layer group in a given workspace.

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status code
     - Formats
     - Default Format
   * - GET
     - Return layer group ``lg`` within workspace ``ws``
     - 200
     - HTML, XML, JSON
     - HTML
   * - POST
     - 
     - 405
     -
     -
   * - PUT
     - Modify layer group ``lg`` within workspace ``ws``
     - 200
     - XML,JSON
     - 
   * - DELETE
     - Delete layer group ``lg`` within workspace ``ws``
     - 200
     -
     -


