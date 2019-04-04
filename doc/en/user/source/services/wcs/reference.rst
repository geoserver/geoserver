.. _wcs_reference: 

WCS reference
============= 

Introduction
------------ 

The `Web Coverage Service <http://www.opengeospatial.org/standards/wcs>`_ (WCS) is a standard created by the OGC that refers to the receiving of geospatial information as 'coverages': digital geospatial information representing space-varying phenomena.  One can think of it as :ref:`wfs` for *raster* data.  It gets the 'source code' of the map, but in this case its not raw vectors but raw imagery.  

An important distinction must be made between WCS and :ref:`wms`.  They are similar, and can return similar formats, but a WCS is able to return more information, including valuable metadata and more formats.  It additionally allows more precise queries, potentially against multi-dimensional backend formats.

Benefits of WCS
--------------- 

WCS provides a standard interface for how to request the raster source of a geospatial image.  While a WMS can return an image it is generally only useful as an image.  The results of a WCS can be used for complex modeling and analysis, as it often contains more information.  It also allows more complex querying - clients can extract just the portion of the coverage that they need.

Operations
---------- 

WCS can perform the following operations: 

.. list-table::
   :widths: 20 80

   * - **Operation**
     - **Description**
   * - ``GetCapabilities``
     - Retrieves a list of the server's data, as well as valid WCS operations and parameters
   * - ``DescribeCoverage``
     - Retrieves an XML document that fully describes the request coverages.
   * - ``GetCoverage`` 
     - Returns a coverage in a well known format.  Like a WMS GetMap request, but with several extensions to support the retrieval of coverages.


.. _wCs_getcap:

.. note:: The following examples show the 1.1 protocol, the full specification for versions 1.0, 1.1 and 2.0 are available on the `OGC web site <http://www.opengeospatial.org/standards/wcs>`_

GetCapabilities
---------------


The **GetCapabilities** operation is a request to a WCS server for a list of what operations and services ("capabilities") are being offered by that server. 

A typical GetCapabilities request would look like this (at URL ``http://www.example.com/wcs``):

Using a GET request (standard HTTP)::

   http://www.example.com/wcs?
   service=wcs&
   AcceptVersions=1.1.0&
   request=GetCapabilities
	  
Here there are three parameters being passed to our WCS server, ``service=wcs``, ``AcceptVersions=1.1.0``, and ``request=GetCapabilities``.  At a bare minimum, it is required that a WCS request have the service and request parameters.  GeoServer relaxes these requirements (setting the default version if omitted), but "officially" they are mandatory, so they should always be included.  The *service* key tells the WCS server that a WCS request is forthcoming.  The *AcceptsVersion* key refers to which version of WCS is being requested.  The *request* key is where the actual GetCapabilities operation is specified.

WCS additionally supports the Sections parameter that lets a client only request a specific section of the Capabilities Document.

.. _wcs_describecoverage:

DescribeCoverage
-------------------

The purpose of the **DescribeCoverage** request is to additional information about a Coverage a client wants to query.  It returns information about the crs, the metadata, the domain, the range and the formats it is available in.  A client generally will need to issue a DescribeCoverage request before being sure it can make the proper GetCoverage request. 

.. _wcs_getcoverage:

GetCoverage
--------------

The **GetCoverage** operation requests the actual spatial data.  It can retrieve subsets of coverages, and the result can be either the coverage itself or a reference to it.  The most powerful thing about a GetCoverage request is its ability to subset domains (height and time) and ranges.  It can also do resampling, encode in different data formats, and return the resulting file in different ways.  
