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
     - Specifies the geometry to be rendered.
   * - ``<Label>``
     - No
     - Specifies the content of the text label.
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
     - Specifies the fill color of the text label.

     
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
can be a mixture of string data and OGC Filter expressions.
These are concatenated to form the final label text.
If a label is provided directly by a feature property, 
the content is a single ``<PropertyName>``.
Extra "boilerplate" text can be provide as well.
Multiple properties can be included in the label,
and property values can be manipulated by filter expressions and functions. 

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
     - Specifies the family name of the font to use for the label.  
       Default is ``Times``.
   * - ``name="font-style"``
     - No
     - Specifies the style of the font.  Options are ``normal``, ``italic``, and ``oblique``.  Default is ``normal``.
   * - ``name="font-weight"``
     - No
     - Specifies the weight of the font.  Options are ``normal`` and ``bold``.  Default is ``normal``.
   * - ``name="font-size"``
     - No
     - Specifies the size of the font in pixels.  Default is ``10``.

LabelPlacement
^^^^^^^^^^^^^^

The ``<LabelPlacement>`` element specifies the placement of the label relative to the geometry being labelled.
There are two possible sub-elements ``<PointPlacement>`` and ``<LinePlacement>``.  
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
     - Specifies the location within the label bounding box that is aligned with the label point.
       The location is specified by ``<AnchorPointX>`` and ``<AnchorPointY>`` sub-elements,
       with values in the range [0..1].
       Values may contain :ref:`expressions <sld_reference_parameter_expressions>`.
   * - ``<Displacement>``
     - No
     - Specifies that the label point should be offset from the original point.
       The offset is specified by ``<DisplacementtX>`` and ``<DisplacementY>`` sub-elements,
       with values in pixels.
       Values may contain :ref:`expressions <sld_reference_parameter_expressions>`.
   * - ``<Rotation>``
     - No
     - Specifies the rotation of the label in clockwise degrees.  
       Value may contain :ref:`expressions <sld_reference_parameter_expressions>`.
       Default is ``0``.


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
     - Specifies the offset from the linear path, in pixels.  
       Positive values offset to the left of the line, negative to the right.
       Value may contain :ref:`expressions <sld_reference_parameter_expressions>`.
       Default is ``0``.


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
     - Specifies the size of the halo radius, in pixels.  
       Value may contain :ref:`expressions <sld_reference_parameter_expressions>`.
       Default is ``1``.
   * - ``<Fill>``
     - No
     - Specifies the color of the halo in the form ``#RRGGBB``.  
       Value may contain :ref:`expressions <sld_reference_parameter_expressions>`.
       Default is white (``#FFFFFF``). 

Fill
^^^^

The ``<Fill>`` element specifies the fill style for the label text.  
The syntax is identical to that of the ``PolygonSymbolizer`` :ref:`sld_reference_fill` element.
     
