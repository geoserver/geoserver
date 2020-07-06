<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" 
    xmlns:mb="http://mapbuilder.sourceforge.net/mapbuilder" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<!--
Description: Output a form for setting any model's url and method values
Author:      Mike Adair
Licence:     GPL as per: http://www.gnu.org/copyleft/gpl.html

$Id$
-->

  <xsl:output method="xml" encoding="utf-8"/>

  <!-- The common params set for all widgets -->
  <xsl:param name="lang">en</xsl:param>
  <xsl:param name="modelId"/>
  <xsl:param name="modelTitle"/>
  <xsl:param name="targetModel"/>
  <xsl:param name="widgetId"/>

  <!-- The name of the form for coordinate output -->
  <xsl:param name="modelUrl"/>
  <xsl:param name="formName">ModelUrlInputForm</xsl:param>

  <!-- Main html -->
  <xsl:template match="/">
    <DIV>
    <form name="{$formName}" id="{$formName}" onsubmit="return config.objects.{$widgetId}.submitForm()">
    
      <table>
        <tr>
          <th align="left" colspan="3">
            <xsl:call-template name="title"/><xsl:value-of select="$modelTitle"/>
          </th>
        </tr>
        <tr>
          <td>
            URL:
          </td>
          <td colspan="2">
            <input name="modelUrl" type="text" size="30" value="{$modelUrl}"/>
            <a href="javascript:config.objects.{$widgetId}.submitForm();">load</a>
          </td>
        </tr>
        <!--tr>
          <td>
            method:
          </td>
          <td>
            get <input name="httpMethod" type="radio" value="GET" checked="true"/> - 
            post <input name="httpMethod" type="radio" value="POST"/>
          </td>
          <td>
            <input type="submit"/>
            <a href="javascript:config.objects.{$widgetId}.submitForm();">load model</a>
          </td>
        </tr-->
      </table>
    </form>
    </DIV>
  </xsl:template>
  
  <xsl:template name="title">
    <xsl:choose>
      <xsl:when test="$lang='fr'">Model URL for:</xsl:when>
      <xsl:otherwise>Enter a URL for: </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  
</xsl:stylesheet>
