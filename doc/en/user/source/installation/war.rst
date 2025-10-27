.. _installation_war:

Web archive
===========

GeoServer is packaged as a standalone Web Archive (:file:`geoserver.war`) file for use with existing application servers such as `Apache Tomcat <https://tomcat.apache.org/>`_ and `Jetty <https://jetty.org/>`_.

+-----------------+-----------------+--------------------+----------------+
| JavaEE          | JakartaEE       | Application Server | GeoServer      |
+=================+=================+====================+================+
|                 | Servlet API 6.1 | Tomcat 11.0.x      | GeoServer 3    |
+-----------------+-----------------+--------------------+----------------+
|                 | Servlet API 6.0 | Tomcat 10.1.x      | GeoServer 3    |
+-----------------+-----------------+--------------------+----------------+
|                 |                 | Tomcat 10.0.x      | not supported  |
+-----------------+-----------------+--------------------+----------------+
| Servlet API 4   |                 | Tomcat 9.x         | GeoServer 2    |
+-----------------+-----------------+--------------------+----------------+
| Servlet API 3.1 |                 | Jetty 9.4          | GeoServer 2    |
+-----------------+-----------------+--------------------+----------------+

GeoServer is tested using Tomcat 11.0.x, and this is the recommended application server.
Other application servers have been known to work, but are not tested regularly by community members. 

.. note:: 

   GeoServer 3 is compatible with Tomcat 11.0.x which provides Jakarta EE Servlet API 6.1.x and annotation processing.

.. note:: 

   GeoServer 2 is compatible with Tomcat 9.x which provides the required Java Enterprise Edition Servlet API 4 and annotation processing.
 
Installation
------------

#. Make sure you have a Java Runtime Environment (JRE) installed on your system. GeoServer requires a **Java 17** or **Java 21** environment.

   **Linux**
   
   .. include:: jdk-linux-guidance.txt
   
   **Windows**
   
   .. include:: jdk-windows-guidance.txt
   
   **MacOS**
   
   .. include:: jdk-macos-guidance.txt
    
   .. note:: For more information about Java and GeoServer compatibility, please see the section on :ref:`production_java`.

#. Navigate to the :website:`GeoServer Download page <download>`.

#. Select the version of GeoServer that you wish to download.  

   * If you're not sure, select :website:`Stable <release/stable>` release.
   
     Examples provided for GeoServer |release|.

   * Testing a Nightly release is a great way to try out new features, and test community modules. Nightly releases
     change on an ongoing basis and are not suitable for a production environment.
     
     Examples are provided for GeoServer |version|, which is provided as a :website:`Nightly <release/main>` release.

#. Select :guilabel:`Web Archive` on the download page:
   
   * :download_release:`war`
   * :nightly_release:`war`
   
#. Download and unpack the archive.

#. Deploy the web archive as you would normally. Often, all that is necessary is to copy the :file:`geoserver.war` file to the application server's :file:`webapps` directory, and the application will be deployed by the application server.

   .. note:: A restart of your application server may be necessary.

Tomcat Hardening
----------------
Hide the Tomcat version in error responses and its error details.

1. To remove the Tomcat version, create the following file with empty parameters
   ::
   
    cd $CATALINA_HOME (where Tomcat binaries are installed)
    mkdir -p ./lib/org/apache/catalina/util/
    cat > ./lib/org/apache/catalina/util/ServerInfo.properties <<EOF
    server.info=
    server.number=
    server.built=
    EOF

2. Additionally add to :file:`server.xml` the ErrorReportValve to disable showReport and showServerInfo. This is used to hide errors handled globally by tomcat in the host section.

   ``vi ./conf/server.xml``

   Add to ``<Host name=...`` section this new ErrorReportValve entry:
   ::
   
    ...
         <Host name="localhost"  appBase="webapps"
               unpackWARs="true" autoDeploy="true">
           
           ...
   
           <Valve className="org.apache.catalina.valves.ErrorReportValve" showReport="false" showServerInfo="false" />
   
         </Host>
       </Engine>
     </Service>
    </Server>


3. Why, if security by obscurity does not work?

   Even though this is not the final solution, it at least mitigates the visible eye-catcher of outdated software packages.

   Let's take the attackers point of view.
   
   Response with just HTTP status:
   ::
   
    HTTP Status 400 – Bad Request
   
   Ok, it looks like a Tomcat is installed.
   
   Default full response:
   ::
   
    HTTP Status 400 – Bad Request
    Type Status Report
    Message Invalid URI
    Description The server cannot or will not process the request due to something that is perceived to be a client error (e.g., malformed request syntax, invalid request message framing, or deceptive request routing).
    Apache Tomcat/7.0.67
   
   Ahh, great, the software is not really maintained. Tomcat is far outdated from Dec. 2015 (6 years old as of today Jan. 2022) with a lot of unfixed vulnerabilities.
   
4. Notice: For support reason, the local output of version.sh still outputs the current version
   ::
   
    $CATALINA_HOME/bin/version.sh
     ...
     Server number:  7.0.67
     ...


Running
-------

1. Use your container application's method of starting and stopping webapps to run GeoServer.

2. To access the :ref:`web_admin`, open a browser and navigate to ``http://SERVER/geoserver`` .
   
   For example, with Tomcat running on port 8080 on localhost, the URL would be ``http://localhost:8080/geoserver``.

3. When you see the GeoServer Welcome page, GeoServer has been successfully installed.

   .. figure:: images/success.png
   
      GeoServer Welcome Page

Update
------

Update GeoServer:

* Backup any customizations you have made to :file:`webapps/geoserver/web.xml`.
  
  In general application properties should be :ref:`configured <application_properties_setting>` using :file:`conf/Catalina/localhost/geoserver.xml` rather
  than by modifying :file:`web.xml` which is replaced each update.

* Follow the :ref:`installation_upgrade` to update :file:`geoserver.war`.

  Before you start, ensure you have moved your data directory to an external location not located inside the :file:`webapps/geoserver/data` folder.

* Be sure to stop the application server before deploying updated :file:`geoserver.war`.
  
  This is important as when Tomcat is running it will replace the entire :file:`webapps/geoserver` folder,
  including any configuration in the default GEOSERVER_DATA_DIR file:`geoserver/data` folder location or
  customizations made to :file:`web.xml`.

* Re-apply any customizations you have made to :file:`webapps/geoserver/web.xml`.

Update Tomcat:

* Update regularly at least the container application! And repeat the hardening process.

  There are a lot of GeoServer installations visible with outdated Tomcat versions.

Uninstallation
--------------

#. Stop the container application.

#. Remove the GeoServer webapp from the container application's ``webapps`` directory. This will usually include the :file:`geoserver.war` file as well as a :file:`geoserver` directory.
   
   Remove :file:`conf/Catalina/localhost/geoserver.xml`.
