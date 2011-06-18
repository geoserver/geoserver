.. _community_spatialite:

SpatiaLite
==========

.. note:: GeoServer does not come built-in with support for SpatiaLite; it must be installed through an extension. Furthermore it requires that additional native libraries be available on the system. Proceed to :ref:`spatialite_install` for installation details.

`SpatiaLite <http://http://www.gaia-gis.it/spatialite/>`_ is the spatial 
extension of the popular `SQLite <http://www.sqlite.org>`_ embedded relational 
database.

.. _spatialite_install:

Installing the SpatiaLite extension
-----------------------------------

#. Download the SpatiaLite extension from the `nightly GeoServer community module builds <http://gridlock.opengeo.org/geoserver/trunk/community-latest/>`_.

   .. warning:: Make sure to match the version of the extension to the version of the GeoServer instance!

#. Extract the contents of the archive into the ``WEB-INF/lib`` directory of the GeoServer installation.

Notes about shared libraries
----------------------------

The version of SpatiaLite included in the extension is compiled against the 
`GEOS <http://geos.osgeo.org>`_ and `PROJ <proj.osgeo.org>`_ libraries so they 
must be installed on the system. If the libraries are not installed on the 
system the extension will cease to function.

If the libraries are not installed on your system in a "default" location you 
must set the ``LD_LIBRARY_PATH`` (or equivalent) environment variable in order 
to load them at runtime.

.. note::
  On Windows systems it is easiest to place the GEOS and PROJ dll's in the 
  ``C:\WINDOWS\system32`` directory.

Adding a SpatiaLite database
----------------------------

Once the extension is properly installed ``SpatiaLite`` will show up as an option when creating a new data store.

.. figure:: images/spatialitecreate.png
   :align: center

   *SpatiaLite in the list of vector data sources*

Configuring a SpatiaLite data store
-----------------------------------

.. figure:: images/spatialiteconfigure.jpg
   :align: center

   *Configuring a SpatiaLite data store*

.. list-table::
   :widths: 20 80

   * - ``database``
     - The name of the database to connect to. See :ref:`notes <database_notes>` below.
   * - ``schema``
     - The database schema to access tables from. Optional.
   * - ``user``
     - The name of the user to connect to the database as. Optional.
   * - ``password``     
     - The password to use when connecting to the database. Optional, leave blank for no password.
   * - ``max connections``
 
       ``min connections``

     - Connection pool configuration parameters. See the :ref:`connection_pooling` section for details.

.. _database_notes:

The *database* parameter may be specified as an absolute path or a relative one.
When specified as a relative path the database will created in the 
``spatialite`` directory, located directly under the root of the GeoServer data
directory.
