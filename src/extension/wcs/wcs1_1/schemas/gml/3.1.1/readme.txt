GML 3.1.1 Schema Package Readme

The schema package contained in this directory is the normative GML
3.1.1 schema set as approved by the OGC membership in May 2005. This
version corrects a number of errors in the GML 3.1.0 schemas that
prevented validation.

Reason for Changes: The XML Schema for GML 3.1 fails to validate using
several of the most popular XML processing software applications.

Four sets of errors have been found in the GML 3.1 Schema, with
corrections applied as described:

1. the content model for "metaDataProperty" is non-determinstic: you
can't have a choice group containing <any> with anything else 
	- delete _MetaData from the choice group

2. membership of the _timeLength, _Value, _ScalarValue,
_ScalarValueList substitution groups relies on an interpretation of the
XML Schema specification which is not uniformly accepted
	- replace with an explicit <choice> groups 

3. axisName (type="string") in grids.xsd clashes with axisName
(different type) in coordinateSystems.xsd 
	- Remove the latter, use the gml:name property instead

4. a "derivation-by-restriction" pattern used widely throughout the
schema has a cardinality constraint (minOccurs="0", and sometimes
maxOccurs="unbounded") applied to a "property" element in a base type,
where the property element is the head of a substitution group; in a
type derived by restriction in which one or more members of the
substitution group is selected, the validation procedure described in
the W3C XML Schema specification requires that the validation
constraint appear on a <choice> container element, rather than on the
substitution group member element
	- move the cardinality constraint up to the <sequence>
	  container element
	- remove unnecessary derivation-by-restriction chains,
	  particularly concerning derivation of property elements

As this pattern is used pervasively in GML, there are numerous changes
in almost all of the schema documents.

The revised schema has been tested with Xerces-J, XSV, .NET, MSXML and
no validation errors are found. 

http://schemas.opengis.net/gml/3.1.1/

2005-08-11

