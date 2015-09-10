Hello World! What Makes scriptlet Scripts Tick
==============================================
In :doc:`install` we presented a very small script that tested a scriptlet
installation by just printing a simple message.  Now let's go back and discuss
how that script works.  For reference, here is a copy of the script::

    var StringRepresentation = Packages.org.restlet.resource.StringRepresentation;
    var MediaType = Packages.org.restlet.data.MediaType;

    response.setEntity(new StringRepresentation(
        "Hello world!", MediaType.TEXT_PLAIN
    ));

The scriptlet Environment
-------------------------
For scriptlet REST endpoints, there is a very simple environment set up on top
of the basic Rhino standard libraries.  This consists of four global
variables:

loader
    A reference to the GeoServer resource loader, which helps to look up files
    relative to the GeoServer data directory.  See the JavaDocs for information
    on the methods available through the catalog.

catalog
    A reference to the GeoServer catalog.  See the JavaDocs for information on
    the methods available through the catalog.

request
    The `Restlet <http://restlet.org/>`_ Request object currently being handled.

response
    The Restlet Response object for the current request.  This is modifiable
    (and in fact is the only way for the script to output to HTTP.)

A Simple Script
---------------
Knowing this, let's dissect the "hello world" script::

    var StringRepresentation = Packages.org.restlet.resource.StringRepresentation;
    var MediaType = Packages.org.restlet.data.MediaType;

These two lines provide aliases for Java packages to be used in the script.  If
you are familiar with Java code, these lines are largely analogous to Java's
import statement.  Any Java library that is on the classpath is available
through the rhino Packages object.

    .. code-block:: javascript

        response.setEntity(new StringRepresentation(
            "Hello world!", MediaType.TEXT_PLAIN
        ));

This statement sets a new Representation (or content body) for the Response
object.  In this case, we use the StringRepresentation which simply wraps a
string and associates a MediaType with it.

.. note:: This, and all the other scripts discussed in this documentation, are
    stored in the GeoServer code repository at
    https://github.com/geoserver/geoserver/tree/master/src/community/scriptlet/src/examples
