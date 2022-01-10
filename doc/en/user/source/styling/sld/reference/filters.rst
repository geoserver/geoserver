.. _sld_reference_filters:

Filters
=======

A *filter* is the mechanism in SLD for specifying conditions. 
They are similar in functionality to the SQL "WHERE" clause.
Filters are used within :ref:`sld_reference_rules` to determine which styles should be applied to which features in a data set.
The filter language used by SLD follows the `OGC Filter Encoding standard <http://www.opengeospatial.org/standards/filter>`_.
It is described in detail in the :ref:`filter_fe_reference`.

A filter condition is specified by using a **comparison operator** or a **spatial operator**,
or two or more of these combined by **logical operators**.
The operators are usually used to compare properties of the features being filtered
to other properties or to literal data.

Comparison operators
--------------------

Comparison operators are used to specify conditions on the non-spatial attributes of a feature. 
The following **binary comparison operators** are available:

 * ``<PropertyIsEqualTo>``
 * ``<PropertyIsNotEqualTo>``
 * ``<PropertyIsLessThan>``
 * ``<PropertyIsLessThanOrEqualTo>``
 * ``<PropertyIsGreaterThan>``
 * ``<PropertyIsGreaterThanOrEqualTo>``

These operators contain two :ref:`filter expressions <sld_filter_expression>` to be compared.
The first operand is often a ``<PropertyName>``, 
but both operands may be any expression, function or literal value.

Binary comparison operators may include a ``matchCase`` attribute with the value ``true`` or ``false``.
If this attribute is ``true`` (which is the default), string comparisons are case-sensitive.
If the attribute is specified and has the value ``false`` strings comparisons do not check case.

Other available **value comparison operators** are:

 * ``<PropertyIsLike>``
 * ``<PropertyIsNull>``
 * ``<PropertyIsBetween>``

``<PropertyIsLike>`` matches a string property value against a text **pattern**.
It contains a ``<PropertyName>`` element 
containing the name of the property containing the string to be matched 
and a ``<Literal>`` element containing the pattern.
The pattern is specified by a sequence of regular characters and
three special pattern characters.
The pattern characters are defined by the following required attributes of the ``<PropertyIsLike>`` element: 

 * ``wildCard`` specifies a pattern character which matches any sequence of zero or more characters
 * ``singleChar`` specifies a pattern character which matches any single character
 * ``escapeChar`` specifies an escape character which can be used to escape these pattern characters

``<PropertyIsNull>`` tests whether a property value is null.  
It contains a single ``<PropertyName>`` element containing the name of the property containing the value to be tested.

``<PropertyIsBetween>`` tests whether an expression value lies within a range.
It contains a :ref:`filter expression <sld_filter_expression>` providing the value to test,
followed by the elements ``<LowerBoundary>`` and ``<UpperBoundary>``, 
each containing a :ref:`filter expression <sld_filter_expression>`.

Examples
^^^^^^^^

* The following filter selects features whose ``NAME`` attribute has the value of "New York":

.. code-block:: xml 
   
   <PropertyIsEqualTo>
      <PropertyName>NAME</PropertyName>
      <Literal>New York</Literal>
   </PropertyIsEqualTo>

* The following filter selects features whose geometry area is greater than 1,000,000:

.. code-block:: xml 
   
   <PropertyIsGreaterThan>
      <ogc:Function name="area"> 
        <PropertyName>GEOMETRY</PropertyName>
      </ogc:Function>
      <Literal>1000000</Literal>
   </PropertyIsEqualTo>

   
Spatial operators
-----------------

Spatial operators are used to specify conditions on the geometric attributes of a feature. 
The following spatial operators are available:

**Topological Operators**

These operators test topological spatial relationships using the standard OGC Simple Features predicates: 

   * ``<Intersects>``
   * ``<Equals>``
   * ``<Disjoint>``
   * ``<Touches>``
   * ``<Within>``
   * ``<Overlaps>``
   * ``<Crosses>``
   * ``<Intersects>``
   * ``<Contains>``
   
The content for these operators is a ``<PropertyName>`` element 
for a geometry-valued property
and a GML geometry literal.
   
**Distance Operators**

These operators compute distance relationships between geometries:
   
   * ``<DWithin>``
   * ``<Beyond>``
   
The content for these elements is a ``<PropertyName>`` element for a geometry-valued property, 
a GML geometry literal, and a ``<Distance>`` element containing the value for the distance tolerance.
The ``<Distance>`` element may include an optional ``units`` attribute.
   
**Bounding Box Operator**

This operator tests whether a feature geometry attribute intersects a given bounding box:

   * ``<BBOX>``
   
The content is an optional ``<PropertyName>`` element, and a GML envelope literal.
If the ``PropertyName`` is omitted the default geometry attribute is assumed.
   
Examples
^^^^^^^^

* The following filter selects features with a geometry that intersects the point (1,1):


.. code-block:: xml 

   <Intersects>
      <PropertyName>GEOMETRY</PropertyName>
      <Literal>
         <gml:Point>
            <gml:coordinates>1 1</gml:coordinates>
         </gml:Point>
      </Literal>
   </Intersects>

   
* The following filter selects features with a geometry that intersects 
  the box [-10,0 : 10,10]:

.. code-block:: xml 

   <ogc:BBOX>
     <ogc:PropertyName>GEOMETRY</ogc:PropertyName>
     <gml:Box srsName="urn:x-ogc:def:crs:EPSG:4326">
       <gml:coord>
         <gml:X>-10</gml:X> <gml:Y>0</gml:Y>
       </gml:coord>
       <gml:coord>
         <gml:X>10</gml:X> <gml:Y>10</gml:Y>
       </gml:coord>
     </gml:Box>
   </ogc:BBOX>


Logical operators
-----------------

Logical operators are used to create logical combinations of other filter operators.
They may be nested to any depth.
The following logical operators are available:

 * ``<And>``
 * ``<Or>``
 * ``<Not>``
 
The content for ``<And>`` and ``<Or>`` is two filter operator elements.
The content for ``<Not>`` is a single filter operator element. 
 
Examples
^^^^^^^^

* The following filter uses ``<And>`` to combine a comparison operator and a spatial operator:

.. code-block:: xml 
  
   <And>
      <PropertyIsEqualTo>
         <PropertyName>NAME</PropertyName>
         <Literal>New York</Literal>
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

.. _sld_filter_expression:

Filter Expressions
------------------
 
Filter expressions allow performing computation on data values.
The following elements can be used to form expressions.

**Arithmetic Operators**

These operators perform arithmetic on numeric values.
Each contains two expressions as sub-elements.

 * ``<Add>``
 * ``<Sub>``
 * ``<Mul>``
 * ``<Div>``

**Functions**
 
The ``<Function>`` element specifies a filter function to be evaluated.
The ``name`` attribute gives the function name. 
The element contains a sequence of zero or more 
filter expressions providing the function arguments.
See the :ref:`filter_function_reference` for details of the functions provided by GeoServer.

**Feature Property Values**

The ``<PropertyName>`` element allows referring to the value of a given feature attribute.
It contains a string specifying the attribute name.

**Literals**

The ``<Literal>`` element allows specifying constant values
of numeric, boolean, string, date or geometry type.





