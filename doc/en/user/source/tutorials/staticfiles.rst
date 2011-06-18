.. _tutorials_staticfiles:

Serving Static Files
====================

Introduction
------------
Let's say you've just setup your data and styles in Geoserver, and you've created a nice front end with a pure javascript library like OpenLayers or MapBuilder. You're ready to tell the world about your new shiny app, there is only a catch... where do you put your static files (html files, a few icons, some javascript) so that they are served on the web?

So far you did not have a quick solution, and had to use one of the following approaches:

#. Roll your own extra web app to be deployed along with Geoserver (in the same container). This requires some java webapp setup knowledge.
#. Unpack geoserver, modify the webapp contents, repack it (ugly, making Geoserver upgrades inconvenient)
#. Use separate web server (Apache, IIS) to serve the pages (which requires some knowledge on its own).

If you application needed to make ajax calls back to Geoserver (WFS-T requires that) you would stumble into another roadblock: ajax calls are sandboxed so that you can call back only the same server that provided the page making the call. This meant that option #3 was out of the question, and an approach using some proxying (mod_proxy or similar) was required.

Directly from the Data Directory
--------------------------------
With GeoServer you can put your own static files in the www subfolder of the data directory, and have them served at ``http:/myhost:8080/geoserver/www``. This means you can put in your html, images and javascript (even a full installation of MapBuilder) and have Geoserver provide them on the web: no need for unpacking, creating a new webapp, or fiddling with another web server, and no problems with ajax callback.

Now, this is handy, but has its own limitations:

*  we cannot serve files whose MIME type does not get recognized (if you get an HTTP 415 error, this is because we cannot spot your file MIME type);
* the solution is pure java and does not make use of eventual accelerators such as the `Tomcat APR library <http://tomcat.apache.org/tomcat-5.5-doc/apr.html>`_, this means if you have tons of static files to be served at high speed, you probably want to switch back to solution #1 or #3 to get optimal performance.













