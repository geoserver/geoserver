<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:gml="http://www.opengis.net/gml"
  version="1.0">
<!--
Description: Convert GML to HTML Graphics. 
Author:      Cameron Shorter cameron ATshorter.net
Licence:     GPL as per: http://www.gnu.org/copyleft/gpl.html

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
  <xsl:param name="color" select="red"/>
  <xsl:param name="lineWidth" select="1"/>
  <xsl:param name="crossSize" select="0"/>
  <xsl:param name="skinDir"/>
  <xsl:param name="pointDiameter" select="10"/>

  <xsl:variable name="xRatio" select="$width div ( $bBoxMaxX - $bBoxMinX )"/>
  <xsl:variable name="yRatio" select="$height div ( $bBoxMaxY - $bBoxMinY )"/>


  <!-- Root node -->
  <xsl:template match="/">
    <div style="position:relative; width:{$width}; height:{$height}">
      <xsl:apply-templates/>
    </div>
  </xsl:template>

  <!-- Match and render a GML Point -->
  <xsl:template match="gml:pointMember/gml:Point">
    <xsl:variable name="x0" select="floor((number(gml:coord/gml:X)-$bBoxMinX)*$xRatio - number($pointDiameter) div 2)"/>
    <xsl:variable name="y0" select="floor($height - (number(gml:coord/gml:Y)-$bBoxMinY)*$yRatio - $pointDiameter div 2)"/>

    <div style="position:absolute; left:{$x0}px; top:{$y0}px; width:{$pointDiameter}px; height:{$pointDiameter}px">
      <img src="{$skinDir}/images/Dot.gif"/>
    </div>
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
        <debug drawCross="x"/>
        <xsl:variable name="xMid" select="floor(($x0 + $x1) div 2)"/>
        <xsl:variable name="yMid" select="floor(($y0 + $y1) div 2)"/>
        <xsl:variable name="crossHalf" select="floor($crossSize div 2)"/>
        <xsl:call-template name="drawLine">
          <xsl:with-param name="x0" select="$xMid"/>
          <xsl:with-param name="y0" select="$yMid - $crossHalf"/>
          <xsl:with-param name="x1" select="$xMid"/>
          <xsl:with-param name="y1" select="$yMid + $crossHalf"/>
        </xsl:call-template>
        <xsl:call-template name="drawLine">
          <xsl:with-param name="x0" select="$xMid - $crossHalf"/>
          <xsl:with-param name="y0" select="$yMid"/>
          <xsl:with-param name="x1" select="$xMid + $crossHalf"/>
          <xsl:with-param name="y1" select="$yMid"/>
        </xsl:call-template>
      </xsl:when>

      <!-- draw a box -->
      <xsl:otherwise>
        <debug drawBox="x"/>
        <xsl:call-template name="drawLine">
          <xsl:with-param name="x0" select="$x0"/>
          <xsl:with-param name="y0" select="$y0"/>
          <xsl:with-param name="x1" select="$x1"/>
          <xsl:with-param name="y1" select="$y0"/>
        </xsl:call-template>
        <xsl:call-template name="drawLine">
          <xsl:with-param name="x0" select="$x1"/>
          <xsl:with-param name="y0" select="$y0"/>
          <xsl:with-param name="x1" select="$x1"/>
          <xsl:with-param name="y1" select="$y1"/>
        </xsl:call-template>
        <xsl:call-template name="drawLine">
          <xsl:with-param name="x0" select="$x1"/>
          <xsl:with-param name="y0" select="$y1"/>
          <xsl:with-param name="x1" select="$x0"/>
          <xsl:with-param name="y1" select="$y1"/>
        </xsl:call-template>
        <xsl:call-template name="drawLine">
          <xsl:with-param name="x0" select="$x0"/>
          <xsl:with-param name="y0" select="$y1"/>
          <xsl:with-param name="x1" select="$x0"/>
          <xsl:with-param name="y1" select="$y0"/>
        </xsl:call-template>

      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- Match and render a LineString -->
  <xsl:template match="gml:LineString">
    <xsl:for-each select="gml:coord">
      <debug lineString="x"
        x0="{floor((number(gml:X)-$bBoxMinX)*$xRatio)}"
        y0="{floor($height - (number(gml:Y) -$bBoxMinY)*$yRatio)}"
        x1="{floor((number(following-sibling::gml:coord[position()=1]/gml:X)-$bBoxMinX)*$xRatio)}"
        y1="{floor($height - (number(following-sibling::gml:coord[position()=1]/gml:Y)-$bBoxMinY)*$yRatio)}"/>
      <xsl:if test="following-sibling::gml:coord">
        <xsl:call-template name="drawLine">
          <xsl:with-param name="x0" select="floor((number(gml:X)-$bBoxMinX)*$xRatio)"/>
          <xsl:with-param name="y0" select="floor($height - (number(gml:Y) -$bBoxMinY)*$yRatio)"/>
          <xsl:with-param name="x1" select="floor((number(following-sibling::gml:coord[position()=1]/gml:X)-$bBoxMinX)*$xRatio)"/>
          <xsl:with-param name="y1" select="floor($height - (number(following-sibling::gml:coord[position()=1]/gml:Y)-$bBoxMinY)*$yRatio)"/>
        </xsl:call-template>
      </xsl:if>
    </xsl:for-each>
  </xsl:template>

  <!-- Draw a line.  -->
  <xsl:template name="drawLine">
    <xsl:param name="x0"/>
    <xsl:param name="y0"/>
    <xsl:param name="x1"/>
    <xsl:param name="y1"/>
    <xsl:variable name="slope">
      <xsl:choose>
        <xsl:when test="$x0 = $x1">0</xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="($y1 - $y0) div ($x1 - $x0)"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <debug select="drawLine{$x0},{$y0},{$x1},{$y1} slope={$slope}"/>
    <xsl:choose>
      <xsl:when test="$x0 = $x1">
        <xsl:call-template name="fillBox">
          <xsl:with-param name="x0" select="$x0 - floor($lineWidth div 2)"/>
          <xsl:with-param name="y0" select="$y0"/>
          <xsl:with-param name="x1" select="$x0 + floor($lineWidth div 2)"/>
          <xsl:with-param name="y1" select="$y1"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:when test="$y0 = $y1">
        <xsl:call-template name="fillBox">
          <xsl:with-param name="x0" select="$x0"/>
          <xsl:with-param name="y0" select="$y0 - floor($lineWidth div 2)"/>
          <xsl:with-param name="x1" select="$x1"/>
          <xsl:with-param name="y1" select="$y1 + floor($lineWidth div 2)"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:when test="$slope > 0.5 or $slope &lt; -0.5">
        <xsl:call-template name="drawSteepLine">
          <xsl:with-param name="slope" select="$slope"/>
          <xsl:with-param name="x0" select="$x0"/>
          <xsl:with-param name="y0" select="$y0"/>
          <xsl:with-param name="x1" select="$x1"/>
          <xsl:with-param name="y1" select="$y1"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="drawFlatLine">
          <xsl:with-param name="slope" select="$slope"/>
          <xsl:with-param name="x0" select="$x0"/>
          <xsl:with-param name="y0" select="$y0"/>
          <xsl:with-param name="x1" select="$x1"/>
          <xsl:with-param name="y1" select="$y1"/>
        </xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  
  <!-- Draw Line with height > width.  Recursively calls itself drawing a series
  of vertical lines with each recursion. -->
  <xsl:template name="drawSteepLine">
    <xsl:param name="slope"/> <!-- height/width -->
    <xsl:param name="x0"/>
    <xsl:param name="y0"/>
    <xsl:param name="x1"/>
    <xsl:param name="y1"/>
    
    <xsl:variable name="inc">
      <xsl:choose>
        <xsl:when test="$x0 &lt; $x1">1</xsl:when>
        <xsl:otherwise>-1</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <debug select="drawSteepLine {$x0},{$y0},{$x1},{$y1} slope={$slope} inc={$inc}"/>

    <xsl:call-template name="fillBox">
      <xsl:with-param name="x0" select="$x0 - floor(($lineWidth - 1) div 2)"/>

      <xsl:with-param name="y0">
        <xsl:choose>
          <xsl:when test="$y0"> <!-- Start of line -->
            <xsl:value-of select="$y0"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="$y1 + floor($slope * ($x0 - $x1 + 0.5 * $inc))"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:with-param>

      <xsl:with-param name="x1" select="$x0 + floor(($lineWidth - 1) div 2)"/>

      <xsl:with-param name="y1">
        <xsl:choose>
          <xsl:when test="$x0 = $x1"> <!-- End of line -->
            <xsl:value-of select="$y1"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="$y1 + floor($slope * ($x0 - $x1 - 0.5 * $inc))"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:with-param>
    </xsl:call-template>
    
    <xsl:if test="$x0 != $x1">
      <xsl:call-template name="drawSteepLine">
        <xsl:with-param name="x0" select="$x0 + $inc"/>
        <xsl:with-param name="x1" select="$x1"/>
        <xsl:with-param name="y1" select="$y1"/>
        <xsl:with-param name="slope" select="$slope"/>
      </xsl:call-template>
    </xsl:if>
  </xsl:template>

  <!--
  Draw Line with width > height.  Recursively calls itself drawing a series
  of horizontal lines with each recursion.
  -->
  <xsl:template name="drawFlatLine">
    <xsl:param name="slope"/> <!-- height/width -->
    <xsl:param name="x0"/> <!-- Only defined on first call -->
    <xsl:param name="y0"/>
    <xsl:param name="x1"/>
    <xsl:param name="y1"/>
    
    <xsl:variable name="inc">
      <xsl:choose>
        <xsl:when test="$y0 &lt; $y1">1</xsl:when>
        <xsl:otherwise>-1</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <debug select="drawFlatLine {$x0},{$y0},{$x1},{$y1} slope={$slope} inc={$inc}"/>

    <xsl:call-template name="fillBox">

      <xsl:with-param name="x0">
        <xsl:choose>
          <xsl:when test="$x0"> <!-- Start of line -->
            <xsl:value-of select="$x0"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="$x1 - floor(($y1 - $y0 + 0.5 * $inc) div $slope)"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:with-param>

      <xsl:with-param name="y0" select="$y0 - floor(($lineWidth - 1) div 2)"/>

      <xsl:with-param name="x1">
        <xsl:choose>
          <xsl:when test="$y0 = $y1"> <!-- End of line -->
            <xsl:value-of select="$x1"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="$x1 - floor(($y1 - $y0 - 0.5 * $inc) div $slope)"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:with-param>

      <xsl:with-param name="y1" select="$y0 + floor(($lineWidth - 1) div 2)"/>
    </xsl:call-template>
    
    <xsl:if test="$y0 != $y1">
      <xsl:call-template name="drawFlatLine">
        <xsl:with-param name="y0" select="$y0 + $inc"/>
        <xsl:with-param name="x1" select="$x1"/>
        <xsl:with-param name="y1" select="$y1"/>
        <xsl:with-param name="slope" select="$slope"/>
      </xsl:call-template>
    </xsl:if>
  </xsl:template>

  <!-- Render a solid box -->
  <xsl:template name="fillBox">
    <xsl:param name="x0"/>
    <xsl:param name="y0"/>
    <xsl:param name="x1"/>
    <xsl:param name="y1"/>

    <debug select="fillBox {$x0},{$y0},{$x1},{$y1}"/>
    <xsl:variable name="xMax">
      <xsl:choose>
        <xsl:when test="$x1 > $x0">
          <xsl:value-of select="$x1"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="$x0"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <xsl:variable name="xMin">
      <xsl:choose>
        <xsl:when test="$x1 > $x0">
          <xsl:value-of select="$x0"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="$x1"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <xsl:variable name="yMin">
      <xsl:choose>
        <xsl:when test="$y1 > $y0">
          <xsl:value-of select="$y0"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="$y1"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <xsl:variable name="yMax">
      <xsl:choose>
        <xsl:when test="$y1 > $y0">
          <xsl:value-of select="$y1"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="$y0"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <div style="position:absolute; left:{$xMin}px; top:{$yMin}px; width:{$xMax - $xMin +1}px; height:{$yMax -$yMin +1}px; background-color:{$color}"><i/></div>
  </xsl:template>

  <!-- Render a <div> box -->
  <xsl:template name="mkDiv">
    <xsl:param name="x"/>
    <xsl:param name="y"/>
    <xsl:param name="w"/>
    <xsl:param name="h"/>

    <div style="position:absolute; left:{$x}px; top:{$y}px; width:{$w}px; height:{$h}px; background-color:{$color}"><i/></div>
  </xsl:template>

  <xsl:template match="text()|@*"/>
  
</xsl:stylesheet>
