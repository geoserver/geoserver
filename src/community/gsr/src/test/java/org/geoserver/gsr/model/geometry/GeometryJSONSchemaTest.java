/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr.model.geometry;

import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.geoserver.gsr.JsonSchemaTest;
import org.geoserver.gsr.api.GeoServicesJacksonJsonConverter;
import org.geoserver.gsr.translate.geometry.GeometryEncoder;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;

/**
 * JSON Schema validation class. Validation is possible with local files, with the following
 * modifications: - All $ref elements need to point to a local JSON Schema - The $id element with
 * absolute URI needs to be removed from parent Schema files
 *
 * @author Juan Marin, OpenGeo
 */
public class GeometryJSONSchemaTest extends JsonSchemaTest {
    public GeometryJSONSchemaTest() {
        super();
    }

    private static final GeometryFactory geometries = new GeometryFactory();

    @Test
    public void testPointSchema() throws Exception {
        org.locationtech.jts.geom.Point point = geometries.createPoint(new Coordinate(77, 39.5));
        String json = representationToJson(new GeometryEncoder().toRepresentation(point, null));
        assertTrue(validateJSON(json, "gsr/1.0/point.json"));
    }

    @Test
    public void testMultipointSchema() throws Exception {
        Coordinate c1 = new Coordinate(-97.06138, 32.837);
        Coordinate c2 = new Coordinate(-97.06133, 32.836);
        Coordinate c3 = new Coordinate(-97.06124, 32.834);
        Coordinate c4 = new Coordinate(-97.06127, 32.832);
        Coordinate[] coords = {c1, c2, c3, c4};
        org.locationtech.jts.geom.MultiPoint mpoint =
                geometries.createMultiPoint(new CoordinateArraySequence(coords));
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

        Coordinate[] path1 = {c1, c2, c3};
        Coordinate[] path2 = {c3, c4, c5};

        org.locationtech.jts.geom.LineString[] lineStrings =
                new org.locationtech.jts.geom.LineString[] {
                    geometries.createLineString(path1), geometries.createLineString(path2)
                };

        org.locationtech.jts.geom.MultiLineString polyline =
                geometries.createMultiLineString(lineStrings);

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

        Coordinate[] ring1 = {c1, c2, c3, c4, c5, c1};

        Coordinate[] ring2 = {c2, c3, c4, c2};

        org.locationtech.jts.geom.LinearRing shell = geometries.createLinearRing(ring1);
        org.locationtech.jts.geom.LinearRing hole = geometries.createLinearRing(ring2);
        org.locationtech.jts.geom.LinearRing[] holes =
                new org.locationtech.jts.geom.LinearRing[] {hole};

        // double[][][] rings = { ring1, ring2 };
        // Polygon polygon = new Polygon(rings, spatialReference);
        org.locationtech.jts.geom.Polygon polygon = geometries.createPolygon(shell, holes);
        String json = representationToJson(new GeometryEncoder().toRepresentation(polygon, null));
        assertTrue(validateJSON(json, "gsr/1.0/polygon.json"));
    }

    @Test
    public void testEnvelopeSchema() throws Exception {
        org.locationtech.jts.geom.Envelope envelope =
                new org.locationtech.jts.geom.Envelope(-77, 39.0, -78, 40.0);
        String json = GeometryEncoder.toJson(envelope);
        assertTrue(validateJSON(json, "gsr/1.0/envelope.json"));
    }

    @Test
    public void testGeometryArraySchema() throws Exception {
        org.locationtech.jts.geom.Point point =
                geometries.createPoint(new org.locationtech.jts.geom.Coordinate(-77, 39.5));
        org.locationtech.jts.geom.Point point2 =
                geometries.createPoint(new org.locationtech.jts.geom.Coordinate(-77.4, 40.5));
        org.locationtech.jts.geom.GeometryCollection collection =
                geometries.createGeometryCollection(
                        new org.locationtech.jts.geom.Geometry[] {point, point2});
        String json =
                representationToJson(new GeometryEncoder().toRepresentation(collection, null));
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
