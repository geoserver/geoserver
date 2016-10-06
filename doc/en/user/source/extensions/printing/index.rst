GeoServer Printing Module
=========================

The ``printing`` module for GeoServer allows easy hosting of the Mapfish
printing service within a GeoServer instance.  The Mapfish printing module
provides an HTTP API for printing that is useful within JavaScript mapping
applications.  User interface components for interacting with the print service
are available from the Mapfish and GeoExt projects.

Installation
------------

* Download the extension (named like ``geoserver-<version>-printing-plugin.zip``)
  from the geoserver site download page.
* Extract the contents of the ZIP archive into the :file:`/WEB-INF/lib/` in the
  GeoServer webapp.  For example, if you have installed the GeoServer binary to
  :file:`/opt/geoserver-2.6/`, the printing extension JAR files should be
  placed in :file:`/opt/geoserver-2.6/webapps/geoserver/WEB-INF/lib/`.
* After extracting the extension, restart GeoServer in order for the changes to
  take effect.  All further configuration can be done with GeoServer running.

Verifying Installation
----------------------

On the first startup after installation, GeoServer should create a print module
configuration file in :file:`{GEOSERVER_DATA_DIR}/printing/config.yaml`.
Checking for this file's existence is a quick way to verify the module is
installed properly.  It is safe to edit this file; in fact there is currently
no way to modify the print module settings other than by opening this
configuration file in a text editor.  

If the module is installed and configured properly, then you will also be able
to retrieve a list of configured printing parameters from
http://localhost:8080/geoserver/pdf/info.json .  This service must be working
properly for JavaScript clients to use the printing service.

Finally, you can test printing in this :download:`sample page
<files/print-example.html>`. You can load it directly to attempt to produce a
map from a GeoServer running at http://localhost:8080/geoserver/.  If you are
running at a different host and port, you can download the file and
modify it with your HTML editor of choice to use the proper URL.

.. warning::
  
   This sample script points at the development version of GeoExt.  You can
   modify it for production use, but if you are going to do so you should also
   host your own, minified build of GeoExt and OpenLayers.  The libraries used
   in the sample are subject to change without notice, so pages using them may
   change behavior without warning.


MapFish documentation
--------------------------------------

.. toctree::
   :maxdepth: 1

   configuration
   protocol
   faq

