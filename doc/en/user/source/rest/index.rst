.. _rest:

REST
====

GeoServer provides a `RESTful <http://en.wikipedia.org/wiki/Representational_state_transfer>`_ interface through which clients can retrieve information about an instance and make configuration changes. Using the REST interface's simple HTTP calls, clients can configure GeoServer without needing to use the :ref:`web_admin`.

REST is an acronym for "`REpresentational State Transfer <http://en.wikipedia.org/wiki/Representational_state_transfer>`_". REST adopts a fixed set of operations on named resources, where the representation of each resource is the same for retrieving and setting information. In other words, you can retrieve (read) data in an XML format and also send data back to the server in similar XML format in order to set (write) changes to the system.

Operations on resources are implemented with the standard primitives of HTTP:  GET to read; and PUT, POST, and DELETE to write changes. Each resource is represented as a URL, such as ``http://GEOSERVER_HOME/rest/workspaces/topp``.


The following links provide direct access to the GeoServer REST API documentation, including definitions and examples of each endpoint:



* `/coverages <http://docs.geoserver.org/api/#/1.0.0/coverages.yaml>`__
* `/coveragestores <http://docs.geoserver.org/api/#/1.0.0/coveragestores.yaml>`__
* `/featuretypes <http://docs.geoserver.org/api/#/1.0.0/featuretypes.yaml>`__
* `/fonts <http://docs.geoserver.org/api/#/1.0.0/fonts.yaml>`__
* `/layergroups <http://docs.geoserver.org/api/#/1.0.0/layergroups.yaml>`__
* `/layers <http://docs.geoserver.org/api/#/1.0.0/layers.yaml>`__
* `/manifests <http://docs.geoserver.org/api/#/1.0.0/manifests.yaml>`__
* `/monitoring <http://docs.geoserver.org/api/#/1.0.0/monitoring.yaml>`__
* `/namespaces <http://docs.geoserver.org/api/#/1.0.0/namespaces.yaml>`__
* `/owsservices <http://docs.geoserver.org/api/#/1.0.0/owsservices.yaml>`__
* `/reload <http://docs.geoserver.org/api/#/1.0.0/reload.yaml>`__
* `/security <http://docs.geoserver.org/api/#/1.0.0/security.yaml>`__
* `/settings <http://docs.geoserver.org/api/#/1.0.0/settings.yaml>`__
* `/structuredcoverages <http://docs.geoserver.org/api/#/1.0.0/structuredcoverages.yaml>`__
* `/styles <http://docs.geoserver.org/api/#/1.0.0/styles.yaml>`__
* `/templates <http://docs.geoserver.org/api/#/1.0.0/templates.yaml>`__
* `/transforms <http://docs.geoserver.org/api/#/1.0.0/transforms.yaml>`__
* `/wmslayers <http://docs.geoserver.org/api/#/1.0.0/wmslayers.yaml>`__
* `/wmsstores <http://docs.geoserver.org/api/#/1.0.0/wmsstores.yaml>`__
* `/workspaces <http://docs.geoserver.org/api/#/1.0.0/workspaces.yaml>`__
* Importer extension:

  * `/imports <http://docs.geoserver.org/api/#/1.0.0/importer.yaml>`__
  * `/imports (tasks) <http://docs.geoserver.org/api/#/1.0.0/importerTasks.yaml>`__
  * `/imports (transforms) <http://docs.geoserver.org/api/#/1.0.0/importerTransforms.yaml>`__
  * `/import (data) <http://docs.geoserver.org/api/#/1.0.0/importerData.yaml>`__

.. note::

   For further information, please refer to the original :ref:`rest_api` section. For practical examples, refer to the :ref:`rest_examples` section.


.. toctree::
   :maxdepth: 1
   :hidden:
   
   fonts
   layers
   styles
   api/index
   examples/index


