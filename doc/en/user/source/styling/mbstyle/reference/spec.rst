Mapbox Style Specification
==========================

A Mapbox style is a document that defines the visual appearance of a map: what data to draw, the order to draw it in, and how to style the data when drawing it. A style document is a `JSON <http://www.json.org/>`__ object with specific root level and nested properties. This specification defines and describes these properties.

The intended audience of this quick reference includes:

-  Advanced designers and cartographers who want to write styles by hand
-  GeoTools developers using the mbstyle module
-  Authors of software that generates or processes Mapbox styles.
- Feature support is provided for the `Mapbox GL JS <https://www.mapbox.com/mapbox-gl-js/api/>`__, the `Open Layers Mapbox Style utility <https://npmjs.com/package/ol-mapbox-style>`__ and the GeoTools mbstyle module.
- Where appropriate examples have been changed to reference `GeoWebCache <http://geowebcache.org/>`__.

.. note::
      The `Mapbox Style Specification <https://www.mapbox.com/mapbox-gl-style-spec>`__ is generated from the BSD `Mapbox GL JS <https://github.com/mapbox/mapbox-gl-js>`__ github repository, reproduced here with details on this GeoTools implementation.


Root Properties
---------------

Root level properties of a Mapbox style specify the map's layers, tile sources and other resources, and default values for the initial camera position when not specified elsewhere.

::

    {
        "version": 8,
        "name": "Mapbox Streets",
        "sprite": "sprites/streets-v8",
        "glyphs": "{fontstack}/{range}.pbf",
        "sources": {...},
        "layers": [...]
    }


version
~~~~~~~

*Required* :ref:`types-enum`.

Style specification version number. Must be 8.

::

    "version": 8


name
~~~~

*Optional* :ref:`types-string`.

A human-readable name for the style.

::

    "name": "Bright"

metadata
~~~~~~~~

*Optional*

Arbitrary properties useful to track with the stylesheet, but do not influence rendering. Properties should be prefixed to avoid collisions.

.. note:: *unsupported.*

center
~~~~~~

*Optional* :ref:`types-array`.


Default map center in longitude and latitude. The style center will be used only if the map has not been positioned by other means (e.g. map options or user interaction).

::

    "center": [
      -73.9749, 40.7736
    ]

.. note:: *unsupported*

zoom
~~~~

*Optional* :ref:`types-number`.


Default zoom level. The style zoom will be used only if the map has not
been positioned by other means (e.g. map options or user interaction).

::

    "zoom": 12.5

bearing
~~~~~~~

*Optional* :ref:`types-number`. *Units in degrees. Defaults to* 0.

Default bearing, in degrees clockwise from true north. The style bearing
will be used only if the map has not been positioned by other means
(e.g. map options or user interaction).

::

    "bearing": 29

.. note:: *unsupported*

pitch
~~~~~

*Optional* :ref:`types-number`. *Units in degrees. Defaults to* 0.

Default pitch, in degrees. Zero is perpendicular to the surface, for a
look straight down at the map, while a greater value like 60 looks ahead
towards the horizon. The style pitch will be used only if the map has
not been positioned by other means (e.g. map options or user
interaction).

::

    "pitch": 50

light
~~~~~

The global light source.

::

    "light": {
      "anchor": "viewport",
      "color": "white",
      "intensity": 0.4
    }

sources
~~~~~~~

*Required* :ref:`sources`.


Data source specifications.

::

    "sources": {
      "mapbox-streets": {
        "type": "vector",
        "tiles": [
          "http://localhost:8080/geoserver/gwc/service/wmts?REQUEST=GetTile
              &SERVICE=WMTS&VERSION=1.0.0&LAYER=mapbox:streets&STYLE=
              &TILEMATRIX=EPSG:900913:{z}&TILEMATRIXSET=EPSG:900913
              &FORMAT=application/vnd.mapbox-vector-tile
              &TILECOL={x}&TILEROW={y}"
        ],
        "minZoom": 0,
        "maxZoom": 14
      }
    }

sprite
~~~~~~

*Optional* :ref:`types-string`.



A base URL for retrieving the sprite image and metadata. The extensions
``.png``, ``.json`` and scale factor ``@2x.png`` will be automatically
appended. This property is required if any layer uses the
``background-pattern``, ``fill-pattern``, ``line-pattern``,
``fill-extrusion-pattern``, or ``icon-image`` properties.

::

    "sprite" : "/geoserver/styles/mark"

glyphs
~~~~~~

*Optional* :ref:`types-string`.



A URL template for loading signed-distance-field glyph sets in PBF
format. The URL must include ``{fontstack}`` and ``{range}`` tokens.
This property is required if any layer uses the ``text-field`` layout
property.

::

    "glyphs": "{fontstack}/{range}.pbf"

transition
~~~~~~~~~~

*Required* :ref:`transition`.



A global transition definition to use as a default across properties.

::

    "transition": {
      "duration": 300,
      "delay": 0
    }

layers
~~~~~~

*Required* :ref:`types-array`.



Layers will be drawn in the order of this array.

::

    "layers": [
      {
        "id": "water",
        "source": "sf:roads",
        "source-layer": "water",
        "type": "fill",
        "paint": {
          "fill-color": "#00ffff"
        }
      }
    ]

Light
-----

A style's ``light`` property provides global light source for that
style.

::

    "light": {
      "anchor": "viewport",
      "color": "white",
      "intensity": 0.4
    }


anchor
~~~~~~

*Optional* :ref:`types-enum`. *One of* map, viewport. *Defaults to* viewport.


Whether extruded geometries are lit relative to the map or viewport.


map
    The position of the light source is aligned to the rotation of the
    map.

viewport
    The position of the light source is aligned to the rotation of the
    viewport.

::

    "anchor": "map"

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.27.0
     - Not yet supported
     - Not yet supported

position
~~~~~~~~

*Optional* :ref:`types-array`. *Defaults to* 1.15,210,30.


Position of the light source relative to lit (extruded) geometries, in
[r radial coordinate, a azimuthal angle, p polar angle] where r
indicates the distance from the center of the base of an object to its
light, a indicates the position of the light relative to 0° (0° when
``light.anchor`` is set to ``viewport`` corresponds to the top of the
viewport, or 0° when ``light.anchor`` is set to ``map`` corresponds to
due north, and degrees proceed clockwise), and p indicates the height of
the light (from 0°, directly above, to 180°, directly below).

::

    "position": [
      1.5,
      90,
      80
    ]


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.27.0
     - Not yet supported
     - Not yet supported

color
~~~~~

*Optional* :ref:`types-color`. *Defaults to* #ffffff.


Color tint for lighting extruded geometries.


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.27.0
     - Not yet supported
     - Not yet supported

intensity
~~~~~~~~~

*Optional* :ref:`types-number`. *Defaults to* 0.5.


Intensity of lighting (on a scale from 0 to 1). Higher numbers will
present as more extreme contrast.


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.27.0
     - Not yet supported
     - Not yet supported

.. _sources:

Sources
-------

Sources supply data to be shown on the map. The type of source is
specified by the ``"type"`` property, and must be one of vector, raster,
geojson, image, video, canvas. Adding a source won't immediately make
data appear on the map because sources don't contain styling details
like color or width. Layers refer to a source and give it a visual
representation. This makes it possible to style the same source in
different ways, like differentiating between types of roads in a
highways layer.

Tiled sources (vector and raster) must specify their details in terms of
the `TileJSON
specification <https://github.com/mapbox/tilejson-spec>`__. This can be
done in several ways:

-  By supplying TileJSON properties such as ``"tiles"``, ``"minzoom"``,
   and ``"maxzoom"`` directly in the source:

   ::

       "mapbox-streets": {
         "type": "vector",
         "tiles": [
           "http://a.example.com/tiles/{z}/{x}/{y}.pbf",
           "http://b.example.com/tiles/{z}/{x}/{y}.pbf"
         ],
         "maxzoom": 14
       }

-  By providing a ``"url"`` to a TileJSON resource:


   ::

       "mapbox-streets": {
         "type": "vector",
         "url": "http://api.example.com/tilejson.json"
       }

-  By providing a url to a WMS server that supports EPSG:3857 (or
   EPSG:900913) as a source of tiled data. The server url should contain
   a ``"{bbox-epsg-3857}"`` replacement token to supply the ``bbox``
   parameter.

   ::

       "wms-imagery": {
         "type": "raster",
         "tiles": [
         'http://a.example.com/wms?bbox={bbox-epsg-3857}&format=image/png&service=WMS&version=1.1.1&request=GetMap&srs=EPSG:3857&width=256&height=256&layers=example'
         ],
         "tileSize": 256
       }

vector
~~~~~~

A vector tile source. Tiles must be in `Mapbox Vector Tile
format <https://www.mapbox.com/developers/vector-tiles/>`__. All
geometric coordinates in vector tiles must be between ``-1 * extent``
and ``(extent * 2) - 1`` inclusive. All layers that use a vector source
must specify a ``"source-layer"`` value. For vector tiles hosted by
Mapbox, the ``"url"`` value should be of the form ``mapbox://mapid``.
::

    "mapbox-streets": {
      "type": "vector",
      "tiles": [
        "http://localhost:8080/geoserver/gwc/service/wmts?REQUEST=GetTile&SERVICE=WMTS
            &VERSION=1.0.0&LAYER=mapbox:streets&STYLE=&TILEMATRIX=EPSG:900913:{z}
            &TILEMATRIXSET=EPSG:900913&FORMAT=application/vnd.mapbox-vector-tile
            &TILECOL={x}&TILEROW={y}"
      ],
      "minZoom": 0,
      "maxZoom": 14
    }

url
^^^

*Optional* :ref:`types-string`.



A URL to a TileJSON resource. Supported protocols are ``http:``,
``https:``, and ``mapbox://<mapid>``.

tiles
^^^^^

*Optional* :ref:`types-array`.



An array of one or more tile source URLs, as in the TileJSON spec.

minzoom
^^^^^^^

*Optional* :ref:`types-number`. *Defaults to* 0.


Minimum zoom level for which tiles are available, as in the TileJSON
spec.

maxzoom
^^^^^^^

*Optional* :ref:`types-number`. *Defaults to* 22.


Maximum zoom level for which tiles are available, as in the TileJSON
spec. Data from tiles at the maxzoom are used when displaying the map at
higher zoom levels.


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - Not yet supported
     - >= 2.4.0

raster
~~~~~~

A raster tile source. For raster tiles hosted by Mapbox, the ``"url"``
value should be of the form ``mapbox://mapid``.

::

    "mapbox-satellite": {
      "type": "raster",
      "tiles": [
        "http://localhost:8080/geoserver/gwc/service/wmts?REQUEST=GetTile&SERVICE=WMTS
            &VERSION=1.0.0&LAYER=mapbox:satellite&STYLE=&TILEMATRIX=EPSG:900913:{z}
            &TILEMATRIXSET=EPSG:900913&FORMAT=image/png&TILECOL={x}&TILEROW={y}"
      ],
      "minzoom": 0,
      "maxzoom": 14
    }

url
^^^

*Optional* :ref:`types-string`.


A URL to a TileJSON resource. Supported protocols are ``http:``,
``https:``, and ``mapbox://<mapid>``.

tiles
^^^^^

*Optional* :ref:`types-array`.



An array of one or more tile source URLs, as in the TileJSON spec.

minzoom
^^^^^^^

*Optional* :ref:`types-number`. *Defaults to* 0.


Minimum zoom level for which tiles are available, as in the TileJSON
spec.

maxzoom
^^^^^^^

*Optional* :ref:`types-number`. *Defaults to* 22.


Maximum zoom level for which tiles are available, as in the TileJSON
spec. Data from tiles at the maxzoom are used when displaying the map at
higher zoom levels.

tileSize
^^^^^^^^

*Optional* :ref:`types-number`. *Defaults to* 512.


The minimum visual size to display tiles for this layer. Only
configurable for raster layers.

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - Not yet supported
     - >= 2.4.0

geojson
~~~~~~~

A `GeoJSON <http://geojson.org/>`__ source. Data must be provided via a
``"data"`` property, whose value can be a URL or inline GeoJSON.
::

    "geojson-marker": {
      "type": "geojson",
      "data": {
        "type": "Feature",
        "geometry": {
          "type": "Point",
          "coordinates": [-77.0323, 38.9131]
        },
        "properties": {
          "title": "Mapbox DC",
          "marker-symbol": "monument"
        }
      }
    }


This example of a GeoJSON source refers to an external GeoJSON document
via its URL. The GeoJSON document must be on the same domain or
accessible using `CORS <http://enable-cors.org/>`__.
::

    "geojson-lines": {
      "type": "geojson",
      "data": "./lines.geojson"
    }

data
^^^^

*Optional*


A URL to a GeoJSON file, or inline GeoJSON.

maxzoom
^^^^^^^

*Optional* :ref:`types-number`. *Defaults to* 18.


Maximum zoom level at which to create vector tiles (higher means greater
detail at high zoom levels).

buffer
^^^^^^

*Optional* :ref:`types-number`. *Defaults to* 128.


Size of the tile buffer on each side. A value of 0 produces no buffer. A
value of 512 produces a buffer as wide as the tile itself. Larger values
produce fewer rendering artifacts near tile edges and slower
performance.

tolerance
^^^^^^^^^

*Optional* :ref:`types-number`. *Defaults to* 0.375.


Douglas-Peucker simplification tolerance (higher means simpler
geometries and faster performance).

cluster
^^^^^^^

*Optional* :ref:`types-boolean`. *Defaults to* false.


If the data is a collection of point features, setting this to true
clusters the points by radius into groups.

clusterRadius
^^^^^^^^^^^^^

*Optional* :ref:`types-number`. *Defaults to* 50.



Radius of each cluster if clustering is enabled. A value of 512
indicates a radius equal to the width of a tile.

clusterMaxZoom
^^^^^^^^^^^^^^

*Optional* :ref:`types-number`.


Max zoom on which to cluster points if clustering is enabled. Defaults
to one zoom less than maxzoom (so that last zoom features are not
clustered).

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - Not yet supported
     - >= 2.4.0
   * - clustering
     - >= 0.14.0
     - Not yet supported
     - Not yet supported

image
~~~~~

An image source. The ``"url"`` value contains the image location.

The ``"coordinates"`` array contains ``[longitude, latitude]`` pairs for
the image corners listed in clockwise order: top left, top right, bottom
right, bottom left.
::

    "image": {
      "type": "image",
      "url": "/mapbox-gl-js/assets/radar.gif",
      "coordinates": [
          [-80.425, 46.437],
          [-71.516, 46.437],
          [-71.516, 37.936],
          [-80.425, 37.936]
      ]
    }

url
^^^

*Required* :ref:`types-string`.

URL that points to an image.

coordinates
^^^^^^^^^^^

*Required* :ref:`types-array`.

Corners of image specified in longitude, latitude pairs.

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - Not yet supported
     - Not yet supported

video
~~~~~

A video source. The ``"urls"`` value is an array. For each URL in the
array, a video element
`source <https://developer.mozilla.org/en-US/docs/Web/HTML/Element/source>`__
will be created, in order to support same media in multiple formats
supported by different browsers.

The ``"coordinates"`` array contains ``[longitude, latitude]`` pairs for
the video corners listed in clockwise order: top left, top right, bottom
right, bottom left.
::

    "video": {
      "type": "video",
      "urls": [
        "https://www.mapbox.com/drone/video/drone.mp4",
        "https://www.mapbox.com/drone/video/drone.webm"
      ],
      "coordinates": [
         [-122.51596391201019, 37.56238816766053],
         [-122.51467645168304, 37.56410183312965],
         [-122.51309394836426, 37.563391708549425],
         [-122.51423120498657, 37.56161849366671]
      ]
    }

urls
^^^^

*Required* :ref:`types-array`.



URLs to video content in order of preferred format.

coordinates
^^^^^^^^^^^

*Required* :ref:`types-array`.


Corners of video specified in longitude, latitude pairs.

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - Not yet supported
     - Not yet supported

canvas
~~~~~~

A canvas source. The ``"canvas"`` value is the ID of the canvas element
in the document.

The ``"coordinates"`` array contains ``[longitude, latitude]`` pairs for
the video corners listed in clockwise order: top left, top right, bottom
right, bottom left.

If an HTML document contains a canvas such as this:
::

    <canvas id="mycanvas" width="400" height="300" style="display: none;"></canvas>


the corresponding canvas source would be specified as follows:

::

    "canvas": {
      "type": "canvas",
      "canvas": "mycanvas",
      "coordinates": [
         [-122.51596391201019, 37.56238816766053],
         [-122.51467645168304, 37.56410183312965],
         [-122.51309394836426, 37.563391708549425],
         [-122.51423120498657, 37.56161849366671]
      ]
    }

coordinates
^^^^^^^^^^^

*Required* :ref:`types-array`.



Corners of canvas specified in longitude, latitude pairs.

animate
^^^^^^^

Whether the canvas source is animated. If the canvas is static,
``animate`` should be set to ``false`` to improve performance.

canvas
^^^^^^

*Required* :ref:`types-string`.

HTML ID of the canvas from which to read pixels.


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.32.0
     - Not yet supported
     - Not yet yupported

.. _sprite:

Sprite
------

A style's ``sprite`` property supplies a URL template for loading small
images to use in rendering ``background-pattern``, ``fill-pattern``,
``line-pattern``, and ``icon-image`` style properties.

::

    "sprite" : "/geoserver/styles/mark"


A valid sprite source must supply two types of files:

-  An *index file*, which is a JSON document containing a description of
   each image contained in the sprite. The content of this file must be
   a JSON object whose keys form identifiers to be used as the values of
   the above style properties, and whose values are objects describing
   the dimensions (``width`` and ``height`` properties) and pixel ratio
   (``pixelRatio``) of the image and its location within the sprite
   (``x`` and ``y``). For example, a sprite containing a single image
   might have the following index file contents:

   ::

       {
         "poi": {
           "width": 32,
           "height": 32,
           "x": 0,
           "y": 0,
           "pixelRatio": 1
         }
       }

   Then the style could refer to this sprite image by creating a symbol
   layer with the layout property ``"icon-image": "poi"``, or with the
   tokenized value ``"icon-image": "{icon}"`` and vector tile features
   with a ``icon`` property with the value ``poi``.
-  *Image files*, which are PNG images containing the sprite data.

Mapbox SDKs will use the value of the ``sprite`` property in the style
to generate the URLs for loading both files. First, for both file types,
it will append ``@2x`` to the URL on high-DPI devices. Second, it will
append a file extension: ``.json`` for the index file, and ``.png`` for
the image file. For example, if you specified
``"sprite": "https://example.com/sprite"``, renderers would load
``https://example.com/sprite.json`` and
``https://example.com/sprite.png``, or
``https://example.com/sprite@2x.json`` and
``https://example.com/sprite@2x.png``.

If you are using Mapbox Studio, you will use prebuilt sprites provided
by Mapbox, or you can upload custom SVG images to build your own sprite.
In either case, the sprite will be built automatically and supplied by
Mapbox APIs. If you want to build a sprite by hand and self-host the
files, you can use
`spritezero-cli <https://github.com/mapbox/spritezero-cli>`__, a command
line utility that builds Mapbox GL compatible sprite PNGs and index
files from a directory of SVGs.

.. _glyphs:

Glyphs
------

A style's ``glyphs`` property provides a URL template for loading
signed-distance-field glyph sets in PBF format.

::

    "glyphs": "{fontstack}/{range}.pbf"


This URL template should include two tokens:

-  ``{fontstack}`` When requesting glyphs, this token is replaced with a
   comma separated list of fonts from a font stack specified in the
   ```text-font`` <#layout-symbol-text-font>`__ property of a symbol
   layer.
-  ``{range}`` When requesting glyphs, this token is replaced with a
   range of 256 Unicode code points. For example, to load glyphs for the
   `Unicode Basic Latin and Basic Latin-1 Supplement
   blocks <https://en.wikipedia.org/wiki/Unicode_block>`__, the range
   would be ``0-255``. The actual ranges that are loaded are determined
   at runtime based on what text needs to be displayed.

.. _transition:

Transition
----------

A style's ``transition`` property provides global transition defaults
for that style.

::

    "transition": {
      "duration": 300,
      "delay": 0
    }

duration
~~~~~~~~

*Optional* :ref:`types-number`. *Units in milliseconds. Defaults to* 300.


Time allotted for transitions to complete.

delay
~~~~~

*Optional* :ref:`types-number`. *Units in milliseconds. Defaults to* 0.

Length of time before a transition begins.

.. _layers:

Layers
------

A style's ``layers`` property lists all of the layers available in that
style. The type of layer is specified by the ``"type"`` property, and
must be one of background, fill, line, symbol, raster, circle,
fill-extrusion.

Except for layers of the background type, each layer needs to refer to a
source. Layers take the data that they get from a source, optionally
filter features, and then define how those features are styled.

::

    "layers": [
      {
        "id": "water",
        "source": "sf:roads",
        "source-layer": "water",
        "type": "fill",
        "paint": {
          "fill-color": "#00ffff"
        }
      }
    ]

Layer Properties
~~~~~~~~~~~~~~~~

id
^^

*Required* :ref:`types-string`.


Unique layer name.

type
^^^^

*Optional* :ref:`types-enum`. *One of fill, line, symbol, circle, fill-extrusion, raster, background.*


Rendering type of this layer.


*fill*
    A filled polygon with an optional stroked border.

*line*
    A stroked line.

*symbol*
    An icon or a text label.

*circle*
    A filled circle.

*fill-extrusion*
    An extruded (3D) polygon.

*raster*
    Raster map textures such as satellite imagery.

*background*
    The background color or pattern of the map.

metadata
^^^^^^^^

*Optional*


Arbitrary properties useful to track with the layer, but do not
influence rendering. Properties should be prefixed to avoid collisions,
like 'mapbox:'.

source
^^^^^^

*Optional* :ref:`types-string`.



Name of a source description to be used for this layer.

source-layer
^^^^^^^^^^^^

*Optional* :ref:`types-string`.



Layer to use from a vector tile source. Required if the source supports
multiple layers.

minzoom
^^^^^^^

*Optional* :ref:`types-number`.



The minimum zoom level on which the layer gets parsed and appears on.

maxzoom
^^^^^^^

*Optional* :ref:`types-number`.



The maximum zoom level on which the layer gets parsed and appears on.

filter
^^^^^^

*Optional* :ref:`Expression <expressions>`.



A expression specifying conditions on source features. Only features
that match the filter are displayed.

layout
^^^^^^

layout properties for the layer

paint
^^^^^

*Optional* paint properties for the layer

Layers have two sub-properties that determine how data from that layer
is rendered: ``layout`` and ``paint`` properties.

*Layout properties* appear in the layer's ``"layout"`` object. They are
applied early in the rendering process and define how data for that
layer is passed to the GPU. For efficiency, a layer can share layout
properties with another layer via the ``"ref"`` layer property, and
should do so where possible. This will decrease processing time and
allow the two layers will share GPU memory and other resources
associated with the layer.

*Paint properties* are applied later in the rendering process. A layer
that shares layout properties with another layer can have independent
paint properties. Paint properties appear in the layer's ``"paint"``
object.

background
~~~~~~~~~~

Layout Properties
^^^^^^^^^^^^^^^^^

visibility
""""""""""

*Optional* :ref:`types-enum`. *One of* visible, none, *Defaults to* visible.


Whether this layer is displayed.


visible
    The layer is shown.

none
    The layer is not shown.

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - Not yet supported
     - >= 2.4.0

Paint Properties
^^^^^^^^^^^^^^^^

background-color
""""""""""""""""

*Optional* :ref:`types-color`. *Defaults to* #000000. *Disabled by* background-pattern.


The color with which the background will be drawn.


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - Not yet supported
     - >= 2.4.0

background-pattern
""""""""""""""""""

*Optional* :ref:`types-string`.



Name of image in sprite to use for drawing an image background. For
seamless patterns, image width and height must be a factor of two (2, 4,
8, ..., 512).


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - Not yet supported
     - Not yet supported

background-opacity
""""""""""""""""""

*Optional* :ref:`types-number`. *Defaults to* 1.

The opacity at which the background will be drawn.

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - Not yet supported
     - >= 2.4.0

fill
~~~~

Layout Properties
^^^^^^^^^^^^^^^^^

visibility
""""""""""


*Optional* :ref:`types-enum`. *One of* visible, none. *Defaults to* visible.

Whether this layer is displayed.

visible
    The layer is shown.

none
    The layer is not shown.

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - >= 17.1
     - >= 2.4.0

Paint Properties
^^^^^^^^^^^^^^^^

fill-antialias
""""""""""""""

*Optional* :ref:`types-boolean`. *Defaults to* true.


Whether or not the fill should be antialiased.


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - Not yet supported
     - Not yet supported
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported

fill-opacity
""""""""""""

*Optional* :ref:`types-number`. *Defaults to* 1.


The opacity of the entire fill layer. In contrast to the ``fill-color``,
this value will also affect the 1px stroke around the fill, if the
stroke is used.

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - >= 17.1
     - >= 2.4.0
   * - data-driven styling
     - >= 0.21.0
     - >= 17.1
     - >= 2.4.0

fill-color
""""""""""

*Optional* :ref:`types-color`. *Defaults to* #000000. *Disabled by* fill-pattern.


The color of the filled part of this layer. This color can be specified
as ``rgba`` with an alpha component and the color's opacity will not
affect the opacity of the 1px stroke, if it is used.


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - >= 17.1
     - >= 2.4.0
   * - data-driven styling
     - >= 0.19.0
     - >= 17.1
     - >= 2.4.0

fill-outline-color
""""""""""""""""""

*Optional* :ref:`types-color`. *Disabled by* fill-pattern. *Requires* fill-antialias = true.


The outline color of the fill. Matches the value of ``fill-color`` if
unspecified.

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - >= 17.1
     - >= 2.4.0
   * - data-driven styling
     - >= 0.19.0
     - >= 17.1
     - >= 2.4.0

fill-translate
""""""""""""""

*Optional* :ref:`types-array`. *Units in* pixels. *Defaults to* 0.0.


The geometry's offset. Values are [x, y] where negatives indicate left
and up, respectively.

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - Not yet supported
     - Not yet supported
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported

fill-translate-anchor
"""""""""""""""""""""

*Optional* :ref:`types-enum`. *One of* map, viewport. *Defaults to* map. *Requires* fill-translate.

Controls the translation reference point.

map
    The fill is translated relative to the map.

viewport
    The fill is translated relative to the viewport.

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - Not yet supported
     - Not yet supported
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported

fill-pattern
""""""""""""

*Optional* :ref:`types-string`.


Name of image in sprite to use for drawing image fills. For seamless
patterns, image width and height must be a factor of two (2, 4, 8, ...,
512).

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - >= 17.1
     - Not yet supported
   * - data-driven styling
     - Not yet supported
     - >= 17.1
     - Not yet supported

line
~~~~

Layout Properties
^^^^^^^^^^^^^^^^^

line-cap
""""""""

*Optional* :ref:`types-enum`. *One of* butt, round, square. *Defaults to* butt.

The display of line endings.


butt
    A cap with a squared-off end which is drawn to the exact endpoint of
    the line.

round
    A cap with a rounded end which is drawn beyond the endpoint of the
    line at a radius of one-half of the line's width and centered on the
    endpoint of the line.

square
    A cap with a squared-off end which is drawn beyond the endpoint of
    the line at a distance of one-half of the line's width.


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - >= 17.1
     - >= 2.4.0
   * - data-driven styling
     - Not yet supported
     - >= 17.1
     - >= 2.4.0

line-join
"""""""""

*Optional* :ref:`types-enum`. *One of* bevel, round, miter. *Defaults to* miter.

The display of lines when joining.


bevel
    A join with a squared-off end which is drawn beyond the endpoint of
    the line at a distance of one-half of the line's width.

round
    A join with a rounded end which is drawn beyond the endpoint of the
    line at a radius of one-half of the line's width and centered on the
    endpoint of the line.

miter
    A join with a sharp, angled corner which is drawn with the outer
    sides beyond the endpoint of the path until they meet.


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - >= 17.1
     - >= 2.4.0
   * - data-driven styling
     - Not yet supported
     - >= 17.1
     - >= 2.4.0

line-miter-limit
""""""""""""""""

*Optional* :ref:`types-number`. *Defaults to* 2. *Requires* line-join = miter.

Used to automatically convert miter joins to bevel joins for sharp
angles.


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - Not yet supported
     - >= 2.4.0
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - >= 2.4.0

line-round-limit
""""""""""""""""

*Optional* :ref:`types-number`. *Defaults to* 1.05. *Requires* line-join = round.


Used to automatically convert round joins to miter joins for shallow
angles.


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - Not yet supported
     - Not yet supported
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported


visibility
""""""""""

*Optional* :ref:`types-enum`. *One of* visible, none. *Defaults to* visible.

Whether this layer is displayed.


visible
    The layer is shown.

none
    The layer is not shown.


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - >= 17.1
     - >= 2.4.0
   * - data-driven styling
     - Not yet supported
     - >= 17.1
     - >= 2.4.0


Paint Properties
^^^^^^^^^^^^^^^^

line-opacity
""""""""""""

*Optional* :ref:`types-number`. *Defaults to* 1.


The opacity at which the line will be drawn.


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - >= 17.1
     - >= 2.4.0
   * - data-driven styling
     - >= 0.29.0
     - >= 17.1
     - >= 2.4.0


line-color
""""""""""

*Optional* :ref:`types-color`. *Defaults to* #000000. *Disabled by* line-pattern.

The color with which the line will be drawn.


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - >= 17.1
     - >= 2.4.0
   * - data-driven styling
     - >= 0.23.0
     - >= 17.1
     - >= 2.4.0


line-translate
""""""""""""""

*Optional* :ref:`types-array`. *Units in* pixels. *Defaults to* 0.0.


The geometry's offset. Values are [x, y] where negatives indicate left
and up, respectively.


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - Not yet supported
     - Not yet supported
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported

line-translate-anchor
"""""""""""""""""""""

*Optional* :ref:`types-enum`. *One of* map, viewport. *Defaults to* map. *Requires* line-translate.

Controls the translation reference point.


map
    The line is translated relative to the map.

viewport
    The line is translated relative to the viewport.


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - Not yet supported
     - Not yet supported
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported


line-width
""""""""""

*Optional* :ref:`types-number`. *Units in* pixels. *Defaults to* 1.

Stroke thickness.


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - >= 17.1
     - >= 2.4.0
   * - data-driven styling
     - Not yet supported
     - >= 17.1
     - >= 2.4.0

line-gap-width
""""""""""""""

*Optional* :ref:`types-number`. *Units in* pixels. *Defaults to* 0.



Draws a line casing outside of a line's actual path. Value indicates the
width of the inner gap.


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - Not yet supported
     - Not yet supported
   * - data-driven styling
     - >= 0.29.0
     - Not yet supported
     - Not yet supported

line-offset
"""""""""""

*Optional* :ref:`types-number`. *Units in* pixels. *Defaults to* 0.


The line's offset. For linear features, a positive value offsets the
line to the right, relative to the direction of the line, and a negative
value to the left. For polygon features, a positive value results in an
inset, and a negative value results in an outset.

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.12.1
     - >= 17.1
     - Not yet supported
   * - data-driven styling
     - >= 0.29.0
     - >= 17.1
     - Not yet supported

line-blur
"""""""""

*Optional* :ref:`types-number`. *Units in* pixels. *Defaults to* 0.


Blur applied to the line, in pixels.

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - Not yet supported
     - Not yet supported
   * - data-driven styling
     - >= 0.29.0
     - Not yet supported
     - Not yet supported


line-dasharray
""""""""""""""

*Optional* :ref:`types-array`. *Units in* line widths. *Disabled by* line-pattern.

Specifies the lengths of the alternating dashes and gaps that form the
dash pattern. The lengths are later scaled by the line width. To convert
a dash length to pixels, multiply the length by the current line width.

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - >= 17.1
     - >= 2.4.0
   * - data-driven styling
     - Not yet supported
     - >= 17.1
     - >= 2.4.0

line-pattern
""""""""""""

*Optional* :ref:`types-string`.



Name of image in sprite to use for drawing image lines. For seamless
patterns, image width must be a factor of two (2, 4, 8, ..., 512).

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - >= 17.1
     - Not yet supported
   * - data-driven styling
     - Not yet supported
     - >= 17.1
     - Not yet supported

symbol
~~~~~~

Layout Properties
^^^^^^^^^^^^^^^^^

symbol-placement
""""""""""""""""


*Optional* :ref:`types-enum`. *One of* point, line. *Defaults to* point.

Label placement relative to its geometry.


point
    The label is placed at the point where the geometry is located.

line
    The label is placed along the line of the geometry. Can only be used
    on ``LineString`` and ``Polygon`` geometries.

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - >= 17.1
     - >= 2.10.0
   * - data-driven styling
     - Not yet supported
     - >= 17.1
     - >= 2.10.0


symbol-spacing
""""""""""""""

*Optional* :ref:`types-number`. *Units in* pixels. *Defaults to* 250. *Requires* symbol-placement = line.

Distance between two symbol anchors.

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - >= 17.1
     - Not yet supported
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported


symbol-avoid-edges
""""""""""""""""""

*Optional* :ref:`types-boolean`. *Defaults to* false.


If true, the symbols will not cross tile edges to avoid mutual
collisions. Recommended in layers that don't have enough padding in the
vector tile to prevent collisions, or if it is a point symbol layer
placed after a line symbol layer.

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - Not yet supported
     - Not yet supported
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported


icon-allow-overlap
""""""""""""""""""

*Optional* :ref:`types-boolean`. *Defaults to* false. *Requires* icon-image.


If true, the icon will be visible even if it collides with other
previously drawn symbols.

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - Not yet supported
     - Not yet supported
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported

icon-ignore-placement
"""""""""""""""""""""

*Optional* :ref:`types-boolean`. *Defaults to* false. *Requires* icon-image.


If true, other symbols can be visible even if they collide with the
icon.

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - Not yet supported
     - Not yet supported
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported


icon-optional
"""""""""""""

*Optional* :ref:`types-boolean`. *Defaults to* false. *<Requires* icon-image, text-field.



If true, text will display without their corresponding icons when the
icon collides with other symbols and the text does not.

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - Not yet supported
     - Not yet supported
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported

icon-rotation-alignment
"""""""""""""""""""""""

*Optional* :ref:`types-enum`. *One of* map, viewport, auto. *Defaults to* auto. *Requires* icon-image.

In combination with ``symbol-placement``, determines the rotation
behavior of icons.


map
    When ``symbol-placement`` is set to ``point``, aligns icons
    east-west. When ``symbol-placement`` is set to ``line``, aligns icon
    x-axes with the line.

viewport
    Produces icons whose x-axes are aligned with the x-axis of the
    viewport, regardless of the value of ``symbol-placement``.

auto
    When ``symbol-placement`` is set to ``point``, this is equivalent to
    ``viewport``. When ``symbol-placement`` is set to ``line``, this is
    equivalent to ``map``.

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - Not yet supported
     - Not yet supported
   * - ``auto`` value
     - >= 0.25.0
     - Not yet supported
     - Not yet supported
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported

icon-size
"""""""""

*Optional* :ref:`types-number`. *Defaults to* 1. *Requires* icon-image.
Scale factor for icon. 1 is original size, 3 triples the size.


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - Not yet supported
     - >= 2.4.0
   * - data-driven styling
     - >= 0.35.0
     - Not yet supported
     - >= 2.4.0

icon-text-fit
"""""""""""""

*Optional* :ref:`types-enum`. *One of* none, width, height, both. *Defaults to* none. *Requires* icon-image, text-field.


Scales the icon to fit around the associated text.


none
    The icon is displayed at its intrinsic aspect ratio.

width
    The icon is scaled in the x-dimension to fit the width of the text.

height
    The icon is scaled in the y-dimension to fit the height of the text.

both
    The icon is scaled in both x- and y-dimensions.


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.21.0
     - >= 17.1
     - Not yet supported
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported

icon-text-fit-padding
"""""""""""""""""""""

*Optional :ref:`types-array`. *Units in* pixels. *Defaults to* 0,0,0,0. *Requires* icon-image, text-field, icon-text-fit = one of both, width, height.

Size of the additional area added to dimensions determined by
``icon-text-fit``, in clockwise order: top, right, bottom, left.


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.21.0
     - >= 17.1
     - Not yet supported
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported


icon-image
""""""""""

*Optional* :ref:`types-string`.



Name of image in sprite to use for drawing an image background. A string
with {tokens} replaced, referencing the data property to pull from.



.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - >= 17.1
     - >= 2.4.0
   * - data-driven styling
     - Not yet supported
     - >= 17.1
     - >= 2.4.0

icon-rotate
"""""""""""

*Optional* :ref:`types-number`. *Units in* degrees. *Defaults to* 0. *Requires* icon-image.

Rotates the icon clockwise.



.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - >= 17.1
     - >= 2.4.0
   * - data-driven styling
     - >= 0.21.0
     - >= 17.1
     - >= 2.4.0

icon-padding
""""""""""""

*Optional* :ref:`types-number`. *Units in* pixels. *Defaults to* 2. *Requires* icon-image.


Size of the additional area around the icon bounding box used for
detecting symbol collisions.


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - Not yet supported
     - Not yet supported
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported

icon-keep-upright
"""""""""""""""""

*Optional* :ref:`types-boolean`. *Defaults to* false. *Requires* icon-image, icon-rotation-alignment = map, symbol-placement = line.


If true, the icon may be flipped to prevent it from being rendered
upside-down.


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - Not yet supported
     - Not yet supported
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported

icon-offset
"""""""""""

*Optional* :ref:`types-array`. *Defaults to* 0,0. *Requires* icon-image.

Offset distance of icon from its anchor. Positive values indicate right
and down, while negative values indicate left and up. When combined with
``icon-rotate`` the offset will be as if the rotated direction was up.


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - >= Not yet supported
     - Not yet supported
   * - data-driven styling
     - >= 0.29.0
     - >= Not yet supported
     - Not yet supported


text-pitch-alignment
""""""""""""""""""""

*Optional* :ref:`types-enum` *One of* map, viewport, auto. *Defaults to* auto. *Requires* text-field.

Orientation of text when map is pitched.


map
    The text is aligned to the plane of the map.

viewport
    The text is aligned to the plane of the viewport.

auto
    Automatically matches the value of ``text-rotation-alignment``.

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - Not yet supported
     - Not yet supported
   * - ``auto`` value
     - >= 0.25.0
     - Not yet supported
     - Not yet supported
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported


text-rotation-alignment
"""""""""""""""""""""""

*Optional* :ref:`types-enum`. *One of* map, viewport, auto. *Defaults to* auto. *Requires* text-field.

In combination with ``symbol-placement``, determines the rotation
behavior of the individual glyphs forming the text.


map
    When ``symbol-placement`` is set to ``point``, aligns text
    east-west. When ``symbol-placement`` is set to ``line``, aligns text
    x-axes with the line.

viewport
    Produces glyphs whose x-axes are aligned with the x-axis of the
    viewport, regardless of the value of ``symbol-placement``.

auto
    When ``symbol-placement`` is set to ``point``, this is equivalent to
    ``viewport``. When ``symbol-placement`` is set to ``line``, this is
    equivalent to ``map``.

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - Not yet supported
     - Not yet supported
   * - ``auto`` value
     - >= 0.25.0
     - Not yet supported
     -
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported

text-field
""""""""""

*Optional* :ref:`types-string`.



Value to use for a text label. Feature properties are specified using
tokens like {field\_name}. (Token replacement is only supported for
literal ``text-field`` values--not for property functions.)


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - >= 17.1
     - >= 2.4.0
   * - data-driven styling
     - >= 0.33.0
     - >= 17.1
     - >= 2.4.0

text-font
"""""""""

*Optional* :ref:`types-array`. *Defaults to* Open Sans Regular,Arial Unicode MS Regular. *Requires* text-field.

Font stack to use for displaying text.

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - >= 17.1
     - >= 2.4.0
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - >= 2.4.0

text-size
"""""""""

*Optional* :ref:`types-number`. *Units in* pixels. *Defaults to* 16. *Requires* text-field.



Font size.



.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - >= 17.1
     - >= 2.4.0
   * - data-driven styling
     - >= 0.35.0
     - >= 17.1
     - >= 2.4.0

text-max-width
""""""""""""""

*Optional* :ref:`types-number`. *Units in* pixels. *Defaults to* 10. *Requires* text-field.



The maximum line width for text wrapping.


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - Not yet supported
     - >= 2.4.0
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - >= 2.4.0

text-line-height
""""""""""""""""

*Optional* :ref:`types-number`. *Units in* ems. *Defaults to* 1.2. *Requires* text-field.



Text leading value for multi-line text.


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - Not yet supported
     - Not yet supported
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported


text-letter-spacing
"""""""""""""""""""

*Optional* :ref:`types-number`. *Units in* ems. *Defaults to* 0. *Requires* text-field.



Text tracking amount.


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - Not yet supported
     - Not yet supported
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported

text-justify
""""""""""""

*Optional* :ref:`types-enum`. *One of* left, center, right. *Defaults to* center. *Requires* text-field.


Text justification options.


left
    The text is aligned to the left.

center
    The text is centered.

right
    The text is aligned to the right.


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - Not yet supported
     - Not yet supported
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported

text-anchor
"""""""""""

*Optional* :ref:`types-enum`. *One of* center, left, right, top, bottom, top-left, top-right, bottom-left, bottom-right.
*Defaults to* center. *Requires* text-field.



Part of the text placed closest to the anchor.


center
    The center of the text is placed closest to the anchor.

left
    The left side of the text is placed closest to the anchor.

right
    The right side of the text is placed closest to the anchor.

top
    The top of the text is placed closest to the anchor.

bottom
    The bottom of the text is placed closest to the anchor.

top-left
    The top left corner of the text is placed closest to the anchor.

top-right
    The top right corner of the text is placed closest to the anchor.

bottom-left
    The bottom left corner of the text is placed closest to the anchor.

bottom-right
    The bottom right corner of the text is placed closest to the anchor.


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - Not yet supported
     - >= 2.4.0
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - >= 2.4.0


text-max-angle
""""""""""""""

*Optional* :ref:`types-number`. *Units in* degrees. *Defaults to* 45. *Requires* text-field, symbol-placement = line.


Maximum angle change between adjacent characters.


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - Not yet supported
     - >= 2.10.0
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - >= 2.10.0

text-rotate
"""""""""""

*Optional* :ref:`types-number`. *Units in* degrees. *Defaults to* 0. *Requires* text-field.



Rotates the text clockwise.


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - Not yet supported
     - >= 2.10.0
   * - data-driven styling
     - >= 0.35.0
     - Not yet supported
     - >= 2.10.0

text-padding
""""""""""""

*Optional* :ref:`types-number`. *Units in* pixels. *Defaults to* 2. *Requires* text-field.



Size of the additional area around the text bounding box used for
detecting symbol collisions.


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - Not yet supported
     - Not yet supported
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported


text-keep-upright
"""""""""""""""""

*Optional* :ref:`types-boolean`. *Defaults to* true. *Requires* text-field, text-rotation-alignment = true, symbol-placement = true.



If true, the text may be flipped vertically to prevent it from being
rendered upside-down.


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - Not yet supported
     - Not yet supported
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported

text-transform
""""""""""""""

*Optional* :ref:`types-enum`. *One of* none, uppercase, lowercase. *Defaults to* none. *Requires* text-field.

Specifies how to capitalize text, similar to the CSS ``text-transform``
property.


none
    The text is not altered.

uppercase
    Forces all letters to be displayed in uppercase.

lowercase
    Forces all letters to be displayed in lowercase.


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - Not yet supported
     - >= 2.4.0
   * - data-driven styling
     - >= 0.33.0
     - Not yet supported
     - >= 2.4.0

text-offset
"""""""""""

*Optional* :ref:`types-array`. *Units in* pixels. *Defaults to* 0,0. *Requires* icon-image.

Offset distance of text from its anchor. Positive values indicate right
and down, while negative values indicate left and up.


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - Not yet supported
     - >= 2.4.0
   * - data-driven styling
     - >= 0.35.0
     - Not yet supported
     - >= 2.4.0

text-allow-overlap
""""""""""""""""""

*Optional* :ref:`types-boolean`. *Defaults to* false. *Requires* text-field.



If true, the text will be visible even if it collides with other
previously drawn symbols.


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - >= 17.1
     - Not yet supported
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported

text-ignore-placement
"""""""""""""""""""""

*Optional* :ref:`types-boolean`. *Defaults to* false. *Requires* text-field



If true, other symbols can be visible even if they collide with the
text.


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - Not yet supported
     - Not yet supported
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported

text-optional
"""""""""""""

*Optional* :ref:`types-boolean`. *Defaults to* false. *Requires* text-field, icon-image.



If true, icons will display without their corresponding text when the
text collides with other symbols and the icon does not.


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - Not yet supported
     - Not yet supported
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported


visibility
""""""""""

*Optional* :ref:`types-enum`. *One of* visible, none. *Defaults to* visible.



Whether this layer is displayed.


visible
    The layer is shown.

none
    The layer is not shown.


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - >= 17.1
     - >= 2.4.0
   * - data-driven styling
     - Not yet supported
     - >= 17.1
     - >= 2.4.0

Paint Properties
^^^^^^^^^^^^^^^^

icon-opacity
""""""""""""

*Optional* :ref:`types-number`. *Defaults to* 1. <i>Requires </i>icon-image.


The opacity at which the icon will be drawn.


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - >= 17.1
     - >= 2.4.0
   * - data-driven styling
     - >= 0.33.0
     - >= 17.1
     - >= 2.4.0


icon-color
""""""""""

*Optional* :ref:`types-color`. *Defaults to* #000000. *Requires* icon-image.



The color of the icon. This can only be used with sdf icons.


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - Not yet supported
     - >= 2.10.0
   * - data-driven styling
     - >= 0.33.0
     - Not yet supported
     - >= 2.10.0

icon-halo-color
"""""""""""""""

*Optional* :ref:`types-color`. *Defaults to* rgba(0, 0, 0, 0). *Requires* icon-image.



The color of the icon's halo. Icon halos can only be used with SDF
icons.


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - Not yet supported
     - Not yet supported
   * - data-driven styling
     - >= 0.33.0
     - Not yet supported
     - Not yet supported

icon-halo-width
"""""""""""""""

*Optional* :ref:`types-number`. *Units in* pixels. *Defaults to* 0. *Requires* icon-image.



Distance of halo to the icon outline.


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - Not yet supported
     - Not yet supported
   * - data-driven styling
     - >= 0.33.0
     - Not yet supported
     - Not yet supported

icon-halo-blur
""""""""""""""

*Optional* :ref:`types-number`. *Units in* pixels. *Defaults to* 0. *Requires* icon-image.



Fade out the halo towards the outside.


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - Not yet supported
     - Not yet supported
   * - data-driven styling
     - >= 0.33.0
     - Not yet supported
     - Not yet supported

icon-translate
""""""""""""""

*Optional* :ref:`types-array`. *Units in* pixels. *Defaults to* 0,0. *Requires* icon-image.



Distance that the icon's anchor is moved from its original placement.
Positive values indicate right and down, while negative values indicate
left and up.



.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - Not yet supported
     - Not yet supported
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported

icon-translate-anchor
"""""""""""""""""""""

*Optional* :ref:`types-enum` *One of* map, viewport. *Defaults to* map. *Requires* icon-image, icon-translate.



Controls the translation reference point.


map
    Icons are translated relative to the map.

viewport
    Icons are translated relative to the viewport.

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - Not yet supported
     - Not yet supported
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported


text-opacity
""""""""""""

*Optional* :ref:`types-number`. *Defaults to* 1. <i>Requires </i>text-field.


The opacity at which the text will be drawn.



.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - >= 17.1
     - Not yet supported
   * - data-driven styling
     - >= 0.33.0
     - >= 17.1
     - Not yet supported


text-color
""""""""""

*Optional* :ref:`types-color`. *Defaults to* #000000. *Requires* text-field.



The color with which the text will be drawn.



.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - >= 17.1
     - >= 2.4.0
   * - data-driven styling
     - >= 0.33.0
     - >= 17.1
     - >= 2.4.0


text-halo-color
"""""""""""""""

*Optional* :ref:`types-color`. *Defaults to* rgba(0, 0, 0, 0). *Requires* text-field.



The color of the text's halo, which helps it stand out from backgrounds.



.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - >= 17.1
     - >= 2.4.0
   * - data-driven styling
     - >= 0.33.0
     - >= 17.1
     - >= 2.4.0

text-halo-width
"""""""""""""""

*Optional* :ref:`types-number`. *Units in* pixels. *Defaults to* 0. *Requires* text-field.



Distance of halo to the font outline. Max text halo width is 1/4 of the
font-size.



.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - >= 17.1
     - >= 2.4.0
   * - data-driven styling
     - >= 0.33.0
     - >= 17.1
     - >= 2.4.0

text-halo-blur
""""""""""""""

*Optional* :ref:`types-number`. *Units in* pixels. *Defaults to* 0. *Requires* text-field.



The halo's fadeout distance towards the outside.


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - Not yet supported
     - Not yet supported
   * - data-driven styling
     - >= 0.33.0
     - Not yet supported
     - Not yet supported

text-translate
""""""""""""""

*Optional* :ref:`types-array`. *Units in* pixels. *Defaults to* 0,0. *Requires* text-field.



Distance that the text's anchor is moved from its original placement.
Positive values indicate right and down, while negative values indicate
left and up.



.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - Not yet supported
     - Not yet supported
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported

text-translate-anchor
"""""""""""""""""""""

*Optional* :ref:`types-enum` *One of* map, viewport. *Defaults to* map. *Requires* text-field, text-translate.



Controls the translation reference point.


map
    The text is translated relative to the map.

viewport
    The text is translated relative to the viewport.

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - Not yet supported
     - Not yet supported
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported


raster
~~~~~~

Layout Properties
^^^^^^^^^^^^^^^^^

visibility
""""""""""

*Optional* :ref:`types-enum`. *One of* visible, none. *Defaults to* visible.



Whether this layer is displayed.


visible
    The layer is shown.

none
    The layer is not shown.


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - >= 17.1
     - Not yet supported
   * - data-driven styling
     - Not yet supported
     - >= 17.1
     - Not yet supported

Paint Properties
^^^^^^^^^^^^^^^^

raster-opacity
""""""""""""""

*Optional* :ref:`types-number`. *Defaults to* 1.


The opacity at which the image will be drawn.



.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - >= 17.1
     - Not yet supported
   * - data-driven styling
     - Not yet supported
     - >= 17.1
     - Not yet supported

`raster-hue-rotate <#paint-raster-hue-rotate>`__

*Optional* :ref:`types-number`. *Units in* degrees. *Defaults to* 0.



Rotates hues around the color wheel.



.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - Not yet supported
     - Not yet supported
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported


raster-brightness-min
"""""""""""""""""""""

*Optional* :ref:`types-number`. *Defaults to* 0.


Increase or reduce the brightness of the image. The value is the minimum
brightness.

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - Not yet supported
     - Not yet supported
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported


raster-brightness-max
"""""""""""""""""""""

*Optional* :ref:`types-number`. *Defaults to* 1.


Increase or reduce the brightness of the image. The value is the maximum
brightness.

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - Not yet supported
     - Not yet supported
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported

raster-saturation
"""""""""""""""""

*Optional* :ref:`types-number`. *Defaults to* 0.


Increase or reduce the saturation of the image.

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - Not yet supported
     - Not yet supported
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported

raster-contrast
"""""""""""""""

*Optional* :ref:`types-number`. *Defaults to* 0.


Increase or reduce the contrast of the image.

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - Not yet supported
     - Not yet supported
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported


raster-fade-duration
""""""""""""""""""""

*Optional* :ref:`types-number` *Units in* milliseconds. *Defaults to* 300.



Fade duration when a new tile is added.


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - Not yet supported
     - Not yet supported
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported


circle
~~~~~~

Layout Properties
^^^^^^^^^^^^^^^^^

visibility
""""""""""

*Optional* :ref:`types-enum`. *One of* visible, none. *Defaults to* visible.



Whether this layer is displayed.


visible
    The layer is shown.

none
    The layer is not shown.

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - >= 17.1
     - >= 2.4.0


Paint Properties
^^^^^^^^^^^^^^^^

circle-radius
"""""""""""""

*Optional* :ref:`types-number`. *Units in* pixels. *Defaults to* 5.



Circle radius.


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - >= 17.1
     - >= 2.4.0
   * - data-driven styling
     - >= 0.18.0
     - >= 17.1
     - >= 2.4.0

circle-color
""""""""""""

*Optional* :ref:`types-color`. *Defaults to* #000000.



The fill color of the circle.


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - >= 17.1
     - >= 2.4.0
   * - data-driven styling
     - >= 0.18.0
     - >= 17.1
     - >= 2.4.0


circle-blur
"""""""""""

*Optional* :ref:`types-number`. *Defaults to* 0.


Amount to blur the circle. 1 blurs the circle such that only the
centerpoint is full opacity.


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - Not yet supported
     - Not yet supported
   * - data-driven styling
     - >= 0.20.0
     - Not yet supported
     - Not yet supported


circle-opacity
""""""""""""""

*Optional* :ref:`types-number`. *Defaults to* 1.


The opacity at which the circle will be drawn.


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - >= 17.1
     - >= 2.10.0
   * - data-driven styling
     - >= 0.20.0
     - >= 17.1
     - >= 2.10.0

circle-translate
""""""""""""""""

*Optional* :ref:`types-array`. *Units in* pixels. *Defaults to* 0,0.



The geometry's offset. Values are [x, y] where negatives indicate left
and up, respectively.


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - >= 17.1
     - Not yet supported
   * - data-driven styling
     - Not yet supported
     - >= 17.1
     - Not yet supported


circle-translate-anchor
"""""""""""""""""""""""

*Optional* :ref:`types-enum` *One of* map, viewport. *Defaults to* map. *Requires* circle-translate.



Controls the translation reference point.


map
    The circle is translated relative to the map.

viewport
    The circle is translated relative to the viewport.

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - Not yet supported
     - Not yet supported
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported


circle-pitch-scale
""""""""""""""""""

*Optional* :ref:`types-enum` *One of* map, viewport. *Defaults to* map.



Controls the scaling behavior of the circle when the map is pitched.


map
    Circles are scaled according to their apparent distance to the
    camera.

viewport
    Circles are not scaled.

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.21.0
     - Not yet supported
     - Not yet supported
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported


circle-stroke-width
"""""""""""""""""""

*Optional* :ref:`types-number`. *Units in* pixels. *Defaults to* 5.



The width of the circle's stroke. Strokes are placed outside of the
``circle-radius``.


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.29.0
     - >= 17.1
     - >= 2.10.0
   * - data-driven styling
     - >= 0.29.0
     - >= 17.1
     - >= 2.10.0

circle-stroke-color
"""""""""""""""""""

*Optional* :ref:`types-color`. *Defaults to* #000000.



The stroke color of the circle.


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.29.0
     - >= 17.1
     - >= 2.4.0
   * - data-driven styling
     - >= 0.29.0
     - >= 17.1
     - >= 2.4.0

circle-stroke-opacity
"""""""""""""""""""""

*Optional* :ref:`types-number`. *Defaults to* 1.


The opacity of the circle's stroke.


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.29.0
     - >= 17.1
     - Not yet supported
   * - data-driven styling
     - >= 0.29.0
     - >= 17.1
     - Not yet supported

fill-extrusion
~~~~~~~~~~~~~~

Layout Properties
^^^^^^^^^^^^^^^^^

visibility
""""""""""

*Optional* :ref:`types-enum`. *One of* visible, none. *Defaults to* visible.



Whether this layer is displayed.


visible
    The layer is shown.

none
    The layer is not shown.

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.27.0
     - >= 17.1
     - Not yet supported


Paint Properties
^^^^^^^^^^^^^^^^

fill-extrusion-opacity
""""""""""""""""""""""

*Optional* :ref:`types-number`. *Defaults to* 1.


The opacity of the entire fill extrusion layer. This is rendered on a
per-layer, not per-feature, basis, and data-driven styling is not
available.



.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.27.0
     - >= 17.1
     - Not yet supported

fill-extrusion-color
""""""""""""""""""""

*Optional* :ref:`types-color`. *Defaults to* #000000. *Disabled by* fill-extrusion-pattern.


The base color of the extruded fill. The extrusion's surfaces will be
shaded differently based on this color in combination with the root
``light`` settings. If this color is specified as ``rgba`` with an alpha
component, the alpha component will be ignored; use
``fill-extrusion-opacity`` to set layer opacity.


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.27.0
     - >= 17.1
     - Not yet supported

fill-extrusion-translate
""""""""""""""""""""""""

*Optional* :ref:`types-array`. *Units in* pixels. *Defaults to* 0,0.



The geometry's offset. Values are [x, y] where negatives indicate left
and up (on the flat plane), respectively.


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.27.0
     - Not yet supported
     - Not yet supported
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported


*Optional* :ref:`types-enum` *One of* map, viewport. *Defaults to* map. *Requires* fill-extrusion-translate.



Controls the translation reference point.


map
    The fill extrusion is translated relative to the map.

viewport
    The fill extrusion is translated relative to the viewport.

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.27.0
     - Not yet supported
     - Not yet supported
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported


fill-extrusion-pattern
""""""""""""""""""""""

*Optional* :ref:`types-string`.



Name of image in sprite to use for drawing images on extruded fills. For
seamless patterns, image width and height must be a factor of two (2, 4,
8, ..., 512).

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.27.0
     - Not yet supported
     - Not yet supported
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported

fill-extrusion-height
"""""""""""""""""""""

*Optional* :ref:`types-number` *Units in* meters. *Defaults to* 0.


The height with which to extrude this layer.


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.27.0
     - Not yet supported
     - Not yet supported
   * - data-driven styling
     - >= 0.27.0
     - Not yet supported
     - Not yet supported


fill-extrusion-base
"""""""""""""""""""

*Optional* :ref:`types-number` *Units in* meters. *Defaults to* 0. *Requires* fill-extrusion-height.



The height with which to extrude the base of this layer. Must be less
than or equal to ``fill-extrusion-height``.

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.27.0
     - Not yet supported
     - Not yet supported
   * - data-driven styling
     - >= 0.27.0
     - Not yet supported
     - Not yet supported


Types
-----

A Mapbox style contains values of various types, most commonly as values
for the style properties of a layer.

.. _types-color:

Color
~~~~~

Colors are written as JSON strings in a variety of permitted formats:
HTML-style hex values, rgb, rgba, hsl, and hsla. Predefined HTML colors
names, like ``yellow`` and ``blue``, are also permitted.

::

    {
      "line-color": "#ff0",
      "line-color": "#ffff00",
      "line-color": "rgb(255, 255, 0)",
      "line-color": "rgba(255, 255, 0, 1)",
      "line-color": "hsl(100, 50%, 50%)",
      "line-color": "hsla(100, 50%, 50%, 1)",
      "line-color": "yellow"
    }

Especially of note is the support for hsl, which can be `easier to
reason about than rgb() <http://mothereffinghsl.com/>`__.

.. _types-enum:

Enum
~~~~

One of a fixed list of string values. Use quotes around values.

::

    {
      "text-transform": "uppercase"
    }

.. _types-string:

String
~~~~~~

A string is basically just text. In Mapbox styles, you're going to put
it in quotes. Strings can be anything, though pay attention to the case
of ``text-field`` - it actually will refer to features, which you refer
to by putting them in curly braces, as seen in the example below.

::

    {
      "text-field": "{MY_FIELD}"
    }


.. _types-boolean:

Boolean
~~~~~~~

Boolean means yes or no, so it accepts the values ``true`` or ``false``.

::

    {
      "fill-enabled": true
    }

.. _types-number:

Number
~~~~~~~

A number value, often an integer or floating point (decimal number).
Written without quotes.

::

    {
      "text-size": 24
    }

.. _types-array:

Array
~~~~~~

Arrays are comma-separated lists of one or more numbers in a specific
order. For example, they're used in line dash arrays, in which the
numbers specify intervals of line, break, and line again.

::

    {
      "line-dasharray": [2, 4]
    }

.. _expressions:

Expressions
-----------

The value for any layout property, paint property, or filter may be specified as an expression. An expression defines a formula for computing the value of the property using the operators described below. The set of expression operators provided by Mapbox GL includes:

- Mathematical operators for performing arithmetic and other operations on numeric values
- Logical operators for manipulating boolean values and making conditional decisions
- String operators for manipulating strings
- Data operators, providing access to the properties of source features
- Camera operators, providing access to the parameters defining the current map view

Expressions are represented as JSON arrays. The first element of an expression array is a string naming the expression operator, e.g. ``"*"`` or ``"case"``. Subsequent elements (if any) are the arguments to the expression. Each argument is either a literal value (a string, number, boolean, or null), or another expression array.

    [expression_name, argument_0, argument_1, ...]

**Data expressions**

A *data expression* is any expression that access feature data -- that is, any expression that uses one of the data operators: ``get``, ``has``, ``id``, ``geometry-type``, ``properties``, or ``feature-state``. Data expressions allow a feature's properties or state to determine its appearance. They can be used to differentiate features within the same layer and to create data visualizations.

::

    {
        "circle-color": [
            "rgb",
            // red is higher when feature.properties.temperature is higher
            ["get", "temperature"],
            // green is always zero
            0,
            // blue is higher when feature.properties.temperature is lower
            ["-", 100, ["get", "temperature"]]
        ]
    }

This example uses the ``get`` operator to obtain the temperature value of each feature. That value is used to compute arguments to the ``rgb`` operator, defining a color in terms of its red, green, and blue components.

Data expressions are allowed as the value of the ``filter`` property, and as values for most paint and layout properties. However, some paint and layout properties do not yet support data expressions. The level of support is indicated by the "data-driven styling" row of the "SDK Support" table for each property. Data expressions with the ``feature-state`` operator are allowed only on paint properties.

**Camera expressions**

A *camera expression* is any expression that uses the ``zoom operator``. Such expressions allow the the appearance of a layer to change with the map's zoom level. Camera expressions can be used to create the appearance of depth and to control data density.

::

    {
        "circle-radius": [
            "interpolate", ["linear"], ["zoom"],
            // zoom is 5 (or less) -> circle radius will be 1px
            5, 1,
            // zoom is 10 (or greater) -> circle radius will be 5px
            10, 5
        ]
    }

This example uses the ``interpolate`` operator to define a linear relationship between zoom level and circle size using a set of input-output pairs. In this case, the expression indicates that the circle radius should be 1 pixel when the zoom level is 5 or below, and 5 pixels when the zoom is 10 or above. In between, the radius will be linearly interpolated between 1 and 5 pixels

Camera expressions are allowed anywhere an expression may be used. However, when a camera expression used as the value of a layout or paint property, it must be in one of the following forms::

    [ "interpolate", interpolation, ["zoom"], ... ]

Or::

    [ "step", ["zoom"], ... ]

Or::

    [
        "let",
        ... variable bindings...,
        [ "interpolate", interpolation, ["zoom"], ... ]
    ]

Or::

    [
        "let",
        ... variable bindings...,
        [ "step", ["zoom"], ... ]
    ]

That is, in layout or paint properties, ``["zoom"]`` may appear only as the input to an outer ``interpolate`` or ``step`` expression, or such an expression within a ``let`` expression.

There is an important difference between layout and paint properties in the timing of camera expression evaluation. Paint property camera expressions are re-evaluated whenever the zoom level changes, even fractionally. For example, a paint property camera expression will be re-evaluated continuously as the map moves between zoom levels 4.1 and 4.6. On the other hand, a layout property camera expression is evaluated only at integer zoom levels. It will not be re-evaluated as the zoom changes from 4.1 to 4.6 -- only if it goes above 5 or below 4.

**Composition**

A single expression may use a mix of data operators, camera operators, and other operators. Such composite expressions allows a layer's appearance to be determined by a combination of the zoom level and individual feature properties.

::

    {
        "circle-radius": [
            "interpolate", ["linear"], ["zoom"],
            // when zoom is 0, set each feature's circle radius to the value of its "rating" property
            0, ["get", "rating"],
            // when zoom is 10, set each feature's circle radius to four times the value of its "rating" property
            10, ["*", 4, ["get", "rating"]]
        ]
    }

An expression that uses both data and camera operators is considered both a data expression and a camera expression, and must adhere to the restrictions described above for both.

**Type system**

The input arguments to expressions, and their result values, use the same set of types as the rest of the style specification: boolean, string, number, color, and arrays of these types. Furthermore, expressions are type safe: each use of an expression has a known result type and required argument types, and the SDKs verify that the result type of an expression is appropriate for the context in which it is used. For example, the result type of an expression in the ``filter`` property must be boolean, and the arguments to the ``+`` operator must be numbers.

When working with feature data, the type of a feature property value is typically not known ahead of time by the SDK. In order to preserve type safety, when evaluating a data expression, the SDK will check that the property value is appropriate for the context. For example, if you use the expression ``["get", "feature-color"]`` for the ``circle-color`` property, the SDK will verify that the ``feature-color`` value of each feature is a string identifying a valid color. If this check fails, an error will be indicated in an SDK-specific way (typically a log message), and the default value for the property will be used instead.

In most cases, this verification will occur automatically wherever it is needed. However, in certain situations, the SDK may be unable to automatically determine the expected result type of a data expression from surrounding context. For example, it is not clear whether the expression ``["<", ["get", "a"], ["get", "b"]]`` is attempting to compare strings or numbers. In situations like this, you can use one of the type assertion expression operators to indicate the expected type of a data expression: ``["<", ["number", ["get", "a"]], ["number", ["get", "b"]]]``. A type assertion checks that the feature data actually matches the expected type of the data expression. If this check fails, it produces an error and causes the whole expression to fall back to the default value for the property being defined. The assertion operators are ``array``, ``boolean``, ``number``, and ``string``.

Expressions perform only one kind of implicit type conversion: a data expression used in a context where a color is expected will convert a string representation of a color to a color value. In all other cases, if you want to convert between types, you must use one of the type conversion expression operators: ``to-boolean``, ``to-number``, ``to-string``, or ``to-color``. For example, if you have a feature property that stores numeric values in string format, and you want to use those values as numbers rather than strings, you can use an expression such as ``["to-number", ["get", "property-name"]]``.

**Expression reference**

.. _expressions.types:

Types
~~~~~

The expressions in this section are provided for the purpose of testing for and converting between different data types like strings, numbers, and boolean values.

Often, such tests and conversions are unnecessary, but they may be necessary in some expressions where the type of a certain sub-expression is ambiguous. They can also be useful in cases where your feature data has inconsistent types; for example, you could use to-number to make sure that values like ``"1.5"`` (instead of ``1.5``) are treated as numeric values.

.. _expressions-array:

array
^^^^^

Asserts that the input is an array (optionally with a specific item type and length). If, when the input expression is evaluated, it is not of the asserted type, then this assertion will cause the whole expression to be aborted.

::

    ["array", value]: array

::

    ["array", type: "string" | "number" | "boolean", value]: array<type>

::

    ["array",
        type: "string" | "number" | "boolean",
        N: number (literal),
        value
    ]: array<type, N>


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.41.0
     - >= 20.0
     - >= 3.0.0

.. _expressions-boolean:

boolean
^^^^^^^

Asserts that the input value is a boolean. If multiple values are provided, each one is evaluated in order until a boolean is obtained. If none of the inputs are booleans, the expression is an error.

::

    ["boolean", value]: boolean

::

    ["boolean", value, fallback: value, fallback: value, ...]: boolean

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.41.0
     - >= 20.0
     - >= 3.0.0

.. _expressions-collator:

collator
^^^^^^^^

Returns a ``collator`` for use in locale-dependent comparison operations. The ``case-sensitive`` and ``diacritic-sensitive`` options default to ``false``. The locale argument specifies the IETF language tag of the locale to use. If none is provided, the default locale is used. If the requested locale is not available, the ``collator`` will use a system-defined fallback locale. Use ``resolved-locale`` to test the results of locale fallback behavior.

::

    ["collator",
        { "case-sensitive": boolean, "diacritic-sensitive": boolean, "locale": string }
    ]: collator

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.45.0
     - >= Not yet supported
     - >= Not yet supported

.. _expressions-format:

format
^^^^^^

Returns ``formatted`` text containing annotations for use in mixed-format ``text-field`` entries. If set, the ``text-font`` argument overrides the font specified by the root layout properties. If set, the ``font-scale`` argument specifies a scaling factor relative to the ``text-size`` specified in the root layout properties.

::

    ["format",
        input_1: string, options_1: { "font-scale": number, "text-font": array<string> },
        ...,
        input_n: string, options_n: { "font-scale": number, "text-font": array<string> }
    ]: formatted

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.48.0
     - >= Not yet supported
     - >= Not yet supported

.. _expressions-literal:

literal
^^^^^^^

Provides a literal array or object value.

::

    ["literal", [...] (JSON array literal)]: array<T, N>

::

    ["literal", {...} (JSON object literal)]: Object

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.41.0
     - >= 20.0
     - >= 3.0.0

.. _expressions-number:

number
^^^^^^

Asserts that the input value is a number. If multiple values are provided, each one is evaluated in order until a number is obtained. If none of the inputs are numbers, the expression is an error.

::

    ["number", value]: number


::

    ["number", value, fallback: value, fallback: value, ...]: number

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.41.0
     - >= 20.0
     - >= 3.0.0

.. _expressions-object:

object
^^^^^^

Asserts that the input value is an object. If multiple values are provided, each one is evaluated in order until an object is obtained. If none of the inputs are objects, the expression is an error.

::

    ["object", value]: object

::

    ["object", value, fallback: value, fallback: value, ...]: object

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.41.0
     - >= 20.0
     - >= 3.0.0

.. _expressions-string:

string
^^^^^^

Asserts that the input value is a string. If multiple values are provided, each one is evaluated in order until a string is obtained. If none of the inputs are strings, the expression is an error.

::

    ["string", value]: string

::

    ["string", value, fallback: value, fallback: value, ...]: string

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.41.0
     - >= 20.0
     - >= 3.0.0

.. _expressions-to-boolean:

to-boolean
^^^^^^^^^^

Converts the input value to a boolean. The result is false when then input is an empty string, 0, ``false``, ``null``, or ``NaN``; otherwise it is ``true``.

::

    ["to-boolean", value]: boolean

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.41.0
     - >= 20.0
     - >= 3.0.0

.. _expressions-to-color:

to-color
^^^^^^^^

Converts the input value to a color. If multiple values are provided, each one is evaluated in order until the first successful conversion is obtained. If none of the inputs can be converted, the expression is an error.

::

    ["to-color", value, fallback: value, fallback: value, ...]: color

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.41.0
     - >= 20.0
     - >= 3.0.0

.. _expressions-to-number:

to-number
^^^^^^^^^

Converts the input value to a number, if possible. If the input is ``null`` or ``false``, the result is 0. If the input is ``true``, the result is 1. If the input is a string, it is converted to a number as specified by the "ToNumber Applied to the String Type" algorithm of the ECMAScript Language Specification. If multiple values are provided, each one is evaluated in order until the first successful conversion is obtained. If none of the inputs can be converted, the expression is an error.

::

    ["to-number", value, fallback: value, fallback: value, ...]: number

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.41.0
     - >= 20.0
     - >= 3.0.0

.. _expressions-to-string:

to-string
^^^^^^^^^

Converts the input value to a string. If the input is ``null``, the result is ``""``. If the input is a boolean, the result is ``"true"`` or ``"false"``. If the input is a number, it is converted to a string as specified by the "NumberToString" algorithm of the ECMAScript Language Specification. If the input is a color, it is converted to a string of the form ``"rgba(r,g,b,a)"``, where ``r``, ``g``, and ``b`` are numerals ranging from 0 to 255, and ``a`` ranges from 0 to 1. Otherwise, the input is converted to a string in the format specified by the ``JSON.stringify`` function of the ECMAScript Language Specification.

::

    ["to-string", value]: string

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.41.0
     - >= 20.0
     - >= 3.0.0

.. _expressions-typeof:

typeof
^^^^^^

Returns a string describing the type of the given value.

::

    ["typeof", value]: string

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.41.0
     - >= 20.0
     - >= 3.0.0

.. _expressions.feature_data:

Feature data
~~~~~~~~~~~~

.. _expressions-feature-state:

feature-state
^^^^^^^^^^^^^

Retrieves a property value from the current feature's state. Returns null if the requested property is not present on the feature's state. A feature's state is not part of the GeoJSON or vector tile data, and must be set programmatically on each feature. Note that ``["feature-state"]`` can only be used with paint properties that support data-driven styling.

::

    ["feature-state", string]: value

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.46.0
     - >= Not yet supported
     - >= Not yet supported

.. _expressions-geometry-type:

geometry-type
^^^^^^^^^^^^^

Gets the feature's geometry type: Point, MultiPoint, LineString, MultiLineString, Polygon, MultiPolygon.

::

    ["geometry-type"]: string


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.41.0
     - >= 20.0
     - >= 3.0.0

.. _expressions-id:

id
^^

Gets the feature's id, if it has one.

::

    ["id"]: value

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.41.0
     - >= 20.0
     - >= 3.0.0

.. _expressions-line-progress:

line-progress
^^^^^^^^^^^^^

Gets the progress along a gradient line. Can only be used in the line-gradient property.

::

    ["line-progress"]: number

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.45.0
     - >= Not yet supported
     - >= Not yet supported

.. _expressions-properties:

properties
^^^^^^^^^^

Gets the feature properties object. Note that in some cases, it may be more efficient to use ["get", "property_name"] directly.

::

    ["properties"]: object

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.41.0
     - >= Not yet supported
     - >= 3.0.0

.. _expressions.lookup:

Lookup
~~~~~~

.. _expressions-at:

at
^^

Retrieves an item from an array.

::

    ["at", number, array]: ItemType

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.41.0
     - >= 20.0
     - >= 3.0.0

.. _expressions-get:

get
^^^

Retrieves a property value from the current feature's properties, or from another object if a second argument is provided. Returns null if the requested property is missing.

::

    ["get", string]: value

::

    ["get", string, object]: value

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.41.0
     - >= 20.0
     - >= 3.0.0

.. _expressions-has:

has
^^^

Tests for the presence of an property value in the current feature's properties, or from another object if a second argument is provided.

::

    ["has", string]: boolean

::

    ["has", string, object]: boolean

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.41.0
     - >= 20.0
     - >= 3.0.0

.. _expressions-length:

length
^^^^^^

Gets the length of an array or string.

::

    ["length", string | array | value]: number

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.41.0
     - >= 20.0
     - >= 3.0.0

.. _expressions.decision:

Decision
~~~~~~~~

The expressions in this section can be used to add conditional logic to your styles. For example, the ``'case'`` expression provides basic "if/then/else" logic, and ``'match'`` allows you to map specific values of an input expression to different output expressions.

.. _expressions-!:

!
^

Logical negation. Returns true if the input is false, and false if the input is true.

::

    ["!", boolean]: boolean

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.41.0
     - >= 20.0
     - >= 3.0.0

.. _expressions-!=:

!=
^^

Returns ``true`` if the input values are not equal, ``false`` otherwise. The comparison is strictly typed: values of different runtime types are always considered unequal. Cases where the types are known to be different at parse time are considered invalid and will produce a parse error. Accepts an optional ``collator`` argument to control locale-dependent string comparisons.

::

    ["!=", value, value]: boolean

::

    ["!=", value, value, collator]: boolean

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.41.0
     - >= 20.0
     - >= 3.0.0
   * - collator
     - >= 0.45.0
     - >= Not yet supported
     - >= Not yet supported

.. _expressions-<:

<
^

Returns ``true`` if the first input is strictly less than the second, ``false`` otherwise. The arguments are required to be either both strings or both numbers; if during evaluation they are not, expression evaluation produces an error. Cases where this constraint is known not to hold at parse time are considered in valid and will produce a parse error. Accepts an optional ``collator`` argument to control locale-dependent string comparisons.

::

    ["<", value, value]: boolean

::

    ["<", value, value, collator]: boolean

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.41.0
     - >= 20.0
     - >= 3.0.0
   * - collator
     - >= 0.45.0
     - >= Not yet supported
     - >= Not yet supported

.. _expressions-<=:

<=
^^

Returns ``true`` if the first input is less than or equal to the second, ``false`` otherwise. The arguments are required to be either both strings or both numbers; if during evaluation they are not, expression evaluation produces an error. Cases where this constraint is known not to hold at parse time are considered in valid and will produce a parse error. Accepts an optional ``collator`` argument to control locale-dependent string comparisons.

::

    ["<=", value, value]: boolean

::

    ["<=", value, value, collator]: boolean

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.41.0
     - >= 20.0
     - >= 3.0.0
   * - collator
     - >= 0.45.0
     - >= Not yet supported
     - >= Not yet supported

.. _expressions-==:

==
^^

Returns ``true`` if the input values are equal, ``false`` otherwise. The comparison is strictly typed: values of different runtime types are always considered unequal. Cases where the types are known to be different at parse time are considered invalid and will produce a parse error. Accepts an optional ``collator`` argument to control locale-dependent string comparisons.

::

    ["==", value, value]: boolean

::

    ["==", value, value, collator]: boolean

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.41.0
     - >= 20.0
     - >= 3.0.0
   * - collator
     - >= 0.45.0
     - >= Not yet supported
     - >= Not yet supported

.. _expressions->:

>
^

Returns ``true`` if the first input is strictly greater than the second, ``false`` otherwise. The arguments are required to be either both strings or both numbers; if during evaluation they are not, expression evaluation produces an error. Cases where this constraint is known not to hold at parse time are considered in valid and will produce a parse error. Accepts an optional ``collator`` argument to control locale-dependent string comparisons.

::

    [">", value, value]: boolean

::

    [">", value, value, collator]: boolean

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.41.0
     - >= 20.0
     - >= 3.0.0
   * - collator
     - >= 0.45.0
     - >= Not yet supported
     - >= Not yet supported

.. _expressions->=:

>=
^^

Returns ``true`` if the first input is greater than or equal to the second, ``false`` otherwise. The arguments are required to be either both strings or both numbers; if during evaluation they are not, expression evaluation produces an error. Cases where this constraint is known not to hold at parse time are considered in valid and will produce a parse error. Accepts an optional ``collator`` argument to control locale-dependent string comparisons.

::

    [">=", value, value]: boolean

::

    [">=", value, value, collator]: boolean

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.41.0
     - >= 20.0
     - >= 3.0.0
   * - collator
     - >= 0.45.0
     - >= Not yet supported
     - >= Not yet supported

.. _expressions-all:

all
^^^

Returns ``true`` if all the inputs are true, ``false`` otherwise. The inputs are evaluated in order, and evaluation is short-circuiting: once an input expression evaluates to ``false``, the result is false and no further input expressions are evaluated.

::

    ["all", boolean, boolean]: boolean

::

    ["all", boolean, boolean, ...]: boolean

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.41.0
     - >= 20.0
     - >= 3.0.0

.. _expressions-any:

any
^^^

Returns ``true`` if any of the inputs are ``true``, ``false`` otherwise. The inputs are evaluated in order, and evaluation is short-circuiting: once an input expression evaluates to ``true``, the result is true and no further input expressions are evaluated.

::

    ["any", boolean, boolean]: boolean

::

    ["any", boolean, boolean, ...]: boolean

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.41.0
     - >= 20.0
     - >= 3.0.0

.. _expressions-case:

case
^^^^

Selects the first output whose corresponding test condition evaluates to true.

::

    ["case",
        condition: boolean, output: OutputType, condition: boolean, output: OutputType, ...,
        default: OutputType
    ]: OutputType

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.41.0
     - >= 20.0
     - >= 3.0.0

.. _expressions-coalesce:

coalesce
^^^^^^^^

Evaluates each expression in turn until the first non-null value is obtained, and returns that value.

::

    ["coalesce", OutputType, OutputType, ...]: OutputType

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.41.0
     - >= 20.0
     - >= 3.0.0

.. _expressions-match:

match
^^^^^

Selects the output whose label value matches the input value, or the fallback value if no match is found. The input can be any expression (e.g. ``["get", "building_type"]``). Each label must either be a single literal value or an array of literal values (e.g. ``"a"`` or ``["c", "b"]``), and those values must be all strings or all numbers. (The values ``"1"`` and ``1`` cannot both be labels in the same match expression.) Each label must be unique. If the input type does not match the type of the labels, the result will be the fallback value.

::

    ["match",
        input: InputType (number or string),
        label_1: InputType | [InputType, InputType, ...], output_1: OutputType,
        label_n: InputType | [InputType, InputType, ...], output_n: OutputType, ...,
        default: OutputType
    ]: OutputType

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.41.0
     - >= 20.0
     - >= 3.0.0

.. _expressions.ramps_scales_curves:

Ramps, scales, curves
~~~~~~~~~~~~~~~~~~~~~

.. _expressions-interpolate:

interpolate
^^^^^^^^^^^

Produces continuous, smooth results by interpolating between pairs of input and output values ("stops"). The ``input`` may be any numeric expression (e.g., ``["get", "population"]``). Stop inputs must be numeric literals in strictly ascending order. The output type must be ``number``, ``array<number>``, or ``color``.

Interpolation types:

- ``["linear"]``: interpolates linearly between the pair of stops just less than and just greater than the input.
- ``["exponential", base]``: interpolates exponentially between the stops just less than and just greater than the input. base controls the rate at which the output increases: higher values make the output increase more towards the high end of the range. With values close to 1 the output increases linearly.
- ``["cubic-bezier", x1, y1, x2, y2]``: interpolates using the cubic bezier curve defined by the given control points.

::

    ["interpolate",
        interpolation: ["linear"] | ["exponential", base] | ["cubic-bezier", x1, y1, x2, y2 ],
        input: number,
        stop_input_1: number, stop_output_1: OutputType,
        stop_input_n: number, stop_output_n: OutputType, ...
    ]: OutputType (number, array<number>, or Color)

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.42.0
     - >= Not yet supported
     - >= 3.0.0

.. _expressions-interpolate-hcl:

interpolate-hcl
^^^^^^^^^^^^^^^

Produces continuous, smooth results by interpolating between pairs of input and output values ("stops"). Works like ``interpolate``, but the output type must be ``color``, and the interpolation is performed in the Hue-Chroma-Luminance color space.

::

    ["interpolate-hcl",
        interpolation: ["linear"] | ["exponential", base] | ["cubic-bezier", x1, y1, x2, y2 ],
        input: number,
        stop_input_1: number, stop_output_1: Color,
        stop_input_n: number, stop_output_n: Color, ...
    ]: Color

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.49.0
     - >= Not yet supported
     - >= 3.0.0

.. _expressions-interpolate-lab:

interpolate-lab
^^^^^^^^^^^^^^^

Produces continuous, smooth results by interpolating between pairs of input and output values ("stops"). Works like ``interpolate``, but the output type must be ``color``, and the interpolation is performed in the CIELAB color space.

::

    ["interpolate-lab",
        interpolation: ["linear"] | ["exponential", base] | ["cubic-bezier", x1, y1, x2, y2 ],
        input: number,
        stop_input_1: number, stop_output_1: Color,
        stop_input_n: number, stop_output_n: Color, ...
    ]: Color

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.49.0
     - >= Not yet supported
     - >= 3.0.0

.. _expressions-step:

step
^^^^

Produces discrete, stepped results by evaluating a piecewise-constant function defined by pairs of input and output values ("stops"). The ``input`` may be any numeric expression (e.g.,  ``["get", "population"]``). Stop inputs must be numeric literals in strictly ascending order. Returns the output value of the stop just less than the input, or the first input if the input is less than the first stop.

::

    ["step",
        input: number,
        stop_output_0: OutputType,
        stop_input_1: number, stop_output_1: OutputType,
        stop_input_n: number, stop_output_n: OutputType, ...
    ]: OutputType

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.42.0
     - >= Not yet supported
     - >= 3.0.0

.. _expressions.variable_binding:

Variable binding
~~~~~~~~~~~~~~~~

.. _expressions-let:

let
^^^

Binds expressions to named variables, which can then be referenced in the result expression using ["var", "variable_name"].

::

    ["let",
        string (alphanumeric literal), any, string (alphanumeric literal), any, ...,
        OutputType
    ]: OutputType

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.41.0
     - >= Not yet supported
     - >= 3.0.0

.. _expressions-var:

var
^^^

References variable bound using "let".

::

    ["var", previously bound variable name]: the type of the bound expression

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.41.0
     - >= Not yet supported
     - >= 3.0.0

.. _expressions.string:

String
~~~~~~

.. _expressions-concat:

concat
^^^^^^

Returns a string consisting of the concatenation of the inputs. Each input is converted to a string as if by ``to-string``.

::

    ["concat", value, value, ...]: string

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.41.0
     - >= 20.0
     - >= 3.0.0

.. _expressions-downcase:

downcase
^^^^^^^^

Returns the input string converted to lowercase. Follows the Unicode Default Case Conversion algorithm and the locale-insensitive case mappings in the Unicode Character Database.

::

    ["downcase", string]: string

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.41.0
     - >= 20.0
     - >= Not yet supported

.. _expressions-is-supported-script:

is-supported-script
^^^^^^^^^^^^^^^^^^^

Returns ``true`` if the input string is expected to render legibly. Returns ``false`` if the input string contains sections that cannot be rendered without potential loss of meaning (e.g. Indic scripts that require complex text shaping, or right-to-left scripts if the the ``mapbox-gl-rtl-text`` plugin is not in use in Mapbox GL JS).

::

    ["is-supported-script", string]: boolean

.. _expressions-resolved-locale:

resolved-locale
^^^^^^^^^^^^^^^

Returns the IETF language tag of the locale being used by the provided ``collator``. This can be used to determine the default system locale, or to determine if a requested locale was successfully loaded.

::

    ["resolved-locale", collator]: string

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.45.0
     - >= Not yet supported
     - >= Not yet supported

.. _expressions-upcase:

upcase
^^^^^^

Returns the input string converted to uppercase. Follows the Unicode Default Case Conversion algorithm and the locale-insensitive case mappings in the Unicode Character Database.

::

    ["upcase", string]: string

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.41.0
     - >= 20.0
     - >= Not yet supported

.. _expressions.color:

Color
~~~~~

.. _expressions-rgb:

rgb
^^^

Creates a color value from red, green, and blue components, which must range between 0 and 255, and an alpha component of 1. If any component is out of range, the expression is an error.

::

    ["rgb", number, number, number]: color

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.41.0
     - >= 20.0
     - >= 3.0.0

.. _expressions-rgba:

rgba
^^^^

Creates a color value from red, green, blue components, which must range between 0 and 255, and an alpha component which must range between 0 and 1. If any component is out of range, the expression is an error.

::

    ["rgba", number, number, number, number]: color

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.41.0
     - >= Not yet supported
     - >= 3.0.0

.. _expressions-to-rgba:

to-rgba
^^^^^^^

Returns a four-element array containing the input color's red, green, blue, and alpha components, in that order.

::

    ["to-rgba", color]: array<number, 4>

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.41.0
     - >= Not yet supported
     - >= 3.0.0

.. _expressions.math:

Math
~~~~

.. _expressions--:

\-
^^

For two inputs, returns the result of subtracting the second input from the first. For a single input, returns the result of subtracting it from 0.

::

    ["-", number, number]: number

::

    ["-", number]: number

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.41.0
     - >= 20.0
     - >= 3.0.0

.. _expressions-*:

\*
^^

Returns the product of the inputs.

::

    ["*", number, number, ...]: number

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.41.0
     - >= 20.0
     - >= 3.0.0

.. _expressions-/:

/
^

Returns the result of floating point division of the first input by the second.

::

    ["/", number, number]: number

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.41.0
     - >= 20.0
     - >= 3.0.0

.. _expressions-%:

%
^

Returns the remainder after integer division of the first input by the second.

::

    ["%", number, number]: number

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.41.0
     - >= 20.0
     - >= 3.0.0

.. _expressions-^:

^
^

Returns the result of raising the first input to the power specified by the second.

::

    ["^", number, number]: number

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.41.0
     - >= 20.0
     - >= 3.0.0

.. _expressions-+:

\+
^^

Returns the sum of the inputs.

::

    ["+", number, number, ...]: number

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.41.0
     - >= 20.0
     - >= 3.0.0

.. _expressions-abs:

abs
^^^

Returns the absolute value of the input.

::

    ["abs", number]: number

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.45.0
     - >= Not yet supported
     - >= 3.0.0

.. _expressions-acos:

acos
^^^^

Returns the arccosine of the input.

::

    ["acos", number]: number

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.41.0
     - >= 20.0
     - >= 3.0.0

.. _expressions-asin:

asin
^^^^

Returns the arcsine of the input.

::

    ["asin", number]: number

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.41.0
     - >= 20.0
     - >= 3.0.0

.. _expressions-atan:

atan
^^^^

Returns the arctangent of the input.

::

    ["atan", number]: number

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.41.0
     - >= 20.0
     - >= 3.0.0

.. _expressions-ceil:

ceil
^^^^

Returns the smallest integer that is greater than or equal to the input.

::

    ["ceil", number]: number

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.45.0
     - >= Not yet supported
     - >= 3.0.0

.. _expressions-cos:

cos
^^^

Returns the cosine of the input.

::

    ["cos", number]: number

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.41.0
     - >= 20.0
     - >= 3.0.0

.. _expressions-e:

e
^

Returns the mathematical constant e.

::

    ["e"]: number

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.41.0
     - >= 20.0
     - >= 3.0.0

.. _expressions-floor:

floor
^^^^^

Returns the largest integer that is less than or equal to the input.

::

    ["floor", number]: number

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.45.0
     - >= Not yet supported
     - >= 3.0.0

.. _expressions-ln:

ln
^^

Returns the natural logarithm of the input.

::

    ["ln", number]: number

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.41.0
     - >= 20.0
     - >= 3.0.0

.. _expressions-ln2:

ln2
^^^

Returns mathematical constant ln(2).

::

    ["ln2"]: number

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.41.0
     - >= 20.0
     - >= 3.0.0

.. _expressions-log10:

log10
^^^^^

Returns the base-ten logarithm of the input.

::

    ["log10", number]: number

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.41.0
     - >= 20.0
     - >= 3.0.0

.. _expressions-log2:

log2
^^^^

Returns the base-two logarithm of the input.

::

    ["log2", number]: number

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.41.0
     - >= 20.0
     - >= 3.0.0

.. _expressions-max:

max
^^^

Returns the maximum value of the inputs.


::

    ["max", number, number, ...]: number

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.41.0
     - >= 20.0
     - >= 3.0.0

.. _expressions-min:

min
^^^

Returns the minimum value of the inputs.

::

    ["min", number, number, ...]: number

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.41.0
     - >= 20.0
     - >= 3.0.0

.. _expressions-pi:

pi
^^

Returns the mathematical constant pi.

::

    ["pi"]: number

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.41.0
     - >= 20.0
     - >= 3.0.0

.. _expressions-:

round
^^^^^

Rounds the input to the nearest integer. Halfway values are rounded away from zero. For example, ``["round", -1.5]`` evaluates to -2.

::

    ["round", number]: number

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.45.0
     - >= Not yet supported
     - >= 3.0.0

.. _expressions-sin:

sin
^^^

Returns the sine of the input.

::

    ["sin", number]: number

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.41.0
     - >= 20.0
     - >= 3.0.0

.. _expressions-sqrt:

sqrt
^^^^

Returns the square root of the input.

::

    ["sqrt", number]: number

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.42.0
     - >= 20.0
     - >= 3.0.0

.. _expressions-tan:

tan
^^^

Returns the tangent of the input.

::

    ["tan", number]: number

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.41.0
     - >= 20.0
     - >= 3.0.0

.. _expressions.zoom:

Zoom
~~~~

.. _expressions-zoom:

zoom
^^^^

Gets the current zoom level. Note that in style layout and paint properties, ["zoom"] may only appear as the input to a top-level "step" or "interpolate" expression.

::

    ["zoom"]: number

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.41.0
     - >= 20.0
     - >= 3.0.0

.. _expressions.heatmap:

Heatmap
~~~~~~~

.. _expressions-heatmap-density:

heatmap-density
^^^^^^^^^^^^^^^

Gets the kernel density estimation of a pixel in a heatmap layer, which is a relative measure of how many data points are crowded around a particular pixel. Can only be used in the ``heatmap-color`` property.

::

    ["heatmap-density"]: number

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.41.0
     - >= Not yet supported
     - >= Not yet supported


Other
-----

.. _other-function:

Function
~~~~~~~~

The value for any layout or paint property may be specified as a
*function*. Functions allow you to make the appearance of a map feature
change with the current zoom level and/or the feature's properties.


.. _stops:

stops
^^^^^

*Required (except for identity functions) :ref:`types-array`.*




Functions are defined in terms of input and output values. A set of one
input value and one output value is known as a "stop."

.. _property:

property
^^^^^^^^

*Optional* :ref:`types-string`.





If specified, the function will take the specified feature property as
an input. See `Zoom Functions and Property
Functions <#types-function-zoom-property>`__ for more information.

.. _base:

base
^^^^

*Optional* :ref:`types-number`. *Default is* 1.


The exponential base of the interpolation curve. It controls the rate at
which the function output increases. Higher values make the output
increase more towards the high end of the range. With values close to 1
the output increases linearly.

.. _type:

type
^^^^

*Optional* :ref:`types-enum`. *One of* identity, exponential, interval, categorical.

identity
    functions return their input as their output.
exponential
    functions generate an output by interpolating between stops just
    less than and just greater than the function input. The domain must
    be numeric. This is the default for properties marked with , the
    "exponential" symbol.
interval
    functions return the output value of the stop just less than the
    function input. The domain must be numeric. This is the default for
    properties marked with , the "interval" symbol.
categorical
    functions return the output value of the stop equal to the function
    input.

.. _function-default:

default
^^^^^^^

A value to serve as a fallback function result when a value isn't
otherwise available. It is used in the following circumstances:


-  In categorical functions, when the feature value does not match any
   of the stop domain values.
-  In property and zoom-and-property functions, when a feature does not
   contain a value for the specified property.
-  In identity functions, when the feature value is not valid for the
   style property (for example, if the function is being used for a
   circle-color property but the feature property value is not a string
   or not a valid color).
-  In interval or exponential property and zoom-and-property functions,
   when the feature value is not numeric.


If no default is provided, the style property's default is used in these
circumstances.

.. _function-colorspace:

colorSpace
^^^^^^^^^^

*Optional* :ref:`types-enum`. *One of* rgb, lab, hcl.

The color space in which colors interpolated. Interpolating colors in
perceptual color spaces like LAB and HCL tend to produce color ramps
that look more consistent and produce colors that can be differentiated
more easily than those interpolated in RGB space.


rgb
    Use the RGB color space to interpolate color values
lab
    Use the LAB color space to interpolate color values.
hcl
    Use the HCL color space to interpolate color values, interpolating
    the Hue, Chroma, and Luminance channels individually.


.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - >= 17.1
     - >= 2.4.0
   * - ``property``
     - >= 0.18.0
     - >= 17.1
     - >= 2.4.0
   * - ``type``
     - >= 0.18.0
     - >= 17.1
     - >= 2.4.0
   * - ``exponential`` type
     - >= 0.18.0
     - >= 17.1
     - >= 2.4.0
   * - ``interval`` type
     - >= 0.18.0
     - >= 17.1
     - >= 2.4.0
   * - ``categorical`` type
     - >= 0.18.0
     - >= 17.1
     - >= 2.4.0
   * - ``identity`` type
     - >= 0.18.0
     - >= 17.1
     - >= 2.4.0
   * - ``default`` type
     - >= 0.18.0
     - >= 17.1
     - >= 2.4.0
   * - ``colorSpace`` type
     - >= 0.26.0
     - Not yet supported
     - >= 2.4.0


**Zoom functions** allow the appearance of a map feature to change with
map’s zoom level. Zoom functions can be used to create the illusion of
depth and control data density. Each stop is an array with two elements:
the first is a zoom level and the second is a function output value.

::

    {
      "circle-radius": {
        "stops": [

          // zoom is 5 -> circle radius will be 1px
          [5, 1],

          // zoom is 10 -> circle radius will be 2px
          [10, 2]

        ]
      }
    }

The rendered values of :ref:`types-color`,
:ref:`types-number`, and :ref:`types-array` properties are
intepolated between stops. :ref:`types-enum`,
:ref:`types-boolean`, and :ref:`types-string` property
values cannot be intepolated, so their rendered values only change at
the specified stops.

There is an important difference between the way that zoom functions
render for *layout* and *paint* properties. Paint properties are
continuously re-evaluated whenever the zoom level changes, even
fractionally. The rendered value of a paint property will change, for
example, as the map moves between zoom levels ``4.1`` and ``4.6``.
Layout properties, on the other hand, are evaluated only once for each
integer zoom level. To continue the prior example: the rendering of a
layout property will *not* change between zoom levels ``4.1`` and
``4.6``, no matter what stops are specified; but at zoom level ``5``,
the function will be re-evaluated according to the function, and the
property's rendered value will change. (You can include fractional zoom
levels in a layout property zoom function, and it will affect the
generated values; but, still, the rendering will only change at integer
zoom levels.)

**Property functions** allow the appearance of a map feature to change
with its properties. Property functions can be used to visually
differentate types of features within the same layer or create data
visualizations. Each stop is an array with two elements, the first is a
property input value and the second is a function output value. Note
that support for property functions is not available across all
properties and platforms at this time.

::

    {
      "circle-color": {
        "property": "temperature",
        "stops": [

          // "temperature" is 0   -> circle color will be blue
          [0, 'blue'],

          // "temperature" is 100 -> circle color will be red
          [100, 'red']

        ]
      }
    }



**Zoom-and-property functions** allow the appearance of a map feature
to change with both its properties *and* zoom. Each stop is an array
with two elements, the first is an object with a property input value
and a zoom, and the second is a function output value. Note that support
for property functions is not yet complete.

::

    {
      "circle-radius": {
        "property": "rating",
        "stops": [

          // zoom is 0 and "rating" is 0 -> circle radius will be 0px
          [{zoom: 0, value: 0}, 0],

          // zoom is 0 and "rating" is 5 -> circle radius will be 5px
          [{zoom: 0, value: 5}, 5],

          // zoom is 20 and "rating" is 0 -> circle radius will be 0px
          [{zoom: 20, value: 0}, 0],

          // zoom is 20 and "rating" is 5 -> circle radius will be 20px
          [{zoom: 20, value: 5}, 20]

        ]
      }
    }

.. _other-filter:

Filter
~~~~~~

A filter selects specific features from a layer. A filter is an array of
one of the following forms:

Existential Filters
^^^^^^^^^^^^^^^^^^^

``["has", key]`` feature[key] exists


``["!has", key]`` feature[key] does not exist


Comparison Filters
^^^^^^^^^^^^^^^^^^

``["==", key, value]`` equality: feature[key] = value


``["!=", key, value]`` inequality: feature[key] ≠ value


``[">", key, value]`` greater than: feature[key] > value


``[">=", key, value]`` greater than or equal: feature[key] ≥ value


``["<", key, value]`` less than: feature[key] < value


``["<=", key, value]`` less than or equal: feature[key] ≤ value


Set Membership Filters
^^^^^^^^^^^^^^^^^^^^^^

``["in", key, v0, ..., vn]`` set inclusion: feature[key] ∈ {v0, ..., vn}


``["!in", key, v0, ..., vn]`` set exclusion: feature[key] ∉ {v0, ...,
vn}


Combining Filters
^^^^^^^^^^^^^^^^^

``["all", f0, ..., fn]`` logical ``AND``: f0 ∧ ... ∧ fn


``["any", f0, ..., fn]`` logical ``OR``: f0 ∨ ... ∨ fn


``["none", f0, ..., fn]`` logical ``NOR``: ¬f0 ∧ ... ∧ ¬fn



A key must be a string that identifies a feature property, or one of the
following special keys:

-  ``"$type"``: the feature type. This key may be used with the
   ``"=="``, ``"!="``, ``"in"``, and ``"!in"`` operators. Possible
   values are ``"Point"``, ``"LineString"``, and ``"Polygon"``.
-  ``"$id"``: the feature identifier. This key may be used with the
   ``"=="``, ``"!="``, ``"has"``, ``"!has"``, ``"in"``, and ``"!in"``
   operators.

A value (and v0, ..., vn for set operators) must be a
:ref:`types-string`, :ref:`types-number`, or :ref:`types-boolean` to
compare the property value against.

Set membership filters are a compact and efficient way to test whether a
field matches any of multiple values.

The comparison and set membership filters implement strictly-typed
comparisons; for example, all of the following evaluate to false:
``0 < "1"``, ``2 == "2"``, ``"true" in [true, false]``.

The ``"all"``, ``"any"``, and ``"none"`` filter operators are used to
create compound filters. The values f0, ..., fn must be filter
expressions themselves.
::

    ["==", "$type", "LineString"]


This filter requires that the ``class`` property of each feature is
equal to either "street\_major", "street\_minor", or "street\_limited".
::

    ["in", "class", "street_major", "street_minor", "street_limited"]


The combining filter "all" takes the three other filters that follow it
and requires all of them to be true for a feature to be included: a
feature must have a ``class`` equal to "street\_limited", its
``admin_level`` must be greater than or equal to 3, and its type cannot
be Polygon. You could change the combining filter to "any" to allow
features matching any of those criteria to be included - features that
are Polygons, but have a different ``class`` value, and so on.
::

    [
      "all",
      ["==", "class", "street_limited"],
      [">=", "admin_level", 3],
      ["!in", "$type", "Polygon"]
    ]

.. list-table::
   :widths: 19, 27, 27, 27
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoTools
     - OpenLayers
   * - basic functionality
     - >= 0.10.0
     - >= 17.1
     - >= 2.4.0
   * - ``has``/``!has``
     - >= 0.19.0
     - >= 17.1
     - >= 2.4.0
