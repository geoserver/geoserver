.. _data_webadmin_styles:

Styles
======

Styles are used to control the appearance of geospatial data. Styles for GeoServer are written in a number of different formats:

* **Styled Layer Descriptor (SLD)**: An OGC standard for geospatial styling. Available by default.
* **Cascading Style Sheets (CSS)**: A CSS-like syntax. Available via an :ref:`extension <extensions_css>`.
* **YSLD**: An SLD-equivalent based on `YAML <http://yaml.org>`_ for improved authoring. Available via a :ref:`community module <community_ysld>`.

This section will detail how to work with the styles pages in the :ref:`web_admin`. For more information on styles and syntax, please see the section on :ref:`styling`.


Styles page
-----------

On the Styles page, you can :ref:`add a new style <data_webadmin_styles_add>`, :ref:`remove a style <data_webadmin_styles_remove>`, or :ref:`view or edit an existing style <data_webadmin_styles_edit>`.

.. figure:: img/data_style.png
   
   Styles page

.. _data_webadmin_styles_add:

Add a Style
~~~~~~~~~~~

The buttons for adding and removing a style can be found at the top of the :guilabel:`Styles` page. 

.. figure:: img/data_style_add_delete.png

   Adding or removing a style
   
To add a new style, click :guilabel:`Add a new style` button. You will be redirected to the new style page, which is the same as the Style Editor :ref:`Data <data_webadmin_styles_edit_data>` tab.

The editor page provides several options for submitting a new style:

* **Type** the style definition directly into the editor.

* **Generate** a new default style based on an internal template:

  .. figure:: img/data_style_editor_generate.png

     Generating a new default style.

* **Copy** the contents of an existing style into the editor:

  .. figure:: img/data_style_editor_copy.png

     Copying an existing Style from GeoServer

* **Upload** a local file that contains the style:

  .. figure:: img/data_style_upload.png

     Uploading an file from the local system

When creating a style, only the :guilabel:`Data` tab will be available. Click :guilabel:`Apply` on the new style to stay on the Style Editor page and gain access to all tabs.

.. _data_webadmin_styles_remove:

Remove a Style
~~~~~~~~~~~~~~

To remove a style, click the check box next to the style. Multiple styles can be selected at the same time. Click the :guilabel:`Remove selected style(s)` link at the top of the page. You will be asked for confirmation:

.. figure:: img/data_style_delete.png
  
   Confirmation prompt for removing styles

 Click :guilabel:`OK` to remove the selected style(s).


.. _data_webadmin_styles_edit:

Style Editor
------------

On the Styles page, click a style name to open the :guilabel:`Style Editor`.

The Style Editor page presents the :ref:`style definition <data_webadmin_styles_edit_definition>`. The page contains four tabs with many configuration options:

* :ref:`Data <data_webadmin_styles_edit_data>`: Includes basic style information, the ability to generate a style, and legend details
* :ref:`Publishing <data_webadmin_styles_edit_publishing>`: Displays which layers are using this style
* :ref:`Layer Preview <data_webadmin_styles_edit_preview>`: Previews the style with an associated layer while editing
* :ref:`Layer Attributes <data_webadmin_styles_edit_attributes>`: Displays a list of attributes for the associated layer

.. warning:: ADD IMAGE

At the bottom of the Style Editor page is a number of options:

.. list-table::
   :widths: 25 75 
   :header-rows: 1

   * - Option
     - Description
   * - :guilabel:`Validate`
     - Will test the current style for correctness according to the :guilabel:`Format` option selected
   * - :guilabel:`Apply`
     - Makes the changes to the style and remain on the Style Editor page. This is useful to update the :ref:`Layer Preview <data_webadmin_styles_edit_preview>` tab.
   * - :guilabel:`Submit`
     - Makes the changes to the style and returns to the Styles page
   * - :guilabel:`Cancel`
     - Cancels all changes to the style and returns to the Styles page

.. _data_webadmin_styles_edit_definition:

Style definition
~~~~~~~~~~~~~~~~

On all tabs, the Style Editor will display the style definition at the bottom, allowing for direct editing of the style. Switch between the tabs in order to facilitate style creation and editing.

.. figure:: img/data_style_editor.png
   
   Style editor

The style editor supports line numbering, automatic indentation, and real-time syntax highlighting. You can also increase or decrease the font size of the editor.

.. list-table::
   :widths: 25 75 
   :header-rows: 1

   * - Button
     - Description
   * - .. image:: img/data_style_editor_undo.png
     - Undo
   * - .. image:: img/data_style_editor_redo.png
     - Redo
   * - .. image:: img/data_style_editor_goto.png
     - Go to line
   * - .. image:: img/data_style_editor_reformat.png
     - Auto-format the editor contents
   * - .. image:: img/data_style_editor_fontsize.png
     - Change the font size in the editor

During editing and especially after editing is complete, you will want to check validation of the syntax. This can be done by clicking the :guilabel:`Validate` button at the bottom.

If no errors are found, you will see this message:

.. figure:: img/data_style_editor_noerrors.png
   
   No validation errors

If any validation errors are found, they will be displayed:   

.. figure:: img/data_style_editor_error.png
   
   Validation error message


.. _data_webadmin_styles_edit_data:

Style Editor: Data tab
~~~~~~~~~~~~~~~~~~~~~~

The Data tab includes basic style information, the ability to generate a style, and legend details.

The :guilabel:`Style Data` area has mandatory basic style information:

.. list-table::
   :widths: 25 75 
   :header-rows: 1

   * - Option
     - Description
   * - :guilabel:`Name`
     - Name of the style
   * - :guilabel:`Workspace`
     - Workspace in which the style is contained. Styles can be inside workspaces, but can also be "global" (no workspace).
   * - :guilabel:`Format`
     - Format of the style. Options are :guilabel:`SLD`, :guilabel:`CSS`, and :guilabel:`YSLD`, depending on availability.

.. warning:: ADD IMAGE

The :guilabel:`Style Content` area allows you to generate a style, copy an existing style, or upload an existing style:

.. list-table::
   :widths: 25 75 
   :header-rows: 1

   * - Option
     - Description
   * - :guilabel:`Generate a default style`
     - Selects a generic style based on geometry. Options are :guilabel:`Point`, :guilabel:`Line`, :guilabel:`Polygon`, :guilabel:`Raster`, and :guilabel:`Generic`. Click :guilabel:`Generate` when selected.
   * - :guilabel:`Copy from existing style`
     - Selects an existing style in GeoServer and copy its contents to this style. Any style in GeoServer is available as an option. Not all styles will work with all layers. Click :guilabel:`Copy` when selected.
   * - :guilabel:`Upload a style file`
     - Selects a plain text file on your local system to add as the style. Click :guilabel:`Upload` when selected.

.. warning:: ADD IMAGE

By default GeoServer will generate a legend based on your style file. The :guilabel:`Legend` area allows you to add, modify, or delete a custom style, and preview the legend for the style:

.. list-table::
   :widths: 25 75 
   :header-rows: 1

   * - Option
     - Description
   * - :guilabel:`Add legend`
     - Allows you to use a custom legend
   * - :guilabel:`Online Resource`
     - Path to the custom legend graphic to use. Can be a URL or a local path (relative to the style file path). See :ref:`datadir_structure` for a description of the styles directory.
   * - :guilabel:`Auto-detect image size and type`
     - Populates the :guilabel:`Width`, :guilabel:`Height`, and :guilabel:`Format` options for the :guilabel:`Online Resource`
   * - :guilabel:`Width`
     - Width of the custom legend graphic
   * - :guilabel:`Height`
     - Height of the custom legend graphic
   * - :guilabel:`Format`
     - Mime type of the custom legend graphic
   * - :guilabel:`Discard legend`
     - Will remove the settings for the custom legend graphic and will instead use the default generated legend.
   * - :guilabel:`Preview legend`
     - Previews the legend based on the current settings

.. figure:: img/data_style_add_legend.png
   
   Legend

.. _data_webadmin_styles_edit_publishing:

Style Editor: Publishing tab
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The Publishing tab displays a list of all layers on the server, with the purpose of showing which layers are associated with the current style. Layers can set a single default style and have any number of additional styles. If this style is set to be either of these options for a layer, it will be shown with a check box in the table.

.. list-table::
   :widths: 25 75 
   :header-rows: 1

   * - Option
     - Description
   * - :guilabel:`Workspace`
     - Workspace of the layer
   * - :guilabel:`Layer`
     - Name of the layer
   * - :guilabel:`Default`
     - Shows whether the style being edited is the default for a given layer 
   * - :guilabel:`Associated`
     - Shows whether the style being edited is an additional style for a given layer 

.. warning:: ADD IMAGE


.. _data_webadmin_styles_edit_preview:

Style Editor: Layer Preview tab
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

It is very common to have to iterate your styles and test how the visualization changes over time. The Layer Preview tab allows you to make changes to the style and see them without having to navigate away from the page.

The Layer Preview tab shows a single image. GeoServer tries to identify which layer should be shown (for example, a layer for which this style is the default), but if the layer being previewed is not the desired one, click the layer name above the preview box and select a layer.

.. warning:: ADD IMAGE


.. _data_webadmin_styles_edit_attributes:

Style Editor: Layer Attributes tab
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Most styles utilize the specific values of certain attributes of the associated layer in order to create more detailed and useful styles. (For example: styling all large cities different from small cities based on a particular attribute.)

The Layer Attributes tab will display a list of attributes for the given associated layer. GeoServer tries to identify which layer should be shown (for example, a layer for which this style is the default), but if the layer being previewed is not the desired one, click the layer name above the table and select a layer.

.. warning:: ADD IMAGE

.. list-table::
   :widths: 25 75 
   :header-rows: 1

   * - Option
     - Description
   * - :guilabel:`name`
     - Name of the attribute
   * - :guilabel:`type`
     - Type of the attribute. Can be a numeric (such as "Long"), a string ("String"), or a geometry (such as "Point").
   * - :guilabel:`sample`
     - Sample value of the attribute taken from the data.
   * - :guilabel:`min`
     - Minimum value of the attribute in the data set, if applicable. 
   * - :guilabel:`max`
     - Minimum value of the attribute in the data set, if applicable. 
   * - :guilabel:`computeStats`
     - Click :guilabel:`Compute` to calculate the :guilabel:`min` and :guilabel:`max` values for that attribute. 
