<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:wfs="http://www.opengis.net/wfs/2.0">
    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="no" />
    <xsl:template match="*">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()" />
        </xsl:copy>
    </xsl:template>
    <xsl:template match="@*|text()|comment()|processing-instruction()">
        <xsl:copy-of select="." />
    </xsl:template>
    <xsl:template match="/wfs:FeatureCollection">
        <xsl:copy>
            <xsl:copy-of select="@*" />
            <xsl:attribute name="numberMatched">
                <xsl:text>unknown</xsl:text>
            </xsl:attribute>
            <xsl:attribute name="numberReturned">
                <xsl:value-of select="count(//wfs:FeatureCollection/wfs:member)" />
            </xsl:attribute>
            <xsl:apply-templates select="*" />
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>