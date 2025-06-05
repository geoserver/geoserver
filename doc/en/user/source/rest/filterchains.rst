Filter Chains
=============

The REST API allows you to list, create, upload, update, and delete filterChains in GeoServer.

.. note:: Read the :api:`API reference for security/filterChains <filterchains.yaml>`.

View a Filter Chain
-----------------------------

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
    --header 'Accept: application/xml' \
    --header 'Content-Type: application/xml' \
    --header 'Authorization: XXXXXX' \
    --data @request.xml


*Response*

200 OK

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

Delete an Authentication Filter
-------------------------------

*Response*

.. admonition:: curl

    curl --location --request DELETE 'http://localhost:9002/geoserver/rest/security/filterChains/web-test-2' \
    --header 'ACCept: application/json' \
    --header 'Authorization: XXXXXX'

*Response*

200 OK

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

Create an Authentication Filter
-------------------------------

*Response*

.. admonition:: curl

    curl --location 'http://localhost:9002/geoserver/rest/security/filterChains' \
    --header 'Accept: application/xml' \
    --header 'Content-Type: application/xml' \
    --header 'Authorization: XXXXXX' \
    --data @request.xml

201 Created

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

List all Authentication Filters
-------------------------------

.. admonition:: curl

    curl --location 'http://localhost:9002/geoserver/rest/security/filterChains' \
    --header 'Accept: application/xml' \
    --header 'Authorization: XXXXXX'

200 OK

.. code-block:: xml

    <filterChainList>
        <filterChain>
            ...
        </filterChain>
        ...
        <filterChain>
            ...
        </filterChain>
    </filterChainList>

