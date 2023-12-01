.. _ogr_extension:

OGR based WFS Output Format
============================

The ogr2ogr based output format leverages the availability of the ogr2ogr command to allow the generation of more output formats than GeoServer can natively produce.
The basics idea is to dump to the file system a file that ogr2ogr can translate, invoke it, zip and return the output of the translation.

Out of the box behaviour
------------------------

Out of the box the plugin assumes the following:

* ogr2ogr is available in the path
* the GDAL_DATA variable is pointing to the GDAL data directory (which stores the spatial reference information for GDAL)

In the default configuration the following formats are supported:

* MapInfo in TAB format
* MapInfo in MIF format
* Un-styled KML
* CSV (without geometry data dumps)

The list might be shorter if ogr2ogr has not been built with support for the above formats.

Once installed in GeoServer four new GetFeature output formats will be available, in particular, ``OGR-TAB``, ``OGR-MIF``, ``OGR-KML``, ``OGR-CSV``.

ogr2ogr conversion abilities
----------------------------

The ogr2ogr utility is usually able to convert more formats than the default setup of this output format allows for, but the exact list depends on how the utility was built from sources. To get a full list of the formats available by your ogr2ogr build just run::

   ogr2ogr --help 

and you'll get the full set of options usable by the program, along with the supported formats. For example, the above produces the following output using the FWTools 2.2.8 distribution (which includes ogr2ogr among other useful information and conversion tools)::

   Usage: ogr2ogr [--help-general] [-skipfailures] [-append] [-update] [-gt n]
               [-select field_list] [-where restricted_where] 
               [-sql <sql statement>] 
               [-spat xmin ymin xmax ymax] [-preserve_fid] [-fid FID]
               [-a_srs srs_def] [-t_srs srs_def] [-s_srs srs_def]
               [-f format_name] [-overwrite] [[-dsco NAME=VALUE] ...]
               [-segmentize max_dist]
               dst_datasource_name src_datasource_name
               [-lco NAME=VALUE] [-nln name] [-nlt type] [layer [layer ...]]

   -f format_name: output file format name, possible values are:
     -f "ESRI Shapefile"
     -f "MapInfo File"
     -f "TIGER"
     -f "S57"
     -f "DGN"
     -f "Memory"
     -f "BNA"
     -f "CSV"
     -f "GML"
     -f "GPX"
     -f "KML"
     -f "GeoJSON"
     -f "Interlis 1"
     -f "Interlis 2"
     -f "GMT"
     -f "SQLite"
     -f "ODBC"
     -f "PostgreSQL"
     -f "MySQL"
     -f "Geoconcept"
   -append: Append to existing layer instead of creating new if it exists
   -overwrite: delete the output layer and recreate it empty
   -update: Open existing output datasource in update mode
   -select field_list: Comma-delimited list of fields from input layer to
                       copy to the new layer (defaults to all)
   -where restricted_where: Attribute query (like SQL WHERE)
   -sql statement: Execute given SQL statement and save result.
   -skipfailures: skip features or layers that fail to convert
   -gt n: group n features per transaction (default 200)
   -spat xmin ymin xmax ymax: spatial query extents
   -segmentize max_dist: maximum distance between 2 nodes.
                         Used to create intermediate points
   -dsco NAME=VALUE: Dataset creation option (format specific)
   -lco  NAME=VALUE: Layer creation option (format specific)
   -nln name: Assign an alternate name to the new layer
   -nlt type: Force a geometry type for new layer.  One of NONE, GEOMETRY,
        POINT, LINESTRING, POLYGON, GEOMETRYCOLLECTION, MULTIPOINT,
        MULTIPOLYGON, or MULTILINESTRING.  Add "25D" for 3D layers.
        Default is type of source layer.
   -a_srs srs_def: Assign an output SRS
   -t_srs srs_def: Reproject/transform to this SRS on output
   -s_srs srs_def: Override source SRS

   Srs_def can be a full WKT definition (hard to escape properly),
   or a well known definition (ie. EPSG:4326) or a file with a WKT
   definition.

The full list of formats that ogr2ogr is able to support is available on the `OGR site <http://www.gdal.org/ogr2ogr.html>`_. Mind that this output format can handle only outputs that are file based and that do support creation. So, for example, you won't be able to use the Postgres output (since it's database based) or the ArcInfo binary coverage (creation not supported).

Customisation
-------------

If ogr2ogr is not available in the default path, the GDAL_DATA is not set, or if the output formats needs tweaking, a ``ogr2ogr.xml`` file can be put in the root of the GeoServer data directory to customize the output format.

The default GeoServer configuration is equivalent to the following xml file:

.. code-block:: xml
  
  <OgrConfiguration>
    <ogr2ogrLocation>ogr2ogr</ogr2ogrLocation>
    <!-- <gdalData>...</gdalData> -->
    <formats>
      <Format>
        <ogrFormat>MapInfo File</ogrFormat>
        <formatName>OGR-TAB</formatName>
        <fileExtension>.tab</fileExtension>
      </Format>
      <Format>
        <ogrFormat>MapInfo File</ogrFormat>
        <formatName>OGR-MIF</formatName>
        <fileExtension>.mif</fileExtension>
        <option>-dsco</option>
        <option>FORMAT=MIF</option>
      </Format>
      <Format>
        <ogrFormat>CSV</ogrFormat>
        <formatName>OGR-CSV</formatName>
        <fileExtension>.csv</fileExtension>
        <singleFile>true</singleFile>
        <mimeType>text/csv</mimeType>
      </Format>
      <Format>
        <ogrFormat>KML</ogrFormat>
        <formatName>OGR-KML</formatName>
        <fileExtension>.kml</fileExtension>
        <singleFile>true</singleFile>
        <mimeType>application/vnd.google-earth.kml</mimeType>
      </Format>
    </formats>
  </OgrConfiguration>

The file showcases all possible usage of the configuration elements:

*  ``ogr2ogrLocation`` can be just ogr2ogr if the command is in the path, otherwise it should be the    full path to the executable. For example, on a Windows box with FWTools installed it might be::

      <ogr2ogrLocation>c:\Programmi\FWTools2.2.8\bin\ogr2ogr.exe</ogr2ogrLocation>

*  ``gdalData`` must point to the GDAL data directory. For example, on a Windows box with FWTools installed it might be::

      <gdalData>c:\Programmi\FWTools2.2.8\data</gdalData>

*  ``Format`` defines a single format, which is defined by the following tags:

          * ``ogrFormat``: the name of the format to be passed to ogr2ogr with the -f option (it's case sensitive).
          * ``formatName``: is the name of the output format as advertised by GeoServer
          * ``fileExtension``: is the extension of the file generated after the translation, if any (can be omitted)
          * ``option``: can be used to add one or more options to the ogr2ogr command line. As you can see by the MIF example, each item must be contained in its own tag. You can get a full list of options by running ogr2ogr --help or by visiting the ogr2ogr web page. Also consider that each format supports specific creation options, listed in the description page for each format (for example, here is the MapInfo one).
          * ``singleFile`` (since 2.0.3): if true the output of the conversion is supposed to be a single file that can be streamed directly back without the need to wrap it into a zip file
          * ``mimeType`` (since 2.0.3): the mime type of the file returned when using ``singleFile``. If not specified ``application/octet-stream`` will be used as a default.

OGR based WPS Output Format
===========================

The OGR based WPS output format provides the ability to turn feature collection (vector layer) output types into formats supported by OGR,
using the same configuration and same machinery provided by the OGR WFS output format (which should also be installed for the WPS portion to work).

Unlike the WFS case the WPS output formats are receiving different treatment in WPS responses depending on whether they are binary, text, or xml, when the Execute response
style chosen by the client is "document":

* Binary types need to be base64 encoded for XML embedding
* Text types need to be included inside a CDATA section 
* XML types can be integrated in the response as-is

In order to understand the nature of the output format a new optional configuration element, ``<type>``, can
be added to the ``ogr2ogr.xml`` configuration file in order to specify the output nature. 
The possible values are ``binary``, ``text``, ``xml``, in case the value is missing, ``binary`` is assumed.
Here is an example showing all possible combinations:

.. code-block:: xml

    <OgrConfiguration>
        <ogr2ogrLocation>ogr2ogr</ogr2ogrLocation>
        <!-- <gdalData>...</gdalData> -->
        <formats>
            <Format>
                <ogrFormat>MapInfo File</ogrFormat>
                <formatName>OGR-TAB</formatName>
                <fileExtension>.tab</fileExtension>
                <type>binary</type> <!-- not really required, it’s the default -->
            </Format>
            <Format>
                <ogrFormat>MapInfo File</ogrFormat>
                <formatName>OGR-MIF</formatName>
                <fileExtension>.mif</fileExtension>
                <option>-dsco</option>
                <option>FORMAT=MIF</option>
            </Format>
            <Format>
                <ogrFormat>CSV</ogrFormat>
                <formatName>OGR-CSV</formatName>
                <fileExtension>.csv</fileExtension>
                <singleFile>true</singleFile>
                <mimeType>text/csv</mimeType>
                <option>-lco</option>
                <option>GEOMETRY=AS_WKT</option>
                <type>text</type>
            </Format>
            <Format>
                <ogrFormat>KML</ogrFormat>
                <formatName>OGR-KML</formatName>
                <fileExtension>.kml</fileExtension>
                <singleFile>true</singleFile>
                <mimeType>application/vnd.google-earth.kml</mimeType>
                <type>xml</type>
            </Format>
        </formats>
    </OgrConfiguration>
