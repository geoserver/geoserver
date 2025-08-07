
.. _rest_api_authproviders:

Auth Providers
==============

The Security REST service lets administrators **list, create, update, delete,
enable, disable and re‑order** authentication‑provider configurations.

Each provider is represented as a *single* XML element / JSON property whose
**name equals the fully‑qualified class name**:

*XML*

.. code-block:: xml

   <org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig> … </…>

*JSON*

.. code-block:: json

   {
     "org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig": { … }
   }

For the full parameter list see the :api:`OpenAPI reference
<authenticationproviders.yaml>`.


--------------------------------------------------------------------
``/security/authProviders``
--------------------------------------------------------------------

Adds or lists providers.

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status codes
     - Formats
   * - **GET**
     - List providers (current *engine* order)
     - 200, 403, 500
     - XML, JSON
   * - **POST**
     - Create provider (optional ``?position=N``)
     - 201, 400, 403, 500
     - XML, JSON

*(existing examples unchanged, see above)*


--------------------------------------------------------------------
``/security/authProviders/{providerName}``
--------------------------------------------------------------------

View, update or delete a provider.

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status codes
     - Formats
   * - **GET**
     - Retrieve provider
     - 200, 403, 404, 500
     - XML, JSON
   * - **PUT**
     - Update provider (body must include ``id``)
     - 200, 400, 403, 404, 500
     - XML, JSON
   * - **DELETE**
     - Remove provider
     - 200, 403, 410, 500
     - –

*(existing examples unchanged, see above)*


--------------------------------------------------------------------
``/security/authProviders/order``
--------------------------------------------------------------------
**Enable, disable or re‑order providers** in a *single* request.

Sending a new ``order`` list:

* makes the **first name** the first to be consulted by GeoServer;
* any providers **omitted** from the list are **disabled** (they remain
  configured on disk but are not used until re‑added).

Only **PUT** is allowed.

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status codes
     - Formats
   * - **PUT**
     - Replace active order (enable / disable)
     - 200, 400, 403, 500
     - XML, JSON

Example — enable *corporateLdap* and make it first:

.. admonition:: curl (JSON body)

   curl -u admin:•••••• \
        -X PUT \
        -H "Content-Type: application/json" \
        http://localhost:8080/geoserver/rest/security/authProviders/order <<'EOF'
   {
     "order": ["corporateLdap", "default"]
   }
   EOF

*200 OK* – new order persisted.

.. admonition:: curl (XML body)

   curl -u admin:•••••• \
        -X PUT \
        -H "Content-Type: application/xml" \
        http://localhost:8080/geoserver/rest/security/authProviders/order <<'EOF'
   <order>
     <order>corporateLdap</order>
     <order>default</order>
   </order>
   EOF

Resulting state when listed:

.. code-block:: xml

   <authProviders>
     <org.geoserver.security.config.LdapAuthenticationProviderConfig>
       <!-- enabled (first) -->
     </org.geoserver.security.config.LdapAuthenticationProviderConfig>

     <org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig>
       <!-- enabled (second) -->
     </org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig>
   </authProviders>

If we later *disable* ``corporateLdap`` we simply omit it:

.. code-block:: json

   { "order": ["default"] }

After that call ``corporateLdap`` remains on disk but is **inactive**.

Exceptions
~~~~~~~~~~

.. list-table::
   :header-rows: 1

   * - Condition
     - Status code
   * - Malformed request / validation failure
     - 400
   * - No administrative privileges
     - 403
   * - Provider not found
     - 404
   * - Gone (already deleted)
     - 410
   * - Internal server error
     - 500
