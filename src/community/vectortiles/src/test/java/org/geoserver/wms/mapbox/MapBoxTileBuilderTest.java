/* (c) 2015-2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.mapbox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.awt.Rectangle;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import no.ecc.vectortile.VectorTileDecoder;
import no.ecc.vectortile.VectorTileDecoder.Feature;

import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.map.RawMap;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.junit.Test;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

public class MapBoxTileBuilderTest {

    private Geometry geom(String wkt) throws ParseException {
        return new WKTReader().read(wkt);
    }

    private ListMultimap<String, Feature> decode(RawMap map) throws IOException {

        ArrayListMultimap<String, VectorTileDecoder.Feature> byLayer = ArrayListMultimap.create();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        map.writeTo(bos);
        bos.close();

        VectorTileDecoder decoder = new VectorTileDecoder();
        decoder.setAutoScale(false);

        for (Feature feat : decoder.decode(bos.toByteArray())) {
            // System.out.println(feat.getLayerName() + ": ");
            // System.out.print(feat.getAttributes());
            // System.out.println(feat.getGeometry());
            byLayer.put(feat.getLayerName(), feat);
        }

        return byLayer;
    }

    @Test
    public void testMapBoxTileBuilder() throws Exception {

        MapBoxTileBuilderFactory builderFact = new MapBoxTileBuilderFactory();

        Rectangle screenSize = new Rectangle(256, 256);
        ReferencedEnvelope mapArea = new ReferencedEnvelope();

        MapBoxTileBuilder tileBuilder = builderFact.newBuilder(screenSize, mapArea);

        Geometry point = geom("POINT(1 10)");
        Map<String, Object> pointProps = ImmutableMap.<String, Object> of("name", "point1");

        Geometry line = geom("LINESTRING(0 0, 1 1, 2 2)");
        Map<String, Object> lineProps = ImmutableMap.<String, Object> of("name", "line1");

        tileBuilder.addFeature("Points", "unused", "unused", point, pointProps);
        tileBuilder.addFeature("Lines", "unused", "unused", line, lineProps);

        WMSMapContent mapContent = mock(WMSMapContent.class);

        RawMap map = tileBuilder.build(mapContent);

        ListMultimap<String, Feature> features = decode(map);

        assertEquals(2, features.size());
        assertEquals(ImmutableSet.of("Points", "Lines"), features.keySet());

        Feature pointFeature = features.get("Points").get(0);
        Feature lineFeature = features.get("Lines").get(0);

        assertTrue(pointFeature.getGeometry() instanceof Point);
        assertEquals(point, pointFeature.getGeometry());
        assertEquals(pointProps, pointFeature.getAttributes());

        assertTrue(lineFeature.getGeometry() instanceof LineString);
        assertEquals(line, lineFeature.getGeometry());
        assertEquals(lineProps, lineFeature.getAttributes());
    }
    
    /*
     * we ensure that the encoder is NOT clipping geometries by giving it 
     * a "too big" line and ensuring it isn't changed by the encoder.
     */
    @Test
    public void testEncoderClipping() throws Exception {

        MapBoxTileBuilderFactory builderFact = new MapBoxTileBuilderFactory();

        Rectangle screenSize = new Rectangle(256, 256);
        ReferencedEnvelope mapArea = new ReferencedEnvelope();

        MapBoxTileBuilder tileBuilder = builderFact.newBuilder(screenSize, mapArea);        

        Geometry line = geom("LINESTRING(-100 -100,300 300)"); //box is 0 to 256, so this is outside the box
        
        Map<String, Object> lineProps = ImmutableMap.<String, Object> of("name", "line1");

        tileBuilder.addFeature("Lines", "unused", "unused", line, lineProps);

        WMSMapContent mapContent = mock(WMSMapContent.class);

        RawMap map = tileBuilder.build(mapContent);

        ListMultimap<String, Feature> features = decode(map);

        assertEquals(1, features.size());
        assertEquals(ImmutableSet.of( "Lines"), features.keySet());

        Feature lineFeature = features.get("Lines").get(0);

        assertTrue(lineFeature.getGeometry() instanceof LineString);
        assertEquals(line, lineFeature.getGeometry()); // line should not be clipped
    }
    
}
