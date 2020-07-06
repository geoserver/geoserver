<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
xmlns:wmc="http://www.opengis.net/context"
xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<!--
Description: Convert a Web Map Context into a HTML Legend
Author:      Cameron Shorter cameron ATshorter.net
Licence:     GPL as per: http://www.gnu.org/copyleft/gpl.html

$Id$
$Name:  $
-->

  <xsl:output method="xml" encoding="utf-8"/>

  <xsl:param name="lang">en</xsl:param>

  <!-- Main html -->
  <xsl:template match="/wmc:ViewContext/wmc:General | /wmc:OWSContext/wmc:General ">
    <span>
      <xsl:choose>
        <xsl:when test="wmc:Title/@xml:lang">              
          <xsl:value-of select="wmc:Title[@xml:lang=$lang]"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="wmc:Title"/>
        </xsl:otherwise>
      </xsl:choose>
    </span>
  </xsl:template>
  
  <xsl:template match="text()|@*"/>

</xsl:stylesheet>

