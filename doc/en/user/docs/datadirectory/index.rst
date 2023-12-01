.. _datadir:

GeoServer data directory
========================

The GeoServer **data directory** is the location in the file system where GeoServer stores its configuration information.

The configuration defines what data is served by GeoServer, where it is stored, and how services interact with and serve the data. The data directory also contains a number of support files used by GeoServer for various purposes.

For production use, it is recommended to define an *external* data directory (outside the application) to make it easier to upgrade. The :ref:`datadir_setting` section describes how to configure GeoServer to use an existing data directory.

.. note:: Since GeoServer provides both an interactive interface (via the :ref:`web admin interface <web_admin>`) and programmatic interface (through the :ref:`REST API <rest>`) to manage configuration, most users do not need to know about the :ref:`internal structure of the data directory <datadir_structure>`, but an overview is provided below.

.. toctree::
   :maxdepth: 1

   location
   setting
   structure
   migrating
   configtemplate
