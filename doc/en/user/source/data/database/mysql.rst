.. _data_mysql:

MySQL
=====

.. note:: GeoServer does not come built-in with support for MySQL; it must be installed through an extension. Proceed to :ref:`mysql_install` for installation details.

.. warning:: Currently the MySQL extension is unmaintained and carries unsupported status. While still usable, do not expect the same reliability as with other extensions.

`MySQL <http://www.mysql.com>`_ is an open source relational database with some limited spatial functionality.

.. _mysql_install:

Installing the MySQL extension
------------------------------

#. Download the MySQL extension from the `GeoServer download page <http://geoserver.org/download>`_.

   .. warning:: Make sure to match the version of the extension to the version of the GeoServer instance!

#. Extract the contents of the archive into the ``WEB-INF/lib`` directory of the GeoServer installation.

Adding a MySQL database
-----------------------

Once the extension is properly installed ``MySQL`` will show up as an option when creating a new data store.

.. figure:: images/mysqlcreate.png
   :align: center

   *MySQL in the list of data sources*

Configuring a MySQL data store
------------------------------

.. figure:: images/mysqlconfigure.png
   :align: center

   *Configuring a MySQL data store*

.. list-table::
   :widths: 20 80

   * - ``host``
     - The mysql server host name or ip address.
   * - ``port``
     - The port on which the mysql server is accepting connections.
   * - ``database``
     - The name of the database to connect to.
   * - ``user``
     - The name of the user to connect to the mysql database as.
   * - ``password``     
     - The password to use when connecting to the database. Left blank for no
       password.
   * - ``max connections``

       ``min connections``

       ``validate connections``

     - Connection pool configuration parameters. See the 
       :ref:`connection_pooling` section for details.
  