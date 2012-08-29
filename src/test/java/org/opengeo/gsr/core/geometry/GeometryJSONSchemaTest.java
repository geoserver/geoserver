package org.opengeo.gsr.core.geometry;

import org.codehaus.jackson.map.ObjectMapper;
import org.opengeo.gsr.validation.JSONSchemaUtils;

/**
 * JSON Schema validation class. Validation is possible with local files, with the following modifications: - All $ref elements need to point to a
 * local JSON Schema - The $id element with absolute URI needs to be removed from parent Schema files
 * 
 * @author Juan Marin, OpenGeo
 * 
 */

public class GeometryJSONSchemaTest extends JSONSchemaUtils {

    ObjectMapper mapper;

    public GeometryJSONSchemaTest() {
        mapper = new ObjectMapper();
    }

    public void testPointSchema() throws Exception {
        Point point = new Point(-77, 39.5, new SpatialReferenceWKID(4326));
        String json = mapper.writeValueAsString(point);
        assertTrue(validateJSON(json, "gsr/1.0/point.json"));
    }

    public void testMultipointSchema() throws Exception {
        SpatialReference spatialReference = new SpatialReferenceWKID(4326);
        double[] c1 = { -97.06138, 32.837 };
        double[] c2 = { -97.06133, 32.836 };
        double[] c3 = { -97.06124, 32.834 };
        double[] c4 = { -97.06127, 32.832 };
        double[][] coords = new double[4][2];
        coords[0] = c1;
        coords[1] = c2;
        coords[2] = c3;
        coords[3] = c4;
        Multipoint multiPoint = new Multipoint(coords, spatialReference);
        String json = mapper.writeValueAsString(multiPoint);
        assertTrue(validateJSON(json, "gsr/1.0/multipoint.json"));
    }

    public void testPolylineSchema() throws Exception {
        double[] c1 = { 0, 0 };
        double[] c2 = { 0, 1 };
        double[] c3 = { 1, 1 };
        double[] c4 = { 1, 0 };
        double[] c5 = { 1, 2 };

        double[][] path1 = new double[3][2];
        path1[0] = c1;
        path1[1] = c2;
        path1[2] = c3;
        double[][] path2 = new double[3][2];
        path2[0] = c3;
        path2[1] = c4;
        path2[2] = c5;

        double[][][] paths = new double[2][3][2];
        paths[0] = path1;
        paths[1] = path2;

        SpatialReference spatialReference = new SpatialReferenceWKID(4326);
        Polyline polyline = new Polyline(paths, spatialReference);
        String json = mapper.writeValueAsString(polyline);
        assertTrue(validateJSON(json, "gsr/1.0/polyline.json"));
    }

    public void testPolygonSchema() throws Exception {
        double[] c1 = { 0, 0 };
        double[] c2 = { 0, 1 };
        double[] c3 = { 1, 1 };
        double[] c4 = { 1, 0 };
        double[] c5 = { 1, 2 };

        double[][] ring1 = new double[6][2];
        ring1[0] = c1;
        ring1[1] = c2;
        ring1[2] = c3;
        ring1[3] = c4;
        ring1[4] = c5;
        ring1[5] = c1;

        double[][] ring2 = new double[4][2];
        ring2[0] = c2;
        ring2[1] = c3;
        ring2[2] = c4;
        ring2[3] = c2;

        SpatialReference spatialReference = new SpatialReferenceWKID(4326);
        double[][][] rings = new double[2][6][2];
        rings[0] = ring1;
        rings[1] = ring2;
        Polygon polygon = new Polygon(rings, spatialReference);
        String json = mapper.writeValueAsString(polygon);
        assertTrue(validateJSON(json, "gsr/1.0/polygon.json"));
    }

    public void testEnvelopeSchema() throws Exception {
        SpatialReference spatialReference = new SpatialReferenceWKID(4326);
        Envelope envelope = new Envelope(-77, 39.0, -78, 40.0, spatialReference);
        String json = mapper.writeValueAsString(envelope);
        assertTrue(validateJSON(json, "gsr/1.0/envelope.json"));
    }

    public void testGeometryArraySchema() throws Exception {
        SpatialReference spatialReference = new SpatialReferenceWKID(4326);
        Point point = new Point(-77, 39.5, spatialReference);
        Point point2 = new Point(-77.4, 40.5, spatialReference);
        Geometry[] geometries = new Geometry[2];
        geometries[0] = point;
        geometries[1] = point2;
        GeometryArray array = new GeometryArray(GeometryType.GeometryPoint, geometries,
                spatialReference);
        String json = mapper.writeValueAsString(array);
        assertTrue(validateJSON(json, "gsr/1.0/geometries.json"));
    }

    public void testSpatialReferenceWKIDSchema() throws Exception {
        SpatialReference spatialReference = new SpatialReferenceWKID(4326);
        String json = mapper.writeValueAsString(spatialReference);
        assertTrue(validateJSON(json, "gsr/1.0/spatialreference.json"));
    }

    public void testWrongSchema() throws Exception {
        Point point = new Point(-77, 39.5, new SpatialReferenceWKID(4326));
        String json = mapper.writeValueAsString(point);
        assertFalse(validateJSON(json, "gsr/1.0/envelope.json"));
    }

}
