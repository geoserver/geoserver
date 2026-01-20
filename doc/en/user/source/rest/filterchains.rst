.. _rest_filterchains:

Filter Chains
=============

The REST API allows you to list, create, upload, update, and delete filterChains in GeoServer.

.. note:: Read the :api:`API reference for security/filterChains <filterchains.yaml>`.

View a Filter Chain
-------------------

*Request*

.. admonition:: curl

    curl --location 'http://localhost:9002/geoserver/rest/security/filterChains/web-test-1' \
        --header 'Accept: application/xml' \
        --header 'Authorization: XXXXXX'

*Response*

200 OK

.. code-block:: xml

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


Update a filter chain
---------------------

.. admonition:: curl

    curl --location --request PUT 'http://localhost:9002/geoserver/rest/security/filterChains/web-test-2' \
    --header 'Content-Type: application/xml' \
    --header 'Authorization: XXXXXX' \
    --data @request.xml

.. code-block:: xml

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

*Response*

200 OK

Delete an Authentication Filter
-------------------------------

*Response*

.. admonition:: curl

    curl --location --request DELETE 'http://localhost:9002/geoserver/rest/security/filterChains/web-test-2' \
    --header 'Authorization: XXXXXX'

*Response*

200 OK

Create an Authentication Filter
-------------------------------

*Request*

.. admonition:: curl

    curl --location --request POST 'http://localhost:9002/geoserver/rest/security/filterChains' \
    --header 'Content-Type: application/xml' \
    --header 'Authorization: XXXXXX' \
    --data @request.xml

.. code-block:: xml

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

*Response*

201 Created
Content-Type: text/plain
Location: "http://localhost:9002/geoserver/rest/security/filterChains/web-test-2"



List all Authentication Filters
-------------------------------

.. admonition:: curl

    curl --location 'http://localhost:9002/geoserver/rest/security/filterChains' \
    --header 'Accept: application/xml' \
    --header 'Authorization: XXXXXX'

200 OK

.. code-block:: xml

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


Configuring Allowed Authentication Filter Chain Classes
========================================================

GeoServer uses reflection to instantiate authentication filter chain
implementations defined in the security configuration.
To reduce the risk of loading unexpected or unsupported classes,
GeoServer enforces an allow-list of authentication filter chain classes.

Default behavior
----------------

By default, GeoServer allows the built-in authentication filter chain
implementations shipped with the platform, including:

* ``org.geoserver.security.HtmlLoginFilterChain``
* ``org.geoserver.security.ConstantFilterChain``
* ``org.geoserver.security.LogoutFilterChain``
* ``org.geoserver.security.ServiceLoginFilterChain``
* ``org.geoserver.security.VariableFilterChain``

These default classes are always permitted.

Extending the allow-list
------------------------

Installations that provide custom authentication filter chain
implementations can extend the allow-list using either a system property
or an environment variable.

The configured values are **merged** with the built-in allow-list.

**System property**

::

  -Dgeoserver.security.allowedAuthFilterChainClasses=\
  com.example.geoserver.security.CustomChain,\
  com.example.geoserver.security.*

**Environment variable**

::

  GEOSERVER_SECURITY_ALLOWED_AUTH_FILTERCHAIN_CLASSES=\
  com.example.geoserver.security.CustomChain,\
  com.example.geoserver.security.*

The value is a comma-separated list of:

* Fully qualified class names, or
* Package prefixes ending in ``.*``

Security notes
--------------

If a class name is not present in the allow-list, GeoServer will reject
the configuration and fail to instantiate the filter chain.

This mechanism prevents the reflective construction of unexpected classes
and mitigates risks associated with unsafe reflection, while
preserving extensibility for custom deployments.
