.. _css_nestedrules:

Nested rules
============

.. highlight:: scss

Starting with GeoServer 2.10 the CSS modules supports rule nesting, that is, 
a child rule can be written among properties of a parent rule.
The nested rules inherits the parent rule selector and properties, adding its
own extra selectors and property overrides.

Each nested rule can be written as normal, however, if other rules or properties
follow, it must be terminated with a semicolon (this char being the separator in the CSS language).

Nesting is a pure syntax improvement, as such it does not actually provide extra
functionality, besides more compact and hopefully readable styles.

This is an example of a CSS style using only cascading to get a different shape,
fill and stroke color for a point symbol in case the ``type`` attribute equals to ``important``::

  [@sd < 3000] {
    mark: symbol(circle)
  }
  
  [@sd < 3000] :mark {
    fill: gray;
    size: 5
  }
  
  [@sd < 3000] [type = 'important'] {
    mark: symbol('triangle')
  }
  
  [@sd < 3000] [type = 'important'] :mark {
    fill: red;
    stroke: yellow
  }

This second version uses rule nesting getting a more compact expression, putting related symbology
element close by::

  [@sd < 3000] {
     mark: symbol(circle);
     :mark {
        fill: gray;
        size: 5
     };
     [type = 'important'] {
        mark: symbol(triangle);
        :mark {
          fill: red;
          stroke: yellow
        }
     }
  }

