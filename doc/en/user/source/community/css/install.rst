Installing the GeoServer CSS Module
===================================

The CSS module is built nightly and published to the `nightly build server <http://gridlock.opengeo.org/geoserver/2.1.x/community-latest/>`_.
The installation process is similar to other GeoServer plugins:

1. Download the file named like ``geoserver-2.0.2-SNAPSHOT-css-plugin.zip``.
   Please verify that the version number in the filename corresponds to the one reported in GeoServer's admin UI.
2. Extract the contents of the ZIP archive into the :file:`/WEB-INF/lib/` direcotry in the GeoServer webapp.
   For example, if you have installed the GeoServer binary to :file:`/opt/geoserver-2.1.0/`, you should place the CSS extension's JAR files in :file:`/opt/geoserver-2.1.0/webapps/geoserver/WEB-INF/lib/`.
3. After extracting the extension, restart GeoServer in order for the changes to take effect.
   All further configuration can be done through the GeoServer web UI.

After installation, you may find the following document useful in getting started styling layers with CSS: :doc:`/community/css/tutorial`.
