.. _rest_about:

About
=====

The REST API allows you to set and retrieve information about the server itself.

.. note:: Read the `API reference for /about/manifests <manifests.yaml>__`.

Retrieving component versions
-----------------------------

**Retrieve the versions of the main components: GeoServer, GeoTools, and GeoWebCache**

*Request*

.. admonition:: curl

   ::

     curl -v -u admin:geoserver -XGET -H "Accept: text/xml" 
       http://localhost:8080/geoserver/rest/about/version.xml

*Response*

.. code-block:: xml

  <about>
   <resource name="GeoServer">
    <Build-Timestamp>11-Dec-2012 17:55</Build-Timestamp>
    <Git-Revision>e66f8da85521f73d0fd00b71331069a5f49f7865</Git-Revision>
    <Version>2.3-SNAPSHOT</Version>
   </resource>
   <resource name="GeoTools">
    <Build-Timestamp>04-Dec-2012 02:31</Build-Timestamp>
    <Git-Revision>380a2b8545ee9221f1f2d38a4f10ef77a23bccae</Git-Revision>
    <Version>9-SNAPSHOT</Version>
   </resource>
   <resource name="GeoWebCache">
    <Git-Revision>2a534f91f6b99e5120a9eaa5db62df771dd01688</Git-Revision>
    <Version>1.3-SNAPSHOT</Version>
   </resource>
  </about>

Retrieving manifests
--------------------

**Retrieve the full manifest and subsets of the manifest as known to the ClassLoader**

*Request*

.. admonition:: curl

   ::

     curl -v -u admin:geoserver -XGET -H "Accept: text/xml"
       http://localhost:8080/geoserver/rest/about/manifest.xml

.. note:: The result will be a very long list of manifest information. While this can be useful, it is often desirable to filter this list.

**Retrieve manifests, filtered by resource name**

.. note:: This example will retrieve only resources where the ``name`` attribute matches ``gwc-.*``.

*Request*

.. admonition:: curl

   ::

     curl -v -u admin:geoserver -XGET -H "Accept: text/xml"
       http://localhost:8080/geoserver/rest/about/manifest.xml?manifest=gwc-.*

*Response*

.. code-block:: xml

  <about>
    <resource name="gwc-2.3.0">
      ...
    </resource>
    <resource name="gwc-core-1.4.0">
      ...
    </resource>
    <resource name="gwc-diskquota-core-1.4.0">
      ...
    </resource>
    <resource name="gwc-diskquota-jdbc-1.4.0">
      ...
    </resource>
    <resource name="gwc-georss-1.4.0">
      ...
    </resource>
    <resource name="gwc-gmaps-1.4.0">
      ...
    </resource>
    <resource name="gwc-kml-1.4.0">
      ...
    </resource>
    <resource name="gwc-rest-1.4.0">
      ...
    </resource>
    <resource name="gwc-tms-1.4.0">
      ...
    </resource>
    <resource name="gwc-ve-1.4.0">
      ...
    </resource>
    <resource name="gwc-wms-1.4.0">
      ...
    </resource>
    <resource name="gwc-wmts-1.4.0">
      ...
    </resource>
  </about>


**Retrieve manifests, filtered by resource property**

.. note:: This example will retrieve only resources with a property equal to ``GeoServerModule``.

*Request*

.. admonition:: curl

   ::

      curl -u admin:geoserver -XGET -H "Accept: text/xml"
        http://localhost:8080/geoserver/rest/about/manifest.xml?key=GeoServerModule

*Response*

.. code-block:: xml

  <about>
   <resource name="control-flow-2.3.0">
    <GeoServerModule>extension</GeoServerModule>
    ...
   </resource>
   ...
   <resource name="wms-2.3.0">
    <GeoServerModule>core</GeoServerModule>
    ...
   </resource>
  </about>


**Retrieve manifests, filtered by both resource name and property**

.. note:: This example will retrieve only resources where a property with named ``GeoServerModule`` has a value equal to ``extension``.\

*Request*

.. admonition:: curl

   ::

       curl -u admin:geoserver -XGET -H "Accept: text/xml"
         http://localhost:8080/geoserver/rest/about/manifest.xml?key=GeoServerModule&value=extension

