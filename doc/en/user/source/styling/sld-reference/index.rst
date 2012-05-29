.. _sld_reference:

SLD Reference
=============

In SLD documents ``Rule``\ s  and ``Filter``\ s are used to determine sets of features to be styled with particular symbology.
Rules may also specify the scale range in which the feature styling is visible.

.. toctree::
   :maxdepth: 2
   
   filters
   
Rues contain **symbolizers** to specify how features are styled.  
There are 5 types of symbolizers:  

* ``PointSymbolizer``, which styles features as **points**
* ``LineSymbolizer``, which styles features as **lines**
* ``PolygonSymbolizer``, which styles features as **polygons**
* ``TextSymbolizer``, which styles **text labels** for features
* ``RasterSymbolizer``, which styles **raster coverages**

Each symbolizer type has options and parameters to control styling.

   
.. toctree::
   :maxdepth: 2
   

   pointsymbolizer
   linesymbolizer
   polygonsymbolizer
   textsymbolizer
   labeling
   rastersymbolizer
