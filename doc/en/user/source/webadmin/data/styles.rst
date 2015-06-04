.. _webadmin_styles:

Styles
======

Styles render, or make available, geospatial data. Styles for GeoServer are written in Styled Layer Descriptor (SLD), a subset of XML. Please see the section on :ref:`styling` for more information on working with styles. 

On the Styles page, you can add a new style, view or edit an existing style, or remove a style.

.. figure:: ../images/data_style.png
   :align: center
   
   *Styles page*

Edit a Style
------------

To view or edit a style, click the style name. A :guilabel:`Style Editor` page will be diplayed.  The page presents options for configuring a style's name and code. Style names are specified at the top in the name field. Typing or pasting of SLD code can be done in one of two modes. The first mode is an embedded `EditArea <http://www.cdolivet.com/index.php?page=editArea>`_ a rich editor. The second mode is an unformatted text editor. Check the :guilabel:`Toggle Editor` to switch between modes.

.. figure:: ../images/data_style_editor.png
   :align: center
   
   *Rich text editor*

.. figure:: ../images/data_style_editor_text.png
   :align: center
   
   *Plain text editor*
   
The rich editor is designed for text formatting, search and replace, line numbering, and real-time syntax highlighting. You can also switch view to full-screen mode for a larger editing area. 

.. list-table::
   :widths: 25 75 

   * - **Button**
     - **Description**

   * - .. image:: ../images/data_style_editor1.png
     - search
   * - .. image:: ../images/data_style_editor2.png
     - go to line   
   * - .. image:: ../images/data_style_editor3.png
     - fullscreen mode
   * - .. image:: ../images/data_style_editor4.png
     - undo     
   * - .. image:: ../images/data_style_editor5.png
     - redo
   * - .. image:: ../images/data_style_editor6.png
     - toggle syntax highlight on/off
   * - .. image:: ../images/data_style_editor7.png
     - reset highlight (if desynchronized from text)
   * - .. image:: ../images/data_style_editor8.png
     - about
     

To confirm that the SLD code is fully compliant with the SLD schema, click the :guilabel:`Validate` button. A message box will confirm whether the style contains validation errors.

.. note:: GeoServer will sometimes render styles that fail validation, but this is not recommended. 

.. figure:: ../images/data_style_editor_noerrors.png
   :align: center
   
   *No validation errors* 
   
.. figure:: ../images/data_style_editor_error.png
   :align: center
   
   *Validation error message* 

Add a Style
-----------

The buttons for adding and removing a style can be found at the top of the :guilabel:`Styles` page. 

.. figure:: ../images/data_style_add_delete.png
   :align: center

   *Adding or removing a style*
   
To add a new style, select the :guilabel:`Add a new style` button. You will be redirected to an editor page. Enter a name for the style. The editor page provides two options for submitting an SLD. You can paste the SLD directly into the editor, or you can select and upload a local file that contains the SLD.

.. figure:: ../images/data_style_upload.png
   :align: center

   *Uploading an SLD file from your local computer*
   
Once a style is successfully submitted, you will be redirected to the main :guilabel:`Styles` page where the new style will be listed.

Remove a Style
--------------

To remove a style, select it by clicking the checkbox next to the style. Multiple styles can be selected, or all can be selected by clicking the checkbox in the header. Click the :guilabel:`Remove selected style(s)` link at the top of the page. You will be asked to confirm or cancel the removal. Clicking :guilabel:`OK` removes the selected style(s). 
 
.. figure:: ../images/data_style_delete.png
   :align: center
   
   *Confirmation prompt for removing styles*
