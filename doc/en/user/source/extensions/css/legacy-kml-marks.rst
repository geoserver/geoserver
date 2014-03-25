.. _randomized_css:

Getting KML marks similar to the old KML encoder (pre v 2.4)
============================================================

The old KML generator was not able to truly respect the marks own shape, and as a result, was simply applying the
mark color to a fixed bull's eye like icon, for example:

.. figure:: ./images/legacy-kml-marks.png
   :align: center


Starting with GeoServer 2.4 the KML engine has been rewritten, and among other things, it can produce an exact
representation of the marks, respecting not only color, but also shape and stroking.
However, what if one want to reproduce the old output look?

The solution is to leverage the ability to respect marks appearance to the letter, and combine two superimposed
marks to generate the desired output:

.. code-block:: css

    * { 
      mark: symbol('circle'), symbol('circle');
      mark-size: 12, 4;
    }

    :nth-mark(1) {
      fill: red;
      stroke: black; 
      stroke-width: 2;
    }

    :nth-mark(2) {
      fill: black;
    }
    
Which results in the following Google Earth output:

.. figure:: ./images/kml-eyesbull-mark.png
   :align: center
