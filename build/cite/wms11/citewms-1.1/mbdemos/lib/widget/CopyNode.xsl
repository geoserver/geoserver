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
    xmlns:wms="http://www.opengis.net/wms" 
    xmlns:wfs="http://www.opengis.net/wfs" 
		xmlns:sld="http://www.opengis.net/sld"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:xlink="http://www.w3.org/1999/xlink">

  <xsl:output method="xml"/>
  <xsl:strip-space elements="*"/>

  <!-- The coordinates of the DHTML Layer on the HTML page -->
  <xsl:param name="modelId"/>
  <xsl:param name="widgetId"/>
  
  <xsl:param name="version"/>
  <xsl:param name="serverUrl"/>
  <xsl:param name="serviceName"/>
  <xsl:param name="serverTitle"/>
  
  <xsl:template match="Layer">
    <wmc:Layer>
      <xsl:attribute name="queryable">0</xsl:attribute>
      <xsl:attribute name="hidden">0</xsl:attribute>
			<wmc:Server>
        <xsl:attribute name="service"><xsl:value-of select="$serviceName"/></xsl:attribute>
        <xsl:attribute name="version"><xsl:value-of select="$version"/></xsl:attribute>
        <xsl:attribute name="title"><xsl:value-of select="$serverTitle"/></xsl:attribute>
				<wmc:OnlineResource xlink:type="simple" xlink:href="{$serverUrl}"/>
			</wmc:Server>
      <xsl:apply-templates select="child::node()"/>
    </wmc:Layer>
  </xsl:template>
  
  <xsl:template match="Title">
    <wmc:Title><xsl:value-of select="."/></wmc:Title>
  </xsl:template>
  
  <xsl:template match="Name">
    <wmc:Name><xsl:value-of select="."/></wmc:Name>
  </xsl:template>
  
  <xsl:template match="Abstract">
    <wmc:Abstract><xsl:value-of select="."/></wmc:Abstract>
  </xsl:template>
  
  <xsl:template match="DataURL">
    <wmc:DataURL><xsl:value-of select="."/></wmc:DataURL>
  </xsl:template>
  
  <xsl:template match="MetadataURL">
    <wmc:MetadataURL><xsl:value-of select="."/></wmc:MetadataURL>
  </xsl:template>
  
  <xsl:template match="SRS">
    <wmc:SRS><xsl:value-of select="."/></wmc:SRS>
  </xsl:template>
  
  <xsl:template match="text()|@*"/>
  
</xsl:stylesheet>
