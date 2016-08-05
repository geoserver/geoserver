.. _data_database:

Databases
=========

This section discusses the database data sources that GeoServer can access.

The standard GeoServer installation supports accessing the following databases:

.. toctree::
   :maxdepth: 1

   postgis
   h2


Other data sources are supplied as GeoServer extensions.  
Extensions are downloadable modules that add functionality to GeoServer.  
Extensions are available at the `GeoServer download page <http://geoserver.org/download>`_.

.. warning:: The extension version must match the version of the GeoServer instance.

.. toctree::
   :maxdepth: 1

   arcsde
   db2
   mysql
   oracle
   sqlserver
   teradata
   
GeoServer provides extensive facilities for controlling how databases are accessed.
These are covered in the following sections.

.. toctree::
   :maxdepth: 1

   connection-pooling
   jndi
   sqlview
   primarykey
   sqlsession


   
