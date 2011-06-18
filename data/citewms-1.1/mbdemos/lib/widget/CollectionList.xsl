<?xml version="1.0" encoding="ISO-8859-1"?>

<!--
Description: parses an OGC context collection document to generate a context pick list
Author:      adair
Licence:     GPL as specified in http://www.gnu.org/copyleft/gpl.html .

$Id$
$Name:  $
-->

<xsl:stylesheet version="1.0" 
    xmlns:wmc="http://www.opengis.net/context" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:xlink="http://www.w3.org/1999/xlink"
    exclude-result-prefixes="wmc xlink">

  <xsl:output method="xml" omit-xml-declaration="yes" encoding="utf-8"/>
  <xsl:strip-space elements="*"/>

  <!-- The common params set for all widgets -->
  <xsl:param name="lang">en</xsl:param>

  <!-- The coordinates of the DHTML Layer on the HTML page -->
  <xsl:param name="jsfunction">config.loadModel('</xsl:param>
  <xsl:param name="targetModel"/>

  <!-- template rule matching source root element -->
  <xsl:template match="/wmc:ViewContextCollection">

    <UL>
      <xsl:call-template name="title"/>
      <xsl:apply-templates select="wmc:ViewContextReference"/>
    </UL>

  </xsl:template>

  <xsl:template match="wmc:ViewContextReference">
    <xsl:param name="linkUrl">javascript:<xsl:value-of select="$jsfunction"/><xsl:value-of select="$targetModel"/>','<xsl:value-of select="wmc:ContextURL/wmc:OnlineResource/@xlink:href"/>')</xsl:param>
    <LI>    
      <A HREF="{$linkUrl}">
        <xsl:choose>
          <xsl:when test="wmc:Title/@xml:lang">              
            <xsl:value-of select="wmc:Title[@xml:lang=$lang]"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="wmc:Title"/>
          </xsl:otherwise>
        </xsl:choose>
      </A>
    </LI>    
  </xsl:template>
  
  <xsl:template name="title">
    <xsl:choose>
      <xsl:when test="$lang='fr'">Choisir une carte pour regardez:</xsl:when>
      <xsl:otherwise>Select a map to load:</xsl:otherwise>
    </xsl:choose>
  </xsl:template>

</xsl:stylesheet>
