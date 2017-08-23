.. _installation_osx_bin:

Mac OS X binary
===============

.. note:: For the installer on OS X, please see the section on the :ref:`installation_osx_installer`. For installing on OS X with an existing application server such as Tomcat, please see the :ref:`installation_war` section.

An alternate way of installing GeoServer on OS X is to use the platform-independent binary. This version is a GeoServer web application bundled inside `Jetty <http://eclipse.org/jetty/>`_, a lightweight and portable application server. It has the advantages of working very similarly across all operating systems and is very simple to set up.

Installation
------------

#. Make sure you have a Java Runtime Environment (JRE) installed on your system. GeoServer requires a **Java 8** environment, and the JRE supplied by OS X is not sufficient. For more information, please see the `instructions for installing Oracle Java on OS X <http://java.com/en/download/faq/java_mac.xml>`_.

   .. note:: Java 9 is not currently supported.

   .. note:: For more information about Java and GeoServer, please see the section on :ref:`production_java`.

#. Navigate to the `GeoServer Download page <http://geoserver.org/download>`_.

#. Select the version of GeoServer that you wish to download.  If you're not sure, select `Stable <http://geoserver.org/release/stable>`_.

#. Select :guilabel:`Platform Independent Binary` on the download page.

#. Download the archive and unpack to the directory where you would like the program to be located.

   .. note:: A suggested location would be :file:`/usr/local/geoserver`.

#. Add an environment variable to save the location of GeoServer by typing the following command:

   .. code-block:: bash
    
      echo "export GEOSERVER_HOME=/usr/local/geoserver" >> ~/.profile
      . ~/.profile

#. Make yourself the owner of the ``geoserver`` folder, by typing the following command:

    .. code-block:: bash

       sudo chown -R <USERNAME> /usr/local/geoserver/

   where ``USER_NAME`` is your user name 

#. Start GeoServer by changing into the directory ``geoserver/bin`` and executing the ``startup.sh`` script:

    .. code-block:: bash
       
       cd geoserver/bin
       sh startup.sh

    .. include:: ./osx_jaierror.txt

#. In a web browser, navigate to ``http://localhost:8080/geoserver``.

If you see the GeoServer logo, then GeoServer is successfully installed.

   .. figure:: images/success.png

      GeoServer installed and running successfully

To shut down GeoServer, either close the persistent command-line window, or run the :file:`shutdown.sh` file inside the :file:`bin` directory.

Uninstallation
--------------

#. Stop GeoServer (if it is running).

#. Delete the directory where GeoServer is installed.
