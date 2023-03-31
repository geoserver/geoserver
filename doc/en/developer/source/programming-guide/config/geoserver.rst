.. _config_geoserver:

GeoServer
=========

Configuration is managed by a top-level ``GeoServer`` object providing ``save()`` and ``reload()`` functionality.

The ``GeoServer`` interface is configured as a spring bean with access to:

* :ref:`config_catalog`
* :ref:`config_resource`
* :ref:`config_geoserver_info`
* :ref:`config_settings`

Access to ``GeoServer`` configuration is available to spring beans during application startup and shutdown:

.. code-block:: java
   
   public Bean implements DisposableBean {} 
   
       GeoServer gs;
   
       public Bean(GeoServer gs){
          this.gs = gs;
       }
       public void destroy(){
          this.gs = null;
       }
   }

``GeoServer`` can be looked up once the application is running:

.. code-block:: java

   GeoServer gs = GeoServerExtensions.bean(GeoServer.class);


Also available for use via wicket (via ``GeoServerApplication`` web application):

.. code-block:: java
   
   GeoServer gs = page.getGeoServerApplication().getGeoServer();

.. _config_info:

Info
----

All Configuration ``Info`` objects have an ``getId()`` identifier used during persistance.

.. _config_geoserver_info:

GeoServerInfo
-------------

Manages some of the global configuration options:

* ``JAIInfo`` image processing settings
* ``CoverageAccessInfo`` image access settings
* Admin username and password
* resourceErrorHandling: Policy for handling misconfigured layers
* updateSequence: Used via WMS protocols to communicate configuration changes to clients
* featureTypeCacheSize
* globalServices
* xmlExternalEntitiesEnabled
* lockProviderName
* metadata: generic metadata map available to store additional settings
* clientProperties: transient information
* webUIMode: how to handle redirection
* allowStoredQueriesPerWorkspace

Both ``clientProperties`` and ``metadata`` can be used to communicate between modules and are intended to experiment with ideas during development. Once established this information can  be recoded as ``GeoServerInfo`` property. Settings such as ``webUIMode`` and ``allowStoredQueriesPerWorkspace`` are examples of this progression.

.. _config_settings:

SettingsInfo
------------

Manages the remaining global configuration settings:

* title
* contact: contact information, used in service description
* charset
* numDecimals
* onlineResource: website used for contact information or service provider details. This setting is available as default if a web service as not been provided with online resource information.
* proxyBaseUrl: Public location of GeoServer instance, if managed behind a proxy or as part of a cluster.
* schemaBaseUrl
* verbose: Flag to control pretty printing and formatting of xml output
* verboseException: flag to include full strack trace in web service expections
* metadata: generic metadata map available to store additional settings
* clientProperties: transient information
* localWorkspaceIncludesPrefix
* showCreatedTimeColumnsInAdminList
* showModifiedTimeColumnsInAdminList
* defaultLocale
* userHeadsProxyURL

Some of these settings can be overriden on a workspace by workspace basis. This allows a workspace to have its own contact information and information policies.


