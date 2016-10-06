.. _rest_api_coveragestores:

Coverage stores
===============

A ``coverage store`` contains raster format spatial data.

``/workspaces/<ws>/coveragestores[.<format>]``
----------------------------------------------

Controls all coverage stores in a given workspace.

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status code
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


``/workspaces/<ws>/coveragestores/<cs>[.<format>]``
---------------------------------------------------

Controls a particular coverage store in a given workspace.

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status code
     - Formats
     - Default Format
     - Parameters
   * - GET
     - Return coverage store ``cs``
     - 200
     - HTML, XML, JSON
     - HTML
     - :ref:`quietOnNotFound <rest_api_coveragestores_quietOnNotFound>`
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
     - Delete coverage store ``cs``
     -
     -
     -
     - :ref:`recurse <rest_api_coveragestores_recurse>`, :ref:`purge <rest_api_coveragestores_purge>`

Exceptions
~~~~~~~~~~

.. list-table::
   :header-rows: 1

   * - Exception
     - Status code
   * - GET for a coverage store that does not exist
     - 404
   * - PUT that changes name of coverage store
     - 403
   * - PUT that changes workspace of coverage store
     - 403
   * - DELETE against a coverage store that contains configured coverage
     - 403

Parameters
~~~~~~~~~~

.. _rest_api_coveragestores_recurse:

``recurse``
^^^^^^^^^^^

The ``recurse`` parameter recursively deletes all layers referenced by the coverage store. Allowed values for this parameter are "true" or "false". The default value is "false".


.. _rest_api_coveragestores_purge:

``purge``
^^^^^^^^^

The ``purge`` parameter is used to customize the delete of files on disk (in case the underlying reader implements a delete method).
It can take one of the three values:

* ``none``-(*Default*) Do not delete any store's file from disk.
* ``metadata``-Delete only auxiliary files and metadata. It's recommended when data files (such as granules) should not be deleted from disk.
* ``all``-Purge everything related to that store (metadata and granules).

.. _rest_api_coveragestores_quietOnNotFound:

``quietOnNotFound``
^^^^^^^^^^^^^^^^^^^^

The ``quietOnNotFound`` parameter avoids to log an Exception when the coverage store is not present. Note that 404 status code will be returned anyway.

``/workspaces/<ws>/coveragestores/<cs>/file[.<extension>]``
-----------------------------------------------------------

This end point allows a file containing spatial data to be added (via a POST or PUT) into an existing coverage store, or will create a new coverage store if it doesn't already exist. In case of coverage stores containing multiple coverages (e.g., mosaic of NetCDF files) all the coverages will be configured unless ``configure=false`` is specified as a parameter.

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status code
     - Formats
     - Default Format
     - Parameters
   * - GET
     - *Deprecated*. Get the underlying files for the coverage store as a zip file with MIME type ``application/zip``.
     - 200
     - 
     - 
     - 
   * - POST
     - If the coverage store is a simple one (e.g. GeoTiff) it will return a 405, if the coverage store is a structured one (e.g., mosaic) it will harvest the specified files into it, which in turn will integrate the files into the store. Harvest meaning is store dependent, for mosaic the new files will be added as new granules of the mosaic, and existing files will get their attribute updated, other stores might have a different behavior.
     - 405 if the coverage store is a simple one, 200 if structured and the harvest operation succeeded
     - 
     - 
     - :ref:`recalculate <rest_api_coveragestores_recalculate>`, :ref:`filename <rest_api_coveragestores_filename>`
   * - PUT
     - Creates or overwrites the files for coverage store ``cs``
     - 200
     - :ref:`See note below <rest_api_coveragestores_file_put>`
     - 
     - :ref:`configure <rest_api_coveragestores_configure>`, :ref:`coverageName <rest_api_coveragestores_coveragename>`
   * - DELETE
     -
     - 405
     -
     -
     -

.. _rest_api_coveragestores_file_put:

.. note::

   A file can be PUT to a coverage store as a standalone or zipped archive file. Standalone files are only suitable for coverage stores that work with a single file such as GeoTIFF store. Coverage stores that work with multiple files, such as the ImageMosaic store, must be sent as a zip archive.

   When uploading a standalone file, set the ``Content-type`` appropriately based on the file type. If you are loading a zip archive, set the ``Content-type`` to ``application/zip``.

Exceptions
~~~~~~~~~~

.. list-table::
   :header-rows: 1

   * - Exception
     - Status code
   * - GET for a data store that does not exist
     - 404
   * - GET for a data store that is not file based
     - 404

Parameters
~~~~~~~~~~

``extension``
^^^^^^^^^^^^^

The ``extension`` parameter specifies the type of coverage store. The
following extensions are supported:

.. list-table::
   :header-rows: 1

   * - Extension
     - Coverage store
   * - geotiff
     - GeoTIFF
   * - worldimage
     - Georeferenced image (JPEG, PNG, TIFF)
   * - imagemosaic
     - Image mosaic

.. _rest_api_coveragestores_configure:

``configure``
^^^^^^^^^^^^^

The ``configure`` parameter controls how the coverage store is configured upon file upload. It can take one of the three values:

* ``first``—(*Default*) Only setup the first feature type available in the coverage store.
* ``none``—Do not configure any feature types.
* ``all``—Configure all feature types.

.. _rest_api_coveragestores_coveragename:

``coverageName``
^^^^^^^^^^^^^^^^

The ``coverageName`` parameter specifies the name of the coverage within the coverage store. This parameter is only relevant if the ``configure`` parameter is not equal to "none". If not specified the resulting coverage will receive the same name as its containing coverage store.

.. note:: At present a one-to-one relationship exists between a coverage store and a coverage. However, there are plans to support multidimensional coverages, so this parameter may change.

.. _rest_api_coveragestores_recalculate:

``recalculate``
^^^^^^^^^^^^^^^

The ``recalculate`` parameter specifies whether to recalculate any bounding boxes for a coverage. Some properties of coverages are automatically recalculated when necessary. In particular, the native bounding box is recalculated when the projection or projection policy is changed. The lat/long bounding box is recalculated when the native bounding box is recalculated or when a new native bounding box is explicitly provided in the request. (The native and lat/long bounding boxes are not automatically recalculated when they are explicitly included in the request.) In addition, the client may explicitly request a fixed set of fields to calculate by including a comma-separated list of their names in the ``recalculate`` parameter. For example:

* ``recalculate=`` (empty parameter)—Do not calculate any fields, regardless of the projection, projection policy, etc. This might be useful to avoid slow recalculation when operating against large datasets.
* ``recalculate=nativebbox``—Recalculate the native bounding box, but do not recalculate the lat/long bounding box.
* ``recalculate=nativebbox,latlonbbox``—Recalculate both the native bounding box and the lat/long bounding box.

.. _rest_api_coveragestores_filename:

``filename``
^^^^^^^^^^^^^^^

The ``filename`` parameter specifies the target file name for a file that needs to harvested as part of a mosaic. This is important to avoid clashes and to make sure the
right dimension values are available in the name for multidimensional mosaics to work.

* ``filename=`NCOM_wattemp_000_20081102T0000000_12.tiff` Set the uploaded file name to ``NCOM_wattemp_000_20081102T0000000_12.tiff`` 
