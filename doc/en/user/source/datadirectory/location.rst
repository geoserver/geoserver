.. _datadir_location:

Data directory default location
===============================

If GeoServer is running in **standalone** mode (via an installer or a binary) the data directory is located at ``<installation root>/data_dir``.

.. list-table::
   :header-rows: 1

   * - Standalone platform
     - Default/typical location
   * - Windows (except XP)
     - |data_directory_win|
   * - Windows XP
     - |data_directory_winXP|
   * - Mac OS X
     - |data_directory_mac|
   * - Linux (Tomcat)
     - |data_directory_linux|

If GeoServer is running as a **web archive** inside of a custom-deployed application server, the data directory is by default located at ``<web application root>/data``. 

Creating a new data directory
-----------------------------

The easiest way to create a new data directory is to copy an existing one. 

Once the data directory has been located, copy it to a new location. To point a GeoServer instance at the new data directory proceed to the next section :ref:`datadir_setting`.
