.. _data_geopkg_vector:

GeoPackage
==========

`GeoPackage <http://www.opengeospatial.org/projects/groups/geopackageswg/>`_ is an SQLite based standard format that is able to hold multiple vector and raster data layers in a single file.

GeoPackage files can be used both as Raster Data Stores as well as Vector Data Stores (so that both kinds of layers can published).


Adding a GeoPackage Vector Data Store
-------------------------------------

When the extension has been installed, :guilabel:`GeoPackage` will be an option in the :guilabel:`Vector Data Sources` list when creating a new data store.

.. figure:: images/geopackagecreate.png
   :align: center

   *GeoPackage in the list of vector data stores*
.. figure:: images/geopackageconfigure.png
   :align: center

   *Configuring a GeoPackage Vector data store*

.. list-table::
   :widths: 20 80

   * - **Option**
     - **Description**
   * - :guilabel:`database`
     - URI specifying geopackage file.
   * - :guilabel:`user`
     - User to access database.
   * - :guilabel:`passwd`
     - Password to access database.
   * - :guilabel:`namespace`
     - Namespace to be associated with the database.  This field is altered by changing the workspace name.
   * - :guilabel:`max connections`
     - Maximum amount of open connections to the database.
   * - :guilabel:`min connections`
     - Minimum number of pooled connections.
   * - :guilabel:`fetch size`
     - Number of records read with each interaction with the database.
   * - :guilabel:`Connection timeout`
     - Time (in seconds) the connection pool will wait before timing out.
   * - :guilabel:`validate connections`
     - Checks the connection is alive before using it.

When finished, click :guilabel:`Save`.
