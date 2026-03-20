.. _mbstyle_limitations:

GeoServer MBStyle Extension Limitations
=======================================

This document outlines all known limitations, missing features, and workarounds for the GeoServer MBStyle (Mapbox Style) extension. They have been initially compiled by `Gabriel Roldan <https://github.com/groldan>`_ and `Ronit Jadhav <https://github.com/ronitjadhav>`_ from Camptocamp.

Critical Limitations
--------------------

These are fundamental parsing limitations that prevent many common styling patterns and force verbose workarounds:

1. Glyphs Not Supported - Font System Incompatibility
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

**Issue**: GeoServer uses Java’s font system, not Mapbox’s glyph system

**Impact**: The ``glyphs`` property in the style specification is ignored

**Affects**:

-  Custom font loading from URLs
-  Mapbox-style glyph range loading
-  Remote font sources

**Workaround**: Use fonts installed on the server’s system or in GeoServer’s font directory

--------------

2. Sprites Limitation - Fixed Size Only
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

**Issue**: ``fill-pattern`` and other pattern properties don’t support dynamic sizing

**Impact**: Sprite images are referenced at their fixed size only

**Affects**:

-  ``fill-pattern``
-  ``line-pattern``
-  ``background-pattern``
-  Pattern scaling based on zoom or data

**Workaround**: Create multiple sprite images at different sizes or use appropriate resolution sprites (@2x, @3x)

--------------

3. JSONArray Literal Support - Core Blocker
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

**Issue**: ``[3, 1.5]`` throws “number from JSONArray not supported”

**Affects**:

-  Line dash arrays
-  Color arrays
-  Coordinate arrays
-  All array properties

**Example Failure**:

.. code:: json

   "line-dasharray": [3, 1.5]  // Fails entirely

**Impact**: Forces verbose ``case`` expressions instead of simple array values

**Workaround**: Must use complex case expressions instead of direct
array values

--------------

4. Missing Filter Operators - Forces Complex Workarounds
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

**Missing Operators**: ``within``, ``!in``, ``!has``, ``none``

**Issue**: Can’t use simple filters like:

.. code:: json

   ["!in", ["get", "class"], ["motorway", "trunk"]]

**Impact**: Forces complex ``case`` workarounds instead of concise filters

**Workaround**: Must write complex ``case`` statements with multiple ``==`` checks

--------------

5. Case Expression Array Returns
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

**Issue**: ``["case", condition, [3, 1.5], [1, 0]]`` fails on array return values

**Affects**:

-  Conditional styling with array properties
-  Dash patterns
-  Offsets

**Impact**: Conditional styling with array properties is completely unusable

**Workaround**: None - feature unusable

--------------

6. Multiple Sprite Sources - Modern Sprite Workflow Blocker
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

**Issue**: ``"sprite": [{"id": "basics", "url": "..."}]`` not supported

**Affects**:

-  Modern multi-sprite workflows
-  Sprite organization
-  Versatiles shortbread styles and other modern sprite patterns

**Example Failure**:

.. code:: json

   "sprite": [
     {"id": "basics", "url": "..."},
     {"id": "icons", "url": "..."}
   ]

**Additional Issue**: Namespace syntax not supported

.. code:: json

   "fill-pattern": "basics:pattern-warning"  // Fails

**Current Limitation**: Only single sprite URL string supported

**Impact**: Cannot use Versatiles shortbread styles and other modern sprite patterns

**Workaround**: Must consolidate all sprites into a single sprite sheet

--------------

7. Literal Expression Support
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

**Issue**: ``["literal", [1,2,3]]`` parsing fails

**Affects**:

-  Complex array expressions
-  Nested data structures
-  Advanced filtering and data-driven styling

**Impact**: Advanced filtering patterns unusable

**Workaround**: None available

--------------

Essential Missing Expression Functions
--------------------------------------

8. Interpolation Functions - Critical for Data-Driven Styling
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

**Missing Functions**: ``interpolate`` and ``step`` (return ``null``)

**Status**: Stub implementations exist but non-functional

**Critical For**:

-  Data-driven styling
-  Choropleth maps
-  Graduated symbols
-  Smooth color/size transitions based on data values

**Impact**: Cannot do smooth transitions based on data values

**Workaround**: Must use ``case`` expressions with discrete values

--------------

9. Math Functions - Basic Operations Missing
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

**Missing Functions**: ``abs``, ``ceil``, ``floor``, ``round``, ``min``, ``max``

**Issue**: Can’t perform basic data transformations

**Example Use Case**: Cannot round values for clean legend categories

**Impact**: Limited data manipulation capabilities

**Workaround**: Pre-process data or use limited available functions

**Update March 2026:**: ``abs``, ``ceil``, ``floor``, ``round`` have meanwhile been implemented, ``min`` and ``max`` are still missing.

--------------

10. Color Expression Support - Limited Color Handling
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

**Broken Functions**: ``rgba``, ``hsl``, ``hsla`` (throw exceptions)

**Missing Functions**: ``to-rgba``, ``to-hsla`` color conversions

**Impact**: Limited color manipulation options

**Workaround**: Use ``rgb`` and manual color calculations only

--------------

Filter Expression Parsing Issues
--------------------------------

11. Expression vs Array Ambiguity - Parser Confusion
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

**Issue**: ``MBObjectParser`` assumes all JSONArrays starting with strings are expressions

**Affects**:

-  Mixed type arrays
-  Literal number arrays

**Root Cause**: Need smarter array type detection

**Impact**: Parser confusion between expressions and literal arrays

**Workaround**: Avoid mixed-type arrays where possible

--------------

12. Nested Expression Support
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

**Issue**: Deep nesting of expressions not properly parsed

**Affects**:

-  Advanced conditional logic
-  Complex data transformations
-  Complex expression trees

**Impact**: Advanced logic patterns fail

**Workaround**: Flatten expressions where possible

--------------

13. Type Coercion - Inconsistent Type Handling
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

**Issue**: Mixed number/string arrays fail parsing

**Affects**: Flexible data-driven styling patterns

**Impact**: Reduced flexibility in dynamic styling

**Workaround**: Use consistent types within arrays

--------------

Secondary Missing Features
--------------------------

*Note: Lower priority for static map rendering*

Advanced Expression Categories
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

-  **Variable Binding** - ``let`` and ``var`` expressions (stub implementations only)
-  **Advanced Lookup** - ``slice``, ``split``, ``index-of``, ``at`` for complex data access
-  **Text Formatting** - ``format``, ``number-format`` for label styling
-  **Feature State** - ``feature-state``, ``accumulated`` for dynamic features
-  **String Operations** - ``is-supported-script``, ``resolved-locale``

Missing Layer Types
~~~~~~~~~~~~~~~~~~~

-  **Heatmap Layer** - Implementation stub exists but incomplete
-  **Hillshade Layer** - Terrain shading (useful but secondary)
-  **Sky Layer** - Atmospheric effects (not needed for static maps)

Advanced 3D/Dynamic Features
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

*Not relevant for static map rendering*

-  **3D Lighting** - ``light`` configuration support
-  **Terrain/DEM** - ``terrain`` or ``raster-dem`` source support
-  **Transitions** - Animation/transition properties
-  **Fog Effects** - Atmospheric fog rendering
-  **Advanced Projections** - Currently limited to Web Mercator

--------------

Performance Considerations
--------------------------

Memory & Processing
~~~~~~~~~~~~~~~~~~~

1. **Sprite Caching**: Sprites are cached for performance but may use significant memory
2. **Complex Expressions**: Very complex expressions may impact rendering performance
3. **3D Rendering**: Fill-extrusion creates multiple feature type styles, increasing overhead

GeoTools Integration Limits
~~~~~~~~~~~~~~~~~~~~~~~~~~~

1. Some Mapbox features have no GeoTools equivalent
2. Limited to GeoTools’ styling capabilities
3. 3D features are approximated using 2D techniques
4. Some vendor-specific options required for advanced features

--------------

Development Priorities
----------------------

*Prioritized by impact and implementation difficulty for static map rendering*

Phase 1: Critical Parser Fixes (High Impact, Medium Effort)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

1. **Fix JSONArray Literal Support**

   -  Modify ``MBObjectParser.numeric()`` and ``MBObjectParser.array()``
   -  Handle ``[3, 1.5]`` style arrays as literals, not expressions
   -  **Unblocks**: Line dash arrays, coordinate offsets, color arrays

2. **Multiple Sprite Sources Support**

   -  Support ``"sprite": [{"id": "basics", "url": "..."}]`` array
      format
   -  Implement sprite namespace resolution
      (``"basics:pattern-warning"``)
   -  Modify ``SpriteGraphicFactory`` to handle multiple sprite sources
   -  **Unblocks**: Versatiles shortbread styles and organized sprite workflows

3. **Complete Basic Filter Operators**

   -  Implement ``!in``, ``!has``, ``none``, ``within`` in
      ``MBFilter.java``
   -  Fix ``!`` (not) operator (partially implemented)
   -  **Enables**: Concise filtering without complex case statements

4. **Fix Case Expression Array Returns**

   -  Allow ``["case", condition, [array], [array]]`` patterns
   -  Handle mixed return types in case branches
   -  **Unblocks**: Conditional dash patterns, offset arrays

Phase 2: Expression Function Implementation (High Impact, High Effort)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

5. **Interpolation Functions**

   -  Implement working ``interpolate`` and ``step`` functions
   -  **Critical for**: Choropleth maps, graduated symbols, smooth transitions
   -  **Current status**: Stub implementations return ``null``

6. **Basic Math Functions**

   -  Implement ``abs``, ``ceil``, ``floor``, ``round``, ``min``, ``max``
   -  **Enables**: Data normalization, clean categorization
   -  **Low complexity, high utility**
   -  Update March 2026: ``abs``, ``ceil``, ``floor``, ``round`` `have meanwhile been implemented <https://github.com/geotools/geotools/pull/5532>`_

7. **Literal Expression Support**

   -  Proper ``["literal", array]`` parsing
   -  **Enables**: Complex nested data structures, advanced filtering

Phase 3: Color & Type Support (Medium Impact, Medium Effort)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

8. **Fix Color Expression Support**

   -  Fix ``rgba``, ``hsl``, ``hsla`` functions (currently throw exceptions)
   -  Implement ``to-rgba``, ``to-hsla`` conversions
   -  **Improves**: Color manipulation flexibility

9. **Type Coercion Improvements**

   -  Better handling of mixed-type arrays
   -  Smarter expression vs literal detection
   -  **Improves**: Overall parser reliability

Phase 4: Advanced Features (Medium Impact, High Effort)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

10. **Advanced Lookup Functions**

    -  Implement ``slice``, ``split``, ``index-of`` for array/string operations
    -  **Enables**: Complex data transformations

11. **Text Formatting**

    -  Implement ``format``, ``number-format`` for advanced labeling
    -  **Enables**: Rich text formatting, localized numbers

Not Prioritized for Static Maps
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

-  Variable binding (``let``, ``var``) - Complex, limited static use
-  Feature state - Dynamic feature management not needed
-  3D/animation features - Not relevant for static rendering
-  New layer types - Current types cover most static map needs

--------------

Summary of Expression Support
-----------------------------

Fully Supported Categories
~~~~~~~~~~~~~~~~~~~~~~~~~~

================== ========= ==============================================================
Category           Coverage  Examples
================== ========= ==============================================================
**Math**           20/25 ops ``+``, ``-``, ``*``, ``/``, ``sin``, ``cos``, ``min``, ``max``
**Decision Logic** 13/14 ops ``case``, ``match``, ``all``, ``any``, ``==``, ``!=``
**String**         3/5 ops   ``concat``, ``upcase``, ``downcase``
**Types**          11/15 ops ``to-string``, ``to-number``, ``typeof``, ``literal``
**Lookup**         4/11 ops  ``get``, ``has``, ``at``, ``length``
================== ========= ==============================================================

Partially Supported Categories
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

================ ======= ====================================
Category         Status  Missing
================ ======= ====================================
**Color**        Limited ``hsl``, ``hsla``, working ``rgba``
**Feature Data** Basic   ``feature-state``, ``line-progress``
**Camera**       Basic   ``pitch``, ``distance-from-center``
================ ======= ====================================

Not Implemented
~~~~~~~~~~~~~~~

-  **Interpolation** (``interpolate``, ``step``) - Critical for styling
-  **Variable Binding** (``let``, ``var``)
-  **Heatmap** (``heatmap-density``)

**Overall Coverage**: ~75% of the Mapbox Expression specification

--------------

Paint Property Limitations
--------------------------

Fill Layer
~~~~~~~~~~

-  :warning: ``fill-pattern`` - Doesn’t support dynamic sizing; it just references a sprite image at its fixed size

Line Layer
~~~~~~~~~~

-  :warning: ``line-dasharray`` - Functions not supported, only literal arrays

Symbol Layer
~~~~~~~~~~~~

-  :warning: ``icon-text-fit`` - Icon sizing relative to text supported, but padding not supported

--------------

Common Workarounds
------------------

For Missing Array Literal Support
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

**Problem**: Cannot use ``[3, 1.5]`` directly

**Workaround**: Use verbose case expressions

.. code:: json

   ["case", true, [3, 1.5], [1, 0]] // Still may fail

For Missing ``!in`` Operator
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

**Problem**: Cannot use ``["!in", value, array]``

**Workaround**: Use nested case with multiple equality checks

.. code:: json

   [
     "case",
     ["==", ["get", "class"], "motorway"],
     false,
     ["==", ["get", "class"], "trunk"],
     false,
     true
   ]

For Missing Interpolation
~~~~~~~~~~~~~~~~~~~~~~~~~

**Problem**: Cannot use smooth data-driven transitions

**Workaround**: Use discrete ``case`` statements

.. code:: json

   [
     "case",
     ["<", ["get", "population"], 1000],
     "#fee5d9",
     ["<", ["get", "population"], 5000],
     "#fcae91",
     ["<", ["get", "population"], 10000],
     "#fb6a4a",
     "#a50f15"
   ]

For Multiple Sprites
~~~~~~~~~~~~~~~~~~~~

**Problem**: Cannot use sprite arrays or namespaces

**Workaround**: Consolidate all sprites into a single sprite sheet with unique names

--------------

Related Resources
-----------------

-  `MapLibre Style Specification <https://maplibre.org/maplibre-style-spec/>`_
-  `GeoTools Styling Documentation <https://docs.geotools.org/latest/userguide/library/render/style.html>`_
-  `GeoTools Mapbox Expression Reference <https://docs.geotools.org/stable/userguide/extension/mbstyle/spec/expressions.html>`_

--------------

*Last Updated: March 20, 2026*
