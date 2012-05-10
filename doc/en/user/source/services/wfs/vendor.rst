.. _wfs_vendor_parameters:

WFS vendor parameters
=====================

WFS vendor parameters are non-standard request parameters 
that are defined by an implementation to provide enhanced capabilities. 
GeoServer supports a variety of vendor-specific WFS parameters.

CQL filters
-----------

In WFS :ref:`wfs_getfeature` GET requests the ``cql_filter`` parameter can be used to specify a filter in ECQL (Extended Common Query Language).  
ECQL provides a more compact and readable syntax compared to OGC XML filters.
For full details see the :ref:`filter_ecql_reference` and :ref:`cql_tutorial` tutorial.

Example
```````

A sample GET request OGC filter is::

   filter=%3CFilter%20xmlns:gml=%22http://www.opengis.net/gml%22%3E%3CIntersects%3E%3CPropertyName%3Ethe_geom%3C/PropertyName%3E%3Cgml:Point%20srsName=%224326%22%3E%3Cgml:coordinates%3E-74.817265,40.5296504%3C/gml:coordinates%3E%3C/gml:Point%3E%3C/Intersects%3E%3C/Filter%3E

Using ECQL the filter is::

   cql_filter=INTERSECT(the_geom,%20POINT%20(-74.817265%2040.5296504))


Reprojection
------------

WFS 1.1 allows the ability to reproject data (to have GeoServer store the data in one projection and return GML in another).

GeoServer supports this using WFS 1.0 as well.  When doing a WFS 1.0 :ref:`wfs_getfeature` GET request you can add this parameter to specify the reprojection SRS::

  srsName=<srsName>
  
where ``<srsName>`` is the code for the projection (such as ``EPSG:4326``).

For POST requests, you can add the same code to the ``Query`` element.


XML request validation
----------------------

By default, GeoServer is slightly more forgiving than the WFS specification requires.  To force incoming XML requests to be strictly valid, use the following parameter::

   strict=[true|false]
   
where ``false`` is the default option.

Example
```````

Consider the following POST request:

.. code-block:: xml

   <wfs:GetFeature service="WFS" version="1.0.0" xmlns:wfs="http://www.opengis.net/wfs">
      <Query typeName="topp:states"/>
   </wfs:GetFeature>

This request will be processed successfully in GeoServer, but technically this request is invalid:

* The ``Query`` element should be prefixed with ``wfs:``
* The namespace prefix has not been mapped to a namespace URI

Executing the above command with ``strict=true`` results in an error.  For the request to be processed, it must be altered:

.. code-block:: xml 

   <wfs:GetFeature service="WFS" version="1.0.0" xmlns:wfs="http://www.opengis.net/wfs" xmlns:topp="http://www.openplans.org/topp">
      <wfs:Query typeName="topp:states"/>
   </wfs:GetFeature>


GetCapabilities namespace filter
--------------------------------

WFS :ref:`wfs_getcap` requests can be filtered to only return layers corresponding to a particular namespace.  To do this, add the following code to your request::

   namespace=<namespace>
   
where ``<namespace>`` is the namespace prefix you wish to filter on.

Using an invalid namespace prefix will not cause any errors, but the document returned will contain no information on any layers.

.. note:: This only affects the capabilities document, and not any other requests. WFS requests given to other layers, even when a different namespace is specified, will still be processed.

.. warning:: Using this parameter may cause your capabilities document to become invalid (as the WFS specification requires the document to return at least one layer).

