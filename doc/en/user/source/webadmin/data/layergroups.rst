.. _webadmin_layergroups:

Layer Groups
============

A layer group is a group of layers that can be referred to by one name. This allows for simpler WMS requests, as the request need only refer to one layer as opposed to multiple individual layers. Layer groups act just like standard layers as far as WMS is concerned. 

.. figure:: ../images/data_layergroups.png
   :align: center

   *Layer Groups page*

Edit Layer Group
----------------

To bring up the layer group edit page, click a layer group name. The initial fields allow you configure the name,  bounds, and projection of the layer group. To automatically set bounding box, select the :guilabel:`Generate Bounds` button. You may also provide your own custom bounding box parameters. To select an appropriate projection click the :guilabel:`Find` button.

.. note:: A layer group can consist of layers with dissimilar bounds and projections. GeoServer will automatically reproject all layers to the projection of the layer group.

The table at the bottom pof the page lists the layers contained within the current layer group. When a layer group is processed, the layers are rendered in the order provided, so the layer at the bottom of list will be rendered last and will show on top of the other layers.

.. figure:: ../images/data_layergroups_edit.png
   :align: center

   *Layer Groups Edit page*

The :guilabel:`Style` column shows the style associated with each layer. To change the style associated with a layer, click the appropriate style link. A list of enabled styles will be displayed. Clicking on a style name reassigns the layer's style. 

.. figure:: ../images/data_layergroups_edit_styles.png
   :align: center
   
   *Style editing for a layer within a layer group*

To remove a layer from the layer group, select the layer's button in the :guilabel:`Remove` column. You will not be prompted to confirm or cancel this deletion.


You can view layers group in the :ref:`layerpreview` section of the web admin.

.. figure:: ../images/data_layergroups_tasmania.png
   :align: center 

   *Openlayers preview of the layer group "tasmania"*

A layer can be positioned higher or lower on this list by clicking the green up or down arrows, respectively. 

A layer can be added to the list by clicking the :guilabel:`Add Layer...` button at the top of the layer table. From the list of layers, select the layer to be added by clicking the layer name. The   selected layer will be appended to the bottom of the layer list. 

.. figure:: ../images/data_layergroups_add_layer.png
   :align: center

   *Dialog for adding a layer to a layer group*

Add a Layer Group
-----------------

The buttons for adding and removing a layer group can be found at the top of the :guilabel:`Layer Groups` page. 

.. figure:: ../images/data_layergroups_add.png
   :align: center

   *Buttons to add or remove a layer group*
   
To add a new layer group, select the "Add a new layer group" button. You will be prompted to name the layer group.
   
.. figure:: ../images/data_layergroups_name.png
   :align: center

   *New layer group dialog*

When finished, click :guilabel:`Submit`. You will be redirected to an empty layer group configuration page. Begin by adding layers by clicking the :guilabel:`Add layer...` button (described in the previous section). Once the layers are positioned accordingly, press :guilabel:`Generate Bounds` to automatically generate the bounding box and projection. Press :guilabel:`Save` to save the new layer group.

.. figure:: ../images/data_layergroups_add_edit.png
   :align: center

   *New layer group configuration page*

Remove a layer group
--------------------

To remove a layer group, click the check box next to the layer group. Multiple layer groups can be selected for batch removal. Click the :guilabel:`remove selected layer group(s)` link. You will be asked to confirm or cancel the deletion. Selecting :guilabel:`OK` successfully removes the layer group. 
 
.. figure:: ../images/data_layergroups_delete.png
   :align: center
   
   *Removing a layer group*
