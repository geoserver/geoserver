.. _rest_api_featuretypes:

Feature types
=============

A ``feature type`` is a vector based spatial resource or data set that originates from a data store. In some cases, such as  with a shapefile, a feature type has a one-to-one relationship with its data store. In other cases, such as PostGIS, the relationship of feature type to data store is many-to-one, feature types corresponding to a table in the database.


``/workspaces/<ws>/datastores/<ds>/featuretypes[.<format>]``
------------------------------------------------------------

Controls all feature types in a given data store / workspace.

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status code
     - Formats
     - Default Format
     - Parameters
   * - GET
     - List all feature types in data store ``ds``
     - 200
     - HTML, XML, JSON
     - HTML
     - :ref:`list <rest_api_featuretypes_list>`
   * - POST
     - Create a new feature type, :ref:`see note below <rest_api_featuretypes_post>`
     - 201 with ``Location`` header
     - XML, JSON
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

.. _rest_api_featuretypes_post:

.. note:: When creating a new feature type via ``POST``, if no underlying dataset with the specified name exists an attempt will be made to create it. This will work only in cases where the underlying data format supports the creation of new types (such as a database). When creating a feature type in this manner the client should include all attribute information in the feature type representation.

Exceptions
~~~~~~~~~~

.. list-table::
   :header-rows: 1

   * - Exception
     - Status code
   * - GET for a feature type that does not exist
     - 404
   * - PUT that changes name of feature type
     - 403
   * - PUT that changes data store of feature type
     - 403

Parameters
~~~~~~~~~~

.. _rest_api_featuretypes_list:

``list``
^^^^^^^^

The ``list`` parameter is used to control the category of feature types that are returned. It can take one of the following values:

* ``configured``—Only configured feature types are returned. This is the default value.
* ``available``—Only feature types that haven't been configured but are available from the specified data store will be returned. 
* ``available_with_geom``—Same as ``available`` but only includes feature types that have a geometry attribute.
* ``all``—The union of ``configured`` and ``available``.


``/workspaces/<ws>/datastores/<ds>/featuretypes/<ft>[.<format>]``
-----------------------------------------------------------------

Controls a particular feature type in a given data store and workspace.

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status code
     - Formats
     - Default Format
     - Parameters
   * - GET
     - Return feature type ``ft``
     - 200
     - HTML, XML, JSON
     - HTML
     - :ref:`quietOnNotFound <rest_api_featuretypes_quietOnNotFound>`	
   * - POST
     -
     - 405
     -
     -
     -
   * - PUT
     - Modify feature type ``ft``
     - 200
     - XML,JSON
     -
     - :ref:`recalculate <rest_api_featuretypes_recalculate>`
   * - DELETE
     - Delete feature type ``ft``
     - 200
     -
     -
     - :ref:`recurse <rest_api_featuretypes_recurse>`

Exceptions
~~~~~~~~~~

.. list-table::
   :header-rows: 1

   * - Exception
     - Status code
   * - GET for a feature type that does not exist
     - 404
   * - PUT that changes name of feature type
     - 403
   * - PUT that changes data store of feature type
     - 403

Parameters
~~~~~~~~~~

.. _rest_api_featuretypes_recurse:

``recurse``
^^^^^^^^^^^

The ``recurse`` parameter recursively deletes all layers referenced by the specified featuretype. Allowed values for this parameter are "true" or "false". The default value is "false". A DELETE request with ``recurse=false`` will fail if any layers reference the featuretype.

.. _rest_api_featuretypes_recalculate:

``recalculate``
^^^^^^^^^^^^^^^

The ``recalculate`` parameter specifies whether to recalculate any bounding boxes for a feature type. Some properties of feature types are automatically recalculated when necessary. In particular, the native bounding box is recalculated when the projection or projection policy are changed, and the lat/long bounding box is recalculated when the native bounding box is recalculated, or when a new native bounding box is explicitly provided in the request. (The native and lat/long bounding boxes are not automatically recalculated when they are explicitly included in the request.) In addition, the client may explicitly request a fixed set of fields to calculate, by including a comma-separated list of their names in the ``recalculate`` parameter. For example:

* ``recalculate=`` (empty parameter): Do not calculate any fields, regardless of the projection, projection policy, etc. This might be useful to avoid slow recalculation when operating against large datasets.
* ``recalculate=nativebbox``: Recalculate the native bounding box, but do not recalculate the lat/long bounding box.
* ``recalculate=nativebbox,latlonbbox``: Recalculate both the native bounding box and the lat/long bounding box.

.. _rest_api_featuretypes_quietOnNotFound:

``quietOnNotFound``
^^^^^^^^^^^^^^^^^^^^

The ``quietOnNotFound`` parameter avoids to log an Exception when the feature type is not present. Note that 404 status code will be returned anyway.
