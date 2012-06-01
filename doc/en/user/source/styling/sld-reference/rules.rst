.. _sld_reference_rules:

Rules
=====

A **rule** combines a :ref:`filter <sld_reference_filters>` with any number of symbolizers 
to define the portrayal of the features which satisfy the filter condition. 
Consider the following example:

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


The above rule applies only to features which have a ``POPULATION`` attribute greater than ``100,000`` and symbolizes then with a red point. 

An SLD document can contain many rules. 
Multiple-rule SLD's are the basis for thematic styling. 
Consider the above example expanded:

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

   
The above snippet defines an additional rule which applies to features whose ``POPULATION`` attribute is less than 100,000, and symbolizes them as green points.

Scale Selection
---------------

Rules support **scale selection**,
to allow specifying the scale range in which a rule may be applied
(assuming the filter is matched as well, if present). 
Scale selection allows for varying portrayal of features at different map scales.
In particular, at smaller scales it is common to use simpler styling for features, 
or even prevent the display of some features altogether.

Scale ranges are specified by using **scale denominators**. 
These values correspond directly to the ground distance covered by a map, 
but are inversely related to the common "large" and "small" terminology for map scale.  
In other words:

* **large scale** maps cover *less* area and have a *smaller* scale denominator
* **small scale** maps cover *more* area and have a *larger* scale denominator



Two elements specify the scale range for a rule:

.. list-table::
   :widths: 30 15 55 

   * - **Tag** 
     - **Required?**
     - **Description**
   * - ``<MinScaleDenominator>``
     - No
     - Specifies the minimum scale denominator (inclusive) for the scale range
       in which this rule applies.
       If omitted, the rule applies at the given scale and all smaller scales.
   * - ``<MaxScaleDenominator>``
     - No
     - Specifies the maximum scale denominator (exclusive) for the scale range 
       in which this rule applies.
       If omitted, the rule applies at all larger scales.

       
The following example shows the use of scale selection:

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

The above rules specify:

* at scales **above** 1:20,000 
  (*larger* scales, with scale denominators *smaller* than 20,000) 
  features are symbolized with 10-pixel red squares, 
* at scales **at or below** 1:20,000 
  (*smaller* scales, with scale denominators *larger* than 20,000) 
  features are symbolized with 4-pixel blue triangles.

