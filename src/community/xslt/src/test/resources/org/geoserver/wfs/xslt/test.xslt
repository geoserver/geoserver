<?xml version="1.0" encoding="ISO-8859-1"?>
<!-- Edited by XMLSpy® -->
<xsl:stylesheet version="1.0" xmlns:wfs="http://www.opengis.net/wfs"
  xmlns:cite="http://www.opengis.net/cite" xmlns:gml="http://www.opengis.net/gml"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:template match="/">
    <html>
      <body>
        <h2>Bridges</h2>
        <xsl:for-each
            select="wfs:FeatureCollection/gml:featureMember/cite:Bridges">
        <ul>
          <li>ID: <xsl:value-of select="@fid" /></li>
          <li>FID: <xsl:value-of select="cite:FID" /></li>
          <li>Name: <xsl:value-of select="cite:NAME" /></li>
        </ul>
        <p/>
        </xsl:for-each>
      </body>
    </html>
  </xsl:template>
</xsl:stylesheet>