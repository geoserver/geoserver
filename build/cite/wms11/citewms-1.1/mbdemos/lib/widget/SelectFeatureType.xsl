<?xml version="1.0" encoding="ISO-8859-1"?>

<!--
Description: parses an OGC context document to generate an array of DHTML layers.
Author:      adair
Licence:     GPL as specified in http://www.gnu.org/copyleft/gpl.html .

$Id$
$Name:  $
-->

<xsl:stylesheet version="1.0" 
    xmlns:ogcwfs="http://www.opengis.net/wfs"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
		xmlns:ogc="http://www.opengis.net/ogc"
		xmlns:gml="http://www.opengis.net/gml"
    xmlns:xlink="http://www.w3.org/1999/xlink">

  <xsl:output method="xml" omit-xml-declaration="no" encoding="utf-8" indent="yes"/>

  <!-- The coordinates of the DHTML Layer on the HTML page -->
  <xsl:param name="modelId"/>
  <xsl:param name="widgetId"/>
  <xsl:param name="toolId"/>
  
  <!-- template rule matching source root element -->
  <xsl:template match="/ogcwfs:WFS_Capabilities">
    <table>
      <tr>
        <th>
          Feature types from: <xsl:value-of select="ogcwfs:Service/ogcwfs:Title"/>
        </th>
        <td colspan="2">
          <a href="javascript:config.paintWidget(config.objects.wfsServerList)">Back to list</a>
        </td>
      </tr>
      <xsl:apply-templates select="ogcwfs:FeatureTypeList/ogcwfs:FeatureType"/>
    </table>
  </xsl:template>

  <!-- template rule matching source root element -->
  <xsl:template match="ogcwfs:FeatureType">
    <xsl:variable name="name"><xsl:value-of select="ogcwfs:Name"/></xsl:variable>
    <xsl:variable name="title"><xsl:value-of select="ogcwfs:Title"/></xsl:variable>
    <xsl:variable name="id"><xsl:value-of select="@id"/></xsl:variable>
    <tr>
      <td>
        <xsl:value-of select="$title"/> (<xsl:value-of select="$name"/>) <xsl:value-of select="ogcwfs:SRS"/>
      </td>
      <td>
        <a href="javascript:config.objects.{$modelId}.setParam('wfs_GetFeature','{$name}')">load</a>
      </td>
      <td>
        <a href="javascript:config.objects.{$modelId}.setParam('wfs_DescribeFeatureType','{$name}')">filter</a>
      </td>
    </tr>
  </xsl:template>

  <xsl:template match="text()|@*"/>

</xsl:stylesheet>
