.. _kml_css:

Detecting switch from raster to vector representation in KML
============================================================

GeoServer 2.4 added a new icon server that KML output uses to make sure the point symbolisers look the same as in 
a normal WMS call no matter what scale they are looked at.

This may pose some issue when working in the default KML generation mode, where the map is a ground overlay up to
a certain scale, and switches to a vector, clickable representation once the number of features in the visualization
fall below a certain scale (as controlled by the ``KMSCORE`` parameter): the end user is not informed "visually" that
the switch happened.

There is however a custom enviroment variable, set by the KML generator, that styles can leverage to know whether
the KML generation is happening in ground overlay or vector mode.

The following example leverages this function to show a larger point symbol when points become clickable: 

.. code-block:: css

    * { 
      mark: symbol("circle");
    }

    :mark [env('kmlOutputMode') = 'vector'] {
      size: 8;
    }

    :mark {
      size: 4;
      fill: yellow;
      stroke: black;
    }

This will result in the following output:

.. figure:: ./images/kml-raster.png
   :align: center

   *Raster output, points are not yet clickable*

.. figure:: ./images/kml-vector.png
   :align: center
   
   *Vector output, points are clickable and painted as larger icons*

One important bit about the above CSS is that the order of the rules is important. The CSS to SLD translator uses specificity to decide which rule overrides which other one, and the specificity is driven, at the time of writing, only by scale rules and access to attributes. The filter using the ``kmlOutputMode`` filter is not actually using any feature attribute, so it has the same specificity as the catch all ``:mark`` rule. Putting it first ensures that it overrides the catch all rule anyways, while putting it second would result in the output size being always 4.
