.. _wcs_vendor_parameters:

WCS Vendor Parameters
=====================

WCS vendor parameters are non-standard request parameters 
that are defined by an implementation to provide enhanced capabilities.

.. include:: ../generic_vendor_options.txt

GetCoverage Request
-------------------

namespace
^^^^^^^^^

Requests to the WCS GetCapabilities operation can be filtered to only return layers corresponding to a particular namespace.

Sample code: ::

   http://example.com/geoserver/wcs?
      service=wcs&
      version=1.0.0&
      request=GetCapabilities&
      namespace=topp

Using an invalid namespace prefix will not cause any errors, but the document returned will not contain information on any layers.

cql_filter
^^^^^^^^^^

The ``cql_filter`` parameter is similar to same named WMS parameter, and allows expressing a filter using ECQL (Extended Common Query Language).
The filter is sent down into readers exposing a ``Filter`` read parameter.

For example, assume a image mosaic has a tile index with a ``cloudCover`` percentage attribute, then it's possible to mosaic only
granules with a cloud cover less than 10% using:

   cql_filter=cloudCover < 10

For full details see the :ref:`filter_ecql_reference` and :ref:`cql_tutorial` tutorial.

sortBy
^^^^^^

The ``sortBy`` parameter allows to control the order of granules being mosaicked, using the same
syntax as WFS 1.0, that is:

* ``&sortBy=att1 A|D,att2 A|D, ...``

This maps to a "SORTING" read parameter that the coverage reader might expose (image mosaic exposes such parameter).

In image mosaic, this causes the first granule found in the sorting will display on top, and then the others will follow.
 
Thus, to sort a scattered mosaic of satellite images so that the most recent image shows on top, and assuming the time attribute is called ``ingestion`` in the mosaic index, the specification will be ``&sortBy=ingestion D``.

clip
^^^^

The ``clip`` parameter can be used to clip WCS responses using a Polygon/Multipolygon mask represented by a valid WKT String.

Here are two examples, the first one using WKT, the second using EWKT::

   clip=POLYGON((-14.50804652396198 55.579454354599356,34.53492222603802 55.579454354599356,34.53492222603802 32.400173313532584,-14.50804652396198 32.400173313532584,-14.50804652396198 55.579454354599356))
   clip=srid=900913;POLYGON ((-1615028.3514525702 7475148.401208023, 3844409.956787858 7475148.401208023, 3844409.956787858 3815954.983140064, -1615028.3514525702 3815954.983140064, -1615028.3514525702 7475148.401208023))

When the WKT syntax is used, the default SRS matches the output coverage CRS (so it accounts for
eventual reprojection in the request).

.. note::
  The Axis order of WKT must be East/North regardless of WCS version.
