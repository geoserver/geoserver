The WPS Hook
============

In GeoScript JS, the ``geoscript/process`` module provides a ``Process`` 
constructor.  A process object wraps a function with a title, description, and
additional metadata about the inputs and outputs.  With the GeoServer scripting 
extension, when a script exports a process, it is exposed in GeoServer via the 
WPS interface.

To better understand how to construct a well described process, we'll examine
the parts of the previously provided ``buffer.js`` script:

.. code-block:: javascript

  var Process = require("geoscript/process").Process;

  exports.process = new Process({
    title: "JavaScript Buffer Process",
    description: "Process that buffers a geometry.",
    inputs: {
      geom: {
        type: "Geometry",
        title: "Input Geometry",
        description: "The target geometry."
      },
      distance: {
        type: "Double",
        title: "Buffer Distance",
        description: "The distance by which to buffer the geometry."
      }
    },
    outputs: {
      result: {
        type: "Geometry",
        title: "Result",
        description: "The buffered geometry."
      }
    },
    run: function(inputs) {
      return {result: inputs.geom.buffer(inputs.distance)};
    }
  });

When this script is saved in the ``$GEOSERVER_DATA_DIR/scripts/wps`` directory,
it will be available to WPS clients with the identifier ``js:buffer``.  In 
general, the process identifier is the name of the script prefixed by the 
language extension. 

First, the ``require`` function is used to pull in the ``Process`` constructor
from the ``geoscript/process`` module:

.. code-block:: javascript

  var Process = require("geoscript/process").Process;

Next, a process is constructed and assigned to the ``process`` property of the
``exports`` object.  This makes it available to other JavaScript modules that 
may want to import this process with the ``require`` function in addition to 
exposing the process to GeoServer's WPS.  The title and description provide WPS
clients with human readable information about what the process does.

.. code-block:: javascript

  exports.process = new Process({
    title: "JavaScript Buffer Process",
    description: "Process that buffers a geometry.",

All the work of a process is handled by the ``run`` method.  Before clients can
execute a process, they need to know some detail about what to provide as input
and what to expect as output.  In general, processes accept multiple inputs and
may return multiple outputs.  These are described by the process' ``inputs`` 
and ``outputs`` properties.

.. code-block:: javascript

    inputs: {
      geom: {
        type: "Geometry",
        title: "Input Geometry",
        description: "The target geometry."
      },
      distance: {
        type: "Double",
        title: "Buffer Distance",
        description: "The distance by which to buffer the geometry."
      }
    },

The buffer process expects two inputs, named ``geom`` and ``distance``.  As with
the process itself, each of these inputs has a human readable title and 
description that will be provided to WPS clients.  The ``type`` property is a
shorthand string identifying the data type of the input.  See the 
`Process API docs <http://geoscript.org/js/api/process.html>`_ for more detail
on supported input and output types.

.. code-block:: javascript

    outputs: {
      result: {
        type: "Geometry",
        title: "Result",
        description: "The buffered geometry."
      }
    },

The buffer process provides a single output identified as ``result``.  As with
each of the inputs, this output is described with ``type``, ``title``, and
``description`` properties.

To see what this process metadata looks like to a WPS client, call the 
WPS `DescribeProcess <http://localhost:8080/geoserver/wps?service=WPS&version=1.0.0&request=DescribeProcess&identifier=js:buffer>`_ 
method::

  http://localhost:8080/geoserver/wps
    ?service=WPS
    &version=1.0.0
    &request=DescribeProcess
    &identifier=js:buffer

Finally, the ``run`` method is provided.

.. code-block:: javascript

    run: function(inputs) {
      return {result: inputs.geom.buffer(inputs.distance)};
    }
  });

The ``run`` method takes a single ``inputs`` argument.  This object will have
named properties corresponding the the client provided inputs.  In this case,
the ``geom`` property is a ``Geometry`` object from the ``geoscript/geom`` 
module.  This geometry has a ``buffer`` method that is called with the 
provided distance.  See the `Geometry API docs <http://geoscript.org/js/api/geom/geometry.html>`__
for more detail on available geometry properties and methods.

The ``run`` method returns an object with properties corresponding to the 
above described outputs - in this case, just a single ``result`` property.

To see the results of this processs in action, call the WPS 
`Execute <http://localhost:8080/geoserver/wps?service=WPS&version=1.0.0&request=Execute&identifier=js:buffer&datainputs=geom=POINT(0%200)@mimetype=application/wkt;distance=10>`_
method::

  http://localhost:8080/geoserver/wps
    ?service=WPS
    &version=1.0.0
    &request=Execute
    &identifier=js:buffer
    &datainputs=geom=POINT(0 0)@mimetype=application/wkt;distance=10

