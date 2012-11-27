.. _rest_api_coverages:

Coverages
=========

A ``coverage`` is a raster data set which originates from a coverage store.

.. todo:: JC: "The second level headings [don't] work so well for the longer paths - maybe another heading format?"

``/workspaces/<ws>/coveragestores/<cs>/coverages[.<format>]``
-------------------------------------------------------------

Controls all coverages in a given coverage store and workspace.

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status code
     - Formats
     - Default Format
   * - GET
     - List all coverages in coverage store ``cs``
     - 200
     - HTML, XML, JSON
     - HTML
   * - POST
     - Create a new coverage
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
   

``/workspaces/<ws>/coveragestores/<cs>/coverages/<c>[.<format>]``
-----------------------------------------------------------------

Controls a particular coverage in a given coverage store and workspace.

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status code
     - Formats
     - Default Format
     - Parameters
   * - GET
     - Return coverage ``c``
     - 200
     - HTML, XML, JSON
     - HTML
     -
   * - POST
     -
     - 405
     -
     -
     -
   * - PUT
     - Modify coverage ``c``
     - 200
     - XML,JSON
     -
     - 
   * - DELETE
     - Delete coverage ``c``
     - 200
     -
     -
     - :ref:`recurse <rest_api_coverages_recurse>`


Exceptions
~~~~~~~~~~

.. list-table::
   :header-rows: 1

   * - Exception
     - Status code
   * - GET for a coverage that does not exist
     - 404
   * - PUT that changes name of coverage
     - 403
   * - PUT that changes coverage store of coverage
     - 403


Parameters
~~~~~~~~~~

.. _rest_api_coverages_recurse:

``recurse``
^^^^^^^^^^^

The ``recurse`` parameter recursively deletes all layers referenced by the specified coverage. Permitted values for this parameter are "true" or "false". The default value is "false".

