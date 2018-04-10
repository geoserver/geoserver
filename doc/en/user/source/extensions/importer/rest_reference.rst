.. _importer_rest_reference:

REST API
========

Importer concepts
-----------------

The importer REST api is built around a tree of objects representing a single import, structured as follows:

   * import
      * target workspace
      * data
      * task (one or more)
          * data
          * layer
          * transformation (one or more)

An **import** refers to the top level object and is a "session" like entity the state of the entire import. It maintains information relevant to the import as a whole such as user infromation, timestamps 
along with optional information that is uniform along all tasks, such as a target workspace, the shared input data (e.g., a directory, a database).
An import is made of any number of task objects. 

A **data** is the description of the source data of a import (overall) or a task. In case the import has a global data definition, this normally refers to an aggregate
store such as a directory or a database, and the data associated to the tasks refers to a single element inside such aggregation, such as a single file or table.

A **task** represents a unit of work to the importer needed to register one new layer, or alter an existing one, and contains the following information:

* The data being imported
* The target store that is the destination of the import
* The target layer
* The data of a task, referred to as its source, is the data to be processed as part of the task. 
* The transformations that we need to apply to the data before it gets imported

This data comes in a variety of forms including:

* A spatial file (Shapefile, GeoTiff, KML, etc...)
* A directory of spatial files
* A table in a spatial database
* A remote location that the server will download data from

A task is classified as either "direct" or "indirect". A *direct task* is one in which the data being imported requires no transformation to be imported. 
It is imported directly. An example of such a task is one that involves simply importing an existing Shapefile as is. 
An *indirect task* is one that does require a **transformation** to the original import data. An example of an indirect task is one that involves importing a Shapefile into an existing PostGIS database. 
Another example of indirect task might involve taking a CSV file as an input, turning a x and y column into a Point, remapping a string column into a timestamp, and finally import the result into a PostGIS.

REST API Reference
------------------

All the imports
^^^^^^^^^^^^^^^

/imports
""""""""

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status Code/Headers
     - Input
     - Output
     - Parameters
   * - GET
     - Retrieve all imports
     - 200
     - n/a
     - Import Collection
     - n/a
   * - POST
     - Create a new import
     - 201 with Location header
     - n/a
     - Imports
     - async=false/true,execute=false/true
     
Retrieving the list of all imports
""""""""""""""""""""""""""""""""""

.. code-block:: text

    GET /imports     

results in::

	Status: 200 OK
	Content-Type: application/json
	
		{
		   "imports": [{
		     "id": 0,
		     "state": "COMPLETE",
		     "href": "http://localhost:8080/geoserver/rest/imports/0"
		
		   }, {
		     "id": 1,
		     "state": "PENDING",
		     "href": "http://localhost:8080/geoserver/rest/imports/1"          
		   }]
		}
	
Creating a new import
"""""""""""""""""""""

Posting to the /imports path a import json object creates a new import session::

	Content-Type: application/json
	
	{
	   "import": {
	      "targetWorkspace": {
	         "workspace": {
	            "name": "scratch"
	         }
	      },
	      "targetStore": {
	         "dataStore": {
	            "name": "shapes"
	         }
	      },
	      "data": {
	        "type": "file",
	        "file": "/data/spearfish/archsites.shp"
	      }
	   }
	}

The parameters are:

.. list-table::
   :widths: 10 10 60
   :header-rows: 1

   * - Name
     - Optional
     - Description
   * - targetWorkspace
     - Y
     - The target workspace to import to
   * - targetStore
     - Y
     - The target store to import to
   * - data
     - Y
     - The data to be imported

The mere creation does not start the import, but it may automatically populate its tasks depending on the target.
For example, by referring a directory of shapefiles to be importer, the creation will automatically fill in a task to import each of the shapefiles as a new layer.

The response to the above POST request will be::

	Status: 201 Created
	Location: http://localhost:8080/geoserver/rest/imports/2
	Content-Type: application/json
	
	{  
	  "import": {
	    "id": 2, 
	    "href": "http://localhost:8080/geoserver/rest/imports/2", 
	    "state": "READY", 
	    "targetWorkspace": {
	      "workspace": {
	        "name": "scratch"
	      }
	    }, 
	    "targetStore": {
	      "dataStore": {
	        "name": "shapes", 
	        "type": "PostGIS"
	      }
	    }, 
	    "data": {
	      "type": "file", 
	      "format": "Shapefile", 
	      "href": "http://localhost:8080/geoserver/rest/imports/2/data", 
	      "file": "archsites.shp"
	    }, 
	    "tasks": [
	      {
	        "id": 0, 
	        "href": "http://localhost:8080/geoserver/rest/imports/2/tasks/0", 
	        "state": "READY"
	      }
	    ]
	  }
	}
	
The operation of populating the tasks can require time, especially if done against a large set of
files, or against a "remote" data (more on this later), in this case the POST request can include ``?async=true``
at the end of the URL to make the importer run it asynchronously. 
In this case the import will be created in INIT state and will remain in such state until all
the data transfer and task creation operations are completed. In case of failure to fetch data
the import will immediately stop, the state will switch to the ``INIT_ERROR`` state,
and a error message will appear in the import context "message" field.

Adding the "execute=true" parameter to the context creation will also make the import start immediately,
assuming tasks can be created during the init phase. Combining both execute and async, "?async=true&execute=true"
will make the importer start an asynchronous initialization and execution.

The import can also have a list of default transformations, that will be applied to tasks
as they get created, either out of the initial data, or by upload. Here is an example of a
import context creation with a default transformation::

    {
      "import": {
        "targetWorkspace": {
          "workspace": {
            "name": "topp"
          }
        },
        "data": {
          "type": "file",
          "file": "/tmp/locations.csv"
        },
        "targetStore": {
          "dataStore": {
            "name": "h2"
          }
        },
        "transforms": [
          {
            "type": "AttributesToPointGeometryTransform",
            "latField": "LAT",
            "lngField": "LON"
          }
        ]
      }
    }

To get more information about transformations see the :ref:`transformations`.


Import object
^^^^^^^^^^^^^

/imports/<importId>
"""""""""""""""""""

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status Code/Headers
     - Input
     - Output
     - Parameters
   * - GET
     - Retrieve import with id <importId>
     - 200
     - n/a
     - Imports
     - n/a
   * - POST
     - Execute import with id <importId>
     - 204
     - n/a
     - n/a
     - async=true/false
   * - PUT
     - Create import with proposed id <importId>. If the proposed id is
       ahead of the current (next) id, the current id will be advanced. If the
       proposed id is less than or equal to the current id, the current will be
       used. This allows an external system to dictate the id management.
     - 201 with Location header
     - n/a
     - Imports
     - n/a
   * - DELETE
     - Remove import with id <importId>
     - 200
     - n/a
     - n/a
     - n/a  
    
The representation of a import is the same as the one contained in the import creation response.
The execution of a import can be a long task, as such, it's possible to add ``async=true`` to the
request to make it run in a asynchronous fashion, the client will have to poll the import representation
and check when it reaches the "COMPLETE" state. 

Data
^^^^

A import can have a "data" representing the source of the data to be imported. The data can
be of different types, in particular, "file", "directory", "mosaic", "database" and "remote".
During the import initialization the importer will scan the contents of said resource, and
generate import tasks for each data found in it.

Most data types are discussed in the task section, the only type that's specific to the whole
import context is the "remote" one, that is used to ask the importer to fetch the data from
a remote location autonomusly, without asking the client to perform an upload.

The representation of a remote resource looks as follows::

      "data": {
        "type": "remote",
        "location": "ftp://fthost/path/to/importFile.zip",
        "username": "user",
        "password": "secret",
        "domain" : "mydomain"
      }

The location can be `any URI supported by Commons VFS <http://commons.apache.org/proper/commons-vfs/filesystems.html>`_,
including HTTP and FTP servers. The ``username``, ``password`` and ``domain`` elements are all optional,
and required only if the remote server demands an authentication of sorts.
In case the referred file is compressed, it will be unpacked as the download completes, and the
tasks will be created over the result of unpacking.
    
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
     - Task Collection
   * - POST
     - Create a new task
     - 201 with Location header
     - :ref:`Multipart form data <file_upload>`
     - Tasks

.. _file_upload:

Getting the list of tasks
"""""""""""""""""""""""""

.. code-block:: text
   
   GET /imports/0/tasks

Results in::

	Status: 200 OK
	Content-Type: application/json
	
	{
	  "tasks": [
	    {
	      "id": 0, 
	      "href": "http://localhost:8080/geoserver/rest/imports/2/tasks/0", 
	      "state": "READY"
	    }
	  ]
	}

Creating a new task as a file upload
""""""""""""""""""""""""""""""""""""

A new task can be created by issuing a POST to ``imports/<importId>/tasks`` as a "Content-type: multipart/form-data" multipart encoded data as defined by `RFC 2388 <https://www.ietf.org/rfc/rfc2388.txt>`_.
One or more file can be uploaded this way, and a task will be created for importing them. In case the file being uploaded is a zip file, it will be unzipped on the server side and treated as a directory of files.

The response to the upload will be the creation of a new task, for example::

	Status: 201 Created
	Location: http://localhost:8080/geoserver/rest/imports/1/tasks/1
	Content-type: application/json
	
	{
	  "task": {
	    "id": 1, 
	    "href": "http://localhost:8080/geoserver/rest/imports/2/tasks/1", 
	    "state": "READY",
	    "updateMode": "CREATE", 
	    "data": {
	      "type": "file", 
	      "format": "Shapefile", 
	      "href": "http://localhost:8080/geoserver/rest/imports/2/tasks/1/data", 
	      "file": "bugsites.shp"
	    }, 
	    "target": {
	      "href": "http://localhost:8080/geoserver/rest/imports/2/tasks/1/target", 
	      "dataStore": {
	        "name": "shapes", 
	        "type": "PostGIS"
	      }
	    },
	    "progress": "http://localhost:8080/geoserver/rest/imports/2/tasks/1/progress", 
	    "layer": {
	      "name": "bugsites", 
	      "href": "http://localhost:8080/geoserver/rest/imports/2/tasks/1/layer"
	    }, 
	    "transformChain": {
	      "type": "vector", 
	      "transforms": []
	    }
	  }
	}

Creating a new task from form upload
""""""""""""""""""""""""""""""""""""

This creation mode assumes the POST to ``imports/<importId>/tasks`` of form url encoded data containing a ``url`` parameter::

	Content-type: application/x-www-form-urlencoded
	
	url=file:///data/spearfish/

The creation response will be the same as the multipart upload.

Single task resource
^^^^^^^^^^^^^^^^^^^^

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
     - Task
   * - PUT
     - Modify task with id <taskId> within import with id <importId>
     - 200
     - Task
     - Task
   * - DELETE
     - Remove task with id <taskId> within import with id <importId>
     - 200
     - n/a
     - n/a

The representation of a task resource is the same one reported in the task creation response.

Updating a task
"""""""""""""""

A PUT request over an existing task can be used to update its representation. The representation can be partial, and just contains
the elements that need to be updated.

The updateMode of a task normally starts as "CREATE", that is, create the target resource if missing. Other possible values are
"REPLACE", that is, delete the existing features in the target layer and replace them with the task source ones, or "APPEND",
to just add the features from the task source into an existing layer.

The following PUT request updates a task from "CREATE" to "APPEND" mode::

	Content-Type: application/json
	
	{
	  "task": {
	     "updateMode": "APPEND"
	  }
	}
	
Directory files representation
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The following operations are specific to data objects of type ``directory``.

/imports/<importId>/task/<taskId>/data/files
""""""""""""""""""""""""""""""""""""""""""""

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status Code/Headers
     - Input
     - Output
   * - GET
     - Retrieve the list of files for a task with id <taskId> within import with id <importId>
     - 200
     - n/a
     - Task

The response to a GET request will be::

	Status: 200 OK
	Content-Type: application/json

	{
		files: [
			{
			file: "tasmania_cities.shp",
			href: "http://localhost:8080/geoserver/rest/imports/0/tasks/0/data/files/tasmania_cities.shp"
			},
			{
			file: "tasmania_roads.shp",
			href: "http://localhost:8080/geoserver/rest/imports/0/tasks/0/data/files/tasmania_roads.shp"
			},
			{
			file: "tasmania_state_boundaries.shp",
			href: "http://localhost:8080/geoserver/rest/imports/0/tasks/0/data/files/tasmania_state_boundaries.shp"
			},
			{
			file: "tasmania_water_bodies.shp",
			href: "http://localhost:8080/geoserver/rest/imports/0/tasks/0/data/files/tasmania_water_bodies.shp"
			}
		]
	}

/imports/<importId>/task/<taskId>/data/files/<fileId>
"""""""""""""""""""""""""""""""""""""""""""""""""""""

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status Code/Headers
     - Input
     - Output
   * - GET
     - Retrieve the file with id <fileId> from the data of a task with id <taskId> within import with id <importId>
     - 200
     - n/a
     - Task
   * - DELETE
     - Remove a specific file from the task with id <taskId> within import with id <importId>
     - 200
     - n/a
     - n/a


Following the links we'll get to the representation of a single file, notice how in this case a main file can be associate to sidecar files::
	
	Status: 200 OK
	Content-Type: application/json

	{
		type: "file",
		format: "Shapefile",
		location: "C:\devel\gs_data\release\data\taz_shapes",
		file: "tasmania_cities.shp",
		href: "http://localhost:8080/geoserver/rest/imports/0/tasks/0/data/files/tasmania_cities.shp",
		prj: "tasmania_cities.prj",
		other: [
			"tasmania_cities.dbf",
			"tasmania_cities.shx"
		]
	}
	
Mosaic extensions
"""""""""""""""""

In case the input data is of ``mosaic`` type, we have all the attributes typical of a directory, plus support
for directly specifying the timestamp of a particular granule.

In order to specify the timestamp a PUT request can be issued against the granule::

	Content-Type: application/json
	
	{
	   "timestamp": "2004-01-01T00:00:00.000+0000"
	}

and the response will be::

	Status: 200 OK
	Content-Type: application/json
	
	{
	  "type": "file", 
	  "format": "GeoTIFF", 
	  "href": "http://localhost:8080/geoserver/rest/imports/0/tasks/0/data/files/bm_200401.tif", 
	  "location": "/data/bluemarble/mosaic", 
	  "file": "bm_200401.tiff", 
	  "prj": null, 
	  "other": [], 
	  "timestamp": "2004-01-01T00:00:00.000+0000"
	}

Database data
^^^^^^^^^^^^^

The following operations are specific to data objects of type ``database``. At the time or writing, the REST API does not allow
the creation of a database data source, but it can provide a read only description of one that has been created using the GUI.

/imports/<importId>/tasks/<taskId>/data
"""""""""""""""""""""""""""""""""""""""

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status Code/Headers
     - Input
     - Output
   * - GET
     - Retrieve the database connection parameters for a task with id <taskId> within import with id <importId>
     - 200
     - n/a
     - List of database connection parameters and available tables

Performing a GET on a database type data will result in the following response::

	{
		type: "database",
		format: "PostGIS",
		href: "http://localhost:8080/geoserver/rest/imports/0/data",
		parameters: {
			schema: "public",
			fetch size: 1000,
			validate connections: true,
			Connection timeout: 20,
			Primary key metadata table: null,
			preparedStatements: true,
			database: "gttest",
			port: 5432,
			passwd: "cite",
			min connections: 1,
			dbtype: "postgis",
			host: "localhost",
			Loose bbox: true,
			max connections: 10,
			user: "cite"
		},
		tables: [
			"geoline",
			"geopoint",
			"lakes",
			"line3d",
		]
	}


Database table
^^^^^^^^^^^^^^^

The following operations are specific to data objects of type ``table``. At the time or writing, the REST API does not allow
the creation of a database data source, but it can provide a read only description of one that has been created using the GUI.
A table description is normally linked to task, and refers to a database data linked to the overall import.

/imports/<importId>/tasks/<taskId>/data
"""""""""""""""""""""""""""""""""""""""

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status Code/Headers
     - Input
     - Output
   * - GET
     - Retrieve the table description for a task with id <taskId> within import with id <importId>
     - 200
     - n/a
     - A table representation

Performing a GET on a database type data will result in the following response::

	{
		type: "table",
		name: "abc",
		format: "PostGIS",
		href: "http://localhost:8080/geoserver/rest/imports/0/tasks/0/data"
	}

	
Task target layer
^^^^^^^^^^^^^^^^^^^

/imports/<importId>/tasks/<taskId>/layer	
""""""""""""""""""""""""""""""""""""""""

The layer defines how the target layer will be created

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status Code/Headers
     - Input
     - Output
   * - GET
     - Retrieve the layer of a task with id <taskId> within import with id <importId>
     - 200
     - n/a
     - A layer JSON representation
   * - PUT
     - Modify the target layer for a task with id <taskId> within import with id <importId>
     - 200
     - Task
     - Task


Requesting the task layer will result in the following::
 
	Status: 200 OK
	Content-Type: application/json
	
	{
		layer: {
		name: "tasmania_cities",
		href: "http://localhost:8080/geoserver/rest/imports/0/tasks/0/layer",
		title: "tasmania_cities",
		originalName: "tasmania_cities",
		nativeName: "tasmania_cities",
		srs: "EPSG:4326",
		bbox: {
			minx: 147.2909004483,
			miny: -42.85110181689001,
			maxx: 147.2911004483,
			maxy: -42.85090181689,
			crs: "GEOGCS["WGS 84", DATUM["World Geodetic System 1984", SPHEROID["WGS 84", 6378137.0, 298.257223563, AUTHORITY["EPSG","7030"]], AUTHORITY["EPSG","6326"]], PRIMEM["Greenwich", 0.0, AUTHORITY["EPSG","8901"]], UNIT["degree", 0.017453292519943295], AXIS["Geodetic longitude", EAST], AXIS["Geodetic latitude", NORTH], AUTHORITY["EPSG","4326"]]"
		},
		attributes: [
			{
				name: "the_geom",
				binding: "com.vividsolutions.jts.geom.MultiPoint"
			},
			{
				name: "CITY_NAME",
				binding: "java.lang.String"
			},
			{
				name: "ADMIN_NAME",
				binding: "java.lang.String"
			},
			{
				name: "CNTRY_NAME",
				binding: "java.lang.String"
			},
			{
				name: "STATUS",
				binding: "java.lang.String"
			},
			{
				name: "POP_CLASS",
				binding: "java.lang.String"
			}
			],
			style: {
				name: "cite_tasmania_cities",
				href: "http://localhost:8080/geoserver/rest/imports/0/tasks/0/layer/style"
			}
		}
	}

All the above attributes can be updated using a PUT request. Even if the above representation is similar to the REST config API, it should not
be confused with it, as it does not support all the same properties, in particular the supported properties are all the ones listed above.

Task transformations
^^^^^^^^^^^^^^^^^^^^

/imports/<importId>/tasks/<taskId>/transforms
"""""""""""""""""""""""""""""""""""""""""""""

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status Code/Headers
     - Input
     - Output
   * - GET
     - Retrieve the list of transformations of a task with id <taskId> within import with id <importId>
     - 200
     - n/a
     - A list of transfromations in JSON format
   * - POST
     - Create a new transormation and append it inside a task with id <taskId> within import with id <importId>
     - 201
     - A JSON transformation representation
     - The transform location 

Retrieving the transformation list
""""""""""""""""""""""""""""""""""

A GET request for the list of transformations will result in the following response::

	Status: 200 OK
	Content-Type: application/json
	
	{
	  "transforms": [
	    {
	      "type": "ReprojectTransform", 
	      "href": "http://localhost:8080/geoserver/rest/imports/0/tasks/1/transforms/0", 
	      "source": null, 
	      "target": "EPSG:4326"
	    }, 
	    {
	      "type": "DateFormatTransform", 
	      "href": "http://localhost:8080/geoserver/rest/imports/0/tasks/1/transforms/1", 
	      "field": "date", 
	      "format": "yyyyMMdd"
	    }
	  ]
	}
	
Appending a new transformation
""""""""""""""""""""""""""""""

Creating a new transformation requires posting a JSON document with a ``type`` property identifying the class of the
transformation, plus any extra attribute required by the transformation itself (this is transformation specific, each one will use a different set of attributes).

The following POST request creates an attribute type remapping::

	Content-Type: application/json
	
	{
	   "type": "AttributeRemapTransform",
	   "field": "cat",
	   "target": "java.lang.Integer"
	}
	
The response will be::

    Status: 201 OK
    Location: http://localhost:8080/geoserver/rest/imports/0/tasks/1/transform/2
    
/imports/<importId>/tasks/<taskId>/transforms/<transformId>
"""""""""""""""""""""""""""""""""""""""""""""""""""""""""""

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status Code/Headers
     - Input
     - Output
   * - GET
     - Retrieve a transformation identified by <transformId> inside a task with id <taskId> within import with id <importId>
     - 200
     - n/a
     - A single transformation in JSON format
   * - PUT
     - Modifies the definition of a transformation identified by <transformId> inside a task with id <taskId> within import with id <importId>
     - 200
     - A JSON transformation representation (eventually just the portion of it that needs to be modified)
     - The full transformation representation
   * - DELETE
     - Removes the transformation identified by <transformId> inside a task with id <taskId> within import with id <importId>
     - 200
     - A JSON transformation representation (eventually just the portion of it that needs to be modified)
     - The full transformation representation
 
Retrieve a single transformation
""""""""""""""""""""""""""""""""

Requesting a single transformation by identifier will result in the following response::

	Status: 200 OK
	Content-Type: application/json
	
	{
	  "type": "ReprojectTransform", 
	  "href": "http://localhost:8080/geoserver/rest/imports/0/tasks/1/transforms/0", 
	  "source": null, 
	  "target": "EPSG:4326"
	}
	
Modify an existing transformation
"""""""""""""""""""""""""""""""""

Assuming we have a reprojection transformation, and that we need to change the target SRS type, the following PUT request will do the job::

	Content-Type: application/json
	{
	   "type": "ReprojectTransform",
	   "target": "EPSG:3005"
	}
	
The response will be::

    Status: 200 OK
	Content-Type: application/json
	
	{
	  "type": "ReprojectTransform", 
	  "href": "http://localhost:8080/geoserver/rest/imports/0/tasks/1/transform/0", 
	  "source": null, 
	  "target": "EPSG:3005"
	}
	
.. _transformations:	
	
Transformation reference
^^^^^^^^^^^^^^^^^^^^^^^^

AttributeRemapTransform
"""""""""""""""""""""""

Remaps a certain field to a given target data type

.. list-table::
   :header-rows: 1

   * - Parameter
     - Optional
     - Description
   * - field
     - N
     - The name of the field to be remapped
   * - target
     - N
     - The "target" field type, as a fully qualified Java class name


AttributeComputeTransform
"""""""""""""""""""""""""

Computes a new field based on an expression that can use the other field values

.. list-table::
   :header-rows: 1

   * - Parameter
     - Optional
     - Description
   * - field
     - N
     - The name of the field to be computed
   * - fieldType
     - N
     - The field type, as a fully qualified Java class name (e.g., ``java.lang.String``, ``java.lang.Integer``, ``java.util.Date`` and so on)
   * - cql
     - N
     - The (E)CQL expression used to compute the new field (can be a constant value, e.g. ``'My String'``)


AttributesToPointGeometryTransform
""""""""""""""""""""""""""""""""""

Transforms two numeric fields ``latField`` and ``lngField`` into a point geometry representation ``POINT(lngField,latField)``, the source fields will be removed.

.. list-table::
   :header-rows: 1

   * - Parameter
     - Optional
     - Description
   * - latField
     - N
     - The "latitude" field
   * - lngField
     - N
     - The "longitude" field

CreateIndexTransform
""""""""""""""""""""

For database targets only, creates an index on a given column after importing the data into the database

.. list-table::
   :header-rows: 1

   * - Parameter
     - Optional
     - Description
   * - field
     - N
     - The field to be indexed
     
DateFormatTransform
"""""""""""""""""""

Parses a string representation of a date into a Date/Timestamp object

.. list-table::
   :header-rows: 1

   * - Parameter
     - Optional
     - Description
   * - field
     - N
     - The field to be parsed
   * - format
     - Y
     - A date parsing pattern, setup using the Java `SimpleDateFormat syntax <http://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html>`_. In case it's missing, a number of built-in formats will be tried instead (short and full ISO date formats, dates without any separators).
   
IntegerFieldToDateTransform
"""""""""""""""""""""""""""

Takes a integer field and transforms it to a date, interpreting the intereger field as a date

.. list-table::
   :header-rows: 1

   * - Parameter
     - Optional
     - Description
   * - field
     - N
     - The field containing the year information

ReprojectTransform
""""""""""""""""""

Reprojects a vector layer from a source CRS to a target CRS

.. list-table::
   :header-rows: 1

   * - Parameter
     - Optional
     - Description
   * - source
     - Y
     - Identifier of the source coordinate reference system (the native one will be used if missing)
   * - target
     - N
     - Identifier of the target coordinate reference system
     
GdalTranslateTransform
""""""""""""""""""""""

Applies ``gdal_translate`` to a single file raster input. Requires ``gdal_translate`` to be inside the PATH used by the web container running GeoServer.


.. list-table::
   :header-rows: 1

   * - Parameter
     - Optional
     - Description
   * - options
     - N
     - Array of options that will be passed to ``gdal_translate`` (beside the input and output names, which are internally managed)
     
GdalWarpTransform
"""""""""""""""""

Applies ``gdalwarp`` to a single file raster input. Requires ``gdalwarp`` to be inside the PATH used by the web container running GeoServer.


.. list-table::
   :header-rows: 1

   * - Parameter
     - Optional
     - Description
   * - options
     - N
     - Array of options that will be passed to ``gdalwarp`` (beside the input and output names, which are internally managed)
     
GdalAddoTransform
"""""""""""""""""

Applies ``gdaladdo`` to a single file raster input. Requires ``gdaladdo`` to be inside the PATH used by the web container running GeoServer.


.. list-table::
   :header-rows: 1

   * - Parameter
     - Optional
     - Description
   * - options
     - N
     - Array of options that will be passed to ``gdaladdo`` (beside the input file name, which is internally managed)
   * - levels
     - N
     - Array of integers with the overview levels that will be passed to ``gdaladdo``

PostScriptTransform
"""""""""""""""""""

Runs the specified script after the data is imported. The script must be located in ``$GEOSERVER_DATA_DIR/importer/scripts``.
The script can be any executable file.
At the time of writing, there is no way to pass information about the data just imported to the script (TBD).

.. list-table::
   :header-rows: 1

   * - Parameter
     - Optional
     - Description
   * - name
     - N
     - Name of the script to be invoked
   * - options
     - Y
     - Array of options that will be passed to the script
