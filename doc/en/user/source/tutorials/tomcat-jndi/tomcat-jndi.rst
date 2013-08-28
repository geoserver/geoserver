.. _tomcat_jndi:

Setting up a JNDI connection pool with Tomcat
=============================================


.. warning:: The screenshots on this tutorial have not yet been updated for the 2.0.x user interface.  But most all the rest of the information should be valid, and the user interface is roughly the same.

This tutorial walks the reader through the procedures necessary to setup a Oracle JNDI connection pool in Tomcat 6 and how to retrieve it from GeoServer

Tomcat setup
------------

In order to setup a connection pool Tomcat needs a JDBC driver and the necessary pool configurations.

First off, you need to find the JDBC driver for your database. Most often it is distributed on the web site of your DBMS provider, or available in the installed version of your database.
For example, a Oracle XE install on a Linux system provides the driver at  :file:`/usr/lib/oracle/xe/app/oracle/product/10.2.0/server/jdbc/lib/ojdbc14.jar`, and that file needs to be copied into Tomcat shared libs directory, :file:`{TOMCAT_HOME}/lib`

Once that is done, the Tomcat configuration file :file:`{TOMCAT_HOME}/conf/context.xml` needs to be edited in order to setup the connection pool. In the case of a local Oracle XE the setup might look like:

.. code-block:: xml
  
  <Context>
     ...
     <Resource name="jdbc/oralocal" auth="Container" type="javax.sql.DataSource"
               url="jdbc:oracle:thin:@localhost:1521:xe"
               driverClassName="oracle.jdbc.driver.OracleDriver"
               username="dbuser" password="dbpasswd"
               maxActive="20" maxIdle="3" maxWait="10000"
               poolPreparedStatements="true"
               maxOpenPreparedStatements="100"
               validationQuery="SELECT SYSDATE FROM DUAL" />
  </Context>


The example sets up a connection pool connecting to the local Oracle XE instance. 
The pool configuration shows is quite full fledged:

* at most 20 active connections (max number of connection that will ever be used in parallel)
* at most 3 connections kept in the pool unused
* prepared statement pooling (very important for good performance)
* at most 100 prepared statements in the pool
* a validation query that double checks the connection is still alive before actually using it (this is not necessary if there is guarantee the connections will never drop, either due to the server forcefully closing them, or to network/maintenance issues).

.. warning:: Modify following settings only if you really know what you are doing. Using too low values for ``removedAbandonedTimeout`` and ``minEvictableIdleTimeMillis`` may result in connection failures, if so try to setup ``logAbandoned`` to ``true`` and check your ``catalina.out`` log file.

Other parameters to setup connection pool:

* timeBetweenEvictionRunsMillis	(default -1) The number of milliseconds to sleep between runs of the idle object evictor thread. When non-positive, no idle object evictor thread will be run.
* numTestsPerEvictionRun	(default 3) The number of objects to examine during each run of the idle object evictor thread (if any).
* minEvictableIdleTimeMillis	(default 1000 * 60 * 30) The minimum amount of time an object may sit idle in the pool before it is eligable for eviction by the idle object evictor (if any).
* removeAbandoned	(default false) Flag to remove abandoned connections if they exceed the removeAbandonedTimout. If set to true a connection is considered abandoned and eligible for removal if it has been idle longer than the removeAbandonedTimeout. Setting this to true can recover db connections from poorly written applications which fail to close a connection.
* removeAbandonedTimeout	(default 300) Timeout in seconds before an abandoned connection can be removed.
* logAbandoned	(default false) Flag to log stack traces for application code which abandoned a Statement or Connection.

For more information about the possible parameters and their values refer to the `DBCP documentation <http://commons.apache.org/dbcp/configuration.html>`_.

GeoServer setup
---------------

To allow a web application reference to a JNDI resource its :file:`web.xml` file must be modified so that the reference is explicit. Following the above example, we have to modify :file:`{TOMCAT_HOME}/webapps/geoserver/WEB-INF/web.xml` and add at its very end the following declaration:

.. code-block:: xml
  
  <web-app>
    ...
    <resource-ref>
      <description>Oracle Datasource</description>
      <res-ref-name>jdbc/oralocal</res-ref-name>
      <res-type>javax.sql.DataSource</res-type>
      <res-auth>Container</res-auth>
    </resource-ref>
  </web-app>

Once that is done, it is possible to login into the GeoServer web administration interface and configure the datastore. 

First, choose the *Oracle (JNDI)* datastore and give it a name:

.. figure:: choose-datastore.png
   :align: center
   
   
   *Choosing a JNDI enabled datastore*

Then, configure the connection parameters so that the JNDI path matches the one specified in the Tomcat configuration:

.. figure:: setup-params.png
   :align: center
   
   *Configuring the JNDI connection*

When you are doing this, make sure the *schema* is properly setup, or the datastore will list all the tables it can find in the schema it can access. In the case of Oracle the schema is usually the user name, upper cased.

Once the datastore is accepted the GeoServer usage proceeds as normal.
