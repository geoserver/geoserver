.. _installation_windows_bin:

Windows binary
==============

.. note:: For installing on Windows with an existing application server such as Tomcat, please see the :ref:`installation_war` section.

The other way of installing GeoServer on Windows is to use the platform-independent binary. This version is a GeoServer web application bundled inside `Jetty <http://eclipse.org/jetty/>`_, a lightweight and portable application server. It has the advantages of working very similarly across all operating systems and is very simple to set up.

Installation
------------

#. Make sure you have a Java Runtime Environment (JRE) installed on your system. GeoServer requires a **Java 11** or **Java 17** environment, as provided by `Adoptium <https://adoptium.net>`__ Windows installers.

   .. note:: For more information about Java and GeoServer, please see the section on :ref:`production_java`.

#. Navigate to the :website:`GeoServer Download page <download>`.

#. Select the version of GeoServer that you wish to download.  If you're not sure, select :website:`Stable <release/stable>` release.

   .. only:: snapshot
      
      These instructions are for GeoServer |version|-SNAPSHOT which is provided as a :website:`Nightly <release/2.23.x>` release.
      Testing a Nightly release is a great way to try out new features, and test community modules. Nightly releases
      change on an ongoing basis and are not suitable for a production environment.
      
   .. only:: not snapshot

      These instructions are for GeoServer |release|.

#. Select :guilabel:`Platform Independent Binary` on the download page: :download_release:`bin`

#. Download the archive and unpack to the directory where you would like the program to be located.

   .. note:: A suggested location would be :file:`C:\\Program Files\\GeoServer`.

Setting environment variables
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

You will need to set the ``JAVA_HOME`` environment variable if it is not already set. This is the path to your JRE such that :file:`%JAVA_HOME%\\bin\\java.exe` exists.

#. Navigate to :menuselection:`Control Panel --> System --> Advanced --> Environment Variables`.

#. Under :guilabel:`System variables` click :guilabel:`New`.

#. For :guilabel:`Variable name` enter ``JAVA_HOME``.  For :guilabel:`Variable value` enter the path to your JDK/JRE.

#. Click OK three times.

.. note:: You may also want to set the ``GEOSERVER_HOME`` variable, which is the directory where GeoServer is installed, and the ``GEOSERVER_DATA_DIR`` variable, which is the location of the GeoServer data directory (which by default is :file:`%GEOSERVER_HOME\\data_dir`). The latter is mandatory if you wish to use a data directory other than the default location. The procedure for setting these variables is identical to setting the ``JAVA_HOME`` variable.

Running
-------

.. note:: This can be done either via Windows Explorer or the command line.

#. Navigate to the :file:`bin` directory inside the location where GeoServer is installed.

#. Run :file:`startup.bat`.  A command-line window will appear and persist. This window contains diagnostic and troubleshooting information. This window must be left open, otherwise GeoServer will shut down.

#. Navigate to ``http://localhost:8080/geoserver`` (or wherever you installed GeoServer) to access the GeoServer :ref:`web_admin`.

   If you see the GeoServer Welcome page, then GeoServer is successfully installed.

   .. figure:: images/success.png
      
      GeoServer Welcome Page

Stopping
--------

To shut down GeoServer, either close the persistent command-line window, or run the :file:`shutdown.bat` file inside the :file:`bin` directory.

Uninstallation
--------------

#. Stop GeoServer (if it is running).

#. Delete the directory where GeoServer is installed.
