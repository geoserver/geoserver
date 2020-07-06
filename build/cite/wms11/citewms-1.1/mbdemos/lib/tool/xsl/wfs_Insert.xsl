<?xml version="1.0" encoding="ISO-8859-1"?>

<!--
Description: Extract FeatureMembers from a FeatureCollection and build
  into a WFS Insert transaction.
Author:      Cameron Shorter
Licence:     GPL as specified in http://www.gnu.org/copyleft/gpl.html .

$Id$
$Name:  $
-->

<xsl:stylesheet version="1.0" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:wfs="http://www.opengis.net/wfs"
    xmlns:gml="http://www.opengis.net/gml"
    xmlns:xlink="http://www.w3.org/1999/xlink">

  <xsl:output method="xml" omit-xml-declaration="no" encoding="utf-8" indent="yes"/>

  <!-- Match root -->
  <xsl:template match="/">
    <wfs:Transaction>
      <wfs:Insert>
        <xsl:apply-templates/>
      </wfs:Insert>
    </wfs:Transaction>
  </xsl:template>

  <!-- Match featureMember -->
  <xsl:template match="gml:featureMember">
    <xsl:for-each select="./*">
      <xsl:copy-of select="."/>
    </xsl:for-each>
  </xsl:template>

  <xsl:template match="text()|@*"/>
</xsl:stylesheet>
