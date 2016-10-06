.. _rest_api_namespaces:

Namespaces
==========

A ``namespace`` is a uniquely identifiable grouping of feature types. It is identified by a prefix and a URI.

``/namespaces[.<format>]``
--------------------------

Controls all namespaces.

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status code
     - Formats
     - Default Format
   * - GET
     - List all namespaces
     - 200
     - HTML, XML, JSON
     - HTML
   * - POST
     - Create a new namespace
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


``/namespaces/<ns>[.<format>]``
-------------------------------

Controls a particular namespace.

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status code
     - Formats
     - Default Format
     - Parameters 
   * - GET
     - Return namespace ``ns``
     - 200
     - HTML, XML, JSON
     - HTML
     - :ref:`quietOnNotFound <rest_api_namespaces_quietOnNotFound>`
   * - POST
     -
     - 405
     -
     -
     -
   * - PUT
     - 200
     - Modify namespace ``ns``
     - XML, JSON
     -
     -
   * - DELETE
     - 200
     - Delete namespace ``ns``
     - XML, JSON
     -
     -

Exceptions
~~~~~~~~~~

.. list-table::
   :header-rows: 1

   * - Exception
     - Status code
   * - GET for a namespace that does not exist
     - 404
   * - PUT that changes prefix of namespace
     - 403
   * - DELETE against a namespace whose corresponding workspace is non-empty
     - 403


Parameters
~~~~~~~~~~

.. _rest_api_namespaces_quietOnNotFound:

``quietOnNotFound``
^^^^^^^^^^^^^^^^^^^^

The ``quietOnNotFound`` parameter avoids to log an Exception when the Namespace is not present. Note that 404 status code will be returned anyway.
	 
``/namespaces/default[.<format>]``
----------------------------------

Controls the default namespace.

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status code
     - Formats
     - Default Format
   * - GET
     - Return default namespace
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
     - Set default namespace
     - XML, JSON
     -
   * - DELETE
     -
     - 405
     -
     -

