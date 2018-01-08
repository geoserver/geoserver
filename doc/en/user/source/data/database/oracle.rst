.. _data_oracle:

Oracle
======

.. note:: GeoServer does not come built-in with support for Oracle; it must be installed through an extension.  Proceed to :ref:`oracle_install` for installation details.

`Oracle Spatial and Locator <http://www.oracle.com/technology/products/spatial/index.html>`_ are the spatial components of Oracle.
**Locator** is provided with all Oracle versions, but has limited spatial functions.
**Spatial** is Oracle's full-featured spatial offering, but requires a specific license to use.

.. _oracle_install:

Installing the Oracle extension
-------------------------------

.. warning:: Due to licensing requirements, not all files are included with the extension.  To install Oracle support, it is necessary to download additional files. 

#. Download the Oracle extension from the `GeoServer download page <http://geoserver.org/download>`_.

   .. warning:: Make sure to match the version of the extension to the version of the GeoServer instance!

#. Extract the contents of the archive into the ``WEB-INF/lib`` directory of the GeoServer installation.

#. Get the Oracle JDBC driver from either your Oracle installation (e.g. ``ojdbc6.jar``, ``ojdbc7.jar``)
   or download them from `the Oracle JDBC driver distribution page <http://www.oracle.com/technetwork/database/features/jdbc/index-091264.html>`_

Consider replacing the Oracle JDBC driver
-----------------------------------------

The Oracle data store zip file comes with ``ojdbc4.jar``, an old, Oracle 10 compatible JDBC driver that normally works fine with 11g as well.
However, minor glitches have been observed with 11g (issues computing layer bounds when session initiation scripts are in use) and the driver
has not been tested with 12i.

If you encounter functionality or performance issues it is advised to remove this driver and download the latest version from the Oracle web site.

Adding an Oracle datastore
--------------------------

Once the extension is properly installed :guilabel:`Oracle` appears as an option in the :guilabel:`Vector Data Sources` list when creating a new data store.

.. figure:: images/oraclecreate.png
   :align: center

   *Oracle in the list of data sources*

Configuring an Oracle datastore
-------------------------------

.. figure:: images/oracleconfigure.png
   :align: center

   *Configuring an Oracle datastore*

.. list-table::
   :widths: 20 80

   * - **Option**
     - **Description**
   * - ``host``
     - The Oracle server host name or IP address.
   * - ``port``
     - The port on which the Oracle server is accepting connections (often this is port 1521).
   * - ``database``
     - The name of the database to connect to.  
       By default this is interpreted as a SID name.  To connect to a Service, prefix the name with a ``/``.
   * - ``schema``
     - The database schema to access tables from. Setting this value greatly increases the speed at which the data store displays its publishable tables and views, so it is advisable to set this.
   * - ``user``
     - The name of the user to use when connecting to the database.
   * - ``password``
     - The password to use when connecting to the database.  Leave blank for no password.
   * - ``max connections``
       ``min connections``
       ``fetch size``
       ``Connection timeout``
       ``validate connections``
     - Connection pool configuration parameters. See :ref:`connection_pooling` for details.
   * - ``Loose bbox``
     - 	Controls how bounding box filters are made against geometries in the database. See the :ref:`oracle_loose_bbox` section below.
   * - ``Metadata bbox``
     - 	Flag controlling the use of MDSYS.USER_SDO_GEOM_METADATA or MDSYS.ALL_SDO_GEOM_METADATA table for bounding box calculations, this brings a better performance if the views access is fast and the bounds are configured right in the tables default is false  

Connecting to an Oracle cluster
-------------------------------

In order to connect to an Oracle RAC one can use an almost full JDBC url as the ``database``, provided it starts with ``(`` it will be used verbatim and options "host" and "port" will be ignored. Here is an example "database" value used to connect to an Oracle RAC::

   (DESCRIPTION=(LOAD_BALANCE=on)(ADDRESS=(PROTOCOL=TCP)(HOST=host1) (PORT=1521))(ADDRESS=(PROTOCOL=TCP)(HOST=host2) (PORT=1521))(CONNECT_DATA=(SERVICE_NAME=service)))

More information about this syntax can be found in the `Oracle documentation <http://docs.oracle.com/cd/B28359_01/java.111/e10788/rac.htm#CHDCDFAC>`_.
     
Connecting to a SID or a Service
````````````````````````````````

Recent versions of Oracle support connecting to a database via either a SID name or a Service name.
A SID connection descriptor has the form:  ``host:port:database``, 
while a Service connection descriptor has the format ``host:port/database``.
GeoServer uses the SID form by default. To connect via a Service,
prefix the ``database`` name configuration entry with a ``/``.

Connecting to database through LDAP
`````````````````````````````````````

For instance if you want to establish a connection with the jdbc thin driver through LDAP, you can use following connect string for the input field ``database`` 
``ldap://[host]:[Port]/[db],cn=OracleContext,dc=[oracle_ldap_context]``.

If you are using referrals, enable it by placing a jndi.properties file in geoserver's CLASSPATH, which is in geoserver/WEB-INF/classes.
This property file contains:

   java.naming.referral=follow


.. _oracle_loose_bbox:

Using loose bounding box
````````````````````````

When the ``Loose bbox`` option is set, only the bounding box of database geometries is used in spatial queries.  This results in a significant performance gain. The downside is that some geometries may be reported as intersecting a BBOX when they actually do not.

If the primary use of the database is through the :ref:`WMS` this flag can be set safely, since querying more geometries does not have any visible effect. However, if using the :ref:`WFS` and making use of BBOX filtering capabilities, this flag should not be set.

Using the geometry metadata table
`````````````````````````````````

The Oracle data store by default looks at the ``MDSYS.USER_SDO*`` and ``MDSYS.ALL_SDO*`` views
to determine the geometry type and native SRID of each geometry column.
Those views are automatically populated with information about the geometry columns stored in tables that the current
user owns (for the ``MDSYS.USER_SDO*`` views) or can otherwise access (for the ``MDSYS.ALL_SDO*`` views).

There are a few issues with this strategy:

  * if the connection pool user cannot access the tables (because :ref:`impersonation <data_sqlsession>` is used) 
    the MDSYS views will be empty, making it impossible to determine both the geometry type and the native SRID
  * the geometry type can be specified only while building the spatial indexes, as an index constraint.  However 
    such information is often not included when creating the indexes
  * the views are populated dynamically based on the current user. If the database has thousands of tables and users
    the views can become very slow
    
Starting with GeoServer 2.1.4 the administrator can address the above issues by manually creating a geometry metadata table
describing each geometry column.
Its presence is indicated via the Oracle datastore connection parameter named *Geometry metadata table*
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
to be classified, and falls back on the MDSYS views only if the table does not contain any information.

Configuring an Oracle database with JNDI
----------------------------------------

See :ref:`tomcat_jndi` for a guide on setting up an Oracle connection using JNDI.
