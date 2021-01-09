.. _data_wpsjdbc:

WPS JDBC
========

.. note:: GeoServer does not come built-in with support for WPS JDBC; it must be installed through an extension. Proceed to :ref:`wpsjdbc_install` for installation details.

.. _wpsjdbc_install:

Installing the WPS JDBC extension
---------------------------------

#. Download the WPS JDBC extension from the `GeoServer download page 
   <http://geoserver.org/download>`_.

   .. warning:: Make sure to match the version of the extension to the version of the GeoServer instance!

#. Extract the contents of the archive into the ``WEB-INF/lib`` directory of the GeoServer installation.

Configuring the WPS JDBC properties
-----------------------------------

#. Create a file named `jdbcstatusstore.props` into the `GEOSERVER_DATA_DIR` root

#. Update the sample content below accordingly to your connection parameters

    .. code-block::

        user=postgres
        port=5432
        password=******
        passwd=******
        host=localhost
        database=gsstore
        driver=org.postgresql.Driver
        dbtype=postgis

#. Restart GeoServer

Share the WPS Execution Dir among the cluster nodes
---------------------------------------------------

Typically the WPS JDBC plugin is useful when setting up a GeoServer cluster.

The plugin allows sharing of the execution status among the nodes of the cluster.

Nevertheless, this won't be sufficient. You will need to share the Execution folder too, in order to allow the different instances to correctly retrieve the executions results.

#. Create a shared folder that all the nodes can reach somehow, e.g. by using `nfs`

#. From the GeoServer Admin dashboard, go to the `WPS` menu and edit the Resource Storage Directory accordingly

.. figure:: images/wps-resource-storage-directory.png
   :align: center

   *WPS JDBC shared Resource Storage Directory*
