<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:wfs="http://www.opengis.net/wfs"
  xmlns:tiger="http://www.census.gov" xmlns:gml="http://www.opengis.net/gml"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:template match="/">
    <html>
      <body>
      <xsl:for-each select="wfs:FeatureCollection/gml:featureMember/*">
        <h2><xsl:value-of select="@fid"/></h2>
        <table border="1">
          <tr>
            <th>Attribute</th>
            <th>Value</th>
          </tr>
            <!-- [not(*)] strips away all nodes having children, in particular, geometries -->
            <xsl:for-each select="./*[not(*)]">
            <tr>
              <td>
                <xsl:value-of select="name()" />
              </td>
              <td>
                <xsl:value-of select="." />
              </td>
            </tr>
            </xsl:for-each>
        </table>
     </xsl:for-each>
     </body>
   </html>
  </xsl:template>
</xsl:stylesheet>
