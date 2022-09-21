.. _community_vector_mosaic:

Vector Mosaic datastore
=======================

The ``Vector Mosaic datastore`` is a datastore that mosaics several vector datasets into a single layer. This provides the convenience of not having to create separate stores and layers for each constituent granule vector dataset.  The datastore uses the index table as an index to speed up cross dataset queries (e.g., finding the granules that match the current bbox and opening only those ones). 
Vector Mosaic datastore layers will have a feature type that incorporates the index table attributes (with the exception of geometry and connection parameters) combined with the vector granule attributes.

.. toctree::
   :maxdepth: 3

   installing
   configuration
   delegate

