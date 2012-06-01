.. _sld_reference_filters:

Filters
=======

A *filter* is the mechanism in SLD for specifying predicates. 
Similar in nature to a "WHERE" clause in SQL, 
filters are used within :ref:`sld_reference_rules` to determine which styles should be applied to which features in a data set.
The filter language used by SLD follows the `OGC Filter Encoding standard <http://www.opengeospatial.org/standards/filter>`_.

There are three types of filters: **attribute**, **spatial** and **logical**.

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


