.. _mbstyle_reference:

MBStyle references
==================

As MBstyle is heavily modeled on `JSON <http://json.org>`_, it may be useful to refer to the `JSON-Schema documentation <http://json-schema.org/documentation.html>`_ for basic syntax.

Mapbox Style Specification
--------------------------

For an extended reference to these styles check out the `Mapbox Style Specifications <https://www.mapbox.com/mapbox-gl-js/style-spec/>`_.

GeoTools MBStyle extension
--------------------------

The implementation used by GeoServer is documented here. The GeoTools project is responsible for the parser/encoder to convert between Mapbox Styles and GeoServer style objects.

This documentation is actively maintained and matches the capabilities in GeoServer:

* :geotools:`Specification <extension/mbstyle/spec/index.html>`

When reading the above reference keep in mind the specification is written in an additive fashion, where new features are documented along with the version number range for which they are supported.
   
As an example the basic functionality of `background-color` support is added in GeoTools `23.0`, as shown in the following table:

.. list-table::
  :widths: 19, 27, 27, 27
  :header-rows: 1

  * - Support
    - Mapbox
    - GeoTools
    - OpenLayers
  * - basic functionality
    - >= 0.10.0
    - Not yet supported
    - >= 2.4.0