.. _config_catalog:

Catalogue API
=============

GeoServer's configuration is formed into a Catalog of available layers to be published, collected into workspaces. Each workspace contains everything needed to publish a layer including data source connection details, and any styling required for visual presentation.

The following types of information are stored:

* namespaces and workspaces
* coverage (raster) and data (vector) stores
* coverages and feature resoures
* styles

The Catalog can be acquired from GeoServer:

.. code-block:: java

   Catalog catalog = geoServer.getCatalog();

Workspaces
----------

A workspace is a container for any number of stores. All workspaces can be obtained with the
``getWorkspaces()``. A workspace is identified by its name (
WorkspaceInfo.getName()``). A workspace can be looked up by its name with the 
``getWorkspaceByName(String)`` method.

Stores
------

The ``getStores(Class)`` method provides access to all the stores in the catalog of a
specific type. For instance, the following would obtain all datstores from the catalog:

.. code-block:: java
   
   //get all datastores
   List<DataStoreInfo> dataStores = catalog.getStores( DataStoreInfo.class );

The methods ``getDataStores()`` and ``getCoverageStores()`` provide a convenience for
the two well known types.

A store is contained within a workspace (see ``StoreInfo.getWorkspace()``). The 
``Catalog.getStoresByWorkspace(WorkspaceInfo, Class)`` method for only stores contained with a specific
workspace. For instance, the following would obtain all datastores contained within a particular
workspace:

.. code-block:: java
   
   //get a workspace
   WorkspaceInfo workspace = catalog.getWorkspace( "myWorkspace" );

   //get all datastores in that workspace
   List<DataStoreInfo> dataStores = catalog.getStoresByWorkspace( workspace, DataStoreInfo.class );

Resources
---------

The ``getResources(Class)`` method provides access to all resources in the catalog of a
particular type. For instance, to acess all feature types in the catalog:

.. code-block:: java
   
   List<FeatureTypeInfo> featureTypes = catalog.getResources( FeatureTypeInfo.class );

The ``getFeatureTypes()`` and ``getCoverages()`` methods are a convenience for the well
known types.

A resource is contained within a namespace, therefore it is identified by a namespace uri,
local name pair. The ``getResourceByName(String, String, Class)`` method provides access to
a resource by its namespace qualified name. The method ``getResourceByName(String, Class)``
provides access to a resource by its unqualified name. The latter method will do an exhaustive
search of all namespaces for a resource with the specified name. If only a single resource with
the name is found it is returned. Some examples:

.. code-block:: java
   
   //get a feature type by its qualified name
   FeatureTypeInfo ft = catalog.getResourceByName(
      "http://myNamespace.org", "myFeatureType", FeatureTypeInfo.class );

   //get a feature type by its unqualified name
   ft = catalog.getResourceByName( "myFeatureType", FeatureTypeInfo.class );

   //get all feature types in a namespace
   NamespaceInfo ns = catalog.getNamespaceByURI( "http://myNamespace.org" );
   List<FeatureTypeInfo> featureTypes = catalog.getResourcesByNamespace( ns, FeatureTypeINfo.class );

Layers
------

A layer is used to publish a resource. The ``getLayers()`` method provides access to all layers
in the catalog. A layer is uniquely identified by its name. The ``getLayerByName(String)``
method provides access to a layer by its name. The ``getLayers(ResourceInfo)`` return all
the layers published from a specific resource. Some examples:

.. code-block:: java
   
   //get a layer by its name
   LayerInfo layer = catalog.getLayer( "myLayer" );

   //get all the layers for a particualr feature type
   FeatureTypeInfo ft = catalog.getFeatureType( "http://myNamespace", "myFeatureType" );
   List<LayerInfo> layers = catalog.getLayers( ft );

Modifing the Catalog
--------------------

Catalog objects such as stores and resoures are mutable and can be modified. However, any
modifications made on an object do not apply until they are saved. For instance, consider the
following example of modifying a feature type:

.. code-block:: java
   
   //get a feature type
   FeatureTypeInfo featureType = catalog.getFeatureType( "http://myNamespace.org", "myFeatureType" );

   //modify it
   featureType.setBoundingBox( new Envelope(...) );

   //save it
   catalog.save( featureType );

Isolated Workspaces
-------------------

It is possible to request a catalog object using its workspace prefix or its namespace URI, the last
method will not work to retrieve the content of an isolated workspace unless in the context of a
virtual service belonging to that workspace.