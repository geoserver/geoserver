.. _filter_ecql_reference:

ECQL Reference
==============

This section provides a reference for the syntax of the ECQL language.
The subsections present the major language constructs.
Each construct is described by listing all syntax options for it
(which may be defined recursively in terms of the current or other constructs).

Syntax Notes
------------

* Keywords are not case-sensitive. 
* The vertical bar symbol **|** indicates where a choice of keyword can be made.  
* Brackets **[ ]** show optional syntax.
* Braces **{ }** show where syntax can be added zero or more times.
 

.. _ecql_cond:
 
Condition
---------
 
A filter condition is a boolean-valued predicate, or a logical combination of other conditions.

.. list-table::
   :widths: 50 50
   
   * - **Syntax**
     - **Description**
   * - :ref:`ecql_pred`
     - A predicate expression
   * - :ref:`ecql_cond` ``AND`` | ``OR`` :ref:`ecql_cond` 
     - Conjunction or disjunction of conditions
   * - ``NOT`` :ref:`ecql_cond`
     - Negation of a condition
   * - ``(`` | ``[`` :ref:`ecql_cond` ``]`` | ``)``
     - Bracketing with ``(`` or ``[`` controls evaluation order

.. _ecql_pred:
 
Predicate
---------

Predicates are boolean-valued expressions which specify relationships between values.

.. list-table::
   :widths: 50 50
   
   * - **Syntax**
     - **Description**
   * - :ref:`ecql_expr`  ``=`` | ``<>`` | ``<`` | ``<=`` | ``>`` | ``>=`` :ref:`ecql_expr`
     - Comparison operations
   * - :ref:`ecql_expr` **[** ``NOT`` **]** ``BETWEEN`` :ref:`ecql_expr` ``AND`` :ref:`ecql_expr` 
     - Tests whether a value lies in or outside a range (inclusive)
   * - :ref:`ecql_expr` **[** ``NOT`` **]** ``LIKE`` *like-pattern*
     - Simple pattern matching.  *like-pattern* uses the ```%`` character as a wild-card
   * - :ref:`ecql_expr` **[** ``NOT`` **]** ``IN (`` :ref:`ecql_expr`  **{** ``,``:ref:`ecql_expr`  **}**  ``)`` 
     - Tests whether an expression value is (not) in a set of values
   * - :ref:`ecql_expr` ``IN (`` :ref:`ecql_literal`  **{** ``,``:ref:`ecql_literal`  **}**  ``)`` 
     - Tests whether a feature ID value is in a given set. ID values are integers or string literals
   * - :ref:`ecql_expr` ``IS`` **[** ``NOT`` **]** ``NULL``
     - Tests whether a value is (non-)null
   * - :ref:`ecql_attr` ``EXISTS`` **|** ``DOES-NOT-EXIST``
     - Tests whether a featuretype does (not) have a given attribute
   * - ``INCLUDE`` | ``EXCLUDE``
     - Always include (exclude) features to which this filter is applied


.. _ecql_temp:
 
Temporal Predicate
^^^^^^^^^^^^^^^^^^

Temporal predicates specify the relationship of a time-valued expression to a time or time period.

.. list-table::
   :widths: 50 50
   
   * - **Syntax**
     - **Description**
   * - :ref:`ecql_expr`  ``BEFORE`` :ref:`Time <ecql_literal>` 
     - Tests whether a time value is before a point in time
   * - :ref:`ecql_expr`  ``BEFORE OR DURING`` :ref:`ecql_period`
     - Tests whether a time value is before or during a time period
   * - :ref:`ecql_expr`  ``DURING`` :ref:`ecql_period`
     - Tests whether a time value is during a time period
   * - :ref:`ecql_expr`  ``DURING OR AFTER`` :ref:`ecql_period`
     - Tests whether a time value is during or after a time period
   * - :ref:`ecql_expr`  ``AFTER`` :ref:`Time <ecql_literal>` 
     - Tests whether a time value is after a point in time


.. _ecql_spat:

Spatial Predicate
^^^^^^^^^^^^^^^^^

Spatial predicates specify the relationship between geometric values.

.. list-table::
   :widths: 50 50
   
   * - **Syntax**
     - **Description**
   * - ``INTERSECTS`` | ``DISJOINT`` | ``CONTAINS`` | ``WITHIN`` | ``TOUCHES`` | ``CROSSES`` | ``OVERLAPS`` | ``EQUALS`` ``(``:ref:`ecql_expr` ``,`` :ref:`ecql_expr` ``)``
     - Predicates for standard OGC spatial relationships
   * - ``RELATE`` ``(`` :ref:`ecql_expr` ``,`` :ref:`ecql_expr` ``,`` *pattern* ``)``
     - Tests whether geometries have the spatial relationship specified by a DE-9IM matrix *pattern*.
       A DE-9IM pattern is a string of length 9 specified using the characters ``*TF012``.
       Example: ``"1*T***T**"``
   * - ``DWITHIN`` | ``BEYOND`` ``(`` :ref:`ecql_expr` ``,`` :ref:`ecql_expr` ``,`` *distance* ``,`` *units* ``)``
     - Tests whether geometries are within (beyond) a distance.
       *distance* is an unsigned numeric value for the distance tolerance.
       *units* is one of ``feet``, ``meters``, ``statute miles``, ``nautical miles``, ``kilometers``      
   * - ``BBOX (`` :ref:`ecql_expr` ``,`` *Number* ``,`` *Number* ``,`` *Number* ``,`` *Number* **[** ``,`` *CRS* **]** ``)``
     - Tests whether a geometry intersects a bounding box 
       specified by its minimum and maximum X and Y values.  
       *CRS* is a string containing an SRS code (the default is *EPSG:4326*)
   * - ``BBOX (`` :ref:`ecql_expr` ``,`` :ref:`ecql_expr` **|** *Geometry* ``)``
     - Tests whether a geometry intersects a bounding box 
       specified by a geometric value computed by a function
       or provided by a geometry literal.
     
     
.. _ecql_expr:

Expression
----------
 
An expression specifies a attribute, literal, or computed value.  
The type of the value is determined by the nature of the expression.
 
.. list-table::
   :widths: 50 50
   
   * - **Syntax**
     - **Description**
   * - :ref:`ecql_attr`
     - Value of a feature attribute
   * - :ref:`ecql_literal`
     - Literal value
   * - :ref:`ecql_expr`  ``+`` | ``-`` | ``*`` | ``/`` :ref:`ecql_expr`
     - Arithmetic operations
   * - *function*  ``(`` :ref:`ecql_expr` { ``,`` :ref:`ecql_expr` } ``)``
     - Value computed by evaluation of a :ref:`filter function <filter_function_reference>`
   * - ``(`` | ``[`` :ref:`ecql_expr` ``]`` | ``)``
     - Bracketing with ``(`` or ``[`` controls evaluation order

     
.. _ecql_attr:
 
Attribute
---------

An attribute name denotes the value of a feature attribute.

* Simple attribute names are sequences of letters and numbers,
* Attribute names quoted with double-quotes may be any sequence of characters.

.. _ecql_literal:
 
Literal
-------

Literals specify constant values of various types.

.. list-table::
   :widths: 20 80
   
   * - **Type**
     - **Description**
   * - *Number*
     - Integer or floating-point number. Scientific notation is supported.
   * - *Boolean*
     - ``TRUE`` or ``FALSE``
   * - *String*
     - String literal delimited by single quotes.  To include a single quote in the
       string use two single-quotes: ``''``
   * - *Geometry*
     - Geometry in WKT format.  All standard types are supported:
       ``POINT``, ``LINESTRING``, ``POLYGON``, 
       ``MULTIPOINT``, ``MULTILINESTRING``, ``MULTIPOLYGON``, ``GEOMETRYCOLLECTION``.
       ``ENVELOPE`` is also supported.
   * - *Time*
     - A UTC date/time value in the format ``yyyy-mm-hhThh:mm:ss``.
       The seconds value may have a decimal fraction.
       The time zone may be specified as ``Z`` or ``+/-hh:mm``.
       Example: ``2006-11-30T00:30:00Z``
   * - *Duration*
     - A time duration specified as ``P`` **[** y ``Y`` m ``M`` d ``D`` **]** ``T`` **[** h ``H`` m ``M`` s ``S`` **]**.  
       The duration can be specified to any desired precision by including 
       only the required year, month, day, hour, minute and second values.
       Example: 
       ``P4Y2M``, 
       ``P4Y2M1DT20H3M36S`` 
 


.. _ecql_period:

Time Period
^^^^^^^^^^^

A construct specifying a duration of time, in several different ways.

.. list-table::
   :widths: 50 50
   
   * - **Syntax**
     - **Description**
   * - :ref:`Time <ecql_literal>` ``/`` :ref:`Time <ecql_literal>`
     - Period specified by start and end time
   * - :ref:`Time <ecql_literal>` ``/`` :ref:`Duration <ecql_literal>`
     - Period specified by a duration before a given time
   * - :ref:`Duration <ecql_literal>` ``/`` :ref:`Time <ecql_literal>`
     - Period specified by a duration after a given time


 

