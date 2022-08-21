.. _group_quickstart:

Publishing a Layer Group
========================

This tutorial walks through the steps of publishing a layer group combing several layers into a basemap.

.. note:: This tutorial assumes that GeoServer is running at ``http://localhost:8080/geoserver``.

Data preparation
----------------

First let's gather that the data that we'll be publishing.

#. Complete the previous tutorials:
   
   * :ref:`geopkg_quickstart` defining the `tutorial:countries` layer
   * :ref:`image_quickstart` defining the `tutorial:shaded` layer
       

Create a layer group
--------------------

#. Navigate to :menuselection:`Data > Layer Group` page.

   .. figure:: images/groups.png
      
      Layer Groups
    
#. This page displays a list of layer groups, workspace that the group belongs to.
   
   .. note:: Layer groups are allowed to be "global" allowing a map to be created combing layers from several workspaces into a single visual.
   
#. At the top of the list :guilabel:`Layer Groups` locate and click the :guilabel:`Add new layer group` link.
   
#. The :guilabel:`Layer group` editor defines 
   
   * :guilabel:`Basic Resource Info` - describes how the layer is presented to others
   * :guilabel:`Coordinate Reference System` - establishes how the spatial data is to be interpreted or drawn on the world
   * :guilabel:`Bounding Boxes` - establishes where the dataset is located in the world
   * :guilabel:`Layers` - the layers to be drawn (listed in draw order)
   
#. Locate :guilabel:`Basic Resource Info` and define the layer:

   .. list-table::
      :widths: 30 70
      :width: 100%
      :stub-columns: 1

      * - Name
        - :kbd:`basemap`
      * - Title
        - :kbd:`Basemap`
      * - Abstract
        - :kbd:`Plain basemap suitable as a backdrop for geospatial data.`
      * - Workspace
        - ``tutorial``
   
   .. figure:: images/basemap.png
      
      Basic resource information
      
#. Scroll down to the :guilabel:`Layers` list which is presently empty.

#. Click :guilabel:`Add Layer` link, select the ``tutorial:shaded`` layer first.
   
   The raster should be drawn first, as other content will be shown over top of it.
   
#. Click :guilabel:`Add Layer` link, select the ``tutorial:countries`` layer second.

   This polygon layer will be drawn second.
   
#. Locate the ``tutorial:countries`` layer in the list and click the :guilabel:`Style` entry to change ``polygon`` to ``line``.

   By drawing only the outline of the countries the shaded relief can show through. 
   
   
   .. figure:: images/layers.png
      
      Layer group layers in drawing order
   
#. Locate the :guilabel:`Coordiante Reference Systems` and press :guilabel:`Generate Bounds`.
   
   Now that layers are listed we they can be used to determine the corodinate reference system and bounds of the layer group.
   
   .. figure:: images/layers_crs.png 
      
      Coordinate Reference Systems
      
#. Press :guilabel:`Save` complete your layer group.

Previewing the layer
--------------------

In order to verify that the ``tutorial:basemap`` layer is published correctly, we can preview the layer.

#. Navigate to the :menuselection:`Data > Layer Preview` page and find the ``tutorial:basemap`` layer.

   .. note:: Use the :guilabel:`Search` field with :kbd:`tutorial` as shown to limit the number of layers to page through.

#. Click the :guilabel:`OpenLayers` link in the :guilabel:`Common Formats` column.

#. An OpenLayers map will load in a new tab. This preview is used to zoom and pan around the dataset, as well as display the attributes of features.

   .. figure:: images/openlayers.png

      Preview basemap
