.. _community_wfstemplating:

Features-Templating Extension
=============================

The Features Templating plug-in works by applying a template file as a mapping level over the stream of features received by a store, allowing a fine grained control over the final output of a getFeature request. More precisely it allows:

* To customize how features will be output in ``GeoJSON`` and ``GML`` output formats.
* To serves features in a ``JSON-LD`` format and allowing the same customization capabilities as for the other output formats. `JSON-LD <https://json-ld.org>`_ is a Linked Data format, based on JSON format, and revolves around the concept of "context" to provide additional mappings from JSON to an RDF model.

The first part of the plug-in documentation will go through the template syntax. The second one will show how to configure the template to apply it to a vector layer. The third part shows the backwards mapping functionality.


The plugin works for both simple and complex-features.

.. toctree::
   :maxdepth: 1

   installing
   directives 
   configuration  
   querying
