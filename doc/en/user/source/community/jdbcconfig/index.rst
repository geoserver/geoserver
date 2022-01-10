.. _community_jdbcconfig:

JDBCConfig
==========

The ``JDBCConfig module`` enhances the scalibility performance of the GeoServer Catalog.
It allows externalising the storage of the Catalog configuration objects (such as workspaces, stores, layers) to a Relational Database Management System,
rather than using xml files in the :ref:`datadir`. This way the Catalog can support access to unlimited numbers of those configuration objects efficiently.

.. toctree::
   :maxdepth: 2

   installing
   configuration

