<?xml version="1.0" encoding="ISO-8859-1"?>
<!--
Description: Render a WFS Transaction Response.
Author:      Cameron Shorter
Licence:     GPL as specified in http://www.gnu.org/copyleft/gpl.html .

$Id$
$Name:  $
-->

<xsl:stylesheet
  version="1.0"
  xmlns:ogc="http://www.opengis.net/ogc"
  xmlns:wfs="http://www.opengis.net/wfs"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:output method="xml"/>

  <!-- template rule matching source root element -->
  <xsl:template match="/">
    <div>
      <xsl:apply-templates/>
    </div>
  </xsl:template>

  <!-- Service Exception -->
  <xsl:template match="ogc:ServiceException">
    <table>
      <tr>
        <td>Service Exception:</td>
        <td><xsl:apply-templates/></td>
      </tr>
    </table>
  </xsl:template>

  <!-- Transaction Response -->
  <xsl:template match="wfs:WFS_TransactionResponse">
    <table>
      <tr>
        <td>Transaction Status:</td>
        <td><xsl:value-of select="name(wfs:TransactionResult/wfs:Status/*)"/></td>
      </tr>
      <tr>
        <td>New Feature Id:</td>
        <td><xsl:value-of select="wfs:InsertResult/ogc:FeatureId/@fid"/></td>
      </tr>
    </table>
  </xsl:template>
</xsl:stylesheet>
