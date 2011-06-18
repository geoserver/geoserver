<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:gml="http://www.opengis.net/gml" version="1.0">

<!--
Description: Convert a GML Feature or FeatureCollection into a HTML form.
Author:      Cameron Shorter cameron ATshorter.net
Licence:     GPL as per: http://www.gnu.org/copyleft/gpl.html
$Id$
$Name:  $
-->

  <xsl:output method="xml" encoding="utf-8"/>

  <!-- Common params for all widgets -->
  <!--xsl:param name="targetModelId"/-->
  <xsl:param name="modelId"/>
  <xsl:param name="widgetId"/>
  <!-- Parameters passed into this xsl -->
  <xsl:param name="space" select='""'/>
  <xsl:param name="tabwidth" select='"_ "'/>

  <!-- Main html -->
  <xsl:template match="/">
    <div>
      <h3>Feature List</h3>
      <form>
        <input type="button" value="Reset"
          onclick="config.objects.{$widgetId}.processButton(config.objects.{$widgetId},'Reset');"/>
        <input type="button" value="Insert Feature"
          onclick="config.objects.{$widgetId}.processButton(config.objects.{$widgetId},'Insert Feature');"/>
        <input type="button" value="Update Feature"
          onclick="config.objects.{$widgetId}.processButton(config.objects.{$widgetId},'Update Feature');"/>
        <table border="1" cellpadding="0" cellspacing="0">
          <xsl:apply-templates/>
        </table>
      </form>
    </div>
  </xsl:template>

  <!-- All nodes -->
  <xsl:template match="*">
    <xsl:param name="tab"/>
    <xsl:variable name="xlink">
      <xsl:call-template name="getXpath">
        <xsl:with-param name="node" select="."/>
      </xsl:call-template>
    </xsl:variable>
    <tr>
      <xsl:if test="text()">
        <td>
          <xsl:value-of select="$tab"/>
          <xsl:value-of select="name(.)"/>
        </td>
        <td>
          <input
            type="text"
            id="{$widgetId}{generate-id()}"
            size="40"
            value="{text()}"
            onchange="config.{$modelId}.setXpathValue(config.{$modelId},'{$xlink}',document.getElementById('{$widgetId}{generate-id()}').value);"/>
        </td>
        <xsl:apply-templates>
          <xsl:with-param name="tab">
            <xsl:value-of select="$tab"/>
            <xsl:value-of select="$tabwidth"/>
          </xsl:with-param>
        </xsl:apply-templates>
      </xsl:if>
      <xsl:if test="not(text())">
        <td>
          <xsl:value-of select="$tab"/>
          <b><xsl:value-of select="name(.)"/></b>
        </td>
        <td><xsl:value-of select="$space"/></td>
        <xsl:apply-templates>
          <xsl:with-param name="tab">
            <xsl:value-of select="$tab"/>
            <xsl:value-of select="$tabwidth"/>
          </xsl:with-param>
        </xsl:apply-templates>
      </xsl:if>
    </tr>
  </xsl:template>

  <!-- Return xpath reference to a node. Calls itself recursively -->
  <xsl:template name="getXpath">
    <xsl:param name="node"/>
    <xsl:if test="name($node/..)">
      <xsl:call-template name="getXpath">
        <xsl:with-param name="node" select="$node/.."/>
      </xsl:call-template>
    </xsl:if>
    <xsl:text>/</xsl:text>
    <xsl:value-of select="name($node)"/>
  </xsl:template>

  <!-- Remove documentation, text, comments -->
  <xsl:template match="comment()|text()|processing-instruction()">
  </xsl:template>
</xsl:stylesheet>
