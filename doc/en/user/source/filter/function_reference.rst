.. _filter_function_reference:

Filter Function Reference
==========================

This reference describes all filter functions that can be used in WFS/WMS filtering or in SLD expressions.

The list of functions available on a GeoServer instance can be determined by 
browsing to http://localhost:8080/geoserver/wfs?request=GetCapabilities 
and searching for ``ogc:FunctionNames`` in the returned XML.  
If a function is described in the Capabilities document but is not in this reference, 
then it might mean that the function cannot be used for filtering, 
or that it is new and has not been documented.  Ask for details on the user mailing list.

Unless otherwise specified, none of the filter functions in this reference are understood natively by the data stores, and thus expressions using them will be evaluated in-memory.

Function argument type reference
---------------------------------

.. list-table::
   :widths: 20 80
   
   * - **Type**
     - **Description**
   * - Double
     - Floating point number, 8 bytes, IEEE 754. Ranges from 4.94065645841246544e-324d to 1.79769313486231570e+308d
   * - Float
     - Floating point number, 4 bytes, IEEE 754. Ranges from 1.40129846432481707e-45 to 3.40282346638528860e+38. Smaller range and less accurate than Double.
   * - Integer
     - Integer number, ranging from -2,147,483,648 to 2,147,483,647
   * - Long
     - Integer number, ranging from -9,223,372,036,854,775,808 to +9,223,372,036,854,775,807
   * - Number
     - A numeric value of any type
   * - Object
     - A value of any type
   * - String
     - A sequence of characters
   * - Timestamp
     - Date and time information
     
Comparison functions
--------------------------------

.. list-table::
   :widths: 20 25 55
   
   
   * - **Name**
     - **Arguments**
     - **Description**
   * - between
     - ``num``:Number, ``low``:Number, ``high``:Number
     - returns true if ``low`` <= ``num`` <= ``high``
   * - equalTo
     - ``a``:Object, ``b``:Object
     - Can be used to compare for equality two numbers, two strings, two dates, and so on
   * - greaterEqualThan
     - ``x``:Object, ``y``:Object
     - Returns true if ``x`` >= ``y``. Parameters can be either numbers or strings (in the second case lexicographic ordering is used)
   * - greaterThan
     - ``x``:Object, ``y``:Object
     - Returns true if ``x`` > ``y``. Parameters can be either numbers or strings (in the second case lexicographic ordering is used)
   * - in2, in3, in4, in5, in6, in7, in8, in9, in10
     - ``candidate``:Object, ``v1``:Object, ..., ``v9``:Object
     - Returns true if ``candidate`` is equal to one of the ``v1``, ..., ``v9`` values. 
       Use the function name matching the number of arguments specified.
   * - in
     - ``candidate``:Object, ``v1``:Object, ``v2``:Object, ...
     - Works exactly the same as the in2, ..., in10 functions described above, but takes any number of values as input.
   * - isLike
     - ``string``:String, ``pattern``:String
     - Returns true if the string matches the specified pattern. For the full syntax of the pattern specification see the `Java Pattern class javadocs <http://java.sun.com/javase/6/docs/api/java/util/regex/Pattern.html>`_
   * - isNull
     - ``obj``:Object
     - Returns true the passed parameter is ``null``, false otherwise
   * - lessThan
     - ``x``:Object, ``y``:Object
     - Returns true if ``x`` < ``y``. Parameters can be either numbers or strings (in the second case lexicographic ordering is used
   * - lessEqualThan
     - ``x``:Object, ``y``:Object
     - Returns true if ``x`` <= ``y``. Parameters can be either numbers or strings (in the second case lexicographic ordering is used
   * - not
     - ``bool``:Boolean
     - Returns the negation of ``bool``
   * - notEqual
     - ``x``:Object, ``y``:Object
     - Returns true if ``x`` and ``y`` are equal, false otherwise
     
     
Control functions
--------------------------------

.. list-table::
   :widths: 20 25 55
   
   
   * - **Name**
     - **Arguments**
     - **Description**
   * - if_then_else
     - ``condition``:Boolean, ``x``:Object, ``y``: Object
     - Returns ``x`` if the condition is true, ``y`` otherwise

Environment function
--------------------

This function returns the value of environment variables
defined in various contexts.
Contexts which define environment variables include
:ref:`SLD rendering <sld_variable_substitution>`
and the :ref:`tutorials_animreflector`.

.. list-table::
   :widths: 20 25 55
   
   
   * - **Name**
     - **Arguments**
     - **Description**
   * - env
     - ``variable``:String
     - Returns the value of the environment variable ``variable``.



Feature functions
------------------

.. list-table::
   :widths: 20 25 55
   
   
   * - **Name**
     - **Arguments**
     - **Description**
   * - id
     - ``feature``:Feature
     - returns the identifier of the feature
   * - PropertyExists
     - ``f``:Feature, ``propertyName``:String
     - Returns ``true`` if ``f`` has a property named ``propertyName``
   * - property
     - ``f``:Feature, ``propertyName``:String
     - Returns the value of the property ``propertyName``.  
       Allows property names to be computed or specified by 
       :ref:`sld_variable_substitution`.
   * - mapGet
     - ``f``:Feature, ``map``:Map, ``key``:String
     - Get the value of the map ``map`` related to the specified ``key``.
     
Spatial Relationship functions
------------------------------

For more information about the precise meaning of the spatial relationships consult the `OGC Simple Feature Specification for SQL <http://www.opengeospatial.org/standards/sfs>`_

.. list-table::
   :widths: 20 25 55
   
   
   * - **Name**
     - **Arguments**
     - **Description**
   * - contains
     - ``a``:Geometry, ``b``:Geometry
     - Returns true if the geometry ``a`` contains ``b``
   * - crosses
     - ``a``:Geometry, ``b``:Geometry
     - Returns true if ``a`` crosses ``b``
   * - disjoint
     - ``a``:Geometry, ``b``:Geometry
     - Returns true if the two geometries are disjoint, false otherwise   
   * - equalsExact
     - ``a``:Geometry, ``b``:Geometry
     - Returns true if the two geometries are exactly equal, same coordinates in the same order
   * - equalsExactTolerance
     - ``a``:Geometry, ``b``:Geometry, ``tol``:Double
     - Returns true if the two geometries are exactly equal, same coordinates in the same order, allowing for a ``tol`` distance in the corresponding points
   * - intersects
     - ``a``:Geometry, ``b``:Geometry
     - Returns true if ``a`` intersects ``b``
   * - isWithinDistance
     - ``a``: Geometry, ``b``:Geometry, ``distance``: Double
     - Returns true if the distance between ``a`` and ``b`` is less than ``distance`` (measured as an euclidean distance)
   * - overlaps
     - ``a``: Geometry, ``b``:Geometry
     - Returns true ``a`` overlaps with ``b``
   * - relate
     - ``a``: Geometry, ``b``:Geometry
     - Returns the DE-9IM intersection matrix for ``a`` and ``b``
   * - relatePattern
     - ``a``: Geometry, ``b``:Geometry, ``pattern``:String
     - Returns true if the DE-9IM intersection matrix for ``a`` and ``b`` matches the specified pattern
   * - touches
     - ``a``: Geometry, ``b``: Geometry
     - Returns true if ``a`` touches ``b`` according to the SQL simple feature specification rules
   * - within
     - ``a``: Geometry, ``b``:Geometry
     - Returns true is fully contained inside ``b``

     
Geometric functions
--------------------

.. list-table::
   :widths: 20 25 55
   
   
   * - **Name**
     - **Arguments**
     - **Description**
   * - area
     - ``geometry``:Geometry
     - The area of the specified geometry. Works in a Cartesian plane, the result will be in the same unit of measure as the geometry coordinates (which also means the results won't make any sense for geographic data)
   * - boundary
     - ``geometry``:Geometry
     - Returns the boundary of a geometry
   * - boundaryDimension
     - ``geometry``:Geometry
     - Returns the number of dimensions of the geometry boundary
   * - buffer
     - ``geometry``:Geometry, ``distance``:Double
     - Returns the buffered area around the geometry using the specified distance
   * - bufferWithSegments
     - ``geometry``:Geometry, ``distance``:Double, ``segments``:Integer
     - Returns the buffered area around the geometry using the specified distance and using the specified number of segments to represent a quadrant of a circle.
   * - centroid
     - ``geometry``:Geometry
     - Returns the centroid of the geometry. Can be often used as a label point for polygons, though there is no guarantee it will actually lie inside the geometry 
   * - convexHull
     - ``geometry``:Geometry
     - Returns the convex hull of the specified geometry
   * - difference
     - ``a``:Geometry, ``b``:Geometry
     - Returns all the points that sit in ``a`` but not in ``b``
   * - dimension
     - ``a``:Geometry
     - Returns the dimension of the specified geometry
   * - distance
     - ``a``:Geometry, ``b``:Geometry
     - Returns the euclidean distance between the two geometries
   * - endAngle
     - ``line``:LineString
     - Returns the angle of the end segment of the linestring
   * - endPoint
     - ``line``:LineString
     - Returns the end point of the linestring
   * - envelope
     - ``geometry``:geometry
     - Returns the polygon representing the envelope of the geometry, that is, the minimum rectangle with sides parallels to the axis containing it
   * - exteriorRing
     - ``poly``:Polygon
     - Returns the exterior ring of the specified polygon
   * - geometryType
     - ``geometry``:Geometry
     - Returns the type of the geometry as a string. May be ``Point``, ``MultiPoint``, ``LineString``, ``LinearRing``, ``MultiLineString``, ``Polygon``, ``MultiPolygon``, ``GeometryCollection``
   * - geomFromWKT
     - ``wkt``:String
     - Returns the ``Geometry`` represented in the Well Known Text format contained in the ``wkt`` parameter
   * - geomLength
     - ``geometry``:Geometry
     - Returns the length/perimeter of this geometry (computed in Cartesian space)
   * - getGeometryN
     - ``collection``:GeometryCollection, ``n``:Integer
     - Returns the n-th geometry inside the collection
   * - getX
     - ``p``:Point
     - Returns the ``x`` ordinate of ``p``
   * - getY
     - ``p``:Point
     - Returns the ``y`` ordinate of ``p``
   * - getZ
     - ``p``:Point
     - Returns the ``z`` ordinate of ``p``
   * - interiorPoint
     - ``geometry``:Geometry
     - Returns a point that is either interior to the geometry, when possible, or sitting on its boundary, otherwise
   * - interiorRingN
     - ``polyg``:Polygon, ``n``:Integer
     - Returns the n-th interior ring of the polygon
   * - intersection
     - ``a``:Geometry, ``b``:Geometry
     - Returns the intersection between ``a`` and ``b``. The intersection result can be anything including a geometry collection of heterogeneous, if the result is empty, it will be represented by an empty collection.
   * - isClosed
     - ``line``: LineString
     - Returns true if ``line`` forms a closed ring, that is, if the first and last coordinates are equal
   * - isEmpty
     - ``geometry``:Geometry
     - Returns true if the geometry does not contain any point (typical case, an empty geometry collection)
   * - isometric
     - ``geometry``:Geometry, ``extrusion``:Double
     - Returns a MultiPolygon containing the isometric extrusions of all components of the input geometry. The extrusion distance is ``extrusion``, expressed in the same unit as the geometry coordinates. Can be used to get a pseudo-3d effect in a map
   * - isRing
     - ``line``:LineString
     - Returns true if the ``line`` is actually a closed ring (equivalent to ``isRing(line) and isSimple(line)``)
   * - isSimple
     - ``line``:LineString
     - Returns true if the geometry self intersects only at boundary points
   * - isValid
     - ``geometry``: Geometry
     - Returns true if the geometry is topologically valid (rings are closed, holes are inside the hull, and so on)
   * - numGeometries
     - ``collection``: GeometryCollection
     - Returns the number of geometries contained in the geometry collection
   * - numInteriorRing
     - ``poly``: Polygon
     - Returns the number of interior rings (holes) inside the specified polygon
   * - numPoint
     - ``geometry``: Geometry
     - Returns the number of points (vertexes) contained in ``geometry``
   * - offset
     - ``geometry``: Geometry, ``offsetX``:Double, ``offsetY``:Double
     - Offsets all points in a geometry by the specified X and Y offsets. Offsets are working in the same coordinate system as the geometry own coordinates.
   * - pointN
     - ``geometry``: Geometry, ``n``:Integer
     - Returns the n-th point inside the specified geometry
   * - startAngle
     - ``line``: LineString
     - Returns the angle of the starting segment of the input linestring
   * - startPoint
     - ``line``: LineString
     - Returns the starting point of the input linestring
   * - symDifference
     - ``a``: Geometry, ``b``:Geometry
     - Returns the symmetrical difference between ``a`` and ``b`` (all points that are inside ``a`` or ``b``, but not both)
   * - toWKT
     - ``geometry``: Geometry
     - Returns the WKT representation of ``geometry``
   * - union
     - ``a``: Geometry, ``b``:Geometry
     - Returns the union of ``a`` and ``b`` (the result may be a geometry collection)
   * - vertices
     - ``geom``: Geometry
     - Returns a multi-point made with all the vertices of ``geom``
   
   
	 
Math functions
--------------

.. list-table::
   :widths: 20 25 55
   
   
   * - **Name**
     - **Arguments**
     - **Description**
   * - abs
     - ``value``:Integer
     - The absolute value of the specified Integer ``value``
   * - abs_2
     - ``value``:Long
     - The absolute value of the specified Long ``value``
   * - abs_3
     - ``value``:Float
     - The absolute value of the specified Float ``value``
   * - abs_4
     - ``value``:Double
     - The absolute value of the specified Double ``value``
   * - acos
     - ``angle``:Double
     - Returns the arc cosine of an ``angle`` in radians, in the range of 0.0 through ``PI``
   * - asin
     - ``angle``:Double
     - Returns the arc sine of an ``angle`` in radians, in the range of ``-PI / 2`` through ``PI / 2``
   * - atan
     - ``angle``:Double
     - Returns the arc tangent of an angle in radians, in the range of ``-PI/2`` through ``PI/2``
   * - atan2
     - ``x``:Double, ``y``:Double
     - Converts a rectangular coordinate ``(x, y)`` to polar **(r, theta)** and returns **theta**.
   * - ceil
     - ``x``: Double
     - Returns the smallest (closest to negative infinity) double value that is greater than or equal to ``x`` and is equal to a mathematical integer.
   * - cos
     - ``angle``: Double
     - Returns the cosine of an ``angle`` expressed in radians
   * - double2bool
     - ``x``: Double
     - Returns ``true`` if ``x`` is zero, ``false`` otherwise
   * - exp
     - ``x``: Double
     - Returns Euler's number **e** raised to the power of ``x``
   * - floor
     - ``x``: Double
     - Returns the largest (closest to positive infinity) value that is less than or equal to ``x`` and is equal to a mathematical integer
   * - IEEERemainder
     - ``x``: Double, ``y``:Double
     - Computes the remainder of ``x`` divided by ``y`` as prescribed by the IEEE 754 standard
   * - int2bbool
     - ``x``: Integer
     - Returns true if ``x`` is zero, false otherwise
   * - int2ddouble
     - ``x``: Integer
     - Converts ``x`` to a Double
   * - log
     - ``x``: Integer
     - Returns the natural logarithm (base ``e``) of ``x``
   * - max, max_3, max_4
     - ``x1``: Double, ``x2``:Double, ``x3``:Double, ``x4``:Double
     - Returns the maximum between ``x1``, ..., ``x4``
   * - min, min_3, min_4
     - ``x1``: Double, ``x2``:Double, ``x3``:Double, ``x4``:Double
     - Returns the minimum between ``x1``, ..., ``x4``
   * - pi
     - None
     - Returns an approximation of ``pi``, the ratio of the circumference of a circle to its diameter
   * - pow
     - ``base``:Double, ``exponent``:Double
     - Returns the value of ``base`` raised to the power of ``exponent``
   * - random
     - None
     - Returns a Double value with a positive sign, greater than or equal to ``0.0`` and less than ``1.0``. Returned values are chosen pseudo-randomly with (approximately) uniform distribution from that range. 
   * - rint
     - ``x``:Double
     -  Returns the Double value that is closest in value to the argument and is equal to a mathematical integer. If two double values that are mathematical integers are equally close, the result is the integer value that is even.
   * - round_2
     - ``x``:Double
     -  Same as ``round``, but returns a Long
   * - round
     - ``x``:Double
     -  Returns the closest Integer to ``x``. The result is rounded to an integer by adding 1/2, taking the floor of the result, and casting the result to type Integer. In other words, the result is equal to the value of the expression ``(int)floor(a + 0.5)``
   * - roundDouble
     - ``x``:Double
     - Returns the closest Long to ``x``
   * - sin
     - ``angle``: Double
     - Returns the sine of an ``angle`` expressed in radians
   * - tan
     - ``angle``:Double
     - Returns the trigonometric tangent of ``angle`` expressed in radians
   * - toDegrees
     - ``angle``:Double
     - Converts an angle expressed in radians into degrees
   * - toRadians
     - ``angle``:Double
     - Converts an angle expressed in radians into degrees
   
   
String functions
-----------------   

String functions generally will accept any type of value for ``String`` arguments.  
Non-string values will be converted into a string representation automatically.

.. list-table::
   :widths: 20 25 55
   
   * - **Name**
     - **Arguments**
     - **Description**
   * - Concatenate
     - ``s1``:String, ``s2``:String, ...
     - Concatenates any number of strings.  Non-string arguments are allowed.
   * - strAbbreviate
     - ``sentence``:String, ``lower``:Integer, ``upper``:Integer, ``append``:String
     - Abbreviates the sentence at first space beyond ``lower`` (or at ``upper``
       if no space). Appends ``append`` if string is abbreviated.
   * - strCapitalize
     - ``sentence``:String
     - Fully capitalizes the sentence. For example, "HoW aRe YOU?" will be turned into "How Are You?"
   * - strConcat
     - ``a``:String, ``b``:String
     - Concatenates the two strings into one
   * - strDefaultIfBlank
     - ``str``:String, ``default``:String
     - returns ``default`` if ``str`` is empty, blank or null
   * - strEndsWith
     - ``string``:String, ``suffix``:String
     - Returns true if ``string`` ends with ``suffix``
   * - strEqualsIgnoreCase
     - ``a``:String, ``b``:String
     - Returns true if the two strings are equal ignoring case considerations
   * - strIndexOf
     - ``string``:String, ``substring``:String
     - Returns the index within this string of the first occurrence of the specified substring, or ``-1`` if not found
   * - strLastIndexOf
     - ``string``:String, ``substring``:String
     - Returns the index within this string of the last occurrence of the specified substring, or ``-1`` if not found
   * - strLength
     - ``string``:String
     - Returns the string length
   * - strMatches
     - ``string``:String, ``pattern``:String
     - Returns true if the string matches the specified regular expression. For the full syntax of the pattern specification see the `Java Pattern class javadocs <http://java.sun.com/javase/6/docs/api/java/util/regex/Pattern.html>`_
   * - strReplace
     - ``string``:String, ``pattern``:String, ``replacement``:String, ``global``: boolean
     - Returns the string with the pattern replaced with the given replacement text.  If the ``global`` argument is ``true`` then all occurrences of the pattern will be replaced, otherwise only the first. For the full syntax of the pattern specification see the `Java Pattern class javadocs <http://java.sun.com/javase/6/docs/api/java/util/regex/Pattern.html>`_
   * - strStartsWith
     - ``string``:String, ``prefix``:String
     - Returns true if ``string`` starts with ``prefix``
   * - strStripAccents
     - ``string``:String
     - Removes diacritics (~= accents) from a string. The case will not be altered.
   * - strSubstring
     - ``string``:String, ``begin``:Integer, ``end``:Integer
     - Returns a new string that is a substring of this string. The substring begins at the specified ``begin`` and extends to the character at index ``endIndex - 1`` (indexes are zero-based).
   * - strSubstringStart
     - ``string``:String, ``begin``:Integer
     - Returns a new string that is a substring of this string. The substring begins at the specified ``begin`` and extends to the last character of the string
   * - strToLowerCase
     - ``string``:String
     - Returns the lower case version of the string
   * - strToUpperCase
     - ``string``:String
     - Returns the upper case version of the string
   * - strTrim
     - ``string``:String
     - Returns a copy of the string, with leading and trailing white space omitted
   
   
   
     
Parsing and formatting functions
--------------------------------

.. list-table::
   :widths: 20 25 55
   
   * - **Name**
     - **Arguments**
     - **Description**
   * - dateFormat
     - ``format``:String, ``date``:Timestamp
     - Formats the specified date according to the provided format. The format syntax can be found in the `Java SimpleDateFormat javadocs <http://java.sun.com/javase/6/docs/api/java/text/SimpleDateFormat.html>`_
   * - dateParse
     - ``format``:String, ``dateString``:String
     - Parses a date from a ``dateString`` formatted according to the ``format`` specification. The format syntax can be found in the `Java SimpleDateFormat javadocs <http://java.sun.com/javase/6/docs/api/java/text/SimpleDateFormat.html>`_
   * - numberFormat
     - ``format``:String, ``number``:Double
     - Formats the number according to the specified ``format``. The format syntax can be found in the `Java DecimalFormat javadocs <http://java.sun.com/javase/6/docs/api/java/text/DecimalFormat.html>`_
   * - parseBoolean
     - ``boolean``:String
     - Parses a string into a boolean. The empty string, ``f``, ``0.0`` and ``0`` are considered false, everything else is considered true.
   * - parseDouble
     - ``number``:String
     - Parses a string into a double. The number can be expressed in normal or scientific form.
   * - parseInt
     - ``number``:String
     - Parses a string into an integer.
   * - parseLong
     - ``number``:String
     - Parses a string into a long integer
     
Transformation functions
--------------------------------

Transformation functions transform values from one data space into another.
These functions provide a concise way to compute styling parameters from feature attribute values.
See also :ref:`transformation_func`.

.. list-table::
   :widths: 20 25 55
   
   * - **Name**
     - **Arguments**
     - **Description**
   * - Recode
     - ``lookupValue``:Object, 
     
       ``data``:Object,
       ``value``:Object, ...
     - Transforms a ``lookupValue`` from a set of discrete data values into another set of values.
       Any number of ``data``/``value`` pairs may be specified.
   * - Categorize
     - ``lookupValue``:Object, 
       ``value``:Object,
       
       ``threshold``:Object, ...
       ``value``:Object,
       
       ``belongsTo`` : String
     - Transforms a continuous-valued attribute value into a set of discrete values.
       ``lookupValue`` and ``value`` must be an orderable type (typically numeric).
       The initial ``value`` is required.
       Any number of additional ``threshold``/``value`` pairs may be specified.
       ``belongsTo`` is optional, with the value ``succeeding`` or ``preceding``.
       It defines which interval to use when the lookup value equals a threshold value.
   * - Interpolate
     - ``lookupValue``:Numeric, 
       
       ``data``:Numeric,
       ``value``:Numeric *or* #RRGGBB, 
       ...
       
       ``mode``:String,
       ``method``:String
     - Transforms a continuous-valued attribute value into another continuous range of values.
       Any number of ``data``/``value`` pairs may be specified.
       ``mode`` is optional, with the value ``linear``, ``cosine`` or ``cubic``.
       It defines the interpolation algorithm to use.
       ``method`` is optional, with the value ``numeric`` or ``color``.
       It defines whether the target values are numeric or RGB color specifications.


