.. _printing_install:

Printing Installation
---------------------

1. Login, and navigate to :menuselection:`About & Status > About GeoServer` and check **Build Information**
   to determine the exact version of GeoServer you are running.

2. Visit the :website:`website download <download>` page, change the **Archive** tab,
   and locate your release.
   
   From the list of **Cartography** extensions download **Printing**.
   
   * |release| example: :download_extension:`printing`
  
   * |version| example: :nightly_extension:`printing`

2. Extract the contents of the ZIP archive into the :file:`/WEB-INF/lib/` in the
   GeoServer webapp.
   
   For example, if you have installed the GeoServer binary to
   :file:`/opt/geoserver/`, the printing extension JAR files should be
   placed in :file:`/opt/geoserver/webapps/geoserver/WEB-INF/lib/`.

3. After extracting the extension, restart GeoServer in order for the changes to
   take effect.  All further configuration can be done with GeoServer running.

Verifying Installation
^^^^^^^^^^^^^^^^^^^^^^

1. On the first startup after installation, GeoServer creates a print module
configuration file in :file:`{GEOSERVER_DATA_DIR}/printing/config.yaml`.

   You may override where the :file:`config.yaml` file is located through
   an application property `GEOSERVER_PRINT_CONFIG_DIR`.

2. The name of the file :file:`config.yaml` cannot be changed; it is possible to
specify the folder when this is located.

   Example: `-DGEOSERVER_PRINT_CONFIG_DIR=/tmp/ext_printing`
  
   allows the module searching the configuration file into the external folder
   :file:`/tmp/ext_printing/config.yaml`

3. Checking for :file:`config.yaml` file's existence is a quick way to verify the module is
installed properly.

   It is safe to edit this file; in fact to control the print module settings
   you need to open this configuration file in a text editor.

4. When the module is installed and configured properly, then you will also be able
   to retrieve a list of configured printing parameters from
   http://localhost:8080/geoserver/pdf/info.json .  This service must be working
   properly for JavaScript clients to use the printing service.

5. Finally, you can test printing in this :download:`sample page <files/print-example.html>`.
   You can load it directly to produce a map from a GeoServer running
   at http://localhost:8080/geoserver/.
   
   If you are running at a different host and port, you can download the file and
   modify it with your HTML editor of choice to use the proper URL.

   .. warning::
     
      This sample script points to development version of GeoExt.  You can
      modify it for production use, but if you are going to do so you should also
      host your own, minified build of GeoExt and OpenLayers.  The libraries used
      in the sample are subject to change without notice, so pages using them may
      change behavior without warning.
