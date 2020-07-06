<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<!--
Description: Outputs model URL and a link to save the current version of it
Author:      Mike Adair
Licence:     GPL as per: http://www.gnu.org/copyleft/gpl.html

AoiForm.xsl,v 1.2 2004/06/25 17:59:38 madair1 Exp
-->

  <xsl:output method="xml" encoding="utf-8"/>

  <!-- The common params set for all widgets -->
  <xsl:param name="lang">en</xsl:param>
  <xsl:param name="modelId"/>
  <xsl:param name="widgetId"/>

  <!-- The name of the form for coordinate output -->
  <xsl:param name="modelUrl"/>
  <xsl:param name="serializeUrl">/mapbuilder/writeXml</xsl:param>
  <xsl:param name="echoUrl">/mapbuilder/echoXml</xsl:param>

  <!-- Main html -->
  <xsl:template match="/">
    <div>
      <h3>Model URLs: <xsl:value-of select="$modelId"/></h3>
      <a href="{$modelUrl}" target="modelWin">original model URL</a><br/>
      <a href="javascript:config.objects.{$modelId}.saveModel(config.objects.{$modelId})">save current model to disk</a><br/>
      <a target="modelXML" id="{$modelId}.{$widgetId}.modelUrl">pick it up here</a><br/>
    </div>
  </xsl:template>
  
</xsl:stylesheet>
