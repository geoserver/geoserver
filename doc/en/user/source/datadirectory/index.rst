.. _data_directory:

GeoServer Data Directory
========================

The GeoServer *data directory* is the location on the file system where GeoServer stores all of its configuration. This configuration defines such things as: What data is served by GeoServer? Where is that data is located? How should services such as WFS and WMS interact with and server that data? And more. The data directory also contains a number of support files used by GeoServer for various purposes. 

In general users does not need to know about the structure of the data directory.  But it is a good idea to define an external data directory when going to production, to make it easier to upgrade.

Or to learn how to create a directory for a GeoServer installation jump to the :ref:`data_dir_creating` section.  :ref:`data_dir_creating` contains details on how to make a GeoServer use an existing data directory.  To learn more about the structure of the GeoServer data directory continue onto the :ref:`data_dir_structure` section. 

.. toctree::
   :maxdepth: 2

   data-dir-creating/
   data-dir-setting/
   data-dir-structure/
   migrating/
   
