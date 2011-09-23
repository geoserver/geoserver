Installing the CSS Module
=========================

The CSS module is built nightly and published to the `nightly build server <http://gridlock.opengeo.org/geoserver/>`_. Click on your version of Geoserver - eg. ``2.1.x`` and then ``community-latest``.

The installation process is similar to other GeoServer plugins:

1. Download the file named like ``geoserver-2.1-SNAPSHOT-css-plugin.zip``.
   Please verify that the version number in the filename corresponds to the one reported in GeoServer's admin UI.
2. Extract the contents of the ZIP archive into the :file:`/WEB-INF/lib/` direcotry in the GeoServer webapp.
   For example, if you have installed the GeoServer binary to :file:`/opt/geoserver-2.1.0/`, you should place the CSS extension's JAR files in :file:`/opt/geoserver-2.1.0/webapps/geoserver/WEB-INF/lib/`.
3. After extracting the extension, restart GeoServer in order for the changes to take effect.
   All further configuration can be done through the GeoServer web UI.

After installation, you may find the following useful to get you started - :doc:`/community/css/tutorial`.

