.. _css_cookbook:

CSS Cookbook
============

The CSS Cookbook is a collection of CSS "recipes" for creating various types of map styles.  Wherever possible, each example is designed to show off a single CSS feature so that code can be copied from the examples and adapted when creating CSS styles of your own. Most examples are shared with the SLD Cookbook, to make a comparison between the two syntaxes immediate.

The CSS Cookbook is divided into four sections: the first three for each of the vector types (points, lines, and polygons) and the fourth section for rasters.  Each example in every section contains a screen-shot showing actual GeoServer WMS output and the full CSS code for reference.

Each section uses data created especially for the Cookbooks (both CSS and SLD), with shapefiles for vector data and GeoTIFFs for raster data.  The projection for data is EPSG:4326.  All files can be easily loaded into GeoServer in order to recreate the examples.  

.. list-table::
   :widths: 20 80
   :header-rows: 1

   * - Data type
     - Shapefile
   * - Point
     - :download:`sld_cookbook_point.zip <../../sld/cookbook/artifacts/sld_cookbook_point.zip>`
   * - Line
     - :download:`sld_cookbook_line.zip <../../sld/cookbook/artifacts/sld_cookbook_line.zip>`
   * - Polygon
     - :download:`sld_cookbook_polygon.zip <../../sld/cookbook/artifacts/sld_cookbook_polygon.zip>`
   * - Raster
     - :download:`sld_cookbook_raster.zip <../../sld/cookbook/artifacts/sld_cookbook_raster.zip>`

.. toctree::
   :maxdepth: 2

   point
   line
   polygon
   raster
