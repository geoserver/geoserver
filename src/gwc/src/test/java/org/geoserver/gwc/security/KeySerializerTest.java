/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.security;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.geotools.api.filter.Filter;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.referencing.CRS;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

public class KeySerializerTest {

    static final GeometryFactory GF = new GeometryFactory();

    @Test
    public void testFilterNormalization() throws Exception {
        // literal-left swap: "13 = population" and "population = 13" must produce the same key
        Filter a = ECQL.toFilter("13 = population");
        Filter b = ECQL.toFilter("population = 13");
        assertEquals(AccessLimitsKeyBuilder.serializeFilter(b), AccessLimitsKeyBuilder.serializeFilter(a));
    }

    @Test
    public void testFilterEcql() throws Exception {
        Filter f = ECQL.toFilter("population > 1000");
        String key = AccessLimitsKeyBuilder.serializeFilter(f);
        // round-trip: re-parsing the key must produce a semantically equal filter
        assertEquals(f, ECQL.toFilter(key));
    }

    @Test
    public void testGeometryNorm() {
        // two rings with same vertices in different order -> same key after norm()
        Coordinate[] cw = {
            new Coordinate(0, 0), new Coordinate(0, 1), new Coordinate(1, 1), new Coordinate(1, 0), new Coordinate(0, 0)
        };
        Coordinate[] ccw = {
            new Coordinate(0, 0), new Coordinate(1, 0), new Coordinate(1, 1), new Coordinate(0, 1), new Coordinate(0, 0)
        };
        Geometry a = GF.createPolygon(GF.createLinearRing(cw));
        Geometry b = GF.createPolygon(GF.createLinearRing(ccw));
        assertEquals(AccessLimitsKeyBuilder.serializeGeometry(a), AccessLimitsKeyBuilder.serializeGeometry(b));
    }

    @Test
    public void testCrsUserData() throws Exception {
        CoordinateReferenceSystem crs = CRS.decode("EPSG:4326");
        Geometry geom = GF.createPoint(new Coordinate(10, 20));
        geom.setUserData(crs);
        String key = AccessLimitsKeyBuilder.serializeGeometry(geom);
        assertThat(key, startsWith("EPSG:4326:"));
    }

    @Test
    public void testCrsSrid() {
        Geometry geom = GF.createPoint(new Coordinate(10, 20));
        geom.setSRID(3857);
        String key = AccessLimitsKeyBuilder.serializeGeometry(geom);
        assertThat(key, startsWith("EPSG:3857:"));
    }

    @Test
    public void testNoCrs() {
        Geometry geom = GF.createPoint(new Coordinate(10, 20));
        // SRID defaults to 0, no userData
        String key = AccessLimitsKeyBuilder.serializeGeometry(geom);
        assertEquals("POINT (10 20)", key);
    }

    @Test
    public void testCrsUserDataWins() throws Exception {
        // userData CRS wins; SRID is ignored when userData is set
        CoordinateReferenceSystem crs = CRS.decode("EPSG:4326");
        Geometry geom = GF.createPoint(new Coordinate(10, 20));
        geom.setUserData(crs);
        geom.setSRID(3857);
        String key = AccessLimitsKeyBuilder.serializeGeometry(geom);
        assertThat(key, startsWith("EPSG:4326:"));
    }

    @Test
    public void testDifferentCrs() throws Exception {
        Geometry geom4326 = GF.createPoint(new Coordinate(10, 20));
        geom4326.setSRID(4326);
        Geometry geom3857 = GF.createPoint(new Coordinate(10, 20));
        geom3857.setSRID(3857);
        assertNotEquals(
                "same WKT in different CRS must produce different keys",
                AccessLimitsKeyBuilder.serializeGeometry(geom4326),
                AccessLimitsKeyBuilder.serializeGeometry(geom3857));
    }
}
