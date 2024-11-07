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
        assertEquals("string", stringSchema.getTitle());

        Schema integerSchema = QueryablesBuilder.getSchema(Integer.class);
        assertEquals("integer", integerSchema.getType());
        assertEquals("integer", integerSchema.getTitle());

        Schema longSchema = QueryablesBuilder.getSchema(Long.class);
        assertEquals("integer", longSchema.getType());
        assertEquals("integer", longSchema.getTitle());

        Schema doubleSchema = QueryablesBuilder.getSchema(Double.class);
        assertEquals("number", doubleSchema.getType());
        assertEquals("number", doubleSchema.getTitle());

        Schema floatSchema = QueryablesBuilder.getSchema(Float.class);
        assertEquals("number", floatSchema.getType());
        assertEquals("number", floatSchema.getTitle());

        Schema booleanSchema = QueryablesBuilder.getSchema(Boolean.class);
        assertEquals("boolean", booleanSchema.getType());
        assertEquals("boolean", booleanSchema.getTitle());

        Schema timeSchema = QueryablesBuilder.getSchema(java.sql.Time.class);
        assertEquals("string", timeSchema.getType());
        assertEquals("time", timeSchema.getFormat());
        assertEquals("Time", timeSchema.getTitle());

        Schema dateSChema = QueryablesBuilder.getSchema(java.sql.Date.class);
        assertEquals("string", dateSChema.getType());
        assertEquals("date", dateSChema.getFormat());
        assertEquals("Date", dateSChema.getTitle());

        Schema dateTimeSchema = QueryablesBuilder.getSchema(java.util.Date.class);
        assertEquals("string", dateTimeSchema.getType());
        assertEquals("date-time", dateTimeSchema.getFormat());
        assertEquals("DateTime", dateTimeSchema.getTitle());

        Schema pointSchema = QueryablesBuilder.getSchema(Point.class);
        assertEquals("geometry-point", pointSchema.getFormat());
        assertEquals("Point", pointSchema.getTitle());

        Schema multiPointSchema = QueryablesBuilder.getSchema(MultiPoint.class);
        assertEquals("geometry-multipoint", multiPointSchema.getFormat());
        assertEquals("MultiPoint", multiPointSchema.getTitle());

        Schema lineStringSchema = QueryablesBuilder.getSchema(LineString.class);
        assertEquals("geometry-linestring", lineStringSchema.getFormat());
        assertEquals("LineString", lineStringSchema.getTitle());

        Schema multiLineStringSchema = QueryablesBuilder.getSchema(MultiLineString.class);
        assertEquals("geometry-multilinestring", multiLineStringSchema.getFormat());
        assertEquals("MultiLineString", multiLineStringSchema.getTitle());

        Schema polygonSchema = QueryablesBuilder.getSchema(Polygon.class);
        assertEquals("geometry-polygon", polygonSchema.getFormat());
        assertEquals("Polygon", polygonSchema.getTitle());

        Schema multiPolygonSchema = QueryablesBuilder.getSchema(MultiPolygon.class);
        assertEquals("geometry-multipolygon", multiPolygonSchema.getFormat());
        assertEquals("MultiPolygon", multiPolygonSchema.getTitle());

        Schema geometrySchema = QueryablesBuilder.getSchema(Geometry.class);
        assertEquals("geometry-any", geometrySchema.getFormat());
        assertEquals("Any geometry", geometrySchema.getTitle());
    }
}
