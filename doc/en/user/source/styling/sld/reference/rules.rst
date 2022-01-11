.. _sld_reference_rules:

Rules
=====

Styling **rules** define the portrayal of features.
A rule combines a :ref:`filter <sld_reference_filters>` with any number of symbolizers. 
Features for which the filter condition evaluates as true
are rendered using the the symbolizers in the rule. 

Syntax
------

The ``<Rule>`` element contains the following elements:

.. list-table::
   :widths: 25 15 60
   
   * - **Tag**
     - **Required?**
     - **Description**
   * - ``<Name>``
     - No
     - Specifies a name for the rule.
   * - ``<Title>``
     - No
     - Specifies a title for the rule.  
       The title is used in display lists and legends.
   * - ``<Abstract>``
     - No
     - Specifies an abstract describing the rule.
   * - ``<Filter>``
     - No
     - Specifies a filter controlling when the rule is applied.
       See :ref:`sld_reference_filters`
   * - ``<MinScaleDenominator>``
     - No
     - Specifies the minimum scale denominator (inclusive) for the scale range
       in which this rule applies.
       If present, the rule applies at the given scale and all smaller scales.
   * - ``<MaxScaleDenominator>``
     - No
     - Specifies the maximum scale denominator (exclusive) for the scale range 
       in which this rule applies.
       If present, the rule applies at scales larger than the given scale.
   * - ``<PointSymbolizer>``
     - 0..N
     - Specifies styling as points.
       See :ref:`sld_reference_pointsymbolizer`
   * - ``<LineSymbolizer>``
     - 0..N
     - Specifies styling as lines.
       See :ref:`sld_reference_linesymbolizer`
   * - ``<PolygonSymbolizer>``
     - 0..N
     - Specifies styling as polygons.
       See :ref:`sld_reference_polygonsymbolizer`
   * - ``<TextSymbolizer>``
     - 0..N
     - Specifies styling for text labels.
       See :ref:`sld_reference_textsymbolizer`
   * - ``<RasterSymbolizer>``
     - 0..N
     - Specifies styling for raster data.
       See :ref:`sld_reference_rastersymbolizer`

       
Scale Selection
---------------

Rules support **scale selection**
to allow specifying the scale range in which a rule may be applied
(assuming the filter condition is satisfied as well, if present). 
Scale selection allows for varying portrayal of features at different map scales.
In particular, at smaller scales it is common to use simpler styling for features, 
or even prevent the display of some features altogether.

Scale ranges are specified by using **scale denominators**. 
These values correspond directly to the ground distance covered by a map, 
but are inversely related to the common "large" and "small" terminology for map scale.  
In other words:

* **large scale** maps cover *less* area and have a *smaller* scale denominator
* **small scale** maps cover *more* area and have a *larger* scale denominator

Two optional elements specify the scale range for a rule:

.. list-table::
   :widths: 30 15 55 

   * - **Tag** 
     - **Required?**
     - **Description**
   * - ``<MinScaleDenominator>``
     - No
     - Specifies the minimum scale denominator (inclusive) for the scale range
       in which this rule applies.
       If present, the rule applies at the given scale and all smaller scales.
   * - ``<MaxScaleDenominator>``
     - No
     - Specifies the maximum scale denominator (exclusive) for the scale range 
       in which this rule applies.
       If present, the rule applies at scales larger than the given scale.

.. note:: 

    The current scale can also be obtained via the ``wms_scale_denominator`` 
    :ref:`SLD environment variable <sld_variable_substitution>`.
    This allows including scale dependency in :ref:`sld_filter_expression`.

       
The following example shows the use of scale selection in a pair of rules.
The rules specify that:

* at scales **above** 1:20,000 
  (*larger* scales, with scale denominators *smaller* than 20,000) 
  features are symbolized with 10-pixel red squares, 
* at scales **at or below** 1:20,000 
  (*smaller* scales, with scale denominators *larger* than 20,000) 
  features are symbolized with 4-pixel blue triangles.


.. code-block:: xml 

  <Rule>
     <MaxScaleDenominator>20000</MaxScaleDenominator>
     <PointSymbolizer>
       <Graphic>
         <Mark>
           <WellKnownName>square</WellKnownName>
           <Fill><CssParameter name="fill">#FF0000</CssParameter>
         </Mark>
         <Size>10</Size>
       </Graphic>
     </PointSymbolizer>
  </Rule>
  <Rule>
     <MinScaleDenominator>20000</MinScaleDenominator>
     <PointSymbolizer>
       <Graphic>
         <Mark>
           <WellKnownName>triangle</WellKnownName>
           <Fill><CssParameter name="fill">#0000FF</CssParameter>
         </Mark>
         <Size>4</Size>
       </Graphic>
     </PointSymbolizer>
  </Rule>

  
Evaluation Order
----------------
    
Within an SLD document each ``<FeatureTypeStyle>`` can contain many rules. 
Multiple-rule SLDs are the basis for thematic styling.
In GeoServer each ``<FeatureTypeStyle>`` 
is evaluated once for each feature processed.
The rules within it are evaluated in the order they occur.
A rule is applied when its filter condition (if any) is true for a feature 
and the rule is enabled at the current map scale.
The rule is applied by rendering the feature using each symbolizer
within the rule, in the order in which they occur.
The rendering is performed into the image buffer
for the parent ``<FeatureTypeStyle>``.
Thus symbolizers earlier in a ``FeatureTypeStyle`` and ``Rule`` are rendered 
*before* symbolizers occuring later in the document
(this is the "Painter's Model" method of rendering).

       
Examples
--------

The following rule applies only to features which have a ``POPULATION`` attribute greater than 100,000,
and symbolizes the features as red points. 

.. code-block:: xml 

  <Rule>
     <ogc:Filter>
       <ogc:PropertyIsGreaterThan>
         <ogc:PropertyName>POPULATION</ogc:PropertyName>
         <ogc:Literal>100000</ogc:Literal>
       </ogc:PropertyIsGreaterThan>
     </ogc:Filter>
     <PointSymbolizer>
       <Graphic>
         <Mark>
           <Fill><CssParameter name="fill">#FF0000</CssParameter>
         </Mark>
       </Graphic>
     </PointSymbolizer>
  </Rule>

An additional rule can be added which applies to features whose ``POPULATION`` attribute is less than 100,000, 
and symbolizes them as green points.

.. code-block:: xml 

   <Rule>
     <ogc:Filter>
       <ogc:PropertyIsLessThan>
         <ogc:PropertyName>POPULATION</ogc:PropertyName>
         <ogc:Literal>100000</ogc:Literal>
       </ogc:PropertyIsLessThan>
     </ogc:Filter>
     <PointSymbolizer>
       <Graphic>
         <Mark>
           <Fill><CssParameter name="fill">#0000FF</CssParameter>
         </Mark>
       </Graphic>
     </PointSymbolizer>
   </Rule>

   

