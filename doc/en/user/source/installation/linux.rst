.. _installation_linux:

Linux binary
============

.. note:: For installing on Linux with an existing application server such as Tomcat, please see the :ref:`installation_war` section.

The platform-independent binary is a GeoServer web application bundled inside `Jetty <http://eclipse.org/jetty/>`_, a lightweight and portable application server. It has the advantages of working very similarly across all operating systems and is very simple to set up.

Installation
------------

#. Make sure you have a Java Runtime Environment (JRE) installed on your system. GeoServer requires a **Java 8** environment. The Oracle JRE is preferred, but OpenJDK has been known to work adequately. You can `download JRE 8 from Oracle <http://www.oracle.com/technetwork/java/javase/downloads/>`_.

   .. note:: Java 9 is not currently supported.

#. Select the version of GeoServer that you wish to download.  If you're not sure, select `Stable <http://geoserver.org/release/stable>`_.  

#. Select :guilabel:`Platform Independent Binary` on the download page.

#. Download the archive and unpack to the directory where you would like the program to be located.

   .. note:: A suggested location would be :file:`/usr/share/geoserver`.

#. Add an environment variable to save the location of GeoServer by typing the following command:

    .. code-block:: bash
    
       echo "export GEOSERVER_HOME=/usr/share/geoserver" >> ~/.profile
       . ~/.profile

#. Make yourself the owner of the ``geoserver`` folder.  Type the following command in the terminal window, replacing ``USER_NAME`` with your own username :

    .. code-block:: bash

       sudo chown -R USER_NAME /usr/share/geoserver/

#. Start GeoServer by changing into the directory ``geoserver/bin`` and executing the ``startup.sh`` script:

    .. code-block:: bash
       
       cd geoserver/bin
       sh startup.sh

#. In a web browser, navigate to ``http://localhost:8080/geoserver``.

If you see the GeoServer logo, then GeoServer is successfully installed.

   .. figure:: images/success.png

      GeoServer installed and running successfully

To shut down GeoServer, either close the persistent command-line window, or run the :file:`shutdown.sh` file inside the :file:`bin` directory.

Uninstallation
--------------

#. Stop GeoServer (if it is running).

#. Delete the directory where GeoServer is installed.
