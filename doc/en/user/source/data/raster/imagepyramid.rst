.. _data_imagepyramid:

ImagePyramid
=============

.. note:: GeoServer does not come built-in with support for Image Pyramid; it must be installed through an extension. Proceed to :ref:`imagepyramid_install` for installation details.

An image pyramid is several layers of an image rendered at various image sizes, to be shown at different zoom levels.

.. _imagepyramid_install:

Installing the ImagePyramid extension
-------------------------------------

#. Visit the :website:`website download <download>` page, locate your release, and download: 
   
   * |release| :download_extension:`pyramid`
   * |version| :nightly_extension:`pyramid`
   
   .. warning:: Ensure to match plugin (example |release| above) version to the version of the GeoServer instance.
   
#. Extract the contents of the archive into the :file:`WEB-INF/lib` directory of the GeoServer installation.

Adding an ImagePyramid data store
---------------------------------

Once the extension is properly installed :guilabel:`ImagePyramid` will be an option in the :guilabel:`Raster Data Sources` list when creating a new data store.

.. figure:: images/imagepyramidcreate.png
   :align: center

   *ImagePyramid in the list of raster data stores*

Configuring an ImagePyramid data store
--------------------------------------

.. figure:: images/imagepyramidconfigure.png
   :align: center

   *Configuring an ImagePyramid data store*

.. list-table::
   :widths: 20 80

   * - **Option**
     - **Description**
   * - ``Workspace``
     - 
   * - ``Data Source Name``
     - 
   * - ``Description``
     - 
   * - ``Enabled``
     -  
   * - ``URL``
     - 