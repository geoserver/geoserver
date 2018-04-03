OpenGIS(r) GML schema version 3.2.1 / ISO 19136 - ReadMe.txt

The schema has been validated with Xerces-J, Xerces C++ and XSV.

-------------------------------------------------------------------

2012-07-21  Kevin Stegemoller

  * v2.0.0 - v3.2.1 WARNING XLink change is NOT BACKWARD COMPATIBLE.
  * changed OGC XLink (xlink:simpleLink) to W3C XLink (xlink:simpleAttrs)
  per an approved TC and PC motion during the Dec. 2011 Brussels meeting.
  see http://www.opengeospatial.org/blog/1597
  * implement 11-025: retroactively require/add all leaf documents of an
  XML namespace shall explicitly <include/> the all-components schema
  * v3.2.1: updated xsd:schema:@version to 3.2.1.2 (06-135r7 s#13.4)

2007-09-06  Kevin Stegemoller

  GML 3.2.1 (ISO 19136)
  * Published GML 3.2.1 schemas from OGC 07-036
  * validated with oXygen 8.2 (xerces-J 2.9.0) - Kevin Stegemoller
  * validated with Xerces-J, Xerces-C++ and XSV - Clemens Portele

2007-08-17  Kevin Stegemoller

  Changes made to these GML 3.2.1 / ISO 19136 schemas:
  * added ReadMe.txt
  * changed gmd.xsd references to "../../iso/19139/20070417/gmd/gmd.xsd"
  * changed xlink references to be relative to /xlink/1.0.0/xlinks.xsd
    available from schemas.opengis.net/xlink/1.0.0/xlinks.xsd (REMOVED 2012-07-21).
  * removed xlinks schema and directory

  Changes made to these ISO 19139 schemas by OGC:
  * added ReadMe.txt
  * changed ISO_19136 path to /gml/3.2.1/
  * changed xlink references to be relative to /xlink/1.0.0/xlinks.xsd
    available from schemas.opengis.net/xlink/1.0.0/xlinks.xsd (REMOVED 2012-07-21).
  * removed xlinks schema and directory

OGC GML 3.2.1 / ISO 19136 schemas files will be published at:
- http://schemas.opengis.net/gml/3.2.1/
- http://standards.iso.org/ittf/PubliclyAvailableStandards/ISO_19136_Schemas/

Files in the folder "ISO/19139/20070417" are also published at
- http://standards.iso.org/ittf/PubliclyAvailableStandards/ISO_19139_Schemas/

-------------------------------------------------------------------

The Open Geospatial Consortium, Inc. official schema repository is at
  http://schemas.opengis.net/ .
Policies, Procedures, Terms, and Conditions of OGC(r) are available
  http://www.opengeospatial.org/ogc/policies/ .
Additional rights of use are described at
  http://www.opengeospatial.org/legal/ . 

Copyright (c) 2007 Open Geospatial Consortium.

-------------------------------------------------------------------
