.. _tutorials_staticfiles:

Serving Static Files
====================

You can place static files in the ``www`` subdirectory of the GeoServer :ref:`data directory <datadir_structure>`, and they will be served at ``http:/myhost:8080/geoserver/www``.  This means you can deploy HTML, images, or JavaScript, and have GeoServer serve them directly on the web. 

This approach has some limitations:

* GeoServer can only serve files whose MIME type is recognized. If you get an HTTP 415 error, this is because GeoServer cannot determine a file's MIME type.
* This approach does not make use of accelerators such as the `Tomcat APR library <http://tomcat.apache.org/tomcat-7.0-doc/apr.html>`_. If you have many static files to be served at high speed, you may wish to create your own web app to be deployed along with GeoServer or use a separate web server to serve the content.

