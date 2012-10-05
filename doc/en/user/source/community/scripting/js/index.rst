.. _scripting_js:

JavaScript
==========

The GeoServer scripting extension provides a number of scripting *hooks* that 
allow script authors to take advantage of extension points in GeoServer.

Hooks
-----

.. toctree::
  :maxdepth: 1

  app
  wps

GeoScript JS
------------

To provide a JavaScript interface for data access and manipulation via GeoTools, 
the GeoServer scripting extension includes the `GeoScript JS <http://geoscript.org/js/>`_
library.  To best leverage the scripting hooks in GeoServer, read through the 
`GeoScript JS API docs <http://geoscript.org/js/api/index.html>`_ for detail on
scripting access to GeoTools functionality with JavaScript.

GeoServer JavaScript Reference
------------------------------

In much the same way as GeoScript JS provides a convenient set of modules for
scripting access to GeoTools, the GeoServer scripting extension includes a 
``geoserver`` JavaScript module that allows convenient access to some of the 
GeoServer internals.  See the :ref:`scripting_javascript_api` for more detail.

.. toctree::
  :hidden:

  api/index
