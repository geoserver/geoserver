.. _css_valuetypes:

CSS value types
===============

.. highlight:: css

This page presents a brief overview of CSS types as used by this project.  Note
that these can be repeated as described in :ref:`css_multivalueprops`.

Numbers
-------

Numeric values consist of a number, or a number annotated with a measurement
value.  In general, it is wise to use measurement annotations most of the time,
to avoid ambiguity and protect against potential future changes to the default
units. 

Currently, the supported units include:

* Length

  * ``px`` pixels
  * ``m`` meters
  * ``ft`` feet

* Angle

  * ``deg`` degrees
    
* Ratio

  * ``%`` percentage 

When using expressions in place of numeric values, the first unit listed for
the type of measure is assumed.

Since the CSS module translates styles to SLD before any rendering occurs, its
model of unit-of-measure is tied to that of SLD.  In practice, this means that
for any particular symbolizer, there only one unit-of-measure applied for the
style.  Therefore, the CSS module extracts that unit-of-measure from one
special property for each symbolizer type.  Those types are listed below for
reference:

* ``fill-size`` determines the unit-of-measure for polygon symbolizers (but
  that doesn't matter so much since it is the only measure associated with
  fills)
* ``stroke-width`` determines the unit-of-measure for line symbolizers
* ``mark-size`` determines the unit-of-measure for point symbolizers
* ``font-size`` determines the unit-of-measure for text symbolizers and the
  associated halos

Strings
-------

String values consist of a small snippet of text.  For example, a string could
be a literal label to use for a subset of roads::

	[lanes>20] {
		label: "Serious Freaking Highway";
	}

Strings can be enclosed in either single or double quotes.  It's easiest to
simply use whichever type of quotes are not in your string value, but you can
escape quote characters by prefixing them with a backslash ``\``.  Backslash
characters themselves must also be prefixed.  For example, ``'\\\''`` is a
string value consisting of a single backslash followed by a single single quote
character.

Labels
------

While labels aren't really a special type of value, they deserve a special
mention since labels are more likely to require special string manipulation
than other CSS values.

If a label is a simple string value, then it works like any other string
would::

   [lanes > 20] {
       label: "Serious Freaking Highway";
   }

However, if a label has multiple values, all of those values will be
concatenated to form a single label::

   [lanes > 20] {
      label: "Serious " "Freaking " "Highway";
   }

Note the whitespace within the label strings here; *no whitespace is added*
when concatenating strings, so you must be explicit about where you want it
included.  You can also mix CQL expressions in with literal string values
here::

   states {
      label: [STATE_NAME] " (" [STATE_ABBR] ")";
   }

.. note::

    This automatic concatenation is currently a special feature only provided
    for labels.  However, string concatenation is also supported directly in
    CQL expressions by using the ``strConcat`` filter function::

        * { fill: [strConcat('#', color_hex)]; }

    This form of concatenation works with any property that supports
    expressions.


Colors
------

Color values are relatively important to styling, so there are multiple ways to
specify them.  

.. list-table::
    :widths: 20 80
    :header-rows: 1

    - * Format 
      * Interpretation 
    - * ``#RRGGBB``
      * A hexadecimal-encoded color value, with two digits each for red, green, and blue.
    - * ``#RGB``
      * A hexadecimal-encoded color value, with one digits each for red, green,
        and blue. This is equivalent to the two-digit-per-channel encoding with
        each digit duplicated.
    - * ``rgb(r, g, b)``
      * A three-part color value with each channel represented by a value in
        the range 0 to 1, or in the range 0 to 255.  0 to 1 is used if any of
        the values include a decimal point, otherwise it is 0 to 255.
    - * *Simple name* 
      * The simple English name of the color.  A full list of the supported
        colors is available at
        http://www.w3.org/TR/SVG/types.html#ColorKeywords

External references
-------------------

When using external images to decorate map features, it is necessary to
reference them by URL.  This is done by a call to the ``url`` function.  The
URL value may be wrapped in single or double-quotes, or not at all.  The same
escaping rules as for string values.  The ``url`` function is also a special
case where the surrounding quote marks can usually be omitted. Some examples::

    /* These properties are all equivalent. */

    * {
        stroke: url("http://example.com/");
        stroke: url('http://example.com/');
        stroke: url(http://example.com/);
    }

.. note:: 

    While relative URLs are supported, they will be fully resolved during the conversion process to SLD and written out as absolute URLs. 
    This may be cause problems when relocating data directories, etc.
    The style can be regenerated with the current correct URL by opening it in the demo editor and using the Submit button there.

Well-known marks
----------------

As defined in the SLD standard, GeoServer's ``css`` module  also allows using a
certain set of well-known mark types without having to provide graphic
resources explicitly.  These include:

* ``circle``
* ``square``
* ``cross``
* ``star``
* ``arrow``

And others.  Additionally, vendors can provide an extended set of well-known
marks, a facet of the standard that is exploited by some GeoTools plugins to
provide dynamic map features such as using characters from TrueType fonts as
map symbols, or dynamic charting.  In support of these extended mark names, the
css module provides a ``symbol`` function similar to ``url``.  The syntax is
the same, aside from the function name::

    * {
        mark: symbol(circle);
        mark: symbol('ttf://Times+New+Roman&char=0x19b2');
        mark: symbol("chart://type=pie&x&y&z");
    }
