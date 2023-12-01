.. _datadir_structure:

Structure of the data directory
===============================

This section gives an overview of the structure and contents of the GeoServer data directory. 

This is not intended to be a complete reference to the GeoServer configuration information, since generally the data directory configuration files should not be accessed directly.

Instead, the :ref:`web_admin` can be used to view and modify the configuration, and for programmatic access and manipulation the :ref:`REST API <rest>` should be used.

The directories that do contain user-modifiable content are:

* :file:`logs`
* :file:`palettes`
* :file:`templates`
* :file:`user_projections`
* :file:`www`

Top-level XML files
-------------------

The top-level XML files contain information about the services and various global options for the server instance. 

.. list-table::
   :header-rows: 1

   * - File
     - Description
   * - :file:`global.xml`
     - Contains settings common to all services, such as contact information, JAI settings, character sets and verbosity.
   * - :file:`logging.xml`
     - Specifies logging parameters, such as logging level, logfile location, and whether to log to stdout.  
   * - :file:`wcs.xml` 
     - Contains the service metadata and various settings for the WCS service.
   * - :file:`wfs.xml`
     - Contains the service metadata and various settings for the WFS service.
   * - :file:`wms.xml` 
     - Contains the service metadata and various settings for the WMS service.


``workspaces``
--------------

The ``workspaces`` directory contain metadata about the layers published by GeoServer. It contains a directory for each defined **workspace**.

Each workspace directory contains directories for the **datastores** defined in it. Each datastore directory contains directories for the **layers** defined for the datastore.

Each layer directory contains a :file:`layer.xml` file, and either a :file:`coverage.xml` or a :file:`featuretype.xml` file depending on whether the layer represents a raster or vector dataset.

``data``
--------

The ``data`` directory can be used to store file-based geospatial datasets being served as layers.

.. note:: This should not be confused with the main GeoServer data directory, which is the parent to this directory.

This directory is commonly used to store shapefiles and raster files, but can be used for any data that is file-based.

The main benefit of storing data files under the ``data`` directory is portability.

Consider a shapefile stored external to the data directory at a location :file:`C:\\gis_data\\foo.shp`. The ``datastore`` entry in :file:`catalog.xml` for this shapefile would look like the following:

.. code-block:: xml

   <datastore id="foo_shapefile">
      <connectionParams>
        <parameter name="url" value="file://C:/gis_data/foo.shp" />
      </connectionParams>
    </datastore>

Now consider trying to port this data directory to another host running GeoServer. The location :file:`C:\\gis_data\\foo.shp` probably does not exist on the second host. So either the file must be copied to this location on the new host, or :file:`catalog.xml` must be changed to reflect a new location.

This problem can be avoided by storing :file:`foo.shp` in the ``data`` directory. In this case the ``datastore`` entry in :file:`catalog.xml` becomes:

.. code-block:: xml

   <datastore id="foo_shapefile">
     <connectionParams>
       <parameter name="url" value="file:data/foo.shp"/>
     </connectionParams>
   </datastore>

The ``value`` attribute is rewritten to be relative to the ``data`` directory. This location independence allows the entire data directory to be copied to a new host and used directly with no additional changes.

``demo``
--------

The ``demo`` directory contains files which define the sample requests available in the :ref:`Demo Request <demos>` page.

``gwc``
-------

The ``gwc`` directory holds the tile cache created by the embedded :ref:`GeoWebCache <gwc>` service.

``layergroups``
---------------

The ``layergroups`` directory contains configuration information for the defined layergroups.

``logs``
--------

The ``logs`` directory contains configuration information for the various defined logging profiles, and the default :file:`geoserver.log` log file.

.. note:: See also the :ref:`Logging <logging>` section for more details.

``palettes``
------------

The ``palettes`` directory is used to store pre-computed **Image Palettes**. Image palettes are used by the GeoServer WMS as way to reduce the size of produced images while maintaining image quality.

.. note:: See also the :ref:`tutorials_palettedimages` tutorial for more information.

``security``
------------

The ``security`` directory contains the files used to configure the GeoServer security subsystem. This includes a set of property files which define access roles, along with the services and data each role is authorized to access.

.. note:: See also the :ref:`security` section for more information.

``styles``
----------

The ``styles`` directory contains files which contain styling information used by the GeoServer WMS.

.. note:: See also the :ref:`styling` section for more information.

For each SLD file in this directory there is a corresponding XML file:

.. code-block:: xml
   
   <style>
     <id>StyleInfoImpl--570ae188:124761b8d78:-7fe1</id>
     <name>grass</name>
     <sldVersion>
       <version>1.0.0</version>
     </sldVersion>
     <filename>grass_poly.sld</filename>
     <legend>
       <width>32</width>
       <height>32</height>
       <format>image/png</format>
       <onlineResource>grass_fill.png</onlineResource>
     </legend>
   </style>

The ``styles`` directory can also be used to host support files referenced during style configuration:

* Support files: SLD files can reference external graphics. This is useful when supplying your own icons in the form of image files or TrueType font files. Without any path information supplied, the default will be this directory.
* A style external graphic is dynamically created for use as a legend. The contents of the directory is published allowing clients to access the legends used. When running GeoServer on localhost, an image file ``image.png`` stored in this directory can be referenced in a browser using ``http:/<host:port>/geoserver/styles/image.png``.

``templates``
-------------

The ``templates`` directory contains files used by the GeoServer templating subsystem. Templates are used to customize the output of various GeoServer operations.

.. note:: See also :ref:`tutorial_freemarkertemplate` for more information..

``user_projections``
--------------------

The ``user_projections`` directory contains a file called :file:`epsg.properties` which is used to define custom spatial reference systems that are not part of the official `EPSG database <http://www.epsg.org/CurrentDB.html>`_.

.. note:: See also :ref:`crs_custom` for more information.

``www``
-------

The ``www`` directory is used to allow GeoServer to serve files like a regular web server. While not a replacement for a full web server, this can be useful for serving client-side mapping applications. The contents of this directory are served at ``http:/<host:port>/geoserver/www``. 

.. note:: See also :ref:`tutorials_staticfiles` for more information.

