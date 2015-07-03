.. _rest_api_workspaces:

Workspaces
==========

A ``workspace`` is a grouping of data stores. Similar to a namespace, it is used to group data that is related in some way.

``/workspaces[.<format>]``
--------------------------

Controls all workspaces.

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status code
     - Formats
     - Default Format
   * - GET
     - List all workspaces
     - 200
     - HTML, XML, JSON
     - HTML
   * - POST
     - Create a new workspace
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

``/workspaces/<ws>[.<format>]``
-------------------------------

Controls a specific workspace.

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status code
     - Formats
     - Default Format
     - Parameters
   * - GET
     - Return workspace ``ws``
     - 200
     - HTML, XML, JSON
     - HTML
     - :ref:`quietOnNotFound <rest_api_workspaces_quietOnNotFound>`
   * - POST
     -
     - 405
     -
     -
     -
   * - PUT
     - 200
     - Modify workspace ``ws``
     - XML, JSON
     -
     -
   * - DELETE
     - 200
     - Delete workspace ``ws``
     - XML, JSON
     -
     - :ref:`recurse <rest_api_workspaces_recurse>`

Exceptions
~~~~~~~~~~

.. list-table::
   :header-rows: 1

   * - Exception
     - Status code
   * - GET for a workspace that does not exist
     - 404
   * - PUT that changes name of workspace
     - 403
   * - DELETE against a workspace that is non-empty
     - 403

Parameters
~~~~~~~~~~

.. _rest_api_workspaces_recurse:

``recurse``
^^^^^^^^^^^

The ``recurse`` parameter recursively deletes all layers referenced by the specified workspace, including data stores, coverage stores, feature types, and so on. Allowed values for this parameter are "true" or "false". The default value is "false".

.. _rest_api_workspaces_quietOnNotFound:

``quietOnNotFound``
^^^^^^^^^^^^^^^^^^^^

The ``quietOnNotFound`` parameter avoids to log an Exception when the Workspace is not present. Note that 404 status code will be returned anyway.


``/workspaces/default[.<format>]``
----------------------------------

Controls the default workspace.

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status code
     - Formats
     - Default Format
   * - GET
     - Returns default workspace
     - 200
     - HTML, XML, JSON
     - HTML
   * - POST
     -
     - 405
     -
     -
   * - PUT
     - 200
     - Set default workspace
     - XML, JSON
     -
   * - DELETE
     -
     - 405
     -
     -


``/workspaces/<ws>/settings[.<format>]``
----------------------------------------

Controls settings on a specific workspace.

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status code
     - Formats
     - Default Format
   * - GET
     - Returns workspace settings
     - 200
     - HTML, XML, JSON
     - HTML
   * - POST
     - 
     - 405
     - 
     - 
   * - PUT
     - Creates or updates workspace settings
     - 200
     - XML, JSON
     -
   * - DELETE
     - Deletes workspace settings
     - 200
     - XML, JSON
     -

