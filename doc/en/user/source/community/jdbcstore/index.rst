.. _community_jdbcstore:

JDBCStore
==========

The ``JDBCStore module`` allows efficient sharing of configuration data in a clustered deployment of GeoServer. It allows externalising the storage of all configuration resources to a Relational Database Management System, rather than using the default File System based :ref:`datadir`. This way the multiple instances of GeoServer can use the same Database and therefore share in the same configuration.

.. toctree::
   :maxdepth: 2

   installing
   configuration

