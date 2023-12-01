.. _css_multivalueprops:

Multi-valued properties
=======================

.. highlight:: css

When rendering maps, it is sometimes useful to draw the same feature multiple
times.  For example, you might want to stroke a roads layer with a thick line
and then a slimmer line of a different color to create a halo effect.

In GeoServer's ``css`` module, all properties may have multiple values.  There
is a distinction between complex properties, and multi-valued properties.
Complex properties are separated by spaces, while multi-valued properties are
separated by commas.  So, this style fills a polygon once::

    * {
        fill: url("path/to/img.png") red;
    }

Using ``red`` as a fallback color if the image cannot be loaded.  If you wanted
to draw red on top of the image, you would have to style like so::

    * {
        fill: url("path/to/img.png"), red;
        /* set a transparency for the second fill,
           leave the first fully opaque. */
        fill-opacity: 100%, 20%;
    }

For each type of symbolizer (``fill``, ``mark``, ``stroke``, and
``label``) the number of values determines the number of times the feature
will be drawn.  For example, you could create a bulls-eye effect by drawing
multiple circles on top of each other with decreasing sizes::

    * {
        mark: symbol(circle), symbol(circle), symbol(circle), symbol(circle);
        mark-size: 40px, 30px, 20px, 10px;
    }

If you do not provide the same number of values for an auxiliary property, the
list will be repeated as many times as needed to finish.  So::

    * {
        mark: symbol(circle), symbol(circle), symbol(circle), symbol(circle);
        mark-size: 40px, 30px, 20px, 10px;
        mark-opacity: 12%;
    }

makes all those circles 12% opaque.  (Note that they are all drawn on top of
each other, so the center one will appear 4 times as solid as the outermost
one.)

Inheritance
-----------

For purposes of inheritance/cascading, property lists are treated as
indivisible units.  For example::

    * {
        stroke: red, green, blue;
        stroke-width: 10px, 6px, 2px;
    }

    [type='special'] {
        stroke: pink;
    }

This style will draw the 'special' features with only one outline.  It has
``stroke-width: 10px, 6px, 2px;`` so that outline will be 10px wide.
