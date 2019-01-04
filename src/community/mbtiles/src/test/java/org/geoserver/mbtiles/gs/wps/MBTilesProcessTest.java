/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mbtiles.gs.wps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wps.WPSTestSupport;
import org.geotools.mbtiles.MBTilesFile;
import org.geotools.mbtiles.MBTilesMetadata;
import org.geotools.util.URLs;
import org.geotools.util.logging.Logging;
import org.junit.Test;

public class MBTilesProcessTest extends WPSTestSupport {

    private static final Logger LOGGER = Logging.getLogger(MBTilesProcessTest.class);

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        testData.setUpDefaultRasterLayers();
    }

    @Test
    public void testMBTilesProcess() throws Exception {
        File path = getDataDirectory().findOrCreateDataRoot();

        String urlPath = string(post("wps", getXml(path))).trim();
        File file = new File(path, "World.mbtiles");
        // File file = getDataDirectory().findFile("data", "test.mbtiles");
        assertNotNull(file);
        assertTrue(file.exists());

        MBTilesFile mbtiles = new MBTilesFile(file);
        MBTilesMetadata metadata = mbtiles.loadMetaData();
        assertEquals(11, mbtiles.maxZoom());
        assertEquals(10, mbtiles.minZoom());
        assertEquals("World", metadata.getName());

        assertEquals(-0.17578125, metadata.getBounds().getMinimum(0), 0.0001);
        assertEquals(-0.087890625, metadata.getBounds().getMinimum(1), 0.0001);
        assertEquals(0.17578125, metadata.getBounds().getMaximum(0), 0.0001);
        assertEquals(0.087890625, metadata.getBounds().getMaximum(1), 0.0001);

        try {
            mbtiles.close();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    public String getXml(File temp) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">"
                + "  <ows:Identifier>gs:MBTiles</ows:Identifier>"
                + "  <wps:DataInputs>"
                + "    <wps:Input>"
                + "      <ows:Identifier>path</ows:Identifier>"
                + "      <wps:Data>"
                + "        <wps:LiteralData>"
                + URLs.fileToUrl(temp)
                + "</wps:LiteralData>"
                + "      </wps:Data>"
                + "    </wps:Input>"
                + "    <wps:Input>"
                + "      <ows:Identifier>layers</ows:Identifier>"
                + "      <wps:Data>"
                + "        <wps:LiteralData>wcs:World</wps:LiteralData>"
                + "      </wps:Data>"
                + "    </wps:Input>"
                + "    <wps:Input>"
                + "      <ows:Identifier>layers</ows:Identifier>"
                + "      <wps:Data>"
                + "        <wps:LiteralData>cite:Lakes</wps:LiteralData>"
                + "      </wps:Data>"
                + "    </wps:Input>"
                + "    <wps:Input>"
                + "      <ows:Identifier>format</ows:Identifier>"
                + "      <wps:Data>"
                + "        <wps:LiteralData>image/png</wps:LiteralData>"
                + "      </wps:Data>"
                + "    </wps:Input>"
                + "    <wps:Input>"
                + "      <ows:Identifier>boundingbox</ows:Identifier>"
                + "    <wps:Data>"
                + "    <wps:BoundingBoxData crs=\"EPSG:4326\" dimensions=\"2\">"
                + "     <ows:LowerCorner>-0.17578125 -0.087890625</ows:LowerCorner>"
                + "    <ows:UpperCorner>0.17578125 0.087890625</ows:UpperCorner>"
                + "    </wps:BoundingBoxData>"
                + "    </wps:Data>"
                + "    </wps:Input>"
                + "    <wps:Input>"
                + "      <ows:Identifier>minZoom</ows:Identifier>"
                + "      <wps:Data>"
                + "        <wps:LiteralData>10</wps:LiteralData>"
                + "      </wps:Data>"
                + "    </wps:Input>"
                + "    <wps:Input>"
                + "      <ows:Identifier>maxZoom</ows:Identifier>"
                + "      <wps:Data>"
                + "        <wps:LiteralData>12</wps:LiteralData>"
                + "      </wps:Data>"
                + "    </wps:Input>"
                + "    <wps:Input>"
                + "      <ows:Identifier>bgColor</ows:Identifier>"
                + "      <wps:Data>"
                + "        <wps:LiteralData>#FFFFFF</wps:LiteralData>"
                + "      </wps:Data>"
                + "    </wps:Input>"
                + "  </wps:DataInputs>"
                + "    <wps:Input>"
                + "      <ows:Identifier>transparency</ows:Identifier>"
                + "      <wps:Data>"
                + "        <wps:LiteralData>true</wps:LiteralData>"
                + "      </wps:Data>"
                + "    </wps:Input>"
                + "  <wps:ResponseForm>"
                + "    <wps:RawDataOutput>"
                + "      <ows:Identifier>mbtile</ows:Identifier>"
                + "    </wps:RawDataOutput>"
                + "  </wps:ResponseForm>"
                + "</wps:Execute>";
    }
}
