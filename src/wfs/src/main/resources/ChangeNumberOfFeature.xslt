<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:fn="http://www.w3.org/2005/xpath-functions"
    xmlns:wfs="http://www.opengis.net/wfs" xmlns:gml="http://www.opengis.net/gml">
    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="no" />
    <xsl:template match="*">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
        </xsl:copy>
    </xsl:template>
    <xsl:template match="@*|text()|comment()|processing-instruction()">
        <xsl:copy-of select="." />
    </xsl:template>
    <xsl:template match="//wfs:FeatureCollection/@numberOfFeatures">
        <xsl:attribute name="numberOfFeatures">
			<xsl:choose>
				<xsl:when test="//wfs:FeatureCollection/gml:featureMember">
					<xsl:value-of select="count(//wfs:FeatureCollection/gml:featureMember)" /> 
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="count(//wfs:FeatureCollection/gml:featureMembers/child::*)" />					
				</xsl:otherwise>
			</xsl:choose>
		</xsl:attribute>
    </xsl:template>
</xsl:stylesheet>