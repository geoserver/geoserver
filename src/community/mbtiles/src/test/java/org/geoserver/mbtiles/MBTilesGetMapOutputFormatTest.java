/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mbtiles;

import static org.junit.Assert.*;
import static org.geoserver.data.test.MockData.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.xml.namespace.QName;

import org.geoserver.data.test.SystemTestData;
import org.geoserver.gwc.GWC;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WMSTestSupport;
import org.geoserver.wms.WebMap;
import org.geoserver.wms.map.RawMap;
import org.geotools.mbtiles.MBTilesTile;
import org.geotools.mbtiles.MBTilesFile;
import org.geotools.mbtiles.MBTilesMetadata;
import org.junit.Before;
import org.junit.Test;

import com.vividsolutions.jts.geom.Envelope;

/**
 * 
 * Test For WMS GetMap Output Format for MBTiles
 * 
 * @author Niels Charlier
 *
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
        mapContent.getRequest().setBbox(
            new Envelope(-0.17578125, -0.087890625, 0.17578125, 0.087890625));
        mapContent.getRequest().getFormatOptions().put("min_zoom", "10");
        mapContent.getRequest().getFormatOptions().put("max_zoom", "11");
        
        WebMap map = format.produceMap(mapContent);
        MBTilesFile mbtiles = createMbTilesFiles(map);
        
        MBTilesMetadata metadata = mbtiles.loadMetaData();
        
        assertEquals("World_Lakes", metadata.getName());
        assertEquals("0", metadata.getVersion());
        assertEquals("World, null", metadata.getDescription());
        assertEquals("-180.0,89.82421875,-179.82421875,90.0", metadata.getBoundsStr());
        assertEquals(MBTilesMetadata.t_type.OVERLAY, metadata.getType());
        assertEquals(MBTilesMetadata.t_format.PNG, metadata.getFormat());
        
        assertEquals(1, mbtiles.numberOfTiles());

        MBTilesFile.TileIterator tiles = mbtiles.tiles();
        assertTrue(tiles.hasNext());
        MBTilesTile e = tiles.next();
        assertEquals(10, e.getZoomLevel());
        assertEquals(0, e.getTileColumn());
        assertEquals(1023, e.getTileRow());
        assertNotNull(e.getData());
        tiles.close();
        
        mbtiles.close();
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
        request.setBbox(new Envelope(-180,180,-90,90));
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
