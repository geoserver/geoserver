User Guide
==========
To add metadata to a layer follow the steps in `Adding metadata to Layer`_ . When the metadata is repeated in multiple layers it is easier to create a template and reuse the data in the template for all the layers. See `Templates`_ .

.. contents:: :local:
    :depth: 1



Adding metadata to Layer
------------------------

Manually adding metadata
^^^^^^^^^^^^^^^^^^^^^^^^
Open the layer: navigate to :menuselection:`Layers --> Choose the layer --> Metadata tab`.

The metadata fields are available in the panel :guilabel:`Metadata fields`.

.. figure:: images/basic-gui.png

Import from geonetwork
^^^^^^^^^^^^^^^^^^^^^^
Choose a geonetwork from the drop downbox, add the UUID from the metadata record and click `import`.
All the content of the fields that are mapped in the geonetwork mapping configuration will be deleted.
The content will be replaced with the content from geonetwork.

Link with metadata template
^^^^^^^^^^^^^^^^^^^^^^^^^^^
A metadata template can contain the content for metadata fields used in multiple layers.
By defining these fields in a template you create one source for the content making it easier to maintain.

To link a layer with template navigate to :menuselection:`Layers --> Choose the layer --> Metadata tab` in the :guilabel:`Link with Template` panel choose a template from the dropdown and click `Link with template`

The values from the template will added to the metadata of the layer. How this is done depents on the type of the field.

The field is not a list
    When the field is not a list the value will be replaced with the value from the template and the field will be read only. This will only happen for fields that are not empty in the template.

The Field is a list
    For Fields that are a list the values from the template will be added as read only fields. The duplicate values in list will be removed if there are any.

When multiple templates are linked with a layer the priority of the template will determine which values are added. If a field is present in both templates the value of the template with the highest priority will be picked. The priority is determined by the `template order`_


Templates
---------
Templates can be created, edited, deleted and ordered in :menuselection:`Metadata --> Templates` .
All changes to the templates will also update the linked layers when the templates are saved by clicking the `Save` button in the overview page.
Templates that are linked to a layer cannot be removed and a warning message will appear.

.. figure:: images/templatesconfig.png

Create template
^^^^^^^^^^^^^^^
Use the `Add new` action to create a new template and choose a name for the template. The name is required and must be unique.

Edit template
^^^^^^^^^^^^^^^
Click on a template name to open the template and edit the values. Click `Save` to go back to the overview page, this will also recalculate the values in all linked layers.

Delete template
^^^^^^^^^^^^^^^
Select the templates that needs to be removed and click delete, the selected rows will be removed from the table. Save the changes by clicking the `Save` button.


Template order
^^^^^^^^^^^^^^
The templates have an order. The templates at the top of the list have a higher priority than the templates at the bottom.
When a field has a value in multiple templates and the layer is linked with those templates the priority will determine wich value is displayed in the metadata UI.
The value defined in the template with the highest priority will be displayed.

Change the order of the templates with the arrow keys in the priority column and save the changes by clicking `Save` button, this will also recalculate the values in all linked layers that may be affected.

