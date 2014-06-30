.. _print_protocol:

Protocol
********

Four commands are available and are documented in the next sections.

Every command uses the HTTP status code to notify errors.

info.json
---------

HTTP command::

  GET {PRINT_URL}/info.json?url={PRINT_URL}%2Finfo.json&var=printConfig

Returns a JSON structure as such:

.. code-block:: javascript

    var printConfig = {
        "scales":[
            {"name":"25000"},
            {"name":"50000"},
            {"name":"100000"}
        ],
        "dpis":[
            {"name":"190"},
            {"name":"254"}
        ],
        "outputFormats":[
            {"name":"pdf"},
            {"name":"png"}
        ],
        "layouts":[
            {
                "name":"A4 portrait",
                "map":{
                    "width":440,
                    "height":483
                }
            }
        ],
        "printURL":"http:\/\/localhost:5000\/print\/print.pdf",
        "createURL":"http:\/\/localhost:5000\/print\/create.json"
    }

This can be loaded through an HTML script tag like that::

  <script type="text/javascript"
        src="http://localhost:5000/print/info.json?var=printConfig"></script>

or through an AJAX request, in this case the ``var`` query parameter will be
omitted.

The "url" query parameter is here to help the print servlet to know what URL is used by the browser to access the servlet. This parameter is here because the servlet can be behind a proxy, hiding the real URL.

print.pdf
---------

HTTP command::

  GET {PRINT_URL}/print.pdf?spec={SPEC}
     or
  POST {PRINT_URL}/print.pdf    with {SPEC} in the request body

The "SPEC" parameter is a JSON structure like that:

.. code-block:: javascript

    {
        layout: 'A4 portrait',
        ...CUSTOM_PARAMS...
        srs: 'EPSG:4326',
        units: 'degrees',
        geodetic: false,
        outputFilename: 'political-boundaries',
        outputFormat: 'pdf',
        mergeableParams: {
            cql_filter: {
                defaultValue: 'INCLUDE',
                separator: ';',
                context: 'http://labs.metacarta.com/wms/vmap0'
            }
        },
        layers: [
            {
                type: 'WMS',
                layers: ['basic'],
                baseURL: 'http://labs.metacarta.com/wms/vmap0',
                format: 'image/jpeg'
            }
        ],
        pages: [
            {
                center: [6, 45.5],
                scale: 4000000,
                dpi: 190,
                geodetic: false,
                strictEpsg4326: false,
                ...CUSTOM_PARAMS...
            }
        ],
        legends: [
            {
                classes: [
                    {
                        icons: [
                            'full url to the image'
                        ],
                        name: 'an icon name',
                        iconBeforeName: true
                    }
                ],
                name: 'a class name'
            }
        ]
    }

The location to show on the map can be specified with a **center** and a **scale** as show or with a **bbox** like that::

   bbox: [5, 45, 6, 46]

The print module will use the nearest scale and will make sure the aspect ratio stays correct.

The geodetic parameter can be set to true so the scale of geodetic layers can correctly be calculated.  Certain projections (Google and Latlong for example) are based on a spheroid and therefore require **geodetic: true** in order to correctly calculate the scale.  If the geodetic parameter is not present it will be assumed to be false.

The _optional_ strictEpsg4326 parameter can be set to true to control how EPSG:4326 is interpreted. This needs to be true for WMS version 1.3.0 GetMap requests. See https://www.google.ch/search?q=epsg+4326+latitude+longitude+order&oq=epsg+4326+&aqs=chrome.3.69i57j0l5.5996j0j4&sourceid=chrome&espv=210&es_sm=93&ie=UTF-8 for some links to the history and mess that is EPSG:4326.

The outputFilename parameter is optional and if omitted the values used in the server's configuration will be used instead.  If it is present it will be the name of the downloaded file.  The suffix will be added if not left off in the parameter.  The date can be substituted into the filename as well if desired.  See configuration's outputFilename for more information and examples

The outputFormat parameter is optional and if omitted the value 'pdf' will be used.  Only the formats returned in the info are permitted.

There are two locations where custom parameters can be added. Those will be ignored by the web service but, will be accessible from the layout templates.

Some layer types support merging more layers request into one, when the server is the same (for example WMS). For those, a mergeableParams section can be used to define merging strategies for some custom parameters.
The default rule is to merge layers with identical custom parameters. Using mergeableParams, defined parameters values can be joined using a given separator and a default value if some of the layers miss the parameter.
Mergeable parameters can have a context, that is the baseURL they can be used for (if not defined they will be used for every layer).

For the format of the **layers** section, please look at the implementations pointed by mapfish.PrintProtocol.SUPPORTED_TYPES.

This command returns the PDF file directly.


create.json
-----------

HTTP command::

  POST {PRINT_URL}/create.json?url={PRINT_URL}%2Fcreate.json

The spec defined in the "print.pdf" command must be included in the POST body.

Returns a JSON structure like that:

.. code-block:: javascript

    {
        getURL: 'http:\/\/localhost:5000\/print\/56723.pdf'
    }

The URL returned can be used to retrieve the PDF file. See the next section.

{ID}.pdf
--------
This command's URL is returned by the "create.json" command.

HTTP command::

  GET {PRINT_URL}/{ID}.pdf

Returns the PDF. Can be called only during a limited time since the server side temporary file is deleted afterwards.

Multiple maps on a single page
******************************
To print more than one map on a single page you need to:
 * specify several map blocks in a page of the yaml file, each with a distinct name property value
 * use a particular syntax in the spec to bind different rendering properties to each map block
 
This is possible specifying a _maps_ object in spec root object with a distinct key - object pair for each map. The
key will refer the map block name as defined in yaml file. The object will contain layers and srs for the named map.
Another _maps_ object has to be specified inside the page object to describe positioning, scale and so on.

.. code-block:: javascript

    {
        ...
        maps: {
            "main": {
                layers: [
                    ...
                ],
                srs: 'EPSG:4326'
            },
            "other": {
                layers: [
                    ...
                ],
                srs: 'EPSG:4326'
            }
        },
        ...
        pages: [
            {
                maps: {
                    "main": {
                        center: [6, 45.5],
                        scale: 4000000,
                        dpi: 190,
                        geodetic: false,
                        strictEpsg4326: false,
                        ...CUSTOM_PARAMS...
                    },
                    "other": {
                        center: [7.2, 38.6],
                        scale: 1000000,
                        dpi: 300,
                        geodetic: false,
                        strictEpsg4326: false,
                        ...CUSTOM_PARAMS...
                    }
                }
                
            }
        ],
        ...
    }

Other config blocks have been enabled to multiple maps usage.
The scalebar block can be bound to a specific map, specifying a name property that matches the map
name.
Also, in text blocks you can use the ${scale.<mapname>} placeholder to print the scale of the map
whose name is <mapname>.

Layers Params
*************

Vector
------
Type: vector

Render vector layers. The geometries and the styling comes directly from the spec JSON.

* opacity (Defaults to ``1.0``)
* geoJson (Required) the geoJson to render
* styleProperty (Defaults to '_style') Name of the property within the features to use as style name. The given property may contain a style object directly.
* styles (Optional) dictonary of styles. One style is defined as in OpenLayers.Feature.Vector.style.
* name (Defaults to ``vector``) the layer name.

WMS
---
Type: wms

Support for the WMS protocol with possibilities to go through a WMS-C service (TileCache).

* opacity (Defaults to ``1.0``)
* baseURL (Required) Service URL
* customParams (Optional) Map, additional URL arguments
* layers (Required)
* styles (Optional)
* format (Required)
* version (Defaults to ``1.1.1``)
* useNativeAngle (Defaults to false) it true transform the map angle to customParams.angle for GeoServer, and customParams.map_angle for MapServer.

WMTS
----
Type: wmts

Support for the protocol using directly the content of a WMTS tiled layer, support REST or KVP.

Two possible mode, standard or simple, the simple mode imply that all the topLeftCorner are identical.

Standard mode:

* opacity (Defaults to 1.0)
* baseURL the 'ResourceURL' available in the WMTS capabilities.
* customParams (Optional) Map, additional URL arguments
* layer (Required) the layer name
* version (Defaults to ``1.0.0``) WMTS protocol version
* requestEncoding (Defaults to ``REST``) ``REST`` or ``KVP``
* style (Optional) the style name
* dimensions (Optional) list of dimensions names
* params (Optional) dictionary of dimensions name (capital) => value
* matrixSet (Required) the name of the matrix set
* matrixIds (Required) array of matrix ids e.g.:

.. code-block:: javascript

    [{
        "identifier": "0",
        "matrixSize": [1, 1],
        "resolution": 4000,
        "tileSize": [256, 256],
        "topLeftCorner": [420000, 350000]
    }, ...]

* format (Optional, Required id requestEncoding is ``KVP``)

Simple mode:

* baseURL base URL without the version.
* layer (Required)
* version (Required)
* requestEncoding (Required) ``REST``
* tileOrigin (Required)
* tileSize (Required)
* extension (Required)
* resolutions (Required)
* style (Required)
* tileFullExtent (Required)
* zoomOffset (Required)
* dimensions (Optional)
* params (Optional)
* formatSuffix (Required)

Tms
---
Type: tms

Support the TMS tile layout.

* opacity (Defaults to 1.0)
* baseURL (Required) Service URL
* customParams (Optional) Map, additional URL arguments
* maxExtent (Required) Array, extent coordinates ``[420000, 30000, 900000, 350000]``
* tileSize (Required) Array, tile size e.g. ``[256, 256]``
* format (Required)
* layer (Required)
* resolutions (Required) Array of resolutions
* tileOrigin (Optional) Object, tile origin.  Defaults to ``0,0``

Resources:

* Quick intro to TMS requests: http://geowebcache.org/docs/current/services/tms.html
* TMS Spec (Not an Official Standard): http://wiki.osgeo.org/wiki/Tile_Map_Service_Specification

Xyz
---
Type: xyz

Support the tile layout z/x/y.<extension>.

* opacity (Defaults to 1.0)
* baseURL (Required) Service URL
* customParams (Optional) Map, additional URL arguments
* maxExtent (Required) Array, extent coordinates ``[420000, 30000, 900000, 350000]``
* tileSize (Required) Array, tile size e.g. ``[256, 256]``
* resolutions (Required) Array of resolutions (Required) Array of resolutions
* extension (Required) file extension (Required) file extension
* tileOrigin (Optional) Array, tile origine e.g. ``[420000, 350000]``
* tileOriginCorner ``tl`` or ``bl`` (Defaults to ``bl``)
* path_format (Optional) url fragment used to construct the tile location. Can support variable replacement of ``${x}``, ``${y}``, ``${z}`` and ``${extension}``. Defaults to zz/x/y.extension format.  You can use multiple "letters" to indicate a replacable pattern (aka, ``${zzzz}`` will ensure the z variable is 0 padded to have a length of AT LEAST 4 characters).

Osm
---
Type: osm

Support the OSM tile layout.

* opacity (Defaults to ``1.0``)
* baseURL (Required) Service URL
* customParams (Optional) Map, additional URL arguments
* maxExtent (Required) Array, extent coordinates ``[420000, 30000, 900000, 350000]``
* tileSize (Required) Array, tile size e.g. ``[256, 256]``
* resolutions (Required) Array of resolutions
* extension (Required) file extension

TileCache
---------
Type: tileCache

Support for the protocol using directly the content of a TileCache directory.

* opacity (Defaults to ``1.0``)
* baseURL (Required) Service URL
* customParams (Optional) Map, additional URL arguments
* layer (Required)
* maxExtent (Required) Array, extent coordinates ``[420000, 30000, 900000, 350000]``
* tileSize (Required) Array, tile size e.g. ``[256, 256]``
* resolutions (Required) Array of resolutions
* extension (Required) file extension

Image
-----
Type: image

* opacity (Defaults to ``1.0``)
* name (Required)
* baseURL (Required) Service URL
* extent (Required)

MapServer
---------
Type: mapServer

Support mapserver WMS server.

* opacity (Defaults to ``1.0``)
* baseURL (Required) Service URL
* customParams (Optional) Map, additional URL arguments
* layers (Required)
* format (Required)

KaMap
-----
Type: kaMap

Support for the protocol using the KaMap tiling method

* opacity (Defaults to ``1.0``)
* baseURL (Required) Service URL
* customParams (Optional) Map, additional URL arguments
* map
* group
* maxExtent (Required) Array, extent coordinates ``[420000, 30000, 900000, 350000]``
* tileSize (Required) Array, tile size e.g. ``[256, 256]``
* resolutions (Required) Array of resolutions
* extension (Required) file extension

KaMapCache
----------
Type: kaMapCache

Support for the protocol talking direclty to a web-accessible ka-Map cache generated by the precache2.php script.

* opacity (Defaults to ``1.0``)
* baseURL (Required) Service URL
* customParams (Optional) Map, additional URL arguments
* map (Required)
* group (Required)
* metaTileWidth (Required)
* metaTileHeight (Required)
* units (Required)
* maxExtent (Required) Array, extent coordinates ``[420000, 30000, 900000, 350000]``
* tileSize (Required) Array, tile size e.g. ``[256, 256]``
* resolutions (Required) Array of resolutions
* extension (Required) file extension

Google
------
Type: google or tiledGoogle

They used the Google Map Static API, tiledGoogle will create tiles and google only one image.

The google map reader has several custom parameters that can be added to the request they are:

* opacity (Optional, Defaults to ``1.0``)
* baseURL (Required, should be 'http://maps.google.com/maps/api/staticmap')
* customParams (Optional) Map, additional URL arguments
* maxExtent (Required, should be ``[-20037508.34, -20037508.34, 20037508.34, 20037508.34]``)
* resolutions (Required, should be ``[156543.03390625, 78271.516953125, 39135.7584765625, 19567.87923828125, 9783.939619140625, 4891.9698095703125, 2445.9849047851562, 1222.9924523925781, 611.4962261962891, 305.74811309814453, 152.87405654907226, 76.43702827453613, 38.218514137268066, 19.109257068634033, 9.554628534317017, 4.777314267158508, 2.388657133579254, 1.194328566789627, 0.5971642833948135, 0.29858214169740677, 0.14929107084870338, 0.07464553542435169]``)
* extension (Required, should be ``png``)
* client (Optional)
* format (Optional)
* maptype (Required) - type of map to display: http://code.google.com/apis/maps/documentation/staticmaps/#MapTypes
* sensor  (Optional) - specifies whether the application requesting the static map is using a sensor to determine the user's location
* language (Optional) - language of labels.
* markers (Optional) - add markers to the map: http://code.google.com/apis/maps/documentation/staticmaps/#Markers

.. code-block:: javascript

    markers: ['color:blue|label:S|46.5195933305192,6.566684726913701']

* path (Optional) - add a path to the map: http://code.google.com/apis/maps/documentation/staticmaps/#Paths

.. code-block:: javascript

    path: 'color:0x0000ff|weight:5|46.5095933305192,6.506684726913701|46.5195933305192,6.526684726913701|46.5395933305192,6.536684726913701|46.5695933305192,6.576684726913701',
    
Warranty disclaimer and license
-------------------------------

The authors provide these documents "AS-IS", without warranty of any kind
either expressed or implied.

Document under `Creative Common License Attribution-Share Alike 2.5 Generic
<http://creativecommons.org/licenses/by-sa/2.5/>`_.

Authors: MapFish developers.