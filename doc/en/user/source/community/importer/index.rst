.. _community_importer:

Importer
========

The GeoServer importer provides another option for loading and publishing a variety of vector and raster format data. You can either access the Layer Importer through the GeoServer Web Interface, or programmatically through the Layer Importer REST API.

The Layer Importer differs from the standard GeoServer interface for loading data in a few ways. The Layer Importer loads stores and publishes layers in a single step, populating the fields with intelligent defaults. It also operates on multiple files at once. Finally, the Layer Importer creates a unique style (:ref:`SLD <styling>`) for every layer loaded.

.. toctree::
   :maxdepth: 2

   guireference
   formats
