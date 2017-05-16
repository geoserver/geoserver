Mapbox Style Specification
==========================

A Mapbox style is a document that defines the visual appearance of a map: what data to draw, the order to draw it in, and how to style the data when drawing it. A style document is a `JSON <http://www.json.org/>`__ object with specific root level and nested properties. This specification defines and describes these properties.

The intended audience of this quick reference includes:

-  Advanced designers and cartographers who want to write styles by hand
-  GeoServer developers using the mbstyle module
-  Authors of software that generates or processes Mapbox styles.
- Feature support is provided for the `Mapbox GL JS <https://www.mapbox.com/mapbox-gl-js/api/>`__, `Open Layers <http://openlayers.org>`__ and the GeoServer mbstyle module.
- Where appropriate examples have been changed to reference `GeoWebCache <http://geowebcache.org/>`__.

:info:
      The `Mapbox Style Specification <https://www.mapbox.com/mapbox-gl-style-spec>`__ is generated from the BSD `Mapbox GL JS <https://github.com/mapbox/mapbox-gl-js>`__ github repository, reproduced here with details on this GeoServer implementation.


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


`version <#root-version>`__


*Required* :ref:`types-enum`.

Style specification version number. Must be 8.

::

    "version": 8


`name <#root-name>`__

*Optional* :ref:`types-string`.

A human-readable name for the style.

::

    "name": "Bright"

`metadata <#root-metadata>`__

*Optional*

Arbitrary properties useful to track with the stylesheet, but do not influence rendering. Properties should be prefixed to avoid collisions.

:note: *unsupported.*

`center <#root-center>`__


*Optional* :ref:`types-array`.


Default map center in longitude and latitude. The style center will be used only if the map has not been positioned by other means (e.g. map options or user interaction).

::

    "center": [
      -73.9749, 40.7736
    ]

:note: *unsupported*

`zoom <#root-zoom>`__

*Optional* :ref:`types-number`.


Default zoom level. The style zoom will be used only if the map has not
been positioned by other means (e.g. map options or user interaction).

::

    "zoom": 12.5

`bearing <#root-bearing>`__

*Optional* :ref:`types-number`. *Units in degrees. Defaults to* 0.

Default bearing, in degrees clockwise from true north. The style bearing
will be used only if the map has not been positioned by other means
(e.g. map options or user interaction).

::

    "bearing": 29

:note: *unsupported*

`pitch <#root-pitch>`__

*Optional* :ref:`types-number`. *Units in degrees. Defaults to* 0.

Default pitch, in degrees. Zero is perpendicular to the surface, for a
look straight down at the map, while a greater value like 60 looks ahead
towards the horizon. The style pitch will be used only if the map has
not been positioned by other means (e.g. map options or user
interaction).

::

    "pitch": 50

.. _root-light:

Light

.. raw:: html

   <i>Optional <a href="#light">light</a>.</i>


The global light source.

::

    "light": {
      "anchor": "viewport",
      "color": "white",
      "intensity": 0.4
    }

`sources <#root-sources>`__

*Required* :ref:`sources`.


Data source specifications.

::

    "sources": {
      "mapbox-streets": {
        "type": "vector",
        "url": "mapbox://mapbox.mapbox-streets-v6"
      }
    }

`sprite <#root-sprite>`__

*Optional* :ref:`types-string`.



A base URL for retrieving the sprite image and metadata. The extensions
``.png``, ``.json`` and scale factor ``@2x.png`` will be automatically
appended. This property is required if any layer uses the
``background-pattern``, ``fill-pattern``, ``line-pattern``,
``fill-extrusion-pattern``, or ``icon-image`` properties.

::

    "sprite" : "/geoserver/styles/mark"

`glyphs <#root-glyphs>`__

*Optional* :ref:`types-string`.



A URL template for loading signed-distance-field glyph sets in PBF
format. The URL must include ``{fontstack}`` and ``{range}`` tokens.
This property is required if any layer uses the ``text-field`` layout
property.

::

    "glyphs": "{fontstack}/{range}.pbf"

`transition <#root-transition>`__

*Required* :ref:`transition`.



A global transition definition to use as a default across properties.

::

    "transition": {
      "duration": 300,
      "delay": 0
    }

`layers <#root-layers>`__

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





.. raw:: html

   <div class="pad2 prose">

`Light <#light>`__
------------------

A style's ``light`` property provides global light source for that
style.

.. raw:: html

   <div class="space-bottom1 pad2x clearfix">

::

    "light": {
      "anchor": "viewport",
      "color": "white",
      "intensity": 0.4
    }


.. raw:: html

   <div class="pad2 keyline-all fill-white">

`anchor <#light-anchor>`__

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
   :widths: 10, 30, 30, 30
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoServer
     - OpenLayers
   * - Basic
     - 0.27.0
     - not yet supported
     - not yet supported

`position <#light-position>`__

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
   :widths: 10, 30, 30, 30
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoServer
     - OpenLayers
   * - Basic
     - 0.27.0
     - not yet supported
     - not yet supported

`color <#light-color>`__

*Optional* :ref:`types-color`. *Defaults to* #ffffff.


Color tint for lighting extruded geometries.


.. list-table::
   :widths: 10, 30, 30, 30
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoServer
     - OpenLayers
   * - Basic
     - 0.27.0
     - not yet supported
     - not yet supported

`intensity <#light-intensity>`__

*Optional* :ref:`types-number`. *Defaults to* 0.5.


Intensity of lighting (on a scale from 0 to 1). Higher numbers will
present as more extreme contrast.


.. list-table::
   :widths: 10, 30, 30, 30
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoServer
     - OpenLayers
   * - Basic
     - 0.27.0
     - not yet supported
     - not yet supported

.. _sources:

Sources
--------

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

   .. raw:: html

      <div class="space-bottom1 clearfix">

   ::

       "mapbox-streets": {
         "type": "vector",
         "tiles": [
           "http://a.example.com/tiles/{z}/{x}/{y}.pbf",
           "http://b.example.com/tiles/{z}/{x}/{y}.pbf"
         ],
         "maxzoom": 14
       }

   .. raw:: html

      </div>

-  By providing a ``"url"`` to a TileJSON resource:

   .. raw:: html

      <div class="space-bottom1 clearfix">

   ::

       "mapbox-streets": {
         "type": "vector",
         "url": "http://api.example.com/tilejson.json"
       }

   .. raw:: html

      </div>

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

.. _sources-vector:

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
      "url": "mapbox://mapbox.mapbox-streets-v6"
    }

`url <#sources-vector-url>`__

*Optional* :ref:`types-string`.



A URL to a TileJSON resource. Supported protocols are ``http:``,
``https:``, and ``mapbox://<mapid>``.

`tiles <#sources-vector-tiles>`__

*Optional* :ref:`types-array`.



An array of one or more tile source URLs, as in the TileJSON spec.

`minzoom <#sources-vector-minzoom>`__

*Optional* :ref:`types-number`. *Defaults to* 0.


Minimum zoom level for which tiles are available, as in the TileJSON
spec.

`maxzoom <#sources-vector-maxzoom>`__

*Optional* :ref:`types-number`. *Defaults to* 22.


Maximum zoom level for which tiles are available, as in the TileJSON
spec. Data from tiles at the maxzoom are used when displaying the map at
higher zoom levels.


.. list-table::
   :widths: 20, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0

.. raw:: html

   <div id="sources-raster" class="pad2 keyline-bottom">

.. _sources-raster:

raster
~~~~~~

A raster tile source. For raster tiles hosted by Mapbox, the ``"url"``
value should be of the form ``mapbox://mapid``.

::

    "mapbox-satellite": {
      "type": "raster",
      "url": "mapbox://mapbox.satellite",
      "tileSize": 256
    }

`url <#sources-raster-url>`__

*Optional* :ref:`types-string`.


A URL to a TileJSON resource. Supported protocols are ``http:``,
``https:``, and ``mapbox://<mapid>``.

`tiles <#sources-raster-tiles>`__

*Optional* :ref:`types-array`.



An array of one or more tile source URLs, as in the TileJSON spec.

`minzoom <#sources-raster-minzoom>`__

*Optional* :ref:`types-number`. *Defaults to* 0.


Minimum zoom level for which tiles are available, as in the TileJSON
spec.

`maxzoom <#sources-raster-maxzoom>`__

*Optional* :ref:`types-number`. *Defaults to* 22.


Maximum zoom level for which tiles are available, as in the TileJSON
spec. Data from tiles at the maxzoom are used when displaying the map at
higher zoom levels.

`tileSize <#sources-raster-tileSize>`__

*Optional* :ref:`types-number`. *Defaults to* 512.


The minimum visual size to display tiles for this layer. Only
configurable for raster layers.

.. list-table::
   :widths: 20, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0


.. raw:: html

   <div id="sources-geojson" class="pad2 keyline-bottom">

.. _sources-geojson:

geojson
~~~~~~~~

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

`data <#sources-geojson-data>`__

*Optional*


A URL to a GeoJSON file, or inline GeoJSON.

`maxzoom <#sources-geojson-maxzoom>`__

*Optional* :ref:`types-number`. *Defaults to* 18.


Maximum zoom level at which to create vector tiles (higher means greater
detail at high zoom levels).

`buffer <#sources-geojson-buffer>`__

*Optional* :ref:`types-number`. *Defaults to* 128.


Size of the tile buffer on each side. A value of 0 produces no buffer. A
value of 512 produces a buffer as wide as the tile itself. Larger values
produce fewer rendering artifacts near tile edges and slower
performance.

`tolerance <#sources-geojson-tolerance>`__

*Optional* :ref:`types-number`. *Defaults to* 0.375.


Douglas-Peucker simplification tolerance (higher means simpler
geometries and faster performance).

`cluster <#sources-geojson-cluster>`__

*Optional* :ref:`types-boolean`. *Defaults to* false.


If the data is a collection of point features, setting this to true
clusters the points by radius into groups.

`clusterRadius <#sources-geojson-clusterRadius>`__

*Optional* :ref:`types-number`. *Defaults to* 50.



Radius of each cluster if clustering is enabled. A value of 512
indicates a radius equal to the width of a tile.

`clusterMaxZoom <#sources-geojson-clusterMaxZoom>`__

*Optional* :ref:`types-number`.



Max zoom on which to cluster points if clustering is enabled. Defaults
to one zoom less than maxzoom (so that last zoom features are not
clustered).

.. list-table::
   :widths: 20, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - clustering
     - >= 0.14.0
     - >= 4.2.0
     - >= 3.4.0
     - >= 0.3.0
..
   SDK Requirements
   Mapbox GL JS
   Android SDK
   iOS SDK
   macOS SDK
   basic functionality
   >= 0.10.0
   >= 2.0.1
   >= 2.0.0
   >= 0.1.0
   clustering
   >= 0.14.0
   >= 4.2.0
   >= 3.4.0
   >= 0.3.0


.. raw:: html

   <div id="sources-image" class="pad2 keyline-bottom">

.. _sources-image:

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


`url <#sources-image-url>`__

*Required* :ref:`types-string`. 



URL that points to an image.

`coordinates <#sources-image-coordinates>`__

*Required* :ref:`types-array`.


Corners of image specified in longitude, latitude pairs.

.. list-table::
   :widths: 20, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - `Not yet supported <https://github.com/mapbox/mapbox-gl-native/issues/1350>`__
     - `Not yet supported <https://github.com/mapbox/mapbox-gl-native/issues/1350>`__
     - `Not yet supported <https://github.com/mapbox/mapbox-gl-native/issues/1350>`__


.. raw:: html

   <div id="sources-video" class="pad2 keyline-bottom">

.. _sources-video:

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

`urls <#sources-video-urls>`__


*Required* :ref:`types-array`.



URLs to video content in order of preferred format.

`coordinates <#sources-video-coordinates>`__

*Required* :ref:`types-array`.



Corners of video specified in longitude, latitude pairs.

.. list-table::
   :widths: 20, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - `Not yet supported <https://github.com/mapbox/mapbox-gl-native/issues/1350>`__
     - `Not yet supported <https://github.com/mapbox/mapbox-gl-native/issues/1350>`__
     - `Not yet supported <https://github.com/mapbox/mapbox-gl-native/issues/1350>`__

..
   SDK Support
   Mapbox GL JS
   Android SDK
   iOS SDK
   macOS SDK
   basic functionality
   >= 0.10.0
   `Not yet
   supported <https://github.com/mapbox/mapbox-gl-native/issues/601>`__
   `Not yet
   supported <https://github.com/mapbox/mapbox-gl-native/issues/601>`__
   `Not yet
   supported <https://github.com/mapbox/mapbox-gl-native/issues/601>`__


.. raw:: html

   <div id="sources-canvas" class="pad2 keyline-bottom">

.. _sources-canvas:

canvas
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

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

`coordinates <#sources-canvas-coordinates>`__


*Required* :ref:`types-array`.



Corners of canvas specified in longitude, latitude pairs.

`animate <#sources-canvas-animate>`__

.. raw:: html

   <i>Optional <a href="#boolean">boolean</a>. </i><i>Defaults to </i>true.



Whether the canvas source is animated. If the canvas is static,
``animate`` should be set to ``false`` to improve performance.

`canvas <#sources-canvas-canvas>`__

*Required* :ref:`types-string`. 



HTML ID of the canvas from which to read pixels.


.. list-table::
   :widths: 20, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.32.0
     - Not supported
     - Not supported
     - Not supported

.. raw:: html

   <div class="pad2 prose">

.. _sprite:

Sprite
--------------------

A style's ``sprite`` property supplies a URL template for loading small
images to use in rendering ``background-pattern``, ``fill-pattern``,
``line-pattern``, and ``icon-image`` style properties.

.. raw:: html

   <div class="space-bottom1 pad2x clearfix">

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

   .. raw:: html

      <div class="space-bottom1 clearfix">

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

   .. raw:: html

      </div>

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


.. raw:: html

   <div class="pad2 prose">

.. _glyphs:

Glyphs
--------------------

A style's ``glyphs`` property provides a URL template for loading
signed-distance-field glyph sets in PBF format.

.. raw:: html

   <div class="space-bottom1 pad2x clearfix">

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


.. raw:: html

   <div class="pad2 prose">

.. _transition:

Transition
----------------------------

A style's ``transition`` property provides global transition defaults
for that style.

.. raw:: html

   <div class="space-bottom1 pad2x clearfix">

::

    "transition": {
      "duration": 300,
      "delay": 0
    }


.. raw:: html

   <div class="pad2 keyline-all fill-white">

`duration <#transition-duration>`__

*Optional* :ref:`types-number`. *Units in milliseconds. Defaults to* 300. 


Time allotted for transitions to complete.

`delay <#transition-delay>`__

*Optional* :ref:`types-number`. *Units in milliseconds. Defaults to* 0. 



Length of time before a transition begins.





.. raw:: html

   <div class="pad2 prose">

.. _layers:

Layers
--------------------

A style's ``layers`` property lists all of the layers available in that
style. The type of layer is specified by the ``"type"`` property, and
must be one of background, fill, line, symbol, raster, circle,
fill-extrusion.

Except for layers of the background type, each layer needs to refer to a
source. Layers take the data that they get from a source, optionally
filter features, and then define how those features are styled.

.. raw:: html

   <div class="space-bottom1 pad2x clearfix">

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


.. raw:: html

   <div class="pad2 keyline-all fill-white">

.. _layer_id:

``id``

*Required* :ref:`types-string`. 


Unique layer name.

.. _layer-type:

``type``


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

``metadata``

*Optional*


Arbitrary properties useful to track with the layer, but do not
influence rendering. Properties should be prefixed to avoid collisions,
like 'mapbox:'.

``ref``

*Optional* :ref:`types-string`.



References another layer to copy ``type``, ``source``, ``source-layer``,
``minzoom``, ``maxzoom``, ``filter``, and ``layout`` properties from.
This allows the layers to share processing and be more efficient.

``source``

*Optional* :ref:`types-string`.



Name of a source description to be used for this layer.

``source-layer``

*Optional* :ref:`types-string`.



Layer to use from a vector tile source. Required if the source supports
multiple layers.

``minzoom``

*Optional* :ref:`types-number`.



The minimum zoom level on which the layer gets parsed and appears on.

``maxzoom``

*Optional* :ref:`types-number`.



The maximum zoom level on which the layer gets parsed and appears on.

``filter``

*Optional* :ref:`types-filter`.



A expression specifying conditions on source features. Only features
that match the filter are displayed.

``layout``

layout properties for the layer

``paint``

*Optional* paint properties for the layer


.. raw:: html

   <div class="pad2 prose">

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

Key: `supports interpolated functions <#function>`__ `supports piecewise
constant functions <#function>`__ transitionable

.. raw:: html

   <div class="space-bottom4 fill-white keyline-all">

.. raw:: html

   <div id="layers-background" class="pad2 keyline-bottom">


`background <#layers-background>`__
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~



`Layout Properties <#layout_background>`__
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

`visibility <#layout-background-visibility>`__

*Optional* :ref:`types-enum`. *One of* visible, none, *Defaults to* visible.


Whether this layer is displayed.


visible
    The layer is shown.

none
    The layer is not shown.

.. list-table::
   :widths: 10, 30, 30, 30
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoServer
     - OpenLayers
   * - Basic
     - 0.10.0
     - not yet supported
     - not yet supported

`Paint Properties <#paint_background>`__
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

`background-color <#paint-background-color>`__


*Optional* :ref:`types-color`. *Defaults to* #000000. *Disabled by* background-pattern.


The color with which the background will be drawn.


.. list-table::
   :widths: 10, 30, 30, 30
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoServer
     - OpenLayers
   * - Basic
     - 0.10.0
     - not yet supported
     - not yet supported

`background-pattern <#paint-background-pattern>`__

*Optional* :ref:`types-string`.



Name of image in sprite to use for drawing an image background. For
seamless patterns, image width and height must be a factor of two (2, 4,
8, ..., 512).


.. list-table::
   :widths: 10, 30, 30, 30
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoServer
     - OpenLayers
   * - Basic
     - 0.10.0
     - not yet supported
     - not yet supported

`background-opacity <#paint-background-opacity>`__

*Optional* :ref:`types-number`. *Defaults to* 1.

The opacity at which the background will be drawn.

.. list-table::
   :widths: 10, 30, 30, 30
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoServer
     - OpenLayers
   * - Basic
     - 0.10.0
     - not yet supported
     - not yet supported

.. raw:: html

   <div id="layers-fill" class="pad2 keyline-bottom">

`fill <#layers-fill>`__
~~~~~~~~~~~~~~~~~~~~~~~

`Layout Properties <#layout_fill>`__
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

`visibility <#layout-fill-visibility>`__


*Optional* :ref:`types-enum`. *One of* visible, none. *Defaults to* visible.

Whether this layer is displayed.

visible
    The layer is shown.

none
    The layer is not shown.

.. list-table::
   :widths: 10, 30, 30, 30
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoServer
     - OpenLayers
   * - Basic
     - 0.10.0
     - not yet supported
     - not yet supported

`Paint Properties <#paint_fill>`__
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

`fill-antialias <#paint-fill-antialias>`__

*Optional* :ref:`types-boolean`. *Defaults to* true.




Whether or not the fill should be antialiased.


.. list-table::
   :widths: 10, 30, 30, 30
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoServer
     - OpenLayers
   * - Basic
     - 0.10.0
     - not yet supported
     - not yet supported
   * - Data
     - not yet supported
     - 18.0
     - not yet supported

`fill-opacity <#paint-fill-opacity>`__

*Optional* :ref:`types-number`. *Defaults to* 1.


The opacity of the entire fill layer. In contrast to the ``fill-color``,
this value will also affect the 1px stroke around the fill, if the
stroke is used.

.. list-table::
   :widths: 10, 30, 30, 30
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoServer
     - OpenLayers
   * - Basic
     - 0.10.0
     - 18.0
     - 4.1.1
   * - Data
     - 0.21.0
     - 18.0
     - 4.1.1

`fill-color <#paint-fill-color>`__


*Optional* :ref:`types-color`. *Defaults to* #000000. *Disabled by* fill-pattern.


The color of the filled part of this layer. This color can be specified
as ``rgba`` with an alpha component and the color's opacity will not
affect the opacity of the 1px stroke, if it is used.


.. list-table::
   :widths: 10, 30, 30, 30
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoServer
     - OpenLayers
   * - Basic
     - 0.10.0
     - 18.0
     - 4.1.1
   * - Data
     - 0.19.0
     - 18.0
     - 4.1.1

`fill-outline-color <#paint-fill-outline-color>`__


*Optional* :ref:`types-color`. *Disabled by* fill-pattern. *Requires* fill-antialias = true.


The outline color of the fill. Matches the value of ``fill-color`` if
unspecified.

.. list-table::
   :widths: 10, 30, 30, 30
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoServer
     - OpenLayers
   * - Basic
     - 0.10.0
     - 18.0
     - 4.1.1
   * - Data
     - 0.19.0
     - 18.0
     - 4.1.1

`fill-translate <#paint-fill-translate>`__

*Optional* :ref:`types-array`. *Units in* pixels. *Defaults to* 0.0.


The geometry's offset. Values are [x, y] where negatives indicate left
and up, respectively.

.. list-table::
   :widths: 10, 30, 30, 30
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoServer
     - OpenLayers
   * - Basic
     - 0.10.0
     - 18.0
     - 4.1.1
   * - Data
     - not yet supported
     - 18.0
     - not yet supported

`fill-translate-anchor <#paint-fill-translate-anchor>`__


*Optional* :ref:`types-enum`. *One of* map, viewport. *Defaults to* map. *Requires* fill-translate.

Controls the translation reference point.

map
    The fill is translated relative to the map.

viewport
    The fill is translated relative to the viewport.

.. list-table::
   :widths: 10, 30, 30, 30
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoServer
     - OpenLayers
   * - Basic
     - 0.10.0
     - 18.0
     - 4.1.1
   * - Data
     - not yet supported
     - not yet supported
     - not yet supported

`fill-pattern <#paint-fill-pattern>`__

*Optional* :ref:`types-string`.


Name of image in sprite to use for drawing image fills. For seamless
patterns, image width and height must be a factor of two (2, 4, 8, ...,
512).

.. list-table::
   :widths: 10, 30, 30, 30
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoServer
     - OpenLayers
   * - Basic
     - 0.10.0
     - 18.0
     - 4.1.1
   * - Data
     - not yet supported
     - 18.0
     - not yet supported

.. raw:: html

   <div id="layers-line" class="pad2 keyline-bottom">

`line <#layers-line>`__
~~~~~~~~~~~~~~~~~~~~~~~

`Layout Properties <#layout_line>`__
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

`line-cap <#layout-line-line-cap>`__


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
   :widths: 10, 30, 30, 30
   :header-rows: 1

   * - Support
     - Mapbox
     - GeoServer
     - OpenLayers
   * - Basic
     - 0.10.0
     - 18.0
     - 4.1.1
   * - Data
     - not yet supported
     - 18.0
     - not yet supported



.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported
     - Not yet supported


`line-join <#layout-line-line-join>`__


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
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported
     - Not yet supported


`line-miter-limit <#layout-line-line-miter-limit>`__


*Optional* :ref:`types-number`. *Defaults to* 2. *Requires* line-join = miter.

Used to automatically convert miter joins to bevel joins for sharp
angles.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported
     - Not yet supported


`line-round-limit <#layout-line-line-round-limit>`__

*Optional* :ref:`types-number`. *Defaults to* 1.05. *Requires* line-join = round.


Used to automatically convert round joins to miter joins for shallow
angles.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported
     - Not yet supported


`visibility <#layout-line-visibility>`__


*Optional* :ref:`types-enum`. *One of* visible, none. *Defaults to* visible.

Whether this layer is displayed.


visible
    The layer is shown.

none
    The layer is not shown.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported
     - Not yet supported


`Paint Properties <#paint_line>`__
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

`line-opacity <#paint-line-opacity>`__

*Optional* :ref:`types-number`. *Defaults to* 1.


The opacity at which the line will be drawn.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - >= 0.29.0
     - >= 5.0.0
     - >= 3.5.0
     - >= 0.4.0


`line-color <#paint-line-color>`__


*Optional* :ref:`types-color`. *Defaults to* #000000. *Disabled by* line-pattern.

The color with which the line will be drawn.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - >= 0.23.0
     - >= 5.0.0
     - >= 3.5.0
     - >= 0.4.0


`line-translate <#paint-line-translate>`__

*Optional* :ref:`types-array`. *Units in* pixels. *Defaults to* 0.0.


The geometry's offset. Values are [x, y] where negatives indicate left
and up, respectively.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported
     - Not yet supported



`line-translate-anchor <#paint-line-translate-anchor>`__



*Optional* :ref:`types-enum`. *One of* map, viewport. *Defaults to* map. *Requires* line-translate.

Controls the translation reference point.


map
    The line is translated relative to the map.

viewport
    The line is translated relative to the viewport.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported
     - Not yet supported


`line-width <#paint-line-width>`__

*Optional* :ref:`types-number`. *Units in* pixels. *Defaults to* 1.

Stroke thickness.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported
     - Not yet supported


`line-gap-width <#paint-line-gap-width>`__

*Optional* :ref:`types-number`. *Units in* pixels. *Defaults to* 0.



Draws a line casing outside of a line's actual path. Value indicates the
width of the inner gap.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - >= 0.29.0
     - >= 5.0.0
     - >= 3.5.0
     - >= 0.4.0

`line-offset <#paint-line-offset>`__

*Optional* :ref:`types-number`. *Units in* pixels. *Defaults to* 0.


The line's offset. For linear features, a positive value offsets the
line to the right, relative to the direction of the line, and a negative
value to the left. For polygon features, a positive value results in an
inset, and a negative value results in an outset.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.12.1
     - >= 3.0.0
     - >= 3.1.0
     - >= 0.1.0
   * - data-driven styling
     - >= 0.29.0
     - >= 5.0.0
     - >= 3.5.0
     - >= 0.4.0


`line-blur <#paint-line-blur>`__

*Optional* :ref:`types-number`. *Units in* pixels. *Defaults to* 0.


Blur applied to the line, in pixels.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - >= 0.29.0
     - >= 5.0.0
     - >= 3.5.0
     - >= 0.4.0


`line-dasharray <#paint-line-dasharray>`__


*Optional* :ref:`types-array`. *Units in* line widths. *Disabled by* line-pattern.

Specifies the lengths of the alternating dashes and gaps that form the
dash pattern. The lengths are later scaled by the line width. To convert
a dash length to pixels, multiply the length by the current line width.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported
     - Not yet supported

`line-pattern <#paint-line-pattern>`__

*Optional* :ref:`types-string`.



Name of image in sprite to use for drawing image lines. For seamless
patterns, image width must be a factor of two (2, 4, 8, ..., 512).


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported
     - Not yet supported


.. raw:: html

   <div id="layers-symbol" class="pad2 keyline-bottom">

`symbol <#layers-symbol>`__
~~~~~~~~~~~~~~~~~~~~~~~~~~~



`Layout Properties <#layout_symbol>`__
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

`symbol-placement <#layout-symbol-symbol-placement>`__


*Optional* :ref:`types-enum`. *One of* point, line. *Defaults to* point.

Label placement relative to its geometry.


point
    The label is placed at the point where the geometry is located.

line
    The label is placed along the line of the geometry. Can only be used
    on ``LineString`` and ``Polygon`` geometries.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported
     - Not yet supported


`symbol-spacing <#layout-symbol-symbol-spacing>`__



*Optional* :ref:`types-number`. *Units in* pixels. *Defaults to* 250. *Requires* symbol-placement = line.

Distance between two symbol anchors.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported
     - Not yet supported


`symbol-avoid-edges <#layout-symbol-symbol-avoid-edges>`__

*Optional* :ref:`types-boolean`. *Defaults to* false.


If true, the symbols will not cross tile edges to avoid mutual
collisions. Recommended in layers that don't have enough padding in the
vector tile to prevent collisions, or if it is a point symbol layer
placed after a line symbol layer.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported
     - Not yet supported


`icon-allow-overlap <#layout-symbol-icon-allow-overlap>`__

*Optional* :ref:`types-boolean`. *Defaults to* false. *Requires* icon-image.


If true, the icon will be visible even if it collides with other
previously drawn symbols.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported
     - Not yet supported


`icon-ignore-placement <#layout-symbol-icon-ignore-placement>`__

*Optional* :ref:`types-boolean`. *Defaults to* false. *Requires* icon-image.


If true, other symbols can be visible even if they collide with the
icon.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported
     - Not yet supported


`icon-optional <#layout-symbol-icon-optional>`__

*Optional* :ref:`types-boolean`. *Defaults to* false. *<Requires* icon-image, text-field.



If true, text will display without their corresponding icons when the
icon collides with other symbols and the text does not.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported
     - Not yet supported


`icon-rotation-alignment <#layout-symbol-icon-rotation-alignment>`__


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
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - ``auto`` value
     - >= 0.25.0
     - >= 4.2.0
     - >= 3.4.0
     - >= 0.3.0
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported
     - Not yet supported


`icon-size <#layout-symbol-icon-size>`__


*Optional* :ref:`types-number`. *Defaults to* 1. *Requires* icon-image.
Scale factor for icon. 1 is original size, 3 triples the size.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - >= 0.35.0
     - Not yet supported
     - Not yet supported
     - Not yet supported



`icon-text-fit <#layout-symbol-icon-text-fit>`__


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
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.21.0
     - >= 4.2.0
     - >= 3.4.0
     - >= 0.2.1
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported
     - Not yet supported


`icon-text-fit-padding <#layout-symbol-icon-text-fit-padding>`__


*Optional :ref:`types-array`. *Units in* pixels. *Defaults to* 0,0,0,0. *Requires* icon-image, text-field, icon-text-fit = one of both, width, height.

Size of the additional area added to dimensions determined by
``icon-text-fit``, in clockwise order: top, right, bottom, left.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.21.0
     - >= 4.2.0
     - >= 3.4.0
     - >= 0.2.1
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported
     - Not yet supported


`icon-image <#layout-symbol-icon-image>`__

*Optional* :ref:`types-string`.



Name of image in sprite to use for drawing an image background. A string
with {tokens} replaced, referencing the data property to pull from.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - >= 0.35.0
     - Not yet supported
     - Not yet supported
     - Not yet supported


`icon-rotate <#layout-symbol-icon-rotate>`__


*Optional* :ref:`types-number`. *Units in* degrees. *Defaults to* 0. *Requires* icon-image.

Rotates the icon clockwise.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - >= 0.21.0
     - >= 5.0.0
     - >= 3.5.0
     - >= 0.4.0


`icon-padding <#layout-symbol-icon-padding>`__


*Optional* :ref:`types-number`. *Units in* pixels. *Defaults to* 2. *Requires* icon-image.


Size of the additional area around the icon bounding box used for
detecting symbol collisions.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported
     - Not yet supported



`icon-keep-upright <#layout-symbol-icon-keep-upright>`__

*Optional* :ref:`types-boolean`. *Defaults to* false. *Requires* icon-image, icon-rotation-alignment = map, symbol-placement = line.


If true, the icon may be flipped to prevent it from being rendered
upside-down.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported
     - Not yet supported


`icon-offset <#layout-symbol-icon-offset>`__


*Optional* :ref:`types-array`. *Defaults to* 0,0. *Requires* icon-image.

Offset distance of icon from its anchor. Positive values indicate right
and down, while negative values indicate left and up. When combined with
``icon-rotate`` the offset will be as if the rotated direction was up.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - >= 0.29.0
     - >= 5.0.0
     - >= 3.5.0
     - >= 0.4.0


`text-pitch-alignment <#layout-symbol-text-pitch-alignment>`__


*Optional* :ref:`types-enum` *One of* map, viewport, auto. *Defaults to* auto. *Requires* text-field.

Orientation of text when map is pitched.


map
    The text is aligned to the plane of the map.

viewport
    The text is aligned to the plane of the viewport.

auto
    Automatically matches the value of ``text-rotation-alignment``.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - ``auto`` value
     - >= 0.25.0
     - >= 4.2.0
     - >= 3.4.0
     - >= 0.3.0
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported
     - Not yet supported



`text-rotation-alignment <#layout-symbol-text-rotation-alignment>`__

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
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - ``auto`` value
     - >= 0.25.0
     - >= 4.2.0
     - >= 3.4.0
     - >= 0.3.0
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported
     - Not yet supported



`text-field <#layout-symbol-text-field>`__

*Optional* :ref:`types-string`.



Value to use for a text label. Feature properties are specified using
tokens like {field\_name}. (Token replacement is only supported for
literal ``text-field`` values--not for property functions.)


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - >= 0.33.0
     - >= 5.0.0
     - >= 3.5.0
     - >= 0.4.0


`text-font <#layout-symbol-text-font>`__


*Optional* :ref:`types-array`. *Defaults to* Open Sans Regular,Arial Unicode MS Regular. *Requires* text-field.

Font stack to use for displaying text.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported
     - Not yet supported



`text-size <#layout-symbol-text-size>`__

*Optional* :ref:`types-number`. *Units in* pixels. *Defaults to* 16. *Requires* text-field.



Font size.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - >= 0.35.0
     - Not yet supported
     - Not yet supported
     - Not yet supported



`text-max-width <#layout-symbol-text-max-width>`__

*Optional* :ref:`types-number`. *Units in* pixels. *Defaults to* 10. *Requires* text-field.



The maximum line width for text wrapping.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported
     - Not yet supported


`text-line-height <#layout-symbol-text-line-height>`__

*Optional* :ref:`types-number`. *Units in* ems. *Defaults to* 1.2. *Requires* text-field.



Text leading value for multi-line text.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported
     - Not yet supported


`text-letter-spacing <#layout-symbol-text-letter-spacing>`__

*Optional* :ref:`types-number`. *Units in* ems. *Defaults to* 0. *Requires* text-field.



Text tracking amount.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported
     - Not yet supported



`text-justify <#layout-symbol-text-justify>`__

*Optional* :ref:`types-enum`. *One of* left, center, right. *Defaults to* center. *Requires* text-field.


Text justification options.


left
    The text is aligned to the left.

center
    The text is centered.

right
    The text is aligned to the right.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported
     - Not yet supported


`text-anchor <#layout-symbol-text-anchor>`__

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
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported
     - Not yet supported


`text-max-angle <#layout-symbol-text-max-angle>`__

*Optional* :ref:`types-number`. *Units in* degrees. *Defaults to* 45. *Requires* text-field, symbol-placement = line.


Maximum angle change between adjacent characters.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported
     - Not yet supported


`text-rotate <#layout-symbol-text-rotate>`__

*Optional* :ref:`types-number`. *Units in* degrees. *Defaults to* 0. *Requires* text-field.



Rotates the text clockwise.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - >= 0.35.0
     - Not yet supported
     - Not yet supported
     - Not yet supported



`text-padding <#layout-symbol-text-padding>`__

*Optional* :ref:`types-number`. *Units in* pixels. *Defaults to* 2. *Requires* text-field.



Size of the additional area around the text bounding box used for
detecting symbol collisions.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported
     - Not yet supported



`text-keep-upright <#layout-symbol-text-keep-upright>`__

*Optional* :ref:`types-boolean`. *Defaults to* true. *Requires* text-field, text-rotation-alignment = true, symbol-placement = true.



If true, the text may be flipped vertically to prevent it from being
rendered upside-down.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported
     - Not yet supported



`text-transform <#layout-symbol-text-transform>`__


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
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - >= 0.33.0
     - >= 5.0.0
     - >= 3.5.0
     - >= 0.4.0



`text-offset <#layout-symbol-text-offset>`__

*Optional* :ref:`types-array`. *Units in* pixels. *Defaults to* 0,0. *Requires* icon-image.

Offset distance of text from its anchor. Positive values indicate right
and down, while negative values indicate left and up.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - >= 0.35.0
     - Not yet supported
     - Not yet supported
     - Not yet supported



`text-allow-overlap <#layout-symbol-text-allow-overlap>`__

*Optional* :ref:`types-boolean`. *Defaults to* false. *Requires* text-field.



If true, the text will be visible even if it collides with other
previously drawn symbols.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported
     - Not yet supported



`text-ignore-placement <#layout-symbol-text-ignore-placement>`__

*Optional* :ref:`types-boolean`. *Defaults to* false. *Requires* text-field



If true, other symbols can be visible even if they collide with the
text.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported
     - Not yet supported



`text-optional <#layout-symbol-text-optional>`__

*Optional* :ref:`types-boolean`. *Defaults to* false. *Requires* text-field, icon-image.



If true, icons will display without their corresponding text when the
text collides with other symbols and the icon does not.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported
     - Not yet supported



`visibility <#layout-symbol-visibility>`__

*Optional* :ref:`types-enum`. *One of* visible, none. *Defaults to* visible.



Whether this layer is displayed.


visible
    The layer is shown.

none
    The layer is not shown.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported
     - Not yet supported



`Paint Properties <#paint_symbol>`__
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

`icon-opacity <#paint-icon-opacity>`__

*Optional* :ref:`types-number`. *Defaults to* 1. <i>Requires </i>icon-image.


The opacity at which the icon will be drawn.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - >= 0.33.0
     - >= 5.0.0
     - >= 3.5.0
     - >= 0.4.0


`icon-color <#paint-icon-color>`__

*Optional* :ref:`types-color`. *Defaults to* #000000. *Requires* icon-image.



The color of the icon. This can only be used with sdf icons.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - >= 0.33.0
     - >= 5.0.0
     - >= 3.5.0
     - >= 0.4.0



`icon-halo-color <#paint-icon-halo-color>`__

*Optional* :ref:`types-color`. *Defaults to* rgba(0, 0, 0, 0). *Requires* icon-image.



The color of the icon's halo. Icon halos can only be used with SDF
icons.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - >= 0.33.0
     - >= 5.0.0
     - >= 3.5.0
     - >= 0.4.0


`icon-halo-width <#paint-icon-halo-width>`__

*Optional* :ref:`types-number`. *Units in* pixels. *Defaults to* 0. *Requires* icon-image.



Distance of halo to the icon outline.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - >= 0.33.0
     - >= 5.0.0
     - >= 3.5.0
     - >= 0.4.0



`icon-halo-blur <#paint-icon-halo-blur>`__

*Optional* :ref:`types-number`. *Units in* pixels. *Defaults to* 0. *Requires* icon-image.



Fade out the halo towards the outside.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - >= 0.33.0
     - >= 5.0.0
     - >= 3.5.0
     - >= 0.4.0



`icon-translate <#paint-icon-translate>`__

*Optional* :ref:`types-array`. *Units in* pixels. *Defaults to* 0,0. *Requires* icon-image.



Distance that the icon's anchor is moved from its original placement.
Positive values indicate right and down, while negative values indicate
left and up.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported
     - Not yet supported



`icon-translate-anchor <#paint-icon-translate-anchor>`__

*Optional* :ref:`types-enum` *One of* map, viewport. *Defaults to* map. *Requires* icon-image, icon-translate.



Controls the translation reference point.


map
    Icons are translated relative to the map.

viewport
    Icons are translated relative to the viewport.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported
     - Not yet supported


`text-opacity <#paint-text-opacity>`__

*Optional* :ref:`types-number`. *Defaults to* 1. <i>Requires </i>text-field.


The opacity at which the text will be drawn.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - >= 0.33.0
     - >= 5.0.0
     - >= 3.5.0
     - >= 0.4.0



`text-color <#paint-text-color>`__

*Optional* :ref:`types-color`. *Defaults to* #000000. *Requires* text-field.



The color with which the text will be drawn.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - >= 0.33.0
     - >= 5.0.0
     - >= 3.5.0
     - >= 0.4.0



`text-halo-color <#paint-text-halo-color>`__

*Optional* :ref:`types-color`. *Defaults to* rgba(0, 0, 0, 0). *Requires* text-field.



The color of the text's halo, which helps it stand out from backgrounds.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - >= 0.33.0
     - >= 5.0.0
     - >= 3.5.0
     - >= 0.4.0



`text-halo-width <#paint-text-halo-width>`__

*Optional* :ref:`types-number`. *Units in* pixels. *Defaults to* 0. *Requires* text-field.



Distance of halo to the font outline. Max text halo width is 1/4 of the
font-size.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - >= 0.33.0
     - >= 5.0.0
     - >= 3.5.0
     - >= 0.4.0



`text-halo-blur <#paint-text-halo-blur>`__

*Optional* :ref:`types-number`. *Units in* pixels. *Defaults to* 0. *Requires* text-field.



The halo's fadeout distance towards the outside.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - >= 0.33.0
     - >= 5.0.0
     - >= 3.5.0
     - >= 0.4.0



`text-translate <#paint-text-translate>`__

*Optional* :ref:`types-array`. *Units in* pixels. *Defaults to* 0,0. *Requires* text-field.



Distance that the text's anchor is moved from its original placement.
Positive values indicate right and down, while negative values indicate
left and up.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported
     - Not yet supported



`text-translate-anchor <#paint-text-translate-anchor>`__

*Optional* :ref:`types-enum` *One of* map, viewport. *Defaults to* map. *Requires* text-field, text-translate.



Controls the translation reference point.


map
    The text is translated relative to the map.

viewport
    The text is translated relative to the viewport.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported
     - Not yet supported



.. raw:: html

   <div id="layers-raster" class="pad2 keyline-bottom">

`raster <#layers-raster>`__
~~~~~~~~~~~~~~~~~~~~~~~~~~~



`Layout Properties <#layout_raster>`__
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

`visibility <#layout-raster-visibility>`__

*Optional* :ref:`types-enum`. *One of* visible, none. *Defaults to* visible.



Whether this layer is displayed.


visible
    The layer is shown.

none
    The layer is not shown.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported
     - Not yet supported



`Paint Properties <#paint_raster>`__
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

`raster-opacity <#paint-raster-opacity>`__

*Optional* :ref:`types-number`. *Defaults to* 1.


The opacity at which the image will be drawn.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported
     - Not yet supported



`raster-hue-rotate <#paint-raster-hue-rotate>`__

*Optional* :ref:`types-number`. *Units in* degrees. *Defaults to* 0.



Rotates hues around the color wheel.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported
     - Not yet supported



`raster-brightness-min <#paint-raster-brightness-min>`__

*Optional* :ref:`types-number`. *Defaults to* 0.


Increase or reduce the brightness of the image. The value is the minimum
brightness.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported
     - Not yet supported



`raster-brightness-max <#paint-raster-brightness-max>`__

*Optional* :ref:`types-number`. *Defaults to* 1.


Increase or reduce the brightness of the image. The value is the maximum
brightness.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported
     - Not yet supported



`raster-saturation <#paint-raster-saturation>`__

*Optional* :ref:`types-number`. *Defaults to* 0.


Increase or reduce the saturation of the image.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported
     - Not yet supported



`raster-contrast <#paint-raster-contrast>`__

*Optional* :ref:`types-number`. *Defaults to* 0.


Increase or reduce the contrast of the image.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported
     - Not yet supported



`raster-fade-duration <#paint-raster-fade-duration>`__

*Optional* :ref:`types-number` *Units in* milliseconds. *Defaults to* 300.



Fade duration when a new tile is added.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported
     - Not yet supported



.. raw:: html

   <div id="layers-circle" class="pad2 keyline-bottom">

`circle <#layers-circle>`__
~~~~~~~~~~~~~~~~~~~~~~~~~~~



`Layout Properties <#layout_circle>`__
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

`visibility <#layout-circle-visibility>`__

*Optional* :ref:`types-enum`. *One of* visible, none. *Defaults to* visible.



Whether this layer is displayed.


visible
    The layer is shown.

none
    The layer is not shown.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0


`Paint Properties <#paint_circle>`__
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

`circle-radius <#paint-circle-radius>`__

*Optional* :ref:`types-number`. *Units in* pixels. *Defaults to* 5.



Circle radius.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - >= 0.18.0
     - >= 5.0.0
     - >= 3.5.0
     - >= 0.4.0



`circle-color <#paint-circle-color>`__

*Optional* :ref:`types-color`. *Defaults to* #000000.



The fill color of the circle.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - >= 0.18.0
     - >= 5.0.0
     - >= 3.5.0
     - >= 0.4.0



`circle-blur <#paint-circle-blur>`__

*Optional* :ref:`types-number`. *Defaults to* 0.


Amount to blur the circle. 1 blurs the circle such that only the
centerpoint is full opacity.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - >= 0.20.0
     - >= 5.0.0
     - >= 3.5.0
     - >= 0.4.0



`circle-opacity <#paint-circle-opacity>`__

*Optional* :ref:`types-number`. *Defaults to* 1.


The opacity at which the circle will be drawn.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - >= 0.20.0
     - >= 5.0.0
     - >= 3.5.0
     - >= 0.4.0


`circle-translate <#paint-circle-translate>`__

*Optional* :ref:`types-array`. *Units in* pixels. *Defaults to* 0,0.



The geometry's offset. Values are [x, y] where negatives indicate left
and up, respectively.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported
     - Not yet supported



`circle-translate-anchor <#paint-circle-translate-anchor>`__

*Optional* :ref:`types-enum` *One of* map, viewport. *Defaults to* map. *Requires* circle-translate.



Controls the translation reference point.


map
    The circle is translated relative to the map.

viewport
    The circle is translated relative to the viewport.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported
     - Not yet supported



`circle-pitch-scale <#paint-circle-pitch-scale>`__

*Optional* :ref:`types-enum` *One of* map, viewport. *Defaults to* map.



Controls the scaling behavior of the circle when the map is pitched.


map
    Circles are scaled according to their apparent distance to the
    camera.

viewport
    Circles are not scaled.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.21.0
     - >= 4.2.0
     - >= 3.4.0
     - >= 0.2.1
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported
     - Not yet supported



`circle-stroke-width <#paint-circle-stroke-width>`__

*Optional* :ref:`types-number`. *Units in* pixels. *Defaults to* 5.



The width of the circle's stroke. Strokes are placed outside of the
``circle-radius``.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.29.0
     - >= 5.0.0
     - >= 3.5.0
     - >= 0.4.0
   * - data-driven styling
     - >= 0.29.0
     - >= 5.0.0
     - >= 3.5.0
     - >= 0.4.0


`circle-stroke-color <#paint-circle-stroke-color>`__

*Optional* :ref:`types-color`. *Defaults to* #000000.



The stroke color of the circle.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.29.0
     - >= 5.0.0
     - >= 3.5.0
     - >= 0.4.0
   * - data-driven styling
     - >= 0.29.0
     - >= 5.0.0
     - >= 3.5.0
     - >= 0.4.0

`circle-stroke-opacity <#paint-circle-stroke-opacity>`__

*Optional* :ref:`types-number`. *Defaults to* 1.


The opacity of the circle's stroke.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.29.0
     - >= 5.0.0
     - >= 3.5.0
     - >= 0.4.0
   * - data-driven styling
     - >= 0.29.0
     - >= 5.0.0
     - >= 3.5.0
     - >= 0.4.0



.. raw:: html

   <div id="layers-fill-extrusion" class="pad2 keyline-bottom">

`fill-extrusion <#layers-fill-extrusion>`__
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~



`Layout Properties <#layout_fill-extrusion>`__
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

`visibility <#layout-fill-extrusion-visibility>`__

*Optional* :ref:`types-enum`. *One of* visible, none. *Defaults to* visible.



Whether this layer is displayed.


visible
    The layer is shown.

none
    The layer is not shown.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.27.0
     - Not yet supported
     - Not yet supported
     - Not yet supported



`Paint Properties <#paint_fill-extrusion>`__
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

`fill-extrusion-opacity <#paint-fill-extrusion-opacity>`__

*Optional* :ref:`types-number`. *Defaults to* 1.


The opacity of the entire fill extrusion layer. This is rendered on a
per-layer, not per-feature, basis, and data-driven styling is not
available.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.27.0
     - Not yet supported
     - Not yet supported
     - Not yet supported



`fill-extrusion-color <#paint-fill-extrusion-color>`__

*Optional* :ref:`types-color`. *Defaults to* #000000. *Disabled by* fill-extrusion-pattern.



The base color of the extruded fill. The extrusion's surfaces will be
shaded differently based on this color in combination with the root
``light`` settings. If this color is specified as ``rgba`` with an alpha
component, the alpha component will be ignored; use
``fill-extrusion-opacity`` to set layer opacity.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.27.0
     - Not yet supported
     - Not yet supported
     - Not yet supported
   * - data-driven styling
     - >= 0.27.0
     - Not yet supported
     - Not yet supported
     - Not yet supported



`fill-extrusion-translate <#paint-fill-extrusion-translate>`__

*Optional* :ref:`types-array`. *Units in* pixels. *Defaults to* 0,0.



The geometry's offset. Values are [x, y] where negatives indicate left
and up (on the flat plane), respectively.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.27.0
     - Not yet supported
     - Not yet supported
     - Not yet supported
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported
     - Not yet supported



`fill-extrusion-translate-anchor <#paint-fill-extrusion-translate-anchor>`__

*Optional* :ref:`types-enum` *One of* map, viewport. *Defaults to* map. *Requires* fill-extrusion-translate.



Controls the translation reference point.


map
    The fill extrusion is translated relative to the map.

viewport
    The fill extrusion is translated relative to the viewport.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.27.0
     - Not yet supported
     - Not yet supported
     - Not yet supported
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported
     - Not yet supported



`fill-extrusion-pattern <#paint-fill-extrusion-pattern>`__

*Optional* :ref:`types-string`.



Name of image in sprite to use for drawing images on extruded fills. For
seamless patterns, image width and height must be a factor of two (2, 4,
8, ..., 512).


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.27.0
     - Not yet supported
     - Not yet supported
     - Not yet supported
   * - data-driven styling
     - Not yet supported
     - Not yet supported
     - Not yet supported
     - Not yet supported



`fill-extrusion-height <#paint-fill-extrusion-height>`__

*Optional* :ref:`types-number` *Units in* meters. *Defaults to* 0.


The height with which to extrude this layer.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.27.0
     - Not yet supported
     - Not yet supported
     - Not yet supported
   * - data-driven styling
     - >= 0.27.0
     - Not yet supported
     - Not yet supported
     - Not yet supported



`fill-extrusion-base <#paint-fill-extrusion-base>`__

*Optional* :ref:`types-number` *Units in* meters. *Defaults to* 0. *Requires* fill-extrusion-height.



The height with which to extrude the base of this layer. Must be less
than or equal to ``fill-extrusion-height``.


.. list-table::
   :widths: 10, 30, 30
   :header-rows: 1

   * - Mapbox
     - GeoServer
     - OpenLayers
   * - 0.27.0
     - 18.0
     - 4.1.1

.. list-table::
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.27.0
     - Not yet supported
     - Not yet supported
     - Not yet supported
   * - data-driven styling
     - >= 0.27.0
     - Not yet supported
     - Not yet supported
     - Not yet supported



.. raw:: html

   <div class="pad2 prose">

`Types <#types>`__
------------------

A Mapbox style contains values of various types, most commonly as values
for the style properties of a layer.

.. raw:: html

   <div class="keyline-all fill-white">

.. raw:: html

   <div class="pad2 keyline-bottom">

.. _types-color:

Color
~~~~~~~~~~~~~~~~~~~~~~~~

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


.. raw:: html

   <div class="pad2 keyline-bottom">

.. _types-enum:

Enum
~~~~

One of a fixed list of string values. Use quotes around values.

::

    {
      "text-transform": "uppercase"
    }


.. raw:: html

   <div class="pad2 keyline-bottom">

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


.. raw:: html

   <div class="pad2 keyline-bottom">

.. _types-boolean:

Boolean
~~~~~~~

Boolean means yes or no, so it accepts the values ``true`` or ``false``.

::

    {
      "fill-enabled": true
    }


.. raw:: html

   <div class="pad2 keyline-bottom">

.. _types-number:

Number
~~~~~~~

A number value, often an integer or floating point (decimal number).
Written without quotes.

::

    {
      "text-size": 24
    }


.. raw:: html

   <div class="pad2 keyline-bottom">

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


.. raw:: html

   <div class="pad2 keyline-bottom">

.. _types-function:

Function
~~~~~~~~~

The value for any layout or paint property may be specified as a
*function*. Functions allow you to make the appearance of a map feature
change with the current zoom level and/or the feature's properties.

.. raw:: html

   <div class="col12 pad1x">

.. raw:: html

   <div class="col12 clearfix pad0y pad2x space-bottom2">



.. _stops:

stops

*Required (except for identity functions) `array <#types-array>`__.*




Functions are defined in terms of input and output values. A set of one
input value and one output value is known as a "stop."



.. raw:: html

   <div class="col12 clearfix pad0y pad2x space-bottom2">

.. _property:

property

*Optional* :ref:`types-string`.





If specified, the function will take the specified feature property as
an input. See `Zoom Functions and Property
Functions <#types-function-zoom-property>`__ for more information.



.. raw:: html

   <div class="col12 clearfix pad0y pad2x space-bottom2">

.. _base:

base
.. raw:: html

   <i>Optional <a href="#number">number</a>. Default is</i> 1.





The exponential base of the interpolation curve. It controls the rate at
which the function output increases. Higher values make the output
increase more towards the high end of the range. With values close to 1
the output increases linearly.



.. raw:: html

   <div class="col12 clearfix pad0y pad2x space-bottom2">

.. _type:

type

.. raw:: html

   <i>Optional <a href="#enum">enum</a>. One of</i> identity, exponential, interval, categorical.



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


.. raw:: html

   <div class="col12 clearfix pad0y pad2x space-bottom2">

.. _function-default:

default




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



.. raw:: html

   <div class="col12 clearfix pad0y pad2x space-bottom2">

.. _function-colorspace:

colorSpace

.. raw:: html

   <i>Optional <a href="#enum">enum</a>. One of</i> rgb, lab, hcl.



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
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - ``property``
     - >= 0.18.0
     - >= 5.0.0
     - >= 3.5.0
     - >= 0.4.0
   * - ``type``
     - >= 0.18.0
     - >= 5.0.0
     - >= 3.5.0
     - >= 0.4.0
   * - ``exponential`` type
     - >= 0.18.0
     - >= 5.0.0
     - >= 3.5.0
     - >= 0.4.0
   * - ``interval`` type
     - >= 0.18.0
     - >= 5.0.0
     - >= 3.5.0
     - >= 0.4.0
   * - ``categorical`` type
     - >= 0.18.0
     - >= 5.0.0
     - >= 3.5.0
     - >= 0.4.0
   * - ``identity`` type
     - >= 0.18.0
     - >= 5.0.0
     - >= 3.5.0
     - >= 0.4.0
   * - ``default`` type
     - >= 0.18.0
     - >= 5.0.0
     - >= 3.5.0
     - >= 0.4.0
   * - ``colorSpace`` type
     - >= 0.26.0
     - Not yet supported
     - Not yet supported
     - Not yet supported


**Zoom functions** allow the appearance of a map feature to change with
map’s zoom level. Zoom functions can be used to create the illusion of
depth and control data density. Each stop is an array with two elements:
the first is a zoom level and the second is a function output value.

.. raw:: html

   <div class="col12 space-bottom">

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



The rendered values of `color <#types-color>`__,
`number <#types-number>`__, and `array <#types-array>`__ properties are
intepolated between stops. `Enum <#types-enum>`__,
`boolean <#types-boolean>`__, and `string <#types-string>`__ property
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

.. raw:: html

   <div class="col12 space-bottom">

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



\ **Zoom-and-property functions** allow the appearance of a map feature
to change with both its properties *and* zoom. Each stop is an array
with two elements, the first is an object with a property input value
and a zoom, and the second is a function output value. Note that support
for property functions is not yet complete.

.. raw:: html

   <div class="col12 space-bottom">

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




.. raw:: html

   <div class="pad2">

.. _types-filter:

Filter
~~~~~~

A filter selects specific features from a layer. A filter is an array of
one of the following forms:

.. raw:: html

   <div class="col12 clearfix space-bottom2">

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
`string <#string>`__, `number <#number>`__, or `boolean <#boolean>`__ to
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
   :widths: 30, 30, 30, 30, 30
   :header-rows: 1

   * - SDK Support
     - Mapbox GL JS
     - Android SDK
     - iOS SDK
     - macOS SDK
   * - basic functionality
     - >= 0.10.0
     - >= 2.0.1
     - >= 2.0.0
     - >= 0.1.0
   * - ``has``/``!has``
     - >= 0.19.0
     - >= 4.1.0
     - >= 3.3.0
     - >= 0.1.0

