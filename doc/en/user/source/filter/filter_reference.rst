.. _filter_fe_reference:

Filter Encoding Reference
=========================

This is a reference for the **Filter Encoding** language
implemented in GeoServer.
The Filter Encoding language uses an XML-based syntax.
It is defined by the `OGC Filter Encoding standard <http://www.opengeospatial.org/standards/filter>`_.

Filters are used to select features or other objects from the context in which they are evaluated.
They are similar in functionality to the SQL "WHERE" clause.
A filter is specified using a **condition**.

.. _filter_condition:

Condition
---------

A condition is a single :ref:`filter_predicate` element, 
or a combination of conditions by :ref:`filter_logical`.

.. _filter_predicate:

Predicate
---------

Predicates are boolean-valued expressions which compute relationships between values.
A predicate is specified by using a **comparison operator** or a **spatial operator**.
The operators are used to compare properties of the features being filtered
to other feature properties or to literal data.

Comparison operators
^^^^^^^^^^^^^^^^^^^^

Comparison operators are used to specify conditions on non-spatial attributes. 

Binary Comparison operators
~~~~~~~~~~~~~~~~~~~~~~~~~~~

The **binary comparison operators** are:

 * ``<PropertyIsEqualTo>``
 * ``<PropertyIsNotEqualTo>``
 * ``<PropertyIsLessThan>``
 * ``<PropertyIsLessThanOrEqualTo>``
 * ``<PropertyIsGreaterThan>``
 * ``<PropertyIsGreaterThanOrEqualTo>``

They contain the elements:

.. list-table::
   :widths: 25 15 60
   
   * - **Element**
     - **Required?**
     - **Description**
   * - :ref:`filter_expression`
     - Yes
     - The first value to compare.
       Often a ``<PropertyName>``.
   * - :ref:`filter_expression`
     - Yes
     - The second value to compare

Binary comparison operator elements may include an optional ``matchCase`` attribute, 
with the value ``true`` or ``false``.
If this attribute is ``true`` (the default), string comparisons are case-sensitive.
If the attribute is ``false`` strings comparisons do not check case.
 
PropertyIsLike operator
~~~~~~~~~~~~~~~~~~~~~~~

The ``<PropertyIsLike>`` operator matches a string property value against a text **pattern**.
It contains the elements:

.. list-table::
   :widths: 25 15 60
   
   * - **Element**
     - **Required?**
     - **Description**
   * - ``<PropertyName>``
     - Yes
     - Contains a string specifying the name of the property to test
   * - ``<Literal>``
     - Yes
     - Contains a pattern string to be matched

The pattern is specified by a sequence of regular characters and
three special pattern characters.
The pattern characters are defined by the following *required* attributes of the ``<PropertyIsLike>`` element: 

 * ``wildCard`` specifies the pattern character which matches any sequence of zero or more string characters
 * ``singleChar`` specifies the pattern character which matches any single string character
 * ``escapeChar`` specifies the escape character which can be used to escape the pattern characters

PropertyIsNull operator
~~~~~~~~~~~~~~~~~~~~~~~

The ``<PropertyIsNull>`` operator tests whether a property value is null.  
It contains the element:

.. list-table::
   :widths: 25 15 60
   
   * - **Element**
     - **Required?**
     - **Description**
   * - ``<PropertyName>``
     - Yes
     - contains a string specifying the name of the property to be tested

PropertyIsBetweeen operator
~~~~~~~~~~~~~~~~~~~~~~~~~~~

The ``<PropertyIsBetween>`` operator tests whether an expression value lies within a range
given by a lower and upper bound (inclusive).
It contains the elements:

.. list-table::
   :widths: 25 15 60
   
   * - **Element**
     - **Required?**
     - **Description**
   * - :ref:`filter_expression`
     - Yes
     - The value to test
   * - ``<LowerBoundary>``
     - Yes
     - Contains an :ref:`filter_expression` giving the lower bound of the range
   * - ``<UpperBoundary>``
     - Yes
     - Contains an :ref:`filter_expression` giving the upper bound of the range
 
   
Spatial operators
^^^^^^^^^^^^^^^^^

Spatial operators are used to specify conditions on the geometric attributes of a feature. 
The following spatial operators are available:

Topological operators
~~~~~~~~~~~~~~~~~~~~~

These operators test topological spatial relationships using the standard OGC Simple Features predicates: 

   * ``<Intersects>`` - Tests whether two geometries intersect
   * ``<Disjoint>`` - Tests whether two geometries are disjoint (do not interact)
   * ``<Contains>`` - Tests whether a geometry contains another one
   * ``<Within>`` - Tests whether a geometry is within another one
   * ``<Touches>`` - Tests whether two geometries touch
   * ``<Crosses>`` - Tests whether two geometries cross
   * ``<Overlaps>`` - Tests whether two geometries overlap
   * ``<Equals>`` - Tests whether two geometries are topologically equal

These contains the elements:

.. list-table::
   :widths: 25 15 60
   
   * - **Element**
     - **Required?**
     - **Description**
   * - ``<PropertyName>``
     - Yes
     - Contains a string specifying the name of the geometry-valued property to be tested.
   * - *GML Geometry*
     - Yes
     - A GML literal value specifying the geometry to test against
   
Distance operators
~~~~~~~~~~~~~~~~~~

These operators test distance relationships between a geometry property and a geometry literal:
   
   * ``<DWithin>``
   * ``<Beyond>``
 
They contain the elements:

.. list-table::
   :widths: 25 15 60
   
   * - **Element**
     - **Required?**
     - **Description**
   * - ``<PropertyName>``
     - Yes
     - Contains a string specifying the name of the property to be tested.
       If omitted, the *default geometry attribute* is assumed.
   * - *GML Geometry*
     - Yes
     - A literal value specifying a geometry to compute the distance to. 
       This may be either a geometry or an envelope in GML 3 format
   * - ``<Distance>``
     - Yes
     - Contains the numeric value for the distance tolerance.
       The element may include an optional ``units`` attribute.

   
Bounding Box operator
~~~~~~~~~~~~~~~~~~~~~

The ``<BBOX>`` operator tests whether a geometry-valued property intersects a fixed bounding box.
It contains the elements:

.. list-table::
   :widths: 25 15 60
   
   * - **Element**
     - **Required?**
     - **Description**
   * - ``<PropertyName>``
     - No
     - Contains a string specifying the name of the property to be tested.
       If omitted, the *default geometry attribute* is assumed.
   * - ``<gml:Box>``
     - Yes
     - A GML Box literal value specifying the bounding box to test against

   
Examples
~~~~~~~~

* This filter selects features with a geometry that intersects the point (1,1).

.. code-block:: xml 

   <Intersects>
     <PropertyName>GEOMETRY</PropertyName>
     <gml:Point>
       <gml:coordinates>1 1</gml:coordinates>
     </gml:Point>
   </Intersects>

* This filter selects features with a geometry that overlaps a polygon.

.. code-block:: xml 

   <Overlaps>
     <PropertyName>Geometry</PropertyName>
     <gml:Polygon srsName="http://www.opengis.net/gml/srs/epsg.xml#63266405">
       <gml:outerBoundaryIs>
         <gml:LinearRing>
            <gml:posList> ... </gml:posList>
         </gml:LinearRing>
       </gml:outerBoundaryIs>
     </gml:Polygon>
    </Overlaps>
   
* This filter selects features with a geometry that intersects 
  the geographic extent [-10,0 : 10,10].

.. code-block:: xml 

   <BBOX>
     <PropertyName>GEOMETRY</PropertyName>
     <gml:Box srsName="urn:x-ogc:def:crs:EPSG:4326">
       <gml:coord>
         <gml:X>-10</gml:X> <gml:Y>0</gml:Y>
       </gml:coord>
       <gml:coord>
         <gml:X>10</gml:X> <gml:Y>10</gml:Y>
       </gml:coord>
     </gml:Box>
   </BBOX>

   
.. _filter_logical:

Logical operators
-----------------

Logical operators are used to specify 
logical combinations of :ref:`filter_condition` elements
(which may be either :ref:`filter_predicate` elements or other **logical operators**).
They may be nested to any depth.

The following logical operators are available:

 * ``<And>`` - computes the logical conjunction of the operands
 * ``<Or>`` - computes the logical disjunction of the operands
 
The content for ``<And>`` and ``<Or>`` is two operands given by :ref:`filter_condition` elements.

 * ``<Not>`` - computes the logical negation of the operand

The content for ``<Not>`` is a single operand given by a :ref:`filter_condition` element. 
 
Examples
^^^^^^^^

* This filter uses ``<And>`` to combine a comparison predicate and a spatial predicate:

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


.. _filter_expression:

Expression
----------

**Filter expressions** specify constant, variable or computed data values.
An expression is formed from one of the following elements
(some of which contain sub-expressions,
meaning that expressions may be of arbitrary depth):

Arithmetic operators
^^^^^^^^^^^^^^^^^^^^

The **arithmetic operator** elements compute arithmetic operations on numeric values.

 * ``<Add>`` - adds the two operands
 * ``<Sub>`` - subtracts the second operand from the first
 * ``<Mul>`` - multiplies the two operands
 * ``<Div>`` - divides the first operand by the second
 
Each arithmetic operator element contains two :ref:`filter_expression` elements
providing the operands.

Function
^^^^^^^^
 
The ``<Function>`` element specifies a filter function to be evaluated.
The required ``name`` attribute gives the function name. 
The element contains a sequence of zero or more 
:ref:`filter_expression` elements providing the values of the function arguments.

See the :ref:`filter_function_reference` for details of the functions provided by GeoServer.

Property Value
^^^^^^^^^^^^^^

The ``<PropertyName>`` element refers to the value of a feature attribute.
It contains a **string** or an **XPath expression** specifying the attribute name.

Literal
^^^^^^^

The ``<Literal>`` element specifies a constant value.
It contains data of one of the following types:

.. list-table::
   :widths: 25 75
   
   * - **Type**
     - **Description**
   * - Numeric
     - A string representing a numeric value (integer or decimal).
   * - Boolean
     - A boolean value of ``true`` or ``false``.
   * - String
     - A string value.
       XML-incompatible text may be included by using 
       **character entities** or ``<![CDATA[`` ``]]>`` delimiters.
   * - Date
     - A string representing a date.
   * - Geometry
     - An element specifying a geometry in GML3 format.

WFS 2.0 namespaces
------------------

WFS 2.0 does not depend on any one GML version and thus requires an explicit namespace and schemaLocation for GML.
In a GET request, namespaces can be placed on a Filter element (that is, ``filter=`` the block below, URL-encoded):

.. code-block:: xml 
 
    <fes:Filter
            xmlns:fes="http://www.opengis.net/fes/2.0"
            xmlns:gml="http://www.opengis.net/gml/3.2">
        <fes:Not>
            <fes:Disjoint>
                <fes:ValueReference>sf:the_geom</fes:ValueReference>
                <gml:Polygon
                        gml:id="polygon.1"
                        srsName='http://www.opengis.net/def/crs/EPSG/0/26713'>
                    <gml:exterior>
                        <gml:LinearRing>
                            <gml:posList>590431 4915204 590430
                                4915205 590429 4915204 590430
                                4915203 590431 4915204</gml:posList>
                        </gml:LinearRing>
                    </gml:exterior>
                </gml:Polygon>
            </fes:Disjoint>
        </fes:Not>
    </fes:Filter>
