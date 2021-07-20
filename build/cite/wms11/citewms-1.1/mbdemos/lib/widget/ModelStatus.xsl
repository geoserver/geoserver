<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" 
    xmlns:mb="http://mapbuilder.sourceforge.net/mapbuilder" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<!--
Description: Output a form for setting any model's url and method values
Author:      Mike Adair
Licence:     GPL as per: http://www.gnu.org/copyleft/gpl.html

$Id$
-->

  <xsl:output method="xml" encoding="utf-8"/>

  <!-- The common params set for all widgets -->
  <xsl:param name="lang">en</xsl:param>
  <xsl:param name="modelId"/>
  <xsl:param name="modelTitle"/>
  <xsl:param name="widgetId"/>

  <!-- The name of the form for coordinate output -->
  <xsl:param name="statusMessage"/>

  <!-- Main html -->
  <xsl:template match="/">
    <DIV>
      <xsl:call-template name="title"/><xsl:value-of select="$modelTitle"/><xsl:value-of select="$statusMessage"/>
    </DIV>
  </xsl:template>
  
  <xsl:template name="title">
    <xsl:choose>
      <xsl:when test="$lang='fr'">Status for:</xsl:when>
      <xsl:otherwise>Status for:</xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  
</xsl:stylesheet>
