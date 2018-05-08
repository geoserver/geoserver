.. _ows_services_overview:

OWS Services Overview
=====================

The Open Geospatial Consortium (OGC) defines of series of web protocols that all follow a similar design. The OGC Open Web Services (OWS) define a service using:

* Service
* Version
* Request - a service 

GeoServer provides a framework for accepting these requests and dispatching them to the appropriate implementation. The services are configured for the Dispatcher using a Spring applicationContext.xml file included in your jar.
