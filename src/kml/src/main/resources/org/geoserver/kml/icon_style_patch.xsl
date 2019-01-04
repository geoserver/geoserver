<?xml version="1.0" encoding="UTF-8"?>
<!--
 Copyright (C) 2017 - Open Source Geospatial Foundation. All rights reserved.
 This code is licensed under the GPL 2.0 license, available at the root
 application directory.
 -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan" xmlns:kml="http://www.opengis.net/kml/2.2">
    <xsl:output indent="yes" method="xml" standalone="yes" xalan:indent-amount="4" />
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*" />
        </xsl:copy>
    </xsl:template>
    <xsl:template match="kml:IconStyle/kml:Icon/kml:refreshInterval|kml:IconStyle/kml:Icon/kml:viewRefreshTime|kml:IconStyle/kml:Icon/kml:viewBoundScale" />
</xsl:stylesheet>
