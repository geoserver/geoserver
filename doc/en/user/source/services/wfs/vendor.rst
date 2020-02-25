.. _wfs_vendor_parameters:

WFS vendor parameters
=====================

WFS vendor parameters are non-standard request parameters defined by an implementation to provide enhanced capabilities. GeoServer supports a variety of vendor-specific WFS parameters.

CQL filters
-----------

In WFS :ref:`wfs_getfeature` GET requests, the ``cql_filter`` parameter can be used to specify a filter in ECQL (Extended Common Query Language) format. ECQL provides a more compact and readable syntax compared to OGC XML filters. 

For full details see the :ref:`filter_ecql_reference` and :ref:`cql_tutorial` tutorial.

The following example illustrates a GET request OGC filter:

:: 

   filter=%3CFilter%20xmlns:gml=%22http://www.opengis.net/gml%22%3E%3CIntersects%3E%3CPropertyName%3Ethe_geom%3C/PropertyName%3E%3Cgml:Point%20srsName=%224326%22%3E%3Cgml:coordinates%3E-74.817265,40.5296504%3C/gml:coordinates%3E%3C/gml:Point%3E%3C/Intersects%3E%3C/Filter%3E

Using ECQL, the identical filter would be defined as follows:

::

   cql_filter=INTERSECTS(the_geom,%20POINT%20(-74.817265%2040.5296504))


Format options
--------------

The ``format_options`` parameter is a container for other parameters that are format-specific. The syntax is::
  
    format_options=param1:value1;param2:value2;...
    
The supported format option is:

* ``callback`` (default is ``parseResponse``)—Specifies the callback function name for the JSONP response format
* ``id_policy`` (default is ``true``)- Specifies id generation for the JSON output format. To include feature id in output use an attribute name, or use ``true`` for feature id generation. To avoid the use of feature id completely use ``false``.

Reprojection
------------

As WFS 1.1.0 and 2.0.0 both support data reprojection, GeoServer can store the data in one projection and return GML in another projection. While not part of the specification, GeoServer supports this using WFS 1.0.0 as well. When submitting a WFS :ref:`wfs_getfeature` GET request, you can add this parameter to specify the reprojection SRS as follows:

::

  srsName=<srsName>
  
The code for the projection is represented by ``<srsName>``, for example ``EPSG:4326``. For POST requests, you can add the same code to the ``Query`` element.


XML request validation
----------------------

GeoServer is less strict than the WFS specification when it comes to the validity of an XML request. To force incoming XML requests to be valid, use the following parameter:

::

  strict=[true|false]
   
The default option for this parameter is ``false``.

For example, the following request is invalid: 

.. code-block:: xml

   <wfs:GetFeature service="WFS" version="1.0.0"
    xmlns:wfs="http://www.opengis.net/wfs">
     <Query typeName="topp:states"/>
   </wfs:GetFeature>

The request is invalid for two reasons:

* The ``Query`` element should be prefixed with ``wfs:``.
* The namespace prefix has not been mapped to a namespace URI.

That said, the request would still be processed by default. Executing the above command with the ``strict=true`` parameter, however, would result in an error. The correct syntax should be:

.. code-block:: xml 

   <wfs:GetFeature service="WFS" version="1.0.0"
    xmlns:wfs="http://www.opengis.net/wfs" 
    xmlns:topp="http://www.openplans.org/topp">
     <wfs:Query typeName="topp:states"/>
   </wfs:GetFeature>


GetCapabilities namespace filter
--------------------------------

WFS :ref:`wfs_getcap` requests may be filtered to return only those layers that correspond to a particular namespace by adding the ``<namespace>`` parameter to the request.

.. note:: This parameter only affects GetCapabilities requests.

To apply this filter, add the following code to your request:

::

   namespace=<namespace>
   
Although providing an invalid namespace will not result in any errors, the GetCapabilities document returned will not contain any layer information.

.. warning:: Using this parameter may result your GetCapabilities document becoming invalid, as the WFS specification requires the document to return at least one layer.

.. note:: This filter is related to :ref:`virtual_services`.

