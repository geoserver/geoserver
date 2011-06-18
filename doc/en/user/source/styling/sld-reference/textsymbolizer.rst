.. _sld_reference_textsymbolizer:

TextSymbolizer
--------------

The TextSymbolizer specifies **text labels**. 

Syntax
``````

A ``<TextSymbolizer>`` contains the following tags:


.. list-table::
   :widths: 20 20 60
   
   * - **Tag**
     - **Required?**
     - **Description**
   * - ``<Label>``
     - Yes
     - Specifies the content of the text label 
   * - ``<Font>``
     - No
     - Specifies the font information for the labels.
   * - ``<LabelPlacement>``
     - No
     - Sets the position of the label relative its associate feature.
   * - ``<Halo>``
     - No
     - Creates a colored background around the text label, for low contrast situations.
   * - ``<Fill>``
     - No
     - Determines the fill color of the text label.

Each of the above tags have additional sub tags.  For the ``<Label>`` tag:

.. list-table::
   :widths: 20 20 60
   
   * - **Tag**
     - **Required?**
     - **Description**
   * - 
     -
     -
   * - 
     -
     -
   
Within the ``<Font>`` tag, the ``<CssParameter>`` tag is the only tag included.  There are four types of parameters for this tag, each included inside the ``<CssParameter>`` tag:

.. list-table::
   :widths: 30 15 55
      
   * - **Parameter**
     - **Required?**
     - **Description**
   * - ``name="font-family"``
     - No
     - Determines the family name of the font to use for the label.  Default is ``Times``.
   * - ``name="font-style"``
     - No
     - Determines the style of the font.  Options are ``normal``, ``italic``, and ``oblique``.  Default is ``normal``.
   * - ``name="font-weight"``
     - No
     - Determines the weight of the font.  Options are ``normal`` and ``bold``.  Default is ``normal``.
   * - ``name="font-size"``
     - No
     - Determines the size of the font in pixels.  Default is ``10``.

Within the ``<LabelPlacement>`` tag, there are many tags for specifying the placement of the label:

.. list-table::
   :widths: 20 20 60
   
   * - **Tag**
     - **Required?**
     - **Description**   
   * - 
     -
     -
   * - 
     -
     -
     
Within the ``<Halo>`` tag, there are two tags to specify the details of the halo:

.. list-table::
   :widths: 20 20 60
   
   * - **Tag**
     - **Required?**
     - **Description**   
   * - ``<Radius>``
     - No
     - Sets the size of the halo radius in pixels.  Default is ``1``.
   * - ``<Fill>``
     - No
     - Sets the color of the halo in the form of ``#RRGGBB``.  Default is white (``#FFFFFF``). 

The ``<Fill>`` tag is identical to that described in the WHERE~? above.
     

Example
```````