.. _dxf:

DXF OutputFormat for WFS and WPS PPIO
=====================================
This extension adds two distinct functionalities to GeoServer, both related to DXF format
support as an output.

DXF is a CAD interchange format, useful to import data in several CAD systems.
Being a textual format it can be easily compressed to a much smaller version, so
the need for a DXF-ZIP format, for low bandwidth usage.

There have been multiple revisions of the format, so we need to choose a "version"
of DXF to write. The extension implements version 14, but can be easily extended
(through SPI providers) to write other versions too.

The DXF OutputFormat for WFS adds the support for two additional output formats for 
WFS GetFeature requests. The new formats, DXF and DXF-ZIP are associated to the 
"application/dxf" and "application/zip" mime type, respectively.
They produce a standard DXF file or a DXF file compressed in zip format.

The WPS PPIO adds dxf as an on output format option for WPS processes.
The WPS PPIO requires the WPS extension to be installed on GeoServer.

WFS Output Format usage
---------------------------

Request Example::

	http://localhost:8080/geoserver/wfs?request=GetFeature&typeName=Polygons&
	outputFormat=dxf
 
 
Output Example (portion)::

	  0
	SECTION
	  2
	HEADER
	  9
	$ACADVER
	  1
	AC1014
	...
	  0
	ENDSEC
	...
	  0
	SECTION
	  2
	TABLES
	...  
	  0
	TABLE
	  2
	LAYER
	...
	  0
	LAYER
	  5
	2E
	330
	2
	100
	AcDbSymbolTableRecord
	100
	AcDbLayerTableRecord
	  2
	POLYGONS
	 70
		 0
	 62
		 7
	  6
	CONTINUOUS
	  0
	ENDTAB
	...
	  0
	ENDSEC
	  0
	SECTION
	  2
	BLOCKS
	  ...
	  0
	ENDSEC
	  0
	SECTION
	  2
	ENTITIES
	  0
	LWPOLYLINE
	  5
	927C0
	330
	1F
	100
	AcDbEntity
	  8
	POLYGONS
	100
	AcDbPolyline
	 90
		 5
	 70
		 1
	 43
	0.0
	 10
	500225.0
	 20
	500025.0
	 10
	500225.0
	 20
	500075.0
	 10
	500275.0
	 20
	500050.0
	 10
	500275.0
	 20
	500025.0
	 10
	500225.0
	 20
	500025.0
	  0
	ENDSEC
	  0
	SECTION
	  2
	OBJECTS
	...
	  0
	ENDSEC
	  0
	EOF


Each single query is rendered as a layer. Geometries are encoded as
entities (if simple enough to be expressed by a single DXF geometry
type) or blocks (if complex, such as polygons with holes or collections).

Some options are available to control the output generated. They are 
described in the following paragraphs.

GET requests format_options
---------------------------
The following format_options are supported:
 #. version: (number) creates a DXF in the specified version format (only 14 is currently supported)
 #. asblock: (true/false) if true, all geometries are written as blocks and then inserted as entities. If false, simple geometries are directly written as entities.
 #. colors: (comma delimited list of numbers): colors to be used for the DXF layers, in sequence. If layers are more than the specified colors, they will be reused many times. A set of default colors is used if the option is not used. Colors are AutoCad color numbers (7=white, etc.).
 #. ltypes: (comma delimited list of line type descriptors): line types to be used for the DXF layers, in sequence. If layers are more than the specified line types, they will be reused many times. If not specified, all layers will be given a solid, continuous line type. A descriptor has the following format: <name>!<repeatable pattern>[!<base length>], where <name> is the name assigned to the line type, <base length> (optional) is a real number that tells how long is each part of the line pattern (defaults to 0.125), and <repeatable pattern> is a visual description of the repeatable part of the line pattern, as a sequence of - (solid line),* (dot) and _ (empty space). For example a dash-dot pattern would be expressed as --_*_.
 #. layers: (comma delimited list of strings) names to be assigned to the DXF layers. If specified, must contain a name for each requested query. By default a standard name will be assigned to layers.
 #. withattributes: (true/false) enables writing an extra layer with attributes from each feature, the layer has a punctual geometry, with a point in the centroid of the original feature

POST options
---------------------------
Unfortunately, it's not currently possibile to use format_options in POST
requests. The only thing we chose to implement is the layers options, via
the handle attribute of Query attributes. So, if specified, the layer
of a Query will be named as its handle attribute.
The handle attribute of the GetFeature tag can also be used to override
the name of the file produced.

WPS PPIO
---------------------------
When the WPS PPIO module is installed, together with the WPS extension, WPS processes
returning a FeatureCollection can use application/dxf or application/zip as output
mime type to get a DXF (or zipped DXF) in output.
