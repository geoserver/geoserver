CSS Style Examples
==================

Markers Sized by an Attribute Value
-----------------------------------

The following produces square markers at each point, but these are sized such that the area of each marker
is proprtional to the ``REPORTS`` attribute.  When zoomed in (when there are less points in view) the size
of the markers is doubled to make the smaller points more noticable.

::

  * {
    mark: symbol(square);
  }
  
  [@scale > 1000000] :mark {
    size: [sqrt(REPORTS)];
  }
  
  /* So that single-report points can be more easily seen */
  [@scale < 1000000] :mark {
    size: [sqrt(REPORTS)*2];
  }


This example uses the ``sqrt`` function.  There are many functions available for use in CSS and SLD.
For more details read - :doc:`/filter/function_reference`

