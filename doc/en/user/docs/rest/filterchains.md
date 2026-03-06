---
render_macros: true
---

# Filter Chains

The REST API allows you to list, create, upload, update, and delete filterChains in GeoServer.

!!! note

    Read the [API reference for security/filterChains]({{ api_url }}/filterchains.yaml).

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
