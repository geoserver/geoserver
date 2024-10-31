.. _extensions_importer:

Importer
========

The Importer extension gives a GeoServer administrator an alternate, more-streamlined method for uploading and configuring new layers.

There are two primary advantages to using the Importer over the standard GeoServer data-loading workflow:

#. **Supports batch operations** (loading and publishing multiple spatial files or database tables in one operation)

#. **Creates unique styles** for each layer, rather than linking to the same (existing) styles.

.. warning:: The importer extension allows upload of data and is currently unable to respect the file system sandbox,
    it uses a configurable location inside the data directory instead. Store creation will fail if the importer
    is used and the sandbox is set. See also :ref:`security_sandbox`.

This section will discuss the Importer extension.

.. toctree::
   :maxdepth: 1

   installing
   configuring
   using
   guireference
   formats
   rest_reference
   rest_examples
