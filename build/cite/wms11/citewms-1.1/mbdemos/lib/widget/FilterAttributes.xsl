<?xml version="1.0" encoding="ISO-8859-1"?>

<!--
Description: parse DescribeFeatureType response to provide filter params
Author:      adair
Licence:     GPL as specified in http://www.gnu.org/copyleft/gpl.html .

$Id$
$Name:  $
-->

<xsl:stylesheet version="1.0" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:xsd="http://www.w3.org/2001/XMLSchema"
		xmlns:gml="http://www.opengis.net/gml">

  <xsl:output method="xml" omit-xml-declaration="no" encoding="utf-8" indent="yes"/>

  <!-- The coordinates of the DHTML Layer on the HTML page -->
  <xsl:param name="modelId"/>
  <xsl:param name="widgetId"/>
  
  <xsl:param name="elementName">
    <xsl:value-of select="/xsd:schema/xsd:element/@name"/>
  </xsl:param>
  <xsl:param name="elementType">
    <xsl:value-of select="/xsd:schema/xsd:element/@type"/>
  </xsl:param>
  <xsl:param name="elementTypeNoNs">
    <xsl:value-of select="substring-after($elementType,':')"/>
  </xsl:param>
  
  <!-- template rule matching source root element -->
  <xsl:template match="/xsd:schema">
    <table>
      <tr>
        <th colspan="3">
          Attributes for: <xsl:value-of select="$elementName"/> of type:<xsl:value-of select="$elementType"/>
        </th>
      </tr>
      <xsl:apply-templates select="xsd:complexType[@name=$elementTypeNoNs]"/>
    </table>
  </xsl:template>

  <!--
<xsd:complexType name="Landuse__Lat_Lon__Type">
<xsd:complexContent>
<xsd:extension base="gml:AbstractFeatureType">
<xsd:sequence>
<xsd:element name="GML_Geometry" -->

  <!-- template rule matching source root element -->
  <xsl:template match="xsd:element">
    <xsl:variable name="name"><xsl:value-of select="@name"/></xsl:variable>
    <xsl:variable name="type"><xsl:value-of select="@type"/></xsl:variable>
    <tr>
      <td>
        <xsl:value-of select="$name"/>
      </td>
      <td>
        <xsl:value-of select="$type"/>
      </td>
      <td>
      </td>
    </tr>
  </xsl:template>

  <!--xsl:template match="text()|@*"/-->

</xsl:stylesheet>
