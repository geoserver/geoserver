.. _scripting_py:

Python
======

.. toctree::
   :hidden:

   api/index

Script Hooks
------------

app
^^^

In Python the app hook is based on `WSGI <http://wsgi.org>`_ which provides a common interface
for Python web application development. This is not a comprehensive introduction to WSGI, that 
can be found `here <http://webpython.codepoint.net/wsgi_tutorial>`__, but the app script must
provide a function named ``app`` that takes a dictionary containing information about the 
environment, and a function to start the response.

.. code-block:: python

   def app(environ, start_response):
     # do stuff here

The function must be present in a file named ``main.py`` in a named *application directory*.
Application directories live under the ``scripts/apps`` directory under the root of the data 
directory::

  GEOSERVER_DATA_DIR/
    ...
    scripts/
      apps/
        app1/
          main.py
          ...
        app2/
          main.py
          ...

The application is web accessible from the path ``/script/apps/{app}`` where ``{app}`` is 
the name of the application. All requests that start with this path are dispatched to the 
``app`` function in ``main.py``.

Hello World Example
~~~~~~~~~~~~~~~~~~~

In this example a simple "Hello World" application is built. First step is to create a 
directory for the app named ``hello``::

  cd $GEOSERVER_DATA_DIR/scripts/apps
  mkdir hello
  
Next step is to create the ``main.py`` file::

  cd hello
  touch main.py
  
Next the ``app`` function is created and stubbed out:

.. code-block:: python
   
   def app(environ, start_response):
     pass

Within the app function the following things will happen:

#. Report an HTTP status code of 200
#. Declare the content type of the response, in this case "text/plain"
#. Generate the response, in this case the string "Hello World"

Steps 1 and 2 are accomplished by invoking the ``start_response`` function:

.. code-block:: python

   start_response('200 OK', [('Content-Type', 'text/plain')]) 
   
Step 3 is achieved by returning an array of string content:

.. code-block:: python

   return ['Hello World']
   
The final completed version:

.. code-block:: python

    def app(environ, start_response):
      start_response('200 OK', [('Content-Type', 'text/plain')])
      return ['Hello World!']

.. note::

   WSGI allows for additional methods of generating responses rather than returning an array. In
   particular it supports returning an generator for the response content. Consult the WSGI 
   documentation for more details.
   
wps
^^^

In Python the wps/process interface is much like the other languages, with a few differences. A 
process is defined with a function named ``run`` that is decorated with the ``geoserver.wps.process``
decorator:

.. code-block:: python

   from geoserver.wps import process
   
   @process(...)
   def run(...):
      # do something

The function is located in a file under the ``scripts/wps`` directory under the root of the data
directory. A WPS process requires metadata to describe itself to the outside world including:

* A **name** identifying the process
* A short **title** describing the process
* An optionally longer **description** that describes the process in more detail
* A dictionary of **inputs** describing the parameters the process accepts as input
* A dictionary of **outputs** describing the results the process generates as output
  
In python the ``name`` is implicitly derived from the name of the file that contains the process
function. The rest of the metadata is passed in as arguments to the ``process`` decorator. The
``title`` and ``description`` are simple strings:

.. code-block:: python

   @process(title='Short Process Title', 
            description='Longer and more detailed process description')
   def run():
      pass
  
The ``inputs`` metadata is a dictionary keyed with strings matching the names of the process inputs. 
The values of the dictionary are tuples in which the first element is the type of the input and the 
second value is the description of the input. The keys of the dictionary must match those declared in
the process function itself:

.. code-block:: python
  
   @process(
     ...
     inputs={'arg1': (<arg1 type>, 'Arg1 description'), 
             'arg2': (<arg2 type>, 'Arg2 description')}
   )
   def run(arg1, arg2):
     pass

Optionally, the input tuples can also host a third argument, a dictionary hosting more input metadata.
Currently the following metadata are supported:

* min: mininum number of occurrences for the input, 0 if the input is optional
* max: maximum number of occurrences for the input, if greater than one the process will receive a list
* domain: the list of values the input can receive, which will be advertised in the WPS DescribeProcess output

For example:

.. code-block:: python


    @process(
      inputs={'geom': (Geometry, 'The geometry to buffer'), 
              'distance':(float, 'The buffer distance'),
              'capStyle': (str, 'The style of buffer endings', 
                          {'min': 0, 'domain' :  ('round', 'flat', 'square')}),
              'quadrantSegments': (int, 'Number of segments' , {'min': 0})}
              
Finally, the default values assigned to the ``run`` function parameter will show up in the capabilities
document as the parameter default value:

.. code-block:: python
   
   @process(...)
   def run(a, b, c='MyDefaultValue')              

The ``outputs`` metadata is the same structure as the inputs dictionary except that for it describes
the output arguments of the process:

.. code-block:: python

   @process(
     ...
     outputs={'result1': (<result1 type>, 'Result1 description'), 
              'result2': (<result2 type>, 'Result2 description')}
   )
   def run(arg1, arg2):
     pass
 
A process must generate and return results matching the ``outputs`` arguments. For processes that 
return a single value this is implicitly determined but processes that return multiple values must
be explicit by returning a dictionary of the return values:

.. code-block:: python

    @process(
      ...
      outputs={'result1': (<result1 type>, 'Result1 description'), 
               'result2': (<result2 type>, 'Result2 description')}
    )
    def run(arg1, arg2):
      # do something
      return {
        'result1': ...,
        'result2': ...
      }

Buffer Example
~~~~~~~~~~~~~~

In this example a simple buffer process is created. First step is to create a file named 
``buffer.py`` in the ``scripts/wps`` directory::

  cd $GEOSERVER_DATA_DIR/scripts/wps
  touch buffer.py
  
Next the ``run`` function is created and stubbed out. The function will take two arguments:

#. A geometry object to buffer
#. A floating point value to use as the buffer value/distance

.. code-block:: python
   
   def run(geom, distance):
     pass

In order for the function to picked up it must first be decorated with the ``process`` 
decorator:

.. code-block:: python

   from geoserver.wps import process
   
   @process(title='Buffer', description='Buffers a geometry')   
   def run(geom, distance):
     pass

Next the process inputs and outputs must be described:

.. code-block:: python

   from geoscript.geom import Geometry

   @process(
      ...,
      inputs={ 'geom': (Geometry, 'The geometry to buffer'), 
               'distance': (float,'The buffer distance')}, 
      outputs={'result': (Geometry, 'The buffered geometry')}
   )
   def run(geom, distance):
     pass

And finally writing the buffer routine which simply just invokes the ``buffer`` 
method of the geometry argument:

.. code-block:: python

   @process(...)
   def run(geom, distance):
     return geom.buffer(distance)
     

In this case since the process returns only a single argument it can be returned
directly without wrapping it in a dictionary.

The final completed version:

.. code-block:: python

   from geoserver.wps import process
   from geoscript.geom import Geometry

   @process(
      title='Buffer', 
      description='Buffers a geometry', 
      inputs={'geom': (Geometry, 'The geometry to buffer'), 
              'distance':(float,'The buffer distance')}, 
      outputs={'result': (Geometry, 'The buffered geometry')}
   )
   def run(geom, distance):
     return geom.buffer(distance);

GeoScript-PY
------------

As mentioned :ref:`previously <scripting_supported_geoscript>` GeoScript provides 
scripting apis for GeoTools in various languages. Naturally the GeoServer Python 
extension comes with GeoScript Python enabled. In the buffer example above an 
example of importing a GeoScript class was shown.

The GeoScript Python api is documented `here <http://geoscript.org/py/api/index.html#api>`_.

API Reference
-------------

In much the same way as GeoScript provides a convenient scripting layer on top of 
GeoTools the Python scripting extension provides a ``geoserver`` Python module that 
provides convenient access to some of the GeoServer internals. 

The GeoServer Python api is documented :ref:`here <scripting_python_api>`.

