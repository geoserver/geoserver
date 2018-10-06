.. _rest_api_manifests:

Manifests
=========

GeoServer provides a REST service to expose a listing of all loaded JARs and resources on the running instance. This is useful for bug reports and to keep track of extensions deployed into the application. There are two endpoints for accessing this information:

* ``about/manifest``—Retrieves details on all loaded JARs
* ``about/version``—Retrieves details for the high-level components: GeoSever, GeoTools, and GeoWebCache
* ``about/status``-Retrieves details for the status of all loaded and configured modules


``/about/manifest[.<format>]``
------------------------------

This endpoint retrieves details on all loaded JARs.

All the GeoServer manifest JARs are marked with the property ``GeoServerModule`` and classified by type, so you can use filtering capabilities to search for a set of manifests using regular expressions (see the :ref:`manifest <rest_api_manifests_manifest>` parameter) or a type category (see the :ref:`key <rest_api_manifests_key>` and :ref:`value <rest_api_manifests_value>` parameter).

The available types are ``core``, ``extension``, or ``community``. To filter modules by a particular type, append a request with ``key=GeoServerModule&value=<type>``

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status Code
     - Formats
     - Default Format
     - Parameters
   * - GET
     - List all manifests into the classpath
     - 200
     - HTML, XML, JSON
     - HTML
     - :ref:`manifest <rest_api_manifests_manifest>`, :ref:`key <rest_api_manifests_key>`, :ref:`value <rest_api_manifests_value>`
   * - POST
     - 
     - 405
     - 
     - 
     -
   * - PUT
     - 
     - 405
     - 
     -
     -
   * - DELETE
     -
     - 405
     -
     -
     -

Usage
~~~~~


The model is very simple and is shared between the version and the resource requests to parse both requests.::

   <about>
     <resource name="{NAME}">
       <{KEY}>{VALUE}</{KEY}>
       ...
     </resource>
     ...
   </about>

You can customize the results adding a properties file called :file:`manifest.properties` into the root of the data directory.
Below is the default implementation that is used when no custom properties file is present::

   resourceNameRegex=.+/(.*).(jar|war)
   resourceAttributeExclusions=Import-Package,Export-Package,Class-Path,Require-Bundle
   versionAttributeInclusions=Project-Version:Version,Build-Timestamp,Git-Revision,
     Specification-Version:Version,Implementation-Version:Git-Revision

where:

* ``resourceNameRegex``—Group(1) will be used to match the attribute name of the resource.
* ``resourceAttributeExclusions``—Comma-separated list of properties to exclude (blacklist), used to exclude parameters that are too verbose such that the resource properties list is left open. Users can add their JARs (with custom properties) having the complete list of properties.
* ``versionAttributeInclusions``—Comma-separated list of properties to include (whitelist). Also supports renaming properties (using ``key:replace``) which is used to align the output of the ``versions`` request to the output of the web page. The model uses a map to store attributes, so the last attribute found in the manifest file will be used.


.. _rest_api_manifests_manifest:

``manifest``
^^^^^^^^^^^^

The ``manifest`` parameter is used to filter over resulting resource (manifest) names attribute using Java regular expressions.

.. _rest_api_manifests_key:

``key``
^^^^^^^

The ``key`` parameter is used to filter over resulting resource (manifest) properties name. It can be combined with the ``value`` parameter.

.. _rest_api_manifests_value:

``value``
^^^^^^^^^

The ``value`` parameter is used to filter over resulting resource (manifest) properties value. It can be combined with the ``key`` parameter.


``/about/version[.<format>]``
-----------------------------

This endpoint shows only the details for the high-level components: GeoServer, GeoTools, and GeoWebCache.

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status Code
     - Formats
     - Default Format
     - Parameters
   * - GET
     - List GeoServer, GeoWebCache and GeoTools manifests
     - 200
     - HTML, XML, JSON
     - HTML
     - :ref:`manifest <rest_api_manifests_manifest>`, :ref:`key <rest_api_manifests_key>`, :ref:`value <rest_api_manifests_value>`
   * - POST
     -
     - 405
     -
     -
     -
   * - PUT
     -
     - 405
     -
     -
     -
   * - DELETE
     -
     - 405
     -
     -
     -

``/about/status[.<format>]``
-----------------------------

This endpoint shows the status details of all installed and configured modules.Status details always include human readable name, and module name. Optional details include version, availability, status message, and links to documentation.

.. list-table::
   :header-rows: 1

   * - Method
     - Action
     - Status Code
     - Formats
     - Default Format
     - Parameters
   * - GET
     - List module statuses
     - 200
     - HTML, XML, JSON
     - HTML
     -
   * - POST
     -
     - 405
     - 
     - 
     -
   * - PUT
     - 
     - 405
     - 
     -
     -
   * - DELETE
     -
     - 405
     -
     -
     -
