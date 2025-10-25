.. _installation_linux:

Linux binary
============

The platform-independent binary is a GeoServer web application bundled with `Jetty <https://eclipse.org/jetty/>`__,
a scalable and memory-efficient web server and Servlet container.
Jerry has the advantages of working very similarly across all operating systems and is straightfoward to set up.

.. note:: For installing on Linux with an existing application server such as Tomcat, please see the :ref:`installation_war` section.

Installation
------------

#. Make sure you have a Java Runtime Environment (JRE) installed on your system. GeoServer requires a **Java 17** or **Java 21** environment.
   
   .. include:: jdk-linux-guidance.txt
   
   .. note:: For more information about Java and GeoServer compatibility, please see the section on :ref:`production_java`.

#. Navigate to the :website:`GeoServer Download page <download>`.

#. Select the version of GeoServer that you wish to download.

   * If you're not sure, select :website:`Stable <release/stable>` release.
   
     Examples provided for GeoServer |release|.

   * Testing a Nightly release is a great way to try out new features, and test community modules. Nightly releases
     change on an ongoing basis and are not suitable for a production environment.
     
     Examples are provided for GeoServer |version|, which is provided as a :website:`Nightly <release/main>` release.

#. Select :guilabel:`Platform Independent Binary` on the download page:
   
   * :download_release:`bin`
   * :nightly_release:`bin`

#. Download the :file:`zip` archive and unpack to the directory where you would like the program to be located.

   .. note:: A suggested location would be :file:`/usr/share/geoserver`.

#. Add an environment variable to save the location of GeoServer by typing the following command:

   .. code-block:: bash
   
      echo "export GEOSERVER_HOME=/usr/share/geoserver" >> ~/.profile
      . ~/.profile

#. Optionally, set the environment variable ``JETTY_OPTS`` to tweak the jetty configuration upfront:

   .. code-block:: bash

      echo "export JETTY_OPTS='jetty.http.port=1234'" >> ~/.profile
      . ~/.profile

#. Make yourself the owner of the ``geoserver`` folder.  Type the following command in the terminal window, replacing ``USER_NAME`` with your own username :

   .. code-block:: bash

      sudo chown -R USER_NAME /usr/share/geoserver/

#. Start GeoServer by changing into the directory :file:`geoserver/bin` and executing the :file:`startup.sh` script:

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
