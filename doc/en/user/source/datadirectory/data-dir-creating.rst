.. _data_dir_creating:

Creating a New Data Directory
=============================

The easiest way to create a new data directory is to copy one that comes with a standard GeoServer installation. 

If GeoServer is running in **Standalone** mode the data directory is located at ``<installation root>/data_dir``.

================ ========================
Platform         Example Location
================ ========================
Windows          |data_directory_win|
Windows XP       |data_directory_winXP|
Mac OSX          |data_directory_mac|
================ ========================

If GeoServer is running as **Web Archive** mode inside of a servlet container, the data directory is located at ``<web application root>/data``. 

================ ========================
Platform         Example Location
================ ========================
Linux            |data_directory_linux|
================ ========================

Once the data directory has been found copy it to a new external location. To point a GeoServer instance at the new data directory proceed to the next section :ref:`data_dir_setting`.

