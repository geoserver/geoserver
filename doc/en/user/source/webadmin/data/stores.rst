.. _webadmin_stores:

Stores
======

A store connects to a data source that contains raster or vector data. A data source can be a file or group of files, a table in a database, a single raster file, or a directory (for example, a Vector Product Format library). Using the store construct means that connection parameters are defined once, rather than for each dataset in a source. As such, it is necessary to register a store before configuring datasets within it.

.. figure:: ../images/data_stores.png
   :align: center
   
   *Stores View*

While there are many potential formats for data sources, there are only four kinds of stores. For raster data, a store can be a file. For vector data, a store can be a file, database, or server. 

.. list-table::
   :widths: 15 85 

   * - **Type Icon**
     - **Description**
   * - .. image:: ../images/data_stores_type1.png
     - raster data in a file
   * - .. image:: ../images/data_stores_type3.png
     - vector data in a file
   * - .. image:: ../images/data_stores_type2.png
     - vector data in a database 
   * - .. image:: ../images/data_stores_type5.png
     - vector server (web feature server)
     

Editing a Store
---------------

To view and edit a store, click a store name. The exact contents of this page depend on the specific format of the store. See the sections :ref:`data_vector`, :ref:`data_raster`, and :ref:`data_database` for information about specific data formats. The example shows the configuration for the ``nurc:ArcGridSample`` store.

.. figure:: ../images/data_stores_edit.png
   :align: center
   
   *Editing a raster data store*

Basic Store Info
^^^^^^^^^^^^^^^^
The basic information is common for all formats. 

* **Workspace** - the store is assigned to the selected workspace
* **Data Source Name** - the store name as listed on the view page
* **Description** - (optional) a description that displays in the administration interface
* **Enabled** - enables or disables access to the store, along with all datasets defined for it

Connection Parameters
^^^^^^^^^^^^^^^^^^^^^
The connection parameters vary depending on data format.

Adding a Store
--------------

The buttons for adding and removing a store can be found at the top of the Stores page. 

.. figure:: ../images/data_stores_add_remove.png
   :align: center
   
   *Buttons to add and remove stores*

To add a store, select the :guilabel:`Add new Store` button. You will be prompted to choose a data source. GeoServer natively supports many formats (with more available via extensions). Click the appropriate data source to continue. 

.. figure:: ../images/data_stores_chooser.png
   :align: center
   
   *Choosing the data source for a new store*

The next page configures the store. Since connection parameters differ across data sources, the exact contents of this page depend on the store's specific format. See the sections :ref:`data_vector`, :ref:`data_raster`, and :ref:`data_database` for information on specific data formats.
The example below shows the ArcGrid raster configuration page.

.. figure:: ../images/data_stores_add.png
   :align: center
   
   *Configuration page for an ArcGrid raster data source*

Removing a Store
----------------
   
To remove a store, click the store's corresponding check box. Multiple stores can be selected for batch removal.

.. figure:: ../images/data_stores_delete.png
   :align: center
   
   *Stores selected for removal*

Click the :guilabel:`Remove selected Stores` button. You will be asked to confirm the deletion of the configuration for the store(s) and all resources defined under them. Selecting :guilabel:`OK` removes the store(s), and returns to the Stores page.

.. figure:: ../images/data_stores_delete_confirm.png
   :align: center   

   *Confirm removal of stores*

















