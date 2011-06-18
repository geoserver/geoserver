.. _wfs_reference: 

WFS reference
============= 

Introduction
------------ 

The `Web Feature Service <http://www.opengeospatial.org/standards/wfs>`_ (WFS) is a standard created by the OGC that refers to the sending and receiving of geospatial data through HTTP. WFS encodes and transfers information in Geography Markup Language, a subset of XML. The current version of WFS is 1.1.0. GeoServer supports both version 1.1.0 (the default since GeoServer 1.6.0) and version 1.0.0. There are differences between these two formats, some more subtle than others, and this will be noted where differences arise. The current version of WFS is 1.1. WFS version 1.0 is still used in places, and we will note where there are differences. However, the syntax will often remain the same. 

An important distinction must be made between WFS and :ref:`wms`, which refers to the sending and receiving of geographic 
information after it has been rendered as a digital image. 

Benefits of WFS
--------------- 

One can think of WFS as the "source code" to the maps that one would 
ordinarily view (via WMS). WFS leads to greater transparency and 
openness in mapping applications. Instead of merely being able to look 
at a picture of the map, as the provider wants the user to see, the 
power is in the hands of the user to determine how to visualize (style) 
the raw geographic and attribute data. The data can also be downloaded, 
further analyzed, and combined with other data. The transactional 
capabilities of WFS allow for collaborative mapping applications. In 
short, WFS is what enables open spatial data. 

Operations
---------- 

WFS can perform the following operations: 

.. list-table::
   :widths: 20 80

   * - **Operation**
     - **Description**
   * - ``GetCapabilities``
     - Retrieves a list of the server's data, as well as valid WFS operations and parameters
   * - ``DescribeFeatureType``
     - Retrieves information and attributes about a particular dataset
   * - ``GetFeature``
     - Retrieves the actual data, including geometry and attribute values
   * - ``LockFeature``
     - Prevents a feature type from being edited
   * - ``Transaction`` 
     - Edits existing featuretypes by creating, updating, and deleting. 
   * - ``GetGMLObject`` 
     - *(Version 1.1.0 only)* - Retrieves element instances by traversing XLinks that refer to their XML IDs.

A WFS server that supports **transactions** is sometimes known as a WFS-T.  **GeoServer fully supports transactions.**

.. _wfs_getcap:

GetCapabilities
---------------


The **GetCapabilities** operation is a request to a WFS server for a list of what operations and services ("capabilities") are being offered by that server. 

A typical GetCapabilities request would look like this (at URL ``http://www.example.com/wfs``):

Using a GET request (standard HTTP):

.. code-block:: xml
 
   http://www.example.com/wfs?
   service=wfs&
   version=1.1.0&
   request=GetCapabilities
	  
The equivalent using POST:
	
.. code-block:: xml 

   <GetCapabilities
   service="WFS"
   xmlns="http://www.opengis.net/wfs"
   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
   xsi:schemaLocation="http://www.opengis.net/wfs 			
   http://schemas.opengis.net/wfs/1.1.0/wfs.xsd"/>
	
GET requests are simplest to decode, so we will discuss them in detail, but the POST requests are analogous.  (The actual requests would be all on one line, with no line breaks, but our convention here is to break lines for clarity.)  Here there are three parameters being passed to our WFS server, ``service=wfs``, ``version=1.1.0``, and ``request=GetCapabilities``.  At a bare minimum, it is required that a WFS request have these three parameters (service, version, and request).  GeoServer relaxes these requirements (setting the default version if omitted), but "officially" they are mandatory, so they should always be included.  The *service* key tells the WFS server that a WFS request is forthcoming.  The *version* key refers to which version of WFS is being requested.  Note that there are only two version numbers officially supported:  "1.0.0" and "1.1.0".  Supplying a value like "1" or "1.1" will likely return an error.  The *request* key is where the actual GetCapabilities operation is specified.

The Capabilities document that is returned is a long and complex chunk of XML, but very important, and so it is worth taking a closer look.  (The 1.0.0 Capabilities document is very different from the 1.1.0 document discussed here, so beware.)  There are five main components we will be discussing (other components are beyond the scope of this document.):

.. list-table::
   :widths: 20 80
   
   * - **ServiceIdentification**
     - This section contains basic "header" information such as the Name and ServiceType.  The ServiceType mentions which version(s) of WFS are supported.
   * - **ServiceProvider**
     - This section provides contact information about the company behind the WFS server, including telephone, website, and email.
   * - **OperationsMetadata**
     - This section describes the operations that the WFS server recognizes and the parameters for each operation.  A WFS server can be set up not to respond to all aforementioned operations.
   * - **FeatureTypeList**
     - This section lists the available featuretypes.  They are listed in the form "namespace:featuretype".  Also, the default projection of the featuretype is listed here, along with the resultant bounding box for the data in that projection.
   * - **Filter_Capabilities**
     - This section lists filters available in which to request the data.  SpatialOperators (Equals, Touches), ComparisonOperators (LessThan, GreaterThan), and other functions are all listed here.  These filters are not defined in the Capabilities document, but most of them (like the ones mentioned here) are self-evident.

DescribeFeatureType
-------------------

The purpose of the **DescribeFeatureType** is to request information about an individual featuretype before requesting the actual data.  Specifically, **DescribeFeatureType** will request a list of features and attributes for the given featuretype, or list the featuretypes available.

Let's say we want a list of featuretypes.  The appropriate GET request would be:

.. code-block:: xml 

   http://www.example.com/wfs?
      service=wfs&
      version=1.1.0&
      request=DescribeFeatureType

Note again the three required fields (``service``, ``version``, and ``request``).  This will return the list of featuretypes, sorted by namespace.

If we wanted information about a specific featuretype, the GET request would be:

.. code-block:: xml 

   http://www.example.com/wfs?
      service=wfs&
      version=1.1.0&
      request=DescribeFeatureType&
      typeName=namespace:featuretype

The only difference between the two requests is the addition of ``typeName=namespace:featuretype``, where ``featuretype`` is the name of the featuretype and ``namespace`` is the name of the namespace that featuretype is contained in.

.. _wfs_getfeature:

GetFeature
----------

The **GetFeature** operation requests the actual spatial data.  This is the "source code" spoken about previously.  More so than the other operations, it is complex and powerful.  Obviously, not all of its abilities will be discussed here.

The simplest way to run a **GetFeature** command is without any arguments.

.. code-block:: xml 

   http://www.example.com/wfs?
      service=wfs&
      version=1.1.0&
      request=GetFeature&
      typeName=namespace:featuretype

This syntax should be familiar from previous examples.  The only difference is the ``request=GetFeature.``

It is not recommended to run this command in a web browser, as this will return the geometries for all features in a featuretype.  This can be a great deal of data.  One way to limit the output is to specify a feature.  In this case, the GET request would be:

.. code-block:: xml 

   http://www.example.com/wfs?
      service=wfs&
      version=1.1.0&
      request=GetFeature&
      typeName=namespace:featuretype&
      featureID=feature

Here there is the additional parameter of ``featureID=feature.``  Replace ``feature`` with the ID of the feature you wish to retrieve.

If the name of the feature is unknown, or if you wish to limit the amount of features returned, there is the ``maxFeatures`` parameter.

.. code-block:: xml 

   http://www.example.com/wfs?
      service=wfs&
      version=1.1.0&
      request=GetFeature&
      typeName=namespace:featuretype&
      maxFeatures=N

In the above example, ``N`` is the number of features to return.

A question that may arise at this point is how the WFS server knows which N Features to return.  The bad news is that it depends on the internal structure of the data, which may not be arranged in a very helpful way.  The good news is that it is possible to sort the features based on an attribute, via the following syntax.  (This is new as of 1.1.0.)

.. code-block:: xml

   http://www.example.com/wfs?
      service=wfs&
      version=1.1.0&
      request=GetFeature&
      typeName=namespace:featuretype&
      maxFeatures=N&
      sortBy=property

In the above example, ``sortBy=property`` determines the sort.  Replace ``property`` with the attribute you wish to sort by.  The default is to sort ascending.  Some WFS servers require sort order to be specified, even if ascending.  If so, append a ``+A`` to the request.  To sort descending, add a ``+D`` to the request, like so:

.. code-block:: xml

   http://www.example.com/wfs?
      service=wfs&
      version=1.1.0&
      request=GetFeature&
      typeName=namespace:featuretype&
      maxFeatures=N&
      sortBy=property+D

It is not necessary to to use ``sortBy`` with ``maxFeatures``, but they can often complement each other.

To narrow the search not by feature, but instead by an attribute, use the ``propertyName`` key in the form ``propertyName=property.``  You can specify a single property, or multiple properties separated by commas.  For a single property from all features, use the following:

.. code-block:: xml

   http://www.example.com/wfs?
      service=wfs&
      version=1.1.0&
      request=GetFeature&
      typeName=namespace:featuretype&
      propertyName=property

For a single property from just one feature:

.. code-block:: xml

   http://www.example.com/wfs?
      service=wfs&
      version=1.1.0&
      request=GetFeature&
      typeName=namespace:featuretype&
      featureID=feature&
      propertyName=property

Or more than one property from a feature:

.. code-block:: xml

   http://www.example.com/wfs?
      service=wfs&
      version=1.1.0&
      request=GetFeature&
      typeName=namespace:featuretype&
      featureID=feature&
      propertyName=property1,property2

All of these permutations so far have centered around parameters of a non-spatial nature, but it is also possible to query for features based on geometry.  While there are very limited tools available in a GET request for spatial queries (much more are available in POST requests using filters) one of the most important can be used.  This is known as the "bounding box" or BBOX.  The BBOX allows us to ask for only such features that are contained (or partially contained) inside a box of the coordinates we specify.  The form of the bbox query is ``bbox=a1,b1,a2,b2``where ``a``, ``b``, ``c``, and ``d`` refer to coordinates.

Notice that the syntax wasn't ``bbox=x1,y1,x2,y2`` or ``bbox=y1,x1,y2,x1``.  The reason the coordinate-free ``a,b`` syntax was used above is because the order depends on the coordinate system used.  To specify the coordinate system, append ``srsName=CRS`` to the WFS request, where ``CRS`` is the coordinate reference system.  As for which corners of the bounding box to specify (bottom left / top right or bottom right / top left), that appears to not matter, as long as the bottom is first.  So the full request for returning features based on bounding box would look like this:  

.. code-block:: xml

   http://www.example.com/wfs?
      service=wfs&
      version=1.1.0&
      request=GetFeature&
      typeName=namespace:featuretype&
      bbox=a1,b1,a2,b2



Transaction
-----------

The **Transaction** operation performs edits of actual data that is exposed by the WFS.  A transaction can add, modify and remove features.  Each transaction consists of zero or more Insert, Update and Delete elements.  Each element is performed in order.  In GeoServer every transaction is 'atomic', meaning that if any of the elements fails then the data is left unchanged.

More information on the syntax of transactions can be found in the WFS specification, and in the GeoServer sample requests.  

LockFeature
-----------

The **LockFeature** operation is theoretically useful in conjunction with transactions, so users can 'lock' an area of the map that they are editing, to ensure that other users don't edit it.  In practice no widely used clients support the LockFeature operation.  

GetGMLObject
------------

**GetGMLObject** is another operation that is little used in practical client applications.  It only really makes sense in situations that require :ref:`app-schema.complex-features`.  It allows clients to extract just a portion of the nested properties.  
