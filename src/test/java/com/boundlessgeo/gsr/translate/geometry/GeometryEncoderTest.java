package com.boundlessgeo.gsr.translate.geometry;

import com.boundlessgeo.gsr.api.GeoServicesJacksonJsonConverter;
import com.boundlessgeo.gsr.model.geometry.Geometry;
import com.boundlessgeo.gsr.model.geometry.SpatialReferenceWKID;
import net.sf.json.JSON;
import net.sf.json.JSONSerializer;
import net.sf.json.test.JSONAssert;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

public class GeometryEncoderTest {

    @Test
    public void testJsonRoundTrip() throws URISyntaxException, IOException {
        //round-trip each type of geometry:
        assertRoundTrip("point_2d.json");
        assertRoundTrip("multipoint_2d.json");
        assertRoundTrip("polygon_2d.json");
        assertRoundTrip("polyline_2d.json");

        assertRoundTrip("geometrycollection_point.json");

        //TODO: add 3d support
        //assertRoundTrip("point_3d.json");
        //assertRoundTrip("multipoint_3d.json");
        //assertRoundTrip("polygon_3d_m.json");

        //TODO: add time support
        //assertRoundTrip("polyline_m.json");

        //TODO: add curve support
        //assertRoundTrip("polyline_curve.json");
        //assertRoundTrip("polygon_curve_m.json");
    }

    public void assertRoundTrip(String filename) throws URISyntaxException, IOException {
        GeometryEncoder encoder = new GeometryEncoder();
        GeoServicesJacksonJsonConverter jsonConverter = new GeoServicesJacksonJsonConverter();

        String stringSource = FileUtils.readFileToString(
                new File(getClass().getResource(filename).toURI()), "UTF-8");
        JSON jsonSource = JSONSerializer.toJSON(stringSource);

        com.vividsolutions.jts.geom.Geometry jtsGeom = GeometryEncoder.jsonToJtsGeometry(jsonSource);
        Geometry gsrGeom = encoder.toRepresentation(jtsGeom, new SpatialReferenceWKID(4326));

        String stringResult = jsonConverter.getMapper().writeValueAsString(gsrGeom);
        JSON jsonResult = JSONSerializer.toJSON(stringResult);

        JSONAssert.assertEquals(jsonSource, jsonResult);
    }
}
