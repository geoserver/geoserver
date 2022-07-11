.. _tool_bulk:

Bulk Load tool
==============

The :guilabel:`Catalog Bulk Load Tool` is used to duplicate GeoServer configuration (workspaces, stores, layers) for testing. The tool can also be used to make a single duplicate for experimenting with configuration and optimization.

.. figure:: img/bulk_tool.png
   
   Catalog Bulk Load Tool

Duplicating Configuration
-------------------------

1. Navigate to :menuselection:`Tools --> Catalog Bulk Load Tool`

2. Select the item to copy:
   
   * :guilabel:`Workspace and Namespace`
   * :guilabel:`Store`
   * :guilabel:`Resource and Layer`
   
3. Fill in the :guilabel:`# of times to duplicate`.

4. Provide a :guilabel:`Suffix to append`

5. Choose to recursively copy:

   * Recursively copying a workspace will duplicate all stores and layers contained in the workspace
   * Recursively copying a store will copy all layers published by the store
   
6. Press :guilabel:`Start` to begin duplicating
