OGC(r) OWS Common - ReadMe.txt
==============================

OpenGIS(r) Web Service Common (OWS) Implementation Specification

More information on the OGC OWS Common standard may be found at
 http://www.opengeospatial.org/standards/common

The most current schema are available at http://schemas.opengis.net/ .

-----------------------------------------------------------------------

2012-07-21  Kevin Stegemoller
  * v1.0 - v2.0 WARNING XLink change is NOT BACKWARD COMPATIBLE.
  * changed OGC XLink (xlink:simpleLink) to W3C XLink (xlink:simpleAttrs)
    per an approved TC and PC motion during the Dec. 2011 Brussels meeting.
    see http://www.opengeospatial.org/blog/1597
  * v1.0 - v2.0: Per 11-025, all leaf documents of a namespace shall retroactively
    and explicitly require/add an <include/> of the all-components schema.
  * v1.0 - v2.0: removed date from xs:schema/xs:annotation/xs:appinfo
  * v2.0: updated xsd:schema/@version to 2.0.2 (06-135r7 s#13.4)
  * v1.1.0: updated xsd:schema/@version to 1.1.0.3 (06-135r7 s#13.4)
  * v1.0.0: updated xsd:schema/@version to 1.0.0.2 (06-135r7 s#13.4)
  * v0.3.x - 0.4.0: schema were removed and archived

2011-10-21  Jim Greenwood
  * v2.0.1: Incorporate OWS 2.0 Corrigendum 1 (OGC 11-157) to allow 
    multi-language use.

2011-02-07  Peter Schut
  * v1.1.0: The 1.1.0 version of owsExceptionReport.xsd has been corrected 
    to reflect the corrigenda (OGC 07-141).  The owsExceptionReport.xsd 
    schema previously referenced an obsolete version of the XML schema.

2010-05-06  Jim Greenwood
  * v2.0.0: The 2.0.0 version of the XML Schema Documents for OGC
    document 06-121r9, posted as ows/2.0, as approved as an 
    Implementation Specification in May 2005.

2010-01-21  Kevin Stegemoller 
  * update/verify copyright (06-135r7 s#3.2)
  * migrate relative to absolute URLs of schema imports (06-135r7 s#15)
  * updated xsd:schema/@version attribute (06-135r7 s#13.4)
  * add archives (.zip) files of previous versions
  * create/update ReadMe.txt (06-135r7 s#17)

2007-04-03  Arliss Whiteside
  * v1.1.0: OWS Common specification has been updated to version 1.1.0
    (OGC 06-121r3). These very small changes are taken from corrigendum
    (OGC 07-016) which corrects the schemaLocation references in
    <import> declarations for the namespace
    http://www.w3.org/1999/xlink, in the OWS Common 1.1 XML Schema.
    These schemaLocation references are changed to relatively reference
    the old schema location at
    http://www.opengis.net/xlink/1.0.0/xlinks.xsd .  
  
2005-11-22  Arliss Whiteside
  * v1.0.0, v0.4.0, v0.3.2, v0.3.1, v0.3.0: All five of these sets of
    XML Schema Documents have been edited to reflect the corrigenda to
    all those OGC documents which are based on the change requests: 
    OGC 05-059r2 "Make element and type names public"
    OGC 05-067r1 "Change ows namespace identifier URL to http://www.opengis.net/ows"
    OGC 05-068r1 "Store xlinks.xsd file at a fixed location"
    OGC 05-080r1 "Store OGC XML schema documents in one place"
    OGC 05-081r2 "Change to use relative paths"
    OGC 05-105 "Remove description and copyright tags from XML schema documents"

  * v1.0.0: The 1.0.0 version are the XML Schema Documents for OGC
    document 05-008, approved as an Implementation Specification in May
    2005.

  * v0.4.0: The 0.4.0 version are the XML Schema Documents for OGC
    document 04-016r5.

  * v0.3.2: The 0.3.2 version are the XML Schema Documents after
    correcting one small incorrect difference from OGC document
    04-016r3.

  * v0.3.1: The 0.3.1 version are the XML Schema Documents attached to
    OGC document 04-016r3, containing that editing of document 04-016r2.
    This Recommendation Paper is available to the public at
    http://portal.opengis.org/files/?artifact_id=6324.

  * v0.3.0: OWS Common set of XML Schema Documents from OGC document
    04-016r2 approved as Recommendation Paper in the April 2004 OGC 
    meetings.

  * Note: check each OGC numbered document for detailed changes.

-----------------------------------------------------------------------

Policies, Procedures, Terms, and Conditions of OGC(r) are available
  http://www.opengeospatial.org/ogc/legal/ .

Copyright (c) 2012 Open Geospatial Consortium.

-----------------------------------------------------------------------

