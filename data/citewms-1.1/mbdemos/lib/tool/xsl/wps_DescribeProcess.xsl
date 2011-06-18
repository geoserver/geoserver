<?xml version="1.0" encoding="ISO-8859-1"?>

<!--
Description: transforms a WFS FeatureType node to a DescribeFeatureType request
Author:      adair
Licence:     GPL as specified in http://www.gnu.org/copyleft/gpl.html .

$Id$
$Name:  $
-->

<xsl:stylesheet version="1.0" 
    xmlns:wps="http://www.opengis.net/wps"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
		xmlns:ogc="http://www.opengis.net/ogc"
    xmlns:ows="http://www.opengis.net/ows"
		xmlns:gml="http://www.opengis.net/gml"
    xmlns:xlink="http://www.w3.org/1999/xlink">

  <xsl:output method="xml" omit-xml-declaration="no" encoding="utf-8" indent="yes"/>

  <xsl:param name="httpMethod">get</xsl:param>
  
  <!-- template rule matching source root element -->
  <xsl:template match="/wps:Capabilities/wps:ProcessOfferings">
    <DescribeProcess version="1.0.0" service="WFS">
      <xsl:apply-templates select="wps:FeatureType"/>
    </DescribeProcess>
  </xsl:template>

  <!-- template rule matching source root element -->
  <xsl:template match="wps:Process">
    <xsl:choose>
      <xsl:when test="$httpMethod='post'">
        <name><xsl:value-of select="wps:name"/></name>
      </xsl:when>
      <xsl:otherwise>
        <QueryString>
          <xsl:variable name="query">
      request=DescribeProcess
 &amp;version=0.0.1
 &amp;service=WPS
&amp;ProcessName=<xsl:value-of select="wps:name"/>
          </xsl:variable>
          <xsl:value-of select="translate(normalize-space($query),' ', '' )" disable-output-escaping="no"/>
        </QueryString>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="text()|@*"/>

</xsl:stylesheet>
