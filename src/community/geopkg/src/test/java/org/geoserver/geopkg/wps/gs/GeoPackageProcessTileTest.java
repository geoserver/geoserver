/* (c) 2014-2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geopkg.wps.gs;

import static org.junit.Assert.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import javax.imageio.ImageIO;
import org.apache.commons.io.FileUtils;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wps.WPSTestSupport;
import org.geotools.geopkg.GeoPackage;
import org.geotools.geopkg.Tile;
import org.geotools.image.test.ImageAssert;
import org.geotools.util.URLs;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class GeoPackageProcessTileTest extends WPSTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        testData.setUpDefaultRasterLayers();
    }

    @Test
    public void testGeoPackageProcessTilesTopLeftTile() throws Exception {
        String urlPath = string(post("wps", getXmlTilesWorld())).trim();
        String resourceUrl = urlPath.substring("http://localhost:8080/geoserver/".length());
        MockHttpServletResponse response = getAsServletResponse(resourceUrl);
        File file = new File(getDataDirectory().findOrCreateDir("tmp"), "worldtest.gpkg");
        FileUtils.writeByteArrayToFile(file, getBinary(response));
        assertNotNull(file);
        assertEquals("worldtest.gpkg", file.getName());
        assertTrue(file.exists());

        GeoPackage gpkg = new GeoPackage(file);
        Tile topLeftTile = gpkg.reader(gpkg.tiles().get(0), 1, 1, 0, 0, 0, 0).next();
        BufferedImage tileImg = ImageIO.read(new ByteArrayInputStream(topLeftTile.getData()));

        ImageAssert.assertEquals(
                URLs.urlToFile(getClass().getResource("wps_toplefttile.png")), tileImg, 250);
        gpkg.close();
    }

    private String getXmlTilesWorld() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">"
                + "  <ows:Identifier>gs:GeoPackage</ows:Identifier>"
                + "  <wps:DataInputs>"
                + "    <wps:Input>"
                + "      <ows:Identifier>contents</ows:Identifier>"
                + "      <wps:Data>"
                + "        <wps:ComplexData mimeType=\"text/xml; subtype=geoserver/geopackage\"><![CDATA["
                + "<geopackage name=\"worldtest\" xmlns=\"http://www.opengis.net/gpkg\">"
                + "  <tiles name=\"world\" identifier=\"wl1\">"
                + "    <description>world overlay</description>  "
                + "    <srs>EPSG:4326</srs>"
                + "    <bbox>"
                + "      <minx>-180</minx>"
                + "      <maxx>180</maxx>"
                + "      <miny>-90</miny>"
                + "      <maxy>90</maxy>"
                + "    </bbox>"
                + "    <layers>wcs:World</layers>"
                + "  </tiles>"
                + "</geopackage>"
                + "]]></wps:ComplexData>"
                + "      </wps:Data>"
                + "    </wps:Input>"
                + "  </wps:DataInputs>"
                + "  <wps:ResponseForm>"
                + "    <wps:RawDataOutput>"
                + "      <ows:Identifier>geopackage</ows:Identifier>"
                + "    </wps:RawDataOutput>"
                + "  </wps:ResponseForm>"
                + "</wps:Execute>";
    }
}
