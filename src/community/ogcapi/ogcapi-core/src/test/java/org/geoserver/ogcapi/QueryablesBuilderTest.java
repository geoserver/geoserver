/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import static org.junit.Assert.assertEquals;

import io.swagger.v3.oas.models.media.Schema;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

public class QueryablesBuilderTest {

    @Test
    public void testGetSchema() throws Exception {
        Schema stringSchema = QueryablesBuilder.getSchema(String.class);
        assertEquals("string", stringSchema.getType());
        assertEquals("string", stringSchema.getDescription());

        Schema integerSchema = QueryablesBuilder.getSchema(Integer.class);
        assertEquals("integer", integerSchema.getType());
        assertEquals("integer", integerSchema.getDescription());

        Schema longSchema = QueryablesBuilder.getSchema(Long.class);
        assertEquals("integer", longSchema.getType());
        assertEquals("integer", longSchema.getDescription());

        Schema doubleSchema = QueryablesBuilder.getSchema(Double.class);
        assertEquals("number", doubleSchema.getType());
        assertEquals("number", doubleSchema.getDescription());

        Schema floatSchema = QueryablesBuilder.getSchema(Float.class);
        assertEquals("number", floatSchema.getType());
        assertEquals("number", floatSchema.getDescription());

        Schema booleanSchema = QueryablesBuilder.getSchema(Boolean.class);
        assertEquals("boolean", booleanSchema.getType());
        assertEquals("boolean", booleanSchema.getDescription());

        Schema timeSchema = QueryablesBuilder.getSchema(java.sql.Time.class);
        assertEquals("string", timeSchema.getType());
        assertEquals("time", timeSchema.getFormat());
        assertEquals("Time", timeSchema.getDescription());

        Schema dateSChema = QueryablesBuilder.getSchema(java.sql.Date.class);
        assertEquals("string", dateSChema.getType());
        assertEquals("date", dateSChema.getFormat());
        assertEquals("Date", dateSChema.getDescription());

        Schema dateTimeSchema = QueryablesBuilder.getSchema(java.util.Date.class);
        assertEquals("string", dateTimeSchema.getType());
        assertEquals("date-time", dateTimeSchema.getFormat());
        assertEquals("DateTime", dateTimeSchema.getDescription());

        Schema pointSchema = QueryablesBuilder.getSchema(Point.class);
        assertEquals(QueryablesBuilder.POINT_SCHEMA_REF, pointSchema.get$ref());
        assertEquals("Point", pointSchema.getDescription());

        Schema multiPointSchema = QueryablesBuilder.getSchema(MultiPoint.class);
        assertEquals(QueryablesBuilder.MULTIPOINT_SCHEMA_REF, multiPointSchema.get$ref());
        assertEquals("MultiPoint", multiPointSchema.getDescription());

        Schema lineStringSchema = QueryablesBuilder.getSchema(LineString.class);
        assertEquals(QueryablesBuilder.LINESTRING_SCHEMA_REF, lineStringSchema.get$ref());
        assertEquals("LineString", lineStringSchema.getDescription());

        Schema multiLineStringSchema = QueryablesBuilder.getSchema(MultiLineString.class);
        assertEquals(QueryablesBuilder.MULTILINESTRING_SCHEMA_REF, multiLineStringSchema.get$ref());
        assertEquals("MultiLineString", multiLineStringSchema.getDescription());

        Schema polygonSchema = QueryablesBuilder.getSchema(Polygon.class);
        assertEquals(QueryablesBuilder.POLYGON_SCHEMA_REF, polygonSchema.get$ref());
        assertEquals("Polygon", polygonSchema.getDescription());

        Schema multiPolygonSchema = QueryablesBuilder.getSchema(MultiPolygon.class);
        assertEquals(QueryablesBuilder.MULTIPOLYGON_SCHEMA_REF, multiPolygonSchema.get$ref());
        assertEquals("MultiPolygon", multiPolygonSchema.getDescription());

        Schema geometrySchema = QueryablesBuilder.getSchema(Geometry.class);
        assertEquals(QueryablesBuilder.GEOMETRY_SCHEMA_REF, geometrySchema.get$ref());
        assertEquals("Generic geometry", geometrySchema.getDescription());
    }
}
