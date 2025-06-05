uth Providers
=============

The REST API allows you to list, create, upload, update, and delete authProviders in GeoServer.

.. note:: Read the :api:`API reference for security/authProviders <authenticationfilters.yaml>`.

View an Authentication Provider
-------------------------------

*Request*

.. admonition:: curl

    curl --location 'http://localhost:8080/geoserver/rest/security/authProviders/default' \
    --header 'Accept: application/xml' \
    --header 'Authorization: Basic XXXXXXX'

*Response*

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

.. code-block:: xml


Update an Authentication Provider
---------------------------------

.. admonition:: curl

    curl --location --request PUT 'http://localhost:9002/geoserver/rest/security/authProviders/Test_Provider2' \
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

.. code-block:: xml

Delete an Authentication Provider
---------------------------------

*Response*

.. admonition:: curl

    curl --location --request DELETE 'http://localhost:9002/geoserver/rest/security/authProviders/Test_Provider13' \
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

201 Created

.. code-block:: xml

    <authProvider>
        <id>-3e8020b4:1973ebc2c56:-8000</id>
        <name>Test_Provider13</name>
        <className>org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig</className>
        <userGroupServiceName>default</userGroupServiceName>
        <position>0</position>
        <config class="org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig">
            <id>-3e8020b4:1973ebc2c56:-8000</id>
            <name>Test_Provider13</name>
            <className>org.geoserver.security.auth.UsernamePasswordAuthenticationProvider</className>
            <userGroupServiceName>default</userGroupServiceName>
        </config>
        <disabled>false</disabled>
    </authProvider>

List all Authentication Providers
---------------------------------

.. admonition:: curl

    curl --location 'http://localhost:9001/geoserver/rest/security/authProviders/default' \
    --header 'Accept: application/xml' \
    --header 'Authorization: Basic: XXXXX'

200 OK

.. code-block:: xml

    <authProviderList>
        <authProvider>
            <id>-4e7ef1a4:196d967dcea:-8000</id>
            <name>-4e7ef1a4:196d967dcea:-8000</name>
            <className>org.geoserver.security.jdbc.config.JDBCConnectAuthProviderConfig</className>
            <userGroupServiceName>default</userGroupServiceName>
            <position>-1</position>
            <config class="org.geoserver.security.jdbc.config.JDBCConnectAuthProviderConfig">
                <id>-4e7ef1a4:196d967dcea:-8000</id>
                <name>JDBC</name>
                <className>org.geoserver.security.jdbc.JDBCConnectAuthProvider</className>
                <driverClassName>org.postgresql.Driver</driverClassName>
                <connectURL>Jdbc::/postgresSQl</connectURL>
                <userGroupServiceName>default</userGroupServiceName>
            </config>
            <disabled>true</disabled>
        </authProvider>
        <authProvider>
            <id>7787843a:196d38d0a14:-7fcc</id>
            <name>7787843a:196d38d0a14:-7fcc</name>
            <className>org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig</className>
            <userGroupServiceName>default</userGroupServiceName>
            <position>-1</position>
            <config class="org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig">
                <id>7787843a:196d38d0a14:-7fcc</id>
                <name>default</name>
                <className>org.geoserver.security.auth.UsernamePasswordAuthenticationProvider</className>
                <userGroupServiceName>default</userGroupServiceName>
            </config>
            <disabled>true</disabled>
        </authProvider>
    </authProviderList>
