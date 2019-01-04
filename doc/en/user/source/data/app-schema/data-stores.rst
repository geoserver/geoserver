.. _app-schema.data-stores:

Data Stores
===========

The app-schema :ref:`app-schema.mapping-file` requires you to specify your data sources in the ``sourceDataStores`` section. For GeoServer simple features, these are configured using the web interface, but because app-schema lacks a web configuration interface, data stores must be configured by editing the mapping file.

Many configuration options may be externalised through the use of :ref:`app-schema.property-interpolation`.


The DataStore element
---------------------

A ``DataStore`` configuration consists of

* an ``id``, which is an opaque identifier used to refer to the data store elsewhere in a mapping file, and
* one or more ``Parameter`` elements, which each contain the ``name`` and ``value`` of one parameter, and are used to configure the data store.

An outline of the ``DataStore`` element::

   <DataStore>
        <id>datastore</id>
        <parameters>
            <Parameter>
                <name>...</name>
                <value>...</value>
            </Parameter>
            ...
        </parameters>
   </DataStore>

Parameter order is not significant.
 
 
Database options
----------------

Databases such as PostGIS, Oracle, and ArcSDE share some common or similar configuration options.

========================    ====================================================    ============================================================================
``name``                    Meaning                                                 ``value`` examples
========================    ====================================================    ============================================================================
``dbtype``                  Database type                                           ``postgisng``, ``Oracle``, ``arcsde``
``host``                    Host name or IP address of database server              ``database.example.org``, ``192.168.3.12``
``port``                    TCP port on database server                             Default if omitted: ``1521`` (Oracle), ``5432`` (PostGIS), ``5151`` (ArcSDE)
``database``                PostGIS/Oracle database
``instance``                ArcSDE instance
``schema``                  The database schema
``user``                    The user name used to login to the database server
``passwd``                  The password used to login to the database server
``Expose primary keys``     Columns with primary keys available for mapping         Default is ``false``, set to ``true`` to use primary key columns in mapping
========================    ====================================================    ============================================================================

PostGIS
-------

Set the parameter ``dbtype`` to ``postgisng`` to use the PostGIS NG (New Generation) driver bundled with GeoServer 2.0 and later.

Example::

    <DataStore>
        <id>datastore</id>
        <parameters>
            <Parameter>
                <name>dbtype</name>
                <value>postgisng</value>
            </Parameter>
            <Parameter>
                <name>host</name>
                <value>postgresql.example.org</value>
            </Parameter>
            <Parameter>
                <name>port</name>
                <value>5432</value>
            </Parameter>
            <Parameter>
                <name>database</name>
                <value>test</value>
            </Parameter>
            <Parameter>
                <name>user</name>
                <value>test</value>
            </Parameter>
            <Parameter>
                <name>passwd</name>
                <value>test</value>
            </Parameter>
        </parameters>
    </DataStore>

.. note:: PostGIS  support is included in the main GeoServer bundle, so a separate plugin is not required.


Oracle
------

Set the parameter ``dbtype`` to ``Oracle`` to use the Oracle Spatial NG (New Generation) driver compatible with GeoServer 2.0 and later.

Example::

    <DataStore>
        <id>datastore</id>
        <parameters>
            <Parameter>
                <name>dbtype</name>
                <value>Oracle</value>
            </Parameter>
            <Parameter>
                <name>host</name>
                <value>oracle.example.org</value>
            </Parameter>
            <Parameter>
                <name>port</name>
                <value>1521</value>
            </Parameter>
            <Parameter>
                <name>database</name>
                <value>demodb</value>
            </Parameter>
            <Parameter>
                <name>user</name>
                <value>orauser</value>
            </Parameter>
            <Parameter>
                <name>passwd</name>
                <value>s3cr3t</value>
            </Parameter>
        </parameters>
    </DataStore>


.. note:: You must install the Oracle plugin to connect to Oracle Spatial databases.


ArcSDE
------

This example connects to an ArcSDE database::

    <DataStore>
        <id>datastore</id>
        <parameters>
            <Parameter>
                <name>dbtype</name>
                <value>arcsde</value>
            </Parameter>
            <Parameter>
                <name>server</name>
                <value>arcsde.example.org</value>
            </Parameter>
            <Parameter>
                <name>port</name>
                <value>5151</value>
            </Parameter>
            <Parameter>
                <name>instance</name>
                <value>sde</value>
            </Parameter>
            <Parameter>
                <name>user</name>
                <value>demo</value>
            </Parameter>
            <Parameter>
                <name>password</name>
                <value>s3cr3t</value>
            </Parameter>
            <Parameter>
                <name>datastore.allowNonSpatialTables</name>
                <value>true</value>
            </Parameter>
        </parameters>
    </DataStore>


The use of non-spatial tables aids delivery of application schemas that use non-spatial properties.

.. note:: You must install the ArcSDE plugin to connect to ArcSDE databases.


Shapefile
---------

Shapefile data sources are identified by the presence of a parameter ``url``, whose value should be the file URL for the .shp file. 

In this example, only the ``url`` parameter is required. The others are optional::

    <DataStore>
        <id>shapefile</id>
        <parameters>
            <Parameter>
                <name>url</name>
                <value>file:/D:/Workspace/shapefiles/VerdeRiverBuffer.shp</value>
            </Parameter>
            <Parameter>
                <name>memory mapped buffer</name>
                <value>false</value>
            </Parameter>
            <Parameter>
                <name>create spatial index</name>
                <value>true</value>
            </Parameter>
            <Parameter>
                <name>charset</name>
                <value>ISO-8859-1</value>
            </Parameter>
        </parameters>
    </DataStore>


.. note:: The ``url`` in this case is an example of a Windows filesystem path translated to URL notation.

.. note:: Shapefile support is included in the main GeoServer bundle, so a separate plugin is not required.


Property file
-------------

Property files are configured by specifying a ``directory`` that is a ``file:`` URI.

* If the directory starts with ``file:./`` it is relative to the mapping file directory. (This is an invalid URI, but it works.)

For example, the following data store is used to access property files in the same directory as the mapping file::

    <DataStore>
        <id>propertyfile</id>
        <parameters>
            <Parameter>
                <name>directory</name>
                <value>file:./</value>
            </Parameter>
        </parameters>
    </DataStore>

A property file data store contains *all* the feature types stored in .properties files in the directory. For example, if the directory contained River.properties and station.properties, the data store would be able to serve them as the feature types ``River`` and ``station``. Other file extensions are ignored.

.. note:: Property file support is included in the main GeoServer bundle, so a separate plugin is not required.


JNDI
----

Defining a JDBC data store with a ``jndiReferenceName`` allows you to use a connection pool provided by your servlet container. This allows detailed configuration of connection pool parameters and sharing of connections between data sources, and even between servlets.

To use a JNDI connection provider:

#. Specify a ``dbtype`` parameter to to indicate the database type. These values are the same as for the non-JNDI examples above.
#. Give the ``jndiReferenceName`` you set in your servlet container. Both the abbreviated form ``jdbc/oracle`` form, as in Tomcat, and the canonical form ``java:comp/env/jdbc/oracle`` are supported.

This example uses JNDI to obtain Oracle connections::

    <DataStore>
        <id>datastore</id>
        <parameters>
            <Parameter>
                <name>dbtype</name>
                <value>Oracle</value>
            </Parameter>
            <Parameter>
                <name>jndiReferenceName</name>
                <value>jdbc/oracle</value>
            </Parameter>
        </parameters>
    </DataStore>

Your servlet container my require you to add a ``resource-ref`` section at the end of  your ``geoserver/WEB-INF/web.xml``. (Tomcat requires this, Jetty does not.) For example::

    <resource-ref>
        <description>Oracle Spatial Datasource</description>
        <res-ref-name>jdbc/oracle</res-ref-name>
        <res-type>javax.sql.DataSource</res-type>
        <res-auth>Container</res-auth>
    </resource-ref>

Here is an example of a Tomcat 6 context in ``/etc/tomcat6/server.xml`` that includes an Oracle connection pool::

    <Context
        path="/geoserver"
        docBase="/usr/local/geoserver"
        crossContext="false"
        reloadable="false">
        <Resource
            name="jdbc/oracle"
            auth="Container"
            type="javax.sql.DataSource"
            url="jdbc:oracle:thin:@YOUR_DATABASE_HOSTNAME:1521:YOUR_DATABASE_NAME"
            driverClassName="oracle.jdbc.driver.OracleDriver"
            username="YOUR_DATABASE_USERNAME"
            password="YOUR_DATABASE_PASSWORD"
            maxActive="20"
            maxIdle="10"
            minIdle="0"
            maxWait="10000"
            minEvictableIdleTimeMillis="300000"
            timeBetweenEvictionRunsMillis="300000"
            numTestsPerEvictionRun="20"
            poolPreparedStatements="true"
            maxOpenPreparedStatements="100"
            testOnBorrow="true"
            validationQuery="SELECT SYSDATE FROM DUAL" />
    </Context>
    
Firewall timeouts can silently sever idle connections to the database and cause GeoServer to hang. If there is a firewall between GeoServer and the database, a connection pool configured to shut down idle connections before the firewall can drop them will prevent GeoServer from hanging. This JNDI connection pool is configured to shut down idle connections after 5 to 10 minutes. 

See also :ref:`tomcat_jndi`.


Expose primary keys
-------------------

By default, GeoServer conceals the existence of database columns with a primary key. To make such columns available for use in app-schema mapping files, set the data store parameter ``Expose primary keys`` to ``true``::

    <Parameter>
        <name>Expose primary keys</name>
       <value>true</value>
    </Parameter>

This is known to work with PostGIS, Oracle, and JNDI data stores.

MongoDB
-------

The data store configuration for a MongoDB data base will look like this:  

.. code-block:: xml 

    <sourceDataStores>
        <DataStore>
            <id>data_source</id>
            <parameters>
                <Parameter>
                    <name>data_store</name>
                    <value>MONGO_DB_URL</value>
                </Parameter>
                <Parameter>
                    <name>namespace</name>
                    <value>NAME_SPACE</value>
                </Parameter>
                <Parameter>
                    <name>schema_store</name>
                    <value>SCHEMA_STORE</value>
                </Parameter>
                <Parameter>
                    <name>data_store_type</name>
                    <value>complex</value>
                </Parameter>
            </parameters>
        </DataStore>
    </sourceDataStores>

Check :ref:`mongo_tutorial` for a more detailed description about how to use MongoDB with app-schema.

.. note:: You must install the MongoDB plugin to connect to MongoDB databases.