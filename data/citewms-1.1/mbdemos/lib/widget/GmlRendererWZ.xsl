<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:gml="http://www.opengis.net/gml"
  version="1.0">
<!--
Description:  Convert GML to wz_jsgraphics function calls.
Precondition: <coordinates> have been converted to <coords> (using
    GmlCoordinates2Coord.xsl).
Author:       Cameron Shorter cameron ATshorter.net
Licence:      GPL as per: http://www.gnu.org/copyleft/gpl.html

$Id$
$Name:  $
-->
  <xsl:output method="xml" encoding="utf-8"/>
  
  <xsl:param name="width" select="400"/>
  <xsl:param name="height" select="200"/>
  <xsl:param name="bBoxMinX" select="-180"/>
  <xsl:param name="bBoxMinY" select="-90"/>
  <xsl:param name="bBoxMaxX" select="180"/>
  <xsl:param name="bBoxMaxY" select="90"/>
  <xsl:param name="lineColor" select="'red'"/>
  <xsl:param name="lineWidth" select="1"/>
  <xsl:param name="crossSize" select="0"/>
  <xsl:param name="skinDir"/>
  <xsl:param name="pointWidth" select="5"/>

  <xsl:variable name="xRatio" select="$width div ( $bBoxMaxX - $bBoxMinX )"/>
  <xsl:variable name="yRatio" select="$height div ( $bBoxMaxY - $bBoxMinY )"/>

  <!-- Root node -->
  <xsl:template match="/">
    <js>
      if (!objRef.jg){
        objRef.jg=new jsGraphics(objRef.outputNodeId);
      }
      objRef.jg.clear();
      objRef.jg.setColor("<xsl:value-of select="$lineColor"/>");
      objRef.jg.setStroke(parseInt(<xsl:value-of select="$lineWidth"/>));
      <xsl:apply-templates/>
      objRef.jg.paint();
    </js>
  </xsl:template>

  <!-- Match and render a GML Point -->
  <xsl:template match="gml:pointMember/gml:Point">
    <xsl:variable name="x0" select="floor((number(gml:coord/gml:X)-$bBoxMinX)*$xRatio - number($pointWidth) div 2)"/>
    <xsl:variable name="y0" select="floor($height - (number(gml:coord/gml:Y)-$bBoxMinY)*$yRatio - $pointWidth div 2)"/>

    // Point
    objRef.jg.fillRect(<xsl:value-of select="$x0"/>,<xsl:value-of select="$y0"/>,<xsl:value-of select="$pointWidth"/>,<xsl:value-of select="$pointWidth"/>);
  </xsl:template>

  <!-- Match and render a GML Envelope -->
  <xsl:template match="gml:Envelope">
    <xsl:variable name="x0" select="floor((number(gml:coord[position()=1]/gml:X)-$bBoxMinX)*$xRatio)"/>
    <xsl:variable name="y0" select="floor($height - (number(gml:coord[position()=1]/gml:Y) -$bBoxMinY)*$yRatio)"/>
    <xsl:variable name="x1" select="floor((number(gml:coord[position()=2]/gml:X)-$bBoxMinX)*$xRatio)"/>
    <xsl:variable name="y1" select="floor($height - (number(gml:coord[position()=2]/gml:Y)-$bBoxMinY)*$yRatio)"/>

    <xsl:choose>
      <!-- If envelope is small, draw a cross instead of a box -->
      <xsl:when test="($x0 - $x1 &lt; $crossSize) and ($x1 - $x0 &lt; $crossSize) and ($y0 - $y1 &lt; $crossSize) and ($y1 - $y0 &lt; $crossSize)">
        <xsl:variable name="xMid" select="floor(($x0 + $x1) div 2)"/>
        <xsl:variable name="yMid" select="floor(($y0 + $y1) div 2)"/>
        <xsl:variable name="crossHalf" select="floor($crossSize div 2)"/>
        // Envelope - cross
        objRef.jg.drawLine(
          <xsl:value-of select="$xMid"/>,
          <xsl:value-of select="$yMid - $crossHalf"/>,
          <xsl:value-of select="$xMid"/>,
          <xsl:value-of select="$yMid + $crossHalf"/>);
        objRef.jg.drawLine(
          <xsl:value-of select="$xMid - $crossHalf"/>,
          <xsl:value-of select="$yMid"/>,
          <xsl:value-of select="$xMid + $crossHalf"/>,
          <xsl:value-of select="$yMid"/>);
      </xsl:when>

      <!-- draw a box -->
      <xsl:otherwise>
        // Envelope - box
        x0=<xsl:value-of select="$x0"/>;
        y0=<xsl:value-of select="$y0"/>;
        x1=<xsl:value-of select="$x1"/>;
        y1=<xsl:value-of select="$y1"/>;
        objRef.jg.drawRect(
          Math.min(x0,x1),
          Math.min(y0,y1),
          Math.abs(x1-x0),
          Math.abs(y1-y0));
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- Match and render a LineString -->
  <xsl:template match="gml:LineString | gml:LinearRing">
    <xsl:choose>
      <!-- Draw a line -->
      <xsl:when test="count(gml:coord)!=1">
        <xsl:for-each select="gml:coord">
          <xsl:if test="following-sibling::gml:coord">
            // LineString
            objRef.jg.drawLine(
              <xsl:value-of select="floor((number(gml:X)-$bBoxMinX)*$xRatio)"/>,
              <xsl:value-of select="floor($height - (number(gml:Y) -$bBoxMinY)*$yRatio)"/>,
              <xsl:value-of select="floor((number(following-sibling::gml:coord[position()=1]/gml:X)-$bBoxMinX)*$xRatio)"/>,
              <xsl:value-of select="floor($height - (number(following-sibling::gml:coord[position()=1]/gml:Y)-$bBoxMinY)*$yRatio)"/>);
          </xsl:if>
        </xsl:for-each>
      </xsl:when>

      <!-- When one coord, draw point -->
      <xsl:otherwise>
        <xsl:variable name="x0" select="floor((number(gml:coord/gml:X)-$bBoxMinX)*$xRatio - number($pointWidth) div 2)"/>
        <xsl:variable name="y0" select="floor($height - (number(gml:coord/gml:Y)-$bBoxMinY)*$yRatio - $pointWidth div 2)"/>

        // LineString - One point
        objRef.jg.fillRect(<xsl:value-of select="$x0"/>,<xsl:value-of select="$y0"/>,<xsl:value-of select="$pointWidth"/>,<xsl:value-of select="$pointWidth"/>);
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="text()|@*"/>
  
</xsl:stylesheet>
