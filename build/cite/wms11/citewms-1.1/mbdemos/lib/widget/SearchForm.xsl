<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

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
  <xsl:param name="formName">AOIForm</xsl:param>

  <!-- Main html -->
  <xsl:template match="/">
    <form name="{$formName}" id="{$formName}" method="get" action="defaultUrl">
      <table>
        <tr>
          <th align="left" colspan="3">
            <xsl:call-template name="title"/>
          </th>
        </tr>
        <tr>
          <td>
            keyword:
          </td>
          <td>
            <input name="keyword" type="text" size="10"/>
          </td>
          <td>
            <input type="submit"/>
          </td>
        </tr>
        <tr>
          <td>
          </td>
          <td>
            <xsl:call-template name="north"/>
            <input NAME="northCoord" TYPE="text" SIZE="10" STYLE="font: 8pt Verdana, geneva, arial, sans-serif;"/>
          </td>
          <td>
          </td>
        </tr>
        <tr>
          <td>
            <xsl:call-template name="west"/>
            <input NAME="westCoord" TYPE="text" SIZE="10" STYLE="font: 8pt Verdana, geneva, arial, sans-serif;"/>
          </td>
          <td>
          </td>
          <td>
            <xsl:call-template name="east"/>
            <input NAME="eastCoord" TYPE="text" SIZE="10" STYLE="font: 8pt Verdana, geneva, arial, sans-serif;"/>
          </td>
        </tr>
        <tr>
          <td>
          </td>
          <td>
            <xsl:call-template name="south"/>
            <input NAME="southCoord" TYPE="text" SIZE="10" STYLE="font: 8pt Verdana, geneva, arial, sans-serif;"/>
          </td>
          <td>
          </td>
        </tr>
      </table>
    </form>
  </xsl:template>
  
  <xsl:template name="title">
    <xsl:choose>
      <xsl:when test="$lang='fr'">Search for layers</xsl:when>
      <xsl:otherwise>Search for layers</xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xsl:template name="aoiTitle">
    <xsl:choose>
      <xsl:when test="$lang='fr'">Région d'intérêt</xsl:when>
      <xsl:otherwise>Area of interest coordinates</xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xsl:template name="north">
    <xsl:choose>
      <xsl:when test="$lang='fr'">Nord:</xsl:when>
      <xsl:otherwise>North:</xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xsl:template name="south">
    <xsl:choose>
      <xsl:when test="$lang='fr'">Sud:</xsl:when>
      <xsl:otherwise>South:</xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xsl:template name="east">
    <xsl:choose>
      <xsl:when test="$lang='fr'">Est:</xsl:when>
      <xsl:otherwise>East:</xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xsl:template name="west">
    <xsl:choose>
      <xsl:when test="$lang='fr'">Ouest:</xsl:when>
      <xsl:otherwise>West:</xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  
</xsl:stylesheet>
