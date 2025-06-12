Auth Filters
============

The REST API allows you to list, create, upload, update, and delete authFilters in GeoServer.

.. note:: Read the :api:`API reference for security/authFilters <authenticationfilters.yaml>`.

View an Authentication Filter
-----------------------------

*Request*

.. admonition:: curl

    curl --location 'http://localhost:8080/geoserver/rest/security/authFilters/restInterceptor' \
    --header 'Accept: application/xml' \
    --header 'Authorization: Basic XXXXXXX'

*Response*

200 OK

.. code-block:: xml

    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
    <authFilter>
        <config xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="securityInterceptorFilterConfig">
            <className>org.geoserver.security.filter.GeoServerSecurityInterceptorFilter</className>
            <allowIfAllAbstainDecisions>false</allowIfAllAbstainDecisions>
            <securityMetadataSource>restFilterDefinitionMap</securityMetadataSource>
        </config>
        <id>-3737ce2b:196b56d5575:-7fea</id>
        <name>restInterceptor</name>
    </authFilter>


Update an Authentication Filter
-------------------------------

.. admonition:: curl

    curl --location --request PUT 'http://localhost:8080/geoserver/rest/security/authFilters/restInterceptor' \
    --header 'Content-Type: application/xml' \
    --header 'Authorization: ••••••' \
    --data '
    <authFilter>
    <config xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="securityInterceptorFilterConfig">
    <className>org.geoserver.security.filter.GeoServerSecurityInterceptorFilter</className>
    <allowIfAllAbstainDecisions>true</allowIfAllAbstainDecisions>
    <securityMetadataSource>restFilterDefinitionMap</securityMetadataSource>
    </config>
    <id>-3737ce2b:196b56d5575:-7fea</id>
    <name>restInterceptor</name>
    </authFilter>'

*Response*

200 OK

Delete an Authentication Filter
-------------------------------

*Response*

.. admonition:: curl

    curl --location --request DELETE 'http://localhost:8080/geoserver/rest/security/authFilters/restInterceptor7' \
    --header 'Authorization: ••••••'

*Response*

200 OK

Create an Authentication Filter
-------------------------------

*Response*

.. admonition:: curl

    curl --location 'http://localhost:8080/geoserver/rest/security/authFilters.xml' \
    --header 'content-type: application/xml' \
    --header 'Authorization: ••••••' \
    --data '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
    <authFilter>
    <name>restInterceptor17</name>
    <className>org.geoserver.security.config.SecurityInterceptorFilterConfig</className>
    <config xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="securityInterceptorFilterConfig">
    <className>org.geoserver.security.filter.GeoServerSecurityInterceptorFilter</className>
    <allowIfAllAbstainDecisions>false</allowIfAllAbstainDecisions>
    <securityMetadataSource>restFilterDefinitionMap</securityMetadataSource>
    </config>
    </authFilter>'

201 Created


List all Authentication Filters
-------------------------------

.. admonition:: curl

    curl --location 'http://localhost:8080/geoserver/rest/security/authFilters' \
    --header 'Accept: application/xml' \
    --header 'Authorization: ••••••'

200 OK

.. code-block:: xml

    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
    <authFilters>
        <authFilter>
            <config xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="anonymousAuthenticationFilterConfig">
                <className>org.geoserver.security.filter.GeoServerAnonymousAuthenticationFilter</className>
            </config>
            <id>-3737ce2b:196b56d5575:-7fed</id>
            <name>anonymous</name>
        </authFilter>
        ...
        <authFilter>
            <config xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="sslFilterConfig">
                <className>org.geoserver.security.filter.GeoServerSSLFilter</className>
                <sslPort>443</sslPort>
            </config>
            <id>-3737ce2b:196b56d5575:-7fe4</id>
            <name>sslFilter</name>
        </authFilter>
    </authFilters>
