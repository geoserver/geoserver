package org.opengeo.gsr.core.geometry;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.opengeo.gsr.JsonSchemaTest;

/**
 * JSON Schema validation class. Validation is possible with local files, with the following modifications: - All $ref elements need to point to a
 * local JSON Schema - The $id element with absolute URI needs to be removed from parent Schema files
 * 
 * @author Juan Marin, OpenGeo
 * 
 */

public class GeometryJSONSchemaTest extends JsonSchemaTest {


    public GeometryJSONSchemaTest() {
        super();
    }

    @Test
    public void testPointSchema() throws Exception {
        Point point = new Point(-77, 39.5, new SpatialReferenceWKID(4326));
        String json = getJson(point);
        assertTrue(validateJSON(json, "gsr/1.0/point.json"));
    }

    @Test
    public void testMultipointSchema() throws Exception {
        SpatialReference spatialReference = new SpatialReferenceWKID(4326);
        double[] c1 = { -97.06138, 32.837 };
        double[] c2 = { -97.06133, 32.836 };
        double[] c3 = { -97.06124, 32.834 };
        double[] c4 = { -97.06127, 32.832 };
        double[][] coords = { c1, c2, c3, c4 };
        Multipoint multiPoint = new Multipoint(coords, spatialReference);
        String json = getJson(multiPoint);
        assertTrue(validateJSON(json, "gsr/1.0/multipoint.json"));
    }

    @Test
    public void testPolylineSchema() throws Exception {
        double[] c1 = { 0, 0 };
        double[] c2 = { 0, 1 };
        double[] c3 = { 1, 1 };
        double[] c4 = { 1, 0 };
        double[] c5 = { 1, 2 };

        double[][] path1 = { c1, c2, c3 };
        double[][] path2 = { c3, c4, c5 };

        double[][][] paths = { path1, path2 };

        SpatialReference spatialReference = new SpatialReferenceWKID(4326);
        Polyline polyline = new Polyline(paths, spatialReference);
        String json = getJson(polyline);
        assertTrue(validateJSON(json, "gsr/1.0/polyline.json"));
    }

    @Test
    public void testPolygonSchema() throws Exception {
        double[] c1 = { 0, 0 };
        double[] c2 = { 0, 1 };
        double[] c3 = { 1, 1 };
        double[] c4 = { 1, 0 };
        double[] c5 = { 1, 2 };

        double[][] ring1 = { c1, c2, c3, c4, c5, c1 };

        double[][] ring2 = { c2, c3, c4, c2 };

        SpatialReference spatialReference = new SpatialReferenceWKID(4326);
        double[][][] rings = { ring1, ring2 };
        Polygon polygon = new Polygon(rings, spatialReference);
        String json = getJson(polygon);
        assertTrue(validateJSON(json, "gsr/1.0/polygon.json"));
    }

    @Test
    public void testEnvelopeSchema() throws Exception {
        SpatialReference spatialReference = new SpatialReferenceWKID(4326);
        Envelope envelope = new Envelope(-77, 39.0, -78, 40.0, spatialReference);
        String json = getJson(envelope);
        assertTrue(validateJSON(json, "gsr/1.0/envelope.json"));
    }

    @Test
    public void testGeometryArraySchema() throws Exception {
        SpatialReference spatialReference = new SpatialReferenceWKID(4326);
        Point point = new Point(-77, 39.5, spatialReference);
        Point point2 = new Point(-77.4, 40.5, spatialReference);
        Geometry[] geometries = { point, point2 };
        GeometryArray array = new GeometryArray(GeometryTypeEnum.POINT, geometries,
                spatialReference);
        String json = getJson(array);
        assertTrue(validateJSON(json, "gsr/1.0/geometries.json"));
    }

    @Test
    public void testSpatialReferenceWKIDSchema() throws Exception {
        SpatialReference spatialReference = new SpatialReferenceWKID(4326);
        String json = getJson(spatialReference);
        assertTrue(validateJSON(json, "gsr/1.0/spatialreference.json"));
    }

    @Test
    public void testWrongSchema() throws Exception {
        Point point = new Point(-77, 39.5, new SpatialReferenceWKID(4326));
        String json = getJson(point);
        assertFalse(validateJSON(json, "gsr/1.0/envelope.json"));
    }

}
