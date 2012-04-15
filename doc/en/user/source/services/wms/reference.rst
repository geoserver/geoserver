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

The **GetMap** operation gets the actual map image (or other output artifact).  
A client has all the information it needs to make this request after it parses the Capabilities document.  
The core parameters allow clients to specify a bounding box, a width and a height, a spatial reference system, a format, and a style.  
Detailing all the supported parameters for a GetMap request is currently beyond the scope of this document.  
A good way to get to know the GetMap parameters is to experiment with the :ref:`tutorials_wmsreflector`.  

The output format options GeoServer supports for the  **format** parameter are documented in the :ref:`wms_output_formats` section.  

GeoServer provides a number of useful vendor-specific parameters, which are documented in the :ref:`wms_vendor_parameters` section.

.. _wms_getfeatureinfo:

GetFeatureInfo
--------------

The **GetFeatureInfo** operation requests the spatial and attribute data for a feature.  
It is similar to the WFS **GetFeature** operation, but that operation provides more flexibility in both input and output.
Since GeoServer provides a WFS we recommend using it instead of ``GetFeatureInfo`` whenever possible.  
 
The one advantage of ``GetFeatureInfo`` is that the request uses an (x,y) pixel value from a returned WMS image.  
This is easier to use for a naive client that is not able to perform the geographic referencing otherwise needed.

Geoserver supports the following output formats for ``GetFeatureInfo``:

.. list-table::
   :widths: 15 35 50
   
   * - **Format**
     - **Syntax**
     - **Notes**
   * - TEXT
     - ``info_format=text/plain``
     - Simple text output. (The default format)
   * - GML 2
     - ``info_format=application/vnd.ogc.wms`` 
     - Works only for Simple Features (see :ref:`app-schema.complex-features`)
   * - GML 3
     - ``info_format=application/vnd.ogc.wms/3.1.1``
     - Works for both Simple and Complex Features (see :ref:`app-schema.complex-features`)
   * - HTML
     - ``info_format=text/html``
     - Uses HTML templates that are defined on the server.  See :ref:`tutorials_getfeatureinfo` for information on how to template HTML output. 

Server-styled HTML is the most commonly-used format. 
However, for maximum control and customisation we suggest the client use GML3 and style the raw data itself.

.. _wms_describelayer:

DescribeLayer
-------------

The **DescribeLayer** operation is used primarily by clients that understand SLD-based WMS.  
In order to make an SLD one needs to know the structure of the data.  
WMS and WFS both have operations to do this, so the **DescribeLayer** operation just routes the client to the appropriate service.


.. _wms_getlegendgraphic:

GetLegendGraphic
----------------

The **GetLegendGraphic** operation provides a mechanism for generating legend symbols, beyond the LegendURL reference of WMS Capabilities.  
It generates a legend based on the style defined on the server, or alternatively based on a user-supplied SLD.  
For more information on this operation and the various options that GeoServer supports see :ref:`get_legend_graphic`.
