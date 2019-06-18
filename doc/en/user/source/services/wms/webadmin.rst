.. _services_webadmin_wms:

WMS settings
============

This page details the configuration options for WMS in the web administration interface.

.. figure:: img/services_WMS.png

   WMS configuration options

Service Metadata
----------------

See the section on :ref:`service_metadata`.

.. _services_webadmin_wms_raster_options:

Root Layer Information
----------------------

.. figure:: img/services_WMS_rootLayerInfo.png

In this section is possible to define a title and an abstract for the root layer in the WMS capabilities. When these are left empty the WMS service title and abstract are used.

It is also possible to set the flag **Always include Root Layer in capabilities**.
This is checked by default, but can be unset so that the root layer is included in capabilities only when there is NOT already a single top level Layer element.
This can be useful to allow compatibility with some WMS clients that are not happy with the two or more layer tree levels.
This default setting can be overridden at the layer or request level.

Raster Rendering Options
------------------------

The Web Map Service Interface Standard (WMS) provides a simple way to request and serve geo-registered map images. During pan and zoom operations, WMS requests generate map images through a variety of raster rendering processes. Such image manipulation is generally called resampling, interpolation, or down-sampling. GeoServer supports three resampling methods that determine how cell values of a raster are outputted. These sampling methods—Nearest Neighbor, Bilinear Interpolation and Bicubic—are available on the Default Interpolation menu.

**Nearest Neighbor**—Uses the center of nearest input cell to determine the value of the output cell. Original values are retained and no new averages are created. Because image values stay exactly the same, rendering is fast but possibly pixelated from sharp edge detail. Nearest neighbor interpolation is recommended for categorical data such as land use classification.

**Bilinear**—Determines the value of the output cell based by sampling the value of the four nearest cells by linear weighting. The closer an input cell, the higher its influence of on the output cell value. Since output values may differ from nearest input, bilinear interpolation is recommended for continuous data like elevation and raw slope values. Bilinear interpolation takes about five times as long as nearest neighbor interpolation.

**Bicubic**—Looks at the sixteen nearest cells and fits a smooth curve through the points to find the output value. Bicubic interpolation may both change the input value as well as place the output value outside of the range of input values. Bicubic interpolation is recommended for smoothing continuous data, but this incurs a processing performance overhead.

Watermark Settings
------------------

Watermarking is the process of embedding an image into a map. Watermarks are usually used for branding, copyright, and security measures. Watermarks are configured in the WMS watermarks setting section.

**Enable Watermark**—Turns on watermarking. When selected, all maps will render with the same watermark. It is not currently possible to specify watermarking on a per-layer or per-feature basis.

**Watermark URL**—Location of the graphic for the watermark. The graphic can be referenced as an absolute path (e.g., :file:`C:\\GeoServer\\watermark.png`), a relative one inside GeoServer's data directory (e.g., :file:`watermark.png`), or a URL (e.g., ``http://www.example.com/images/watermark.png``).

Each of these methods have their own advantages and disadvantages. When using an absolute or relative link, GeoServer keeps a cached copy of the graphic in memory, and won't continually link to the original file. This means that if the original file is subsequently deleted, GeoServer won't register it missing until the watermark settings are edited. Using a URL might seem more convenient, but it is more I/O intensive. GeoServer will load the watermark image for every WMS request. Also, should the URL cease to be valid, the layer will not properly display.

**Watermark Transparency**–Determines the opacity level of the watermark. Numbers range between 0 (opaque) and 100 (fully invisible).

**Watermark Position**—Specifies the position of the watermark relative to the WMS request. The nine options indicate which side and corner to place the graphic (top-left, top-center, top-right, etc). The default watermark position is bottom-right. Note that the watermark will always be displayed flush with the boundary. If extra space is required, the graphic itself needs to change.

Because each WMS request renders the watermark, a single tiled map positions *one* watermark relative to the view window while a tiled map positions the watermark for each tile.  The only layer specific aspect of watermarking occurs because a single tile map is one WMS request, whereas a tiled map contains many WMS requests.  (The latter watermark display resembles Google Maps faint copyright notice in their Satellite imagery.)  The following three examples demonstrate watermark position, transparency and tiling display, respectively.

.. figure:: img/services_WMS_watermark1.png

   Single tile watermark (aligned top-right, transparency=0)

.. figure:: img/services_WMS_watermark2.png

   Single tile watermark (aligned top-right, transparency=90)

.. figure:: img/services_WMS_watermark3.png

   Tiled watermark (aligned top-right, transparency=90)

SVG Options
-----------

The GeoServer WMS supports SVG (Scalable Vector Graphics) as an output format. GeoServer currently supports two SVG renderers, available from the SVG producer menu.


#. *Simple*—Simple SVG renderer. It has limited support for SLD styling, but is very fast.
#. *Batik*—Batik renderer (as it uses the Batik SVG Framework). It has full support for SLD styling, but is slower.

**Enable Anti-aliasing**
Anti-aliasing is a technique for making edges appear smoother by filling in the edges of an object with pixels that are between the object's color and the background color. Anti-aliasing creates the illusion of smoother lines and smoother selections. Turning on anti-aliasing will generally make maps look nicer, but will increase the size of the images, and will take longer to return. If you are overlaying the anti-aliased map on top of others, beware of using transparencies as the anti-aliasing process mixes with the colors behind and can create a "halo" effect.


Advanced projection handling and map wrapping
---------------------------------------------

Advanced projection handling is a set of extra "smarts" applied while rendering that help getting
a good looking map despite the data touching or crossing "difficult areas" in selected map
projection. This includes, among others:

* Cutting the geometries so that they fit within the area of mathematical stability of the projection math,
  e.g., it will cut any bit at more than 45 degrees west and east from the central meridian of a
  transverse Mercator projection, or beyond 85 degrees north or south in a Mercator projection
* Make sure both "ends" of the world get queried for data when a map in polar stereographic is
  hitting an area that includes the dateline
* Ability to optionally preprocess geometries with a densify operation that allows better results when a reprojection
  operation causes a lot of deformation in the original geometry. Adding more points to the original geometry
  produces a more precise reprojected one (e.g. straight lines that become curves when reprojected).

Along with advanced projection handling there is the possibility of creating a continuous map
across the dateline, wrapping the data on the other side of the longitude range, to get a continuous
map. This is called continuous map wrapping, and it's enabled in Mercator and Equirectangular (plate carrée) projections.
This also uses an heuristic to guess direction of lines that cross the dateline (west to east or east to west). The
heuristic can be disabled using the **Disable dateline wrapping heuristic** option.

Advanced projection handling and continuous map wrapping functionalities are rather useful, and enabled by default, but the
tendency to generate multiple or-ed bounding boxes (to query both sides of the dateline) can cause extreme slowness in certain
databases (e.g. Oracle), and some users might simply not like the wrapping output, thus, it's possible to disable both functions
in the WMS UI:

.. figure:: img/services_WMS_aph.png

Continuous map wrapping is disabled if advanced projection handling is disabled.

Automatic densification can slow down rendering, so it's disabled by default, but can be enabled using the **Enable automatic
densification of geometries** option.

Advanced projection handling can also be disabled using the ``advancedProjectionHandling`` :ref:`Format Option <format_options>`.
Similarly, continuous map wrapping can also be disabled using the ``mapWrapping`` :ref:`Format Option <format_options>`, 
automatic densification can be enabled using the ``advancedProjectionHandlingDensification`` :ref:`Format Option <format_options>`,
and the dateline heuristic can be disabled using the ``disableDatelineWrappingHeuristic`` :ref:`Format Option <format_options>`.


Restricting MIME types for GetMap and GetFeatureInfo requests
-------------------------------------------------------------

GeoServer supports restricting formats for WMS GetMap and WMS GetFeatureInfo requests. The default is to allow all MIME types for both kinds of request.

.. figure:: img/service_WMS_allMimeTypesAllowed.png

The following figure shows an example for MIME type restriction. The MIME types
**image/png** and **text/html;subtype=openlayers** are allowed for GetMap requests, the MIME types **text/html** and **text/plain** are allowed for
GetFeatureInfo requests. A GetMap/GetFeatureInfo request with a MIME type not allowed will result in a service exception reporting the error.

.. figure:: img/service_WMS_allowedMimeTypes.png

.. note:: Activating MIME type restriction and not allowing at least one MIME type disables the particular request.

Disabling usage of dynamic styling in GetMap and GetFeatureInfo requests
------------------------------------------------------------------------

Dynamic styles can be applied to layers in GetMap and GetFeatureInfo requests using the SLD or SLD_BODY parameters for GET requests.

In addition, GetMap POST requests can contain inline style definition for layers.

The usage of dynamic styling can be restricted on a global or per virtual service basis using the **Dynamic styling** section.

.. figure:: img/service_WMS_disableDynamicStyling.png

When the flag is checked, a GetMap/GetFeatureInfo request with a dynamic style will result in a service exception reporting the error.

Disabling GetFeatureInfo requests results reprojection
------------------------------------------------------

By default GetFeatureInfo results are reproject to the map coordinate reference system. This behavior can be deactivated on a global or per virtual service basis in the **GetFeatureInfo results reprojection** section.

.. figure:: img/service_WMS_disableFeaturesReprojection.png

When the flag is checked, GetFeatureInfo requests results will not be reprojected and will instead used the layer coordinate reference system.
