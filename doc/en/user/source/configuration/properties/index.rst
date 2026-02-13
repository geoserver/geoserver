.. _application_properties:

Application Properties
----------------------

While many configuration and setup options are available through the Web Administration application :menuselection:`Settings > Global` page, more fundamental (and security minded) changes to how the application operates are made using "Application Properties" defined by (in order of priority):

  1. Java System Properties
  2. Web Application context parameters
  3. System Environmental Variables

As part of the operating environment GeoServer application properties, unlike settings, cannot be changed at runtime.

For more information see :ref:`production_config`.

GeoServer Property Reference
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. list-table::
   :width: 100%
   :widths: 70 10 10 10

   * - Application Property
     - System
       Property
     - Context
       Param
     - Env
       Variable
   * - GEOSERVER_DATA_DIR
       
       :doc:`/datadirectory/setting`
     - x
     - x
     - x
   * - GEOSERVER_MODULE_SYSTEM_ENVIRONMENT_STATUS_ENABLED
       
       :ref:`module_status_security_environment_vars`
     -  
     -  
     - x
   * - GEOSERVER_MODULE_SYSTEM_PROPERTY_STATUS_ENABLED
       
       :ref:`module_status_security_environment_vars`     
     -  
     -  
     - x
   * - GEOWEBCACHE_CACHE_DIR
       
       :doc:`/geowebcache/config`
     - x
     - x
     - x
   * - GEOSERVER_NODE_OPTS
       
       :doc:`/production/identify`
     - x
     - x
     - x
   * - serviceStrategy
       
       :doc:`/production/config`, default PARTIAL-BUFFER2
     - x
     - x
     - x
   * - GEOSERVER_CONSOLE_DISABLED
       
       :doc:`/production/config`
     - x
     - 
     - 
   * - GWC_DISKQUOTA_DISABLED
     - x
     - x
     - x
   * - geoserver.login.autocomplete
       
       :doc:`/production/config`, default on.
     - x
     - 
     - x
   * - CONFIGURATION_TRYLOCK_TIMEOUT
       
       Delay for REST API and Web Administration configuration changes (default 30000 MS)
     - x
     - x
     - x
   * - COMPARISON_TOLERANCE
       
       Referencing tolerance when matching PRJ to EPSG code (default 0.00000001)
     - x
     - x
     - x
   * - GEOSERVER_CSRF_DISABLED
       
       :doc:`/security/webadmin/csrf`
     - x
     - x
     - x
   * - GEOSERVER_CSRF_WHITELIST
       
       :doc:`/security/webadmin/csrf`
     - x
     - x
     - x
   * - org.geoserver.web.csp.strict
       
       :ref:`csp_strict`, default true.
     - x
     -
     - 
   * - org.geoserver.catalog.loadingThreads
       
       Number of threads used to load catalogue (Default 4).
     - x
     - x
     - x
   * - CAPABILITIES_CACHE_CONTROL_ENABLED
       
       Use false to disable, defaults to true.
     - x
     - x
     - x
   * - GEOSERVER_FILEBROWSER_HIDEFS
       
       When set to true only GEOSERVER_DATA_DIR available to browse.
     - x
     - x
     - x
   * - GEOSERVER_XSTREAM_WHITELIST
       
       Used to restrict catalogue persistence.
     - x
     - x
     - x
   * - ENTITY_RESOLUTION_UNRESTRICTED
       
       :doc:`/production/config`, default false.
     - x
     - x
     - x
   * - ENTITY_RESOLUTION_UNRESTRICTED_INTERNAL
       
       :doc:`/production/config`, default false.
     - x
     - x
     - x
   * - ENTITY_RESOLUTION_ALLOWLIST
       
       :doc:`/production/config`.
     - x
     - x
     - x
   * - geoserver.xframe.shouldSetPolicy
       
       :doc:`/production/config`, default true.
     - x
     - x
     - x
   * - geoserver.xframe.policy
       
       :doc:`/production/config`, default SAMEORIGIN
     - x
     - x
     - x
   * - geoserver.xContentType.shouldSetPolicy
       
       :doc:`/production/config`, default true
     - x
     - x
     - x
   * - geoserver.xXssProtection.shouldSetPolicy
       
       :doc:`/production/config`, default false
     - x
     - x
     - x
   * - geoserver.xXssProtection.policy
       
       :doc:`/production/config`, default 0
     - x
     - x
     - x
   * - geoserver.hsts.shouldSetPolicy
       
       :doc:`/production/config`, default false
     - x
     - x
     - x
   * - geoserver.hsts.policy
       
       :doc:`/production/config`, default max-age=31536000 ; includeSubDomains
     - x
     - x
     - x
   * - geoserver.csp.remoteResources
       
       :doc:`/security/csp`
     - x
     - x
     - x
   * - geoserver.csp.frameAncestors
       
       :doc:`/security/csp`
     - x
     - x
     - x
   * - geoserver.csp.fallbackDirectives
       
       :doc:`/security/csp`, default base-uri 'none'; form-action 'none'; default-src 'none'; frame-ancestors 'none';
     - x
     - x
     - x
   * - GEOSERVER_DISABLE_STATIC_WEB_FILES
       
       :ref:`production_config_static_files`, default false
     - x
     - x
     - x
   * - GEOSERVER_STATIC_WEB_FILES_SCRIPT
       
       :doc:`/tutorials/staticfiles`, default UNSAFE
     - x
     - x
     - x
   * - GEOSERVER_FEATUREINFO_HTML_SCRIPT
       
       :ref:`security_csp_featureinfo_html_script`, default SELF
     - x
     - x
     - x
   * - GEOSERVER_FORCE_FREEMARKER_ESCAPING

       :doc:`/production/config`, default true
     - x
     - x
     - x
   * - GEOSERVER_FREEMARKER_ALLOW_LIST

       :doc:`/tutorials/GetFeatureInfo/html`
     - x
     - x
     - x
   * - GEOSERVER_FREEMARKER_BLOCK_LIST

       :doc:`/tutorials/GetFeatureInfo/html`
     - x
     - x
     - x
   * - GEOSERVER_FREEMARKER_API_EXPOSED

       :doc:`/tutorials/GetFeatureInfo/html`, default false
     - x
     - x
     - x
   * - ows10.exception.xml.responsetype
       
       :doc:`/production/config`
     - x
     -
     - 
   * - ows11.exception.xml.responsetype
       
       :doc:`/production/config`
     - x
     -
     - 
   * - ENABLE_MAP_WRAPPING
       
       Default if setting unavailable (true)
     - x
     - x
     - x
   * - ENABLE_ADVANCED_PROJECTION
       
       Default if setting unavailable (true)
     - x
     - x
     - x
   * - OPTIMIZE_LINE_WIDTH
       
       :doc:`/services/wms/global`, default true (can be set false.)
     - x
     - x
     - x
   * - MAX_FILTER_RULES
       
       :doc:`/services/wms/global`, default 20
     - x
     - x
     - x
   * - USE_GLOBAL_RENDERING_POOL
       
       Default is true, can be set false
     - x
     - x
     - x
   * - org.geoserver.render.raster.direct.disable
   
       Used to bypass direct raster rendering
     - x
     - 
     - 
   * - wms.raster.disableGutter
       
       Disable gutter used to request larger area when reprojecting raster content.
     - x
     - 
     - 
   * - wms.raster.enableRasterChainDebug
       
       Trouble shoot raster rendering
     - x
     - 
     - 
   * - GEOSERVER_GLOBAL_LAYER_GROUP_INHERIT
       
       Should workspaces include layer groups from the global workspace, default true.
     - x
     - x
     - x
   * - PROXY_BASE_URL
       
       Supply PROXY_BASE_URL, overriding settings.
     - x
     - x
     - x
   * - PROXY_BASE_URL_HEADER
       
       Enables PROXY_BASE_URL to use headers variables if set to true, overriding GeoServer datadir settings.  Default false.
     - x
     - x
     - x
   * - org.geoserver.service.disabled
       
       :ref:`Layer service <data_webadmin_layers_services>` default comma separated list of disabled services.
     - x
     - x
     - x
   * - GEOSERVER_DEFAULT_CACHE_PROVIDER
       
       Request custom cache implementation for catalog.
     - x
     - x
     - x
   * - org.geoserver.wfs.xml.WFSURIHandler.disabled
   
       Flag to disable internal handling of references to GeoServer.
       Force reflective references such as DescribeFeatureType to be handled as separate request.
     - x
     - 
     - 
   * - org.geoserver.wfs.xml.WFSURIHandler.additionalHostnames
   
       default localhost.
     - x
     - 
     - 
   * - force200
       
       Use true to force the http return code to always be 200.
       Required for WCS2.0, breaks OWS2 and WCS2 standards.

     - x
     - 
     - 
   * - GS_SHAPEFILE_CHARSET
   
       Supply default for shapefile datastore
     - x
     - x
     - x
   * - GEOSERVER_GEOJSON_LEGACY_CRS
       
       true to enable legacy GeoJSON output.
     - x
     - x
     - x
   * - ENABLE_JSONP
       
       :doc:`/services/wms/global`
     - x
     - x
     - x
   * - XML_LOOKAHEAD
       
       Number of bytes read to determine XML POST request (default 8192).
     - x
     - x
     - x
   * - org.geoserver.wfs.getfeature.cachelimit
       
       :doc:`/production/config`, default 0 (disabled)
     - x
     - 
     - 
   * - org.geoserver.wfs.xml.entityExpansionLimit
       
       Default 100.
     - x
     - x
     - x
   * - org.geoserver.htmlTemplates.staticMemberAccess
       
       :doc:`/tutorials/GetFeatureInfo/html`
     - x
     - x
     - x
   * - ENABLE_OL3
       
       Default true.
     - x
     - x
     - x
   * - GEOSERVER_LOG_LOCATION
       
       :doc:`/configuration/logging`
     - x
     - x
     - x
   * - GEOSERVER_PRINT_CONFIG_DIR
       
       :ref:`printing_install`
     - x
     - 
     - x
   * - RELINQUISH_LOG4J_CONTROL
       
       :doc:`/configuration/logging`
     - x
     - x
     - x
   * - GT2_LOGGING_REDIRECTION
       
       :doc:`/configuration/logging`
     - x
     - x
     - x
   * - wicket.configuration
       
       Wicket RuntimeConfigurationType (DEPLOYMENT or DEVELOPMENT)
     - x
     - x
     - x
   * - GEOSERVER_FILESYSTEM_SANDBOX

       :doc:`/security/sandbox`
     - x
     - x
     - x
   * - GEOSERVER_ROOT_LOGIN_ENABLED

       :ref:`security_root`
     - x
     - x
     - x
   * - ALLOW_ENV_PARAMETRIZATION

       :doc:`/datadirectory/configtemplate`
     - x
     - 
     - 
   * - ENV_PROPERTIES

       :doc:`/datadirectory/configtemplate`
     - x
     - x
     - x

   * - WORKSPACE_ADMIN_SERVICE_ACCESS

       :ref:`Workspaces <data_webadmin_workspaces_service_settings>`
     - x
     - x
     - x

   * - GEOSERVER_DATA_DIR_LOADER_ENABLED

       :doc:`/datadirectory/setting`
     - x
     - 
     - x

   * - GEOSERVER_DATA_DIR_LOADER_THREADS

       :doc:`/datadirectory/setting`
     - x
     - 
     - x

   * - TRACK_USER

       Flag to enable user tracking in GeoServer.
       Allows to store the username of user that performed creation/modification of layer, layergroup, store, style, workspace. Has precedence over "Display the user who performed last modification" option in global settings.
     - x
     -
     -

.. _application_properties_setting:

Setting Application property
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Application properties are determined using the first value obtained from: Java System Properties, Web Application context parameters, or System Environmental Variable.

Using ``GEOSERVER_DATA_DIR`` as an example:

1. Java System Properties: Supplied to the java virtual machine as part of your application server configuration.
   
   .. code-block:: bash
      
      -DGEOSERVER_DATA_DIR=/var/lib/geoserver_data
   
   * For Tomcat on Linux edit :file:`setenv.sh` to append additional java system properties:
     
     .. code-block:: bash
     
        # Append system properties
        CATALINA_OPTS="${CATALINA_OPTS} -DGEOSERVER_DATA_DIR=/var/lib/geoserver_data"

   * For Tomcat on Windows use :command:`Apache Tomcat Properties` application, navigating to the :guilabel:`Java` tab to edit :guilabel:`Java Options`:
     
     .. code-block:: text
     
        -DGEOSERVER_DATA_DIR=C:\ProgramData\GeoServer\data
   
   While not commonly used for GEOSERVER_DATA_DIR, this approach is a popular way to enable/disable optional GeoServer functionality.

2. Web Application context parameter:
   
   * Tomcat: Use your application server to configure the GeoServer web application via :file:`conf/Catalina/localhost/geoserver.xml` file:
     
     .. code-block:: xml
     
        <Context docBase="geoserver.war">
          <Parameter name="GEOSERVER_DATA_DIR"
                     value="/var/opt/geoserver/data" override="false"/>
        </Context>
          
     .. note:: Tomcat management of application properties as using ``override="false"`` is not the most straight forward to understand. This setting prevents parameter defined in :file:`WEB-INF/web.xml` (from the :file:`geoserver.war` ) to override the provided file location.
        
        Other application servers provide a user interface to manage web application properties and are more intuitive.
     
   * Not recommended: Hand editing the `webapps/geoserver/WEB-INF/web.xml` file:
     
     .. code-block:: xml
     
        <context-param>
          <param-name>GEOSERVER_DATA_DIR</param-name>
          <param-value>/var/lib/geoserver_data</param-value>
        </context-param>
     
     .. note:: This file is part of the GeoServer application and will be replaced when updating the application.
        
        As a result this approach is error prone making updates more difficult and is not recommended.
   
3. System environmental variable:

   .. code-block:: bash
      
      export GEOSERVER_DATA_DIR=/var/lib/geoserver_data
   
   This approach can be useful for GEOSERVER_DATA_DIR when running GeoServer in a docker container, traditionally managed with environmental variables.
   
Additional system properties
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Cascading WFS and WMS services where GeoServer acts as a client for another web service make use of the Apache Http Components HTTP client library.

The HTTP client library respects the following java system properties::

   ssl.TrustManagerFactory.algorithm
   javax.net.ssl.trustStoreType
   javax.net.ssl.trustStore
   javax.net.ssl.trustStoreProvider
   javax.net.ssl.trustStorePassword
   ssl.KeyManagerFactory.algorithm
   javax.net.ssl.keyStoreType
   javax.net.ssl.keyStore
   javax.net.ssl.keyStoreProvider
   javax.net.ssl.keyStorePassword
   https.protocols
   https.cipherSuites
   http.proxyHost
   http.proxyPort
   https.proxyHost
   https.proxyPort
   http.nonProxyHosts
   http.keepAlive
   http.maxConnections
   http.agent

Reference:

* `HttpClientBuilder <https://hc.apache.org/httpcomponents-client-4.5.x/current/httpclient/apidocs/index.html?org/apache/http/impl/client/HttpClientBuilder.html>`__
