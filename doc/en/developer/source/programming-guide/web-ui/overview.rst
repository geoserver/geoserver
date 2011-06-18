.. _wicket_ui_overview:

Overview
========

GeoServer uses a web application framework known as 
`Wicket <http://wicket.apache.org//>`_ for its user interface. Wicket differs 
from most Java web frameworks in that it is component based rather than JSP 
template based. This makes Wicket a more natural web framework for many Java
programmers who are more familiar with Swing programming than web programming. 

Plug-ins
--------

Because of its component based nature Wicket components can be loaded from the
classpath. Which means that web applications can be built in a modular fashion, 
rather than in a monolithic fashion.

GeoServer takes this concept one step further to provide a pluggable user 
interface, in which Wicket components can be plugged in via Spring and the 
regular GeoServer plug-in mechanism.

Each component that is plugged in is described by a **component descriptor**.
A component descriptor is an instance of the ``org.geoserver.web.ComponentInfo``
class:

  .. literalinclude:: ComponentInfo.java
     :lines: 24-42,105

A ``ComponentInfo`` instance contains meta information about the component 
being plugged in such as its title and description, as well as the class which
implements the component.

Each subclass of ``ComponentInfo`` represents a specific 
:ref:`extension point <extension_points>`.  For instance the class
``org.geoserver.web.MenuPageInfo`` represents the extension point for "main" 
pages, ie pages that are linked to from the main menu of the application.
