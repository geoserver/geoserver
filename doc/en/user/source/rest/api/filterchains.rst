.. _rest_api_filterchains:

Filter Chains
=============

This section documents the REST endpoints for managing **authentication filter chains**.

**Base path:** ``/rest/security/filterChain``

The API supports both XML and JSON. When sending JSON requests, fields use
regular property names (for example ``name``, ``clazz``, ``path``).
**Responses**, however, encode filter-chain attributes with an ``@`` prefix
(for example ``@name``, ``@class``, ``@path``) to match the XML attribute
representation.

----------------------------
List or create filter chains
----------------------------

**Endpoint:** ``/security/filterChain``

.. list-table::
   :header-rows: 1

   * - Method
     - Description
     - Request ``Content-Type``
     - Response ``Content-Type``
     - Success
   * - ``GET``
     - List all configured filter chains
     - –
     - ``application/json`` or ``application/xml``
     - ``200 OK``
   * - ``POST``
     - Create a new filter chain
     - ``application/json`` or ``application/xml``
     - ``application/json`` or ``application/xml``
     - ``201 Created`` (with ``Location`` header)

**Response (JSON) – GET list**

.. code-block:: json

   {
     "filterChain": {
       "filters": [
         {
           "@name": "web",
           "@class": "org.geoserver.security.HtmlLoginFilterChain",
           "@path": "/web/**,/gwc/rest/web/**,/",
           "@disabled": false,
           "@allowSessionCreation": true,
           "@ssl": false,
           "@matchHTTPMethod": false,
           "@interceptorName": "interceptor",
           "@exceptionTranslationName": "exception",
           "filter": ["rememberme", "form", "oidc-test", "anonymous"]
         },
         {
           "@name": "webLogin",
           "@class": "org.geoserver.security.ConstantFilterChain",
           "@path": "/j_spring_security_check,/j_spring_security_check/",
           "@disabled": false,
           "@allowSessionCreation": true,
           "@ssl": false,
           "@matchHTTPMethod": false,
           "filter": "form"
         },
         {
           "@name": "webLogout",
           "@class": "org.geoserver.security.LogoutFilterChain",
           "@path": "/j_spring_security_logout,/j_spring_security_logout/",
           "@disabled": false,
           "@allowSessionCreation": false,
           "@ssl": false,
           "@matchHTTPMethod": false,
           "filter": "formLogout"
         },
         {
           "@name": "rest",
           "@class": "org.geoserver.security.ServiceLoginFilterChain",
           "@path": "/rest.*,/rest/**",
           "@disabled": false,
           "@allowSessionCreation": false,
           "@ssl": false,
           "@matchHTTPMethod": false,
           "@interceptorName": "restInterceptor",
           "@exceptionTranslationName": "exception",
           "filter": ["basic", "anonymous"]
         },
         {
           "@name": "gwc",
           "@class": "org.geoserver.security.ServiceLoginFilterChain",
           "@path": "/gwc/rest.*,/gwc/rest/**",
           "@disabled": false,
           "@allowSessionCreation": false,
           "@ssl": false,
           "@matchHTTPMethod": false,
           "@interceptorName": "restInterceptor",
           "@exceptionTranslationName": "exception",
           "filter": "basic"
         },
         {
           "@name": "default",
           "@class": "org.geoserver.security.ServiceLoginFilterChain",
           "@path": "/**",
           "@disabled": false,
           "@allowSessionCreation": false,
           "@ssl": false,
           "@matchHTTPMethod": false,
           "@interceptorName": "interceptor",
           "@exceptionTranslationName": "exception",
           "filter": ["basic", "oidc-test", "anonymous"]
         }
       ]
     }
   }

**Response (XML) – GET list**

.. code-block:: xml

   <filterChain>
     <filters name="web" class="org.geoserver.security.HtmlLoginFilterChain"
              path="/web/**,/gwc/rest/web/**,/"
              disabled="false" allowSessionCreation="true"
              ssl="false" matchHTTPMethod="false"
              interceptorName="interceptor" exceptionTranslationName="exception">
       <filter>rememberme</filter>
       <filter>form</filter>
       <filter>oidc-test</filter>
       <filter>anonymous</filter>
     </filters>
     <!-- more <filters> ... -->
   </filterChain>

**Request (JSON) – POST create**

.. code-block:: json

   {
     "filters": {
       "name": "custom-web",
       "clazz": "org.geoserver.security.HtmlLoginFilterChain",
       "path": "/web/**,/gwc/rest/web/**,/",
       "disabled": false,
       "allowSessionCreation": true,
       "requireSSL": false,
       "matchHTTPMethod": false,
       "interceptorName": "interceptor",
       "exceptionTranslationName": "exception",
       "filters": ["rememberme", "form", "anonymous"]
     }
   }

**Request (XML) – POST create**

.. code-block:: xml

   <filters name="custom-web" class="org.geoserver.security.HtmlLoginFilterChain"
            path="/web/**,/gwc/rest/web/**,/"
            disabled="false" allowSessionCreation="true"
            ssl="false" matchHTTPMethod="false"
            interceptorName="interceptor" exceptionTranslationName="exception">
     <filter>rememberme</filter>
     <filter>form</filter>
     <filter>anonymous</filter>
   </filters>

-----------------------------
Get, update or delete a chain
-----------------------------

**Endpoint:** ``/security/filterChain/{chain_name}``

.. list-table::
   :header-rows: 1

   * - Method
     - Description
     - Request ``Content-Type``
     - Response ``Content-Type``
     - Success
   * - ``GET``
     - Retrieve a filter chain
     - –
     - ``application/json`` or ``application/xml``
     - ``200 OK``
   * - ``PUT``
     - Update a filter chain (optionally move with ``?position=<n>``)
     - ``application/json`` or ``application/xml``
     - ``application/json`` or ``application/xml``
     - ``200 OK``
   * - ``DELETE``
     - Delete a filter chain
     - –
     - –
     - ``200 OK`` (``410 Gone`` if already deleted)

**Response (JSON) – GET single**

.. code-block:: json

   {
     "filters": {
       "@name": "web",
       "@class": "org.geoserver.security.HtmlLoginFilterChain",
       "@path": "/web/**,/gwc/rest/web/**,/",
       "@disabled": false,
       "@allowSessionCreation": true,
       "@ssl": false,
       "@matchHTTPMethod": false,
       "@interceptorName": "interceptor",
       "@exceptionTranslationName": "exception",
       "filter": ["rememberme", "form", "oidc-test", "anonymous"]
     }
   }

**Response (XML) – GET single**

.. code-block:: xml

   <filters name="web" class="org.geoserver.security.HtmlLoginFilterChain"
            path="/web/**,/gwc/rest/web/**,/"
            disabled="false" allowSessionCreation="true"
            ssl="false" matchHTTPMethod="false"
            interceptorName="interceptor" exceptionTranslationName="exception">
     <filter>rememberme</filter>
     <filter>form</filter>
     <filter>oidc-test</filter>
     <filter>anonymous</filter>
   </filters>

**Request (JSON) – PUT update**

The JSON request body uses the same shape as **POST create**:

.. code-block:: json

   {
     "filters": {
       "name": "web",
       "clazz": "org.geoserver.security.HtmlLoginFilterChain",
       "path": "/web/**,/gwc/rest/web/**,/",
       "disabled": true,
       "allowSessionCreation": true,
       "requireSSL": false,
       "matchHTTPMethod": false,
       "interceptorName": "interceptor",
       "exceptionTranslationName": "exception",
       "filters": ["rememberme", "form"]
     }
   }

**Request (XML) – PUT update**

.. code-block:: xml

   <filters name="web" class="org.geoserver.security.HtmlLoginFilterChain"
            path="/web/**,/gwc/rest/web/**,/"
            disabled="true" allowSessionCreation="true"
            ssl="false" matchHTTPMethod="false"
            interceptorName="interceptor" exceptionTranslationName="exception">
     <filter>rememberme</filter>
     <filter>form</filter>
   </filters>

------------------
Reorder the chains
------------------

**Endpoint:** ``/security/filterChain/order``

**Method:** ``PUT``

Replaces the global filter-chain execution order.

**Request (JSON)**

.. code-block:: json

   { "order": ["web", "webLogin", "webLogout", "rest", "gwc", "default"] }

**Request (XML)**

.. code-block:: xml

   <order>
     <order>web</order>
     <order>webLogin</order>
     <order>webLogout</order>
     <order>rest</order>
     <order>gwc</order>
     <order>default</order>
   </order>

**Responses**

* ``200 OK`` on success
* ``400 Bad Request`` if the provided names are not a valid permutation
* ``403 Forbidden`` if not authenticated as an administrator

-------
Remarks
-------

* In **JSON responses**, filter-chain attributes are rendered with an ``@`` prefix to
  mirror XML attributes. In **JSON requests**, use the plain field names (no ``@``).
* The ``filter`` property in JSON **responses** can be either a single string (for a
  single filter) or an array of strings. In XML, filters are always repeated
  ``<filter>`` elements.
* When updating, you may move a chain to a specific position by supplying the
  ``position`` query parameter, e.g. ``PUT /security/filterChain/myChain?position=0``.

-----------
Error codes
-----------

.. list-table::
   :header-rows: 1

   * - Exception
     - Status code
   * - Malformed request
     - 400
   * - No administrative privileges
     - 403
   * - Authentication filter or chain not found
     - 404
   * - Gone – on delete only
     - 410
   * - Internal Server Error
     - 500
