.. _rest_api_authfilters:

Auth Filters
============

This section documents the REST endpoints for managing authentication filters in GeoServer.

- Collection endpoint: :ref:`/security/authFilters <security_authfilters>`
- Item endpoint: :ref:`/security/authFilters/{authFilter} <security_authfilters_authfilter>`

.. note::
   Admin privileges are required. All examples below use Basic authentication.

Content negotiation
-------------------

- **Requests with bodies** must set ``Content-Type`` to either ``application/xml`` or ``application/json``.
- **Responses** are selected using the ``Accept`` header (``application/xml`` or ``application/json``).
- If the requested response format is not supported, GeoServer returns **406 Not Acceptable**.
- If the request body type is unsupported, GeoServer returns **415 Unsupported Media Type**.

Representation conventions
--------------------------

- **XML**: The root element is the **fully qualified** filter configuration class name, for example:
  ``<org.geoserver.security.oauth2.OpenIdConnectFilterConfig>...</...>``.
- **JSON**: The payload is **wrapped** under a single key named after the fully qualified class, for example::

    {
      "org.geoserver.security.oauth2.OpenIdConnectFilterConfig": {
        "...": "..."
      }
    }

Fields are specific to each filter type (OpenId Connect, Anonymous, Security Interceptor, etc.).

.. _security_authfilters:

``/security/authFilters``
-------------------------

Adds or lists authentication filters.

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status codes
     - Formats
     - Default format
   * - GET
     - List all authentication filters
     - 200, 403, 406, 500
     - XML, JSON
     - (uses ``Accept``)
   * - POST
     - Create a new authentication filter
     - 201, 200, 400, 403, 406, 415, 500
     - XML, JSON
     - (uses ``Accept`` if a body is returned)

**Base URL used in examples**

.. code-block:: bash

   BASE="http://localhost:9002/geoserver/rest/security"

GET — List all filters
~~~~~~~~~~~~~~~~~~~~~~

**XML**

.. code-block:: bash

   curl -u admin:geoserver \
        -H "Accept: application/xml" \
        "$BASE/authFilters"

**Response: 200 OK**

.. code-block:: xml

   <authFilters>
     <authFilter>
       <name>Keycloak</name>
       <atom:link xmlns:atom="http://www.w3.org/2005/Atom"
                  rel="alternate"
                  href="http://localhost:9002/geoserver/rest/security/authFilters/Keycloak.xml"
                  type="application/atom+xml"/>
     </authFilter>
   </authFilters>

**JSON**

.. code-block:: bash

   curl -u admin:geoserver \
        -H "Accept: application/json" \
        "$BASE/authFilters"

**Response: 200 OK**

.. code-block:: json

   {
     "authFilters": {
       "authFilter": [
         {
           "name": "Keycloak",
           "href": "http://localhost:9002/geoserver/rest/security/authFilters/Keycloak.json"
         }
       ]
     }
   }

POST — Create a filter
~~~~~~~~~~~~~~~~~~~~~~

**XML**

.. code-block:: bash

   curl -u admin:geoserver \
        -H "Content-Type: application/xml" \
        -i \
        -d @- "$BASE/authFilters" <<'XML'
   <org.geoserver.security.oauth2.OpenIdConnectFilterConfig>
     <name>Keycloak7</name>
     <className>org.geoserver.security.oauth2.OpenIdConnectAuthenticationFilter</className>
     <roleSource class="org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig$PreAuthenticatedUserNameRoleSource">RoleService</roleSource>
     <roleServiceName>default</roleServiceName>
     <clientId>myclient</clientId>
     <clientSecret>UGIAvmT8qgfikS9cbAi2vUJOTVUU61sG</clientSecret>
     <accessTokenUri>http://localhost:8080/realms/myrealm/protocol/openid-connect/token</accessTokenUri>
     <userAuthorizationUri>http://localhost:8080/realms/myrealm/protocol/openid-connect/auth</userAuthorizationUri>
     <redirectUri>http://localhost:9001/geoserver/</redirectUri>
     <checkTokenEndpointUrl>http://localhost:8080/realms/myrealm/protocol/openid-connect/userinfo</checkTokenEndpointUrl>
     <introspectionEndpointUrl>http://localhost:8080/realms/myrealm/protocol/openid-connect/token/introspect</introspectionEndpointUrl>
     <logoutUri>http://localhost:8080/realms/myrealm/protocol/openid-connect/logout</logoutUri>
     <scopes>openid email</scopes>
     <enableRedirectAuthenticationEntryPoint>false</enableRedirectAuthenticationEntryPoint>
     <forceAccessTokenUriHttps>false</forceAccessTokenUriHttps>
     <forceUserAuthorizationUriHttps>false</forceUserAuthorizationUriHttps>
     <loginEndpoint>/j_spring_oauth2_openid_connect_login</loginEndpoint>
     <logoutEndpoint>/j_spring_oauth2_openid_connect_logout</logoutEndpoint>
     <allowUnSecureLogging>false</allowUnSecureLogging>
     <principalKey>email</principalKey>
     <jwkURI>http://localhost:8080/realms/myrealm/protocol/openid-connect/certs</jwkURI>
     <postLogoutRedirectUri>http://localhost:9001/geoserver/</postLogoutRedirectUri>
     <sendClientSecret>false</sendClientSecret>
     <allowBearerTokens>true</allowBearerTokens>
     <usePKCE>false</usePKCE>
     <enforceTokenValidation>false</enforceTokenValidation>
     <cacheAuthentication>false</cacheAuthentication>
   </org.geoserver.security.oauth2.OpenIdConnectFilterConfig>
   XML

**Response**

- ``201 Created``  
- ``Location: http://localhost:9002/geoserver/rest/security/authFilters/Keycloak7``

**JSON**

.. code-block:: bash

   curl -u admin:geoserver \
        -H "Content-Type: application/json" \
        -i \
        -d @- "$BASE/authFilters" <<'JSON'
   {
     "org.geoserver.security.oauth2.OpenIdConnectFilterConfig": {
       "name": "Keycloak12",
       "className": "org.geoserver.security.oauth2.OpenIdConnectAuthenticationFilter",
       "roleSource": {
         "@class": "org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig$PreAuthenticatedUserNameRoleSource",
         "$": "RoleService"
       },
       "roleServiceName": "default",
       "clientId": "myclient",
       "clientSecret": "UGIAvmT8qgfikS9cbAi2vUJOTVUU61sG",
       "accessTokenUri": "http://localhost:8080/realms/myrealm/protocol/openid-connect/token",
       "userAuthorizationUri": "http://localhost:8080/realms/myrealm/protocol/openid-connect/auth",
       "redirectUri": "http://localhost:9001/geoserver/",
       "checkTokenEndpointUrl": "http://localhost:8080/realms/myrealm/protocol/openid-connect/userinfo",
       "introspectionEndpointUrl": "http://localhost:8080/realms/myrealm/protocol/openid-connect/token/introspect",
       "logoutUri": "http://localhost:8080/realms/myrealm/protocol/openid-connect/logout",
       "scopes": "openid email",
       "enableRedirectAuthenticationEntryPoint": false,
       "forceAccessTokenUriHttps": false,
       "forceUserAuthorizationUriHttps": false,
       "loginEndpoint": "/j_spring_oauth2_openid_connect_login",
       "logoutEndpoint": "/j_spring_oauth2_openid_connect_logout",
       "allowUnSecureLogging": false,
       "principalKey": "email",
       "jwkURI": "http://localhost:8080/realms/myrealm/protocol/openid-connect/certs",
       "postLogoutRedirectUri": "http://localhost:9001/geoserver/",
       "sendClientSecret": false,
       "allowBearerTokens": true,
       "usePKCE": false,
       "enforceTokenValidation": false,
       "cacheAuthentication": false
     }
   }
   JSON

**Response**

- ``200 OK`` **or** ``201 Created``  
- ``Location: http://localhost:9002/geoserver/rest/security/authFilters/Keycloak12``

**Error status codes (collection)**

.. list-table::
   :header-rows: 1

   * - Condition
     - Status
   * - Malformed request body or fields
     - 400
   * - No administrative privileges
     - 403
   * - Unsupported ``Accept`` header
     - 406
   * - Unsupported ``Content-Type`` (POST)
     - 415
   * - Internal server error
     - 500

.. _security_authfilters_authfilter:

``/security/authFilters/{authFilter}``
--------------------------------------

View, update, or delete an existing authentication filter.

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status codes
     - Formats
     - Default format
   * - GET
     - View details of an authentication filter
     - 200, 403, 404, 406, 500
     - XML, JSON
     - (uses ``Accept``)
   * - PUT
     - Update the authentication filter
     - 200, 400, 403, 404, 406, 415, 500
     - XML, JSON
     - (uses ``Accept``)
   * - DELETE
     - Remove the authentication filter
     - 200, 403, 410, 500
     -
     -

GET — View a filter
~~~~~~~~~~~~~~~~~~~

**XML**

.. code-block:: bash

   curl -u admin:geoserver \
        -H "Accept: application/xml" \
        "$BASE/authFilters/anonymous"

**Response: 200 OK**

.. code-block:: xml

   <org.geoserver.security.config.AnonymousAuthenticationFilterConfig>
     <id>52857278:13c7ffd66a8:-7ff7</id>
     <name>anonymous</name>
     <className>org.geoserver.security.filter.GeoServerAnonymousAuthenticationFilter</className>
   </org.geoserver.security.config.AnonymousAuthenticationFilterConfig>

**JSON**

.. code-block:: bash

   curl -u admin:geoserver \
        -H "Accept: application/json" \
        "$BASE/authFilters/Keycloak"

**Response: 200 OK**

.. code-block:: json

   {
     "org.geoserver.security.oauth2.OpenIdConnectFilterConfig": {
       "id": "6bc4a33d:196d8c8ede2:-8000",
       "name": "Keycloak",
       "@class": "org.geoserver.security.oauth2.OpenIdConnectFilterConfig",
       "className": "org.geoserver.security.oauth2.OpenIdConnectAuthenticationFilter",
       "roleSource": {
         "@class": "org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig$PreAuthenticatedUserNameRoleSource",
         "$": "RoleService"
       },
       "roleServiceName": "default",
       "clientId": "myclient",
       "clientSecret": "UGIAvmT8qgfikS9cbAi2vUJOTVUU61sG",
       "accessTokenUri": "http://localhost:8080/realms/myrealm/protocol/openid-connect/token",
       "userAuthorizationUri": "http://localhost:8080/realms/myrealm/protocol/openid-connect/auth",
       "redirectUri": "http://localhost:9001/geoserver/",
       "checkTokenEndpointUrl": "http://localhost:8080/realms/myrealm/protocol/openid-connect/userinfo",
       "introspectionEndpointUrl": "http://localhost:8080/realms/myrealm/protocol/openid-connect/token/introspect",
       "logoutUri": "http://localhost:8080/realms/myrealm/protocol/openid-connect/logout",
       "scopes": "openid email",
       "enableRedirectAuthenticationEntryPoint": false,
       "forceAccessTokenUriHttps": false,
       "forceUserAuthorizationUriHttps": false,
       "loginEndpoint": "/j_spring_oauth2_openid_connect_login",
       "logoutEndpoint": "/j_spring_oauth2_openid_connect_logout",
       "allowUnSecureLogging": false,
       "principalKey": "email",
       "jwkURI": "http://localhost:8080/realms/myrealm/protocol/openid-connect/certs",
       "postLogoutRedirectUri": "http://localhost:9001/geoserver/",
       "sendClientSecret": false,
       "allowBearerTokens": true,
       "usePKCE": false,
       "enforceTokenValidation": false,
       "cacheAuthentication": false
     }
   }

PUT — Update a filter
~~~~~~~~~~~~~~~~~~~~~

**XML**

.. code-block:: bash

   curl -u admin:geoserver \
        -X PUT \
        -H "Content-Type: application/xml" \
        -d @- "$BASE/authFilters/restInterceptor9" <<'XML'
   <org.geoserver.security.config.SecurityInterceptorFilterConfig>
     <id>-2bf62d17:196c4deaf9b:-7fff</id>
     <name>restInterceptor9</name>
     <className>org.geoserver.security.filter.GeoServerSecurityInterceptorFilter</className>
     <allowIfAllAbstainDecisions>false</allowIfAllAbstainDecisions>
     <securityMetadataSource>restFilterDefinitionMap</securityMetadataSource>
   </org.geoserver.security.config.SecurityInterceptorFilterConfig>
   XML

**Response**

- ``200 OK``

**JSON**

.. code-block:: bash

   curl -u admin:geoserver \
        -X PUT \
        -H "Content-Type: application/json" \
        -d @- "$BASE/authFilters/restInterceptor13" <<'JSON'
   {
     "org.geoserver.security.config.SecurityInterceptorFilterConfig": {
       "id": "-3abefb99:196c5207331:-7ffe",
       "name": "restInterceptor13",
       "className": "org.geoserver.security.filter.GeoServerSecurityInterceptorFilter",
       "allowIfAllAbstainDecisions": true,
       "securityMetadataSource": "restFilterDefinitionMap"
     }
   }
   JSON

**Response**

- ``200 OK``

DELETE — Remove a filter
~~~~~~~~~~~~~~~~~~~~~~~~

.. code-block:: bash

   curl -u admin:geoserver \
        -X DELETE \
        "$BASE/authFilters/restInterceptor13"

**Response**

- ``200 OK``

Error status codes (item)
~~~~~~~~~~~~~~~~~~~~~~~~~

.. list-table::
   :header-rows: 1

   * - Condition
     - Status
   * - Malformed request body or fields
     - 400
   * - No administrative privileges
     - 403
   * - Authentication filter not found
     - 404
   * - Not acceptable (unsupported ``Accept``)
     - 406
   * - Unsupported media type (unsupported ``Content-Type``)
     - 415
   * - Gone — the filter does not exist or has already been removed (DELETE only)
     - 410
   * - Internal server error
     - 500

Tips and troubleshooting
------------------------

- If JSON requests fail with status **415**, ensure **``Content-Type: application/json``** is set.
- If you receive **406**, adjust the **``Accept``** header to ``application/xml`` or ``application/json``.
- When creating resources, the server typically returns **201 Created** with a **``Location``** header pointing to the new resource. Some deployments may return **200 OK**.
- For JSON, remember to **wrap** the payload using the fully qualified filter class name as the top-level key; for XML, use that class name as the **root element**.
