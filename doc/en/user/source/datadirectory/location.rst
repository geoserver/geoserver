.. _datadir_location:

Data directory location
=======================

The current data directory location is always shown on the :ref:`config_serverstatus` page.

.. figure:: /configuration/img/server_status.png
   
   Status Page (default tab)
   
Default data directory location
-------------------------------

By default GeoServer includes an example data directory allowing you to try out the application quickly:

* Platform Independent Binary: The data directory is located at :file:`<installation root>/data_dir`.

  .. list-table::
     :header-rows: 1
  
     * - Platform
       - Default location
     * - Linux 
       - :file:`/usr/share/geoserver/data_dir`
     * - Windows
       - :file:`C:\\Program Files\\GeoServer\\data_dir`
  
  The windows :file:`Program Files` location above is not ideal due to restrictions placed on this location.
  
* Web archive: If GeoServer is running as a **web archive** inside of your application server, the data directory is by default located at :file:`<web application root>/data``. 
  
  .. list-table::
     :header-rows: 1
  
     * - Platform
       - Default location
     * - Linux (Tomcat)
       - :file:`/var/lib/tomcat9/webapps/geoserver/data`
     * - Windows (Tomcat)
       - :file:`C:\\Program Files\\Apache Software Foundation\\Tomcat 9.0\\webapps\\geoserver\\data`

* Windows Installer: The windows installer unpacks the data directory to :file:`%PROGRAMDATA%\\GeoServer`:

  .. list-table::
     :header-rows: 1
  
     * - Platform
       - Default location
     * - Windows (Installer)
       - :file:`%ProgramData%\\GeoServer`

* Docker: The Docker image maintains a data directory in :file:``/opt/geoserver_data``.
  
  This location should be mapped to an absolute path in your host as described in :ref:`installation_docker_data`.

External data directory location
--------------------------------

To make :ref:`upgrading <installation_upgrade>` easier, **Web Archive** users should switch to an *external* data directory (outside the application).


.. list-table::
   :header-rows: 1

   * - Platform
     - Example location
   * - Linux 
     - :file:`/var/opt/geoserver/data`
   * - Windows
     - :file:`C:\\ProgramData\\GeoServer`
   * - MacOS 
     - :file:`~/Library/Application Support/GeoServer/data_dir`

Creating a new data directory
-----------------------------

To create a new data directory:

* The easiest way to create a new data directory is to copy an existing default data directory above.

  Once the data directory has been located, copy it to a new location. To point a GeoServer instance at the new data directory proceed to the next section :ref:`datadir_setting`.

* You may download the sample data directory.
  
  Navigate to the :website:`GeoServer Download page <download>`, select a version of GeoServer, and download the provided data directory zip.
  
  * For GeoServer |release| Nightly: :download_release:`data`

  * For GeoServer |version|: :nightly_release:`data`

* You may also use a new empty folder as the data directory location.
  
  GeoServer will create configuration files and folders as needed.