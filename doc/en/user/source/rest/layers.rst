.. _rest_layers:

Layers
======

The REST API allows you to list, create, upload, update, and delete layers in GeoServer.

.. note:: Read the :api:`API reference for /layers <layers.yaml>`.

Listing all layers
------------------

**List all layers on the server, in JSON format:**

*Request*

.. admonition:: curl

   ::

     curl -u admin:geoserver -XGET http://localhost:8080/geoserver/rest/layers.json

*Response*

::

   {
     "layers": {
        "layer": [
            {
                "name": "giant_polygon",
                "href": "http://localhost:8080/geoserver/rest/layers/giant_polygon.json"
            },
            {
                "name": "poi",
                "href": "http://localhost:8080/geoserver/rest/layers/poi.json"
            },
            ...
         ]
      }
   }

**List all styles in a workspace, in XML format:**

*Request*

.. admonition:: curl

   ::

     curl -u admin:geoserver -XGET http://localhost:8080/geoserver/rest/layers.xml

*Response*

.. code-block:: xml

   <layers>
     <layer>
       <name>giant_polygon</name>
       <atom:link xmlns:atom="http://www.w3.org/2005/Atom" rel="alternate" href="http://localhost:8080/geoserver/rest/layers/giant_polygon.xml" type="application/xml"/>
     </layer>
     <layer>
       <name>poi</name>
       <atom:link xmlns:atom="http://www.w3.org/2005/Atom" rel="alternate" href="http://localhost:8080/geoserver/rest/layers/poi.xml" type="application/xml"/>
     </layer>
     ...
   </layers>

