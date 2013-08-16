.. _monitor_db:

Database Persistence
====================

The monitor extension is capable of persisting request data to a database via the 
`Hibernate <http://www.hibernate.org/>`_ library.

.. note::

   In order to utilize hibernate persistence the hibernate extension must be installed on
   top of the core monitoring extension. See the :ref:`monitor_installation` for details.


Configuration
-------------

General
^^^^^^^

In order to activate hibernate persistence the ``storage`` parameter must be set to the
value "hibernate"::

  storage=hibernate

The hibernate storage backend supports both the ``history`` and ``live`` modes however
care should be taken when enabling the ``live`` mode as it results in many transactions 
with the database over the life of a request. Unless updating the database in real time 
is required the ``history`` mode is recommended.


Database
^^^^^^^^

The file ``db.properties`` in the ``<GEOSERVER_DATA_DIR>/monitoring`` directory specifies
the Hibernate database. By default an embedded H2 database located in the ``monitoring`` 
directory is used. This can be changed by editing the ``db.properties`` file::

   # default configuration is for h2 
   driver=org.h2.Driver
   url=jdbc:h2:file:${GEOSERVER_DATA_DIR}/monitoring/monitoring

For example to store request data in an external PostgreSQL database, set ``db.properties`` to::

   driver=org.postgresql.Driver 
   url=jdbc:postgresql://192.168.1.124:5432/monitoring
   username=bob
   password=foobar
   defaultAutoCommit=false

In addition to ``db.properties`` file is the ``hibernate.properties`` file that contains
configuration for Hibernate itself. An important parameter of this file is the hibernate
dialect that informs hibernate of the type of database it is talking to. 

When changing the type of database both the ``databasePlatform`` and ``database`` parameters 
must be updated. For example to switch to PostgreSQL::

   # hibernate dialect
   databasePlatform=org.hibernate.dialect.PostgreSQLDialect
   database=POSTGRESQL
   
   # other hibernate configuration
   hibernate.use_sql_comments=true
   generateDdl=true
   hibernate.format_sql=true
   showSql=false
   hibernate.generate_statistics=true
   hibernate.session_factory_name=SessionFactory
   hibernate.hbm2ddl.auto=update
   hibernate.bytecode.use_reflection_optimizer=true
   hibernate.show_sql=false

Hibernate
^^^^^^^^^

As mentioned in the previous section the ``hibernate.properties`` file contains the configuration
for Hibernate itself. Aside from the database dialect parameters it is not recommended that you 
change this file unless you are an experienced Hibernate user.