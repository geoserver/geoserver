package org.geoserver.wms.mapbox;

import static org.geoserver.data.test.MockData.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.util.List;

import javax.xml.namespace.QName;

import no.ecc.vectortile.VectorTileDecoder;
import no.ecc.vectortile.VectorTileDecoder.Feature;
import no.ecc.vectortile.VectorTileDecoder.FeatureIterable;

import org.geoserver.data.test.SystemTestData;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WMSTestSupport;
import org.geoserver.wms.map.RawMap;
import org.geoserver.wms.vector.VectorTileMapOutputFormat;
import org.geotools.referencing.CRS;
import org.junit.Test;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;


public class MapBoxTileBuilderTest extends WMSTestSupport {
    
    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        testData.setUpDefaultRasterLayers();
    }
    
    private WMSMapContent createMapContent(String srs, Envelope bbox, QName... layers) throws Exception {
        GetMapRequest mapRequest = createGetMapRequest(layers);
        if (srs != null) {
            mapRequest.setSRS(srs);
            CoordinateReferenceSystem crs = CRS.decode(srs);
            mapRequest.setCrs(crs);
        }
        if (bbox != null) {
            mapRequest.setBbox(bbox);
        }
        WMSMapContent map = new WMSMapContent(mapRequest);
        for (QName l : layers) {
            map.addLayer(createMapLayer(l));
        }
        return map;
    }
    
    @Test
    public void testMapBoxTileBuilder() throws Exception {        
        WMSMapContent mapContent = createMapContent(null, 
                new Envelope(-0.17578125, -0.087890625, 0.17578125, 0.087890625), POINTS, POLYGONS);
        mapContent.setMapHeight(1024);
        mapContent.setMapWidth(1024);
        
        MapBoxTileBuilderFactory builderFact = new MapBoxTileBuilderFactory();
        
        VectorTileMapOutputFormat outputFormat = new VectorTileMapOutputFormat(getWMS(), builderFact);
        
        RawMap map = (RawMap) outputFormat.produceMap(mapContent);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        map.writeTo(bos);
        VectorTileDecoder decoder = new VectorTileDecoder();
        decoder.setAutoScale(false);
        
        FeatureIterable feats = decoder.decode(bos.toByteArray());
        
        for (Feature feat : feats) {
            System.out.println(feat.getLayerName() + ": ");
            System.out.print(feat.getAttributes());
            System.out.println(feat.getGeometry());
        }
        
        bos.close();
        
        List<Feature> featList = feats.asList();
        assertEquals(2, featList.size());
        assertEquals("Points", featList.get(0).getLayerName());
        assertTrue(featList.get(0).getGeometry() instanceof Point);
        assertEquals(new Coordinate(0, 512), ((Point) featList.get(0).getGeometry()).getCoordinate());
        assertEquals("Polygons", featList.get(1).getLayerName());
        assertTrue(featList.get(1).getGeometry() instanceof Polygon);
        assertEquals(new Coordinate(796, 1024), (((Polygon) featList.get(1).getGeometry()).getCoordinates()[0]));
    }
    
    @Test
    public void testMapBoxTileBuilderForceCrs() throws Exception {
        WMSMapContent mapContent = createMapContent(null, 
                new Envelope(-0.17578125, -0.087890625, 0.17578125, 0.087890625), POINTS, POLYGONS);
        mapContent.setMapHeight(1024);
        mapContent.setMapWidth(1024);
        
        MapBoxTileBuilderFactory builderFact = new MapBoxTileBuilderFactory();
        builderFact.setForceCrs(true);
        
        VectorTileMapOutputFormat outputFormat = new VectorTileMapOutputFormat(getWMS(), builderFact);
        
        RawMap map = (RawMap) outputFormat.produceMap(mapContent);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        map.writeTo(bos);
        VectorTileDecoder decoder = new VectorTileDecoder();
        decoder.setAutoScale(false);
        
        FeatureIterable feats = decoder.decode(bos.toByteArray());
        
        for (Feature feat : feats) {
            System.out.println(feat.getLayerName() + ": ");
            System.out.print(feat.getAttributes());
            System.out.println(feat.getGeometry());
        }
        
        bos.close();
        
        List<Feature> featList = feats.asList();
        assertEquals(2, featList.size());
        assertEquals("Points", featList.get(0).getLayerName());
        assertTrue(featList.get(0).getGeometry() instanceof Point);
        assertEquals(new Coordinate(0, 512), ((Point) featList.get(0).getGeometry()).getCoordinate());
        assertEquals("Polygons", featList.get(1).getLayerName());
        assertTrue(featList.get(1).getGeometry() instanceof Polygon);
        assertEquals(new Coordinate(796, 1024), (((Polygon) featList.get(1).getGeometry()).getCoordinates()[0]));
    }
    
    @Test
    public void testMapBoxTileBuilderCustomCrs() throws Exception {      
        //TODO: some crs that makes sense in this location but with other than mercator projection?
        WMSMapContent mapContent = createMapContent("EPSG:27571", null, POINTS, POLYGONS);
        mapContent.setMapHeight(1024);
        mapContent.setMapWidth(1024);
        
        MapBoxTileBuilderFactory builderFact = new MapBoxTileBuilderFactory();
        
        VectorTileMapOutputFormat outputFormat = new VectorTileMapOutputFormat(getWMS(), builderFact);
        
        RawMap map = (RawMap) outputFormat.produceMap(mapContent);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        map.writeTo(bos);
        VectorTileDecoder decoder = new VectorTileDecoder();
        decoder.setAutoScale(false);
        
        FeatureIterable feats = decoder.decode(bos.toByteArray());
        
        for (Feature feat : feats) {
            System.out.println(feat.getLayerName() + ": ");
            System.out.print(feat.getAttributes());
            System.out.println(feat.getGeometry());
        }
        
        bos.close();
    }

}
