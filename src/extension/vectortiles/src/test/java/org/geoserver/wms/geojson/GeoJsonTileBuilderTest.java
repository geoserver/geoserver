/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.geojson;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import com.google.common.collect.ImmutableMap;
import java.awt.Rectangle;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.map.RawMap;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

public class GeoJsonTileBuilderTest {

    @Test
    public void testGeoJsonWMSBuilder() throws ParseException, IOException {
        GeoJsonBuilderFactory builderFact = new GeoJsonBuilderFactory();
        Rectangle screenSize = new Rectangle(256, 256);
        ReferencedEnvelope mapArea = new ReferencedEnvelope(DefaultGeographicCRS.WGS84);
        Geometry point = geom("POINT(1 10)");
        Map<String, Object> pointProps = ImmutableMap.<String, Object>of("name", "point1");

        Geometry line = geom("LINESTRING(0 0, 1 1, 2 2)");
        Map<String, Object> lineProps = ImmutableMap.<String, Object>of("name", "line1");

        GeoJsonWMSBuilder tileBuilder = builderFact.newBuilder(screenSize, mapArea);

        tileBuilder.addFeature("Points", "unused", "unused", point, pointProps);
        tileBuilder.addFeature("Lines", "unused", "unused", line, lineProps);

        WMSMapContent mapContent = mock(WMSMapContent.class);

        RawMap map = tileBuilder.build(mapContent);

        String json = decode(map);

        assertEquals(
                "{\"type\":\"FeatureCollection\",\"totalFeatures\":\"unknown\",\"features\":[{\"type\":\"Feature\",\"id\":\"unused\",\"geometry\":{\"type\":\"Point\","
                        + "\"coordinates\":[1,10]},\"geometry_name\":\"unused\",\"properties\":{\"name\":\"point1\"}},{\"type\":\"Feature\",\"id\":\"unused\",\"geometry\":{\"type\":\"LineString\","
                        + "\"coordinates\":[[0,0],[1,1],[2,2]]},\"geometry_name\":\"unused\",\"properties\":{\"name\":\"line1\"}}]}",
                json);
    }

    private Geometry geom(String wkt) throws ParseException {
        return new WKTReader().read(wkt);
    }

    private String decode(RawMap map) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        map.writeTo(bos);
        bos.close();

        String out = new String(bos.toByteArray(), "UTF-8");
        return out;
    }
}
