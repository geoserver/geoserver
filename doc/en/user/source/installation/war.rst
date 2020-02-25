.. _installation_war:

Web archive
===========

GeoServer is packaged as a standalone servlet for use with existing application servers such as `Apache Tomcat <http://tomcat.apache.org/>`_ and `Jetty <http://eclipse.org/jetty/>`_.

.. note:: GeoServer has been mostly tested using Tomcat, and so is the recommended application server. GeoServer requires a newer version of Tomcat (7.0.65 or later) that implements Servlet 3 and annotation processing. Other application servers have been known to work, but are not guaranteed.
 
Installation
------------

#. Make sure you have a Java Runtime Environment (JRE) installed on your system. GeoServer requires a **Java 8** environment. 

   .. note:: Java 9 is not currently supported.

   .. note:: For more information about Java and GeoServer, please see the section on :ref:`production_java`.

#. Navigate to the `GeoServer Download page <http://geoserver.org/download>`_.

#. Select :guilabel:`Web Archive` on the download page.

#. Download and unpack the archive.

#. Deploy the web archive as you would normally. Often, all that is necessary is to copy the :file:`geoserver.war` file to the application server's ``webapps`` directory, and the application will be deployed.

   .. note:: A restart of your application server may be necessary.

Running
-------

Use your container application's method of starting and stopping webapps to run GeoServer. 

To access the :ref:`web_admin`, open a browser and navigate to ``http://SERVER/geoserver`` . For example, with Tomcat running on port 8080 on localhost, the URL would be ``http://localhost:8080/geoserver``.

Uninstallation
--------------

#. Stop the container application.

#. Remove the GeoServer webapp from the container application's ``webapps`` directory. This will usually include the :file:`geoserver.war` file as well as a ``geoserver`` directory.
