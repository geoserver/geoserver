.. _installation_windows_bin:

Windows binary
==============

.. note:: For installing on Windows with an existing application server such as Tomcat, please see the :ref:`installation_war` section.

The other way of installing GeoServer on Windows is to use the platform-independent binary. This version is a GeoServer web application bundled inside `Jetty <http://eclipse.org/jetty/>`_, a lightweight and portable application server. It has the advantages of working very similarly across all operating systems and is very simple to set up.

Installation
------------

#. Make sure you have a Java Runtime Environment (JRE) installed on your system. GeoServer requires a **Java 8** environment.  You can `download JRE 8 from Oracle <http://www.oracle.com/technetwork/java/javase/downloads/>`_.

   .. note:: Java 9 is not currently supported.

   .. note:: For more information about Java and GeoServer, please see the section on :ref:`production_java`.

#. Navigate to the `GeoServer Download page <http://geoserver.org/download>`_.

#. Select the version of GeoServer that you wish to download.  If you're not sure, select `Stable <http://geoserver.org/release/stable>`_.  

#. Select :guilabel:`Platform Independent Binary` on the download page.

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

If you see the GeoServer logo, then GeoServer is successfully installed.

   .. figure:: images/success.png

      GeoServer installed and running successfully

Stopping
--------

To shut down GeoServer, either close the persistent command-line window, or run the :file:`shutdown.bat` file inside the :file:`bin` directory.

Uninstallation
--------------

#. Stop GeoServer (if it is running).

#. Delete the directory where GeoServer is installed.
