OGC(r) WPS 1.0.0 - ReadMe.txt

Web Processing Service (WPS) 1.0.0

The WPS schema are described in the OGC WPS 1.0.0 document 05-007r7.

There is a obsolete reference in the OWS Common 1.1.0
ExceptionReport.xsd schema which causes the WPS 1.0.0
examples/90_wpsExceptionReport.xml not to validate correctly in some
validators.  Below is the summary of the issue detailed in the OGC
Change Request 07-141 .


Reason for Change
-----------------

The current OWS Common 1.1.0 ExceptionReport.xsd schema references an
obsolete version of the XML schema, and therefore does not validate
properly if an XML validator actually attempts to import the XML
schema at this obsolete location.  As a consequence other OGC schemas
cannot import the exception report schema.  

Summary of Change
-----------------

In ows/1.1.0/owsExceptionReport.xsd 
replace
	<import namespace="http://www.w3.org/XML/1998/namespace"/>
with
	<import namespace=" http://www.w3.org/XML/1998/namespace"
		schemaLocation="http://www.w3.org/2001/xml.xsd"/>

 -- from Change Request OGC 07-141 by Peter Schut, WPS RWG


-----------------------------------------------------------------------

Policies, Procedures, Terms, and Conditions of OGC(r) are available
  http://www.opengeospatial.org/ogc/policies/ .

Copyright (c) 2007 Open Geospatial Consortium, Inc. All Rights Reserved.
