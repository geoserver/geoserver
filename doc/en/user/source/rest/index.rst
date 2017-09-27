.. _rest:

REST
====

GeoServer provides a `RESTful <http://en.wikipedia.org/wiki/Representational_state_transfer>`_ interface through which clients can retrieve information about an instance and make configuration changes. Using the REST interface's simple HTTP calls, clients can configure GeoServer without needing to use the :ref:`web_admin`.

REST is an acronym for "`REpresentational State Transfer <http://en.wikipedia.org/wiki/Representational_state_transfer>`_". REST adopts a fixed set of operations on named resources, where the representation of each resource is the same for retrieving and setting information. In other words, you can retrieve (read) data in an XML format and also send data back to the server in similar XML format in order to set (write) changes to the system.

Operations on resources are implemented with the standard primitives of HTTP:  GET to read; and PUT, POST, and DELETE to write changes. Each resource is represented as a URL, such as ``http://GEOSERVER_HOME/rest/workspaces/topp``.


API
---

The following links provide direct access to the GeoServer REST API documentation, including definitions and examples of each endpoint:

* :api:`/about/manifests <manifests.yaml>`
* :api:`/datastores <datastores.yaml>`
* :api:`/coverages <coverages.yaml>`
* :api:`/coveragestores <coveragestores.yaml>`
* :api:`/featuretypes <featuretypes.yaml>`
* :api:`/fonts <fonts.yaml>`
* :api:`/layergroups <layergroups.yaml>`
* :api:`/layers <layers.yaml>`
* :api:`/monitoring <monitoring.yaml>`
* :api:`/namespaces <namespaces.yaml>`
* :api:`/services/wms|wfs|wcs/settings <owsservices.yaml>`
* :api:`/reload <reload.yaml>`
* :api:`/resource <resource.yaml>`
* :api:`/security <security.yaml>`
* :api:`/settings <settings.yaml>`
* :api:`/structuredcoverages <structuredcoverages.yaml>`
* :api:`/styles <styles.yaml>`
* :api:`/templates <templates.yaml>`
* :api:`/transforms <transforms.yaml>`
* :api:`/wmslayers <wmslayers.yaml>`
* :api:`/wmsstores <wmsstores.yaml>`
* :api:`/wmtslayers <wmtslayers.yaml>`
* :api:`/wmtsstores <wmtsstores.yaml>`
* :api:`/workspaces <workspaces.yaml>`
* :api:`/usergroup <usergroup.yaml>`
* :api:`/roles <roles.yaml>`

* GeoWebCache:

  * :api:`/bounds <gwcbounds.yaml>`
  * :api:`/diskquota <gwcdiskquota.yaml>`
  * :api:`/filterupdate <gwcfilterupdate.yaml>`
  * :api:`/index <gwcindex.yaml>`
  * :api:`/layers <gwclayers.yaml>`
  * :api:`/masstruncate <gwcmasstruncate.yaml>`
  * :api:`/statistics <gwcmemorycachestatistics.yaml>`
  * :api:`/reload <gwcreload.yaml>`
  * :api:`/seed <gwcseed  .yaml>`

* Importer extension:

  * :api:`/imports <importer.yaml>`
  * :api:`/imports (tasks) <importerTasks.yaml>`
  * :api:`/imports (transforms) <importerTransforms.yaml>`
  * :api:`/imports (data) <importerData.yaml>`

* Monitor extension:
  
  * :api:`/monitor <monitoring.yaml>`

* XSLT extension:

  * :api:`/services/wfs/transforms <transforms.yaml>`

.. note:: You can also view the original :ref:`rest_api` section.

Examples
--------

This section contains a number of examples which illustrate some of the most common uses of the REST API. They are grouped by endpoint.


.. toctree::
   :maxdepth: 1

   about
   fonts
   layergroups
   layers
   security
   styles
   workspaces

.. toctree::
   :maxdepth: 1
   :hidden:

   api/index
