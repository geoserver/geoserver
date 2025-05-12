.. _rest_api_authfilters:

Auth Filters
==============

.. _security_authfilters:

``/security/authFilters``
----------------------------------

Adds or Lists the authentication filters in the geoserver systems


.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status code
     - Formats
     - Default Format
   * - GET
     - List all auth filters in the system
     - 200,403,500
     - XML, JSON
     -
   * - POST
     - Great a new authFilter
     - 200,400,403,500
     - XML, JSON
     -

Formats:

**XML**

For Get (List - Response)

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
    </authFilters>

For Post (Create - Request)

.. code-block:: xml

    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
    <authFilter>
        <name>restInterceptor14</name>
        <className>org.geoserver.security.config.SecurityInterceptorFilterConfig</className>
        <config xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="securityInterceptorFilterConfig">
            <className>org.geoserver.security.filter.GeoServerSecurityInterceptorFilter</className>
            <allowIfAllAbstainDecisions>false</allowIfAllAbstainDecisions>
            <securityMetadataSource>restFilterDefinitionMap</securityMetadataSource>
        </config>
    </authFilter>

For Post (Create - Response)

.. code-block:: xml

    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
    <authFilter>
        <config xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="securityInterceptorFilterConfig">
            <className>org.geoserver.security.filter.GeoServerSecurityInterceptorFilter</className>
            <allowIfAllAbstainDecisions>false</allowIfAllAbstainDecisions>
            <securityMetadataSource>restFilterDefinitionMap</securityMetadataSource>
        </config>
        <id>2d3ea9bb:196c91945a2:-8000</id>
        <name>restInterceptor14</name>
    </authFilter>

**JSON**

For Get (list)

.. code-block:: json

      {
      "filters": [
        {
          "id": "-3737ce2b:196b56d5575:-7fed",
          "name": "anonymous",
          "config": {
            "@class": "org.geoserver.security.config.AnonymousAuthenticationFilterConfig",
            "className": "org.geoserver.security.filter.GeoServerAnonymousAuthenticationFilter"
          }
        }
      ]}

For Post (create - request)

.. code-block:: json

    {
        "authFilter": {
            "name": "restInterceptor15",
            "config": {
                "@class": "org.geoserver.security.config.SecurityInterceptorFilterConfig",
                "className": "org.geoserver.security.filter.GeoServerSecurityInterceptorFilter",
                "allowIfAllAbstainDecisions": true,
                "securityMetadataSource": "restFilterDefinitionMap"
            }
        }
    }

For Post (create - response)

.. code-block:: json

    {
        "id": "2d3ea9bb:196c91945a2:-7ffe",
        "name": "restInterceptor16",
        "config": {
            "@class": "org.geoserver.security.config.SecurityInterceptorFilterConfig",
            "className": "org.geoserver.security.filter.GeoServerSecurityInterceptorFilter",
            "allowIfAllAbstainDecisions": true,
            "securityMetadataSource": "restFilterDefinitionMap"
        }
    }

Exceptions
~~~~~~~~~~

.. list-table::
   :header-rows: 1

   * - Exception
     - Status code
   * - Malformed request
     - 400
   * - No administrative privileges
     - 403
   * - Internal Server Error
     - 500


.. _security_authfilters_authfilter:

``/security/authFilters/{authFilter}``
--------------------------------------

View, Update or Delete an existing auth filter


.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status code
     - Formats
     - Default Format
   * - GET
     - View the details of an authentication filter on the geoserver
     - 200,403,404,500
     - XML, JSON
     -
   * - PUT
     - Update the details of an authentication filter on the geoserver
     - 200,400,403,404,500
     - XML, JSON
     -
   * - DELETE
     - Update the details of an authentication filter on the geoserver
     - 200,403,410,500
     -
     -


Formats:

**XML**

For PUT and GET

.. code-block:: xml

  <authFilter>
    <id>-2bf62d17:196c4deaf9b:-7fff</id>
    <name>restInterceptor9</name>
    <className>org.geoserver.security.config.SecurityInterceptorFilterConfig</className>
    <config xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="securityInterceptorFilterConfig">
        <className>org.geoserver.security.filter.GeoServerSecurityInterceptorFilter</className>
        <allowIfAllAbstainDecisions>false</allowIfAllAbstainDecisions>
        <securityMetadataSource>restFilterDefinitionMap</securityMetadataSource>
    </config>
  </authFilter>


**JSON**

For PUT and GET

.. code-block:: json

    {
        "authFilter": {
            "id": "-3abefb99:196c5207331:-7ffe",
            "name": "restInterceptor13",
            "config": {
                "@class": "org.geoserver.security.config.SecurityInterceptorFilterConfig",
                "className": "org.geoserver.security.filter.GeoServerSecurityInterceptorFilter",
                "allowIfAllAbstainDecisions": true,
                "securityMetadataSource": "restFilterDefinitionMap"
            }
        }
    }



Exceptions
~~~~~~~~~~

.. list-table::
   :header-rows: 1

   * - Exception
     - Status code
   * - Malformed request
     - 400
   * - No administrative privileges
     - 403
   * - Authentication filter not found
     - 404
   * - Gone - On Delete Only
     - 410
   * - Internal Server Error
     - 500
