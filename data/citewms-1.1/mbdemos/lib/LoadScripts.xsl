<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet
  xmlns:mb="http://mapbuilder.sourceforge.net/mapbuilder" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  exclude-result-prefixes="mb" version="1.0" >
<!--
Description: Convert Mapbuilder Config to a list of buttons.
Author:      Mike Adair
Licence:     GPL as per: http://www.gnu.org/copyleft/gpl.html

ButtonBar.xsl,v 1.5 2004/03/25 21:25:43 madair1 Exp
 || mb:widgets/* || mb:tools/*
-->

  <xsl:output method="html" omit-xml-declaration="yes"/>
  
  <xsl:param name="baseDir">/mapbuilder/lib</xsl:param>
  
  <xsl:template match="/mb:MapbuilderConfig">
    <xsl:apply-templates />
  </xsl:template>
  
  <xsl:template match="mb:models/*">
    <xsl:variable name="scriptfile"><xsl:value-of select="$baseDir"/>/model/<xsl:value-of select="name(.)"/>.js</xsl:variable>
    <script type="text/javascript" src="{$scriptfile}"></script>
    <xsl:apply-templates />
  </xsl:template>
  
  <xsl:template match="mb:widgets/*">
    <xsl:variable name="scriptfile"><xsl:value-of select="$baseDir"/>/widget/<xsl:value-of select="name(.)"/>.js</xsl:variable>
    <script type="text/javascript" src="{$scriptfile}"></script>
    <xsl:apply-templates select="mb:scriptfile"/>
  </xsl:template>
  
  <xsl:template match="mb:tools/*">
    <xsl:variable name="scriptfile"><xsl:value-of select="$baseDir"/>/tool/<xsl:value-of select="name(.)"/>.js</xsl:variable>
    <script type="text/javascript" src="{$scriptfile}"></script>
  </xsl:template>
  
  <xsl:template match="mb:scriptfile">
    <xsl:variable name="scriptfile"><xsl:value-of select="."/></xsl:variable>
    <script type="text/javascript" src="{$scriptfile}"></script>
  </xsl:template>
  
  <xsl:template match="text()|@*"/>

</xsl:stylesheet>
