Auth Providers
==============

The REST API allows you to list, create, upload, update, and delete authProviders in GeoServer.

.. note:: Read the :api:`API reference for security/authProviders <authenticationfilters.yaml>`.

View an Authentication Provider
-------------------------------

*Request*

.. admonition:: curl

    curl --request GET --location 'http://localhost:8080/geoserver/rest/security/authProviders/default' \
    --header 'Accept: application/xml' \
    --header 'Authorization: Basic XXXXXXX'

*Response*
Status: 200
Content-type: application/xml

.. code-block:: xml

    <authProvider>
        <id>7787843a:196d38d0a14:-7fcc</id>
        <name>7787843a:196d38d0a14:-7fcc</name>
        <className>org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig</className>
        <userGroupServiceName>default</userGroupServiceName>
        <position>0</position>
        <config class="org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig">
            <id>7787843a:196d38d0a14:-7fcc</id>
            <name>default</name>
            <className>org.geoserver.security.auth.UsernamePasswordAuthenticationProvider</className>
            <userGroupServiceName>default</userGroupServiceName>
        </config>
        <disabled>false</disabled>
    </authProvider>

200 OK


Update an Authentication Provider
---------------------------------

.. admonition:: curl

    curl --request PUT --location  'http://localhost:9002/geoserver/rest/security/authProviders/Test_Provider2' \
    --header 'Accept: application/xml' \
    --header 'Content-Type: application/xml' \
    --header 'Authorization: ••••••' \
    --data '<authProvider>
    <name>Test_Provider2</name>
    <className>org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig</className>
    <userGroupServiceName>default</userGroupServiceName>
    <position>1</position>
    <config class="org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig">
    <name>default</name>
    <className>org.geoserver.security.auth.UsernamePasswordAuthenticationProvider</className>
    <userGroupServiceName>default</userGroupServiceName>
    </config>
    <disabled>false</disabled>
    </authProvider>'

*Response*

200 OK


Delete an Authentication Provider
---------------------------------

*Response*

.. admonition:: curl

    curl --request DELETE --location  'http://localhost:9002/geoserver/rest/security/authProviders/Test_Provider13' \
    --header 'Accept: application/xml' \
    --header 'Authorization: ••••••'

*Response*

200 OK

<HttpStatus>OK</HttpStatus>

Create an Authentication Provider
---------------------------------

*Response*

.. admonition:: curl

    curl --location 'http://localhost:9002/geoserver/rest/security/authProviders' \
    --header 'Content-type: application/xml' \
    --header 'Accept: application/xml' \
    --header 'Authorization: ••••••' \
    --data '
    <authProvider>
    <name>Test_Provider13</name>
    <className>org.geoserver.security.auth.UsernamePasswordAuthenticationProvider</className>
    <userGroupServiceName>default</userGroupServiceName>
    <position>1</position>
    <config class="org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig">
    <userGroupServiceName>default</userGroupServiceName>
    </config>
    <disabled>false</disabled>
    </authProvider>'


@Response*
Status: 201 Created
Location: http://localhost:9002/geoserver/rest/security/authProvider/Test_Provider13
Content-Type: application/xml

List all Authentication Providers
---------------------------------

.. admonition:: curl

    curl --location 'http://localhost:9002/geoserver/rest/security/authProviders' \
    --header 'Accept: application/xml' \
    --header 'Authorization: ••••••'

200 OK

.. code-block:: xml

    <authProviders>
        <authProvider>
            <name>Test_Provider10</name>
            <atom:link xmlns:atom="http://www.w3.org/2005/Atom" rel="alternate" href="http://localhost:9002/geoserver/rest/security/authProviders/Test_Provider10.xml" type="application/atom+xml"/>
        </authProvider>
        <authProvider>
            <name>Test_Provider11</name>
            <atom:link xmlns:atom="http://www.w3.org/2005/Atom" rel="alternate" href="http://localhost:9002/geoserver/rest/security/authProviders/Test_Provider11.xml" type="application/atom+xml"/>
        </authProvider>
    </authProviders>
