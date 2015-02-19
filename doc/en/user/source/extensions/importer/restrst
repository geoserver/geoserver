.. _importer_rest:

REST API
========

The importer REST api provides a CRUD interface to the import. 

Representations
---------------

All entities in this api are represented as JSON.

.. _imports:

Imports
^^^^^^^

.. _import_collection:

An import is the top level object of the importer data model. An import
collection is represented as JSON with the following structure::

  {
    "imports": [
      {
        "id": 0,
        "href": "http://<host>:<port>/geoserver/rest/imports/0"
      }, 
      {
        "id": 1,
        "href": "http://<host>:<port>/geoserver/rest/imports/1"
      }, 
      ...
    ]
  }

Each object in the ``imports`` array contains the following attributes:

.. list-table::
   :header-rows: 0

   * - id
     - Globally unique identifier for the import
   * - href
     - URI pointing to the import

.. _import:

An individual import is represented with the following structure::

    {
      "import": {
        "id": 1
        "tasks": [
           //array of task objects
        ] 
      }
    }

An ``import`` object contains the following attributes:

.. list-table::
   :header-rows: 0

   * - id
     - Globally unique identifier for the import.
   * - state
     - Current state of the import. See :ref:`import states <import_state>`. 
   * - tasks
     - The :ref:`tasks <tasks>` that compose the import.

.. _import_state:

An import can be in one of the following states:

.. list-table::
   :header-rows: 0
 
   * - PENDING
     - Initial state for an import, indicating that the import is newly created and yet to be processed.
   * - READY
     - The import has been processed without issue and is ready to be executed.
   * - RUNNING
     - The import is currently executing.
   * - INCOMPLETE
     - The import can't be processed do to one or more tasks being non-complete. See :ref:`task states <task_state>`.
   * - COMPLETE
     - The import has executed successfully. 

.. _tasks:

Tasks
^^^^^

A task represents a unit of work within an import. An individual ``task`` is represented with the following structure::

    {
      "task": {
        "id": <taskId>,
        "state": "<state>", 
        "href": "http://<host>:<port>/geoserver/rest/imports/<importId>/tasks/<taskId>", 
        "source": {
           // source representation, see below
        },
        "target": {
           // store representation from GeoServer restconfig
        }, 
        "items": [
           // array of item objects
        ]
      }
    }

A ``task`` object contains the following attributes:

.. list-table::
   :header-rows: 0

   * - id
     - Unique identifier for the task, relative to the containing import
   * - state
     - Current state of the task. See :ref:`task states <task_state>`. 
   * - href
     - URI pointing to the task
   * - source
     - The data to be imported. See :ref:`task source <task_source>`. 
   * - target
     - The store that the data for task is to be imported into. See :ref:`task target <task_target>`
   * - items
     - The :ref:`items <items>` that compose the task.

.. _task_state:

A task can be in one of the following states:

.. list-table::
   :header-rows: 0
 
   * - PENDING
     - Initial state for an task, indicating that the task is newly created and yet to be processed.
   * - READY
     - The task has been processed without issue and is ready to be executed.
   * - RUNNING
     - The task is currently executing.
   * - INCOMPLETE
     - The task can't be processed do to one or more items being non-complete. See :ref:`item states <item_state>`.
   * - COMPLETE
     - The task has executed successfully.

.. _task_source:

Source
""""""

The source of a task represents the data that is to imported as part of the task. The structure of the source 
is dependent on the type of source. The following lists the set of source types:

.. list-table:: Source
   :header-rows: 0

   * - file
     - A single file (Shapefile, GeoTIFF, etc...).
   * - directory
     - A directory of files.
   * - database
     - A relational database.

A ``source`` object is represented with the following structure::

   {
     "source": {
          "type": "file"
          "format": "<Shapefile|GeoTIFF|PostGIS|...>", 
          // source specific attributes
        }
   }

All ``source`` objects contain the following attributes:

.. list-table::
   :header-rows: 0

   * - type
     - Data source type.
   * - format
     - The data type or format of the source.

.. _source_file:

Specific sources have additional attributes. A ``file`` source contains the following attributes:

.. list-table::
   :header-rows: 0

   * - file
     - Primary spatial file.
   * - prj
     - Supplementary ``.prj`` file defining the projection of the data.
   * - other
     - Additional files that supplement the primary file. A Shapefile for instance would contain 
       ``.dbf`` and ``.shx`` files.
   * - location
     - Path of directory containing the file.

A ``directory`` source contains the following attributes:

.. list-table::
   :header-rows: 0

   * - location
     - Path of the directory.
   * - files
     - Array of file objects, as described :ref:`above <source_file>`.

.. _task_target:

Target
""""""

The target of a task represents the destination store (data store, coverage store, etc...) that the task data source is to import into. The structure of the target 
is dependent on the type store, which depends on the type of data source. Vector data results in a data store target represented with the following::

   {
     "target": {
        "dataStore": {
           // same representation as GeoServer restconfig
        }
     }
   }

Similarly raster data results in a coverage store target::

    {
      "target": {
        "coverageStore": {
           // same representation as GeoServer restconfig
        }
      }
    }

.. _items:

Items
^^^^^

An item represents a layer/resource to be imported as part of a task. An individual ``item`` is represented with the following structure::

    {
      "item": {
        "id": <itemId>, 
        "state": "COMPLETE", 
        "href": "http://<host>:<port>/geoserver/rest/imports/<importId>/tasks/<taskId>/items/<itemId>", 
        "layer": {
           // same representation as GeoServer restconfig
        },
        "resource": {
           // same representation as GeoServer restconfig
        } 
      }
    }

An ``item`` object contains the following attributes:

.. list-table::
   :header-rows: 0

   * - id
     - Unique identifier for the item, relative to the containing task
   * - state
     - Current state of the item. See :ref:`item states <item_state>`. 
   * - href
     - URI pointing to the item
   * - layer
     - Geoserver layer that publishes the item after it has been imported
   * - resource
     - Underlying resource for the publishing layer

.. _item_state:

An item can be in one of the following states:

.. list-table::
   :header-rows: 0

   * - PENDING
     - Initial state for an item, indicating that the task is newly created and yet to be processed.
   * - READY
     - The item has been processed without issue and is ready to be executed.
   * - RUNNING
     - The item is currently executing.
   * - NO_CRS
     - Projection for the item could not be determined from the data.
   * - NO_BOUNDS
     - Spatial extent of the item could not be determined from the data, or is too expensive to compute.
   * - ERROR
     - Error occurred during import execution.
   * - COMPLETE
     - The item has executed successfully.

If an item is in one of the ``NO_CRS``, ``NO_BOUNDS`` states then the client should modify the 
item configuration (via PUT) with the necessary information. 

.. see :ref:`item endpoint <item_op>`.

.. _item_layer:

Layer
"""""

The layer of an item represents the GeoServer configuration that will ultimately be used to publish the data. A layer
is represented the same as it is in the GeoServer RESTful configuration api (restconfig)::

  {
    "layer": {
      "layer": {
        "name": "<layerName>",
        "type": "<VECTOR|RASTER>",
        "defaultStyle": {
           // same representation as GeoServer restconfig
       } 
     }
  }

.. _item_resource:

Resource
""""""""

The resource of an item represents the data configuration underlying the layer/publishing configuration discussed above. 
The type of resource depends on the type of data. The resource of a vector item is a feature type, whereas the resource
of a raster item is a coverage. A resource is represented the same as it is in the GeoServer RESTful configuration api 
(restconfig)::

  {
    "resource": {
      "featureType": {
        "name": "...", 
        "nativeName": "...", 
        "title": "...", 
        "srs": "...", 
        "nativeCRS": {...}, 
        "projectionPolicy": "...", 
        "nativeBoundingBox": {...}, 
        "latLonBoundingBox": {...},
        ...
      }
    }
  }
  {
    "resource": {
      "coverage": {
        "name": "...", 
        "nativeName": "...", 
        "title": "...", 
        "srs": "...", 
        "nativeCRS": {...}, 
        "projectionPolicy": "...", 
        "nativeBoundingBox": {...}, 
        "latLonBoundingBox": {...},
        ...
        "dimensions": {...},
        "interpolationMethods": {...},
        ...
      }
    }
  }

Operations
----------

Imports
^^^^^^^

/imports
""""""""

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status Code/Headers
     - Input
     - Output
   * - GET
     - Retrieve all imports
     - 200
     - n/a
     - :ref:`Import Collection <import_collection>`
   * - POST
     - Create a new import
     - 201 with Location header
     - n/a
     - :ref:`Imports <import>`

/imports/<importId>
"""""""""""""""""""

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status Code/Headers
     - Input
     - Output
   * - GET
     - Retrieve import with id <importId>
     - 200
     - n/a
     - :ref:`Imports <import>`
   * - POST
     - Execute import with id <importId>
     - 204
     - n/a
     - n/a
   * - PUT
     - Create import with proposed id <importId>. If the proposed id is
       ahead of the current (next) id, the current id will be advanced. If the
       proposed id is less than or equal to the current id, the current will be
       used. This allows an external system to dictate the id management.
     - 201 with Location header
     - n/a
     - :ref:`Imports <import>`
   * - DELETE
     - Remove import with id <importId>
     - 200
     - n/a
     - n/a

Tasks
^^^^^

/imports/<importId>/tasks
"""""""""""""""""""""""""

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status Code/Headers
     - Input
     - Output
   * - GET
     - Retrieve all tasks for import with id <importId>
     - 200
     - n/a
     - :ref:`Task Collection <tasks>`
   * - POST
     - Create a new task
     - 201 with Location header
     - :ref:`Multipart form data <file_upload>`
     - :ref:`Tasks <tasks>`

.. _file_upload:

To create a new task within an import a client may upload file(s) to the ``tasks`` collection
via a multi part form. The ``Content-Type`` header should have a value of "multipart/form-data"
(optionally with a subtype). 

/imports/<importId>/task/<taskId>
"""""""""""""""""""""""""""""""""

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status Code/Headers
     - Input
     - Output
   * - GET
     - Retrieve task with id <taskId> within import with id <importId>
     - 200
     - n/a
     - :ref:`Task <tasks>`
   * - PUT
     - Modify task with id <taskId> within import with id <importId>
     - 200
     - :ref:`Task <tasks>`
     - :ref:`Task <tasks>`
   * - DELETE
     - Remove task with id <taskId> within import with id <importId>
     - 200
     - n/a
     - n/a

Items
^^^^^

/imports/<importId>/tasks/<taskId>/items
""""""""""""""""""""""""""""""""""""""""

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status Code/Headers
     - Input
     - Output
   * - GET
     - Retrieve all items within import/task <importId>/<taskId>
     - 200
     - n/a
     - :ref:`Item Collection <items>`

/imports/<importId>/tasks/<taskId>/items/<itemId>
"""""""""""""""""""""""""""""""""""""""""""""""""

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status Code/Headers
     - Input
     - Output
   * - GET
     - Retrieve item with id <item> within import/task <importId>/<taskId>
     - 200
     - n/a
     - :ref:`Item <items>`
   * - PUT
     - Modify task with id <itemId> within import/task <importId>/<taskId>
     - 200
     - :ref:`Item <items>`
     - :ref:`Item <items>`
   * - DELETE
     - Remove item with id <itemId> within import/task <importId>/<taskId>
     - 200
     - n/a
     - n/a
