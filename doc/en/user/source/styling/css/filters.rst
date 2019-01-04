.. _css_filters:

Filter syntax
=============

.. highlight:: css

Filters limit the set of features affected by a rule's properties.  There are
several types of simple filters, which can be combined to provide more complex
filters for rules.  

Combining filters
-----------------

Combination is done in the usual CSS way.  A rule with two filters separated by
a comma affects any features that match *either* filter, while a rule with
two filters separated by only whitespace affects only features that match
*both* filters.  Here's an example using a basic attribute filter (described
below)::

    /* Matches places where the lake is flooding */
    [rainfall>12] [lakes>1] {
        fill: black;
    }

    /* Matches wet places */
    [rainfall>12], [lakes>1] {
        fill: blue;
    }
    
When writing a selector that uses both *and* and *or* combinators, remember that the *and*
combinator has higher precedence. For example::

    restricted [cat='2'], [cat='3'], [cat='4'] [@sd <= 200k] [@sd > 100k] {
      fill: #EE0000;
    }
    
The above selector should be read as:

* typename is 'restricted' and ``cat='2'`` *or*
* ``cat='3'`` *or*
* ``cat='4'`` and scale is between 100000 and 200000

If instead the intention was to combine in or just the three cat filters, the right syntax would
have been::

    restricted [cat='2' or cat='3' or cat='4'] [@sd <= 200k] [@sd > 100k] {
      fill: #EE0000;
    }

Which should be read as:
 
* typename is 'restricted' *and*
* (``cat='2'`` or ``cat='3'`` or ``cat='4'``) *and*
* scale is between 100000 and 200000
 

Filtering on data attributes
----------------------------

An attribute filter matches some attribute of the data (for example, a column
in a database table).  This is probably the most common type of filter.  An
attribute filter takes the form of an attribute name and a data value separated
by some predicate operator (such as the less-than operator ``<``).

Supported predicate operators include the following:

.. list-table:: 
    :widths: 15 85
    :header-rows: 1

    * - Operator
      - Meaning
    * - ``=``  
      - The property must be exactly `equal` to the specified value.
    * - ``<>``
      - The property must not be exactly equal to the specified value.
    * - ``>``
      - The property must be greater than (or alphabetically later than) the
        specified value.
    * - ``>=``
      - The property must be greater than or equal to the specified value.
    * - ``<``
      - The property must be less than (or alphabetically earlier than) the
        specified value.
    * - ``<=`` 
      - The property must be less than or equal to the specified value.
    * - ``LIKE``  
      - The property must match the pattern described by the specified value.
        Patterns use ``_`` to indicate a single unspecified character and ``%``
        to indicate an unknown number of unspecified characters.

For example, to only render outlines for the states whose names start with
letters in the first half of the alphabet,  the rule would look like::

    [STATE_NAME<='M'] {
        stroke: black;
    }

.. note:: 
    The current implementation of property filters uses ECQL syntax, described
    on the `GeoTools documentation <http://docs.geotools.org/latest/userguide/library/cql/index.html>`_.

Filtering on type
-----------------

When dealing with data from multiple sources, it may be useful to provide rules
that only affect one of those sources.  This is done very simply; just specify
the name of the layer as a filter::

    states {
        stroke: black;
    }

Filtering by ID
---------------

For layers that provide feature-level identifiers, you can style specific
features simply by specifying the ID.  This is done by prefixing the ID with a
hash sign (``#``)::

    #states.2 {
        stroke: black;
    }

.. note:: 
    In CSS, the ``.`` character is not allowed in element ids; and the
    ``#states.foo`` selector matches the element with id ``states`` only if it also
    has the class ``foo``.  Since this form of identifier comes up so frequently in
    GeoServer layers, the CSS module deviates from standard CSS slightly in this
    regard.  Future revisions may use some form of munging to avoid this deviation.

Filtering by rendering context (scale)
--------------------------------------

Often, there are aspects of a map that should change based on the context in
which it is being viewed.  For example, a road map might omit residential roads
when being viewed at the state level, but feature them prominently at the
neighborhood level.  Details such as scale level are presented as
pseudo-attributes; they look like property filters, but the property names
start with an ``@`` symbol::

    [roadtype = 'Residential'][@sd > 100k] {
        stroke: black;
    }

The context details that are provided are as follows:

.. list-table::
    :widths: 20 80
    :header-rows: 1

    * - Pseudo-Attribute
      - Meaning
    * - @sd
      - The scale denominator for the current rendering.  More explicitly, this
        is the ratio of real-world distance to screen/rendered distance. 
    * - @scale
      - Same as above, the scale denominator (not scale) for the current rendering. 
        Supported for backwards compatibility 

The scale value can be expressed as a plain number, for for brevity and readability
the suffixes k (kilo), M (mega), G (giga) can be used, for example::

  [@sd > 100k]
  [@sd < 12M]
  [@sd < 1G]

.. note:: 
    While property filters (currently) use the more complex ECQL syntax,
    pseudo-attributes cannot use complex expressions and MUST take the form of
    <PROPERTY><OPERATOR><LITERAL>.

Filtering symbols
-----------------

When using symbols to create graphics inline, you may want to apply some
styling options to them.  You can specify style attributes for built-in symbols by using a few special selectors:

.. list-table::
    :widths: 30 70
    :header-rows: 1

    * - PseudoSelector
      - Meaning
    * - ``:mark``
      - specifies that a rule applies to symbols used as point markers
    * - ``:stroke`` 
      - specifies that a rule applies to symbols used as stroke patterns
    * - ``:fill``
      - specifies that a rule applies to symbols used as fill patterns
    * - ``:symbol`` 
      - specifies that a rule applies to any symbol, regardless of which
        context it is used in
    * - ``:nth-mark(n)`` 
      - specifies that a rule applies to the symbol used for the nth stacked
        point marker on a feature.
    * - ``:nth-stroke(n)`` 
      - specifies that a rule applies to the symbol used for the nth stacked
        stroke pattern on a feature.
    * - ``:nth-fill(n)``
      - specifies that a rule applies to the symbol used for the nth stacked
        fill pattern on a feature.
    * - ``:nth-symbol(n)`` 
      - specifies that a rule applies to the symbol used for the nth stacked
        symbol on a feature, regardless of which context it is used in.

For more discussion on using these selectors, see :ref:`css_styledmarks`.

Global rules
------------

Sometimes it is useful to have a rule that matches all features, for example,
to provide some default styling for your map (remember, by default nothing is
rendered).  This is accomplished using a single asterisk ``*`` in place of
the usual filter.  This catch-all rule can be used in complex expressions,
which may be useful if you want a rule to provide defaults as well as
overriding values for some features::

    * {
        stroke: black;
    }
