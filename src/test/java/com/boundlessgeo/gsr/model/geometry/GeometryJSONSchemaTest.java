/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.model.geometry;

import static org.junit.Assert.assertTrue;

import com.boundlessgeo.gsr.translate.geometry.GeometryEncoder;
import org.junit.Test;

import com.boundlessgeo.gsr.JsonSchemaTest;
import com.boundlessgeo.gsr.api.GeoServicesJacksonJsonConverter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

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

    private static final GeometryFactory geometries = new GeometryFactory();

    @Test
    public void testPointSchema() throws Exception {
        com.vividsolutions.jts.geom.Point point = geometries.createPoint(new Coordinate(77, 39.5));
        String json = representationToJson(new GeometryEncoder().toRepresentation(point, null));
        assertTrue(validateJSON(json, "gsr/1.0/point.json"));
    }

    @Test
    public void testMultipointSchema() throws Exception {
        Coordinate c1 = new Coordinate(-97.06138, 32.837);
        Coordinate c2 = new Coordinate(-97.06133, 32.836);
        Coordinate c3 = new Coordinate(-97.06124, 32.834);
        Coordinate c4 = new Coordinate(-97.06127, 32.832);
        Coordinate[] coords = { c1, c2, c3, c4 };
        com.vividsolutions.jts.geom.MultiPoint mpoint = geometries.createMultiPoint(coords);
        String json = representationToJson(new GeometryEncoder().toRepresentation(mpoint, null));
        assertTrue(validateJSON(json, "gsr/1.0/multipoint.json"));
    }

    @Test
    public void testPolylineSchema() throws Exception {
        Coordinate c1 = new Coordinate(0, 0);
        Coordinate c2 = new Coordinate(0, 1);
        Coordinate c3 = new Coordinate(1, 1);
        Coordinate c4 = new Coordinate(1, 0);
        Coordinate c5 = new Coordinate(1, 2);

        Coordinate[] path1 = { c1, c2, c3 };
        Coordinate[] path2 = { c3, c4, c5 };

        com.vividsolutions.jts.geom.LineString[] lineStrings = new com.vividsolutions.jts.geom.LineString[] {
            geometries.createLineString(path1),
            geometries.createLineString(path2)
        };

        com.vividsolutions.jts.geom.MultiLineString polyline = geometries.createMultiLineString(lineStrings);

        String json = representationToJson(new GeometryEncoder().toRepresentation(polyline, null));
        assertTrue(validateJSON(json, "gsr/1.0/polyline.json"));
    }

    @Test
    public void testPolygonSchema() throws Exception {
        Coordinate c1 = new Coordinate(0, 0);
        Coordinate c2 = new Coordinate(0, 1);
        Coordinate c3 = new Coordinate(1, 1);
        Coordinate c4 = new Coordinate(1, 0);
        Coordinate c5 = new Coordinate(1, 2);

        Coordinate[] ring1 = { c1, c2, c3, c4, c5, c1 };

        Coordinate[] ring2 = { c2, c3, c4, c2 };

        com.vividsolutions.jts.geom.LinearRing shell = geometries.createLinearRing(ring1);
        com.vividsolutions.jts.geom.LinearRing hole = geometries.createLinearRing(ring2);
        com.vividsolutions.jts.geom.LinearRing[] holes = new com.vividsolutions.jts.geom.LinearRing[] { hole };

        // double[][][] rings = { ring1, ring2 };
        // Polygon polygon = new Polygon(rings, spatialReference);
        com.vividsolutions.jts.geom.Polygon polygon = geometries.createPolygon(shell, holes);
        String json = representationToJson(new GeometryEncoder().toRepresentation(polygon, null));
        assertTrue(validateJSON(json, "gsr/1.0/polygon.json"));
    }

    @Test
    public void testEnvelopeSchema() throws Exception {
        com.vividsolutions.jts.geom.Envelope envelope = new com.vividsolutions.jts.geom.Envelope(-77, 39.0, -78, 40.0);
        String json = GeometryEncoder.toJson(envelope);
        assertTrue(validateJSON(json, "gsr/1.0/envelope.json"));
    }

    @Test
    public void testGeometryArraySchema() throws Exception {
        com.vividsolutions.jts.geom.Point point = geometries.createPoint(new com.vividsolutions.jts.geom.Coordinate(-77, 39.5));
        com.vividsolutions.jts.geom.Point point2 = geometries.createPoint(new com.vividsolutions.jts.geom.Coordinate(-77.4, 40.5));
        com.vividsolutions.jts.geom.GeometryCollection collection = geometries.createGeometryCollection(new com.vividsolutions.jts.geom.Geometry [] { point, point2 });
        String json = representationToJson(new GeometryEncoder().toRepresentation(collection, null));
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
//        assertFalse(validateJSON(json, "gsr/1.0/envelope.json"));
    }

    private String representationToJson(Object obj) throws JsonProcessingException {
        return new GeoServicesJacksonJsonConverter().getMapper().writeValueAsString(obj);
    }
}
