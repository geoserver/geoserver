.. _scripting_supported:

Supported Languages
===================

Support for the following scripting languages is available:

* Python
* JavaScript
* Groovy
* Beanshell
* Ruby


Adding support for additional languages is relatively straight forward. The requirements 
for adding a new language are:

#. The language has an implementation that runs on the Java virtual machine
#. The language runtime provides a 
   `JSR-223 <http://java.sun.com/developer/technicalArticles/J2SE/Desktop/scripting/>`_  
   compliant script engine
   
.. _scripting_supported_geoscript:

GeoScript
---------

`GeoScript <http://geoscript.org>`_ is a project that adds scripting capabilities to the 
GeoTools library. It can be viewed as bindings for GeoTools in various other languages that
are supposed on the JVM. It is the equivalent of the various language bindings that GDAL 
and OGR provide.

Currently GeoScript is available for the following languages:

* `Python <http://geoscript.org/py>`_
* `JavaScript <http://geoscript.org/js>`_
* `Groovy <http://geoscript.org/groovy>`_

The associated GeoServer scripting extension for these languages come with GeoScript for 
that language enabled. This means that when writing scripts one has access to the GeoScript
modules and packages like they would any other standard library package.

Those languages that don't have a GeoScript implementation can still implement the same 
functionality that GeoScript provides but must do it against the GeoTools api directly. The
downside being that usually the GeoTools api is much more verbose than the GeoScript 
equivalent. But the upside is that going straight against the GeoTools api is usually more
efficient.

Therefore GeoScript can be viewed purely as a convenience for script writers.