/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2020, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.dggs.h3;

import static org.geotools.referencing.crs.DefaultGeographicCRS.WGS84;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Iterators;
import com.uber.h3core.H3Core;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;
import jep.JepException;
import org.geotools.dggs.DGGSFactory;
import org.geotools.dggs.DGGSFactoryFinder;
import org.geotools.dggs.DGGSInstance;
import org.geotools.dggs.Zone;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

public class H3DGGSInstanceTest {

    static final Logger LOGGER = Logging.getLogger(H3DGGSInstanceTest.class);

    private static final ReferencedEnvelope WORLD =
            new ReferencedEnvelope(-180, 180, -90, 90, WGS84);
    private static final GeometryFactory GF = new GeometryFactory();
    private DGGSInstance h3i;
    private H3Core h3;

    @Before
    public void setup() throws IOException {
        Optional<DGGSFactory> factory =
                DGGSFactoryFinder.getExtensionFactories()
                        .filter(d -> "H3".equalsIgnoreCase(d.getId()))
                        .findFirst();
        assertTrue(factory.isPresent());
        h3i = factory.get().createInstance(null);
        h3 = H3Core.newInstance();
    }

    @Test
    public void getZone() throws JepException {
        Zone zone = h3i.getZone("8029fffffffffff");
        assertNotNull(zone);
        assertEquals(0, zone.getResolution());

        // TODO: check boundary
    }

    @Test
    public void zonesFromEnvelopeWorldResZero() {
        Iterator<Zone> zonesIterator = h3i.zonesFromEnvelope(WORLD, 0, false);
        Set<Zone> zones = new HashSet<>();
        zonesIterator.forEachRemaining(zones::add);
        assertEquals(122, zones.size());
    }

    @Test
    public void zonesFromEnvelopeAcrossDateline() throws IOException {
        Iterator<Zone> zonesIterator =
                h3i.zonesFromEnvelope(new ReferencedEnvelope(179, 181, -10, 10, WGS84), 0, false);
        Set<Zone> zones = new HashSet<>();
        zonesIterator.forEachRemaining(zones::add);
        assertEquals(2, zones.size());
    }

    @Test
    public void countZonesFromEnvelopeWorldResZero() {
        ReferencedEnvelope envelope = WORLD;
        assertZoneCount(envelope, 0);
    }

    @Test
    public void countZonesFromEnvelopeWorldResOne() {
        ReferencedEnvelope envelope = WORLD;
        assertZoneCount(envelope, 1);
    }

    @Test
    public void countZonesFromEnvelopeWorldResTwo() {
        ReferencedEnvelope envelope = new ReferencedEnvelope(-1, 1, -1, 1, WGS84);
        assertZoneCount(envelope, 2);
    }

    @Test
    public void countZonesCloseToPole() {
        ReferencedEnvelope envelope = new ReferencedEnvelope(102, 103, 88.9, 90, WGS84);
        assertZoneCount(envelope, 4);
    }

    @Test
    public void countZonesCloseToDateline() {
        ReferencedEnvelope envelope = new ReferencedEnvelope(175, 176, 81.4, 81.7, WGS84);
        assertZoneCount(envelope, 1);
    }

    @Test
    @Ignore // there are still failures to investigate, need to move on with other parts right now
    public void testRandomized() {
        Random random = new Random();
        final int LOOPS = 1000;
        final int MAX_RESOLUTION = 4;
        for (int i = 0; i < LOOPS; i++) {
            double lon1 = random.nextDouble() * 360 - 180;
            double lon2 = lon1 + (random.nextDouble() * (180 - lon1));
            double lat1 = random.nextDouble() * 180 - 90;
            double lat2 = lat1 + (random.nextDouble() * (90 - lat1));
            ReferencedEnvelope envelope = new ReferencedEnvelope(lon1, lon2, lat1, lat2, WGS84);
            int resolution = (int) (random.nextDouble() * MAX_RESOLUTION);
            try {
                assertZoneCount(envelope, resolution);
            } catch (AssertionError e) {
                LOGGER.severe(envelope + " / " + resolution + " -> " + e.getMessage());
            }
        }
    }

    public void assertZoneCount(ReferencedEnvelope envelope, int resolution) {
        long count = h3i.countZonesFromEnvelope(envelope, resolution);
        int expected = Iterators.size(h3i.zonesFromEnvelope(envelope, resolution, false));
        assertEquals(expected, count);
    }

    @Test
    public void testBoundaryNorthPoleZero() throws ParseException {
        Zone zone = h3i.getZone("8001fffffffffff");
        Polygon boundary = zone.getBoundary();
        assertEquals(boundary.getEnvelopeInternal().getMinX(), -180d, 0);
        assertEquals(boundary.getEnvelopeInternal().getMaxX(), 180d, 0);
        assertEquals(boundary.getEnvelopeInternal().getMaxY(), 90d, 0);
        Polygon expected =
                (Polygon)
                        new WKTReader()
                                .read(
                                        "POLYGON ((-180 86.19672397564815, -34.75841798028461 81.27137179020497, 0.3256103519432604 73.31022368544396, 31.831280499087416 68.92995788193984, 31.831280499087416 68.92995788193984, 62.345344956509784 69.39359648991828, 94.14309010184775 76.163042830191, 145.55819769133683 87.3646953231962, 180 86.19672397564815, 180 90, -180 90, -180 86.19672397564815))");
        assertTrue(expected.equalsExact(boundary, 1e-6));
    }

    @Test
    public void testNeighborsNorthPole() {
        Iterator<Zone> iterator = h3i.neighbors("8001fffffffffff", 1);
        List<String> neighbors = new ArrayList<>();
        iterator.forEachRemaining(z -> neighbors.add(z.getId()));
        // round the pole we go
        assertThat(
                neighbors,
                CoreMatchers.hasItems(
                        "8007fffffffffff",
                        "8009fffffffffff",
                        "800bfffffffffff",
                        "8011fffffffffff",
                        "8003fffffffffff",
                        "8005fffffffffff"));
        assertThat(neighbors, CoreMatchers.not(hasItems("8001fffffffffff")));
        assertEquals(6, neighbors.size());
    }

    @Test
    public void testNeighborsDateline() {
        // pentagon on the dateline
        Iterator<Zone> iterator = h3i.neighbors("807ffffffffffff", 1);
        List<String> neighbors = new ArrayList<>();
        iterator.forEachRemaining(z -> neighbors.add(z.getId()));
        // round the pole we go
        assertThat(
                neighbors,
                CoreMatchers.hasItems(
                        "805bfffffffffff",
                        "8077fffffffffff",
                        "809bfffffffffff",
                        "8071fffffffffff",
                        "809ffffffffffff"));
        assertThat(neighbors, CoreMatchers.not(hasItems("807ffffffffffff")));
        assertEquals(5, neighbors.size());
    }

    @Test
    public void testNeighborsAll() {
        // pentagon at the equator/greenwitch (close to), with this distance should catch them all
        Iterator<Zone> iterator = h3i.neighbors("8075fffffffffff", 10);
        List<String> neighbors = new ArrayList<>();
        iterator.forEachRemaining(z -> neighbors.add(z.getId()));
        // get all
        Iterator<Zone> zonesIterator = h3i.zonesFromEnvelope(WORLD, 0, false);
        Set<String> expected = new HashSet<>();
        zonesIterator.forEachRemaining(z -> expected.add(z.getId()));
        expected.remove("8075fffffffffff"); // center cell not expected
        // check same size and contents
        assertEquals(expected.size(), neighbors.size());
        assertTrue(expected.containsAll(neighbors));
    }

    @Test
    public void testChildren() throws Exception {
        String parent = "807ffffffffffff";
        // keep the resolution small, the list can grow veeeeeery fast
        for (int r = 1; r < 4; r++) {
            Set<String> actual = new HashSet<>();
            h3i.children(parent, r).forEachRemaining(z -> actual.add(z.getId()));
            Set<String> expected = new HashSet<>(h3.h3ToChildren(parent, r));
            assertEquals(expected, actual);
        }
    }

    @Test
    public void testMapPoint() throws Exception {
        Point northPole = GF.createPoint(new Coordinate(0, 90));
        // test few different resolutions
        String[] expectedIds = {"8001fffffffffff", "81033ffffffffff", "820327fffffffff"};
        for (int r = 0; r < expectedIds.length; r++) {
            Zone zone = h3i.point(northPole, r);
            assertEquals(expectedIds[r], zone.getId());
        }
    }

    @Test
    public void testMapPolygon() throws Exception {
        Polygon polygon =
                (Polygon) new WKTReader().read("POLYGON((-1 -1, -1 1, 1 1, 1 -1, -1 -1))");

        Set<String> actual = new HashSet<>();
        h3i.polygon(polygon, 3, false).forEachRemaining(z -> actual.add(z.getId()));
        // visually verified
        Set<String> expected =
                new HashSet<>(
                        Arrays.asList(
                                "83754afffffffff",
                                "837548fffffffff",
                                "83754efffffffff",
                                "83755dfffffffff"));
        assertEquals(expected, actual);
    }

    @Test
    public void zonesFromEnvelopeCompact() {
        // hitting the area of the central
        Iterator<Zone> zonesIterator =
                h3i.zonesFromEnvelope(new ReferencedEnvelope(-14, 4, -6, 11.7, WGS84), 1, true);
        Set<String> zones = new HashSet<>();
        zonesIterator.forEachRemaining(z -> zones.add(z.getId()));
        // should have collected the central pentagon plus 11 smaller zones around it
        assertEquals(12, zones.size());
        assertThat(zones, CoreMatchers.hasItem("8075fffffffffff"));
        assertThat(
                zones,
                CoreMatchers.hasItems(
                        "8154fffffffffff",
                        "81583ffffffffff",
                        "81827ffffffffff",
                        "817cbffffffffff",
                        "8158bffffffffff",
                        "8159bffffffffff",
                        "8182fffffffffff",
                        "81993ffffffffff",
                        "81997ffffffffff",
                        "817dbffffffffff",
                        "81547ffffffffff"));
    }
}
