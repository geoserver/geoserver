OGC(r) WFS schema - ReadMe.txt
==============================

OGC(r) Web Feature Service (WFS) Implementation Standard
-------------------------------------------------------------------

The OpenGIS Web Feature Service Interface Standard (WFS) defines an
interface for specifying requests for retrieving geographic features
across the Web using platform-independent calls. The WFS standard
defines interfaces and operations for data access and manipulation
on a set of geographic features.

More information may be found at
 http://www.opengeospatial.org/standards/wfs

The most current schema are available at http://schemas.opengis.net/ .

-----------------------------------------------------------------------

2016-07-05  Clemens Portele, Panagiotis (Peter) A. Vretanos
  * v2.0: Update Query element in LockFeature to be optional
    per 09-025r2 corrigendum.  No version change.

2015-10-13  Panagiotis (Peter) A. Vretanos
  * v2.0: Update ReturnFeatureType to be optional.   No version change 
    per 09-025r2 corrigendum. 

2014-10-02  Panagiotis (Peter) A. Vretanos
  * v2.0: Update examples.  No version change.

2014-09-23  Panagiotis (Peter) A. Vretanos
  * v2.0: Added WFS 2.0.2 as wfs/2.0 from OGC 09-025r2

2012-07-21  Kevin Stegemoller
  * v1.0 - v2.0: WARNING XLink change is NOT BACKWARD COMPATIBLE.
  * Changed OGC XLink (xlink:simpleLink) to W3C XLink (xlink:simpleAttrs)
    per an approved TC and PC motion during the Dec. 2011 Brussels meeting.
    See http://www.opengeospatial.org/blog/1597 
  * v2.0: Updated xsd:schema/@version to 2.0.1 (06-135r7 s#13.4)
  * v1.1: Updated xsd:schema/@version to 1.1.2.0 (06-135r7 s#13.4)
  * v1.0: Updated xsd:schema/@version to 1.0.0.3 (06-135r7 s#13.4)
  * v1.0.0: Add wfs.xsd per 11-025, all leaf documents of a namespace shall
    retroactively and explicitly require/add an <include/> of the all-components schema.
  * v1.0 - v2.0: Updated copyright and ReadMe.txt

2010-11-02  Panagiotis (Peter) A. Vretanos
  * v2.0: Published 2.0.0 from OGC 09-025r1 also branded as ISO 19142:2010

2010-02-03  Kevin Stegemoller
  * v1.1.0: Updated xsd:schema/@version attribute to 1.1.2 (06-135r7 s#13.4)
  * v1.0.0: Updated xsd:schema/@version to 1.0.0 2010-02-02 (06-135r7 s#13.4)
  * v1.1.0, 1.0.0:
    + Updated xsd:schema/@version attribute (06-135r7 s#13.4)
    + Update relative schema imports to absolute URLs (06-135r7 s#15)
    + Update/verify copyright (06-135r7 s#3.2)
    + Add archives (.zip) files of previous versions
    + Create/update ReadMe.txt (06-135r7 s#17)

2009-05-08  Clemens Portele
  * v1.1.0: The cardinality of the InsertResults element is 1 which means that
    the element must always be present in a transaction response ...  even if
    that transaction contains no insert actions.  The cardinality should be
    zero.  Every instance that validates against the buggy schema document will
    also validate against the fixed schema document. See wfs-1_1_0-1.zip.

2005-11-22  Arliss Whiteside
  * v1.1.0, v1.0.0: The sets of XML Schema Documents for WFS versions have been
    edited to reflect the corrigenda to documents OGC 02-058 (WFS 1.0.0) and
    OGC 04-09 (WFS 1.1.0) that are based on the change requests: 
     OGC 05-068r1 "Store xlinks.xsd file at a fixed location"
     OGC 05-081r2 "Change to use relative paths"

 Note: check each OGC numbered document for detailed changes.

-- [ VERSION NOTES ] --------------------------------------------------

  OGC is incrementally changing how schemas will be hosted. A new
  revision of the Specification Best Practice policy document (06-135r11)
  clarifies this practices.

  OGC is moving to host the schemas using a 2 digit version number so
  that dependent documents (schemas) will not have to change each time a
  schema is corrected (by a corrigendum). The schemas actual version
  number will be kept in the version attribute on the schema element
  which will be used to signify that there has been a change to the
  schema. Each previous revision will be available online in a ZIP
  archive.
  
  The LATEST version is the M.N directory where 
   * M is the major version
   * N is the minor version
  The latest bugfix version now is always in the M.N directory and 
  documented in the version attribute on the schema element. The older
  versions are now archived in the -M_N_X.zip files.
  
  Previously the OGC used M.N.C where
   * M is the major version
   * N is the minor version
   * C is the corrigendum version
  These older M.N.C versions will be updated using M.N.C.X where 
  X may be a bugfix version. These schema will also be .zip archived.

-- 2010-01-21  Kevin Stegemoller  updated 2012-07-21

-----------------------------------------------------------------------

Policies, Procedures, Terms, and Conditions of OGC(r) are available
  http://www.opengeospatial.org/ogc/legal/ .

Copyright (c) 2012 Open Geospatial Consortium.

-----------------------------------------------------------------------

