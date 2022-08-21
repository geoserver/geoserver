.. _style_quickstart:

Publishing a style
==================

This tutorial walks through the steps of defining a style and associating it with a layer for use.

.. note:: This tutorial assumes that GeoServer is running at ``http://localhost:8080/geoserver``.

Data preparation
----------------

First let's gather that the data that we'll be publishing.

#. Complete the previous tutorials:
   
   * :ref:`geopkg_quickstart` defining the `tutorial:countries` layer
   * :ref:`image_quickstart` defining the `tutorial:shaded` layer
   * :ref:`group_quickstart` defining the `tutorial:basemap` layer
       
Create a style
--------------------

#. Navigate to :menuselection:`Data > Style` page.

   .. figure:: images/styles_page.png
      
      Styles
    
#. This page displays a list of styles, including the workspace the style belongs to.
   
   .. note:: Styles groups are allowed to be "global" allowing a style to be defined that can be used by any layer.
   
#. At the top of the list :guilabel:`Styles` list, locate and click the :guilabel:`Add a new style` link.
      
#. Locate :guilabel:`Style Data` and define the style:

   .. list-table::
      :widths: 30 70
      :width: 100%
      :stub-columns: 1

      * - Name
        - :kbd:`background`
      * - Workspace
        - ``tutorial``
      * - Format
        - ``SLD``
   .. figure:: images/style_data.png
      
      Style data

#. Locate :guilabel:`Style Content` and carefully:
   
   * Under :guilabel:`Generate a default style` select ``Polygon``
   
   .. figure:: images/style_content.png
      
      Style Content configured to generate a polygon default style.

#. Under :guilabel:`Generate a default style` locate and click the :guilabel:`Generate` link to populate the style editor with a generated outline of a polygion style.

   .. figure:: images/generate.png
   
#. Press the :guilabel:`Apply` button to define this style.
   
   Now that the style is defined there are more options for interactively working with the style.
   
#. Change to :guilabel:`Publishing` tab.
   
   * Use the search to filter with ``tutorial`` to locate ``tutorial:countries``.
   
   * Select the :guilabel:`Default` checkbox for ``tutorial:countries`` to use the ``tutorial:background`` style the default for this layer.
      
   .. figure:: images/publish.png
      
      Style publish

#. Next to :guilabel:`Publishing` navigate to the :guilabel:`Layer Preview` tab.

   * Locate the :guilabel:`Preview on layer` and click on the link to select ``tutorial:countries`` as a dataset to use when editing the style.
   
   .. figure:: images/preview.png
   
      Styled editor Layer Preview tab

#. Edit your style by inserting ``fill-opacity`` value of ``0.25``.

   .. literalinclude:: files/background.sld
      :language: xml
      :emphasize-lines: 17

#. Press :guilabel:`Apply` to edit your style and check the resulting visual change in the layer preview.

#. Experiment with:
   
   * Updating the title information, watching the displayed legend change
   * Full screen mode for side-by-side editing
   
   .. figure:: images/full.png
       
      Full screen mode
      
#. When this style is used as part of the ``tutorial::basemap`` the ``fill-opacity`` allows the shaded relief detail to be shown.

   .. figure:: images/basemap.png
      
      Basemap with background style applied to countries