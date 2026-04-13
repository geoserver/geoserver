# Application Properties

While many configuration and setup options are available through the Web Administration application **Settings > Global** page, more fundamental (and security minded) changes to how the application operates are made using "Application Properties" defined by (in order of priority):

1.  Java System Properties
2.  Web Application context parameters
3.  System Environmental Variables

As part of the operating environment GeoServer application properties, unlike settings, cannot be changed at runtime.

For more information see [Configuration Considerations](../../production/config.md).

## GeoServer Property Reference

| Application Property                                                             | System Property | Context Param | Env Variable |
|----------------------------------------------------------------------------------|-----------------|---------------|--------------|
| GEOSERVER_DATA_DIR<br>[/datadirectory/setting](../../datadirectory/setting.md)        | x               | x             | x            |
| GEOSERVER_MODULE_SYSTEM_ENVIRONMENT_STATUS_ENABLED<br>[Showing Environment Variables and Java System Properties](../../production/config.md#module_status_security_environment_vars) |                 |               | x            |
| GEOSERVER_MODULE_SYSTEM_PROPERTY_STATUS_ENABLED<br>[Showing Environment Variables and Java System Properties](../../production/config.md#module_status_security_environment_vars) |                 |               | x            |
| GEOWEBCACHE_CACHE_DIR<br>[/geowebcache/config](../../geowebcache/config.md)           | x               | x             | x            |
| GEOSERVER_NODE_OPTS<br>[/production/identify](../../production/identify.md)           | x               | x             | x            |
| serviceStrategy<br>[/production/config](../../production/config.md), default PARTIAL-BUFFER2 | x               | x             | x            |
| GEOSERVER_CONSOLE_DISABLED<br>[/production/config](../../production/config.md)        | x               |               |              |
| GWC_DISKQUOTA_DISABLED                                                           | x               | x             | x            |
| geoserver.login.autocomplete<br>[/production/config](../../production/config.md), default on. | x               |               | x            |
| CONFIGURATION_TRYLOCK_TIMEOUT<br>Delay for REST API and Web Administration configuration changes (default 30000 MS) | x               | x             | x            |
| COMPARISON_TOLERANCE<br>Referencing tolerance when matching PRJ to EPSG code (default 0.00000001) | x               | x             | x            |
| GEOSERVER_CSRF_DISABLED<br>[/security/webadmin/csrf](../../security/webadmin/csrf.md) | x               | x             | x            |
| GEOSERVER_CSRF_WHITELIST<br>[/security/webadmin/csrf](../../security/webadmin/csrf.md) | x               | x             | x            |
| org.geoserver.web.csp.strict<br>[User interface non-responsive](../../production/troubleshooting.md#csp_strict), default true. | x               |               |              |
| org.geoserver.catalog.loadingThreads<br>Number of threads used to load catalogue (Default 4). | x               | x             | x            |
| CAPABILITIES_CACHE_CONTROL_ENABLED<br>Use false to disable, defaults to true.    | x               | x             | x            |
| GEOSERVER_FILEBROWSER_HIDEFS<br>When set to true only GEOSERVER_DATA_DIR available to browse. | x               | x             | x            |
| GEOSERVER_XSTREAM_WHITELIST<br>Used to restrict catalogue persistence.           | x               | x             | x            |
| ENTITY_RESOLUTION_UNRESTRICTED<br>[/production/config](../../production/config.md), default false. | x               | x             | x            |
| ENTITY_RESOLUTION_UNRESTRICTED_INTERNAL<br>[/production/config](../../production/config.md), default false. | x               | x             | x            |
| ENTITY_RESOLUTION_ALLOWLIST<br>[/production/config](../../production/config.md).      | x               | x             | x            |
| geoserver.xframe.shouldSetPolicy<br>[/production/config](../../production/config.md), default true. | x               | x             | x            |
| geoserver.xframe.policy<br>[/production/config](../../production/config.md), default SAMEORIGIN | x               | x             | x            |
| geoserver.xContentType.shouldSetPolicy<br>[/production/config](../../production/config.md), default true | x               | x             | x            |
| geoserver.xXssProtection.shouldSetPolicy<br>[/production/config](../../production/config.md), default false | x               | x             | x            |
| geoserver.xXssProtection.policy<br>[/production/config](../../production/config.md), default 0 | x               | x             | x            |
| geoserver.hsts.shouldSetPolicy<br>[/production/config](../../production/config.md), default false | x               | x             | x            |
| geoserver.hsts.policy<br>[/production/config](../../production/config.md), default max-age=31536000 ; includeSubDomains | x               | x             | x            |
| geoserver.csp.remoteResources<br>[/security/csp](../../security/csp.md)               | x               | x             | x            |
| geoserver.csp.frameAncestors<br>[/security/csp](../../security/csp.md)                | x               | x             | x            |
| geoserver.csp.fallbackDirectives<br>[/security/csp](../../security/csp.md), default base-uri 'none'; form-action 'none'; default-src 'none'; frame-ancestors 'none'; | x               | x             | x            |
| GEOSERVER_DISABLE_STATIC_WEB_FILES<br>[Static Web Files](../../production/config.md#production_config_static_files), default false | x               | x             | x            |
| GEOSERVER_STATIC_WEB_FILES_SCRIPT<br>[/tutorials/staticfiles](../../tutorials/staticfiles.md), default UNSAFE | x               | x             | x            |
| GEOSERVER_FEATUREINFO_HTML_SCRIPT<br>[WFS GetFeatureInfo CSP Policy](../../security/csp.md#security_csp_featureinfo_html_script), default SELF | x               | x             | x            |
| GEOSERVER_FORCE_FREEMARKER_ESCAPING<br>[/production/config](../../production/config.md), default true | x               | x             | x            |
| GEOSERVER_FREEMARKER_ALLOW_LIST<br>[/tutorials/GetFeatureInfo/html](../../tutorials/GetFeatureInfo/html.md) | x               | x             | x            |
| GEOSERVER_FREEMARKER_BLOCK_LIST<br>[/tutorials/GetFeatureInfo/html](../../tutorials/GetFeatureInfo/html.md) | x               | x             | x            |
| GEOSERVER_FREEMARKER_API_EXPOSED<br>[/tutorials/GetFeatureInfo/html](../../tutorials/GetFeatureInfo/html.md), default false | x               | x             | x            |
| ows10.exception.xml.responsetype<br>[/production/config](../../production/config.md)  | x               |               |              |
| ows11.exception.xml.responsetype<br>[/production/config](../../production/config.md)  | x               |               |              |
| ENABLE_MAP_WRAPPING<br>Default if setting unavailable (true)                     | x               | x             | x            |
| ENABLE_ADVANCED_PROJECTION<br>Default if setting unavailable (true)              | x               | x             | x            |
| OPTIMIZE_LINE_WIDTH<br>[/services/wms/global](../../services/wms/global.md), default true (can be set false.) | x               | x             | x            |
| MAX_FILTER_RULES<br>[/services/wms/global](../../services/wms/global.md), default 20  | x               | x             | x            |
| USE_GLOBAL_RENDERING_POOL<br>Default is true, can be set false                   | x               | x             | x            |
| org.geoserver.render.raster.direct.disable<br>Used to bypass direct raster rendering | x               |               |              |
| wms.raster.disableGutter<br>Disable gutter used to request larger area when reprojecting raster content. | x               |               |              |
| wms.raster.enableRasterChainDebug<br>Trouble shoot raster rendering              | x               |               |              |
| GEOSERVER_GLOBAL_LAYER_GROUP_INHERIT<br>Should workspaces include layer groups from the global workspace, default true. | x               | x             | x            |
| PROXY_BASE_URL<br>Supply PROXY_BASE_URL, overriding settings.                    | x               | x             | x            |
| PROXY_BASE_URL_HEADER<br>Enables PROXY_BASE_URL to use headers variables if set to true, overriding GeoServer datadir settings. Default false. | x               | x             | x            |
| org.geoserver.service.disabled<br>[Layer service](../../data/webadmin/layers.md#data_webadmin_layers_services) default comma separated list of disabled services. | x               | x             | x            |
| GEOSERVER_DEFAULT_CACHE_PROVIDER<br>Request custom cache implementation for catalog. | x               | x             | x            |
| org.geoserver.wfs.xml.WFSURIHandler.disabled<br>Flag to disable internal handling of references to GeoServer. Force reflective references such as DescribeFeatureType to be handled as separate request. | x               |               |              |
| org.geoserver.wfs.xml.WFSURIHandler.additionalHostnames<br>default localhost.    | x               |               |              |
| force200<br>Use true to force the http return code to always be 200. Required for WCS2.0, breaks OWS2 and WCS2 standards. | x               |               |              |
| GS_SHAPEFILE_CHARSET<br>Supply default for shapefile datastore                   | x               | x             | x            |
| GEOSERVER_GEOJSON_LEGACY_CRS<br>true to enable legacy GeoJSON output.            | x               | x             | x            |
| ENABLE_JSONP<br>[/services/wms/global](../../services/wms/global.md)                  | x               | x             | x            |
| XML_LOOKAHEAD<br>Number of bytes read to determine XML POST request (default 8192). | x               | x             | x            |
| org.geoserver.wfs.getfeature.cachelimit<br>[/production/config](../../production/config.md), default 0 (disabled) | x               |               |              |
| org.geoserver.wfs.xml.entityExpansionLimit<br>Default 100.                       | x               | x             | x            |
| org.geoserver.htmlTemplates.staticMemberAccess<br>[/tutorials/GetFeatureInfo/html](../../tutorials/GetFeatureInfo/html.md) | x               | x             | x            |
| ENABLE_OL3<br>Default true.                                                      | x               | x             | x            |
| GEOSERVER_LOG_LOCATION<br>[/configuration/logging](../logging.md)    | x               | x             | x            |
| GEOSERVER_PRINT_CONFIG_DIR<br>[Printing Installation](../../extensions/printing/install.md) | x               |               | x            |
| RELINQUISH_LOG4J_CONTROL<br>[/configuration/logging](../logging.md)  | x               | x             | x            |
| GT2_LOGGING_REDIRECTION<br>[/configuration/logging](../logging.md)   | x               | x             | x            |
| wicket.configuration<br>Wicket RuntimeConfigurationType (DEPLOYMENT or DEVELOPMENT) | x               | x             | x            |
| GEOSERVER_FILESYSTEM_SANDBOX<br>[/security/sandbox](../../security/sandbox.md)        | x               | x             | x            |
| GEOSERVER_ROOT_LOGIN_ENABLED<br>[Root account](../../security/root.md)           | x               | x             | x            |
| ALLOW_ENV_PARAMETRIZATION<br>[/datadirectory/configtemplate](../../datadirectory/configtemplate.md) | x               |               |              |
| ENV_PROPERTIES<br>[/datadirectory/configtemplate](../../datadirectory/configtemplate.md) | x               | x             | x            |
| WORKSPACE_ADMIN_SERVICE_ACCESS<br><!-- BROKEN LINK: Workspaces -->               | x               | x             | x            |
| GEOSERVER_DATA_DIR_LOADER_ENABLED<br>[/datadirectory/setting](../../datadirectory/setting.md) | x               |               | x            |
| GEOSERVER_DATA_DIR_LOADER_THREADS<br>[/datadirectory/setting](../../datadirectory/setting.md) | x               |               | x            |
| TRACK_USER<br>Flag to enable user tracking in GeoServer. Allows to store the username of user that performed creation/modification of layer, layergroup, store, style, workspace. Has precedence over "Display the user who performed last modification" option in global settings. | x               |               |              |

## Setting Application property {: #application_properties_setting }

Application properties are determined using the first value obtained from: Java System Properties, Web Application context parameters, or System Environmental Variable.

Using `GEOSERVER_DATA_DIR` as an example:

1.  Java System Properties: Supplied to the java virtual machine as part of your application server configuration.

    ``` bash
    -DGEOSERVER_DATA_DIR=/var/lib/geoserver_data
    ```

    - For Tomcat on Linux edit **`setenv.sh`** to append additional java system properties:

      ``` bash
      # Append system properties
      CATALINA_OPTS="${CATALINA_OPTS} -DGEOSERVER_DATA_DIR=/var/lib/geoserver_data"
      ```

    - For Tomcat on Windows use ***Apache Tomcat Properties*** application, navigating to the **Java** tab to edit **Java Options**:

      ```text
      -DGEOSERVER_DATA_DIR=C:\ProgramData\GeoServer\data
      ```

    While not commonly used for GEOSERVER_DATA_DIR, this approach is a popular way to enable/disable optional GeoServer functionality.

2.  Web Application context parameter:

    - Tomcat: Use your application server to configure the GeoServer web application via **`conf/Catalina/localhost/geoserver.xml`** file:

      ``` xml
      <Context docBase="geoserver.war">
        <Parameter name="GEOSERVER_DATA_DIR"
                   value="/var/opt/geoserver/data" override="false"/>
      </Context>
      ```

    !!! note
          Tomcat management of application properties as using `override="false"` is not the most straight forward to understand. This setting prevents parameter defined in **`WEB-INF/web.xml`** (from the **`geoserver.war`** ) to override the provided file location.
    
          Other application servers provide a user interface to manage web application properties and are more intuitive.

    - Not recommended: Hand editing the `webapps/geoserver/WEB-INF/web.xml` file:

      ``` xml
      <context-param>
        <param-name>GEOSERVER_DATA_DIR</param-name>
        <param-value>/var/lib/geoserver_data</param-value>
      </context-param>
      ```

    !!! note
          This file is part of the GeoServer application and will be replaced when updating the application.
    
          As a result this approach is error prone making updates more difficult and is not recommended.

3.  System environmental variable:

    ``` bash
    export GEOSERVER_DATA_DIR=/var/lib/geoserver_data
    ```

    This approach can be useful for GEOSERVER_DATA_DIR when running GeoServer in a docker container, traditionally managed with environmental variables.

## Additional system properties

Cascading WFS and WMS services where GeoServer acts as a client for another web service make use of the Apache Http Components HTTP client library.

The HTTP client library respects the following java system properties:

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

- [HttpClientBuilder](https://hc.apache.org/httpcomponents-client-4.5.x/current/httpclient/apidocs/index.html?org/apache/http/impl/client/HttpClientBuilder.md)
