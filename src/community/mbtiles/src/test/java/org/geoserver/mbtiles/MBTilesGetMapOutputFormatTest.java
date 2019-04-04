/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mbtiles;

import static org.geoserver.data.test.MockData.*;
import static org.junit.Assert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.xml.namespace.QName;
import org.apache.commons.io.FileUtils;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.gwc.GWC;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WMSTestSupport;
import org.geoserver.wms.WebMap;
import org.geoserver.wms.map.RawMap;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.mbtiles.MBTilesFile;
import org.geotools.mbtiles.MBTilesMetadata;
import org.geotools.mbtiles.MBTilesTile;
import org.geotools.referencing.CRS;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Envelope;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Test For WMS GetMap Output Format for MBTiles
 *
 * @author Niels Charlier
 */
public class MBTilesGetMapOutputFormatTest extends WMSTestSupport {

    MBTilesGetMapOutputFormat format;

    @Before
    public void setUpFormat() {
        format = new MBTilesGetMapOutputFormat(getWebMapService(), getWMS(), GWC.get());
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
        mapContent.getRequest().getFormatOptions().put("min_zoom", "10");
        mapContent.getRequest().getFormatOptions().put("max_zoom", "11");

        WebMap map = format.produceMap(mapContent);
        MBTilesFile mbtiles = createMbTilesFiles(map);

        MBTilesMetadata metadata = mbtiles.loadMetaData();

        assertEquals("World_Lakes", metadata.getName());
        assertEquals("0", metadata.getVersion());
        assertEquals("World, null", metadata.getDescription());
        assertEquals(-0.17578125, metadata.getBounds().getMinimum(0), 0.001);
        assertEquals(-0.087890625, metadata.getBounds().getMaximum(0), 0.001);
        assertEquals(0.17578125, metadata.getBounds().getMaximum(1), 0.001);
        assertEquals(0.087890625, metadata.getBounds().getMinimum(1), 0.001);
        assertEquals(MBTilesMetadata.t_type.OVERLAY, metadata.getType());
        assertEquals(MBTilesMetadata.t_format.PNG, metadata.getFormat());

        assertEquals(1, mbtiles.numberOfTiles());

        MBTilesFile.TileIterator tiles = mbtiles.tiles();
        assertTrue(tiles.hasNext());
        MBTilesTile e = tiles.next();
        assertEquals(10, e.getZoomLevel());
        assertEquals(511, e.getTileColumn());
        assertEquals(512, e.getTileRow());
        assertNotNull(e.getData());
        tiles.close();

        mbtiles.close();
    }

    @Test
    public void testTileEntriesWithAddTiles() throws Exception {
        // Create a getMap request
        WMSMapContent mapContent = createMapContent(WORLD, LAKES);
        mapContent
                .getRequest()
                .setBbox(new Envelope(-0.17578125, -0.087890625, 0.17578125, 0.087890625));
        mapContent.getRequest().getFormatOptions().put("min_zoom", "10");
        mapContent.getRequest().getFormatOptions().put("max_zoom", "11");
        // Create a temporary file for the mbtiles
        File f = File.createTempFile("temp2", ".mbtiles", new File("target"));
        MBTilesFile mbtiles = new MBTilesFile(f);
        mbtiles.init();
        // Add tiles to the file(Internally uses the MBtilesFileWrapper)
        format.addTiles(mbtiles, mapContent.getRequest(), null);
        // Ensure everything is correct
        MBTilesMetadata metadata = mbtiles.loadMetaData();

        assertEquals("World_Lakes", metadata.getName());
        assertEquals("0", metadata.getVersion());
        assertEquals("World, null", metadata.getDescription());
        assertEquals(-0.17578125, metadata.getBounds().getMinimum(0), 0.001);
        assertEquals(-0.087890625, metadata.getBounds().getMaximum(0), 0.001);
        assertEquals(0.17578125, metadata.getBounds().getMaximum(1), 0.001);
        assertEquals(0.087890625, metadata.getBounds().getMinimum(1), 0.001);
        assertEquals(MBTilesMetadata.t_type.OVERLAY, metadata.getType());
        assertEquals(MBTilesMetadata.t_format.PNG, metadata.getFormat());

        assertEquals(1, mbtiles.numberOfTiles());

        MBTilesFile.TileIterator tiles = mbtiles.tiles();
        assertTrue(tiles.hasNext());
        MBTilesTile e = tiles.next();
        assertEquals(10, e.getZoomLevel());
        assertEquals(511, e.getTileColumn());
        assertEquals(512, e.getTileRow());
        assertNotNull(e.getData());
        tiles.close();
        // Closure of the files
        mbtiles.close();
        FileUtils.deleteQuietly(f);
    }

    @Test
    public void testDifferentBbox() throws NoSuchAuthorityCodeException, FactoryException {
        // Instantiate a request
        GetMapRequest req = new GetMapRequest();
        // Define CRS
        CoordinateReferenceSystem crs = CRS.decode("EPSG:4326");
        // Create the first bbox
        ReferencedEnvelope bbox1 = new ReferencedEnvelope(0, 1, 0, 1, crs);
        req.setBbox(bbox1);
        req.setCrs(crs);
        ReferencedEnvelope bounds1 = format.bounds(req);
        // Create the second bbox
        ReferencedEnvelope bbox2 = new ReferencedEnvelope(1, 2, 1, 2, crs);
        req.setBbox(bbox2);
        ReferencedEnvelope bounds2 = format.bounds(req);
        // Ensure that the 2 generated bbox are not the same so that they are not cached
        double tolerance = 0.1d;
        assertNotSame(bounds1, bounds2);
        assertNotEquals(bounds1.getMinX(), bounds2.getMinX(), tolerance);
        assertNotEquals(bounds1.getMinY(), bounds2.getMinY(), tolerance);
        assertNotEquals(bounds1.getMaxX(), bounds2.getMaxX(), tolerance);
        assertNotEquals(bounds1.getMaxY(), bounds2.getMaxY(), tolerance);
    }

    MBTilesFile createMbTilesFiles(WebMap map) throws IOException {
        assertTrue(map instanceof RawMap);

        RawMap rawMap = (RawMap) map;
        File f = File.createTempFile("temp", ".mbtiles", new File("target"));
        FileOutputStream fout = new FileOutputStream(f);
        rawMap.writeTo(fout);
        fout.flush();
        fout.close();

        return new MBTilesFile(f);
    }

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
}
