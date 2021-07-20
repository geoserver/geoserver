<?xml version="1.0" encoding="ISO-8859-1"?>
<!--
Description: transforms a WMS Layer node into an XML structure that contains
             the URL for the GetMap request for both context docs and WMS caps.
             This one is not used yet, because WMS caps don't use a namespace,
             use GteMap.xsl instead.
Author:      adair
Licence:     GPL as specified in http://www.gnu.org/copyleft/gpl.html .

$Id$
$Name:  $
-->

<xsl:stylesheet version="1.0" 
    xmlns:wmc="http://www.opengis.net/context" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:xlink="http://www.w3.org/1999/xlink">

  <xsl:output method="xml"/>
  <xsl:strip-space elements="*"/>
  <!--
  <xsl:include href="ogcMapImgObjects.xsl" />
  -->

  <xsl:param name="bbox"/>
  <xsl:param name="width"/>
  <xsl:param name="height"/>
  <xsl:param name="srs"/>
  <xsl:param name="version"/>
  <xsl:param name="timeList"/>
  <xsl:param name="timeListName"/>
  
  <xsl:template match="Layer | wmc:Layer">
    
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
    <xsl:variable name="format">image/png</xsl:variable>
    <xsl:variable name="styleParam"/>
    <xsl:variable name="visibility"/>
    <xsl:variable name="mapRequest">
      <xsl:choose>
        <xsl:when test="starts-with($version, '1.0')">
            WMTVER=<xsl:value-of select="$version"/>&amp;REQUEST=map
        </xsl:when>            
        <xsl:otherwise>
            VERSION=<xsl:value-of select="$version"/>&amp;REQUEST=GetMap
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <GetMap>
      <QueryString>
        <xsl:variable name="src">    
            <xsl:value-of select="$mapRequest"/>
   &amp;SRS=<xsl:value-of select="$srs"/>
  &amp;BBOX=<xsl:value-of select="$bbox"/>
 &amp;WIDTH=<xsl:value-of select="$width"/>
&amp;HEIGHT=<xsl:value-of select="$height"/>
&amp;LAYERS=<xsl:value-of select="Name"/>
&amp;FORMAT=<xsl:value-of select="$format"/>
       &amp;<xsl:value-of select="$styleParam"/>
&amp;TRANSPARENT=true
        <xsl:if test="$timestamp">
       &amp;TIME=<xsl:value-of select="$timestamp"/>
        </xsl:if>
<!--	
  //TBD: these still to be properly handled 
  //also sld support
  if (this.transparent) src += '&' + 'TRANSPARENT=' + this.transparent;
	if (this.bgcolor) src += '&' + 'BGCOLOR=' + this.bgcolor;
	//if (this.exceptions) src += '&' + 'EXCEPTIONS=' + this.exceptions;
	if (this.vendorstr) src += '&' + this.vendorstr;
  // -->
        </xsl:variable>
        <xsl:value-of select="translate(normalize-space($src),' ', '' )" disable-output-escaping="no"/>
      </QueryString>
    </GetMap>
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

  <xsl:template match="wmc:Layer">
    <xsl:param name="version">
        <xsl:value-of select="wmc:Server/@version"/>    
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
    <xsl:variable name="visibility">
      <xsl:choose>
        <xsl:when test="@hidden='1'">hidden</xsl:when>
        <xsl:otherwise>visible</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="mapRequest">
      <xsl:choose>
        <xsl:when test="starts-with($version, '1.0')">
            WMTVER=<xsl:value-of select="$version"/>&amp;REQUEST=map
        </xsl:when>            
        <xsl:otherwise>
            VERSION=<xsl:value-of select="$version"/>&amp;REQUEST=GetMap
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <GetMap>
      <QueryString>
        <xsl:variable name="src">    
            <xsl:value-of select="$mapRequest"/>
   &amp;SRS=<xsl:value-of select="$srs"/>
  &amp;BBOX=<xsl:value-of select="$bbox"/>
 &amp;WIDTH=<xsl:value-of select="$width"/>
&amp;HEIGHT=<xsl:value-of select="$height"/>
&amp;LAYERS=<xsl:value-of select="wmc:Name"/>
&amp;FORMAT=<xsl:value-of select="$format"/>
       &amp;<xsl:value-of select="$styleParam"/>
&amp;TRANSPARENT=true
        <xsl:if test="$timestamp">
       &amp;TIME=<xsl:value-of select="$timestamp"/>
        </xsl:if>
<!--	
  //TBD: these still to be properly handled 
  //also sld support
  if (this.transparent) src += '&' + 'TRANSPARENT=' + this.transparent;
	if (this.bgcolor) src += '&' + 'BGCOLOR=' + this.bgcolor;
	//if (this.exceptions) src += '&' + 'EXCEPTIONS=' + this.exceptions;
	if (this.vendorstr) src += '&' + this.vendorstr;
  // -->
        </xsl:variable>
        <xsl:value-of select="translate(normalize-space($src),' ', '' )" disable-output-escaping="no"/>
      </QueryString>
    </GetMap>
  </xsl:template>

</xsl:stylesheet>
