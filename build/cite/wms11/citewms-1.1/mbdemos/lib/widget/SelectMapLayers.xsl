<?xml version="1.0" encoding="ISO-8859-1"?>

<!--
Description: parses an OGC context document to generate an array of DHTML layers.
Author:      adair
Licence:     GPL as specified in http://www.gnu.org/copyleft/gpl.html .

$Id$
$Name:  $
-->

<xsl:stylesheet version="1.0" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:wms="http://www.opengis.net/wms"
		xmlns:ogc="http://www.opengis.net/ogc"
    xmlns:xlink="http://www.w3.org/1999/xlink">

  <xsl:output method="xml" omit-xml-declaration="no" encoding="utf-8" indent="yes"/>

  <!-- The coordinates of the DHTML Layer on the HTML page -->
  <xsl:param name="modelId"/>
  <xsl:param name="widgetId"/>
  
  <!-- template rule matching source root element -->
  <xsl:template match="/WMT_MS_Capabilities">
    <table>
      <tr>
        <th colspan="3">
          Map Layers from: <xsl:value-of select="Service/Title"/>
        </th>
        <td colspan="2">
          <a href="javascript:config.paintWidget(config.objects.wmsServerList)">Back to list</a>
        </td>
      </tr>
      <xsl:apply-templates/>
    </table>
  </xsl:template>

  <!-- template rule matching source root element -->
  <xsl:template match="Layer">
    <xsl:variable name="name"><xsl:value-of select="Name"/></xsl:variable>
    <xsl:variable name="id"><xsl:value-of select="@id"/></xsl:variable>
    <tr>
      <td>
        <xsl:value-of select="Title"/>
      </td>
      <td>
        <a href="javascript:config.objects.{$modelId}.setParam('GetMap','{$name}')">preview</a>
      </td>
      <td>
        <a href="javascript:config.objects.{$modelId}.addToContext('{$name}')">add to map</a>
      </td>
    </tr>
    <xsl:apply-templates/>
  </xsl:template>

  <xsl:template match="text()|@*"/>

</xsl:stylesheet>
