.. _data_sqlserver:

Microsoft SQL Server and SQL Azure
==================================

.. note:: GeoServer does not come built-in with support for SQL Server; it must be installed through an extension. Proceed to :ref:`sqlserver_install` for installation details.

Microsoft's `SQL Server <http://www.microsoft.com/sqlserver/2008>`_ is a relational database with spatial functionality. SQL Azure is the database option provided in the Azure cloud solution which is in many respects similar to SQL Server 2008.

Supported versions
------------------

The extension supports SQL Server 2008 and SQL Azure.

.. _sqlserver_install:

Installing the SQL Server extension
-----------------------------------

.. warning:: Due to licensing requirements, not all files are included with the extension.  To install SQL Server support, it is necessary to download additional files. 

GeoServer files
```````````````

#. Download the SQL Server extension from the `GeoServer download page <http://geoserver.org/download>`_.

   .. warning:: Make sure to match the version of the extension to the version of the GeoServer instance!

#. Extract the contents of the archive into the ``WEB-INF/lib`` directory of the GeoServer installation.

#. Restart the GeoServer to load the extension.

Microsoft files
```````````````

#. Navigate to the download page for `Microsoft JDBC Drivers for SQL Server <https://www.microsoft.com/en-us/download/details.aspx?id=11774>`_.

#. Extract the contents of the archive.

#. Copy the file ``sqljdbc4.jar`` to the ``WEB-INF/lib`` directory of the GeoServer installation.

#. If GeoServer is installed on Windows, additionally copy ``auth\x86\sqljdbc_auth.dll`` and ``xa\x86\sqljdbc_xa.dll`` to ``C:\Windows\System32``.

Adding a SQL Server database
----------------------------

Once the extension is properly installed ``SQL Server`` will show up as an option when creating a new data store.

.. figure:: images/sqlservercreate.png
   :align: center

   *SQL Server in the list of vector data sources*

Configuring a SQL Server data store
-----------------------------------

.. figure:: images/sqlserverconfigure.png
   :align: center

   *Configuring a SQL Server data store*

.. list-table::
   :widths: 20 80

   * - ``host``
     - The sql server instance host name or ip address, only. Note that ``server\instance`` notation is not accepted - specify the port below, instead, if you have a non-default instance.
   * - ``port``
     - The port on which the SQL server instance is accepting connections.  See the :ref:`note <port_notes>` below.
   * - ``database``
     - The name of the database to connect to. Might be left blank if the user connecting to SQL Server has a "default database" set in the user configuration
   * - ``schema``
     - The database schema to access tables from (optional).
   * - ``user``
     - The name of the user to connect to the oracle database as.
   * - ``password``     
     - The password to use when connecting to the database. Leave blank for no password.
   * - ``max connections``
 
       ``min connections``

     - Connection pool configuration parameters. See the :ref:`connection_pooling` section for details. If you are connecting to SQL Azure make sure to set the ``validate connections`` flag as SQL Azure closes inactive connections after a very short delay.

.. _port_notes:

Determining the port used by the SQL Server instance
````````````````````````````````````````````````````

You can determine the port in use by connecting to your SQL server instance using some other software, and then using :command:`netstat` to display details on network connections.  In the following example on a Windows PC, the port is 2646 ::

    C:\>netstat -a | find "sql1"
    TCP   DPI908194:1918   maittestsql1.dpi.nsw.gov.au:2646   ESTABLISHED


Using the geometry metadata table
`````````````````````````````````

The SQL server data store can determine the geometry type and native SRID of a particular column only by data inspection,
by looking at the first row in the table. Of course this is error prone, and works only if there is data in the table.
The administrator can address the above issue by manually creating a geometry metadata table describing each geometry column.
Its presence is indicated via the SQL Server datastore connection parameter named *Geometry metadata table*
(which may be a simple table name or a schema-qualified one).
The table has the following structure (the table name is flexible, just specify the one chosen in the data store connection parameter)::

	CREATE TABLE GEOMETRY_COLUMNS(
	   F_TABLE_SCHEMA VARCHAR(30) NOT NULL, 
	   F_TABLE_NAME VARCHAR(30) NOT NULL, 
	   F_GEOMETRY_COLUMN VARCHAR(30) NOT NULL, 
	   COORD_DIMENSION INTEGER, 
	   SRID INTEGER NOT NULL, 
	   TYPE VARCHAR(30) NOT NULL,
	   UNIQUE(F_TABLE_SCHEMA, F_TABLE_NAME, F_GEOMETRY_COLUMN),
	   CHECK(TYPE IN ('POINT','LINE', 'POLYGON', 'COLLECTION', 'MULTIPOINT', 'MULTILINE', 'MULTIPOLYGON', 'GEOMETRY') ));
	   
When the table is present the store first searches it for information about each geometry column
to be classified, and falls back on data inspection only if the table does not contain any information.


