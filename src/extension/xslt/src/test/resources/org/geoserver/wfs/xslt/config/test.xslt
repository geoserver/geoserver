<?xml version="1.0" encoding="ISO-8859-1"?>
<!-- Edited by XMLSpy® -->
<xsl:stylesheet version="1.0" xmlns:wfs="http://www.opengis.net/wfs"
  xmlns:tiger="http://www.census.gov" xmlns:gml="http://www.opengis.net/gml"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:template match="/">
    <html>
      <body>
        <h2>POI</h2>
        <table border="1">
          <tr>
            <th>Name</th>
            <th>Location</th>
          </tr>
          <xsl:for-each
            select="wfs:FeatureCollection/gml:featureMember/tiger:poi">
            <tr>
              <td>
                <xsl:value-of select="tiger:NAME" />
              </td>
              <td>
                <xsl:value-of select="tiger:the_geom/gml:Point/gml:coordinates" />
              </td>
            </tr>
          </xsl:for-each>
        </table>
      </body>
    </html>
  </xsl:template>
</xsl:stylesheet>