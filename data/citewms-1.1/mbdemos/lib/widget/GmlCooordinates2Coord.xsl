<?xml version="1.0" encoding="ISO-8859-1"?>

<!--
Description: Convert <coordinates> to <coord x> <coord y>
Author:      Cameron Shorter cameron ATshorterDOTnet
Licence:     GPL as specified in http://www.gnu.org/copyleft/gpl.html .

$Id$
$Name:  $
-->

<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:gml="http://www.opengis.net/gml"
  version="1.0">

  <!-- Coordinates template -->
  <xsl:template match="gml:coordinates">

    <!-- Convert decimal, coord symbol, tuple symbol to defaults -->
    <xsl:variable name="str" select="translate(.,@decimal,'.')"/>
    <xsl:variable name="str2" select="translate($str,@cs,',')"/>
    <xsl:variable name="str3" select="translate($str2,@ts,' ')"/>
    <xsl:if test="string-length(normalize-space($str3))!=0">
      <xsl:call-template name="parseTuples">
        <xsl:with-param name="str" select="normalize-space($str3)"/>
      </xsl:call-template>
    </xsl:if>
  </xsl:template>

  <!-- Print one tuple, then recursively call this template to print remaining tuples -->
  <xsl:template name="parseTuples">
    <xsl:param name="str"/>
    <xsl:param name="cs" select="','"/> <!--symbol to separate coords-->
    <xsl:param name="ts" select="' '"/> <!-- symbol to separate tuples-->
    <xsl:choose>
      <xsl:when test="not(contains($str,$ts))">
        <xsl:call-template name="parseCoords">
          <xsl:with-param name="str" select="$str"/>
          <xsl:with-param name="cs" select="$cs"/>
          <xsl:with-param name="ts" select="$ts"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="parseCoords">
          <xsl:with-param name="str" select="substring-before($str,$ts)"/>
          <xsl:with-param name="cs" select="$cs"/>
          <xsl:with-param name="ts" select="$ts"/>
        </xsl:call-template>
        <xsl:call-template name="parseTuples">
          <xsl:with-param name="str" select="substring-after($str,$ts)"/>
          <xsl:with-param name="cs" select="$cs"/>
          <xsl:with-param name="ts" select="$ts"/>
        </xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- Extract coords out of a tuple -->
  <xsl:template name="parseCoords">
    <xsl:param name="str"/>
    <xsl:param name="cs"/>

    <gml:coord>
      <!-- X coord -->
      <xsl:choose>
        <xsl:when test="not(contains($str,$cs))">
          <xsl:call-template name="printCoord">
            <xsl:with-param name="coord" select="'gml:X'"/>
            <xsl:with-param name="value" select="$str"/>
          </xsl:call-template>
        </xsl:when>
        <xsl:otherwise>
          <xsl:call-template name="printCoord">
            <xsl:with-param name="coord" select="'gml:X'"/>
            <xsl:with-param name="value" select="substring-before($str,$cs)"/>
          </xsl:call-template>
          <!-- Y coord -->
          <xsl:variable name="yz" select="substring-after($str,$cs)"/>
          <xsl:choose>
            <xsl:when test="not(contains($yz,$cs))">
              <xsl:call-template name="printCoord">
                <xsl:with-param name="coord" select="'gml:Y'"/>
                <xsl:with-param name="value" select="$yz"/>
              </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
              <xsl:call-template name="printCoord">
                <xsl:with-param name="coord" select="'gml:Y'"/>
                <xsl:with-param name="value" select="substring-before($yz,$cs)"/>
              </xsl:call-template>
              <!-- Z coord -->
              <xsl:call-template name="printCoord">
                <xsl:with-param name="coord" select="'gml:Z'"/>
                <xsl:with-param name="value" select="substring-after($yz,$cs)"/>
              </xsl:call-template>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:otherwise>
      </xsl:choose>
    </gml:coord>
  </xsl:template>

  <!-- Print X, Y or Z coord: <X>123</X> -->
  <xsl:template name="printCoord">
    <xsl:param name="coord"/>
    <xsl:param name="value"/>
    <xsl:element name="{$coord}">
      <xsl:value-of select="$value"/>
    </xsl:element>
  </xsl:template>

  <!-- All other nodes are copied -->
  <xsl:template match="*|@*|comment()|processing-instruction()|text()">
    <xsl:copy>
      <xsl:apply-templates
       select="*|@*|comment()|processing-instruction()|text()"/>
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>
