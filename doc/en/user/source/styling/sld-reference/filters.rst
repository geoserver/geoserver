.. _sld_reference_filters:

Filters
=======

A *filter* is the mechanism in SLD for specifying predicates. Similar in nature to a "WHERE" clause in SQL, filters are the language for specifying which styles should be applied to which features in a data set.

The filter language used by SLD follows the `OGC Filter Encoding standard <http://www.opengeospatial.org/standards/filter>`_.

A filter is used to select a subset of features of a dataset to apply a symbolizer to.

There are three types of filters:

Attribute filters
-----------------

Attribute filters are used to constrain the non-spatial attributes of a feature. 
For example:

.. code-block:: xml 
   
   <PropertyIsEqualTo>
      <PropertyName>NAME</PropertyName>
      <Literal>Bob</Literal>
   </PropertyIsEqualTo>

The above filter selects those features which have a ``NAME`` attribute which has a value of "Bob". 
A variety of comparison operators are available:

   * PropertyIsEqualTo
   * PropertyIsNotEqualTo
   * PropertyIsLessThan
   * PropertyIsLessThanOrEqualTo
   * PropertyIsGreaterThan
   * PropertyIsGreaterThanOrEqualTo
   * PropertyIsBetween

Spatial filters
---------------

Spatial filters used to constrain the spatial attributes of a feature. 
For example:

.. code-block:: xml 

   <Intersects>
      <PropertyName>GEOMETRY</PropertyName>
      <Literal>
         <gml:Point>
            <gml:coordinates>1 1</gml:coordinates>
         </gml:Point>
      </Literal>
   </Intersects>

The above filter selects those features with a geometry that intersects the point (1,1). 
A variety of spatial operators are available:

   * Intersects
   * Equals
   * Disjoint
   * Within
   * Overlaps
   * Crosses
   * DWithin
   * Beyond
   * Distance

Logical filters
---------------

Logical filters are used to create combinations of filters using the logical operators ``And``, ``Or``, and ``Not``. 
For example:

.. code-block:: xml 
  
   <And>
      <PropertyIsEqualTo>
         <PropertyName>NAME</PropertyName>
         <Literal>Bob</Literal>
      </PropertyIsEqualTo>
      <Intersects>
         <PropertyName>GEOMETRY</PropertyName>
         <Literal>
            <gml:Point>
                <gml:coordinates>1 1</gml:coordinates>
            </gml:Point>
         </Literal>
      </Intersects>
   </And>

.. _rules:

Rules
-----

A **rule** combines a filter with any number of symbolizers to define the portrayal of the features which satisfy the filter condition. 
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
to allow specifying the scale range in which a rule may be applied. 
This allows for varying portrayal of features at different map scales.
In particular, at smaller scales it is common to use simpler styling for features, 
or even prevent the display of some features altogether.

Consider the following example:

.. code-block:: xml 

  <Rule>
     <MaxScaleDenominator>20000</MaxScaleDenominator>
     <PointSymbolizer>
       <Graphic>
         <Mark>
           <Fill><CssParameter name="fill">#FF0000</CssParameter>
         </Mark>
       </Graphic>
     </PointSymbolizer>
  </Rule>
  <Rule>
     <MinScaleDenominator>20000</MinScaleDenominator>
     <PointSymbolizer>
       <Graphic>
         <Mark>
           <Fill><CssParameter name="fill">#0000FF</CssParameter>
         </Mark>
       </Graphic>
     </PointSymbolizer>
  </Rule>

The above rules specify that at scales above 1:20,000 features are symbolized with red points, 
and at scales at or below 1:20,000 features are symbolized with blue points.

