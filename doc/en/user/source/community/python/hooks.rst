.. _python_hooks:

Python Scripting Hooks
======================

app
---

The `app` hook provides a way to add scripts that are invoked via HTTP. Scripts
are provided with a WSGI environment for execution. A simple hello world 
example looks like this:: 

  def app(environ, start_response):
     start_response('200 OK', [('Content-type','text/plain')])
     return 'Hello world!'  

The script must define a function named `app` that takes an `environ` which is
a dict instance that contains information about the current request, and the 
executing environment. The `start_response` method starts the response and takes
a status code and a set of response headers. 

The `app` method returns an iterator that generates the response content, or 
just a single string representing the entire body.

For more information about WSGI go to http://wsgi.org.

datastore
---------

TODO 

filter
------

The `filter` hook provides filter function implementations to be used in an OGC
filter. These filters appear in WFS queries, and in SLD styling rules.

A simple filter function looks like this::

  from geoserver.filter import function
  from geoscript.geom import Polygon

  @function
  def areaGreaterThan(feature, area):
    return feature.geom.area > area

The above function returns true or false depending on if the area of a feature
is greater than a certain threshold.

format
------

The `format` hook provides output format implementations for various OWS service
operations. Examples include PNG for WMS GetMap, GeoJSON and GML for WFS
GetFeature, HTML and plain text for WMS GetFeatureInfo.

Currently formats fall into two categories. The first are formats that can 
encode vector data (features). A simple example looks like::

  from geoserver.format import vector_format
  
  @vector_format('property', 'text/plain')
  def write(data, out):
   for feature in data.features:
     out.write("%s=%s\n" % (f.id, '|'.join([str(val) for val in f.values()])))

The above function encodes a set of features as a java property file. Given the
following feature set::

  Feature(id="fid.0", geometry="POINT(0 0)", name="zero")
  Feature(id="fid.1", geometry="POINT(1 1)", name="one")
  Feature(id="fid.2", geometry="POINT(1 1)", name="two")

The above function would output::

  fid.0=POINT(0 0)|one
  fid.1=POINT(1 1)|two
  fid.2=POINT(2 2)|three

Vector formats can be invoked by the following service operations:

* WFS GetFeature (``?outputFormat=property``)
* WMS GetMap (``?format=property``)
* WMS GetFeatureInfo (``?info_format=property``)

A vector format is a python function that is decorated by the ``vector_format``
decorator. The decorator accepts two arguments. The first is the `name` of the 
output format. This is the identifier that clients use to request the format. 
The second parameter is the `mime type` that describes the type of content the 
format creates.

The second type of output format is one that encodes a complete map. This format
can only be used with the WMS GetMap operation. 

TODO: example

process
-------

The `process` hook provides process implementations that are invoked by the 
GeoServer WPS. A simple example looks like::

  from geoserver import process
  from geoscript.geom import Geometry

  @process('Buffer', 'Buffer a geometry', args=[('geom', Geometry)], 
           result=('The buffered result', Geometry))
  def buffer(geom):
     return geom.buffer(10)

A process is a function that is decorated by the ``process`` decorator. The 
decorator takes the following arguments:

.. list-table::
   :widths: 30 60

   * - title
     - The title of the process to displayed to clients
   * - description
     - The description of the process.
   * - version
     - The version of the process
   * - args
     - The arguments the process accepts as a list of tuples
   * - result
     - The result of a process as a tuple

The ``args`` parameter is a list of tuples describing the input arguments of the
process. Each tuple can contain up to three values. The first value is the name
of the parameter and is mandatory. The second value is the type of the parameter
and is optional. The third value is a description of the parameter and is 
optional.

The ``result`` parameter describes the result of the process and is a tuple 
containing up to two values. This parameter is optional. The first value is the type of the result and the second value is a description of the result.

