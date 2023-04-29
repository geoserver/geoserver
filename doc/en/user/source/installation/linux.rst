.. _installation_linux:

Linux binary
============

.. note:: For installing on Linux with an existing application server such as Tomcat, please see the :ref:`installation_war` section.

The platform-independent binary is a GeoServer web application bundled inside `Jetty <http://eclipse.org/jetty/>`__, a lightweight and portable application server. It has the advantages of working very similarly across all operating systems and is very simple to set up.

Installation
------------

#. Make sure you have a Java Runtime Environment (JRE) installed on your system. GeoServer requires a **Java 11** or **Java 17** environment, available from `OpenJDK <https://openjdk.java.net>`__, `Adoptium <https://adoptium.net>`__, or provided by your OS distribution.

   .. note:: For more information about Java and GeoServer compatibility, please see the section on :ref:`production_java`.

#. Navigate to the :website:`GeoServer Download page <download>`.

#. Select the version of GeoServer that you wish to download. If you're not sure, select :website:`Stable <release/stable>` release.

   .. only:: snapshot
      
      These instructions are for GeoServer |version|-SNAPSHOT which is provided as a :website:`Nightly <release/main>` release.
      Testing a Nightly release is a great way to try out new features, and test community modules. Nightly releases
      change on an ongoing basis and are not suitable for a production environment.
      
   .. only:: not snapshot

      These instructions are for GeoServer |release|.

#. Select :guilabel:`Platform Independent Binary` on the download page: :download_release:`bin`

#. Download the :file:`zip` archive and unpack to the directory where you would like the program to be located.

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

   If you see the GeoServer Welcome page, then GeoServer is successfully installed.

   .. figure:: images/success.png

      GeoServer Welcome Page

#. To shut down GeoServer, either close the persistent command-line window, or run the :file:`shutdown.sh` file inside the :file:`bin` directory.
    
Uninstallation
--------------

#. Stop GeoServer (if it is running).

#. Delete the directory where GeoServer is installed.
