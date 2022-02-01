.. _community_solr_optimize:

Optimize rendering of complex polygons
--------------------------------------

Rendering large maps with complex polygons, to show the overall distribution of the data, can
take a significant toll, especially if GeoServer cannot connect to the SOLR server via a high
speed network.

A common approach to handle this issue is to add a second geometry to the SOLR documents,
representing the centroid of the polygon, and using that one to render the features when
fairly zoomed out.

Once the SOLR documents have been updated with a centroid column, and it has been populated,
the column can be added as a secondary geometry. Make sure to keep the polygonal geometry
as the default one:

.. figure:: images/optimize_ft1.png
   :align: center

... (other fields omitted)

.. figure:: images/optimize_ft2.png
   :align: center

   
   *Configuring a layer with multiple geometries*

With this setup the polygonal geometry will still be used for all spatial filters, and for
rendering, unless the style otherwise specifical demands for the centroid.

Then, a style with scale dependencies can be setup in order to fetch only then centroids
when fairly zoomed out, like in the following CSS example: ::

    [@scale > 50000] {
      geometry: [centroid];
      mark: symbol(square);
    }
    :mark {
      fill: red;
      size: 3;
    }​
    [@scale <= 50000] {
      fill: red;
      stroke: black;
    }

Using this style the ``spatial`` field will still be used to resolve the BBOX filter implicit
in the WMS requests, but only the much smaller ``centroid`` one will be transferred to GeoServer
for rendering. 