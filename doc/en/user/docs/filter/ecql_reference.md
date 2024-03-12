# ECQL Reference

This section provides a reference for the syntax of the ECQL language. The full language grammar is documented in the GeoTools [ECQL BNF definition](https://github.com/geotools/geotools/blob/main/modules/library/cql/ECQL.md)

## Syntax Notes

The sections below describe the major language constructs. Each construct lists all syntax options for it. Each option is defined as a sequence of other constructs, or recursively in terms of itself.

-   Symbols which are part of the ECQL language are shown in `code font`. All other symbols are part of the grammar description.
-   ECQL keywords are not case-sensitive.
-   A vertical bar symbol '**|**' indicates that a choice of keyword can be made.
-   Brackets '**[** \... **]**' delimit syntax that is optional.
-   Braces '**{** \... **}**' delimit syntax that may be present zero or more times.

## Condition {: #ecql_cond }

A filter condition is a single predicate, or a logical combination of other conditions.

|                                                                                                   |                                                      |
|---------------------------------------------------------------------------------------------------|------------------------------------------------------|
| **Syntax**                                                                                        | **Description**                                      |
| [Predicate](ecql_reference.md#ecql_pred)                                                         | Single predicate expression                          |
| [Condition](ecql_reference.md#ecql_cond) `AND` | `OR` [Condition](ecql_reference.md#ecql_cond) | Conjunction or disjunction of conditions             |
| `NOT` [Condition](ecql_reference.md#ecql_cond)                                                   | Negation of a condition                              |
| `(` | `[` [Condition](ecql_reference.md#ecql_cond) `]` | `)`                                   | Bracketing with `(` or `[` controls evaluation order |

## Predicate {: #ecql_pred }

Predicates are boolean-valued expressions which specify relationships between values.

|                                                                                                                                                                               |                                                                                                                                                     |
|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| **Syntax**                                                                                                                                                                    | **Description**                                                                                                                                     |
| [Expression](ecql_reference.md#ecql_expr) `=` | `<>` | `<` | `<=` | `>` | `>=` [Expression](ecql_reference.md#ecql_expr)                                               | Comparison operations                                                                                                                               |
| [Expression](ecql_reference.md#ecql_expr) **[** `NOT` **]** `BETWEEN` [Expression](ecql_reference.md#ecql_expr) `AND` [Expression](ecql_reference.md#ecql_expr)          | Tests whether a value lies in or outside a range (inclusive)                                                                                        |
| [Expression](ecql_reference.md#ecql_expr) **[** `NOT` **]** `LIKE` | `ILIKE` *like-pattern*                                                                               | Simple pattern matching. *like-pattern* uses the `%` character as a wild-card for any number of characters. `ILIKE` does case-insensitive matching. |
| [Attribute](ecql_reference.md#ecql_attr) **[** `NOT` **]** `IN (` [Expression](ecql_reference.md#ecql_expr) **{** `,`[Expression](ecql_reference.md#ecql_expr) **}** `)` | Tests whether an attribute value is (not) in a set of values.                                                                                       |
| **[** `NOT` **]** `IN (` [Literal](ecql_reference.md#ecql_literal) **{** `,`[Literal](ecql_reference.md#ecql_literal) **}** `)`                                           | Tests whether a feature ID value is (not) in a given set. ID values are integers or string literals                                                 |
| [Expression](ecql_reference.md#ecql_expr) `IS` **[** `NOT` **]** `NULL`                                                                                                    | Tests whether a value is (non-)null                                                                                                                 |
| [Attribute](ecql_reference.md#ecql_attr) `EXISTS` **|** `DOES-NOT-EXIST`                                                                                                    | Tests whether a featuretype does (not) have a given attribute                                                                                       |
| `INCLUDE` | `EXCLUDE`                                                                                                                                                        | Always include (exclude) features to which this filter is applied                                                                                   |

### Temporal Predicate {: #ecql_temp }

Temporal predicates specify the relationship of a time-valued expression to a time or time period.

|                                                                                                             |                                                              |
|-------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------|
| **Syntax**                                                                                                  | **Description**                                              |
| ``ecql_expr`` `BEFORE` `Time <ecql_reference.rst#ecql_literal>`{.interpreted-text role="ref"}_   | Tests whether a time value is before a point in time         |
| [Expression](ecql_reference.md#ecql_expr) `BEFORE OR DURING` [Time Period](ecql_reference.md#ecql_period) | Tests whether a time value is before or during a time period |
| [Expression](ecql_reference.md#ecql_expr) `DURING` [Time Period](ecql_reference.md#ecql_period)           | Tests whether a time value is during a time period           |
| [Expression](ecql_reference.md#ecql_expr) `DURING OR AFTER` [Time Period](ecql_reference.md#ecql_period)  | Tests whether a time value is during or after a time period  |
| ``ecql_expr`` `AFTER` `Time <ecql_reference.rst#ecql_literal>`{.interpreted-text role="ref"}_    | Tests whether a time value is after a point in time          |

### Spatial Predicate {: #ecql_spat }

Spatial predicates specify the relationship between geometric values. Topological spatial predicates (`INTERSECTS`, `DISJOINT`, `CONTAINS`, `WITHIN`, `TOUCHES` `CROSSES`, `OVERLAPS` and `RELATE`) are defined in terms of the DE-9IM model described in the OGC [Simple Features for SQL](http://www.opengeospatial.org/standards/sfs) specification.

|                                                                                                                                                                                                                                                                 |                                                                                                                                                                                                                                                                 |
|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Syntax**                                                                                                                                                                                                                                                      | **Description**                                                                                                                                                                                                                                                 |
| `INTERSECTS(`[Expression](ecql_reference.md#ecql_expr) `,` [Expression](ecql_reference.md#ecql_expr) `)`                                                                                                                                                      | Tests whether two geometries intersect. The converse of `DISJOINT`                                                                                                                                                                                              |
| `DISJOINT(`[Expression](ecql_reference.md#ecql_expr) `,` [Expression](ecql_reference.md#ecql_expr) `)`                                                                                                                                                        | Tests whether two geometries are disjoint. The converse of `INTERSECTS`                                                                                                                                                                                         |
| `CONTAINS(`[Expression](ecql_reference.md#ecql_expr) `,` [Expression](ecql_reference.md#ecql_expr) `)`                                                                                                                                                        | Tests whether the first geometry topologically contains the second. The converse of `WITHIN`                                                                                                                                                                    |
| `WITHIN(`[Expression](ecql_reference.md#ecql_expr) `,` [Expression](ecql_reference.md#ecql_expr) `)`                                                                                                                                                          | Tests whether the first geometry is topologically within the second. The converse of `CONTAINS`                                                                                                                                                                 |
| `TOUCHES(`[Expression](ecql_reference.md#ecql_expr) `,` [Expression](ecql_reference.md#ecql_expr) `)`                                                                                                                                                         | Tests whether two geometries touch. Geometries touch if they have at least one point in common, but their interiors do not intersect.                                                                                                                           |
| `CROSSES(`[Expression](ecql_reference.md#ecql_expr) `,` [Expression](ecql_reference.md#ecql_expr) `)`                                                                                                                                                         | Tests whether two geometries cross. Geometries cross if they have some but not all interior points in common                                                                                                                                                    |
| `OVERLAPS(`[Expression](ecql_reference.md#ecql_expr) `,` [Expression](ecql_reference.md#ecql_expr) `)`                                                                                                                                                        | Tests whether two geometries overlap. Geometries overlap if they have the same dimension, have at least one point each not shared by the other, and the intersection of the interiors of the two geometries has the same dimension as the geometries themselves |
| `EQUALS(`[Expression](ecql_reference.md#ecql_expr) `,` [Expression](ecql_reference.md#ecql_expr) `)`                                                                                                                                                          | Tests whether two geometries are topologically equal                                                                                                                                                                                                            |
| `RELATE(` [Expression](ecql_reference.md#ecql_expr) `,` [Expression](ecql_reference.md#ecql_expr) `,` *pattern* `)`                                                                                                                                           | Tests whether geometries have the spatial relationship specified by a DE-9IM matrix *pattern*. A DE-9IM pattern is a string of length 9 specified using the characters `*TF012`. Example: `'1*T***T**'`                                                         |
| `DWITHIN(` [Expression](ecql_reference.md#ecql_expr) `,` [Expression](ecql_reference.md#ecql_expr) `,` *distance* `,` *units* `)`                                                                                                                             | Tests whether the distance between two geometries is no more than the specified distance. *distance* is an unsigned numeric value for the distance tolerance. *units* is one of `feet`, `meters`, `statute miles`, `nautical miles`, `kilometers`               |
| `BEYOND(` [Expression](ecql_reference.md#ecql_expr) `,` [Expression](ecql_reference.md#ecql_expr) `,` *distance* `,` *units* `)`                                                                                                                              | Similar to `DWITHIN`, but tests whether the distance between two geometries is greater than the given distance.                                                                                                                                                 |
| `BBOX (` [Expression](ecql_reference.md#ecql_expr) `,` [Number](ecql_reference.md#ecql_literal) `,` [Number](ecql_reference.md#ecql_literal) `,` [Number](ecql_reference.md#ecql_literal) `,` [Number](ecql_reference.md#ecql_literal) [ `,` *CRS* ] `)` | Tests whether a geometry intersects a bounding box specified by its minimum and maximum X and Y values. The optional *CRS* is a string containing an SRS code (For example, `'EPSG:1234'`. The default is to use the CRS of the queried layer)                  |

## Expression {: #ecql_expr }

An expression specifies a attribute, literal, or computed value. The type of the value is determined by the nature of the expression. The standard [PEMDAS](http://en.wikipedia.org/wiki/Order_of_operations#Mnemonics) order of evaluation is used.

|                                                                                                                        |                                                                                                          |
|------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------|
| **Syntax**                                                                                                             | **Description**                                                                                          |
| [Attribute](ecql_reference.md#ecql_attr)                                                                              | Name of a feature attribute                                                                              |
| [Literal](ecql_reference.md#ecql_literal)                                                                             | Literal value                                                                                            |
| [Expression](ecql_reference.md#ecql_expr) `+` | `-` | `*` | `/` [Expression](ecql_reference.md#ecql_expr)         | Arithmetic operations                                                                                    |
| *function* `(` [ [Expression](ecql_reference.md#ecql_expr) { `,` [Expression](ecql_reference.md#ecql_expr) } ] `)` | Value computed by evaluation of a [filter function](function_reference.md) with zero or more arguments. |
| `(` \| `[` [Expression](ecql_reference.rst#ecql_expr) `]` \| `)`                                                       | Bracketing with `(` or ```` controls evaluation order                                                     |

## Attribute {#ecql_attr}

An attribute name denotes the value of a feature attribute.

-   Simple attribute names are sequences of letters and numbers, e.g. [States123``
-   Attribute names quoted with double-quotes may be any sequence of characters, e.g. ``\"States!@#\"``
-   [Note](https://gis.stackexchange.com/a/475826/68995): ``id`` is one of a few [reserved keywords](https://github.com/geotools/geotools/blob/2058be01323c3dea23d6df4d84b623be7f0b4102/modules/library/cql/src/main/jjtree/ECQLGrammar.jjt#L180) in ECQL and thus an attribute (or database column) named ``id`` must be quoted, e.g. ``\"id\"``

## Literal {#ecql_literal}

Literals specify constant values of various types.

**Type**

:   **Description**

*Number*

:   Integer or floating-point number. Scientific notation is supported.

*Boolean*

:   `TRUE` or `FALSE`

*String*

:   String literal delimited by single quotes. To include a single quote in the string use two single-quotes: `''`

*Geometry*

:   Geometry in WKT or EWKT format. WKT is defined in the OGC [Simple Features for SQL](http://www.opengeospatial.org/standards/sfs) specification. All standard geometry types are supported: `POINT`, `LINESTRING`, `POLYGON`, `MULTIPOINT`, `MULTILINESTRING`, `MULTIPOLYGON`, `GEOMETRYCOLLECTION`. EWKT allows specifying a geometry spatial reference system by prefixing it with a numerical code, in the form `SRID=number;WKT`, for example, `SRID=4326;POINT (1 2)`. A custom type of Envelope is also supported with syntax `ENVELOPE (` *x1* *x2* *y1* *y2* `)`.

*Time*

:   A UTC date/time value in the format `yyyy-mm-hhThh:mm:ss`. The seconds value may have a decimal fraction. The time zone may be specified as `Z` or `+/-hh:mm`. Example: `2006-11-30T00:30:00Z`

*Duration*

:   A time duration specified as `P` **\[** y `Y` m `M` d `D` **\]** `T` **\[** h `H` m `M` s `S` **\]**. The duration can be specified to any desired precision by including only the required year, month, day, hour, minute and second components. Examples: `P1Y2M`, `P4Y2M20D`, `P4Y2M1DT20H3M36S`

### Time Period {#ecql_period}

Specifies a period of time, in several different formats.

**Syntax**

:   **Description**

[Time](ecql_reference.rst#ecql_literal) `/` [Time](ecql_reference.rst#ecql_literal)

:   Period specified by a start and end time

[Duration](ecql_reference.rst#ecql_literal) `/` [Time](ecql_reference.rst#ecql_literal)

:   Period specified by a duration before a given time

[Time](ecql_reference.rst#ecql_literal) `/` [Duration](ecql_reference.rst#ecql_literal)

:   Period specified by a duration after a given time
