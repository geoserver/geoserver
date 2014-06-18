.. _styler_extension:

GeoExt Styler
=============

Installation
************


   1. Download the REST plugin for your version of GeoServer from the `download page <http://geoserver.org/download>`_ .
   2. Unzip the archive into the WEB-INF/lib directory of the GeoServer installation.
   3. Restart GeoServer
   4. Download the GeoExt Styler extension from `here <http://downloads.sourceforge.net/geoserver/styler-1.7.3.zip>`_ (it says 1.7.3 but the version number doesn't matter.  Soon there will be an updated release)
   5. Unzip the archive into the *www/* directory of the GeoServer data directory.

Usage
*****


   1. Visit 
   `http://localhost:8080/geoserver/www/styler/index.html <http://localhost:8080/geoserver/www/styler/index.html>`_
   2. Use the "Layers" panel to select a layer to style.

       .. figure:: images/layers.png

   3. In the "Legend" panel select a rule by clicking on it.

       .. figure:: images/legend.png

   4. Change the color by clicking in the color box.

       .. figure:: images/color.png

   5. Click on a feature to view information about its attributes and which rules applied to it.

       .. figure:: images/info.png
