.. _rest_authproviders:

Auth Providers (How-To)
=======================

The Security REST service lets administrators **list, create, update, delete,
enable/disable, and re-order** authentication-provider configurations.

**Permissions:** all endpoints require an authenticated user with the
``ROLE_ADMINISTRATOR`` role.

Representation
--------------

- **XML** uses the **concrete config class name** as the element name, e.g.:

  .. code-block:: xml

     <org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig>
       …
     </org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig>

- **JSON** requests/responses are **plain objects** (example below). For requests,
  the controller also accepts the **envelope** ``{ "authprovider": { … } }``.

General rules
-------------

- **Create (POST):** ``className`` is **required**.
- **Update (PUT):**
  - Path ``{providerName}`` **must match** payload ``name``.
  - ``className`` **cannot change**; omit it to keep the current value.
  - ``id`` may be omitted; the existing value is preserved.
- **Order & enable/disable:**
  - The active list in the security config (``<authproviderNames>``) defines both **order** and **which providers are enabled**.
  - Names **present** = enabled (in that order). Names **absent** = disabled (kept on disk).

Base URL
--------

All examples assume:

``http://localhost:8080/geoserver/rest``

------------------------------------------------
List providers — ``GET /security/authproviders``
------------------------------------------------

Returns providers in the **current active order**.

.. admonition:: curl (XML)

   curl -u admin:•••• \
        -H "Accept: application/xml" \
        http://localhost:8080/geoserver/rest/security/authproviders

**Response 200 (XML)**

.. code-block:: xml

   <authproviders>
     <org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig>
       <id>52857278:13c7ffd66a8:-7ff0</id>
       <name>default</name>
       <className>org.geoserver.security.auth.UsernamePasswordAuthenticationProvider</className>
       <userGroupServiceName>default</userGroupServiceName>
     </org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig>
   </authproviders>

.. admonition:: curl (JSON)

   curl -u admin:•••• \
        -H "Accept: application/json" \
        http://localhost:8080/geoserver/rest/security/authproviders

**Response 200 (JSON)**

.. code-block:: json

   {
     "authproviders": [
       {
         "id": "52857278:13c7ffd66a8:-7ff0",
         "name": "default",
         "className": "org.geoserver.security.auth.UsernamePasswordAuthenticationProvider",
         "userGroupServiceName": "default"
       }
     ]
   }

---------------------------------------------------------------
Get a provider — ``GET /security/authproviders/{providerName}``
---------------------------------------------------------------

.. admonition:: curl (XML)

   curl -u admin:•••• \
        -H "Accept: application/xml" \
        http://localhost:8080/geoserver/rest/security/authproviders/default

**Response 200 (XML)**

.. code-block:: xml

   <org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig>
     <id>52857278:13c7ffd66a8:-7ff0</id>
     <name>default</name>
     <className>org.geoserver.security.auth.UsernamePasswordAuthenticationProvider</className>
     <userGroupServiceName>default</userGroupServiceName>
   </org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig>

**Response 200 (JSON)**

.. code-block:: json

   {
     "id": "52857278:13c7ffd66a8:-7ff0",
     "name": "default",
     "className": "org.geoserver.security.auth.UsernamePasswordAuthenticationProvider",
     "userGroupServiceName": "default"
   }

Status: ``200``, ``403``, ``404``, ``500``

----------------------------------------------------
Create a provider — ``POST /security/authproviders``
----------------------------------------------------

Optional ``?position=N`` (0-based) inserts the new provider at that index; omit to append.

.. admonition:: curl (XML body)

   .. code-block:: bash

      curl -u admin:•••• \
           -X POST \
           -H "Content-Type: application/xml" \
           -H "Accept: application/xml" \
           --data-binary @- \
           http://localhost:8080/geoserver/rest/security/authproviders <<'EOF'
      <org.geoserver.security.config.LdapAuthenticationProviderConfig>
        <name>corporateLdap</name>
        <className>org.geoserver.security.auth.LdapAuthenticationProvider</className>
        <userGroupServiceName>ldapUsers</userGroupServiceName>
      </org.geoserver.security.config.LdapAuthenticationProviderConfig>
      EOF


.. admonition:: curl (JSON body — bare)

   .. code-block:: bash

      curl -u admin:•••• \
           -X POST \
           -H "Content-Type: application/json" \
           -H "Accept: application/json" \
           --data '{"name":"corporateLdap","className":"org.geoserver.security.auth.LdapAuthenticationProvider","userGroupServiceName":"ldapUsers"}' \
           http://localhost:8080/geoserver/rest/security/authproviders

.. admonition:: curl (JSON body — envelope)

   .. code-block:: bash

      curl -u admin:•••• \
           -X POST \
           -H "Content-Type: application/json" \
           -H "Accept: application/json" \
           --data '{"authprovider":{"name":"corporateLdap","className":"org.geoserver.security.auth.LdapAuthenticationProvider","userGroupServiceName":"ldapUsers"}}' \
           http://localhost:8080/geoserver/rest/security/authproviders

**Response 201**

.. code-block:: none

   Location: /geoserver/rest/security/authproviders/corporateLdap

Body echoes the created provider with a server-assigned ``id``.

Status: ``201``, ``400`` (validation/duplicate/reserved name/position), ``403``, ``500``


------------------------------------------------------------------
Update a provider — ``PUT /security/authproviders/{providerName}``
------------------------------------------------------------------

You may also **move** a provider by adding ``?position=N``.

Rules recap:

- path ``{providerName}`` must equal payload ``name``
- ``className`` cannot change (omit to keep)
- ``id`` may be omitted (kept as is)

.. admonition:: curl (XML body)

   .. code-block:: bash

      curl -u admin:•••• \
           -X PUT \
           -H "Content-Type: application/xml" \
           -H "Accept: application/xml" \
           --data-binary @- \
           "http://localhost:8080/geoserver/rest/security/authproviders/corporateLdap?position=0" <<'EOF'
      <org.geoserver.security.config.LdapAuthenticationProviderConfig>
        <name>corporateLdap</name>
        <className>org.geoserver.security.auth.LdapAuthenticationProvider</className>
        <userGroupServiceName>ldapUsers</userGroupServiceName>
      </org.geoserver.security.config.LdapAuthenticationProviderConfig>
      EOF

.. admonition:: curl (JSON)

   .. code-block:: bash

      curl -u admin:•••• \
           -X PUT \
           -H "Content-Type: application/json" \
           -H "Accept: application/json" \
           --data '{"name":"corporateLdap","className":"org.geoserver.security.auth.LdapAuthenticationProvider","userGroupServiceName":"ldapUsers"}' \
           "http://localhost:8080/geoserver/rest/security/authproviders/corporateLdap?position=0"

**Response 200** returns the updated provider.

.. rubric:: Status codes

``200``, ``400`` (name mismatch/class change/position), ``403``, ``404``, ``500``


---------------------------------------------------------------------
Delete a provider — ``DELETE /security/authproviders/{providerName}``
---------------------------------------------------------------------

Removes the provider **and** drops it from the active order.

.. admonition:: curl

   .. code-block:: bash

      curl -u admin:•••• \
           -X DELETE \
           http://localhost:8080/geoserver/rest/security/authproviders/corporateLdap

.. rubric:: Status codes

``200``, ``403``, ``404`` (not found), ``410`` (already removed), ``500``

-------------------------------------------------------------------
Re-order / enable / disable — ``PUT /security/authproviders/order``
-------------------------------------------------------------------

Send the **complete** list of provider names in the desired order:

- providers **listed** = **enabled** (in that order)
- providers **omitted** = **disabled** (remain configured on disk)

Only **PUT** is allowed.

.. admonition:: curl (JSON)

   .. code-block:: bash

      curl -u admin:•••• \
           -X PUT \
           -H "Content-Type: application/json" \
           --data '{"order":["corporateLdap","default"]}' \
           http://localhost:8080/geoserver/rest/security/authproviders/order

.. admonition:: curl (XML)

   .. code-block:: bash

      curl -u admin:•••• \
           -X PUT \
           -H "Content-Type: application/xml" \
           --data-binary @- \
           http://localhost:8080/geoserver/rest/security/authproviders/order <<'EOF'
      <order>
        <order>corporateLdap</order>
        <order>default</order>
      </order>
      EOF

.. rubric:: Status codes

``200``, ``400`` (unknown name/empty list), ``403``, ``500``


Error responses
---------------

All errors use a simple payload:

.. code-block:: json

   { "status": 400, "message": "Missing 'className' in JSON payload" }

.. code-block:: xml

   <ErrorResponse>
     <status>400</status>
     <message>Missing 'className' in JSON payload</message>
   </ErrorResponse>

Operational notes
-----------------

- Writes are serialized to protect on-disk XML and the security manager state.
- After **POST**, **PUT**, **DELETE**, and **/order** updates, the security configuration is **reloaded** automatically.
- For POST/PUT, prefer to omit ``id``; it is server-managed.

See also
--------

- :api:`OpenAPI reference <authenticationproviders.yaml>`
