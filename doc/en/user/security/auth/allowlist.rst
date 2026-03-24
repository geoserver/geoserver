.. _security_auth_allowlist:

Authentication REST Class Allowlist
===================================

GeoServer can restrict which authentication-related classes may be created by the REST security configuration endpoints.

This protection applies to reflective class loading used when REST requests define authentication filter chain classes or authentication provider classes. The allowlists are intended to reduce the risk of creating unexpected classes from request input.

Two separate allowlists are available:

* one for authentication filter chain classes
* one for authentication provider and provider configuration classes

The configuration accepts comma-separated values. Supported syntax depends on the specific allowlist.

Authentication Filter Chain Allowlist
-------------------------------------

This allowlist is used by both:

* the XML/domain path for authentication filter chains
* the REST controller path that creates or updates authentication filter chains

Configuration keys:

* System property: ``geoserver.security.allowedAuthFilterChainClasses``
* Environment variable: ``GEOSERVER_SECURITY_ALLOWED_AUTH_FILTERCHAIN_CLASSES``

Default allowed classes:

* ``org.geoserver.security.HtmlLoginFilterChain``
* ``org.geoserver.security.ConstantFilterChain``
* ``org.geoserver.security.LogoutFilterChain``
* ``org.geoserver.security.ServiceLoginFilterChain``
* ``org.geoserver.security.VariableFilterChain``

Supported values:

* exact fully qualified class names
* package prefixes ending in ``.*``

Example prefix value::

  geoserver.security.allowedAuthFilterChainClasses=com.example.security.*

Authentication Provider Allowlist
---------------------------------

This allowlist is used by the REST controller handling authentication provider configuration.

Configuration keys:

* System property: ``geoserver.security.allowedAuthenticationProviderClasses``
* Environment variable: ``GEOSERVER_SECURITY_ALLOWED_AUTHENTICATION_PROVIDER_CLASSES``

Default allowed classes:

* ``org.geoserver.security.auth.UsernamePasswordAuthenticationProvider``
* ``org.geoserver.security.jdbc.JDBCConnectAuthProvider``
* ``org.geoserver.security.ldap.LDAPAuthenticationProvider``
* ``org.geoserver.geoserver.authentication.auth.GeoFenceAuthenticationProvider``
* ``org.geoserver.security.auth.web.WebServiceAuthenticationProvider``
* ``org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig``
* ``org.geoserver.security.jdbc.config.JDBCConnectAuthProviderConfig``
* ``org.geoserver.security.ldap.LDAPSecurityServiceConfig``
* ``org.geoserver.geoserver.authentication.auth.GeoFenceAuthenticationProviderConfig``
* ``org.geoserver.security.auth.web.WebAuthenticationConfig``

How Configuration Is Applied
----------------------------

For each allowlist:

* GeoServer starts with the built-in defaults listed above
* if a system property is set, its values are added to the built-in defaults
* otherwise, if the environment variable is set, its values are added to the built-in defaults
* blank values are ignored

System properties take precedence over environment variables.

The configured values do not replace the defaults. They extend them.

Examples
--------

To allow a custom authentication filter chain class, add its fully qualified name to the filter chain allowlist.

Example using a system property::

  -Dgeoserver.security.allowedAuthFilterChainClasses=com.example.security.CustomFilterChain

Example using an environment variable::

  export GEOSERVER_SECURITY_ALLOWED_AUTH_FILTERCHAIN_CLASSES=com.example.security.CustomFilterChain

To allow a custom authentication provider and its configuration class:

System property::

  -Dgeoserver.security.allowedAuthenticationProviderClasses=com.example.security.CustomAuthenticationProvider,com.example.security.CustomAuthenticationProviderConfig

Environment variable::

  export GEOSERVER_SECURITY_ALLOWED_AUTHENTICATION_PROVIDER_CLASSES=com.example.security.CustomAuthenticationProvider,com.example.security.CustomAuthenticationProviderConfig

Notes
-----

* The filter chain allowlist accepts exact class names and package prefixes ending in ``.*``.
* The authentication provider allowlist accepts exact class names only. Package wildcards are not supported.
* A custom authentication provider typically requires both the provider class and its corresponding configuration class to be allowed.
* These allowlists only affect the REST security class-loading paths described above. They do not replace the normal type checks performed by GeoServer.
