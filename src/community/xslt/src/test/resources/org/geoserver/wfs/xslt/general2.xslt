<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:wfs="http://www.opengis.net/wfs"
  xmlns:tiger="http://www.census.gov" xmlns:gml="http://www.opengis.net/gml"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:template match="/">
    <html>
      <body>
        <ul>
          <xsl:for-each select="wfs:FeatureCollection/gml:featureMember/*">
            <li>
              <xsl:value-of select="@fid" />
              <ul>
                <xsl:for-each select="./*[not(*)]">
                  <li>
                    <xsl:value-of select="name()" />
                    :
                    <xsl:value-of select="." />
                  </li>
                </xsl:for-each>
              </ul>
            </li>
          </xsl:for-each>
        </ul>
      </body>
    </html>
  </xsl:template>
</xsl:stylesheet>
