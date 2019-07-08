.. _wfs_reference: 

WFS reference
=============

The `Web Feature Service <http://www.opengeospatial.org/standards/wfs>`_ (WFS) is a standard created by the Open Geospatial Consortium (OGC) for creating, modifying and exchanging vector format geographic information on the Internet using HTTP. A WFS encodes and transfers information in Geography Markup Language (GML), a subset of XML. 

The current version of WFS is **2.0.0**. GeoServer supports versions 2.0.0, 1.1.0, and 1.0.0. Although there are some important differences between the versions, the request syntax often remains the same.

A related OGC specification, the :ref:`wms`, defines the standard for exchanging geographic information in digital image format.

Benefits of WFS
---------------

The WFS standard defines the framework for providing access to, and supporting transactions on, discrete geographic features in a manner that is independent of the underlying data source. Through a combination of discovery, query, locking, and transaction operations, users have access to the source spatial and attribute data in a manner that allows them to interrogate, style, edit (create, update, and delete), and download individual features. The transactional capabilities of WFS also support the development and deployment of collaborative mapping applications. 

Operations
----------

All versions of WFS support these operations: 

.. list-table::
   :widths: 20 80
   :header-rows: 1

   * - Operation
     - Description
   * - ``GetCapabilities``
     - Generates a metadata document describing a WFS service provided by server  as well as valid WFS operations and parameters
   * - ``DescribeFeatureType``
     - Returns a description of feature types supported by a WFS service 
   * - ``GetFeature``
     - Returns a selection of features from a data source including geometry and attribute values
   * - ``LockFeature``
     - Prevents a feature from being edited through a persistent feature lock
   * - ``Transaction`` 
     - Edits existing feature types by creating, updating, and deleting 

The following operations are available in **version 2.0.0 only**:

.. list-table::
   :widths: 20 80
   :header-rows: 1

   * - Operation
     - Description
   * - ``GetPropertyValue``
     - Retrieves the value of a feature property or part of the value of a complex feature property from the data store for a set of features identified using a query expression
   * - ``GetFeatureWithLock``
     - Returns a selection of features and also applies a lock on those features
   * - ``CreateStoredQuery``
     - Create a stored query on the WFS server
   * - ``DropStoredQuery``
     - Deletes a stored query from the WFS server
   * - ``ListStoredQueries``
     - Returns a list of the stored queries on a WFS server
   * - ``DescribeStoredQueries``
     - Returns a metadata document describing the stored queries on a WFS server

The following operations are available in **version 1.1.0 only**:

.. list-table::
   :widths: 20 80
   :header-rows: 1

   * - Operation
     - Description
   * - ``GetGMLObject``
     - Retrieves features and elements by ID from a WFS 

.. note:: In the examples that follow, the fictional URL ``http://example.com/geoserver/wfs`` is used for illustration. To test the examples, substitute the address of a valid WFS. Also, although the request would normally be defined on one line with no breaks, breaks are added for clarity in the examples provided. 

Exceptions
----------

WFS also supports a number of formats for reporting exceptions. The supported values for exception reporting are:

.. list-table::
   :widths: 15 35 50
   :header-rows: 1
   
   * - Format
     - Syntax
     - Description
   * - XML
     - ``exceptions=text/xml``
     - *(default)* XML output
   * - JSON
     - ``exceptions=application/json``
     - Simple JSON
   * - JSONP
     - ``exceptions=text/javascript``
     - Return a JsonP in the form: parseResponse(...jsonp...). See :ref:`wms_vendor_parameters` to change the callback name. Note that this format is disabled by default (See :ref:`wms_global_variables`).
.. _wfs_getcap:

GetCapabilities
~~~~~~~~~~~~~~~

The **GetCapabilities** operation is a request to a WFS server for a list of the operations and services, or *capabilities*, supported by that server.

To issue a GET request using HTTP::

   http://example.com/geoserver/wfs?
     service=wfs&
     version=1.1.0&
     request=GetCapabilities
	  
The equivalent request using POST:
	
.. code-block:: xml 

   <GetCapabilities
    service="WFS"
    xmlns="http://www.opengis.net/wfs"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.opengis.net/wfs 			
    http://schemas.opengis.net/wfs/1.1.0/wfs.xsd"/>
	
GET requests are simplest to decode, but the POST requests are equivalent. 

The parameters for GetCapabilities are:

.. list-table::
   :widths: 20 20 60
   :header-rows: 1
   
   * - Parameter
     - Required?
     - Description

   * - ``service``
     - Yes
     - Service name—Value is ``WFS``  
   * - ``version``
     - Yes
     - Service version—Value is the current version number. The full version number must be supplied ("1.1.0", "1.0.0"), not the abbreviated form ("1" or "1.1").
   * - ``request``
     - Yes
     - Operation name—Value is ``GetCapabilities``

Although all of the above parameters are technically required as per the specification, GeoServer will provide default values if any parameters are omitted from a request.

The GetCapabilities response is a lengthy XML document, the format of which is different for each of the supported versions. There are five main components in a GetCapabilities document:

.. list-table::
   :widths: 20 80
   :header-rows: 1

   * - Component
     - Description
   * - ``ServiceIdentification``
     - Contains basic header information for the request such as the ``Title`` and ``ServiceType``. The ``ServiceType`` indicates which version(s) of WFS are supported.
   * - ``ServiceProvider``
     - Provides contact information about the company publishing the WFS service, including telephone, website, and email.
   * - ``OperationsMetadata``
     - Describes the operations that the WFS server supports and the parameters for each operation. A WFS server may be configured not to respond to the operations listed above.
   * - ``FeatureTypeList``
     - Lists the feature types published by a WFS server. Feature types are listed in the form ``namespace:featuretype``. The default projection of the feature type is also listed, along with the bounding box for the data in the stated projection.
   * - ``Filter_Capabilities``
     - Lists the filters, or expressions, that are available to form query predicates, for example, ``SpatialOperators`` (such as ``Equals``, ``Touches``) and ``ComparisonOperators`` (such as ``LessThan``, ``GreaterThan``). The filters themselves are not included in the GetCapabilities document.

.. _wfs_dft:

DescribeFeatureType
~~~~~~~~~~~~~~~~~~~

**DescribeFeatureType** requests information about an individual feature type before requesting the actual data. Specifically, the operation will request a list of features and attributes for the given feature type, or list the feature types available.

The parameters for DescribeFeatureType are:

.. list-table::
   :widths: 20 20 60
   :header-rows: 1
   
   * - Parameter
     - Required?
     - Description
   * - ``service``
     - Yes
     - Service name—Value is ``WFS``
   * - ``version``
     - Yes
     - Service version—Value is the current version number
   * - ``request``
     - Yes
     - Operation name—Value is ``DescribeFeatureType``
   * - ``typeNames``
     - Yes
     - Name of the feature type to describe (``typeName`` for WFS 1.1.0 and earlier)
   * - ``exceptions``
     - No
     - Format for reporting exceptions—default value is ``application/vnd.ogc.se_xml``
   * - ``outputFormat``
     - No
     - Defines the scheme description language used to describe feature types

To return a list of feature types, the GET request would be as follows. This request will return the list of feature types, sorted by namespace::

   http://example.com/geoserver/wfs?
     service=wfs&
     version=2.0.0&
     request=DescribeFeatureType

To list information about a specific feature type called ``namespace:featuretype``, the GET request would be::

   http://example.com/geoserver/wfs?
     service=wfs&
     version=2.0.0&
     request=DescribeFeatureType&
     typeNames=namespace:featuretype

.. _wfs_getfeature:

GetFeature
~~~~~~~~~~

The **GetFeature** operation returns a selection of features from the data source. 

This request will execute a GetFeature request for a given layer ``namespace:featuretype``::

   http://example.com/geoserver/wfs?
     service=wfs&
     version=2.0.0&
     request=GetFeature&
     typeNames=namespace:featuretype

Executing this command will return the geometries for all features in given a feature type, potentially a large amount of data. To limit the output, you can restrict the GetFeature request to a single feature by including an additional parameter, ``featureID`` and providing the ID of a specific feature. In this case, the GET request would be::

   http://example.com/geoserver/wfs?
     service=wfs&
     version=2.0.0&
     request=GetFeature&
     typeNames=namespace:featuretype&
     featureID=feature

If the ID of the feature is unknown but you still want to limit the amount of features returned, use the ``count`` parameter for WFS 2.0.0 or the ``maxFeatures`` parameter for earlier WFS versions. In the examples below, ``N`` represents the number of features to return::

   http://example.com/geoserver/wfs?
     service=wfs&
     version=2.0.0&
     request=GetFeature&
     typeNames=namespace:featuretype&
     count=N

:: 

   http://example.com/geoserver/wfs?
     service=wfs&
     version=1.1.0&
     request=GetFeature&
     typeName=namespace:featuretype&
     maxFeatures=N

Exactly which N features will be returned depends in the internal structure of the data. However, you can sort the returned selection based on an attribute value. In the following example, an attribute is included in the request using the ``sortBy=attribute`` parameter (replace ``attribute`` with the attribute you wish to sort by)::

   http://example.com/geoserver/wfs?
      service=wfs&
      version=2.0.0&
      request=GetFeature&
      typeNames=namespace:featuretype&
      count=N&
      sortBy=attribute

The default sort operation is to sort in ascending order. Some WFS servers require the sort order to be specified, even if an ascending order sort if required. In this case, append a ``+A`` to the request. Conversely, add a ``+D`` to the request to sort in descending order as follows::

   http://example.com/geoserver/wfs?
     service=wfs&
     version=2.0.0&
     request=GetFeature&
     typeNames=namespace:featuretype&
     count=N&
     sortBy=attribute+D

There is no obligation to use ``sortBy`` with ``count`` in a GetFeature request, but they can be used together to manage the returned selection of features more effectively.

To restrict a GetFeature request by attribute rather than feature, use the ``propertyName`` key in the form ``propertyName=attribute``. You can specify a single attribute, or multiple attributes separated by commas. To search for a single attribute in all features, the following request would be required::

   http://example.com/geoserver/wfs?
     service=wfs&
     version=2.0.0&
     request=GetFeature&
     typeNames=namespace:featuretype&
     propertyName=attribute

For a single property from just one feature, use both ``featureID`` and ``propertyName``::

   http://example.com/geoserver/wfs?
     service=wfs&
     version=2.0.0&
     request=GetFeature&
     typeNames=namespace:featuretype&
     featureID=feature&
     propertyName=attribute

For more than one property from a single feature, use a comma-separated list of values for ``propertyName``::

   http://example.com/geoserver/wfs?
     service=wfs&
     version=2.0.0&
     request=GetFeature&
     typeNames=namespace:featuretype&
     featureID=feature&
     propertyName=attribute1,attribute2

While the above permutations for a GetFeature request focused on non-spatial parameters, it is also possible to query for features based on geometry. While there are limited options available in a GET request for spatial queries (more are available in POST requests using filters), filtering by bounding box (BBOX) is supported.

The BBOX parameter allows you to search for features that are contained (or partially contained) inside a box of user-defined coordinates. The format of the BBOX parameter is ``bbox=a1,b1,a2,b2,[crs]`` where ``a1``, ``b1``, ``a2``, and ``b2`` represent the coordinate values. The optional ``crs`` parameter is used to name the CRS for the bbox coordinates (if they are different to the featureTypes native CRS.) The order of coordinates passed to the BBOX parameter depends on the coordinate system used
(this is why the coordinate syntax isn't represented with ``x`` or ``y``.)

To specify the coordinate system for the returned features, append ``srsName=CRS`` to the WFS request, where ``CRS`` is the Coordinate Reference System you wish to use.

As for which corners of the bounding box to specify, the only requirement is for a bottom corner (left or right) to be provided first. For example, bottom left and top right, or bottom right and top left.

An example request returning features based on a bounding box (using the featureTypes native CRS)::

   http://example.com/geoserver/wfs?
     service=wfs&
     version=2.0.0&
     request=GetFeature&
     typeNames=namespace:featuretype&
     srsName=CRS
     bbox=a1,b1,a2,b2

To request features using a bounding box with CRS different from featureTypes native CRS::

   http://example.com/geoserver/wfs?
     service=wfs&
     version=2.0.0&
     request=GetFeature&
     typeNames=namespace:featuretype&
     srsName=CRS
     bbox=a1,b1,a2,b2,CRS

LockFeature
~~~~~~~~~~~

A **LockFeature** operation provides a long-term feature locking mechanism to ensure consistency in edit transactions. If one client fetches a feature and makes some changes before submitting it back to the WFS, locks prevent other clients from making any changes to the same feature, ensuring a transaction that can be serialized. If a WFS server supports this operation, it will be reported in the server's GetCapabilities response.

In practice, few clients support this operation.


.. _wfs_wfst:

Transaction
~~~~~~~~~~~

The **Transaction** operation can create, modify, and delete features published by a WFS. Each transaction will consist of zero or more Insert, Update, and Delete elements, with each transaction element performed in order. Every GeoServer transaction is *atomic*, meaning that if any of the elements fail, the transaction is abandoned, and the data is unaltered. A WFS server that supports **transactions** is sometimes known as a WFS-T server. **GeoServer fully supports transactions.** 

More information on the syntax of transactions can be found in the `WFS specification <http://www.opengeospatial.org/standards/wfs>`_ and in the :ref:`GeoServer sample requests <demos>`.


GetGMLObject
~~~~~~~~~~~~

.. note:: This operation is valid for **WFS version 1.1.0 only**.

A **GetGMLObject** operation accepts the identifier of a GML object (feature or geometry) and returns that object. This operation is relevant only in situations that require :ref:`app-schema.complex-features` by allowing clients to extract just a portion of the nested properties of a complex feature. As a result, this operation is not widely used by client applications.


GetPropertyValue
~~~~~~~~~~~~~~~~

.. note:: This operation is valid for **WFS version 2.0.0 only**.

A **GetPropertyValue** operation retrieves the value of a feature property, or part of the value of a complex feature property, from a data source for a given set of features identified by a query.

This example retrieves the geographic content only of the features in the ``topp:states`` layer::

  http://example.com/geoserver/wfs?
    service=wfs&
    version=2.0.0&
    request=GetPropertyValue&
    typeNames=topp:states&
    valueReference=the_geom

The same example in a POST request:

.. code-block:: xml

   <wfs:GetPropertyValue service='WFS' version='2.0.0'
    xmlns:topp='http://www.openplans.org/topp'
    xmlns:fes='http://www.opengis.net/fes/2.0'
    xmlns:wfs='http://www.opengis.net/wfs/2.0'
    valueReference='the_geom'>
     <wfs:Query typeNames='topp:states'/>
   </wfs:GetPropertyValue>

To retrieve value for a different attribute, alter the ``valueReference`` parameter.


GetFeatureWithLock
~~~~~~~~~~~~~~~~~~

.. note:: This operation is valid for **WFS version 2.0.0 only**.

A **GetFeatureWithLock** operation is similar to a **GetFeature** operation, except that when the set of features are returned from the WFS server, the features are also locked in anticipation of a subsequent transaction operation.

This POST example retrieves the features of the ``topp:states`` layer, but in addition locks those features for five minutes.

.. code-block:: xml

   <wfs:GetFeatureWithLock service='WFS' version='2.0.0'
    handle='GetFeatureWithLock-tc1' expiry='5' resultType='results'
    xmlns:topp='http://www.openplans.org/topp'
    xmlns:fes='http://www.opengis.net/fes/2.0'
    xmlns:wfs='http://www.opengis.net/wfs/2.0'
    valueReference='the_geom'>
     <wfs:Query typeNames='topp:states'/>
   </wfs:GetFeatureWithLock>

To adjust the lock time, alter the ``expiry`` parameter.


CreateStoredQuery 
~~~~~~~~~~~~~~~~~

.. note:: This operation is valid for **WFS version 2.0.0 only**.

A **CreateStoredQuery** operation creates a stored query on the WFS server. The definition of the stored query is encoded in the ``StoredQueryDefinition`` parameter and is given an ID for a reference.

This POST example creates a new stored query (called "myStoredQuery") that filters the ``topp:states`` layer to those features that are within a given area of interest (``${AreaOfInterest}``):

.. code-block:: xml

   <wfs:CreateStoredQuery service='WFS' version='2.0.0'
    xmlns:wfs='http://www.opengis.net/wfs/2.0'
    xmlns:fes='http://www.opengis.org/fes/2.0'
    xmlns:gml='http://www.opengis.net/gml/3.2'
    xmlns:myns='http://www.someserver.com/myns'
    xmlns:topp='http://www.openplans.org/topp'>
     <wfs:StoredQueryDefinition id='myStoredQuery'>
       <wfs:Parameter name='AreaOfInterest' type='gml:Polygon'/>
       <wfs:QueryExpressionText
        returnFeatureTypes='topp:states'
        language='urn:ogc:def:queryLanguage:OGC-WFS::WFS_QueryExpression'
        isPrivate='false'>
         <wfs:Query typeNames='topp:states'>
           <fes:Filter>
             <fes:Within>
               <fes:ValueReference>the_geom</fes:ValueReference>
                ${AreaOfInterest}
             </fes:Within>
           </fes:Filter>
         </wfs:Query>
       </wfs:QueryExpressionText>
     </wfs:StoredQueryDefinition>
   </wfs:CreateStoredQuery>

DropStoredQuery
~~~~~~~~~~~~~~~

.. note:: This operation is valid for **WFS version 2.0.0 only**.

A **DropStoredQuery** operation drops a stored query previous created by a CreateStoredQuery operation. The request accepts the ID of the query to drop.

This example will drop a stored query with an ID of ``myStoredQuery``::

  http://example.com/geoserver/wfs?
    request=DropStoredQuery&
    storedQuery_Id=myStoredQuery

The same example in a POST request:

.. code-block:: xml

  <wfs:DropStoredQuery
   xmlns:wfs='http://www.opengis.net/wfs/2.0'
   service='WFS' id='myStoredQuery'/>


ListStoredQueries
~~~~~~~~~~~~~~~~~

.. note:: This operation is valid for **WFS version 2.0.0 only**.

A **ListStoredQueries** operation returns a list of the stored queries currently maintained by the WFS server.

This example lists all stored queries on the server::

  http://example.com/geoserver/wfs?
    request=ListStoredQueries&
    service=wfs&
    version=2.0.0

The same example in a POST request:

.. code-block:: xml

   <wfs:ListStoredQueries service='WFS'
    version='2.0.0'
    xmlns:wfs='http://www.opengis.net/wfs/2.0'/>


DescribeStoredQueries
~~~~~~~~~~~~~~~~~~~~~

.. note:: This operation is valid for **WFS version 2.0.0 only**.

A **DescribeStoredQuery** operation returns detailed metadata about each stored query maintained by the WFS server. A description of an individual query may be requested by providing the ID of the specific query. If no ID is provided, all queries are described.


This example describes the existing stored query with an ID of ``urn:ogc:def:query:OGC-WFS::GetFeatureById``::

  http://example.com/geoserver/wfs?
    request=DescribeStoredQueries&
    storedQuery_Id=urn:ogc:def:query:OGC-WFS::GetFeatureById

The same example in a POST request:

.. code-block:: xml

   <wfs:DescribeStoredQueries
    xmlns:wfs='http://www.opengis.net/wfs/2.0'
    service='WFS'>
     <wfs:StoredQueryId>urn:ogc:def:query:OGC-WFS::GetFeatureById</wfs:StoredQueryId>
   </wfs:DescribeStoredQueries>

