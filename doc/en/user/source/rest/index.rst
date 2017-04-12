.. _rest:

REST
====

GeoServer provides a `RESTful <http://en.wikipedia.org/wiki/Representational_state_transfer>`_ interface through which clients can retrieve information about an instance and make configuration changes. Using the REST interface's simple HTTP calls, clients can configure GeoServer without needing to use the :ref:`web_admin`.

REST is an acronym for "`REpresentational State Transfer <http://en.wikipedia.org/wiki/Representational_state_transfer>`_". REST adopts a fixed set of operations on named resources, where the representation of each resource is the same for retrieving and setting information. In other words, you can retrieve (read) data in an XML format and also send data back to the server in similar XML format in order to set (write) changes to the system.

Operations on resources are implemented with the standard primitives of HTTP:  GET to read; and PUT, POST, and DELETE to write changes. Each resource is represented as a URL, such as ``http://GEOSERVER_HOME/rest/workspaces/topp``.

The follow REST Reference provides both a REST API definition and examples of using each endpoint:

* coverages - `API Reference <../api/coverages/index.html>`__ ( `online <http://docs.geoserver.org/api/#/api/docs/coverages.yaml>`__ )
* coveragestores - `API Reference <../api/coveragestores/index.html>`__ ( `online <http://docs.geoserver.org/api/#/api/docs/coveragestores.yaml>`__ )
* featuretypes - `API Reference <../api/featuretypes/index.html>`__ ( `online <http://docs.geoserver.org/api/#/api/docs/featuretypes.yaml>`__ )
* :doc:`fonts` - `API Reference <../api/styles/fonts.html>`__ ( `online <http://docs.geoserver.org/api/#/api/docs/fonts.yaml>`__ )
* layergroups - `API Reference <../api/layergroups/index.html>`__ ( `online <http://docs.geoserver.org/api/#/api/docs/layergroups.yaml>`__ )
* :doc:`layers` - `API Reference <../api/layers/index.html>`__ ( `online <http://docs.geoserver.org/api/#/api/docs/layers.yaml>`__ )
* manifests - `API Reference <../api/manifests/index.html>`__ ( `online <http://docs.geoserver.org/api/#/api/docs/manifests.yaml>`__ )
* monitoring - `API Reference <../api/monitoring/index.html>`__ ( `online <http://docs.geoserver.org/api/#/api/docs/monitoring.yaml>`__ )
* namespaces - `API Reference <../api/namespaces/index.html>`__ ( `online <http://docs.geoserver.org/api/#/api/docs/namespaces.yaml>`__ )
* owsservices - `API Reference <../api/owsservices/index.html>`__ ( `online <http://docs.geoserver.org/api/#/api/docs/owsservices.yaml>`__ )
* reload - `API Reference <../api/reload/index.html>`__ ( `online <http://docs.geoserver.org/api/#/api/docs/reload.yaml>`__ )
* security - `API Reference <../api/security/index.html>`__ ( `online <http://docs.geoserver.org/api/#/api/docs/security.yaml>`__ )
* settings - `API Reference <../api/settings/index.html>`__ ( `online <http://docs.geoserver.org/api/#/api/docs/settings.yaml>`__ )
* structuredcoverages - `API Reference <../api/structuredcoverages/index.html>`__ ( `online <http://docs.geoserver.org/api/#/api/docs/structuredcoverages.yaml>`__ )
* :doc:`styles` - `API Reference <../api/styles/index.html>`__ ( `online <http://docs.geoserver.org/api/#/api/docs/styles.yaml>`__ )
* templates - `API Reference <../api/templates/index.html>`__ ( `online <http://docs.geoserver.org/api/#/api/docs/templates.yaml>`__ )
* transforms - `API Reference <../api/transforms/index.html>`__ ( `online <http://docs.geoserver.org/api/#/api/docs/transforms.yaml>`__ )
* wmslayers - `API Reference <../api/wmslayers/index.html>`__ ( `online <http://docs.geoserver.org/api/#/api/docs/wmslayers.yaml>`__ )
* wmsstores - `API Reference <../api/wmsstores/index.html>`__ ( `online <http://docs.geoserver.org/api/#/api/docs/wmsstores.yaml>`__ )
* :doc:`workspaces` - `API Reference <../api/workspaces/index.html>`__ ( `online <http://docs.geoserver.org/api/#/api/docs/workspaces.yaml>`__ )
* Importer Extension:

  * imports - `API Reference <../api/importer/index.html>`__ ( `online <http://docs.geoserver.org/api/#/api/docs/importer.yaml>`__ )
  * import tasks - `API Reference <../api/importerTasks/index.html>`__ ( `online <http://docs.geoserver.org/api/#/api/docs/importerTasks.yaml>`__ )
  * import transforms - `API Reference <../api/importerTransforms/index.html>`__ ( `online <http://docs.geoserver.org/api/#/api/docs/importerTransforms.yaml>`__ )
  * import data - `API Reference <../api/importerData/index.html>`__ ( `online <http://docs.geoserver.org/api/#/api/docs/importerData.yaml>`__ )

.. toctree::
   :maxdepth: 1
   :hidden:
   
   fonts
   layers
   styles

.. note::

   For further information about the refer to the original :ref:`rest_api` section. For practical examples, refer to the :ref:`rest_examples` section.

   .. toctree::
      :maxdepth: 1
      :hidden:

      api/index
      examples/index
