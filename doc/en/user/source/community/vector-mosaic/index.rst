.. _community_vector_mosaic:

Vector Mosaic datastore
=======================

The ``Vector Mosaic datastore`` is a datastore that mosaics several vector datasets into a single layer. This provides the convenience of not having to create separate stores and layers for each constituent granule vector dataset.  The datastore also supports using the layer that defines the collection (known as the "delegate layer") as an index to speed up cross dataset queries. Vector Mosaic datastore layers will have a featuretype that incorporates delegate attributes (with the exception of geometry and connection parameters) combined with the vector granule attributes.

.. toctree::
   :maxdepth: 3

   installing
   configuration
   delegate

