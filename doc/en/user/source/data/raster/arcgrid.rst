.. _data_arcgrid:

ArcGrid
=======

.. note:: GeoServer does not come built-in with support for ArcGrid; it must be installed through an extension. Proceed to :ref:`arcgrid_install` for installation details.

ArcGrid is a coverage file format created by ESRI.

.. _arcgrid_install:

Installing the ArcGrid extension
-------------------------------------

#. Visit the :website:`website download <download>` page, locate your release, and download:

   * |release| :download_extension:`arcgrid`
   * |version| :nightly_extension:`arcgrid`

   .. warning:: Ensure to match plugin (example |release| above) version to the version of the GeoServer instance.

#. Extract the contents of the archive into the :file:`WEB-INF/lib` directory of the GeoServer installation.

Adding an ArcGrid data store
----------------------------

Once the extension is properly installed :guilabel:`ArcGrid` will be an option in the :guilabel:`Raster Data Sources` list when creating a new data store.

.. figure:: images/arcgridcreate.png
   :align: center

   *ArcGrid in the list of raster data stores*

Configuring a ArcGrid data store
--------------------------------

.. figure:: images/arcgridconfigure.png
   :align: center

   *Configuring an ArcGrid data store*

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