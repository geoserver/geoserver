.. _monitor_configuration:

Monitor Configuration
=====================

Many aspects of the monitor extension are configurable. All configuration files
are stored in the data directory under the ``monitoring`` directory::

  <data_directory>
      monitoring/
          filter.properties
          monitor.properties


The ``monitor.properties`` file is the main configuration file whose contents are 
described in the following sections. The ``filter.properties`` 
allows for :ref:`filtering <request_filters>` out those requests from being monitored.

Database persistence with hibernate is described in more detail in the :ref:`monitor_db` section.

.. _monitor_storage:

Monitor Storage
---------------

How request data is persisted is configurable via the ``storage`` property defined in the 
``monitor.properties`` file. The following values are supported for the ``storage`` property:

* **memory** - Request data is to be persisted in memory alone.

The default value is ``memory``.

The "monitor hibernate" community module allows to also store the requests in a relational database.

Memory Storage
^^^^^^^^^^^^^^

With memory storage only the most recent 100 requests are stored. And by definition this 
storage is volatile in that if the GeoServer instance is restarted, shutdown, or crashes 
this data is lost.

.. _monitor_mode:

Monitor Mode
------------

The monitor extension supports different "monitoring modes" that control how
request data is captured. Currently two modes are supported:

* **history** *(Default)* - Request information updated post request only. No 
  live information made available.
* **live** - Information about a request is captured and updated in real time.

The monitor mode is set with the ``mode`` property in the ``monitor.properties`` file.
The default value is ``history``.

History Mode
^^^^^^^^^^^^

History mode persists information (sending it to storage) about a request after 
a request has completed. This mode is appropriate in cases where a user is most 
interested in analyzing request data after the fact and doesn't require real time
updates.

Live Mode
^^^^^^^^^

Live mode updates request data (sending it to storage) in real time as it 
changes. This mode is suitable for users who care about what a service is doing now.

Bounding Box
------------

When applicable one of the attributes the monitor extension can capture is the request
bounding box. In some cases, such as WMS and WCS requests, capturing the bounding box 
is easy. However in other cases  such as WFS it is not always possible to 100% reliably 
capture the bounding box. An example being a WFS request with a complex filter element. 

How the bounding box is captured is controlled by the ``bboxMode`` property in the 
``monitor.properties`` file. It can have one of the following values.

* **none** - No bounding box information is captured.
* **full** - Bounding box information is captured and heuristics are applied for WFS
  requests.
* **no_wfs** - Bounding box information is captured except for WFS requests.

Part of a bounding box is a coordinate reference system (crs).Similar to the WFS case it 
is not always straight forward to determine what the crs is. For this reason the ``bboxCrs`` 
property is used to configure a default crs to be used. The default value for the property is 
"EPSG:4326" and will be used in cases where all lookup heuristics fail to determine a crs for 
the bounding box.

Request Body Size
-----------------

The monitor extension will capture the contents of the request body when a body is 
specified as is common with a PUT or POST request. However since a request body can 
be large the extension limits the amount captured to the first 1024 bytes by default. 

A value of ``0`` indicates that no data from the request body should be captured. A 
value of ``-1`` indicates that no limit should be placed on the capture and the entire
body content should be stored.

This limit is configurable with the ``maxBodySize`` property of the ``monitor.properties``
file. 

.. note::

   When using database persistence it is important to ensure that the size of the body 
   field in the database can accommodate the ``maxBodySize`` property.

Ignore Post Processors
----------------------

The monitor passes request information through post processors which enrich the request
information with DNS lookup, Location using IP database etc. It is possible to disable
these post processors if some enrichments are not required with ``ignorePostProcessors``
property of the ``monitor.properties`` file.

This parameter takes comma separated names of known post processors.
The valid values are ``reverseDNS,geoIp,layerNameNormalizer``

.. _request_filters:

Request Filters
---------------

By default not all requests are monitored. Those requests excluded include any web admin requests or any :ref:`monitor_query_api` requests. These exclusions are configured in the ``filter.properties`` file:: 

   /rest/monitor/**
   /web/** 

These default filters can be changed or extended to filter more types of 
requests. For example to filter out all WFS requests the following entry
is added::

   /wfs

How to determine the filter path
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The contents of ``filter.properties`` are a series of ant-style patterns that 
are applied to the *path* of the request. Consider the following request::

   http://localhost:8080/geoserver/wms?request=getcapabilities

The path of the above request is ``/wms``. In the following request::

   http://localhost:8080/geoserver/rest/workspaces/topp/datastores.xml

The path is ``/rest/workspaces/topp/datastores.xml``.

In general, the path used in filters is comprised of the portion of the URL
after ``/geoserver`` (including the preceding ``/``) and before the query string ``?``:: 

   http://<host>:<port>/geoserver/<path>?<queryString>

.. note::  For more information about ant-style pattern matching, see the `Apache Ant manual <http://ant.apache.org/manual/dirtasks.html>`_.

Samples
-------

monitor.properties
^^^^^^^^^^^^^^^^^^

::

  # storage and mode
  storage=memory
  mode=history

  # request body capture
  maxBodySize=1024

  # bounding box capture
  bboxMode=no_wfs
  bboxCrs=EPSG:4326

filter.properties
^^^^^^^^^^^^^^^^^

::

  # filter out monitor query api requests
  /rest/monitor/**

  # filter out all web requests
  /web
  /web/**

  # filter out requests for WCS service
  /wcs