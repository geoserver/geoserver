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
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
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
        CoordinateReferenceSystem crs = null;
        if (srs != null) {
            crs = CRS.decode(srs);
            mapRequest.setSRS(srs);
            mapRequest.setCrs(crs);
        }
        if (bbox != null) {
            mapRequest.setBbox(bbox);
        }
        WMSMapContent map = new WMSMapContent(mapRequest);
        map.getViewport().setBounds(new ReferencedEnvelope(bbox, crs));
        for (QName l : layers) {
            map.addLayer(createMapLayer(l));
        }
        return map;
    }
            
    @Test
    public void testMapBoxTileBuilder() throws Exception {         
        
        Envelope env = new Envelope(-92.8, -93.2, 4.5, 4.6);
        env = JTS.transform(env, CRS.findMathTransform(CRS.decode("EPSG:4326"), CRS.decode("EPSG:900913"), true));
        
        WMSMapContent mapContent = createMapContent("EPSG:900913", env, POINTS, POLYGONS);
        mapContent.setMapHeight(256);
        mapContent.setMapWidth(256);

        MapBoxTileBuilderFactory builderFact = new MapBoxTileBuilderFactory();
        
        VectorTileMapOutputFormat outputFormat = new VectorTileMapOutputFormat(getWMS(), builderFact);
        outputFormat.setTransformToScreenCoordinates(true);
        
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
        assertEquals(new Coordinate(641, 973), ((Point) featList.get(0).getGeometry()).getCoordinate());
        assertEquals("Polygons", featList.get(1).getLayerName());
        assertTrue(featList.get(1).getGeometry() instanceof Polygon);
        assertEquals(new Coordinate(646, 976), (((Polygon) featList.get(1).getGeometry()).getCoordinates()[0]));
    }
    

    @Test
    public void testMapBoxTileBuilderOtherCrs() throws Exception {        
        WMSMapContent mapContent = createMapContent("EPSG:4326", 
                new Envelope(-92.8, -93.2, 4.5, 4.6), POINTS, POLYGONS);
        
        mapContent.setMapHeight(256);
        mapContent.setMapWidth(256);
        
        MapBoxTileBuilderFactory builderFact = new MapBoxTileBuilderFactory();
        
        VectorTileMapOutputFormat outputFormat = new VectorTileMapOutputFormat(getWMS(), builderFact);
        outputFormat.setTransformToScreenCoordinates(true);
        
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
        assertEquals(new Coordinate(641, 973), ((Point) featList.get(0).getGeometry()).getCoordinate());
        assertEquals("Polygons", featList.get(1).getLayerName());
        assertTrue(featList.get(1).getGeometry() instanceof Polygon);
        assertEquals(new Coordinate(646, 976), (((Polygon) featList.get(1).getGeometry()).getCoordinates()[0]));
    }

}
