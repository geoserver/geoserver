.. _tutorials_staticfiles:

Serving Static Files
====================

Let's say you've just set up your data and styles in Geoserver, and you've created a nice front end application with a Javascript library such as OpenLayers or MapBuilder. 
You're ready to tell the world about your shiny new app.  But there is a catch... 
where do you put the static files (e.g. HTML files, icons, and Javascript) so that they are served on the web?

Until recently there was no quick solution.  You had to use one of the following approaches:

#. Roll your own web app to be deployed along with Geoserver (in the same container). This requires some Java webapp setup knowledge.
#. Unpack the GeoServer WAR, modify the webapp contents, and repack it.  This is tedious, and makes Geoserver upgrades inconvenient.
#. Use a separate web server (e.g. Apache, IIS) to serve the pages (which requires some knowledge on its own).

If your application makes AJAX calls back to Geoserver (WFS-T requires this) you would hit another roadblock: AJAX calls are sandboxed by the browser so that you can onlu call back to the same server that provided the page making the call. 
This means that option #3 is out of the question, and previously an approach using proxying (mod_proxy or similar) was required.

Directly from the Data Directory
--------------------------------

With GeoServer you can put static files in the ``www`` subdirectory of the :ref:`data directory <data_dir_structure>`, and they will be served at ``http:/myhost:8080/geoserver/www``. 
This means you can deploy HTML, images and Javascript (even a full installation of MapBuilder) and have Geoserver serve them on the web. 
There's no need for unpacking, creating a new webapp, or fiddling with another web server, and no problems with AJAX callbacks.

Note that this approach has some limitations:

* GeoServer can only serve files whose MIME type is recognized.  
  If you get an HTTP 415 error, this is because GeoServer cannot determine a file's MIME type.
* This approach does not make use of accelerators such as the `Tomcat APR library <http://tomcat.apache.org/tomcat-5.5-doc/apr.html>`_, 
  If you have many static files to be served at high speed, you probably want to switch back to solution #1 or #3 to get optimal performance.













