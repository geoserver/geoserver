---
render_macros: true
---

# Filter Chains

The REST API allows you to list, create, upload, update, and delete filterChains in GeoServer.

!!! note
    Read the [API reference for security/filterChains]({{ api_url3 }}?urls.primaryName=Filter%20Chains).

## View a Filter Chain

*Request*

!!! abstract "curl"
    curl --location 'http://localhost:9002/geoserver/rest/security/filterChains/web-test-1' --header 'Accept: application/xml' --header 'Authorization: XXXXXX'

*Response*

200 OK

``` xml
<filterChainList>
    <filterChain>
        <name>web-test-1</name>
        <className>org.geoserver.security.HtmlLoginFilterChain</className>
        <patterns>
            <string>/web/**</string>
            <string>/gwc/rest/web/**</string>
            <string>/</string>
        </patterns>
        <filters>
            <string>rememberme</string>
            <string>form</string>
            <string>anonymous</string>
        </filters>
        <disabled>false</disabled>
        <allowSessionCreation>true</allowSessionCreation>
        <requireSSL>false</requireSSL>
        <matchHTTPMethod>false</matchHTTPMethod>
        <position>0</position>
    </filterChain>
</filterChainList>
```

## Update a filter chain

!!! abstract "curl"
    curl --location --request PUT 'http://localhost:9002/geoserver/rest/security/filterChains/web-test-2' --header 'Content-Type: application/xml' --header 'Authorization: XXXXXX' --data @request.xml

``` xml
<filterChain>
    <name>web-test-2</name>
    <className>org.geoserver.security.HtmlLoginFilterChain</className>
    <patterns>
        <string>/web/**</string>
        <string>/gwc/rest/web/**</string>
        <string>/</string>
    </patterns>
    <filters>
        <string>rememberme</string>
        <string>form</string>
        <string>anonymous</string>
    </filters>
    <disabled>false</disabled>
    <allowSessionCreation>true</allowSessionCreation>
    <requireSSL>false</requireSSL>
    <matchHTTPMethod>false</matchHTTPMethod>
    <position>1</position>
</filterChain>
```

*Response*

200 OK

## Delete an Authentication Filter

*Response*

!!! abstract "curl"
    curl --location --request DELETE 'http://localhost:9002/geoserver/rest/security/filterChains/web-test-2' --header 'Authorization: XXXXXX'

*Response*

200 OK

## Create an Authentication Filter

*Request*

!!! abstract "curl"
    curl --location --request POST 'http://localhost:9002/geoserver/rest/security/filterChains' --header 'Content-Type: application/xml' --header 'Authorization: XXXXXX' --data @request.xml

``` xml
<filterChain>
    <name>web-test-2</name>
    <className>org.geoserver.security.HtmlLoginFilterChain</className>
    <patterns>
        <string>/web/**</string>
        <string>/gwc/rest/web/**</string>
        <string>/</string>
    </patterns>
    <filters>
        <string>rememberme</string>
        <string>form</string>
        <string>anonymous</string>
    </filters>
    <disabled>false</disabled>
    <allowSessionCreation>true</allowSessionCreation>
    <requireSSL>false</requireSSL>
    <matchHTTPMethod>false</matchHTTPMethod>
    <position>1</position>
</filterChain>
```

*Response*

201 Created Content-Type: text/plain Location: "http://localhost:9002/geoserver/rest/security/filterChains/web-test-2"

## List all Authentication Filters

!!! abstract "curl"
    curl --location 'http://localhost:9002/geoserver/rest/security/filterChains' --header 'Accept: application/xml' --header 'Authorization: XXXXXX'

200 OK

``` xml
<filterChains>
    <filterChain>
        <name>web-test-2</name>
        <atom:link xmlns:atom="http://www.w3.org/2005/Atom" rel="alternate" href="http://localhost:8080/geoserver/rest/security/filterChains/web-test-2.xml" type="application/atom+xml"/>
    </filterChain>
    ...
    <filterChain>
        <name>web-test-5</name>
        <atom:link xmlns:atom="http://www.w3.org/2005/Atom" rel="alternate" href="http://localhost:8080/geoserver/rest/security/filterChains/web-test-5.xml" type="application/atom+xml"/>
    </filterChain>
</filterChains>
```

## Authentication REST Class Allowlist

GeoServer can restrict which authentication-related classes may be created by the REST security configuration endpoints.

This protection applies to reflective class loading used when REST requests define authentication filter chain classes
or authentication provider classes. The allowlists are intended to reduce the risk of creating unexpected classes from
request input.

Two separate allowlists are available:

- one for authentication filter chain classes
- one for authentication provider and provider configuration classes

The configuration accepts comma-separated values. Supported syntax depends on the specific allowlist.

### Authentication Filter Chain Allowlist

GeoServer uses reflection to instantiate authentication filter chain implementations defined in the security
configuration. To reduce the risk of loading unexpected or unsupported classes, GeoServer enforces an allow-list of
authentication filter chain classes.

This allowlist is used by both:

- the XML/domain path for authentication filter chains
- the REST controller path that creates or updates authentication filter chains

Configuration keys:

- System property: `geoserver.security.allowedAuthFilterChainClasses`
- Environment variable: `GEOSERVER_SECURITY_ALLOWED_AUTH_FILTERCHAIN_CLASSES`

Default allowed classes:

- `org.geoserver.security.HtmlLoginFilterChain`
- `org.geoserver.security.ConstantFilterChain`
- `org.geoserver.security.LogoutFilterChain`
- `org.geoserver.security.ServiceLoginFilterChain`
- `org.geoserver.security.VariableFilterChain`

Supported values:

- exact fully qualified class names
- package prefixes ending in `.*`

Example prefix value:

``` properties
geoserver.security.allowedAuthFilterChainClasses=com.example.security.*
```

If a class name is not present in the allow-list, GeoServer will reject the configuration and fail to instantiate the
filter chain.

This mechanism prevents the reflective construction of unexpected classes and mitigates risks associated with unsafe
reflection, while preserving extensibility for custom deployments.

### Authentication Provider Allowlist

This allowlist is used by the REST authentication provider endpoint for both JSON and XML payloads.

Configuration keys:

- System property: `geoserver.security.allowedAuthenticationProviderClasses`
- Environment variable: `GEOSERVER_SECURITY_ALLOWED_AUTHENTICATION_PROVIDER_CLASSES`

Default allowed classes:

- `org.geoserver.security.auth.UsernamePasswordAuthenticationProvider`
- `org.geoserver.security.jdbc.JDBCConnectAuthProvider`
- `org.geoserver.security.ldap.LDAPAuthenticationProvider`
- `org.geoserver.geoserver.authentication.auth.GeoFenceAuthenticationProvider`
- `org.geoserver.security.auth.web.WebServiceAuthenticationProvider`
- `org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig`
- `org.geoserver.security.jdbc.config.JDBCConnectAuthProviderConfig`
- `org.geoserver.security.ldap.LDAPSecurityServiceConfig`
- `org.geoserver.geoserver.authentication.auth.GeoFenceAuthenticationProviderConfig`
- `org.geoserver.security.auth.web.WebAuthenticationConfig`
- `org.geoserver.security.WebServiceBodyResponseSecurityProvider`
- `org.geoserver.security.WebServiceBodyResponseSecurityProviderConfig`

Supported values:

- exact fully qualified class names
- package prefixes ending in `.*`

### How Configuration Is Applied

For each allowlist:

- GeoServer starts with the built-in defaults listed above
- if a system property is set, its values are added to the built-in defaults
- otherwise, if the environment variable is set, its values are added to the built-in defaults
- blank values are ignored

System properties take precedence over environment variables.

The configured values do not replace the defaults. They extend them.

### Examples

To allow a custom authentication filter chain class, add its fully qualified name to the filter chain allowlist.

Example using a system property:

``` properties
-Dgeoserver.security.allowedAuthFilterChainClasses=com.example.security.CustomFilterChain
```

Example using an environment variable:

``` bash
export GEOSERVER_SECURITY_ALLOWED_AUTH_FILTERCHAIN_CLASSES=com.example.security.CustomFilterChain
```

To allow a custom authentication provider and its configuration class:

System property:

``` properties
-Dgeoserver.security.allowedAuthenticationProviderClasses=com.example.security.CustomAuthenticationProvider,com.example.security.CustomAuthenticationProviderConfig
```

Environment variable:

``` bash
export GEOSERVER_SECURITY_ALLOWED_AUTHENTICATION_PROVIDER_CLASSES=com.example.security.CustomAuthenticationProvider,com.example.security.CustomAuthenticationProviderConfig
```

To allow classes in a custom package:

``` properties
-Dgeoserver.security.allowedAuthenticationProviderClasses=com.example.security.*
```

### Migration Note

Before upgrading, deployments with custom authentication provider classes should configure the allowlist with those
provider and provider-config class names (or package prefixes) so create/update operations keep working after upgrade.

At startup, GeoServer logs the active authentication provider allowlist and warns if persisted provider configurations
reference classes outside the effective allowlist.

### Notes

- The filter chain allowlist accepts exact class names and package prefixes ending in `.*`.
- The authentication provider allowlist accepts exact class names and package prefixes ending in `.*`.
- A custom authentication provider typically requires both the provider class and its corresponding configuration class
  to be allowed.
- These allowlists only affect the REST security class-loading paths described above. They do not replace the normal
  type checks performed by GeoServer.
