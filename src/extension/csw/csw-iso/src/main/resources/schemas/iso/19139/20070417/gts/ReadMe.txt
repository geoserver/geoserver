ISO(c) GTS schema ReadMe.txt
------------------------------------------------------------------------------

Geographic Temporal Schema (GTS) extensible markup language

GTS is a component of the XML Schema Implementation of Geographic
Information Metadata documented in ISO/TS 19139:2007.

GTS includes all the definitions of http://www.isotc211.org/2005/gts
namespace. The root document of this namespace is the file gts.xsd.

The most current schemas are available at:
http://standards.iso.org/ittf/PubliclyAvailableStandards/ISO_19139_Schemas/

-------------------------------------------------------------------------------

2012-07-13 Nicolas Lesage on behalf of the ISO/TC 211 XML Maintenance Group
	* Update of readme.txt file and schema annotations
	* Use of absolute schema locations of imported namespaces
	* Simplification of the schema location of included XML Schemas
	* Addition of the version attribute to the schema element. The value of
	  this attribute is expected to be the date of the last release of the
	  XML schemas (e.g. 2012-07-13 for this release)
	* Include root XML Schema document in all schema documents

	Validation: Schemas have been validated with XML Spy 2010 Rel. 2 (MSXML 6.0)

2009-03-16 Marcellin Prudham & Nicolas Lesage
	* Change of GML namespace: http://www.opengis.net/gml (GML 3.2) => 
	                           http://www.opengis.net/gml/3.2 (GML 3.2.1=ISO 19136)
							   
	Note: ISO/TS 19139:2007 (published 2007-04-17) normatively reference
	ISO 19136 which was	published 2007-08-23. The major change applied to
	ISO 19136 is the change of the namespace URI. Previous release of GTS
	are not compliant with ISO/TS 19139:2007
	
	Validation: Schemas have been validated with XSV 2.10, Xerces J 2.7.1 and 
	XML Spy 2009 (2009-03-02, IGN / France - Nicolas Lesage / Marcellin Prudham)
							   
2006-05-04 Marie-Pierre Escher & Nicolas Lesage
	* First official release of GTS
	* GTS XML Schema files were generated from ISO/TC 211 UML class diagrams
  	  in accordance with ISO/TS 19139:2007. The XML Schema generator is a
	  Rational Rose Plug-in developed by IGN France (http://www.ign.fr).
