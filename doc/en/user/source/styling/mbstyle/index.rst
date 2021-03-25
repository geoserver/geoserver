.. _mbstyle_styling:

MBStyle Styling
===============

This module allows GeoServer to use Mapbox style documents directly.

A `Mapbox style document <https://www.mapbox.com/mapbox-gl-js/style-spec/>`__ is a JSON based language that defines the visual appearance of a map, what data is drawn, and the order data and styling to use when drawing.

A Mapbox style document is an alternative to SLD, with different strengths and weaknesses:

* Both Mapbox style and SLD documents can define an entire Map, selecting what data is drawn and in what order.

  As both these documents define the order in which layers are drawn they can be used to define a Layer Group (using the :guilabel:`Add Style Group` link).

* Mapbox style documents provide less control then the GeoServer SLD vendor options or accomplish a result using a different approach.
  
  A GeoServer SLD TextSymbolizers allows a label priority used when drawing labels. This priority can even be generated on the fly using an expression.  A MapBox style document producing the same effect would use several symbol layers, each drawing labels of different importance, and rely on draw order to ensure that the most important labels are drawn first (and are thus shown).

* The key advantage of Mapbox style documents is their compatibility with `Mapbox GL JS <https://docs.mapbox.com/mapbox-gl-js/>`__ and `OpenLayers <https://openlayers.org>`__.

  GeoServer publishes the styles used for rendering, web mapping clients or mobile apps to make use of the same Mapbox style document used by GeoServer.
  
* Feel free to experiment with Mapbox style documents, use the GeoServer REST API to convert to SLD (complete with GeoServer vendor options).

Mapbox style document support is not a part of GeoServer by default, but is available as an optional extension to install.

.. toctree::
   :maxdepth: 2

   installing
   source
   reference/index
   cookbook/index
