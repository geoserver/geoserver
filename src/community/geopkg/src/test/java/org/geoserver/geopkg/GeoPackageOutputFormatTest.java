package org.geoserver.geopkg;

import static org.junit.Assert.*;
import static org.geoserver.data.test.MockData.*;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.io.FileUtils;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.util.IOUtils;
import org.geoserver.gwc.GWC;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WMSTestSupport;
import org.geoserver.wms.WebMap;
import org.geoserver.wms.map.RawMap;
import org.geotools.data.simple.SimpleFeatureReader;
import org.geotools.geopkg.FeatureEntry;
import org.geotools.geopkg.GeoPackage;
import org.geotools.geopkg.Tile;
import org.geotools.geopkg.TileEntry;
import org.geotools.geopkg.TileReader;
import org.geotools.map.MapContent;
import org.junit.Before;
import org.junit.Test;

import com.vividsolutions.jts.geom.Envelope;

public class GeoPackageOutputFormatTest extends WMSTestSupport {

    GeoPackageOutputFormat format;

    @Before
    public void setUpFormat() {
        format = new GeoPackageOutputFormat(getWebMapService(), getWMS(), GWC.get());
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        testData.setUpDefaultRasterLayers();
    }

    @Test
    public void testFeatureEntries() throws Exception {

        WebMap map = format.produceMap(createMapContent(FORESTS, LAKES));
        assertNotNull(map);

        GeoPackage geopkg = createGeoPackage(map);
        List<FeatureEntry> entries = geopkg.features();

        assertEquals(2, entries.size());

        FeatureEntry e = geopkg.feature(FORESTS.getLocalPart());
        assertNotNull(e);

        SimpleFeatureReader r = geopkg.reader(e, null, null);
        assertTrue(r.hasNext());
        assertNotNull(r.next());

        geopkg.close();
        geopkg.getFile().delete();
    }

    @Test
    public void testRasterEntries() throws Exception {

        WMSMapContent mapContent = createMapContent(WORLD);
        mapContent.getRequest().getFormatOptions().put("mode", "hybrid");
        
        WebMap map = format.produceMap(mapContent);
        assertNotNull(map);

        GeoPackage geopkg = createGeoPackage(map);
        assertEquals(1, geopkg.rasters().size());
        assertNotNull(geopkg.raster(WORLD.getLocalPart()));

        geopkg.close();
        //geopkg.getFile().delete();
    }

    @Test
    public void testTileEntries() throws Exception {
        WMSMapContent mapContent = createMapContent(WORLD, LAKES);
        mapContent.getRequest().setBbox(
            new Envelope(-0.17578125, -0.087890625, 0.17578125, 0.087890625));
        mapContent.getRequest().getFormatOptions().put("mode", "tiled");
        mapContent.getRequest().getFormatOptions().put("min_zoom", "10");
        mapContent.getRequest().getFormatOptions().put("max_zoom", "11");
        
        WebMap map = format.produceMap(mapContent);
        GeoPackage geopkg = createGeoPackage(map);

        assertTrue(geopkg.features().isEmpty());
        assertTrue(geopkg.rasters().isEmpty());
        assertEquals(1, geopkg.tiles().size());
        assertNotNull(geopkg.tile("tiles"));
    }

    GeoPackage createGeoPackage(WebMap map) throws IOException {
        assertTrue(map instanceof RawMap);

        RawMap rawMap = (RawMap) map;
        File f = File.createTempFile("geopkg", "geopackage", new File("target"));
        FileOutputStream fout = new FileOutputStream(f);
        rawMap.writeTo(fout);
        fout.flush(); 
        fout.close();
        
        return new GeoPackage(f);
//        File f = File.createTempFile("geopkg", "zip", new File("target"));
//        FileOutputStream fout = new FileOutputStream(f);
//        rawMap.writeTo(fout);
//        fout.flush(); 
//        fout.close();
//
//        File g = File.createTempFile("geopkg", "db", new File("target"));
//        g.delete();
//        g.mkdir();
//
//        IOUtils.decompress(f, g);
//        return new GeoPackage(g.listFiles(new FileFilter() {
//            @Override
//            public boolean accept(File file) {
//                return file.getName().endsWith(".geopackage");
//            }
//        })[0]);
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

    public static void main(String[] args) throws Exception {
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
    }
}
