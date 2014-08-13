.. _installation_windows_bin:

Windows Binary
==============

.. note:: This section is for the OS-independent binary.  Please see the section on the :ref:`installation_windows_installer` for the wizard-based installer for Windows.

The most common way to install GeoServer is using the OS-independent binary.  This version is a GeoServer web application (webapp) bundled inside `Jetty <http://www.mortbay.org/jetty/>`_, a lightweight servlet container system.  It has the advantages of working very similarly across all operating systems plus being very simple to set up.


Installation
------------

#. Navigate to the `GeoServer Download page <http://geoserver.org/download>`_ and pick the appropriate version to download.

#. Select :guilabel:`OS-independent binary` on the download page.

#. Download the archive, and unpack to the directory where you would like the program to be located.

========= ===============================================
Platform  Example Location 
========= ===============================================
Windows   :file:`C:\\Program Files\\GeoServer`
========= ===============================================

Setting environment variables
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

You will need to set the ``JAVA_HOME`` environment variable if it is not already set.  This is the path to your JRE (or JDK) such that :file:`%JAVA_HOME%\\bin\\java.exe` exists.  You can download a JRE at `<http://www.oracle.com/technetwork/java/javase/downloads/index.html>`_.

.. note:: Java 7 is required, as of GeoServer 2.6.x Java 5 is no longer supported. For more information about Java and GeoServer, please see the section on :ref:`production_java`.

#. Navigate to :menuselection:`Control Panel --> System --> Advanced --> Environment Variables`.

#. Under :guilabel:`System variables` click :guilabel:`New`. 

#. For :guilabel:`Variable name` enter ``JAVA_HOME``.  For :guilabel:`Variable value` enter the path to your JDK/JRE.

#. Click OK three times.

.. note:: You may also want to set the ``GEOSERVER_HOME`` variable, which is the directory where GeoServer is installed, and the ``GEOSERVER_DATA_DIR`` variable, which is the location of the GeoServer data directory (usually :file:`%GEOSERVER_HOME\\data_dir`).  The latter is mandatory if you wish to use a data directory other than the one built in to GeoServer. The procedure for setting these variables is identical to the above. Note that the specified data directory should be a valid :ref:`data_directory`.

Running
-------

.. note:: This can be done either via Windows Explorer or the command line.

#. Navigate to the :file:`bin` directory inside the location where GeoServer is installed.

#. Run :file:`startup.bat`.  A command-line window will appear and persist.  This window contains diagnostic and troubleshooting information.  This window should not be closed, or else GeoServer will shut down.

#. To access the :ref:`web_admin`, navigate to ``http://localhost:8080/geoserver``. 

Stopping
--------

Either close the persistent command-line window, or run the :file:`shutdown.bat` file inside the :file:`bin` directory.

Uninstallation
--------------

#. Stop GeoServer (if it is running)

#. Delete the directory where GeoServer is installed.
