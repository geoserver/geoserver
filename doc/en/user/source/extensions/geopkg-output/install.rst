.. _geopkgoutput.install:

Installing the GeoPackage Output Extension
------------------------------------------

The GeoPackage Output extension is an official extension.  Download the extension here - :download_extension:`geopkg-output`

#. Download the extension for your version of GeoServer. 

   .. warning:: Make sure to match the version of the extension to the version of GeoServer.

#. Extract the archive and copy the contents into the GeoServer :file:`WEB-INF/lib` directory.

#. Restart GeoServer.

Verify Installation
^^^^^^^^^^^^^^^^^^^

To verify that the extension was installed successfully:

#. Request the `WFS 1.0.0 <http://localhost:8080/geoserver/ows?service=wfs&version=1.0.0&request=GetCapabilities>`__ GetCapabilities document from your server.
#. Inside the resulting WFS 1.0.0 XML GetCapabilities document, find the ``WFS_Capabilities/Capability/GetFeature/ResultFormat`` section
#. Verify that `geopkg`, `geopackage`, and `gpkg` are listed as a supported format

   .. code-block:: XML

      <GetFeature>
          <ResultFormat>
              <GML2/>
              <GML3/>
              <SHAPE-ZIP/>
              <CSV/>
              <JSON/>
              <KML/>
              <geopackage/>
              <geopkg/>
              <gpkg/>
          </ResultFormat>
      </GetFeature>

.. note::

    You can also verify installation by looking for ``GeoPKG Output Extension`` on the server's `Module Status Page`.