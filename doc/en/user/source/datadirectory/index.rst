.. _data_directory:

GeoServer data directory
========================

The GeoServer **data directory** is the location in the file system where GeoServer stores its configuration information. 
The configuration defines things such as what data is served by GeoServer, where it is stored, and how services such as WFS and WMS interact with and serve the data. 
The data directory also contains a number of support files used by GeoServer for various purposes. 

For production use, it is a good idea to define an external data directory for GeoServer instances, to make it easier to upgrade.
To learn how to create a data directory for a GeoServer installation see the :ref:`data_dir_creating` section.  
:ref:`data_dir_setting` describes how to configure GeoServer to use an existing data directory.  

Since GeoServer provides both interactive and programmatic interfaces 
to manage confiuration information, 
in general users do not need to know about the internal structure of the data directory.
As background, an overview is provided in the :ref:`data_dir_structure` section. 

.. toctree::
   :maxdepth: 2

   data-dir-creating/
   data-dir-setting/
   data-dir-structure/
   migrating/
   
