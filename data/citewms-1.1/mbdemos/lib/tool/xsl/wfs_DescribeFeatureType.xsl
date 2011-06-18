<?xml version="1.0" encoding="ISO-8859-1"?>

<!--
Description: transforms a WFS FeatureType node to a DescribeFeatureType request
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

  <xsl:param name="httpMethod">get</xsl:param>
  
  <!-- template rule matching source root element -->
  <xsl:template match="/ogcwfs:WFS_Capabilities/ogcwfs:FeatureTypeList">
    <DescribeFeatureType version="1.0.0" service="WFS">
      <xsl:apply-templates select="ogcwfs:FeatureType"/>
    </DescribeFeatureType>
  </xsl:template>

  <!-- template rule matching source root element -->
  <xsl:template match="ogcwfs:FeatureType">
    <xsl:choose>
      <xsl:when test="$httpMethod='post'">
        <TypeName><xsl:value-of select="ogcwfs:Name"/></TypeName>
      </xsl:when>
      <xsl:otherwise>
        <QueryString>
          <xsl:variable name="query">
      request=DescribeFeatureType
 &amp;version=1.0.0
 &amp;service=WFS
&amp;typename=<xsl:value-of select="ogcwfs:Name"/>
          </xsl:variable>
          <xsl:value-of select="translate(normalize-space($query),' ', '' )" disable-output-escaping="no"/>
        </QueryString>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="text()|@*"/>

</xsl:stylesheet>
