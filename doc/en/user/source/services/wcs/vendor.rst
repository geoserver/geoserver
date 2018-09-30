.. _wcs_vendor_parameters:

WCS Vendor Parameters
=====================

namespace
---------

Requests to the WCS GetCapabilities operation can be filtered to only return layers corresponding to a particular namespace.

Sample code: ::

   http://example.com/geoserver/wcs?
      service=wcs&
      version=1.0.0&
      request=GetCapabilities&
      namespace=topp

Using an invalid namespace prefix will not cause any errors, but the document returned will not contain information on any layers.

cql_filter
----------

The ``cql_filter`` parameter is similar to same named WMS parameter, and allows expressing a filter using ECQL (Extended Common Query Language).
The filter is sent down into readers exposing a ``Filter`` read parameter.

For example, assume a image mosaic has a tile index with a ``cloudCover`` percentage attribute, then it's possible to mosaic only
granules with a cloud cover less than 10% using:

   cql_filter=cloudCover < 10

For full details see the :ref:`filter_ecql_reference` and :ref:`cql_tutorial` tutorial.

sortBy
------

The ``sortBy`` parameter allows to control the order of granules being mosaicked, using the same
syntax as WFS 1.0, that is:

* ``&sortBy=att1 A|D,att2 A|D, ...``

This maps to a "SORTING" read parameter that the coverage reader might expose (image mosaic exposes such parameter).

In image mosaic, this causes the first granule found in the sorting will display on top, and then the others will follow.
 
Thus, to sort a scattered mosaic of satellite images so that the most recent image shows on top, and assuming the time attribute is called ``ingestion`` in the mosaic index, the specification will be ``&sortBy=ingestion D``.
