.. _rest_api_authfilters:

Auth Filters
==============

.. _security_authfilters:

``/security/authFilters``
-------------------------

Adds or Lists the authentication filters in the geoserver systems


.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status code
     - Formats
     - Default Format
   * - GET
     - List all auth filters in the system
     - 200,403,500
     - XML, JSON
     -
   * - POST
     - Great a new authFilter
     - 200,400,403,500
     - XML, JSON
     -

GET
---

*Request*
GET: http://localhost:9002/geoserver/rest/security/authFilters
Authorisation: XXXXX
Accept: application/xml

*Response*
200 OK

.. code-block:: xml

    <authFilters>
        <authFilter>
            <name>Keycloak</name>
            <atom:link xmlns:atom="http://www.w3.org/2005/Atom" rel="alternate" href="http://localhost:9002/geoserver/rest/security/authFilters/Keycloak.xml" type="application/atom+xml"/>
        </authFilter>
    <authFilters>

*Request*
GET: http://localhost:9002/geoserver/rest/security/authFilters
Authorisation: XXXXX
Accept: application/json

.. code-block:: json

    {
        "authFilters": {
            "authFilter": [
                {
                    "name": "Keycloak",
                    "href": "http://localhost:9002/geoserver/rest/security/authFilters/Keycloak.json"
                },
            ]
        }
    }



POST
-----

*Request*
POST: http://localhost:9002/geoserver/rest/security/authFilters
Authorisation: XXXXX
Content-Type: application/xml

.. code-block:: xml

    <org.geoserver.security.oauth2.OpenIdConnectFilterConfig>
        <name>Keycloak7</name>
        <className>org.geoserver.security.oauth2.OpenIdConnectAuthenticationFilter</className>
        <roleSource class="org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig$PreAuthenticatedUserNameRoleSource">RoleService</roleSource>
        <roleServiceName>default</roleServiceName>
        <cliendId>myclient</cliendId>
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

*Response*
201 CREATED
Location: http://localhost:9002/geoserver/rest/rest/security/authFilters/Keycloak7


*Request*
POST: http://localhost:9002/geoserver/rest/security/authFilters
Authorisation: XXXXX
Content-Type: application/jspn

.. code-block:: json

    {
    "org.geoserver.security.oauth2.OpenIdConnectFilterConfig":
      {
        "name": "Keycloak12",
        "className": "org.geoserver.security.oauth2.OpenIdConnectAuthenticationFilter",
        "roleSource": {
            "@class": "org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig$PreAuthenticatedUserNameRoleSource",
            "$": "RoleService"
        },
        "roleServiceName": "default",
        "cliendId": "myclient",
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

*Response*
200 OK
Location: http://localhost:9002/geoserver/rest/rest/security/authFilters/Keycloak12
Content-Type: application/xml


Exceptions
~~~~~~~~~~

.. list-table::
   :header-rows: 1

   * - Exception
     - Status code
   * - Malformed request
     - 400
   * - No administrative privileges
     - 403
   * - Internal Server Error
     - 500


.. _security_authfilters_authfilter:

``/security/authFilters/{authFilter}``
--------------------------------------

View, Update or Delete an existing auth filter


.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status code
     - Formats
     - Default Format
   * - GET
     - View the details of an authentication filter on the geoserver
     - 200,403,404,500
     - XML, JSON
     -
   * - PUT
     - Update the details of an authentication filter on the geoserver
     - 200,400,403,404,500
     - XML, JSON
     -
   * - DELETE
     - Update the details of an authentication filter on the geoserver
     - 200,403,410,500
     -
     -

GET
---

*Request*
GET http://localhost:9002/geoserver/rest/security/authFilters/anonymous
Accept: application/json
Authorisation: XXXXX

*Response*
Status: 200

.. code-block:: xml

    <org.geoserver.security.config.AnonymousAuthenticationFilterConfig>
        <id>52857278:13c7ffd66a8:-7ff7</id>
        <name>anonymous</name>
        <className>org.geoserver.security.filter.GeoServerAnonymousAuthenticationFilter</className>
    </org.geoserver.security.config.AnonymousAuthenticationFilterConfig>

GET

*Request*

GET http://localhost:9002/geoserver/rest/security/authFilters/Keycloak
Accept: application/json
Authorisation: XXXXX

*Response*
Status: 200
Content-Type: application/json

.. code-block::json
    {
        "org.geoserver.security.oauth2.OpenIdConnectFilterConfig": {
            "id": "6bc4a33d:196d8c8ede2:-8000",
            "name": "Keycloak",
            "@class": "org.geoserver.security.oauth2.OpenIdConnectFilterConfig",
            "id": "6bc4a33d:196d8c8ede2:-8000",
            "name": "Keycloak",
            "className": "org.geoserver.security.oauth2.OpenIdConnectAuthenticationFilter",
            "roleSource": {
                "@class": "org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig$PreAuthenticatedUserNameRoleSource",
                "$": "RoleService"
            },
            "roleServiceName": "default",
            "cliendId": "myclient",
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

PUT
---

*Request*
POST http://localhost:9002/geoserver/rest/security/authFilters/restInterceptor9
Authorisation: XXXXX
Content-Type: application/xml

.. code-block:: xml

  <org.geoserver.security.config.SecurityInterceptorFilterConfig>
    <id>-2bf62d17:196c4deaf9b:-7fff</id>
    <name>restInterceptor9</name>
    <className>org.geoserver.security.filter.GeoServerSecurityInterceptorFilter</className>
    <allowIfAllAbstainDecisions>false</allowIfAllAbstainDecisions>
    <securityMetadataSource>restFilterDefinitionMap</securityMetadataSource>
  </org.geoserver.security.config.SecurityInterceptorFilterConfig>

*Response*
Status:200

*Request*
POST http://localhost:9002/geoserver/rest/security/authFilters/restInterceptor13
Authorisation: XXXXX
Content-Type: application/json

.. code-block:: json

    {
        "org.geoserver.security.config.SecurityInterceptorFilterConfig": {
            "id": "-3abefb99:196c5207331:-7ffe",
            "name": "restInterceptor13",
            "className": "org.geoserver.security.filter.GeoServerSecurityInterceptorFilter",
            "allowIfAllAbstainDecisions": true,
            "securityMetadataSource": "restFilterDefinitionMap"
        }
    }

*Response*
Status:200

DELETE
------

*Request*
DELETE: http://localhost:9002/geoserver/rest/security/authFilters/restInterceptor13
Authorisation: XXXXX

*Response*
Status:200

Exceptions
~~~~~~~~~~

.. list-table::
   :header-rows: 1

   * - Exception
     - Status code
   * - Malformed request
     - 400
   * - No administrative privileges
     - 403
   * - Authentication filter not found
     - 404
   * - Gone - On Delete Only
     - 410
   * - Internal Server Error
     - 500
