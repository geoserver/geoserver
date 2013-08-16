.. _rest:

REST configuration
==================

GeoServer provides a `RESTful <http://en.wikipedia.org/wiki/Representational_state_transfer>`_ interface through which clients can retrieve information about an instance and make configuration changes. Using the REST interface's simple HTTP calls, clients can configure GeoServer without needing to use the :ref:`web_admin`.

REST is an acronym for "`REpresentational State Transfer <http://en.wikipedia.org/wiki/Representational_state_transfer>`_". REST adopts a fixed set of operations on named resources, where the representation of each resource is the same for retrieving and setting information. In other words, you can retrieve (read) data in an XML format and also send data back to the server in similar XML format in order to set (write) changes to the system.

Operations on resources are implemented with the standard primitives of HTTP:  GET to read; and PUT, POST, and DELETE to write changes. Each resource is represented as a URL, such as ``http://GEOSERVER_HOME/rest/workspaces/topp``.

For further information about the REST API, refer to the :ref:`rest_api` section. For practical examples, refer to the :ref:`rest_examples` section.
 
.. toctree::
   :maxdepth: 2

   api/index
   examples/index

