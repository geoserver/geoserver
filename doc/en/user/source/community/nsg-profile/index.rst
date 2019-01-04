.. _community_nsg_profile:

NSG Profile
===========
NSG Profile introduces a new operation for WFS 2.0.2 named PageResults. This operation will allow clients to access paginated results using random positions.

The current WFS 2.0.2 OGC specification defines a basic pagination support that can been used to navigate through features responses results.

Pagination is activated when parameters count and startIndex are used in the query, for example:

   ::
   
      http://<host>/geoserver/ows?service=WFS&version=2.0.0&request=GetFeature&typeNames=topp%3Atasmania_roads&count=5&startIndex=0



In this case each page will contain five features.
The returned feature collection will have the next and previous attributes which will contain an URL that will allow clients to navigate through the results pages, i.e. previous page and next page:

   ::
   
      <wfs:FeatureCollection
         previous="http://localhost:8080/geoserver/wfs?
         REQUEST=GetFeature&
         VERSION=2.0.0&
         TYPENAMES=topp:tasmania_roads&
         SERVICE=WFS&
         COUNT=2&
         STARTINDEX=0"
         next="http://localhost:8080/geoserver/wfs?
         REQUEST=GetFeature&
         VERSION=2.0.0&
         TYPENAMES=topp:tasmania_roads&
         SERVICE=WFS&
         COUNT=2&
         STARTINDEX=4"
         numberMatched="14"
         numberReturned="2">
         

This means that this type of navigation will always be sequential, if the client is showing page two and the user wants to see page five the client will have to:

#. request page three and use the provided next URL to retrieve page four
#. request page four and use the provided next URI to retrieve page five

This is not an ideal solution to access random pages, which is common action. 
PageResults operation adds a standard way to perform request random pages directly. 

Installing the extension
------------------------

#. Download the NSG Profile extension from the nightly GeoServer community module builds.

#. Place the JARs into the ``WEB-INF/lib`` directory of the GeoServer installation.

Configure the extension
-----------------------

The root directory inside the GeoServer data directory for the nsg-profile community module is named nsg-profile and all the configurations properties are stored in a file named **configuration.properties**.

All configuration properties are changeable at runtime, which means that if a property is updated the module take it into account.

When the application starts if no configuration file exists one with the default values is created.

The GetFeature requests representations associated with an index result type is serialized and stored in the file system in a location that is configurable.

The default location, relative to the GeoServer data directory, is nsg-profie/resultSets.

The GetFeature request to resultSetID mapping is stored by default in an H2 DB in nsg-profie/resultSets folder; for details on database configuration see `GeoTools JDBCDataStore syntax <http://docs.geotools.org/stable/userguide/library/jdbc/datastore.html>`_

The configuration properties are the follows:


.. list-table::
   :widths: 20 30 50
   :header-rows: 1

   * - Name
     - Default Value
     - Description
   * - resultSets.storage.path
     - ${GEOSERVER_DATA_DIR}/nsg-profile/resultSets
     - Path where to store GetFeature requests representations 
   * - resultSets.timeToLive
     - 600
     - How long a GetFeature request should be maintained by the server (in seconds)
   * - resultSets.db.dbtype
     - h2
     - DB type used to store GetFeature request to resultSetID mapping 
   * - resultSets.db.database
     - ${GEOSERVER_DATA_DIR}/nsg-profile/db/resultSets
     - path where to store GetFeature request to resultSetID mapping
   * - resultSets.db.user
     - sa
     - database user username
   * - resultSets.db.password
     - sa
     - database user password
   * - resultSets.db.port
     - 
     - database port to connect to
   * - resultSets.db.schema
     - 
     - database schema
   * - resultSets.db.host
     - 
     - server to connect to 
     

Index Result Type
-----------------
The **index result type** extends the WFS **hits result type** by adding an extra attribute named **resultSetID** to the response. 
The **resultSetID** attribute can then be used by the **PageResults operation** to navigate randomly through the results.

A GetFeature request that uses the index result type should look like this:

   ::
      
      http://<host>/geoserver/ows?service=WFS&version=2.0.0&request=GetFeature&typeNames=topp%3Atasmania_roads&resultType=index


The response of a GetFeature operation when the index result type is used should look like this:

   ::
      
      <?xml version="1.0" encoding="UTF-8"?>
      <wfs:FeatureCollection
       numberMatched="14"
       numberReturned="0"
       resultSetID="ef35292477a011e7b5a5be2e44b06b34"
       xmlns:fes="http://www.opengis.net/fes/2.0"
       xmlns:gml="http://www.opengis.net/gml/3.2"
       xmlns:ows="http://www.opengis.net/ows/1.1"
       xmlns:wfs="http://www.opengis.net/wfs/2.0"
       xmlns:xlink="http://www.w3.org/1999/xlink"
       xmlns:xs="http://www.w3.org/2001/XMLSchema"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.opengis.net/wfs/2.0  
       http://schemas.opengis.net/wfs/2.0/wfs.xsd"/>

The **resultSetID** is an unique identifier that identifies the original request. 

Clients will use the **resultSetID** with the PageResults operation to reference the original request.

If pagination is used, the previous and next attributes should appear as in hits result type request.

PageResults Operation
---------------------

The **PageResults operation** allows clients to query random positions of an existing result set (stored GetFeature request) that was previously created using the **index result type** request. 

The available parameters are this ones:

.. list-table::
   :widths: 40 20 40
   :header-rows: 1

   * - Name
     - Mandatory
     - Default Value
   * - service
     - YES
     - WFS
   * - version
     - YES
     - 2.0.2
   * - request
     - YES
     - PageResults
   * - resultSetID
     - YES
     - 
   * - startIndex
     - NO
     - 0
   * - count
     - NO
     - 10
   * - outputFormat
     - NO
     - application/gml+xml; version=3.2
   * - resultType
     - NO
     - results
   * - timeout
     - NO
     - 300


The two parameters that are not already supported by the GetFeature operation are the **resultSetID** parameter and the **timeout** parameter. 

#. The **resultSetID** parameter should reference an existing result set (stored GetFeature request). 

   A typical PageResults request will look like this:
   
   ::
   
      http://<host>/geoserver/ows?service=WFS&version=2.0.2&request=PageResults&resultSetID=ef35292477a011e7b5a5be2e44b06b34&startIndex=5&count=10&outputFormat=application/gml+xml; version=3.2&resultType=results
      
   
   This looks like a GetFeature request where the **query expression was substituted by the resultSetID parameter**.
   
#. The **timeout** parameter is not implemented yet.

The following parameters of index request are override using the ones provided with the PageResults operation or the default values:

#. startIndex
#. count
#. outputFormat
#. resultType

and finally the GetFeature response is returned.
