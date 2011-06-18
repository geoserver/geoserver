.. _webadmin_stores:

Stores
======

A store connects to a data source that contains raster or vector data. A data source can be a file or group of files such as a table in a database, a single file (such as a shapefile), or a directory (such as Vector Product Format library).  The store construct is used so that connection parameters are defined once, rather than for each piece of data in a source.  As such, it is necessary to register a store before loading any data.

.. figure:: ../images/data_stores.png
   :align: center
   
   *Stores View*

While there are many potential formats for data source, there are only four types of stores.  For raster data, a store can be a file.  For vector data, a store can be a file, database, or server.  

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

In order to view and edit a store, click on a store name.  The exact contents of this page will depend on the specific format chosen.  (See the section on :ref:`data` for information about specific data formats.)  In the example below we have the contents of the ``nurc:ArcGridSample`` store.

.. figure:: ../images/data_stores_edit.png
   :align: center
   
   *Editing a raster data store*

While connection parameters will vary depending on data format, some the basic information is common across formats.  The Workspace drop down menu lists all registered workspaces.  The store is assigned to the selected workspace (``nurc``).  :guilabel:`Data Source Name` is the store name as listed on the view page.  The :guilabel:`Description` is optional and only displays in the administration interface.  :guilabel:`Enabled` allows you to enable or disable access to the store, along with all data defined in it. 

Adding a Store
--------------

The buttons for adding and removing a workspace can be found at the top of the Stores page. 

.. figure:: ../images/data_stores_add_remove.png
   :align: center
   
   *Buttons to add and remove stores*

To add a workspace, select the :guilabel:`Add new Store` button.  You will be prompted to choose a data source.  GeoServer natively supports many formats (with more available via extensions).  Click the appropriate data source to continue. 

.. figure:: ../images/data_stores_chooser.png
   :align: center
   
   *Choosing the data source for a new store*

The next page will configure the store. (The example below shows the ArcGrid raster configuration page.)  However, since connection parameters differ across data sources, the exact contents of this page depend on the store's specific format. Please see the section on :ref:`data` for information on specific data formats.

.. figure:: ../images/data_stores_add.png
   :align: center
   
   *Configuration page for an ArcGrid raster data source*

Removing a Store
----------------
   
In order to remove a store, click on the store's corresponding check box.  Multiple stores can be checked for batch removal.

.. figure:: ../images/data_stores_delete.png
   :align: left
   
   *Stores checked for deletion*

Click the :guilabel:`Remove selected Stores` button.  You will be asked to confirm the deletion of the the data within each store. Selecting :guilabel:`OK` removes the store(s), and will redirect to the main Stores page.

.. figure:: ../images/data_stores_delete_confirm.png
   :align: left
   
   *Confirm deletion of stores*

















