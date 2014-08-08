.. _installation_war:

Web archive (WAR)
=================

GeoServer is packaged as a standalone servlet for use with existing servlet container applications such as `Apache Tomcat <http://tomcat.apache.org/>`_ and `Jetty <https://jetty.mortbay.com/>`_.

.. note:: GeoServer has been mostly tested using Tomcat, and therefore these instructions may not work with other container applications.

Java
----

GeoServer requires a *Java 7* runtime to be installed on your system. For more information about Java and GeoServer, please see the section on :ref:`production_java`.
 
Installation
------------

#. Navigate to the `GeoServer Download page <http://geoserver.org/download>`_ and pick the appropriate version to download.

#. Select :guilabel:`Web archive` on the download page.

#. Download and unpack the archive.  Copy the file :file:`geoserver.war` to the directory that contains your container application's webapps.

#. Your container application should unpack the web archive and automatically set up and run GeoServer.

   .. note:: A restart of your container application may be necessary.

Running
-------

Use your container application's method of starting and stopping webapps to run GeoServer.

#. To access the :ref:`web_admin`, open a browser and navigate to ``http://{container_application_URL}/geoserver`` .  For example, with Tomcat running on port 8080 on localhost, the URL would be ``http://localhost:8080/geoserver``.

Uninstallation
--------------

#. Stop the container application.

#. Remove the GeoServer webapp from the container application's webapps directory.