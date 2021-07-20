<?xml version="1.0" encoding="ISO-8859-1"?>

<!--
Description: parses an service registry listing of OGC servers
Author:      adair
Licence:     GPL as specified in http://www.gnu.org/copyleft/gpl.html .

$Id$
$Name:  $
-->

<xsl:stylesheet version="1.0" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
		xmlns:ogc="http://www.opengis.net/ogc"
		xmlns:gml="http://www.opengis.net/gml"
    xmlns:xlink="http://www.w3.org/1999/xlink">

  <xsl:output method="xml" omit-xml-declaration="no" encoding="utf-8" indent="yes"/>

  <!-- The coordinates of the DHTML Layer on the HTML page -->
  <xsl:param name="modelTitle"/>
  <xsl:param name="modelId"/>
  <xsl:param name="widgetId"/>
  <xsl:param name="targetModelId"/>
  <xsl:param name="targetModel2"/>
  
  <xsl:param name="webServiceUrl">http://geodiscover.cgdi.ca/ceonetWeb/biz</xsl:param>
  <xsl:param name="formName">OGCServiceList</xsl:param>
  
  <!-- template rule matching source root element -->
  <xsl:template match="/searchDetails">
    <xsl:variable name="numPerPage"><xsl:value-of select="searchCriteria/numResultsPerPage"/></xsl:variable>
    <xsl:variable name="pageNum"><xsl:value-of select="paging/currentPage"/></xsl:variable>
    <xsl:variable name="nextPage"><xsl:value-of select="paging/nextPage"/></xsl:variable>
    <xsl:variable name="prevPage"><xsl:value-of select="paging/prevPage"/></xsl:variable>
    <form name="{$formName}" id="{$formName}" method="get" action="{$webServiceUrl}">
      <input type="hidden" name="language" value="en"/>
      <input type="hidden" name="levelOfDetail" value="brief"/>
      <input type="hidden" name="serviceType" value="CgdiMapServices"/>
      <input type="hidden" name="request" value="searchForService"/>
      <input type="hidden" name="numResultsPerPage" value="{$numPerPage}"/>
      <input type="hidden" name="page" value="{$pageNum}"/>
      <input type="hidden" name="sortOrder" value="default"/>
      keyword filter:<input type="text" name="keywords"/>
      <a href="javascript:config.objects.{$widgetId}.webServiceForm.page.value={$nextPage};config.objects.{$widgetId}.submitForm()">next</a>
    <dl>
      <xsl:apply-templates select="searchResults/entry"/>
    </dl>
    </form>
  </xsl:template>

  <!-- template rule matching source root element -->
  <xsl:template match="entry">
    <xsl:variable name="capsUrl"><xsl:value-of select="accessUrl"/></xsl:variable>
    <dt>
      <a href="javascript:config.loadModel('{$targetModel2}','{$capsUrl}')">
        <xsl:value-of select="name"/>
      </a>
    </dt>
    <dd><xsl:value-of select="custodianName"/></dd>
  </xsl:template>

  <xsl:template match="text()|@*"/>

</xsl:stylesheet>
