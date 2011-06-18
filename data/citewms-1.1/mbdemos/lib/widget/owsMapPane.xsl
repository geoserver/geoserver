<?xml version="1.0" encoding="ISO-8859-1"?>

<!--
Description: parses an OGC context document to generate an array of DHTML layers.
Author:      adair
Licence:     GPL as specified in http://www.gnu.org/copyleft/gpl.html .

$Id$
$Name:  $
-->

<xsl:stylesheet version="1.0" 
    xmlns:wmc="http://www.opengis.net/context" 
    xmlns:ows="http://www.opengis.net/ows"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:xlink="http://www.w3.org/1999/xlink" exclude-result-prefixes="wmc xlink">

  <xsl:output method="xml"/>
  <xsl:strip-space elements="*"/>

  <!-- The coordinates of the DHTML Layer on the HTML page -->
  <xsl:param name="modelId"/>
  <xsl:param name="widgetId"/>
  <xsl:param name="context">config['<xsl:value-of select="$modelId"/>']</xsl:param>

  <xsl:param name="lowerLeft"><xsl:value-of select="/wmc:OWSContext/wmc:General/ows:BoundingBox/ows:LowerCorner"/></xsl:param>
  <xsl:param name="upperRight"><xsl:value-of select="/wmc:OWSContext/wmc:General/ows:BoundingBox/ows:UpperCorner"/></xsl:param>
  <xsl:param name="bbox"><xsl:value-of select="translate($lowerLeft,' ',',')"/>,<xsl:value-of select="translate($upperRight,' ',',')"/></xsl:param>
  <xsl:param name="width">
    <xsl:value-of select="/wmc:OWSContext/wmc:General/wmc:Window/@width"/>
  </xsl:param>
  <xsl:param name="height">
    <xsl:value-of select="/wmc:OWSContext/wmc:General/wmc:Window/@height"/>
  </xsl:param>
  <xsl:param name="srs" select="/wmc:OWSContext/wmc:General/ows:BoundingBox/@crs"/>
  
  <!-- template rule matching source root element -->
  <xsl:template match="/wmc:OWSContext">
      <DIV STYLE="width:{$width}; height:{$height}; position:absolute">
        <xsl:apply-templates select="wmc:ResourceList/*"/>
      </DIV>
  </xsl:template>
  
  <!-- these handled outside of the stylesheet -->
  <xsl:template match="wmc:Coverage"/>
  <xsl:template match="wmc:FeatureType"/>
  
  <xsl:template match="wmc:Layer">
    <xsl:param name="version">
        <xsl:value-of select="wmc:Server/@version"/>    
    </xsl:param>
    <xsl:param name="baseUrl">
        <xsl:value-of select="wmc:Server/wmc:OnlineResource/@xlink:href"/>    
    </xsl:param>
    <xsl:variable name="visibility">
      <xsl:choose>
        <xsl:when test="@hidden='1'">hidden</xsl:when>
        <xsl:otherwise>visible</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="firstJoin">
      <xsl:choose>
        <xsl:when test="substring($baseUrl,string-length($baseUrl))='?'"></xsl:when>
        <xsl:when test="contains($baseUrl, '?')">&amp;</xsl:when> 
        <xsl:otherwise>?</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="mapRequest">
      <xsl:choose>
        <xsl:when test="starts-with($version, '1.0')">
            WMTVER=<xsl:value-of select="$version"/>&amp;REQUEST=map
        </xsl:when>            
        <xsl:otherwise>
            VERSION=<xsl:value-of select="$version"/>&amp;REQUEST=GetMap&amp;SERVICE=WMS
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <DIV>    
        <xsl:attribute name="STYLE">position:absolute; visibility:<xsl:value-of select="$visibility"/>; top:0; left:0;</xsl:attribute>
        <xsl:attribute name="ID"><xsl:value-of select="$modelId"/>_<xsl:value-of select="$widgetId"/>_<xsl:value-of select="wmc:Name"/></xsl:attribute>
    
    <xsl:element name="IMG">    
        <xsl:variable name="src">    
            <xsl:value-of select="$baseUrl"/>
            <xsl:value-of select="$firstJoin"/>
             <xsl:value-of select="$mapRequest"/>
   &amp;SRS=<xsl:value-of select="$srs"/>
  &amp;BBOX=<xsl:value-of select="$bbox"/>
 &amp;WIDTH=<xsl:value-of select="$width"/>
&amp;HEIGHT=<xsl:value-of select="$height"/>
&amp;LAYERS=<xsl:value-of select="wmc:Name"/>
&amp;STYLES=<xsl:value-of select="translate(wmc:StyleList/wmc:Style[@current='1']/wmc:Name,' ','+')"/>
&amp;FORMAT=<xsl:value-of select="wmc:FormatList/wmc:Format[@current='1']"/>
&amp;TRANSPARENT=TRUE

<!--	
  //TBD: these still to be properly handled 
  //also sld support
  if (this.transparent) src += '&' + 'TRANSPARENT=' + this.transparent;
	if (this.bgcolor) src += '&' + 'BGCOLOR=' + this.bgcolor;
	//if (this.exceptions) src += '&' + 'EXCEPTIONS=' + this.exceptions;
	if (this.vendorstr) src += '&' + this.vendorstr;
  //
        -->
        </xsl:variable>
        <xsl:attribute name="SRC">    
            <xsl:value-of select="translate(normalize-space($src),' ', '' )" disable-output-escaping="no"/>
        </xsl:attribute>
        <xsl:attribute name="WIDTH">
            <xsl:value-of select="$width"/>
        </xsl:attribute>
        <xsl:attribute name="HEIGHT">
            <xsl:value-of select="$height"/>
        </xsl:attribute>
        <xsl:attribute name="ALT">
            <xsl:value-of select="wmc:Title"/>
        </xsl:attribute>
    </xsl:element>
    </DIV>    
  </xsl:template>

</xsl:stylesheet>
