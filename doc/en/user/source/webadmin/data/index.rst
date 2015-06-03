.. _webadmin_data:

Data
====


This section is the largest and perhaps the most important section of the Web Administration Interface. 
It describes the core configuration data types that GeoServer uses to access and publish geospatial information.
Each subsection describes a data type page which provides add, view, edit, and delete capabilities.

.. toctree::
   :maxdepth: 2

   workspaces
   stores
   layers
   layergroups
   styles
   
**Using Data type List views**


The main page for each data type is a list view showing the items of that data type configured in the GeoServer instance.
The page displays links to the data type items, and where applicable their parent data types.
To facilitate working with large sets of items, the list allows sorting and searching across all items in the data type.

In the example below, the :guilabel:`Layers` list page displays a table of layers and their parent Store and Workspace. 

.. figure:: ../images/data_layers.png
   :align: center

   *Layers list page*

**Sorting**

To sort a data type alphabetically, click the column header. 

.. figure:: ../images/data_sort.png
   :align: center

   *Unsorted (left) and sorted (right) columns*

**Searching**

To search data type items, enter the search string in the search box and click Enter. GeoServer will search the data type for items that match your query, and display a list view showing the search results.
Searching is useful for working with data types that contain a large number of items.

.. figure:: ../images/data_search_results.png
   :align: center
   
   *Search results for the query "top" on the Workspace page*




