.. _filter_ecql_reference:

ECQL Reference
==============

This section provides a reference for the syntax of the ECQL language.
The full language grammar is documented in the the GeoTools `ECQL BNF definition <https://github.com/geotools/geotools/blob/master/modules/library/cql/ECQL.md>`_ 

Syntax Notes
------------

The sections below describe the major language constructs.
Each construct lists all syntax options for it.
Each option is defined as a sequence of other constructs, or recursively in terms of itself.

* Symbols which are part of the ECQL language are shown in ``code font``.  
  All other symbols are part of the grammar description. 
* ECQL keywords are not case-sensitive. 
* A vertical bar symbol '**|**' indicates that a choice of keyword can be made.  
* Brackets '**[** ... **]**' delimit syntax that is optional.
* Braces '**{** ... **}**' delimit syntax that may be present zero or more times.
 

.. _ecql_cond:
 
Condition
---------
 
A filter condition is a single predicate, or a logical combination of other conditions.

.. list-table::
   :widths: 50 50
   
   * - **Syntax**
     - **Description**
   * - :ref:`ecql_pred`
     - Single predicate expression
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
   * - :ref:`ecql_expr` **[** ``NOT`` **]** ``LIKE`` | ``ILIKE`` *like-pattern*
     - Simple pattern matching.  
       *like-pattern* uses the ``%`` character as a wild-card for any number of characters.
       ``ILIKE`` does case-insensitive matching.
   * - :ref:`ecql_attr` **[** ``NOT`` **]** ``IN (`` :ref:`ecql_expr`  **{** ``,``:ref:`ecql_expr`  **}**  ``)`` 
     - Tests whether an expression value is (not) in a set of values
   * - ``IN (`` :ref:`ecql_literal`  **{** ``,``:ref:`ecql_literal`  **}**  ``)`` 
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
Topological spatial predicates
(``INTERSECTS``, ``DISJOINT``, ``CONTAINS``, ``WITHIN``, 
``TOUCHES`` ``CROSSES``, ``OVERLAPS`` and ``RELATE``)
are defined in terms of the DE-9IM model described in the 
OGC `Simple Features for SQL <http://www.opengeospatial.org/standards/sfs>`_ specification.

.. list-table::
   :widths: 50 50
   
   * - **Syntax**
     - **Description**
   * - ``INTERSECTS(``:ref:`ecql_expr` ``,`` :ref:`ecql_expr` ``)``
     - Tests whether two geometries intersect.
       The converse of ``DISJOINT`` 
   * - ``DISJOINT(``:ref:`ecql_expr` ``,`` :ref:`ecql_expr` ``)``
     - Tests whether two geometries are disjoint.
       The converse of ``INTERSECTS`` 
   * - ``CONTAINS(``:ref:`ecql_expr` ``,`` :ref:`ecql_expr` ``)``
     - Tests whether the first geometry topologically contains the second.
       The converse of  ``WITHIN`` 
   * - ``WITHIN(``:ref:`ecql_expr` ``,`` :ref:`ecql_expr` ``)``
     - Tests whether the first geometry is topologically within the second.
       The converse of ``CONTAINS``
   * - ``TOUCHES(``:ref:`ecql_expr` ``,`` :ref:`ecql_expr` ``)``
     - Tests whether two geometries touch.
       Geometries touch if they have at least one point in common, but their interiors do not intersect.
   * - ``CROSSES(``:ref:`ecql_expr` ``,`` :ref:`ecql_expr` ``)``
     - Tests whether two geometries cross.
       Geometries cross if they have some but not all interior points in common
   * - ``OVERLAPS(``:ref:`ecql_expr` ``,`` :ref:`ecql_expr` ``)``
     - Tests whether two geometries overlap.
       Geometries overlap if they have the same dimension, have at least one point each not shared by the other, and the intersection of the interiors of the two geometries has the same dimension as the geometries themselves
   * - ``EQUALS(``:ref:`ecql_expr` ``,`` :ref:`ecql_expr` ``)``
     - Tests whether two geometries are topologically equal
   * - ``RELATE(`` :ref:`ecql_expr` ``,`` :ref:`ecql_expr` ``,`` *pattern* ``)``
     - Tests whether geometries have the spatial relationship specified by a DE-9IM matrix *pattern*.
       A DE-9IM pattern is a string of length 9 specified using the characters ``*TF012``.
       Example: ``'1*T***T**'``
   * - ``DWITHIN(`` :ref:`ecql_expr` ``,`` :ref:`ecql_expr` ``,`` *distance* ``,`` *units* ``)``
     - Tests whether the distance between two geometries is no more than the specified distance.
       *distance* is an unsigned numeric value for the distance tolerance.
       *units* is one of ``feet``, ``meters``, ``statute miles``, ``nautical miles``, ``kilometers``      
   * - ``BEYOND(`` :ref:`ecql_expr` ``,`` :ref:`ecql_expr` ``,`` *distance* ``,`` *units* ``)``
     - Similar to ``DWITHIN``, but tests whether the distance between two geometries is greater than the given distance.
   * - ``BBOX (`` :ref:`ecql_expr` ``,``
       :ref:`Number <ecql_literal>` ``,`` :ref:`Number <ecql_literal>` ``,`` :ref:`Number <ecql_literal>` ``,`` :ref:`Number <ecql_literal>`
       [ ``,`` *CRS* ] ``)``
     - Tests whether a geometry intersects a bounding box 
       specified by its minimum and maximum X and Y values.  
       The optional *CRS* is a string containing an SRS code
       (For example, ``'EPSG:1234'``.  
       The default is to use the CRS of the queried layer)
     
     
.. _ecql_expr:

Expression
----------
 
An expression specifies a attribute, literal, or computed value.  
The type of the value is determined by the nature of the expression.
The standard `PEMDAS <http://en.wikipedia.org/wiki/Order_of_operations#Mnemonics>`_
order of evaluation is used.
 
.. list-table::
   :widths: 50 50
   
   * - **Syntax**
     - **Description**
   * - :ref:`ecql_attr`
     - Name of a feature attribute
   * - :ref:`ecql_literal`
     - Literal value
   * - :ref:`ecql_expr`  ``+`` | ``-`` | ``*`` | ``/`` :ref:`ecql_expr`
     - Arithmetic operations
   * - *function*  ``(`` [ :ref:`ecql_expr` { ``,`` :ref:`ecql_expr` } ] ``)``
     - Value computed by evaluation of a :ref:`filter function <filter_function_reference>`
       with zero or more arguments.
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
     - Geometry in WKT format. 
       WKT is defined in the OGC `Simple Features for SQL <http://www.opengeospatial.org/standards/sfs>`_ specification.
       All standard geometry types are supported:
       ``POINT``, ``LINESTRING``, ``POLYGON``, 
       ``MULTIPOINT``, ``MULTILINESTRING``, ``MULTIPOLYGON``, ``GEOMETRYCOLLECTION``.
       A custom type of Envelope is also supported 
       with syntax ``ENVELOPE (`` *x1* *x2* *y1* *y2* ``)``.
       
   * - *Time*
     - A UTC date/time value in the format ``yyyy-mm-hhThh:mm:ss``.
       The seconds value may have a decimal fraction.
       The time zone may be specified as ``Z`` or ``+/-hh:mm``.
       Example: ``2006-11-30T00:30:00Z``
   * - *Duration*
     - A time duration specified as ``P`` **[** y ``Y`` m ``M`` d ``D`` **]** ``T`` **[** h ``H`` m ``M`` s ``S`` **]**.  
       The duration can be specified to any desired precision by including 
       only the required year, month, day, hour, minute and second components.
       Examples: 
       ``P1Y2M``, 
       ``P4Y2M20D``, 
       ``P4Y2M1DT20H3M36S`` 
 


.. _ecql_period:

Time Period
^^^^^^^^^^^

Specifies a period of time, in several different formats.

.. list-table::
   :widths: 50 50
   
   * - **Syntax**
     - **Description**
   * - :ref:`Time <ecql_literal>` ``/`` :ref:`Time <ecql_literal>`
     - Period specified by a start and end time
   * - :ref:`Duration <ecql_literal>` ``/`` :ref:`Time <ecql_literal>`
     - Period specified by a duration before a given time
   * - :ref:`Time <ecql_literal>` ``/`` :ref:`Duration <ecql_literal>`
     - Period specified by a duration after a given time


 

