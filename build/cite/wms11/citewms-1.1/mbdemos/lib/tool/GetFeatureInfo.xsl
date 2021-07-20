<?xml version="1.0" encoding="ISO-8859-1"?>

<!--
Description: parses an OGC context document to generate a GetFeatureInfo url
Author:      Nedjo
Licence:     GPL as specified in http://www.gnu.org/copyleft/gpl.html .

$Id$
$Name:  $
-->
<xsl:stylesheet version="1.0" xmlns:cml="http://www.opengis.net/context" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xlink="http://www.w3.org/1999/xlink">
  <xsl:output method="xml"/>
  <!--xsl:strip-space elements="*"/-->

  <!-- parameters to be passed into the XSL -->
  <!-- The name of the WMS GetFeatureInfo layer -->
  <xsl:param name="queryLayer"/>
  <xsl:param name="xCoord"/>
  <xsl:param name="yCoord"/>
  <xsl:param name="infoFormat" select="text/html"/>
  <xsl:param name="featureCount" select="1"/>

  <!-- Global variables -->
  <xsl:variable name="bbox">
    <xsl:value-of select="/cml:ViewContext/cml:General/cml:BoundingBox/@minx"/>,<xsl:value-of select="/cml:ViewContext/cml:General/cml:BoundingBox/@miny"/>,<xsl:value-of select="/cml:ViewContext/cml:General/cml:BoundingBox/@maxx"/>,<xsl:value-of select="/cml:ViewContext/cml:General/cml:BoundingBox/@maxy"/>
  </xsl:variable>
  <xsl:variable name="width">
    <xsl:value-of select="/cml:ViewContext/cml:General/cml:Window/@width"/>
  </xsl:variable>
  <xsl:variable name="height">
    <xsl:value-of select="/cml:ViewContext/cml:General/cml:Window/@height"/>
  </xsl:variable>
  <xsl:variable name="srs" select="/cml:ViewContext/cml:General/cml:BoundingBox/@SRS"/>

  <!-- Root template -->
  <xsl:template match="/">
    <xsl:apply-templates select="cml:ViewContext/cml:LayerList/cml:Layer[cml:Name=$queryLayer]"/>
  </xsl:template>

  <!-- Layer template -->
  <xsl:template match="cml:Layer">

    <!-- Layer variables -->
    <xsl:variable name="version">
      <xsl:value-of select="cml:Server/@version"/>    
    </xsl:variable>
    <xsl:variable name="baseUrl">
      <xsl:value-of select="cml:Server/cml:OnlineResource/@xlink:href"/>    
    </xsl:variable>
    <xsl:variable name="firstJoin">
      <xsl:choose>
        <xsl:when test="substring($baseUrl,string-length($baseUrl))='?'"></xsl:when>
        <xsl:when test="contains($baseUrl, '?')">&amp;</xsl:when> 
        <xsl:otherwise>?</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <!-- Print the URL -->
    <url>
      <xsl:value-of select="$baseUrl"/><xsl:value-of select="$firstJoin"/>VERSION=<xsl:value-of select="$version"/>&amp;REQUEST=GetFeatureInfo&amp;SRS=<xsl:value-of select="$srs"/>&amp;BBOX=<xsl:value-of select="$bbox"/>&amp;WIDTH=<xsl:value-of select="$width"/>&amp;HEIGHT=<xsl:value-of select="$height"/>&amp;INFO_FORMAT=<xsl:value-of select="$infoFormat"/>&amp;FEATURE_COUNT=<xsl:value-of select="$featureCount"/>&amp;QUERY_LAYERS=<xsl:value-of select="$queryLayer"/>&amp;X=<xsl:value-of select="$xCoord"/>&amp;Y=<xsl:value-of select="$yCoord"/>
    </url>
  </xsl:template>
</xsl:stylesheet>
