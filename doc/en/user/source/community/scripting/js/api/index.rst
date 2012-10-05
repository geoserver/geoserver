.. _scripting_javascript_api:

GeoServer JavaScript API Documentation
======================================

The scripting extension includes a ``geoserver/catalog`` module that allows
scripts to access resources in the GeoServer catalog.

The ``catalog`` module
----------------------

.. code-block:: javascript

  var catalog = require("geoserver/catalog");

Properties
----------

.. attribute:: namespaces

  ``Array``
  A list of namespace objects.  Namespaces have ``alias`` and ``uri`` 
  properties.

  .. code-block:: javascript

    catalog.namespaces.forEach(function(namespace) {
      // do something with namespace.alias or namespace.uri
    });


Methods
-------

.. function:: getVectorLayer(id)

  :arg id: ``String`` The fully qualified feature type identifier (e.g. 
    "topp:states")
  :returns: ``geoscript.layer.Layer``

  Access a feature type in the catalog as a `GeoScript Layer <http://geoscript.org/js/api/layer.html>`_.

  .. code-block:: javascript

    var states = catalog.getVectorLayer("topp:states");
    


