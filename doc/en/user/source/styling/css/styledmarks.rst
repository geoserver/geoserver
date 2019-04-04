.. _css_styledmarks:

Styled marks
============

.. highlight:: scss

GeoServer's CSS module provides a collection of predefined symbols that you can
use and combine to create simple marks, strokes, and fill patterns without
needing an image editing program.  You can access these symbols via the
symbol() CSS function.  For example, the built-in circle symbol makes it easy
to create a simple 'dot' marker for a point layer::

    * {
      mark: symbol(circle);
    }

Symbols work anywhere you can use a ``url()`` to reference an image (as in, you
can use symbols for stroke and fill patterns as well as markers.)

Symbol names
------------

GeoServer extensions can add extra symbols (such as the ``chart://`` symbol
family which allows the use of charts as symbols via a naming scheme similar to
the Google Charts API).  However, there are a few symbols that are always available:

   * circle
   * square
   * triangle
   * arrow
   * cross
   * star
   * x
   * shape://horizline
   * shape://vertline
   * shape://backslash
   * shape://slash
   * shape://plus
   * shape://times
   * windbarbs://default(size)[unit]

Symbol selectors
----------------

Symbols offer some additional styling options beyond those offered for image
references. To specify these style properties, just add another
rule with a special selector.  There are 8 "pseudoclass" selectors that are
used to style selectors:

    * ``:mark`` specifies that a rule applies to symbols used as point markers
    * ``:shield`` specifies that a rule applies to symbols used as label
      shields (icons displayed behind label text)
    * ``:stroke`` specifies that a rule applies to symbols used as stroke
      patterns
    * ``:fill`` specifies that a rule applies to symbols used as fill patterns
    * ``:symbol`` specifies that a rule applies to any symbol, regardless of
      which context it is used in
    * ``:nth-mark(n)`` specifies that a rule applies to the symbol used for the
      nth stacked point marker on a feature.
    * ``:nth-shield(n)`` specifies that a rule applies to the symbol used for
      the background of the nth stacked label on a feature
    * ``:nth-stroke(n)`` specifies that a rule applies to the symbol used for
      the nth stacked stroke pattern on a feature.
    * ``:nth-fill(n)`` specifies that a rule applies to the symbol used for the
      nth stacked fill pattern on a feature.
    * ``:nth-symbol(n)`` specifies that a rule applies to the symbol used for
      the nth stacked symbol on a feature, regardless of which context it is
      used in.
      
These pseudoclass selectors can be used in a top level rule, but starting with GeoServer 2.10,
they are more commonly used in sub-rules close to the mark property, to get better readability
(see example below).

Symbol styling properties
-------------------------

Styling a built-in symbol is similar to styling a polygon feature. However, the
styling options are slightly different from those available to a true polygon
feature:
 
    * The ``mark`` and ``label`` families of properties are unavailable for
      symbols.
    * Nested symbol styling is not currently supported.
    * Only the first ``stroke`` and ``fill`` will be used.
    * Additional ``size`` (as a length) and ``rotation`` (as an angle)
      properties are available.  These are analogous to the
      ``(mark|stroke|fill)-size`` and ``(mark|stroke|fill)-rotation``
      properties available for true geometry styling.

.. note:: 
    The various prefixed '-size' and '-rotation' properties on the
    containing style override those for the symbol if they are present.

Example styled symbol
---------------------

As an example, consider a situation where you are styling a layer that includes data about hospitals in your town.  You can create a simple hospital logo by placing a red cross symbol on top of a white circle background::

    [usage='hospital'] {
      mark: symbol('circle'), symbol('cross');
      :nth-mark(1) {
        size: 16px;
        fill: white;
        stroke: red;
      };
      :nth-mark(2) {
        size: 12px;
        fill: red;
      }
    }

Also an windbarb example where you get wind speed and direction from your data fields horSpeed and horDir (direction)::

    * {
      /* select windbard based on speed( here in meters per second, and south hemisphere) */
      mark: symbol('windbarbs://default(${horSpeed})[m/s]?hemisphere=s');
    
      /* rotate windbarb based on horDir property (in degrees) */
      mark-rotation: [horDir];
    
      mark-size: 20;
    }

