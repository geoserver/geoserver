.. _datadir_setting:

Setting the data directory location
===================================

The ``GEOSERVER_DATA_DIR`` application property are determined using the first value obtained from: Java System Properties, Web Application context parameters, or System Environmental Variable.

* Follow the instructions below to learn how to set the environmental variable location. There are different instructions for each target platform.

* The page on :ref:`application_properties` has instructions for defining ``GEOSERVER_DATA_DIR`` using a Java System Property or Web Application context parameters.

.. note:: If the location of the GeoServer data directory is not set explicitly, the directory ``data_dir`` under the root of the GeoServer installation will be chosen by default.


Windows
-------

On Windows platforms the location of the GeoServer data directory is controlled by the ``GEOSERVER_DATA_DIR`` environment variable, system property, or web context parameter.

* :file:`C:\\\\ProgramData\\\\GeoServer` (example location)

To set the environment variable:

#. Open the System Control Panel.

#. Click :guilabel:`Advanced System Properties`.

#. Click :guilabel:`Environment Variables`.

#. Click the ``New`` button and create a environment variable called ``GEOSERVER_DATA_DIR`` and set it to the desired location.

   .. figure:: img/envvar_win.png

      Setting an environment variable on Windows

Linux
-----

On Linux platforms the location of the GeoServer data directory is controlled by the ``GEOSERVER_DATA_DIR`` environment variable, system property, or web context parameter.

* :file:`/var/opt/geoserver/data` (example location)

To set the environment variable:

#. Setting the variable can be achieved with the following command (in the terminal):

   .. code-block:: console

      export GEOSERVER_DATA_DIR=/var/opt/geoserver/data

#. To make the variable persist, place the command in the :file:``.bash_profile`` or :file:``.bashrc`` file.
   Ensure that this done for the user running GeoServer.

Mac OS X
--------

For the binary install of GeoServer on Mac OS X, the data directory is set in the same way as for Linux. 

* file:`~/Library/Application Support/GeoServer/data_dir` (example location)

For the Mac OS X install, set the ``GEOSERVER_DATA_DIR`` environment variable to the desired directory location.

#. Setting the variable can be achieved with the following command (in the terminal):

   .. code-block:: console

      export GEOSERVER_DATA_DIR="~/Library/Application Support/GeoServer/data_dir"

#. To make the variable persist, place the command in your preferred shell startup file:

    * Bash: :file:``.bash_profile`` or :file:``.bashrc``
    * ZSH: :file:`~/.zshrc`

Web archive
-----------

When running a GeoServer WAR inside a servlet container, the data directory can be specified in a number of ways. 

Context parameter
^^^^^^^^^^^^^^^^^

1. Tomcat: Use your application server to configure the GeoServer web application via :file:`conf/Catalina/localhost/geoserver.xml` file:

   .. code-block:: xml

      <Context docBase="geoserver.war">
        <Parameter name="GEOSERVER_DATA_DIR"
                   value="/var/opt/geoserver/data" override="false"/>
      </Context>

2. To specify the data directory using a servlet context parameter, create the following ``<context-param>`` element in the ``WEB-INF/web.xml`` file for the GeoServer application.

   This approach is not recommended, as the same steps must be performed each time you update.

   .. code-block:: xml
   
      <web-app>
        ...
        <context-param>
          <param-name>GEOSERVER_DATA_DIR</param-name>
          <param-value>/var/lib/geoserver_data</param-value>
        </context-param>
        ...
      </web-app>

Java system property
^^^^^^^^^^^^^^^^^^^^

It is also possible to specify the data directory location with a Java system property. This method can be useful during upgrades, as it avoids the need to set the data directory after every upgrade.

.. warning:: Using a Java system property will typically set the property for all applications running in the servlet container, not just GeoServer.

The method of setting the Java system property is dependent on the servlet container:

* For **Tomcat** on Linux, edit the file :file:`bin/setenv.sh` under the root of the Tomcat installation. Specify the ``GEOSERVER_DATA_DIR`` system property by setting the ``CATALINA_OPTS`` variable:

  .. code-block:: console

     # Append system properties
     CATALINA_OPTS="${CATALINA_OPTS} -DGEOSERVER_DATA_DIR=/var/lib/geoserver_data"

* For **Tomcat** on Windows use Apache Tomcat Properties application, navigating to the **Java** tab to edit **Java Options**::

     -DGEOSERVER_DATA_DIR=C:\ProgramData\GeoServer\data
   
* For **Glassfish**, edit the file :file:`domains/<<domain>>/config/domain.xml` under the root of the Glassfish installation, where ``<<domain>>`` refers to the domain that the GeoServer web application is deployed under. Add a ``<jvm-options>`` element inside the ``<java-config>`` element:

  .. code-block:: xml

     ...
     <java-config>
        ...
       <jvm-options>-DGEOSERVER_DATA_DIR=/var/lib/geoserver_data</jvm-options>  
     </java-config>
     ...

Require files to exist
----------------------

If the data directory is on a network filesystem, it can be desirable for security reasons to require one or more files or directories to exist before GeoServer will start, to prevent GeoServer from falling back into a default insecure configuration if the data directory appears to be empty because of the loss of this network resource.

To require files or directories to exist, use any of the methods above to set ``GEOSERVER_REQUIRE_FILE``. Do not specify a mount point as this will still exist if a network filesystem is unavailable; instead specify a file or directory *inside* a network mount. For example:

Environment variable:

.. code-block:: console

   export GEOSERVER_REQUIRE_FILE=/mnt/server/geoserver_data/global.xml

Servlet context parameter:

.. code-block:: xml

   <web-app>
     ...
     <context-param>
       <param-name>GEOSERVER_REQUIRE_FILE</param-name>
       <param-value>/mnt/server/geoserver_data/global.xml</param-value>
     </context-param>
     ...
   </web-app>

Java system property:

.. code-block:: console

   CATALINA_OPTS="${CATALINA_OPTS} -DGEOSERVER_REQUIRE_FILE=/mnt/server/geoserver_data/global.xml"

Multiple files
^^^^^^^^^^^^^^

To specify multiple files or directories that must exist, separate them with the path separator (``:`` on Linux, ``;`` on Windows):

Environment variable:

.. code-block:: console

   export GEOSERVER_REQUIRE_FILE=/mnt/server/geoserver_data/global.xml:/mnt/server/data

Servlet context parameter:

.. code-block:: xml

   <web-app>
     ...
     <context-param>
       <param-name>GEOSERVER_REQUIRE_FILE</param-name>
       <param-value>/mnt/server/geoserver_data/global.xml:/mnt/server/data</param-value>
     </context-param>
     ...
   </web-app>

Java system property:

.. code-block:: console

   CATALINA_OPTS="${CATALINA_OPTS} -DGEOSERVER_REQUIRE_FILE=/mnt/server/geoserver_data/global.xml:/mnt/server/data"

.. _datadir-loader:

Data directory loader
-------------------------------

GeoServer includes a data directory loader that is designed to efficiently load configuration files, especially for deployments with large data directories. This loader is enabled by default.

Benefits
^^^^^^^^

The data directory loader provides several advantages:

* **Parallel Processing**: Both I/O calls and XML parsing of catalog and configuration files are parallelized
* **Efficient Directory Traversal**: Makes a single pass over the ``workspaces`` directory tree, loading most catalog and configuration files in one pass
* **Network Performance**: Particularly beneficial for deployments using network filesystems like NFS, which are typically slow when serving many small files

Configuration
^^^^^^^^^^^^^

The data directory loader can be configured with the following environment variables or system properties:

* ``GEOSERVER_DATA_DIR_LOADER_ENABLED``: Controls whether the data directory loader optimizations are used.
   * ``true``: Default setting, used to enable data directory optimizations.
   * ``false``: Used to disable the optimizations and fall back to the traditional loader used prior to GeoServer 2.27 release.

* ``GEOSERVER_DATA_DIR_LOADER_THREADS``: Controls the number of threads used for loading and parsing
   * By default, the loader uses a heuristic that selects the minimum between ``16`` and the number of available processors as reported by the JVM
   * Set to a specific number to override this heuristic (e.g., ``8`` to use 8 threads)
   * Values less than or equal to zero will produce a warning and fall back to the default heuristic

Example usage with environment variables:

.. code-block:: bash

   # Disable the optimized loader
   export GEOSERVER_DATA_DIR_LOADER_ENABLED=false

   # Use 8 threads for loading
   export GEOSERVER_DATA_DIR_LOADER_THREADS=8

   # Start GeoServer
   ./bin/startup.sh

Example usage with system properties:

.. code-block:: bash

   # Start GeoServer with customized loader settings
   CATALINA_OPTS="${CATALINA_OPTS} -DGEOSERVER_DATA_DIR_LOADER_ENABLED=true -DGEOSERVER_DATA_DIR_LOADER_THREADS=4"
