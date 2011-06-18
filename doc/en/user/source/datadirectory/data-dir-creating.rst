.. _data_dir_creating:

Creating a New Data Directory
=============================

The easiest way to create a new data directory is to copy one that comes with a standard GeoServer installation. 

If GeoServer is running in **Standalone** mode the data directory is located at ``<installation root>/data_dir``.

.. note::

   On Windows systems the ``<installation root>`` is located at ``C:\Program Files\GeoServer 1.7.0``. 

If GeoServer is running in **Web Archive** mode inside of a servlet container, the data directory is located at ``<web application root>/data``. 

Once the data directory has been found copy it to a new external location. To point a GeoServer instance at the new data directory proceed to the next section :ref:`data_dir_setting`.

