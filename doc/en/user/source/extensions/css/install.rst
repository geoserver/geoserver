Installing the CSS Module
=========================

The CSS extension is listed among the other extension downloads on the GeoServer download page.
Please ensure that you download a version of the extension that corresponds to the version of GeoServer that you use.

The installation process is similar to other GeoServer plugins:

1. Download the ZIP archive.
   Please verify that the version number in the filename corresponds to the one reported in GeoServer's admin UI.
2. Extract the contents of the ZIP archive into the :file:`/WEB-INF/lib/` direcotry in the GeoServer webapp.
   For example, if you have installed the GeoServer binary to :file:`/opt/geoserver-2.4.0/`, you should place the CSS extension's JAR files in :file:`/opt/geoserver-2.4.0/webapps/geoserver/WEB-INF/lib/`.
3. After extracting the extension, restart GeoServer in order for the changes to take effect.
   All further configuration can be done through the GeoServer web UI.

After installation, you may find the following useful to get you started - :doc:`/extensions/css/tutorial`.

Nightly builds
--------------

For those interested in trying out new features and other experimental changes, nightly builds are available from the GeoServer continuous integration system at http://ares.boundlessgeo.com/geoserver/ .
After downloading the ZIP archive, the steps to install are the same as above.
