.. _rest_services_overview:

Overview
========

GeoServer uses a library known as `Restlet <http://www.restlet.org/>`_ for all
REST related functionality. Restlet is a lightweight rest framework written
in Java that integrates nicely with existing servlet based applications. 

REST dispatching
----------------

In GeoServer, all requests under the path ``/rest`` are considered a call to
a restful service. Every call of this nature is handled by a *rest 
dispatcher*. The job of the dispatcher is to route the request to the 
appropriate end point. This end point is known as a *restlet*. 

.. image:: rest-dispatch.png

Restlets are loaded from the spring context, and therefore are pluggable.

Restlets
--------

A *restlet* is the generic entity which handles calls routed by the
dispatcher, and corresponds to the class ``org.restlet.Restlet``. One 
can extend this class directly to implement a service endpoint. Alternatively
one can extend a subclass for a specialized purpose. Namely a *finder*, which 
is described in the next section.

Finders and resources
---------------------

Restful services are often implemented around the concept of *resources*. A
*finder* is a special kind of restlet whose job is to find the correct 
resource for a particular request. The resource then serves as the final
end point and handles the request. The appropriate classes from the restlet
library are ``org.restlet.Finder`` and ``org.restlet.resource.Resource``.

Representations
---------------

A *representation*, commonly referred to as a format, is the state of a 
particular state or encoding of a resource. For instance, when a request for 
a particular resource comes in, a representation of that resource is returned
to the client.	

