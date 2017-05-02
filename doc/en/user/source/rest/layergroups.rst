.. _rest_layergroups:

Layer groups
============

The REST API allows you to create and modify layer groups in GeoServer.

.. note:: The examples below specify global layer groups, but the examples will work in a workspace-specific construction as well.

.. note:: Read the :api:`API reference for /layergroups <layergroups.yaml>`.

Creating a layer group
----------------------

**Create a new layer group based on already-published layers**


Given the following content saved as :file:`nycLayerGroup.xml`:

.. code-block:: xml

   <layerGroup>
     <name>nyc</name>
     <layers>
       <layer>roads</layer>
       <layer>parks</layer>
       <layer>buildings</layer>
     </layers>
     <styles>
       <style>roads_style</style>
       <style>polygon</style>
       <style>polygon</style>
     </styles>
   </layerGroup>

*Request*

.. admonition:: curl

   ::

       curl -v -u admin:geoserver -XPOST -d @nycLayerGroup.xml -H "Content-type: text/xml" 
         http://localhost:8080/geoserver/rest/layergroups

*Response*

::

   201 Created

.. note::

   This layer group can be viewed with a WMS GetMap request::

     http://localhost:8080/geoserver/wms/reflect?layers=nyc