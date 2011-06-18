OpenGIS GML 2.1.2.1 ReadMe.txt

NOTICE: GML 2.1.2 will link to the newest version of GML 2.1.2.x .  As
of 5 September 2007, the contents of 2.1.2 contain GML 2.1.2.1 per OGC
06-189.  If you have questions, please contact the webmaster (at)
opengeospatial.org.

2007-08-27  Chris Holmes

  * v2.1.2: update 2.1.2.1 and ReadMe.txt changes
  * v2.1.2.0: Contains previous version of GML 2.1.2 (pre- 5 Sep 2007)
  * v2.1.2.1: Contains Corrigendum 1 for GML 2.1.2 schema fix (OGC 06-189).
  * v2.1.2.1: Corrigendum 1 for GML 2.1.2 schema fix (OGC 06-189) includes:
    + Official schema location is now http://schemas.opengis.net
    + replace xlink import schema location with ../../xlink/1.0.0/xlinks.xsd
    + remove gml/2.1.2/xlinks.xsd (optional, as is now unused).
    + geometry.xsd: fixed so will now validate by conformant processors by:
    + geometry.xsd: moving minOccurs/maxOccurs cardinality indicators from
      <element> declarations to their containing <sequence> elements in the
      context of the GML property pattern. -- SJDC 2006-12-07
    + gml:Coord is suppressed. -- SJDC 2006-12-07

2005-11-22  Arliss Whiteside

  * GML versions 2.0.0 through 3.1.1: The sets of XML Schema Documents for
    OpenGIS GML Versions 2.0.0 through 3.1.1 have been edited to reflect the
    corrigenda to all those OGC documents that is based on the change requests: 
  OGC 05-068r1 "Store xlinks.xsd file at a fixed location"
  OGC 05-081r2 "Change to use relative paths"
 
  * Note: check each OGC numbered document for detailed changes.

