
Auth Providers
==============

The Security REST service lets administrators **list, create, update and
delete** authentication‑provider configurations.

Each provider is represented as a single element/property whose name is
the *fully‑qualified class name* of the configuration, e.g.

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

------------------------------------------------------------------------
View an authentication provider
------------------------------------------------------------------------

*Request*

.. admonition:: curl

   curl -u admin:•••••• \
        -H "Accept: application/xml" \
        http://localhost:8080/geoserver/rest/security/authProviders/default

*Response (200 OK — XML)*

.. code-block:: xml

   <org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig>
     <id>52857278:13c7ffd66a8:-7ff0</id>
     <name>default</name>
     <className>org.geoserver.security.auth.UsernamePasswordAuthenticationProvider</className>
     <userGroupServiceName>default</userGroupServiceName>
   </org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig>

------------------------------------------------------------------------
Create an authentication provider
------------------------------------------------------------------------

*Request*

.. admonition:: curl

   curl -u admin:•••••• \
        -X POST \
        -H "Content-Type: application/xml" \
        -H "Accept: application/xml" \
        --data-binary @- \
        http://localhost:8080/geoserver/rest/security/authProviders <<'EOF'
   <org.geoserver.security.config.LdapAuthenticationProviderConfig>
     <name>corporateLdap</name>                  <!-- id omitted on POST -->
     <className>org.geoserver.security.auth.LdapAuthenticationProvider</className>
     <userGroupServiceName>ldapUsers</userGroupServiceName>
   </org.geoserver.security.config.LdapAuthenticationProviderConfig>
   EOF

*Response (201 Created)*

```
Location: /geoserver/rest/security/authProviders/corporateLdap
Content-Type: application/xml
```

.. code-block:: xml

   <org.geoserver.security.config.LdapAuthenticationProviderConfig>
     <id>9abcc9d4:e30aef012cf:-7fe0</id>
     <name>corporateLdap</name>
     <className>org.geoserver.security.auth.LdapAuthenticationProvider</className>
     <userGroupServiceName>ldapUsers</userGroupServiceName>
   </org.geoserver.security.config.LdapAuthenticationProviderConfig>

------------------------------------------------------------------------
Update an authentication provider
------------------------------------------------------------------------

.. admonition:: curl

   curl -u admin:•••••• \
        -X PUT \
        -H "Content-Type: application/xml" \
        -H "Accept: application/xml" \
        --data-binary @- \
        http://localhost:8080/geoserver/rest/security/authProviders/default <<'EOF'
   <org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig>
     <id>52857278:13c7ffd66a8:-7ff0</id>         <!-- id *required* on PUT -->
     <name>default</name>
     <className>org.geoserver.security.auth.UsernamePasswordAuthenticationProvider</className>
     <userGroupServiceName>default</userGroupServiceName>
   </org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig>
   EOF

*Response (200 OK — updated provider echoed)*

------------------------------------------------------------------------
Delete an authentication provider
------------------------------------------------------------------------

.. admonition:: curl

   curl -u admin:•••••• \
        -X DELETE \
        http://localhost:8080/geoserver/rest/security/authProviders/corporateLdap

*Response (200 OK — empty body)*

------------------------------------------------------------------------
List all authentication providers
------------------------------------------------------------------------

.. admonition:: curl

   curl -u admin:•••••• \
        -H "Accept: application/xml" \
        http://localhost:8080/geoserver/rest/security/authProviders

*Response (200 OK — XML)*

.. code-block:: xml

   <authProviders>
     <org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig>
       <id>52857278:13c7ffd66a8:-7ff0</id>
       <name>default</name>
       <className>org.geoserver.security.auth.UsernamePasswordAuthenticationProvider</className>
       <userGroupServiceName>default</userGroupServiceName>
     </org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig>

     <org.geoserver.security.config.LdapAuthenticationProviderConfig>
       <id>9abcc9d4:e30aef012cf:-7fe0</id>
       <name>corporateLdap</name>
       <className>org.geoserver.security.auth.LdapAuthenticationProvider</className>
       <userGroupServiceName>ldapUsers</userGroupServiceName>
     </org.geoserver.security.config.LdapAuthenticationProviderConfig>
   </authProviders>


------------------------------------------------------------------------
Re‑order / enable / disable providers
------------------------------------------------------------------------

The special ``/security/authProviders/order`` endpoint takes a *single*
list of provider **names**.  
The order of the list becomes the **execution order**; any providers
*missing* from the list are **disabled** (but kept on disk).

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

Enable *corporateLdap* and make it first:

.. admonition:: curl (JSON body)

   curl -u admin:•••••• \
        -X PUT \
        -H "Content-Type: application/json" \
        http://localhost:8080/geoserver/rest/security/authProviders/order <<'EOF'
   { "order": ["corporateLdap", "default"] }
   EOF

Same request in XML:

.. code-block:: xml

   <order>
     <order>corporateLdap</order>
     <order>default</order>
   </order>

Disable *corporateLdap* again:

.. code-block:: json

   { "order": ["default"] }
