.. _sld_cookbook:

SLD Cookbook
============

The SLD Cookbook is a collection of SLD "recipes" for creating various types of map styles.  Wherever possible, each example is designed to show off a single SLD feature so that code can be copied from the examples and adapted when creating SLDs of your own.  While not an exhaustive reference like the :ref:`sld_reference` or the `OGC SLD 1.0 specification <http://www.opengeospatial.org/standards/sld>`_ the SLD Cookbook is designed to be a practical reference, showing common style templates that are easy to understand.

The SLD Cookbook is divided into four sections: the first three for each of the vector types (points, lines, and polygons) and the fourth section for rasters.  Each example in every section contains a screenshot showing actual GeoServer WMS output, a snippet of the SLD code for reference, and a link to download the full SLD.

Each section uses data created especially for the SLD Cookbook, with shapefiles for vector data and GeoTIFFs for raster data.  The projection for data is EPSG:4326.  All files can be easily loaded into GeoServer in order to recreate the examples.  

.. list-table::
   :widths: 20 80

   * - **Data Type**
     - **Shapefile**
   * - Point
     - :download:`sld_cookbook_point.zip <artifacts/sld_cookbook_point.zip>`
   * - Line
     - :download:`sld_cookbook_line.zip <artifacts/sld_cookbook_line.zip>`
   * - Polygon
     - :download:`sld_cookbook_polygon.zip <artifacts/sld_cookbook_polygon.zip>`
   * - Raster
     - :download:`sld_cookbook_raster.zip <artifacts/sld_cookbook_raster.zip>`

.. toctree::
   :maxdepth: 2

   points
   lines
   polygons
   rasters

