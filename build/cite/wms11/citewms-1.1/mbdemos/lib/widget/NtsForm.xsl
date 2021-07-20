<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" 
    xmlns:mb="http://mapbuilder.sourceforge.net/mapbuilder" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<!--
Description: Output a form for display of the context doc AOI
Author:      Mike Adair
Licence:     GPL as per: http://www.gnu.org/copyleft/gpl.html

$Id$
-->

  <xsl:output method="xml" encoding="utf-8"/>

  <!-- The common params set for all widgets -->
  <xsl:param name="lang">en</xsl:param>
  <xsl:param name="modelId"/>
  <xsl:param name="widgetId"/>

  <!-- The name of the form for coordinate output -->
  <xsl:param name="webServiceUrl">http://geoservices.cgdi.ca/NTS/NTSLookup</xsl:param>
  <xsl:param name="formName">NTSForm</xsl:param>

  <!-- Main html -->
  <xsl:template match="/">
    <DIV>
    <form name="{$formName}" id="{$formName}" method="get" action="{$webServiceUrl}">
      <input name="request" type="hidden" value="GetMapsheet"/>
      <input name="version" type="hidden" value="1.1.2"/>
    
      <table>
        <tr>
          <th align="left" colspan="3">
            <xsl:call-template name="title"/>
          </th>
        </tr>
        <tr>
          <td>
            <xsl:call-template name="mapsheet"/>
          </td>
          <td>
            <input name="mapsheet" type="text" size="10" value="31G05"/>
          </td>
          <td>
            <!--input type="submit"/-->
            <a href="javascript:config.objects.{$widgetId}.submitForm();">load web service data</a>
          </td>
        </tr>
      </table>
    </form>
    </DIV>
  </xsl:template>
  
  <xsl:template name="title">
    <xsl:choose>
      <xsl:when test="$lang='fr'">NTS Lookup web service</xsl:when>
      <xsl:otherwise>NTS Lookup web service</xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xsl:template name="mapsheet">
    <xsl:choose>
      <xsl:when test="$lang='fr'">NTS index:</xsl:when>
      <xsl:otherwise>NTS index:</xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  
</xsl:stylesheet>
