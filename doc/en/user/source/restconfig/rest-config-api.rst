.. _rest_config_api_ref:

REST Configuration API Reference
================================

Formats and representations
---------------------------

A ``format`` specifies how a resource should be represented. A format is used:

- In an operation to specify what representation should be returned to the 
  client
- In a POST or PUT operation to specify the representation being sent to the 
  server

In a GET operation the format can be specified in a number of ways. The first is
with the ``Accepts`` header. For instance setting the header to "text/xml" would
specify the desire to have the resource returned as XML. The second method of 
specifying the format is via file extension. For example consider the resource 
"foo". To request a representation of foo as XML the request uri would end with
"foo.xml". To request as JSON the request uri would end with "foo.json". When no
format is specified the server will use its own internal format, usually html.

In a POST or PUT operation the format specifies 1) the representatin of the 
content being sent to the server, and 2) the representation of the resposne to
be sent back. The former is specified with the ``Content-type`` header. To send
a representation in XML, the content type "text/xml" or "application/xml" would
be used. The latter is specified with the ``Accepts`` header as specified in the
above paragraph describing a GET operation.

The following table defines the ``Content-type`` values for each format: 

.. list-table::
   :header-rows: 1

   * - Format
     - Content-type
   * - XML
     - application/xml
   * - JSON
     - application/json
   * - HTML
     - application/html
   * - SLD
     - application/vnd.ogc.sld+xml

Authentication
--------------

POST, PUT, and DELETE requests (requests that modify resources) require the 
client to be authenticated. Currently the only supported method of 
authentication is Basic authentication. See the 
:ref:`examples <rest_config_examples>` section for examples of how to perform 
authentication with various clients and environments.

Status codes
------------

A Http request uses a ``status code`` to relay the outcome of the request to the
client. Different status codes are used for various purposes through out this 
document. These codes are described in detail by the `http specification <http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html>`_.

Global Settings
---------------

Allows accessing global settings for GeoServer

Operations
^^^^^^^^^^

``/settings[.<format>]``

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Return Code
     - Formats
     - Default Format
   * - GET
     - List all global settings
     - 200
     - HTML, XML, JSON
     - HTML
   * - POST
     - 
     - 405
     - 
     - 
   * - PUT
     - Update global settings
     - 200
     - XML, JSON
     -
   * - DELETE
     -
     - 405
     -
     -

*Representations*:

- :download:`HTML <representations/settings_html.txt>`
- :download:`XML <representations/settings_xml.txt>`
- :download:`JSON <representations/settings_json.txt>`


``/settings/contact[.<format>]``

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Return Code
     - Formats
     - Default Format
   * - GET
     - List global contact information
     - 200
     - HTML, XML, JSON
     - HTML
   * - POST
     - 
     - 405
     - 
     - 
   * - PUT
     - Update global contact
     - 200
     - XML, JSON
     -
   * - DELETE
     -
     - 405
     -
     -

*Representations*:

- :download:`HTML <representations/contact_html.txt>`
- :download:`XML <representations/contact_xml.txt>`
- :download:`JSON <representations/contact_json.txt>`


Workspaces
----------

A ``workspace`` is a grouping of data stores. More commonly known as a 
namespace, it is commonly used to group data that is related in some way.

.. note::

   For GeoServer 1.x a workspace can be considered the equivalent of a
   namespace, and the two are kept in sync. For example, the namespace
   "topp, http://openplans.org/topp" corresponds to the workspace "topp".

Operations
^^^^^^^^^^

``/workspaces[.<format>]``

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Return Code
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

*Representations*:

- :download:`HTML <representations/workspaces_html.txt>`
- :download:`XML <representations/workspaces_xml.txt>`
- :download:`JSON <representations/workspaces_json.txt>`

``/workspaces/<ws>[.<format>]``

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Return Code
     - Formats
     - Default Format
     - Parameters
   * - GET
     - Returns workspace ``ws``
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
     - :ref:`recurse <workspace_recurse>`

*Representations*:

- :download:`HTML <representations/workspace_html.txt>`
- :download:`XML <representations/workspace_xml.txt>`
- :download:`JSON <representations/workspace_json.txt>`


*Exceptions*:

- GET for a workspace that does not exist -> 404
- PUT that changes name of workspace -> 403
- DELETE against a workspace that is non-empty -> 403

.. _workspace_recurse:

The ``recurse`` parameter is used to recursively delete all resources contained 
by the specified workspace. This includes data stores, coverage stores, 
feature types, etc... Allowable values for this parameter are "true" or "false". 
The default value is "false".

``/workspaces/default[.<format>]``

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Return Code
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

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Return Code
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

*Representations*:

- :download:`HTML <representations/workspaceSettings_html.txt>`
- :download:`XML <representations/workspaceSettings_xml.txt>`
- :download:`JSON <representations/workspaceSettings_json.txt>`



Namespaces
----------

A ``namespace`` is a uniquely identifiable grouping of feature types. A
namespaces is identified by a prefix and a uri.

.. note::

   In GeoServer 1.7.x a namespace is used to group data stores, serving the 
   same purpose as a workspace. In 1.7.x the two are kept in sync. Therefore
   when adding a new namespace a workspace whose name matches the prefix of
   the namespace is implicitly created.

Operations
^^^^^^^^^^

``/namespaces[.<format>]``

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Return Code
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

*Representations*:

- :download:`HTML <representations/namespaces_html.txt>`
- :download:`XML <representations/namespaces_xml.txt>`
- :download:`JSON <representations/namespaces_json.txt>`


``/namespaces/<ns>[.<format>]``

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Return Code
     - Formats
     - Default Format
   * - GET
     - Returns namespace ``ns``
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
     - Modify namespace ``ns``
     - XML, JSON
     -
   * - DELETE
     - 200
     - Delete namespace ``ns``
     - XML, JSON
     -

*Representations*:

- :download:`HTML <representations/namespace_html.txt>`
- :download:`XML <representations/namespace_xml.txt>`
- :download:`JSON <representations/namespace_json.txt>`

*Exceptions*:

- GET for a namespace that does not exist -> 404
- PUT that changes prefix of namespace -> 403
- DELETE against a namespace whose corresponding workspace is non-empty -> 403

``/namespaces/default[.<format>]``

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Return Code
     - Formats
     - Default Format
   * - GET
     - Returns default namespace
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

Data stores
-----------

A ``data store`` is a source of spatial data that is vector based. It can be a 
file in the case of a Shapefile, a database in the case of PostGIS, or a 
server in the case of a remote Web Feature Service.

Operations
^^^^^^^^^^

``/workspaces/<ws>/datastores[.<format>]``

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Return Code
     - Formats
     - Default Format
   * - GET
     - List all data stores in workspace ``ws``
     - 200
     - HTML, XML, JSON
     - HTML
   * - POST
     - Create a new data store
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

*Representations*:

- :download:`HTML <representations/datastores_html.txt>`
- :download:`XML <representations/datastores_xml.txt>`
- :download:`JSON <representations/datastores_json.txt>`

``/workspaces/<ws>/datastores/<ds>[.<format>]``

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Return Code
     - Formats
     - Default Format
     - Parameters
   * - GET
     - Return data store ``ds``
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
     - Modify data store ``ds``
     -
     -
     -
     -
   * - DELETE
     - Delete data store ``ds``
     -
     -
     -
     - :ref:`recurse <datastore_recurse>`

*Representations*:

- :download:`HTML <representations/datastore_html.txt>`
- :download:`XML <representations/datastore_xml.txt>`
- :download:`JSON <representations/datastore_json.txt>`

*Exceptions*:

- GET for a data store that does not exist -> 404
- PUT that changes name of data store -> 403
- PUT that changes workspace of data store -> 403
- DELETE against a data store that contains configured feature types -> 403

.. _datastore_recurse:

The ``recurse`` parameter is used to recursively delete all feature types contained
by the specified data store. Allowable values for this parameter are "true" or  "false". 
The default value is "false".

``/workspaces/<ws>/datastores/<ds>/file[.<extension>]``
``/workspaces/<ws>/datastores/<ds>/url[.<extension>]``
``/workspaces/<ws>/datastores/<ds>/external[.<extension>]``

This operation uploads a file containing spatial data into an existing datastore, or 
creates a new datastore.

.. _extension_parameter:

The ``extension`` parameter specifies the type of data being uploaded. The following 
extensions are supported:

.. list-table::
   :header-rows: 1

   * - Extension
     - Datastore
   * - shp
     - Shapefile
   * - properties
     - Property file
   * - h2
     - H2 Database
   * - spatialite
     - SpatiaLite Database

The ``file``, ``url``, and ``external`` endpoints are used to specify the method that is 
used to upload the file. 

The ``file`` method is used to directly upload a file from a local source. The body of the request is the 
file itself.

The ``url`` method is used to indirectly upload a file from an remote source. The body of the request is
a url pointing to the file to upload. This url must be visible from the server. 

The ``external`` method is used to forgo upload and use an existing file on the server. The body of the 
request is the absolute path to the existing file.
	
.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Return Code
     - Formats
     - Default Format
     - Parameters
   * - GET
     - Get the underlying files for the data store as a zip file with 
       mime type ``application/zip``. *Deprecated*.
     - 200
     - 
     - 
     - 
   * - POST
     - 
     - 405
     - 
     - 
     -
   * - PUT
     - Uploads files to the data store ``ds``, creating it if necessary.
     - 200
     - See :ref:`notes <datastore_file_put_notes>` below.
     - 
     - :ref:`configure <configure_parameter>`, :ref:`target <target_parameter>`, :ref:`update <update_parameter>`, :ref:`charset <charset_parameter>`
   * - DELETE
     -
     - 405
     -
     -
     -

*Exceptions*:

- GET for a data store that does not exist -> 404
- GET for a data store that is not file based -> 404

.. _datastore_file_put_notes:

When the file for a datastore are PUT, it can be as a standalone file, or as
a zipped archive. The standalone file method is only applicable to data stores 
that work from a single file, GML for example. Data stores like Shapefile 
must be sent as a zip archive.

When uploading a zip archive the ``Content-type`` should be set to
``application/zip``. When uploading a standalone file the content type should
be appropriately set based on the file type.

.. _configure_parameter:

The ``configure`` parameter is used to control how the data store is
configured upon file upload. It can take one of the three values "first",
"none", or "all".

- ``first`` - Only setup the first feature type available in the data store. This is the default.
- ``none`` - Do not configure any feature types.
- ``all`` - Configure all feature types.

.. _target_parameter:

The ``target`` parameter is used to control the type of datastore that is created
on the server when the datastore being PUT to does not exist. The allowable values
for this parameter are the same as for the :ref:`extension parameter <extension_parameter>`. 

.. _update_parameter:

The ``update`` parameter is used to control how existing data is handled when the 
file is PUT into a datastore that (a) already exists and (b) already contains a 
schema that matches the content of the file. It can take one of the two values 
"append", or "overwrite".

- ``append`` - Data being uploaded is appended to the existing data. This is the default.
- ``overwrite`` - Data being uploaded replaces any existing data.

.. _charset_parameter:

The ``charset`` parameter is used to specify the character encoding of the file
being uploaded. For example "ISO-8559-1". 

Feature types
-------------

A ``feature type`` is a vector based spatial resource or data set that
originates from a data store. In some cases, like Shapefile, a feature type
has a one-to-one relationship with its data store. In other cases, like
PostGIS, the relationship of feature type to data store is many-to-one, with
each feature type corresponding to a table in the database.

Operations
^^^^^^^^^^

``/workspaces/<ws>/datastores/<ds>/featuretypes[.<format>]``

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Return Code
     - Formats
     - Default Format
     - Parameters
   * - GET
     - List all feature types in datastore ``ds``
     - 200
     - HTML, XML, JSON
     - HTML
     - :ref:`list <list_parameter>`
   * - POST
     - Create a new feature type, see :ref:`notes <featuretypes_post_notes>` below
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

*Representations*:

- :download:`HTML <representations/featuretypes_html.txt>`
- :download:`XML <representations/featuretypes_xml.txt>`
- :download:`JSON <representations/featuretypes_json.txt>`

*Exceptions*:

- GET for a feature type that does not exist -> 404
- PUT that changes name of feature type -> 403
- PUT that changes data store of feature type -> 403

.. _featuretypes_post_notes:

When creating a new feature type via ``POST``, if no underlying dataset with the specified name exists an attempt will be made to create it. This will work only in cases where the underlying data format supports the creation of new types (such as a database). When creating a feature type in this manner the client should include all attribute information in 
the feature type representation.
 
.. _list_parameter:

The ``list`` parameter is used to control the category of feature types that 
are returned. It can take one of the three values "configured", "available", "available_with_geom" or "all".

- ``configured`` - Only setup or configured feature types are returned. This
  is the default value.
- ``available`` - Only unconfigured feature types (not yet setup) but are 
  available from the specified datastore  will be returned.
- ``available_with_geom`` - Same as ``available`` but only includes feature 
  types that have a geometry attribute.
- ``all`` - The union of ``configured`` and ``available``.

``/workspaces/<ws>/datastores/<ds>/featuretypes/<ft>[.<format>]``

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Return Code
     - Formats
     - Default Format
     - Parameters
   * - GET
     - Return feature type ``ft``
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
     - Modify feature type ``ft``
     - 200
     - XML,JSON
     -
     - :ref:`recalculate <featuretype_recalculate>`
   * - DELETE
     - Delete feature type ``ft``
     - 200
     -
     -
     - :ref:`recurse <featuretype_recurse>`

*Representations*:

- :download:`HTML <representations/featuretype_html.txt>`
- :download:`XML <representations/featuretype_xml.txt>`
- :download:`JSON <representations/featuretype_json.txt>`

*Exceptions*:

- GET for a feature type that does not exist -> 404
- PUT that changes name of feature type -> 403
- PUT that changes data store of feature type -> 403

.. _featuretype_recurse:

The ``recurse`` parameter is used to recursively delete all layers that reference
by the specified feature type. Allowable values for this parameter are "true" or  
"false".  The default value is "false".

.. _featuretype_recalculate:

Some properties of feature types are automatically recalculated when necessary.
In particular, the native bounding box is recalculated when the projection or projection policy are changed, and the lat/lon bounding box is recalculated when the native bounding box is recalculated, or when a new native bounding box is explicitly provided in the request.
*The native and lat/lon bounding boxes are never automatically recalculated when they are explicitly included in the request.*
In addition, the client may explicitly request a fixed set of fields to calculate by including a comma-separated list of their names as a parameter named ``recalculate``.  For example:

   * ``recalculate=`` (empty parameter): Do not calculate any fields, regardless of the projection, projection policy, etc.
     This might be useful to avoid slow recalculation when operating against large datasets.
   * ``recalculate=nativebbox``: Recalculate the native boundingbox, do not recalculate the lat/lon bounding box.
   * ``recalculate=nativebbox,latlonbbox``: Recalculate both the native boundingbox and the lat/lon bounding box.


Coverage stores
---------------

A ``coverage store`` is a source of spatial data that is raster based.

Operations
^^^^^^^^^^

``/workspaces/<ws>/coveragestores[.<format>]``

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Return Code
     - Formats
     - Default Format
   * - GET
     - List all coverage stores in workspace ``ws``
     - 200
     - HTML, XML, JSON
     - HTML
   * - POST
     - Create a new coverage store
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

*Representations*:

- :download:`HTML <representations/coveragestores_html.txt>`
- :download:`XML <representations/coveragestores_xml.txt>`
- :download:`JSON <representations/coveragestores_json.txt>`

``/workspaces/<ws>/coveragestores/<cs>[.<format>]``

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Return Code
     - Formats
     - Default Format
     - Parameters
   * - GET
     - Return coverage store ``cs``
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
     - Modify coverage store ``cs``
     -
     -
     -
     -
   * - DELETE
     - Delete coverage store ``ds``
     -
     -
     -
     - :ref:`recurse <coveragestore_recurse>`

*Representations*:

- :download:`HTML <representations/coveragestore_html.txt>`
- :download:`XML <representations/coveragestore_xml.txt>`
- :download:`JSON <representations/coveragestore_json.txt>`

*Exceptions*:

- GET for a coverage store that does not exist -> 404
- PUT that changes name of coverage store -> 403
- PUT that changes workspace of coverage store -> 403
- DELETE against a coverage store that contains configured coverage -> 403

.. _coveragestore_recurse:

The ``recurse`` parameter is used to recursively delete all coverages contained
by the specified coverage store. Allowable values for this parameter are "true" or  "false". 
The default value is "false".

``/workspaces/<ws>/coveragestores/<cs>/file[.<extension>]``

The ``extension`` parameter specifies the type of coverage store. The
following extensions are supported:

.. list-table::
   :header-rows: 1

   * - Extension
     - Coveragestore
   * - geotiff
     - GeoTIFF
   * - worldimage
     - Geo referenced image (JPEG,PNG,TIF)
   * - imagemosaic
     - Image mosaic

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Return Code
     - Formats
     - Default Format
     - Parameters
   * - GET
     - Get the underlying files for the coverage store as a zip file with 
       mime type ``application/zip``.
     - 200
     - 
     - 
     - 
   * - POST
     - 
     - 405
     - 
     - 
     - :ref:`recalculate <coverage_recalculate>`
   * - PUT
     - Creates or overwrites the files for coverage store ``cs``.
     - 200
     - See :ref:`notes <coveragestore_file_put_notes>` below.
     - 
     - :ref:`configure <configure_parameter>`, :ref:`coverageName <coverageName_parameter>`
   * - DELETE
     -
     - 405
     -
     -
     -

*Exceptions*:

- GET for a data store that does not exist -> 404
- GET for a data store that is not file based -> 404

.. _coveragestore_file_put_notes:

When the file for a coveragestore is PUT, it can be as a standalone file, or
as a zipped archive. The standalone file method is only applicable to coverage
stores that work from a single file, GeoTIFF for example. Coverage stores like
Image moscaic must be sent as a zip archive.

When uploading a zip archive the ``Content-type`` should be set to
``application/zip``. When uploading a standalone file the content type should
be appropriately set based on the file type.

.. _coverageName_parameter:

The ``coverageName`` parameter is used to specify the name of the coverage
within the coverage store. This parameter is only relevant if the ``configure``
parameter is not equal to "none". If not specified the resulting coverage will
receive the same name as its containing coverage store.

.. note::

   Currently the relationship between a coverage store and a coverage is one to
   one. However there is currently work underway to support multi-dimensional
   coverages, so in the future this parameter is likely to change.

.. _coverage_recalculate:

Some properties of Coverages are automatically recalculated when necessary.
In particular, the native bounding box is recalculated when the projection or projection policy are changed, and the lat/lon bounding box is recalculated when the native bounding box is recalculated, or when a new native bounding box is explicitly provided in the request.
*The native and lat/lon bounding boxes are never automatically recalculated when they are explicitly included in the request.*
In addition, the client may explicitly request a fixed set of fields to calculate by including a comma-separated list of their names as a parameter named ``recalculate``.  For example:

   * ``recalculate=`` (empty parameter): Do not calculate any fields, regardless of the projection, projection policy, etc.
     This might be useful to avoid slow recalculation when operating against large datasets.
   * ``recalculate=nativebbox``: Recalculate the native boundingbox, do not recalculate the lat/lon bounding box.
   * ``recalculate=nativebbox,latlonbbox``: Recalculate both the native boundingbox and the lat/lon bounding box.


Coverages
---------

A ``coverage`` is a raster based data set which originates from a coverage 
store.

Operations
^^^^^^^^^^

``/workspaces/<ws>/coveragestores/<cs>/coverages[.<format>]``

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Return Code
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
   
*Representations*:

- :download:`HTML <representations/coverages_html.txt>`
- :download:`XML <representations/coverages_xml.txt>`
- :download:`JSON <representations/coverages_json.txt>`

``/workspaces/<ws>/coveragestores/<cs>/coverages/<c>[.<format>]``

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Return Code
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
     - :ref:`recurse <coverage_recurse>`

*Representations*:

- :download:`HTML <representations/coverage_html.txt>`
- :download:`XML <representations/coverage_xml.txt>`
- :download:`JSON <representations/coverage_json.txt>`

*Exceptions*:

- GET for a coverage that does not exist -> 404
- PUT that changes name of coverage -> 403
- PUT that changes coverage store of coverage -> 403

.. _coverage_recurse:

The ``recurse`` parameter is used to recursively delete all layers that reference
by the specified coverage. Allowable values for this parameter are "true" or  
"false".  The default value is "false".

Styles
------

A ``style`` describes how a resource (feature type or coverage) should be 
symbolized or rendered by a Web Map Service. In GeoServer styles are 
specified with :ref:`SLD <styling>`.

Operations
^^^^^^^^^^

``/styles[.<format>]``

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Return Code
     - Formats
     - Default Format
     - Parameters
   * - GET
     - Return all styles
     - 200
     - HTML, XML, JSON
     - HTML
     -
   * - POST
     - Create a new style
     - 201 with ``Location`` header
     - SLD, XML, JSON
       See :ref:`notes <sld_post_put>` below
     -
     - :ref:`name <name_parameter>`
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
     - :ref:`purge <purge_parameter>`

*Representations*:

- :download:`HTML <representations/styles_html.txt>`
- :download:`XML <representations/styles_xml.txt>`
- :download:`JSON <representations/styles_json.txt>`

.. _sld_post_put:

When POSTing or PUTing a style as SLD, the ``Content-type`` header should be
set to ``application/vnd.ogc.sld+xml``.

.. _name_parameter:

The ``name`` parameter specifies the name to be given to the style. This 
option is most useful when POSTing a style in SLD format, and an appropriate
name can be not be inferred from the SLD itself.

``/styles/<s>[.<format>]``

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Return Code
     - Formats
     - Default Format
   * - GET
     - Return style ``s``
     - 200
     - SLD, HTML, XML, JSON
     - HTML
   * - POST
     - 
     - 405
     -
     -
   * - PUT
     - Modify style ``s`` 
     - 200
     - SLD, XML, JSON
       See :ref:`notes <sld_post_put>` above
     - 
   * - DELETE
     - Delete style ``s``
     - 200
     -
     -

.. _purge_parameter:

The ``purge`` parameter specifies whether the underlying SLD file for the style should be deleted on disk. It is specified as a boolean value ``(true|false)``. When set to ``true`` the underlying file will be deleted. 

*Representations*:

- :download:`SLD <representations/style_sld.txt>`
- :download:`HTML <representations/style_html.txt>`
- :download:`XML <representations/style_xml.txt>`
- :download:`JSON <representations/style_json.txt>`

*Exceptions*:

- GET for a style that does not exist -> 404
- PUT that changes name of style -> 403
- DELETE against style which is referenced by existing layers -> 403

``/workspaces/<ws>/styles[.<format>]``

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Return Code
     - Formats
     - Default Format
     - Parameters
   * - GET
     - Return all styles within workspace ``ws``
     - 200
     - HTML, XML, JSON
     - HTML
     -
   * - POST
     - Create a new style within workspace ``ws``
     - 201 with ``Location`` header
     - SLD, XML, JSON
       See :ref:`notes <sld_post_put>` below
     -
     - :ref:`name <name_parameter>`
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
     - :ref:`purge <purge_parameter>`

*Representations*:

- :download:`HTML <representations/styles_html.txt>`
- :download:`XML <representations/styles_xml.txt>`
- :download:`JSON <representations/styles_json.txt>`

``/workspaces/<ws>/styles/<s>[.<format>]``

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Return Code
     - Formats
     - Default Format
   * - GET
     - Return style ``s`` within workspace ``ws``
     - 200
     - SLD, HTML, XML, JSON
     - HTML
   * - POST
     - 
     - 405
     -
     -
   * - PUT
     - Modify style ``s`` within workspace ``ws``
     - 200
     - SLD, XML, JSON
       See :ref:`notes <sld_post_put>` above
     - 
   * - DELETE
     - Delete style ``s`` within workspace ``ws``
     - 200
     -
     -

*Representations*:

 - :download:`SLD <representations/style_sld.txt>`
 - :download:`HTML <representations/style_html.txt>`
 - :download:`XML <representations/style_xml.txt>`
 - :download:`JSON <representations/style_json.txt>`

Layers
------

A ``layer`` is a *published* resource (feature type or coverage). 

.. note::

   In GeoServer 1.x a layer can considered the equivalent of a feature type or
   a coverage. In GeoServer 2.x, the two will be separate entities, with the 
   relationship from a feature type to a layer being one-to-many.

Operations
^^^^^^^^^^

``/layers[.<format>]``

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Return Code
     - Formats
     - Default Format
   * - GET
     - Return all layers
     - 200
     - HTML, XML, JSON
     - HTML
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

*Representations*:

- :download:`HTML <representations/layers_html.txt>`
- :download:`XML <representations/layers_xml.txt>`
- :download:`JSON <representations/layers_json.txt>`

``/layers/<l>[.<format>]``

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Return Code
     - Formats
     - Default Format
     - Parameters
   * - GET
     - Return layer ``l``
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
     - Modify layer ``l`` 
     - 200
     - XML,JSON
     -
     - 
   * - DELETE
     - Delete layer ``l``
     - 200
     -
     -
     - :ref:`recurse <layer_recurse>`

*Representations*:

- :download:`HTML <representations/layer_html.txt>`
- :download:`XML <representations/layer_xml.txt>`
- :download:`JSON <representations/layer_json.txt>`

*Exceptions*:

- GET for a layer that does not exist -> 404
- PUT that changes name of layer -> 403
- PUT that changes resource of layer -> 403

.. _layer_recurse:

The ``recurse`` parameter is used to recursively delete all resources referenced
by the specified layer. Allowable values for this parameter are "true" or  
"false".  The default value is "false".

``/layers/<l>/styles[.<format>]``

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Return Code
     - Formats
     - Default Format
   * - GET
     - Return all styles for layer ``l``
     - 200
     - SLD, HTML, XML, JSON
     - HTML
   * - POST
     - Add a new style to layer ``l``
     - 201, with ``Location`` header
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

Layer groups
------------

A ``layer group`` is a grouping of layers and styles that can be accessed as a 
single layer in a WMS GetMap request. A Layer group is often referred to as a 
"base map".

Operations
^^^^^^^^^^

``/layergroups[.<format>]``

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Return Code
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

*Representations*:

- :download:`HTML <representations/layergroups_html.txt>`
- :download:`XML <representations/layergroups_xml.txt>`
- :download:`JSON <representations/layergroups_json.txt>`

``/layergroups/<lg>[.<format>]``

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Return Code
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

*Representations*:

- :download:`HTML <representations/layergroup_html.txt>`
- :download:`XML <representations/layergroup_xml.txt>`
- :download:`JSON <representations/layergroup_json.txt>`

*Exceptions*:

- GET for a layer group that does not exist -> 404
- POST that specifies layer group with no layers -> 400
- PUT that changes name of layer group -> 403 

``/workspaces/<ws>/layergroups[.<format>]``

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Return Code
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

*Representations*:

- :download:`HTML <representations/layergroups_html.txt>`
- :download:`XML <representations/layergroups_xml.txt>`
- :download:`JSON <representations/layergroups_json.txt>`

``/workspaces/<ws>/layergroups/<lg>[.<format>]``

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Return Code
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

*Representations*:

- :download:`HTML <representations/layergroup_html.txt>`
- :download:`XML <representations/layergroup_xml.txt>`
- :download:`JSON <representations/layergroup_json.txt>`


Fonts 
------

This operation provides the list of ``fonts`` available in GeoServer and can be useful to verify if a ``font`` used in a SLD file is available before uploading it.


``/fonts[.<format>]``

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Return Code
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

- :download:`XML <representations/fonts_xml.txt>`
- :download:`JSON <representations/fonts_json.txt>`


Freemarker Templates
---------------------

Freemarker is a simple yet powerful template engine that GeoServer uses whenever developer allowed user customization of outputs.

Operations
^^^^^^^^^^

- ``/templates/<template>.ftl``
- ``/workspaces/<ws>/templates/<template>.ftl``
- ``/workspaces/<ws>/datastores/<ds>/templates/<template>.ftl``
- ``/workspaces/<ws>/datastores/<ds>/featuretypes/<f>/templates/<template>.ftl``
- ``/workspaces/<ws>/coveragestores/<cs>/templates/<template>.ftl``
- ``/workspaces/<ws>/coveragestores/<cs>/coverages/<c>/templates/<template>.ftl``

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Return Code
     - Formats
     - Default Format
   * - GET
     - Return a template
     - 200
     - 
     - 	
   * - PUT
     - Insert or update a template
     - 405
     - 
     - 
   * - DELETE
     - Delete a template
     - 405
     - 
     - 

	 
- ``/templates[.<format>]``
- ``/workspaces/<ws>/templates[.<format>]``
- ``/workspaces/<ws>/datastores/<ds>/templates[.<format>]``
- ``/workspaces/<ws>/datastores/<ds>/featuretypes/<f>/templates[.<format>]``
- ``/workspaces/<ws>/coveragestores/<cs>/templates[.<format>]``
- ``/workspaces/<ws>/coveragestores/<cs>/coverages/<c>/templates[.<format>]``

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Return Code
     - Formats
     - Default Format
   * - GET
     - Return templates 
     - 200
     - HTML, XML, JSON
     - HTML
 
*Representations*:

- :download:`HTML <representations/templates_html.txt>`
- :download:`XML <representations/templates_xml.txt>`
- :download:`JSON <representations/templates_json.txt>`


OWS Services
-------------

GeoServer includes several types of OGC services like WCS, WFS and
WMS, commonly referred to as "OWS" services. These services can be
global for the whole GeoServer instance or local to a particular
workspace. In this last case, they are usually called "Virtual
Services".


Operations
^^^^^^^^^^

``/services/wcs/settings[.<format>]``

.. list-table::
   :header-rows: 1



   * - Method
     - Action
     - Return Code
     - Formats
     - Default Format
   * - GET
     - Return global wcs settings
     - 200
     - XML, JSON
     - HTML
   * - POST
     -
     - 405
     - 
     - 
   * - PUT
     - Modify global wcs settings
     - 200
     - 
     - 
   * - DELETE
     -
     - 405
     - 
     - 




*Representations*:

- :download:`HTML <representations/wcs_html.txt>`
- :download:`XML <representations/wcs_xml.txt>`
- :download:`JSON <representations/wcs_json.txt>`


``/services/wcs/workspaces/<ws>/settings[.<format>]``


.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Return Code
     - Formats
     - Default Format
   * - GET
     - Return wcs settings for workspace <ws>
     - 200
     - HTML, XML, JSON
     - HTML
   * - POST
     - 
     - 405
     -
     -
   * - PUT
     - Create or modify wcs settings for workspace <ws>
     - 200
     - XML,JSON
     - 
   * - DELETE
     - Delete wcs settings for workspace <ws>
     - 200
     -
     -

*Representations*:

- :download:`HTML <representations/wcsWS_html.txt>`
- :download:`XML <representations/wcsWS_xml.txt>`
- :download:`JSON <representations/wcsWS_json.txt>`


``/services/wfs/settings[.<format>]``

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Return Code
     - Formats
     - Default Format
   * - GET
     - Return global wfs settings
     - 200
     - HTML, XML, JSON
     - HTML
   * - POST
     - 
     - 405
     -
     -
   * - PUT
     - Modify global wfs settings
     - 200
     - XML,JSON
     - 
   * - DELETE
     - 
     - 405
     -
     -

*Representations*:

- :download:`HTML <representations/wfs_html.txt>`
- :download:`XML <representations/wfs_xml.txt>`
- :download:`JSON <representations/wfs_json.txt>`


``/services/wfs/workspaces/<ws>/settings[.<format>]``


.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Return Code
     - Formats
     - Default Format
   * - GET
     - Return wfs settings for workspace <ws>
     - 200
     - HTML, XML, JSON
     - HTML
   * - POST
     - 
     - 405
     -
     -
   * - PUT
     - Modify wfs settings for workspace <ws>
     - 200
     - XML,JSON
     - 
   * - DELETE
     - Delete wfs settings for workspace <ws>
     - 200
     -
     -

*Representations*:

- :download:`HTML <representations/wfsWS_html.txt>`
- :download:`XML <representations/wfsWS_xml.txt>`
- :download:`JSON <representations/wfsWS_json.txt>`


``/services/wms/settings[.<format>]``

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Return Code
     - Formats
     - Default Format
   * - GET
     - Return global wms settings
     - 200
     - HTML, XML, JSON
     - HTML
   * - POST
     - 
     - 405
     -
     -
   * - PUT
     - Modify global wms settings
     - 200
     - XML,JSON
     - 
   * - DELETE
     - 
     - 405
     -
     -

*Representations*:

- :download:`HTML <representations/wms_html.txt>`
- :download:`XML <representations/wms_xml.txt>`
- :download:`JSON <representations/wms_json.txt>`


``/services/wms/workspaces/<ws>/settings[.<format>]``


.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Return Code
     - Formats
     - Default Format
   * - GET
     - Return wms settings for workspace <ws>
     - 200
     - HTML, XML, JSON
     - HTML
   * - POST
     - 
     - 405
     -
     -
   * - PUT
     - Modify wms settings for workspace <ws>
     - 200
     - XML,JSON
     - 
   * - DELETE
     - Delete wms settings for workspace <ws>
     - 200
     -
     -

*Representations*:

- :download:`HTML <representations/wmsWS_html.txt>`
- :download:`XML <representations/wmsWS_xml.txt>`
- :download:`JSON <representations/wmsWS_json.txt>`


Configuration reloading 
----------------------- 

Reloads the catalog and configuration from disk. This operation is used to 
reload GeoServer in cases where an external tool has modified the on disk 
configuration. This operation will also force GeoServer to drop any internal 
caches and reconnect to all data stores.

``/reload``

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Return Code
     - Formats
     - Default Format
   * - GET
     -
     - 405
     - 
     - 
   * - POST
     - Reloads the configuration from disk
     - 200
     - 
     - 
   * - PUT
     - Reloads the configuration from disk
     - 200
     - 
     - 
   * - DELETE
     -
     - 405
     -
     -
     
Resource reset 
----------------------- 

Resets all store/raster/schema caches and starts fresh. This operation is used to 
force GeoServer to drop all caches and stores and reconnect fresh to each of them first time they 
are needed by a request.
This is useful in case the stores themselves cache some information about the data structures
they manage that changed in the meantime.

``/reset``

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Return Code
     - Formats
     - Default Format
   * - GET
     -
     - 405
     - 
     - 
   * - POST
     - Reloads the configuration from disk
     - 200
     - 
     - 
   * - PUT
     - Reloads the configuration from disk
     - 200
     - 
     - 
   * - DELETE
     -
     - 405
     -
     -
