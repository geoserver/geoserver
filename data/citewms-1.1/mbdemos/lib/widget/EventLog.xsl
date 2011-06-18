<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
xmlns:mb="http://mapbuilder.sourceforge.net/mapbuilder"
xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<!--
Description: Show an event log, used for debugging.
Author:      Mike Adair
Licence:     GPL as per: http://www.gnu.org/copyleft/gpl.html

$Id$
$Name:  $
-->

  <xsl:output method="xml" encoding="utf-8"/>

  <xsl:param name="listenerFilter"></xsl:param>
  <xsl:param name="targetFilter"></xsl:param>

  <!-- Main html -->
  <xsl:template match="/mb:Logger">
    <div style="height:300px;overflow:scroll">
    <table border="1">
      <tr>
        <td colspan="2">
          Event Log
        </td>
        <td>
          <a href="javascript:window.logger.clearLog();">Clear</a> - 
          <a href="javascript:window.logger.callListeners('refresh');">Refresh</a> - 
          <a href="javascript:window.logger.saveLog();">Save</a>
        </td>
      </tr>
      <tr>
        <th>
          Event
        </th>
        <th>
          Listener
        </th>
        <th>
          Target
        </th>
      </tr>
      <xsl:choose>
        <xsl:when test="string-length($listenerFilter)>0">              
          <xsl:choose>
            <xsl:when test="string-length($targetFilter)>0">              
              <xsl:apply-templates select="event[@listener=$listenerFilter and @target=$targetFilter]"/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:apply-templates select="event[@listener=$listenerFilter]"/>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:when>
        <xsl:otherwise>
          <xsl:apply-templates select="event"/>
        </xsl:otherwise>
      </xsl:choose>
    </table>
    </div>
  </xsl:template>
  
  <xsl:template match="event">
    <tr>
      <td>
        <xsl:value-of select="."/>
      </td>
      <td>
        <xsl:value-of select="@listener"/>
      </td>
      <td>
        <xsl:value-of select="@target"/>
      </td>
    </tr>
  </xsl:template>

</xsl:stylesheet>

