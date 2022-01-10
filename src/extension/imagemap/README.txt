HTMLImageMap GetMapProducer for GeoServer

USAGE
---------------------------

The HTMLImageMap GetMapProducer adds support for an additional output format for 
WMS GetMap requests. The new format is associated to the "text/html" mime type
and produces an HTML 4.0 image map as defined by W3C.

Image Maps can be used to cut images in several areas. Each area can have a link, 
title or alt attribute.
 
Request Example.
http://localhost:8080/geoserver/wms?bbox=-130,24,-66,50&styles=population&Format=text/
html&request=GetMap&layers=topp:states&width=550&height=250&srs=EPSG:4326
 
Output Example.
<map name="states">
	<area shape="poly" id="states.1.0"  coords="360,120 358,121 357,121 357,122 
357,123 357,124 356,124 355,124 354,123 352,123 351,124 351,125 350,124 350,125 
349,125 349,124 348,123 348,122 348,121 348,120 348,118 347,118 346,117 345,116 
344,116 343,115 342,114 341,113 341,112 341,111 341,110 342,110 342,109 343,108 
342,107 340,106 339,107 338,107 338,105 338,104 337,103 336,102 336,102 335,101 
333,100 332,99 332,98 331,97 331,96 331,95 331,94 331,93 332,91 332,90 333,90 334,90 
334,89 335,88 336,86 335,85 334,84 335,83 336,82 336,82 338,82 339,82 340,81 341,81 
341,80 342,79 342,78 342,77 342,76 341,75 340,74 340,73 339,73 338,72 340,72 344,72 
349,72 353,72 354,72 358,72 359,72 363,72 362,74 363,75 364,76 364,78 365,80 365,82 
365,84 365,85 365,86 365,89 365,91 365,95 365,97 365,100 365,101 365,102 364,103 
364,104 364,105 365,106 365,107 365,108 365,109 364,110 364,110 363,111 363,112 
362,113 361,113 362,114 361,115 361,116 360,117 360,118 360,119 360,120" title="IL"/>
	...
</map>

Each single feature is rendered as one or more area tag. More than one area tag is 
used if the geometry is not compatible with imagemaps constraints, and a split in 
more simple geometries is needed.

An ID attribute is generated for each area tag. The ID is made up of two parts: 
<featureId>.<sequence>, where <featureId> is the id of the feature containing the 
geometry rendered in the tag. Sequence is a sequential number (0,1,2, ...) appended 
to the id distinguishing different simple geometries in which the feature could have 
been splitted.

Usage Example.
<img src="..." usemap="#states"/>

STYLING
----------------------------
A little bit of styling is supported through SLD symbolizers. In particular:
 - TextSymbolizers are used to append attributes to the area tags. You can define a 
Label, whose output will be rendered as an attribute value. By default, the 
attribute name is "title" (this permits to define tooltips). You can define several 
custom attributes, using different rules, the rule name will be used as attribute 
name (eg. 
<Rule>...<Name>href</Name>...<TextSymbolizer>...</TextSymbolizer>...</Rule> defines 
an href attribute, binding links to the rendered areas).
 - LineSymbolizers stroke-width parameter is used to define a buffer along 
linestrings (defaults to 2px if not defined).
 - PointSymbolizers Size is used to define circle radius for rendered points 
(currently only Marks with a WellKnownName of "circle" are
   supported).
   
Examples.

1) Point with a label attached as title attribute ("ADDRESS: <...>"). The point is 
rendered as 6px radius circle.

<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor version="1.0.0" 	
	xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd" 	
	xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc"
 	xmlns:xlink="http://www.w3.org/1999/xlink" 	
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
	<UserStyle>
		<Name>Default Styler</Name>
		<Title>Default Styler</Title>
		<Abstract/>
		<FeatureTypeStyle>
			<FeatureTypeName>Feature</FeatureTypeName>
			<Rule>
				<Name>title</Name>
				<Title>all</Title>
				<PointSymbolizer>
					<Graphic>
						<Mark>
							<WellKnownName>circle</WellKnownName>
							<Stroke>
								<CssParameter name="stroke">#0000ff</CssParameter>
							</Stroke>
						</Mark>
						<Size>6</Size>
						<Opacity>1</Opacity>
					</Graphic>
				</PointSymbolizer>
				<TextSymbolizer>
					<Label>
						<Function name="strConcat">
							<Literal>ADDRESS: </Literal>
							<PropertyName>ADDRESS</PropertyName>
						</Function>
					</Label>
				</TextSymbolizer>
			</Rule>
		</FeatureTypeStyle>
	</UserStyle>
</StyledLayerDescriptor>  

2) LineStrings rendered with a buffer of 4px.

<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor version="1.0.0" 
	xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd" 
	xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" 
	xmlns:xlink="http://www.w3.org/1999/xlink" 
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <UserStyle>
        <Name>Default Styler</Name>
        <Title>Default Styler</Title>
        <Abstract></Abstract>
        <FeatureTypeStyle>
            <FeatureTypeName>Feature</FeatureTypeName>
			<Rule>
				<Name>title</Name>
				<Title>Roads</Title>				
				<LineSymbolizer>
					<Stroke>						
						<CssParameter name="stroke-width">
							<ogc:Literal>4</ogc:Literal>
						</CssParameter>
					</Stroke>
				</LineSymbolizer>
			</Rule>
			
        </FeatureTypeStyle>
    </UserStyle>
</StyledLayerDescriptor>

3) GeometryCollection where LineStrings are rendered with a buffer of 4px, 
points as circles with 6px radius, any geometry has an attribute href whose 
value is given by the DESCRIPTION feature attribute.

<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor version="1.0.0" 
	xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd" 
	xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" 
	xmlns:xlink="http://www.w3.org/1999/xlink" 
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <UserStyle>
        <Name>Default Styler</Name>
        <Title>Default Styler</Title>
        <Abstract></Abstract>
        <FeatureTypeStyle>
            <FeatureTypeName>Feature</FeatureTypeName>
			<Rule>
				<Name>href</Name>
				<Title>all</Title>				
				<LineSymbolizer>
					<Stroke>						
						<CssParameter name="stroke-width">
							<ogc:Literal>4</ogc:Literal>
						</CssParameter>
					</Stroke>
				</LineSymbolizer>
				<PointSymbolizer>
					<Graphic>
						<Mark>
							<WellKnownName>circle</WellKnownName>
							<Stroke>
								<CssParameter name="stroke">#0000ff</CssParameter>
							</Stroke>
						</Mark>
						<Size>6</Size>
						<Opacity>1</Opacity>
					</Graphic>
				</PointSymbolizer>
				<TextSymbolizer>
					<Label>
						<PropertyName>DESCRIPTION</PropertyName>
					</Label>
				</TextSymbolizer>
			</Rule>
			
        </FeatureTypeStyle>
    </UserStyle>
</StyledLayerDescriptor>

BUILD FROM SOURCE
---------------------------

1) Install JAVA SDK
Download and install the Java SDK.

Create an environment variable called JAVA_HOME and point it to your Java SDK 
directory.
Then modify the PATH variable and add: ;%JAVA_HOME%/bin
Apply the changes.

2) Download and install Maven
Windows: http://www.apache.org/dyn/closer.cgi/maven/binaries/maven-2.0.4.exe
Linux: http://www.apache.org/dyn/closer.cgi/maven/binaries/maven-2.0.4.zip


If you are using Linux, execute the following commands:
export M2_HOME=/usr/java/maven-2.0.4
export PATH=$PATH:$M2_HOME/bin

3) Build Source Code
Go to the command line and navigate to the root of the source tree.
Execute the command:
mvn install

If it fails, just try again. It trys to download jars and some might not be 
available at that time. So just keep trying.

DEPLOY THE GETMAPPRODUCER
---------------------------

1) copy target/htmlimagemap-*.jar to geoserver WEB-INF/lib.

2) restart geoserver.
