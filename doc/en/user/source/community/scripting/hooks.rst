.. _scripting_hooks:

Scripting Hooks
===============

This page describes all available scripting hooks. Every hook listed on this page is 
implemented by all the supported language extensions. However, depending on the 
language, the interfaces and api used to write a script may differ. Continue reading
for more details.

Applications
------------

The "app" hook provides a way to contribute scripts that are intended to be run over http. 
An app corresponds to a named directory under the ``scripts/apps`` directory. For example::

  GEOSERVER_DATA_DIR/
    ...
    scripts/
      apps/
        hello/

An app directory must contain a *main* file that contains the "entry point" into the 
application. Every time the app is invoked via an http request this main file is 
executed.

The contains of the main file differ depending on the language. The default for all 
languages is simply that the main file contain a function named "run" that takes two
arguments, the http request and response. For example, in beanshell:

.. code-block:: java
 
  import org.restlet.data.*;

  run(request,response) {
    response.setEntity("Hello World!", MediaType.TEXT_PLAIN);
  }

As explained above this api can differ depending on the language. For example in 
Python we have the well defined `WSGI <http://wsgi.org>`_ specification that gives
us a standard interface for Python web development. The equivalent Python script 
to that above is:

.. code-block:: python

  def app(environ, start_response):
    start_response('200 OK', [('Content-Type', 'text/plain')])
    return ['Hello World!']

For the JavaScript app hook, scripts are expected to export an ``app`` function that
conforms to the `JSGI <http://wiki.commonjs.org/wiki/JSGI>`_ specification (v0.3).
The equivalent 'Hello World' app in JavaScript would look like the following
(in ``/scripts/apps/hello/main.js``):

.. code-block:: javascript

  exports.app = function(request) {
    return {
      status: 200,
      headers: {"Content-Type": "text/plain"},
      body: ["Hello World"]
    }
  }; 

Applications are http accessible at the path ``/script/apps/{app}`` where ``{app}`` 
is the name of the application. For example assuming a local GeoServer the url for
for the application would be::

  http://localhost:8080/geoserver/script/apps/hello

.. warning::

   Because of security risks the path will not be accessible if the default admin password has not been changed.


Web Processing Service
----------------------

The wps hook provides a way to provides a way to contribute scripts runnable as a 
WPS process. The process is invoked using the standard WPS protocol the same way 
an existing well-known process would be.

All processes are located under the ``scripts/wps`` directory. Each process is 
located in a file named for the process. For example::

  GEOSERVER_DATA_DIR/
    ...
    scripts/
      wps/
        buffer.bsh
        
The process will be exposed using the extension as the namespace prefix, and the file name as 
the process name, for example, the above process will show up as ``bsh:buffer``. 
It is also possible to put scripts in subdirectories of ``script/wps``, in this case the directory name
will be used as the process namespace, for example::

  GEOSERVER_DATA_DIR/
    ...
    scripts/
      wps/
        foo/
          buffer.bsh

will expose the process as ``foo:buffer``. 

A process script must define two things:

#. The process metadata: title, description, inputs, and outputs
#. The process routine itself

The default for languages is to define the metadata as global variables in the 
script and the process routine as a function named "run". For example, in 
groovy:

.. code-block:: groovy
 
  import org.locationtech.jts.geom.Geometry

  title = 'Buffer'
  description = 'Buffers a geometry'

  inputs = [
    geom: [name: 'geom', title: 'The geometry to buffer', type: Geometry.class], 
    distance: [name: 'distance', title: 'The buffer distance', type: Double.class]
  ]

  outputs = [
    result: [name: 'result', title: 'The buffered geometry',  type: Geometry.class]
  ]

  def run(input) {
    return [result: input.geom.buffer(input.distance)]
  }
    
In Python the api is slightly different and makes use of Python decorators:

.. code-block:: python

  from geoserver.wps import process
  from org.locationtech.jts.geom import Geometry

  @process(
    title='Buffer', 
    description='Buffers a geometry',
    inputs={ 
      'geom': (Geometry, 'The geometry to buffer'), 
      'distance':(float,'The buffer distance')
    }, 
    outputs={
      'result': (Geometry, 'The buffered geometry')
    } 
  )
  def run(geom, distance):
    return geom.buffer(distance);

In JavaScript, a script exports a ``process`` object (see the 
`GeoScript JS API docs <http://geoscript.org/js/api/process.html>`_ for more detail)
in order to be exposed as a WPS process.  The following is an example of a simple
buffer process (saved in ``scripts/wps/buffer.js``):

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


Once implemented a process is invoked using the standard WPS protocol. For example
assuming a local GeoServer the url to execute the process would be::

  http://localhost:8080/geoserver/wps
    ?service=WPS
    &version=1.0.0
    &request=Execute
    &identifier=XX:buffer
    &datainputs=geom=POINT(0 0)@mimetype=application/wkt;distance=10

(Substitue ``XX:buffer`` for the script name followed by the extension.  E.g. 
``py:buffer`` for Python or ``js:buffer`` for JavaScript.)


Filter Functions
----------------

The Filter Functions hook provides a way to create new Filter Function. These functions 
may be used, for example, in WFS/WMS filtering or in SLD expressions, for more information about 
Filter Functions see :ref:`filter_function`. GeoServer already provides many 
built in functions, for a complete list see :ref:`filter_function_reference`.

All created functions are located under the ``scripts/function`` directory. For creating
new functions use :ref:`scripting_ui` or place directly function file in ``scripts/function`` 
directory, for example, to create a function named ``camelcase`` using the python language create file
``scripts/function/camelcase.py``.

The contains of the function file differ depending on the language. The default for all 
languages is simply that the function file contains a function named "run". For example, in python:

.. code-block:: python

  def run(value, args):
    return ''.join(x for x in args[0].title() if not x.isspace())

The filter function name equals the function file name, for example, if there is ``scripts/function/camelcase.py``
file then it can be used in SLD like this:

.. code-block:: xml

     ...
     <TextSymbolizer>
            <Label>
               <ogc:Function name="camelcase">
                 <ogc:PropertyName>STATE_NAME</ogc:PropertyName>
               </ogc:Function>
            </Label>
       ...
     </TextSymbolizer>
     ...


WFS Transactions
----------------

WFS Transactions hook provides a way one can intercept WFS Transactions. It could be used, for example, 
to add validation or fill some attributes based on other ones.

All created WFS Transactions hooks are located under the ``scripts/wfs/tx`` directory. For creating
new functions use :ref:`scripting_ui` or place file directly in ``scripts/wfs/tx`` directory. The file name 
does not matter in WFS Transaction hook.

To intercept transaction one should declare a method with name specific to transaction phase, for example,
to manipulate data before update use ``preUpdate``. Available methods in python are:

.. code-block:: python

   from geoserver.wfs import tx 

   def before(req, context):
     context['before'] = True
   
   def preInsert(inserted, req, context):
     context['preInsert'] = True
   
   def postInsert(inserted, req, context):
     context['postInsert'] = True
   
   def preUpdate(updated, props, req, context):
     context['preUpdate'] = True
   
   def postUpdate(updated, props, req, context):
     context['postUpdate'] = True
   
   def preDelete(deleted, req, context):
     context['preDelete'] = True
   
   def postDelete(deleted, req, context):
     context['postDelete'] = True
   
   def preCommit(req, context):
     context['preCommit'] = True
    
   def postCommit(req, res, context):
     context['postCommit'] = True
   
   def abort(req, res, context):
     context['abort'] = True

For example, to disallow feature deleting in python, create script:

.. code-block:: python

   from org.geoserver.wfs import WFSException

   def preDelete(deleted, req, context):
     raise WFSException("It is not allowed to delete Features in this layer!")




            