<?xml version="1.0" encoding="ISO-8859-1"?>
<!--
Description: parses an OGC context document to generate an array of DHTML layers.
Author:      adair
Licence:     GPL as specified in http://www.gnu.org/copyleft/gpl.html .

$Id$
$Name:  $
-->

<xsl:stylesheet version="1.0" xmlns:wmc="http://www.opengis.net/context" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xlink="http://www.w3.org/1999/xlink">

  <xsl:output method="xml"/>
  <xsl:strip-space elements="*"/>
  <!--
  <xsl:include href="ogcMapImgObjects.xsl" />
  -->

  <!-- The coordinates of the DHTML Layer on the HTML page -->
  <xsl:param name="modelId"/>
  <xsl:param name="widgetId"/>
  <xsl:param name="context">config['<xsl:value-of select="$modelId"/>']</xsl:param>

  <xsl:param name="bbox">
    <xsl:value-of select="/wmc:ViewContext/wmc:General/wmc:BoundingBox/@minx"/>,<xsl:value-of select="/wmc:ViewContext/wmc:General/wmc:BoundingBox/@miny"/>,
    <xsl:value-of select="/wmc:ViewContext/wmc:General/wmc:BoundingBox/@maxx"/>,<xsl:value-of select="/wmc:ViewContext/wmc:General/wmc:BoundingBox/@maxy"/>
  </xsl:param>
  <xsl:param name="width">
    <xsl:value-of select="/wmc:ViewContext/wmc:General/wmc:Window/@width"/>
  </xsl:param>
  <xsl:param name="height">
    <xsl:value-of select="/wmc:ViewContext/wmc:General/wmc:Window/@height"/>
  </xsl:param>
  <xsl:param name="srs" select="/wmc:ViewContext/wmc:General/wmc:BoundingBox/@SRS"/>
  <xsl:param name="timeList"/>
  <xsl:param name="timeListName"/>
  
  <!-- template rule matching source root element -->
  <xsl:template match="/wmc:ViewContext">
      <DIV STYLE="position:absolute; width:{$width}; height:{$height}">
        <xsl:apply-templates select="wmc:LayerList/wmc:Layer"/>
        <xsl:apply-templates select="wmc:ResourceList/*"/>
      </DIV>
  </xsl:template>
  
  <xsl:template match="wmc:FeatureType"/>
  <xsl:template match="wmc:Layer">
    
    <xsl:choose>
      <xsl:when test="$timeList and wmc:DimensionList/wmc:Dimension[@name='time']">
          <xsl:call-template name="tokenize">
            <xsl:with-param name="str" select="$timeList"/>
            <xsl:with-param name="sep" select="','"/>
          </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="layerOutput"/>
      </xsl:otherwise>
    </xsl:choose>
    
  </xsl:template>

  <xsl:template name="layerOutput">
    <xsl:param name="version">
        <xsl:value-of select="wmc:Server/@version"/>    
    </xsl:param>
    <xsl:param name="baseUrl">
        <xsl:value-of select="wmc:Server/wmc:OnlineResource/@xlink:href"/>    
    </xsl:param>
    <xsl:param name="timestamp">
        <xsl:value-of select="wmc:DimensionList/wmc:Dimension[@name='time']/@default"/>    
    </xsl:param>
    <xsl:param name="visibility">
      <xsl:choose>
        <xsl:when test="@hidden='1'">hidden</xsl:when>
        <xsl:otherwise>visible</xsl:otherwise>
      </xsl:choose>
    </xsl:param>
    <xsl:variable name="format">
      <xsl:choose>
        <xsl:when test="wmc:FormatList"><xsl:value-of select="wmc:FormatList/wmc:Format[@current='1']"/></xsl:when>
        <xsl:otherwise>image/gif</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="styleParam">
      <xsl:choose>
        <xsl:when test="wmc:StyleList/wmc:Style[@current='1']/wmc:SLD">
          SLD=<xsl:value-of select="wmc:StyleList/wmc:Style[@current='1']/wmc:SLD/wmc:OnlineResource/@xlink:href"/>
        </xsl:when>
        <xsl:when test="wmc:StyleList/wmc:Style[@current='1']/wmc:SLD/wmc:StyeLayerDescriptor">
          SLD=<xsl:value-of select="wmc:StyleList/wmc:Style[@current='1']/wmc:SLD/wmc:StyeLayerDescriptor"/>
        </xsl:when>
        <xsl:when test="wmc:StyleList/wmc:Style[@current='1']/wmc:SLD/wmc:FeatureTypeStyle">
          SLD=<xsl:value-of select="wmc:StyleList/wmc:Style[@current='1']/wmc:SLD/wmc:FeatureTypeStyle"/>
        </xsl:when>
        <xsl:otherwise>
          STYLES=<xsl:value-of select="wmc:StyleList/wmc:Style[@current='1']/wmc:Name"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="firstJoin">
      <xsl:choose>
        <xsl:when test="substring($baseUrl,string-length($baseUrl))='?'"></xsl:when>
        <xsl:when test="contains($baseUrl, '?')">&amp;</xsl:when> 
        <xsl:otherwise>?</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="mapRequest">
      <xsl:choose>
        <xsl:when test="starts-with($version, '1.0')">
            WMTVER=<xsl:value-of select="$version"/>&amp;REQUEST=map
        </xsl:when>            
        <xsl:otherwise>
            VERSION=<xsl:value-of select="$version"/>&amp;REQUEST=GetMap&amp;SERVICE=WMS
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <DIV>    
      <xsl:attribute name="STYLE">position:absolute; visibility:<xsl:value-of select="$visibility"/>; top:0; left:0;</xsl:attribute>
      <xsl:attribute name="ID">
        <xsl:value-of select="$modelId"/>_<xsl:value-of select="$widgetId"/>_<xsl:value-of select="wmc:Name"/><xsl:if test="$timestamp and wmc:DimensionList/wmc:Dimension[@name='time']">_<xsl:value-of select="$timestamp"/></xsl:if>
      </xsl:attribute>
      <xsl:if test="$timestamp and wmc:DimensionList/wmc:Dimension[@name='time']">
        <xsl:attribute name="TIME"><xsl:value-of select="$timestamp"/></xsl:attribute>
      </xsl:if>
    
    <xsl:element name="IMG">    
        <xsl:variable name="src">    
            <xsl:value-of select="$baseUrl"/>
            <xsl:value-of select="$firstJoin"/>
            <xsl:value-of select="$mapRequest"/>
   &amp;SRS=<xsl:value-of select="$srs"/>
  &amp;BBOX=<xsl:value-of select="$bbox"/>
 &amp;WIDTH=<xsl:value-of select="$width"/>
&amp;HEIGHT=<xsl:value-of select="$height"/>
&amp;LAYERS=<xsl:value-of select="wmc:Name"/>
&amp;FORMAT=<xsl:value-of select="$format"/>
       &amp;<xsl:value-of select="$styleParam"/>
&amp;TRANSPARENT=TRUE
        <xsl:if test="$timestamp">
       &amp;TIME=<xsl:value-of select="$timestamp"/>
        </xsl:if>
<!--	
  //TBD: these still to be properly handled 
  //if (this.exceptions) src += '&' + 'EXCEPTIONS=' + this.exceptions;
  //if (this.vendorstr) src += '&' + this.vendorstr;
  // -->
        </xsl:variable>
        <xsl:attribute name="SRC">    
            <xsl:value-of select="translate(normalize-space($src),' ', '' )" disable-output-escaping="no"/>
        </xsl:attribute>
        <xsl:attribute name="WIDTH">
            <xsl:value-of select="$width"/>
        </xsl:attribute>
        <xsl:attribute name="HEIGHT">
            <xsl:value-of select="$height"/>
        </xsl:attribute>
        <xsl:attribute name="ALT">
            <xsl:value-of select="wmc:Title"/>
        </xsl:attribute>
    </xsl:element>    
    </DIV>    
  </xsl:template>

  
<xsl:template name="tokenize"> <!-- tokenize a string -->
 <xsl:param name="str"/> <!-- String to process -->
 <xsl:param name="sep"/> <!-- Legal separator character -->
 <xsl:choose>
  <xsl:when test="contains($str,$sep)"> <!-- Only tokenize if there is a separator present in the string -->
    <xsl:call-template name="process-token"> <!-- Process the token before the separator -->
      <xsl:with-param name="token" select="substring-before($str,$sep)"/>
    </xsl:call-template>
    <xsl:call-template name="tokenize">  <!-- Re-tokenize the new string which is contained after the separator -->
      <xsl:with-param name="str" select="substring-after($str,$sep)"/>
      <xsl:with-param name="sep" select="$sep"/> <!-- carriage return -->
    </xsl:call-template>
  </xsl:when>
  <xsl:otherwise>  <!-- If there is nothing else to tokenize, just treat the last part of the str as a regular token -->
    <xsl:call-template name="process-token">
      <xsl:with-param name="token" select="$str"/>
    </xsl:call-template>
  </xsl:otherwise>
 </xsl:choose>
</xsl:template>

<xsl:template name="process-token">  <!-- process - separate with <br> -->
  <xsl:param name="token"/> <!-- token to process -->
  <xsl:call-template name="layerOutput">
    <xsl:with-param name="timestamp" select="$token"/>
    <xsl:with-param name="visibility">hidden</xsl:with-param>
  </xsl:call-template>
</xsl:template>
  
</xsl:stylesheet>
