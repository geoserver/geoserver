.. _stac_data_store_index:

STAC Datastore extension
========================

This plugin adds a vector data store that can connect to a STAC API, delivering collections
as feeare types, and items as features.

.. warning:: The current version requires a STAC API RC1 with the "search" conformance class. It will works best if the API supports field selection, sorting and CQL2 filtering, but can fall back on in-memory operations if they are not natively supported.

.. toctree::
   :maxdepth: 1

   install
   data-store
