.. _installation_linux_bin:

Linux Binary
============

.. note:: This section is for the OS-independent binary.

The most common way to install GeoServer is using the OS-independent binary.  This version is a GeoServer web application (webapp) bundled inside `Jetty <http://www.mortbay.org/jetty/>`_, a lightweight servlet container system.  It has the advantages of working very similarly across all operating systems plus being very simple to set up.

Installation
------------

#. Navigate to the `GeoServer Download <http://geoserver.org/download>`_ page and click your preferred GeoServer version--Stable, Maintenance or Development. In "Nightly" builds are provided to help test the latest features of bug fixes.

#. On the resulting page, download and save the :guilabel:`Binary (OS independent)` format of your preferred GeoServer version.  

    .. note:: Download GeoServer wherever you find appropriate.  In this example we download the GeoServer archive to the Desktop.  If GeoServer is in a different location, simply replace ``Desktop`` in the following command to your own folder path.

#. After saving the Geoserver archive, move to the location of your download, by first opening a terminal and then typing the following command:

    .. code-block:: bash

       cd Desktop/

#. Confirm that you are in the right directory by listing its contents.  You should see your specific GeoServer archive (e.g., ``GeoServer-2.0-RC1-bin.zip``) by typing:  

    .. code-block:: bash

       ls -l
    
#. Unzip ``geoserver-2.0-RC1.zip`` to ``/usr/local/geoserver`` with the following two commands:
 
    .. code-block:: bash

       unzip $geoserver-2.6.x.zip  .
       sudo mv geoserver-2.6.x/  geoserver
  
    .. note:: Notice the **.** in the first command.  This means the archive will unzip in the current directory. 

#. Add an environment variable to save the location of GeoServer by typing the following command:

    .. code-block:: bash
    
       echo "export GEOSERVER_HOME=/usr/local/geoserver" >> ~/.profile
       . ~/.profile

#. Make yourself the owner of the ``geoserver`` folder.  Type the following command in the terminal window, replacing ``USER_NAME`` with your own username :

    .. code-block:: bash

       sudo chown -R USER_NAME /usr/local/geoserver/

#. Start GeoServer by changing into the directory ``geoserver/bin`` and executing the ``startup.sh`` script:

    .. code-block:: bash
       
       cd geoserver/bin
       sh startup.sh

#. Use ``open http://localhost:8080/geoserver`` to visit GeoServer in a web browser.

