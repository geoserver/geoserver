<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<!--
Description: Output a form for display of the cursor coordinates
Author:      Mike Adair
Licence:     GPL as per: http://www.gnu.org/copyleft/gpl.html

$Id$
-->

  <xsl:output method="xml" encoding="utf-8"/>

  <!-- The common params set for all widgets -->
  <xsl:param name="lang">en</xsl:param>
  
  <!-- text value params -->
  <xsl:param name="longitude">lon:</xsl:param>
  <xsl:param name="latitude">lat:</xsl:param>
  
  <!-- The name of the form for coordinate output -->
  <xsl:param name="formName"/>

  <!-- Main html -->
  <xsl:template match="/">
    <DIV>
    <FORM NAME="{$formName}" ID="{$formName}" STYLE="font: 8pt Verdana, geneva, arial, sans-serif;">
      <xsl:value-of select="$longitude"/> <input NAME="longitude" TYPE="text" SIZE="6" READONLY="true" STYLE="border: 0px blue none; font: 8pt Verdana, geneva, arial, sans-serif;"/>
      <xsl:value-of select="$latitude"/> <input NAME="latitude" TYPE="text" SIZE="6" READONLY="true" STYLE="border: 0px blue none; font: 8pt Verdana, geneva, arial, sans-serif;"/>
    </FORM>
    </DIV>
  </xsl:template>

</xsl:stylesheet>
