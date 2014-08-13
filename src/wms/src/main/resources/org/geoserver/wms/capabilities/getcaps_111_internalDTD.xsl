<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output omit-xml-declaration="no" method="xml" indent="yes" />

  <xsl:param name="DTDDeclaration">
    <xsl:comment>
      The value for this parameter is passed by GetCapabilitiesResponse and contains
      the full DTD declaration including
      SYSTEM identifiers and internal DTD elements
    </xsl:comment>
  </xsl:param>


  <xsl:template match="@*|node()">

    <xsl:if test="name() = 'WMT_MS_Capabilities'">
      <xsl:value-of select="$DTDDeclaration" disable-output-escaping="yes" />
    </xsl:if>

    <!-- identity template -->
    <xsl:copy>
      <xsl:apply-templates select="@*|node()" />
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>
