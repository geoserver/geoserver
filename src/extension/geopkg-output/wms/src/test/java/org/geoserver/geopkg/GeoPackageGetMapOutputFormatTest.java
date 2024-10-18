/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geopkg;

import static org.geoserver.data.test.MockData.LAKES;
import static org.geoserver.data.test.MockData.WORLD;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.xml.namespace.QName;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.geopkg.wms.GeoPackageGetMapOutputFormat;
import org.geoserver.gwc.GWC;
import org.geoserver.platform.ServiceException;
import org.geoserver.tiles.FileBackedRawMap;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WMSTestSupport;
import org.geoserver.wms.WebMap;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geopkg.GeoPackage;
import org.geotools.geopkg.Tile;
import org.geotools.geopkg.TileEntry;
import org.geotools.image.test.ImageAssert;
import org.geotools.util.URLs;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Envelope;

/**
 * Test For WMS GetMap Output Format for GeoPackage
 *
 * @author Justin Deoliveira, Boundless
 */
public class GeoPackageGetMapOutputFormatTest extends WMSTestSupport {

    GeoPackageGetMapOutputFormat format;

    @Before
    public void setUpFormat() {
        format = new GeoPackageGetMapOutputFormat(getWebMapService(), getWMS(), GWC.get());
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        testData.setUpDefaultRasterLayers();
    }

    @Test
    public void testTileEntries() throws Exception {
        WMSMapContent mapContent = createMapContent(WORLD, LAKES);
        mapContent
                .getRequest()
                .setBbox(new Envelope(-0.17578125, -0.087890625, 0.17578125, 0.087890625));

        WebMap map = format.produceMap(mapContent);
        try (GeoPackage geopkg = createGeoPackage(map)) {
            assertTrue(geopkg.features().isEmpty());
            assertEquals(1, geopkg.tiles().size());
            TileEntry tile = geopkg.tile("World_Lakes");

            assertNotNull(tile);
            // the bounds should be the ones of the tile matrix, not the ones of the layer
            ReferencedEnvelope bounds = tile.getTileMatrixSetBounds();
            assertEquals(-180, bounds.getMinimum(0), 0d);
            assertEquals(180, bounds.getMaximum(0), 0d);
            assertEquals(-90, bounds.getMinimum(1), 0d);
            assertEquals(90, bounds.getMaximum(1), 0d);
        }
    }

    @Test
    /*
     * From the OGC GeoPackage Specification [1]:
     *
     * "The tile coordinate (0,0) always refers to the tile in the upper left corner of the tile matrix at any zoom
     * level, regardless of the actual availability of that tile"
     *
     * [1]: http://www.geopackage.org/spec/#tile_matrix
     */
    public void testTopLeftTile() throws Exception {
        WMSMapContent mapContent = createMapContent(WORLD);
        mapContent.getRequest().setBbox(new Envelope(-180, 180, -90, 90));

        WebMap map = format.produceMap(mapContent);
        try (GeoPackage geopkg = createGeoPackage(map)) {

            assertTrue(geopkg.features().isEmpty());
            assertEquals(1, geopkg.tiles().size());

            Tile topLeftTile = geopkg.reader(geopkg.tiles().get(0), 1, 1, 0, 0, 0, 0).next();

            BufferedImage tileImg = ImageIO.read(new ByteArrayInputStream(topLeftTile.getData()));

            ImageAssert.assertEquals(
                    URLs.urlToFile(getClass().getResource("toplefttile.png")), tileImg, 250);
        }
    }

    @Test
    public void testTimeout() throws Exception {
        GeoServer gs = getGeoServer();
        WMSInfo wms = gs.getService(WMSInfo.class);
        wms.setMaxRenderingTime(1);
        gs.save(wms);

        // a set of zooms so large, it can only go in timeout
        WMSMapContent mapContent = createMapContent(WORLD, LAKES);
        mapContent
                .getRequest()
                .setBbox(new Envelope(-0.17578125, -0.087890625, 0.17578125, 0.087890625));
        mapContent.getRequest().getFormatOptions().put("min_zoom", "10");
        mapContent.getRequest().getFormatOptions().put("max_zoom", "30");

        try {
            format.produceMap(mapContent);
            fail("Should not have got here, and if it does, would have taken hours?");
        } catch (ServiceException e) {
            assertThat(e.getMessage(), CoreMatchers.containsString("used more time than allowed"));
        } finally {
            wms.setMaxRenderingTime(-1);
            gs.save(wms);
        }
    }

    GeoPackage createGeoPackage(WebMap map) throws IOException {
        assertTrue(map instanceof FileBackedRawMap);

        try (FileBackedRawMap rawMap = (FileBackedRawMap) map) {
            File f = File.createTempFile("temp", ".gpkg", new File("target"));
            try (FileOutputStream fout = new FileOutputStream(f)) {
                rawMap.writeTo(fout);
                fout.flush();
            }

            return new GeoPackage(f);
        }
    }

    @Override
    protected GetMapRequest createGetMapRequest(QName[] layerNames) {
        GetMapRequest request = super.createGetMapRequest(layerNames);
        request.setBbox(new Envelope(-180, 180, -90, 90));
        return request;
    };

    WMSMapContent createMapContent(QName... layers) throws IOException {
        GetMapRequest mapRequest = createGetMapRequest(layers);
        WMSMapContent map = new WMSMapContent(mapRequest);
        for (QName l : layers) {
            map.addLayer(createMapLayer(l));
        }
        return map;
    }

    /*public static void main(String[] args) throws Exception {
        GeoPackage geopkg = new GeoPackage(new File(
            "/Users/jdeolive/geopkg.db"));;
        File d = new File("/Users/jdeolive/tiles");
        d.mkdir();

        TileEntry te = geopkg.tiles().get(0);
        TileReader r = geopkg.reader(te, null, null, null, null, null, null);
        while(r.hasNext()) {
            Tile t = r.next();
            File f = new File(d, String.format("%d-%d-%d.png", t.getZoom(), t.getColumn(), t.getRow()));

            FileUtils.writeByteArrayToFile(f, t.getData());
        }
    }*/
}
