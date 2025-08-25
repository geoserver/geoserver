.. _rest_api_authproviders:

Auth Providers (Endpoint Reference)
===================================

This page summarizes the REST endpoints for managing authentication providers.
For request/response shapes and full examples, see :ref:`rest_api_authproviders`.

Base path: ``/geoserver/rest``

Security
--------

- HTTP Basic auth
- Requires ``ROLE_ADMINISTRATOR``

Content types
-------------

- ``application/xml`` — uses the concrete config class name as the element
- ``application/json`` — plain objects; request envelopes supported:
  - ``{ "authprovider": { … } }`` for single
  - ``{ "authproviders": [ { … }, … ] }`` for lists

Status codes
------------

- ``200`` OK, ``201`` Created
- ``400`` Bad Request (malformed/validation/duplicate/reserved/position)
- ``403`` Forbidden (not an administrator)
- ``404`` Not Found
- ``410`` Gone (already deleted)
- ``500`` Internal Server Error

Error body
----------

.. code-block:: json

   { "status": 400, "message": "Reason here" }

Endpoints
---------

``GET /security/authproviders``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

List providers in **active order**.

- Produces: XML, JSON
- Returns: object with ``authproviders`` array; each entry is a provider

``POST /security/authproviders``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Create a provider; optionally **insert at position** via ``?position=N`` (0-based).

- Consumes/Produces: XML, JSON
- Body (JSON, bare example):

  .. code-block:: json

     {
       "name": "corporateLdap",
       "className": "org.geoserver.security.auth.LdapAuthenticationProvider",
       "userGroupServiceName": "ldapUsers"
     }

- Body (XML):

  .. code-block:: xml

     <org.geoserver.security.config.LdapAuthenticationProviderConfig>
       <name>corporateLdap</name>
       <className>org.geoserver.security.auth.LdapAuthenticationProvider</className>
       <userGroupServiceName>ldapUsers</userGroupServiceName>
     </org.geoserver.security.config.LdapAuthenticationProviderConfig>

- Response: ``201`` with ``Location`` header and created provider in body

Rules:
- ``className`` required
- Name ``order`` is reserved
- Duplicate names rejected
- ``position`` must be within ``[0..size]``

``GET /security/authproviders/{providerName}``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Retrieve a provider by name (``.xml``/``.json`` suffix in the name is accepted and normalized).

- Produces: XML, JSON
- Response: provider object

``PUT /security/authproviders/{providerName}``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Update a provider and/or **move** it via ``?position=N``.

- Consumes/Produces: XML, JSON
- Body (JSON, bare example):

  .. code-block:: json

     {
       "name": "corporateLdap",
       "className": "org.geoserver.security.auth.LdapAuthenticationProvider",
       "userGroupServiceName": "ldapUsers"
     }

- Body (XML):

  .. code-block:: xml

     <org.geoserver.security.config.LdapAuthenticationProviderConfig>
       <name>corporateLdap</name>
       <className>org.geoserver.security.auth.LdapAuthenticationProvider</className>
       <userGroupServiceName>ldapUsers</userGroupServiceName>
     </org.geoserver.security.config.LdapAuthenticationProviderConfig>

Rules:
- Path name must equal payload ``name``
- ``className`` cannot change (omit to keep)
- ``position`` clamped to list bounds; if omitted, order unchanged

``DELETE /security/authproviders/{providerName}``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Delete a provider and remove it from the active order.

- Produces: XML, JSON
- Response: ``200`` (empty body)

``PUT /security/authproviders/order``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Replace the **active order**.

- Consumes/Produces: XML, JSON
- Body (JSON):

  .. code-block:: json

     { "order": ["corporateLdap", "default"] }

- Body (XML):

  .. code-block:: xml

     <order>
       <order>corporateLdap</order>
       <order>default</order>
     </order>

Semantics:
- Names **listed** → **enabled** (in order)
- Names **omitted** → **disabled** (config remains on disk)

Validation:
- The list must be non-empty
- All names must correspond to known provider configs

Operational notes
-----------------

- All write operations persist to the security XML and **reload** the security manager.
- Writes are serialized to avoid concurrent update issues.

OpenAPI
-------

See the :api:`Authentication-provider OpenAPI spec <authenticationproviders.yaml>`
for schemas and machine-readable definitions.
