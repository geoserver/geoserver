<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" 
  xmlns:mb="http://mapbuilder.sourceforge.net/mapbuilder" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<!--
Description: Convert Mapbuilder Config to a list of buttons.  This stylesheet 
            takes a Button node from config as input
Author:      Mike Adair
Licence:     GPL as per: http://www.gnu.org/copyleft/gpl.html

ButtonBar.xsl,v 1.5 2004/03/25 21:25:43 madair1 Exp
-->

  <xsl:output method="xml" omit-xml-declaration="yes"/>
  
  <!-- The common params set for all widgets -->
  <xsl:param name="lang">en</xsl:param>
  <xsl:param name="modelId"/>
  <xsl:param name="widgetId"/>
  <xsl:param name="action"/>
  <xsl:param name="skinDir" select="/mb:MapbuilderConfig/mb:skinDir"/>
  
  <xsl:template match="*">
    <xsl:param name="linkUrl">javascript:config.objects.<xsl:value-of select="$widgetId"/>.select()<xsl:if test="$action">;config.objects.<xsl:value-of select="$action"/></xsl:if></xsl:param>
    <xsl:param name="buttonTitle"><xsl:value-of select="mb:tooltip[@xml:lang=$lang]"/></xsl:param>
    <A HREF="{$linkUrl}"><IMG SRC="{$skinDir}{mb:disabledSrc}" ID="{@id}" TITLE="{$buttonTitle}" BORDER="0"/></A>
  </xsl:template>

</xsl:stylesheet>
