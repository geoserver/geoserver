<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:wmc="http://www.opengis.net/context" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xlink="http://www.w3.org/1999/xlink" >

<!--
Description: Output a form for display of the context doc AOI
Author:      Mike Adair
Licence:     GPL as per: http://www.gnu.org/copyleft/gpl.html

$Id$
-->

  <xsl:output method="xml" encoding="utf-8"/>

  <!-- The common params set for all widgets -->
  <xsl:param name="modelId"/>
  <xsl:param name="widgetId"/>

  <xsl:param name="rootElement">ContactInformation</xsl:param>
  <xsl:param name="nsTypeName"><xsl:value-of select="//xs:element[@name=$rootElement]/@type"/></xsl:param>
  
  <!-- The name of the form for coordinate output -->
  <xsl:param name="formName">XmlForm</xsl:param>
  <xsl:param name="xmlDoc" select="document('/mapbuilder/demo/context/atlasWorld.xml')"/>  <!-- Element tag names. -->

  <!-- Main html -->
	<xsl:template match="/">
    <xsl:variable name="typeName"><xsl:value-of select="substring-after($nsTypeName,':')"/></xsl:variable>
    id:<xsl:value-of select="$xmlDoc/ViewContext/@id"/>
    <DIV>
    <FORM NAME="{$formName}" ID="{$formName}">
      <xsl:apply-templates select="xs:schema/xs:complexType[@name=$typeName]"> 
        <xsl:with-param name="currentDocNode" select="$xmlDoc/ViewContext/General/ContactInformation"/>
      </xsl:apply-templates>
    </FORM>
    </DIV>
  </xsl:template>
	
	<xsl:template match="xs:element">
    <xsl:param name="currentDocNode"/>
    <xsl:choose>
      <xsl:when test="@type='xs:string'">
        <tr><td>
          <xsl:value-of select="@name"/>
        </td><td>
          <xsl:variable name="docValue"><xsl:value-of select="$currentDocNode/@name"/></xsl:variable>
          <xsl:element name="INPUT">
            <xsl:attribute name="ID">elId</xsl:attribute>
            <xsl:attribute name="TYPE">text</xsl:attribute>
            <xsl:attribute name="VALUE"><xsl:value-of select="$docValue"/></xsl:attribute>
          </xsl:element>
          <xsl:apply-templates> 
            <xsl:with-param name="currentDocNode" select="$currentDocNode/@name"/>
          </xsl:apply-templates>
        </td></tr>
      </xsl:when>
      <xsl:otherwise>
        <xsl:variable name="complexTypeName"><xsl:value-of select="substring-after(@type,':')"/></xsl:variable>
        <tr><td colspan="2">
          <xsl:value-of select="@name"/>
          <xsl:apply-templates select="ancestor::*/xs:complexType[@name=$complexTypeName]"> 
            <xsl:with-param name="currentDocNode" select="$currentDocNode/@name"/>
          </xsl:apply-templates>
        </td></tr>
      </xsl:otherwise>
    </xsl:choose>
	</xsl:template>
  
	<xsl:template match="xs:complexType">
    <xsl:param name="currentDocNode"/>
    <dl>
      <dd>
        <xsl:apply-templates> 
          <xsl:with-param name="currentDocNode" select="$currentDocNode"/>
        </xsl:apply-templates>
      </dd>
    </dl>
	</xsl:template>
  
	<xsl:template match="xs:sequence">
    <xsl:param name="currentDocNode"/>
    <table border="1">
      <xsl:apply-templates> 
        <xsl:with-param name="currentDocNode" select="$currentDocNode"/>
      </xsl:apply-templates>
    </table>
	</xsl:template>
  
	<xsl:template match="text()|@*"/>

</xsl:stylesheet>
