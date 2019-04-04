.. _sld_reference_textsymbolizer:

TextSymbolizer
==============

A **TextSymbolizer** styles features as **text labels**. 
Text labels are positioned eoither at points or along linear paths
derived from the geometry being labelled.

Labelling is a complex operation, and effective labelling
is crucial to obtaining legible and visually pleasing cartographic output.
For this reason SLD provides many options to control label placement.
To improve quality even more GeoServer provides additional options and parameters.
The usage of the standard and extended options are described in greater detail
in the following section on :ref:`sld_reference_labeling`.


Syntax
------

A ``<TextSymbolizer>`` contains the following elements:

.. list-table::
   :widths: 20 20 60
   
   * - **Tag**
     - **Required?**
     - **Description**
   * - ``<Geometry>``
     - No
     - The geometry to be labelled.
   * - ``<Label>``
     - No
     - The text content for the label.
   * - ``<Font>``
     - No
     - The font information for the label.
   * - ``<LabelPlacement>``
     - No
     - Sets the position of the label relative to its associated geometry.
   * - ``<Halo>``
     - No
     - Creates a colored background around the label text, for improved legibility.
   * - ``<Fill>``
     - No
     - The fill style of the label text.
   * - ``<Graphic>``
     - No
     - A graphic to be displayed behind the label text.
       See :ref:`sld_reference_graphic` for content syntax.
   * - ``<Priority>``
     - No
     - The priority of the label during conflict resolution.
       Content may contains :ref:`expressions <sld_reference_parameter_expressions>`. 
       See also :ref:`labeling_priority`.
   * - ``<VendorOption>``
     - 0..N
     - A GeoServer-specific option.
       See :ref:`sld_reference_labeling` for descriptions of the available options.
       Any number of options may be specified.

     
Geometry
^^^^^^^^

The ``<Geometry>`` element is optional.  
If present, it specifies the featuretype property from which to obtain the geometry to label,
using a ``<PropertyName>`` element.
See also :ref:`geometry_transformations` for GeoServer extensions for specifying geometry.

Any kind of geometry may be labelled with a ``<TextSymbolizer>``.
For non-point geometries, a representative point is used (such as the centroid of a line or polygon).


Label
^^^^^

The ``<Label>`` element specifies the text that will be rendered as the label.
It allows content of mixed type, which means that the content
can be a mixture of string data and :ref:`sld_filter_expression`.
These are concatenated to form the final label text.
If a label is provided directly by a feature property, 
the content is a single ``<PropertyName>``.
Multiple properties can be included in the label,
and property values can be manipulated by filter expressions and functions. 
Additional "boilerplate" text can be provided as well.
Whitespace can be preserved by surrounding it with XML ``<![CDATA[`` ``]]>`` delimiters.

If this element is omitted, no label is rendered.

   
Font
^^^^

The ``<Font>`` element specifes the font to be used for the label.
A set of ``<CssParameter>`` elements specify the details of the font.  

The ``name`` **attribute** indicates what aspect of the font is described,
using the standard CSS/SVG font model.
The **content** of the element supplies the
value of the font parameter.
The value may contain :ref:`expressions <sld_reference_parameter_expressions>`.

.. list-table::
   :widths: 30 15 55
      
   * - **Parameter**
     - **Required?**
     - **Description**
   * - ``name="font-family"``
     - No
     - The family name of the font to use for the label.  
       Default is ``Times``.
   * - ``name="font-style"``
     - No
     - The style of the font.  Options are ``normal``, ``italic``, and ``oblique``.  Default is ``normal``.
   * - ``name="font-weight"``
     - No
     - The weight of the font.  Options are ``normal`` and ``bold``.  Default is ``normal``.
   * - ``name="font-size"``
     - No
     - The size of the font in pixels.  Default is ``10``.

LabelPlacement
^^^^^^^^^^^^^^

The ``<LabelPlacement>`` element specifies the placement of the label relative to the geometry being labelled.
There are two possible sub-elements: ``<PointPlacement>`` or ``<LinePlacement>``.  
Exactly one of these must be specified.

.. list-table::
   :widths: 20 20 60
   
   * - **Tag**
     - **Required?**
     - **Description**   
   * - ``<PointPlacement>``
     - No
     - Labels a geometry at a single point
   * - ``<LinePlacement>``
     - No
     - Labels a geometry along a linear path
     
PointPlacement
^^^^^^^^^^^^^^

The ``<PointPlacement>`` element indicates the label is placed 
at a labelling point derived from the geometry being labelled. 
The position of the label relative to the labelling point may be controlled by the 
following sub-elements:

.. list-table::
   :widths: 20 20 60 

   * - **Tag** 
     - **Required?**
     - **Description**
   * - ``<AnchorPoint>``
     - No
     - The location within the label bounding box that is aligned with the label point.
       The location is specified by ``<AnchorPointX>`` and ``<AnchorPointY>`` sub-elements,
       with values in the range [0..1].
       Values may contain :ref:`expressions <sld_reference_parameter_expressions>`.
   * - ``<Displacement>``
     - No
     - Specifies that the label point should be offset from the original point.
       The offset is specified by ``<DisplacementX>`` and ``<DisplacementY>`` sub-elements,
       with values in pixels.
       Values may contain :ref:`expressions <sld_reference_parameter_expressions>`.
       Default is ``(0, 0)``.
   * - ``<Rotation>``
     - No
     - The rotation of the label in clockwise degrees
       (negative values are counterclockwise).  
       Value may contain :ref:`expressions <sld_reference_parameter_expressions>`.
       Default is ``0``.

The anchor point justification, displacement offsetting, and rotation are applied in that order. 

LinePlacement
^^^^^^^^^^^^^

The ``<LinePlacement>`` element indicates the label 
is placed along a linear path derived from the geometry being labelled. 
The position of the label relative to the linear path may be controlled by the 
following sub-element:


.. list-table::
   :widths: 20 20 60 

   * - **Tag** 
     - **Required?**
     - **Description**
   * - ``<PerpendicularOffset>``
     - No
     - The offset from the linear path, in pixels.  
       Positive values offset to the left of the line, negative to the right.
       Value may contain :ref:`expressions <sld_reference_parameter_expressions>`.
       Default is ``0``.

The appearance of text along linear paths can be further controlled 
by the vendor options ``followLine``, ``maxDisplacement``, ``repeat``, ``labelAllGroup``, and ``maxAngleDelta``.
These are described in :ref:`sld_reference_labeling`.

Halo
^^^^

A halo creates a colored background around the label text, which improves readability in low contrast situations.
Within the ``<Halo>`` element there are two sub-elements which control the appearance of the halo:

.. list-table::
   :widths: 20 20 60
   
   * - **Tag**
     - **Required?**
     - **Description**   
   * - ``<Radius>``
     - No
     - The halo radius, in pixels.  
       Value may contain :ref:`expressions <sld_reference_parameter_expressions>`.
       Default is ``1``.
   * - ``<Fill>``
     - No
     - The color and opacity of the halo
       via ``CssParameter`` elements for ``fill`` and ``fill-opacity``.
       See :ref:`sld_reference_fill` for full syntax.
       The parameter values may contain :ref:`expressions <sld_reference_parameter_expressions>`.
       Default is a **white** fill (``#FFFFFF``) at **100%** opacity. 

Fill
^^^^

The ``<Fill>`` element specifies the fill style for the label text.  
The syntax is the same as that of the ``PolygonSymbolizer`` :ref:`sld_reference_fill` element.
The default fill color is **black** (``#FFFFFF``) at **100%** opacity..
     
Graphic
^^^^^^^

The ``<Graphic>`` element specifies a graphic symbol to be displayed behind the label text (if any).
A classic use for this is to display "highway shields" behind road numbers
provided by feature attributes.
The element content has the same syntax as the ``<PointSymbolizer>`` :ref:`sld_reference_graphic` element.
Graphics can be provided by internal :ref:`mark symbols <pointsymbols>`, or by external images or SVG files.
Their size and aspect ratio can be changed to match the text displayed with them
by using the vendor options :ref:`labeling_graphic_resize` and :ref:`labeling_graphic_margin`.

Example
-------

The following symbolizer is taken from the :ref:`sld_cookbook_points` section in the :ref:`sld_cookbook`.

.. code-block:: xml 
   :linenos:

          <TextSymbolizer>
            <Label>
              <ogc:PropertyName>name</ogc:PropertyName>
            </Label>
            <Font>
              <CssParameter name="font-family">Arial</CssParameter>
              <CssParameter name="font-size">12</CssParameter>
              <CssParameter name="font-style">normal</CssParameter>
              <CssParameter name="font-weight">bold</CssParameter>
            </Font>
            <LabelPlacement>
              <PointPlacement>
                <AnchorPoint>
                  <AnchorPointX>0.5</AnchorPointX>
                  <AnchorPointY>0.0</AnchorPointY>
                </AnchorPoint>
                <Displacement>
                  <DisplacementX>0</DisplacementX>
                  <DisplacementY>25</DisplacementY>
                </Displacement>
                <Rotation>-45</Rotation>
              </PointPlacement>
            </LabelPlacement>
            <Fill>
              <CssParameter name="fill">#990099</CssParameter>
            </Fill>
          </TextSymbolizer>

The symbolizer labels features with the text from the ``name`` property.
The font is Arial in bold at 12 pt size, filled in purple.
The labels are centered on the point along their lower edge,
then displaced 25 pixels upwards, 
and finally rotated 45 degrees counterclockwise.

The displacement takes effect before the rotation during rendering, 
so the 25 pixel vertical displacement is itself rotated 45 degrees.

.. figure:: img/text_pointwithrotatedlabel.png
   :align: center

   *Point with rotated label*

Scalable Font Size
------------------

The font size can also be set depending on the scale denominator as follows:

.. code-block:: xml 
   :linenos:
   
          <CssParameter name="font-size">
            <ogc:Function name="Categorize">
              <!-- Value to transform -->
              <ogc:Function name="env">
                <ogc:Literal>wms_scale_denominator</ogc:Literal>
              </ogc:Function>
              <!-- Output values and thresholds -->
              <!-- Ranges: -->
              <!-- [scale <= 300, font 12] -->
              <!-- [scale 300 - 2500, font 10] -->
              <!-- [scale > 2500, font 8] -->
              <ogc:Literal>12</ogc:Literal>
              <ogc:Literal>300</ogc:Literal>
              <ogc:Literal>10</ogc:Literal>
              <ogc:Literal>2500</ogc:Literal>
              <ogc:Literal>8</ogc:Literal>
            </ogc:Function>
          </CssParameter>
		  
The above example would display text at different sizes depending on the scale
denominator setting.  A font size of **12** for scale denominator of less than or equal
to 300, a font size of **10** for scale denominator from 300-2500 and a font size of **8** for scale 
denominator greater than 2500.