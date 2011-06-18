.. _wms_reference: 

WMS reference
============= 

Introduction
------------ 

The `Web Map Service <http://www.opengeospatial.org/standards/wms>`_ (WMS) is a standard created by the OGC that refers to the sending and receiving of georeferenced images over HTTP.  These images can be produced from both vector and raster data formats.  The most widely used version of WMS is 1.1.1, which GeoServer supports.  The Styled Layer Descriptor (SLD) standard specifies extensions to WMS to control the styling of the WMS over the web, and GeoServer supports all these additional operations as well.

An important distinction must be made between WMS and :ref:`wfs`, which refers to the sending and receiving of raw geographic information, before it has been rendered as a digital image. 

Benefits of WMS
--------------- 

WMS provides a standard interface for how to request a geospatial image.  The main benefit of this is that clients can request images from multiple servers, and then combine them in to one view for the user.  The standard guarantees that these images can all be overlaid on one another as they actually would be in reality.  Numerous servers and clients support WMS.

Operations
---------- 

WMS can perform the following operations: 

.. list-table::
   :widths: 20 80

   * - **Operation**
     - **Description**
   * - ``GetCapabilities``
     - Retrieves a list of the server's data, as well as valid WMS operations and parameters
   * - ``GetMap``
     - Retrieves the image requested by the client
   * - ``GetFeatureInfo`` (optional)
     - Retrieves the actual data, including geometry and attribute values, for a pixel location
   * - ``DescribeLayer`` (optional)
     - Indicates the WFS or WCS to retrieve additional information about the layer.
   * - ``GetLegendGraphic`` (optional)
     - General mechanism for retrieving generated legend symbols 


Additionally a  WFS server that supports **transactions** is sometimes known as a WFS-T.  **GeoServer fully supports transactions.**

.. _wms_getcap:

GetCapabilities
---------------


The **GetCapabilities** operation is a request to a WMS server for a list of what operations and services ("capabilities") are being offered by that server. 

A typical GetCapabilities request would look like this (at URL ``http://www.example.com/wms``):

Using a GET request (standard HTTP):

.. code-block:: xml
 
   http://www.example.com/wms?
   service=wms&
   version=1.1.1&
   request=GetCapabilities
	  
Here there are three parameters being passed to our WMS server, ``service=wms``, ``version=1.1.1``, and ``request=GetCapabilities``.  At a bare minimum, it is required that a WFS request have these three parameters (service, version, and request).  GeoServer relaxes these requirements (setting the default version if omitted), but "officially" they are mandatory, so they should always be included.  The *service* key tells the WMS server that a WMS request is forthcoming.  The *version* key refers to which version of WMS is being requested.  The *request* key is where the actual GetCapabilities operation is specified.

The Capabilities document that is returned is a long and complex chunk of XML, but very important, and so it is worth taking a closer look.  There are three main components we will be discussing (other components are beyond the scope of this document.):

.. list-table::
   :widths: 20 80
   
   * - **Service**
     - This section contains basic "header" information such as the Name and basic service metadata, as well as contact information about the company behind the WMS Server.
   * - **Request**
     - This section describes the operations that the WMS server recognizes and the parameters and output formats for each operation.  A WMS server can be set up not to respond to all aforementioned operations.
   * - **Layer**
     - This section lists the available projectsions and layers.  In GeoServer they are listed in the form "namespace:layer".  Each layer also includes service metadata, like title, abstract and keywords.

.. _wms_getmap:

GetMap
-------------------

The purpose of the GetMap request is to get the actual image.  A client should have all the information it needs to make such a request after it understands the Capabilities document.  Detailing all the potential parameters for a GetMap request is currently outside the scope of this document.  But the basics are letting clients specify a bounding box, a width and a height, a spatial reference system, a format, and a style.  

A great way to get to know the GetMap parameters is to experiment with the :ref:`tutorials_wmsreflector`.  The options for the **format** parameter in GeoServer can be found in the :ref:`wms_output_formats` section.  And there are a number of vendor specific parameters that GeoServer makes availabe, see :ref:`wms_output_formats`

.. _wms_getfeatureinfo:

GetFeatureInfo
--------------

The **GetFeatureInfo** operation requests the actual spatial data.  It is very similar to the WFS **GetFeature** operation, and indeed since GeoServer always provides a WFS we recommend using it whenever possible.  It provides more flexibility in both input and output.  The one advantage that GetFeatureInfo has is that it issues its request as an x,y pixel value from a returned WMS image.  So it is easier to use by a naive client that doesn't understand all the geographic referencing needed.

Geoserver supports the following output formats for GetFeatureInfo:

.. list-table::
   :widths: 15 35 50
   
   * - **Format**
     - **Syntax**
     - **Notes**
   * - TEXT
     - ``info_format=text/plain``
     - Simple text output. Default.
   * - GML 2
     - ``info_format=application/vnd.ogc.wms`` 
     - Only works on Simple Features (see :ref:`app-schema.complex-features`)
   * - GML 3
     - ``info_format=application/vnd.ogc.wms/3.1.1``
     - Works on both Simple as well as Complex Features (see :ref:`app-schema.complex-features`)
   * - HTML
     - ``info_format=text/html``
     - Uses html templates that are defined on the server side.  See the tutorial on :ref:`tutorials_getfeatureinfo` for information on how to template the html output. 

Server-side styled HTML is most commonly used, but for optimal control and better customisation we suggest the client uses GML3 and styles the raw data in the way that it wants.

.. _wms_describelayer:

DescribeLayer
-------------

The **DescribeLayer** is used primarily by clients that understand SLD based WMS.  In order to make an SLD one needs to know the structure of the data.  WMS and WFS both have good operations to do this, thankfully the **DescribeLayer** operation just routes the client to the appropriate service.


.. _wms_getlegendgraphic:

GetLegendGraphic
----------------

**GetLegendGraphic** is an operation that provides a general mechanism for acquiring legend symbols, beyond the LegendURL reference of WMS Capabilities.  It will generate a legend automatically, based on the style defined on the server, or even based on a user supplied SLD.  For more information on this operation and the various options that GeoServer supports see :ref:`get_legend_graphic`.
