.. _spatialjson_installation:

Installation
============

Manual Installation
-------------------

To download and install the required extensions by hand:

#. Download the geoserver-|release|-spatialjson-plugin.zip from:

   * `Community Builds <https://build.geoserver.org/geoserver/main/community-latest/>`_ (GeoServer WebSite)
   
   It is important to download the version that matches the GeoServer you are running.

#. Stop the GeoServer application.

#. Navigate into the :file:`webapps/geoserver/WEB-INF/lib` folder.

   These files make up the running GeoServer application.

#. Unzip the contents of the zip file into the :file:`lib` folder.

#. Restart the Application Server.

After restarting the Application Server the SpatialJSON WFS output format is available and ready to
use.
