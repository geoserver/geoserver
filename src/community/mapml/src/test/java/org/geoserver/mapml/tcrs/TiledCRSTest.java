/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml.tcrs;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.mapml.MapMLConstants;
import org.geoserver.mapml.tcrs.TiledCRS.TileComparator;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.operation.TransformException;

/** @author Peter.Rushforth@canada.ca */
public class TiledCRSTest {

    public TiledCRSTest() {}

    @Before
    public void setUp() {
        MapMLConstants.PAGESIZE = 100;
    }

    @Test
    public void testWGS84Projection() {
        TiledCRS wgs84 = new TiledCRS("WGS84");

        // a location
        LatLng latlng = new LatLng(45.398043D, -75.70683D);

        // the location in pixels
        Point projected = null;
        try {
            projected = wgs84.latLngToPoint(latlng, 17);
        } catch (MismatchedDimensionException | TransformException ex) {
            fail("Exception during project operation");
        }

        // reverse the process
        LatLng unprojected = null;
        try {
            unprojected = wgs84.pointToLatLng(projected, 17);
        } catch (MismatchedDimensionException | TransformException ex) {
            fail("Exception during unproject operation");
        }
        assertEquals(latlng.lng, unprojected.lng, 0.00000001);
        assertEquals(latlng.lat, unprojected.lat, 0.00000001);
    }

    @Test
    public void testOSMTTILEProjection() {
        TiledCRS osmtile = new TiledCRS("OSMTILE");

        // a location
        LatLng latlng = new LatLng(45.398043D, -75.70683D);

        // the location above measured off a Leaflet map:
        Point expected = new Point(9720828.0D, 12017718.0D);

        // the location in pixels calculated with proj4j
        Point actual = null;
        try {
            actual = osmtile.latLngToPoint(latlng, 17);
        } catch (MismatchedDimensionException | TransformException ex) {
            fail("Exception during project operation");
        }

        // 10cm seems close enough...
        assertEquals(expected.x, actual.x, 0.1);
        assertEquals(expected.y, actual.y, 0.1);

        // reverse the process
        LatLng unprojected = null;
        try {
            unprojected = osmtile.pointToLatLng(actual, 17);
        } catch (MismatchedDimensionException | TransformException ex) {
            fail("Exception during unproject operation");
        }
        assertEquals(latlng.lat, unprojected.lat, 0.00000001);
        assertEquals(latlng.lng, unprojected.lng, 0.00000001);
    }

    @Test
    public void testOSMTILESorting() {
        TiledCRS osmtile = new TiledCRS("OSMTILE");

        // this query has 8 tiles in it (at zoom=15)
        LatLngBounds query =
                new LatLngBounds(
                        new LatLng(45.39079543037812, -75.7205629348755),
                        new LatLng(45.40525984235134, -75.693097114563));
        List<TileCoordinates> tiles = osmtile.getTilesForExtent(query, 15, 0);
        assertEquals("Expect 8 tiles for extent: ", 8, tiles.size());

        ArrayList<TileCoordinates> expectedOrder = new ArrayList<>();
        // observed order in default Leaflet results:
        expectedOrder.add(new TileCoordinates(9492, 11736, 15));
        expectedOrder.add(new TileCoordinates(9493, 11736, 15));
        expectedOrder.add(new TileCoordinates(9492, 11735, 15));
        expectedOrder.add(new TileCoordinates(9493, 11735, 15));
        expectedOrder.add(new TileCoordinates(9491, 11736, 15));
        expectedOrder.add(new TileCoordinates(9494, 11736, 15));
        expectedOrder.add(new TileCoordinates(9491, 11735, 15));
        expectedOrder.add(new TileCoordinates(9494, 11735, 15));

        Bounds pb = null;
        try {
            pb = osmtile.getPixelBounds(query, 15);
        } catch (MismatchedDimensionException | TransformException ex) {
            fail("Exception during project operation");
        }
        Point centre = pb.getCentre().divideBy(256);
        Collections.sort(expectedOrder, osmtile.new TileComparator(centre));

        ListIterator<TileCoordinates> li = tiles.listIterator();
        while (li.hasNext()) {
            int index = li.nextIndex();
            TileCoordinates actual = li.next();
            TileCoordinates expected = expectedOrder.get(index);
            boolean coordinatesAreTheSame =
                    (expected.x == actual.x && expected.y == actual.y && expected.z == actual.z);
            assertTrue(coordinatesAreTheSame);
        }
    }

    @Test
    public void testOSMTILEPaging() {
        // now test the tiles returned for defined extents and scales
        TiledCRS osmtile = new TiledCRS("OSMTILE");

        osmtile.setPageSize(5);
        LatLngBounds query =
                new LatLngBounds(
                        new LatLng(45.39079543037812, -75.7205629348755),
                        new LatLng(45.40525984235134, -75.693097114563));
        List<TileCoordinates> tiles = osmtile.getTilesForExtent(query, 15, 0);
        assertEquals("Expect 5 tiles for first page of extent: ", 5, tiles.size());
        tiles = osmtile.getTilesForExtent(query, 15, 5);
        assertEquals("Expect 3 tiles for second page extent: ", 3, tiles.size());
    }

    @Test
    public void testOSMTILEScaleSet() {
        TiledCRS osmtile = new TiledCRS("OSMTILE");
        // assure that there are at least 18 zoom levels.  Uncertain how many
        // would be standard.  Seems that most Web maps go up to 19 levels or so...
        // probably would be wise to go a bit higher for OSMTILE
    }

    @Ignore
    public void testOSMTILEBounds() {
        // assure that the limits of the tiled projection are respected at different
        // zoom levels.
        // what does *respected* mean???  wrapped? exception?  empty result?

    }

    @Test
    public void testCBMLCCProjection() {
        TiledCRS cbmlcc = new TiledCRS("CBMTILE");

        // a location
        LatLng latlng = new LatLng(45.398043, -75.70683);

        // the location above measured off a Proj4Leaflet map at zoom=17:
        Point expected = new Point(7810966.680052839, 8527151.324584836);

        // the location in meters calculated with proj4j and scaled by this class
        Point actual = null;
        try {
            actual = cbmlcc.latLngToPoint(latlng, 17);
        } catch (MismatchedDimensionException | TransformException ex) {
            fail("Exception during project operation");
        }

        // one centimeter seems close enough
        assertEquals(expected.x, actual.x, 0.01);
        assertEquals(expected.y, actual.y, 0.01);

        // reverse the process
        LatLng unprojected = null;
        try {
            unprojected = cbmlcc.pointToLatLng(actual, 17);
        } catch (MismatchedDimensionException | TransformException ex) {
            Logger.getLogger(TiledCRSTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        assertEquals(latlng.lat, unprojected.lat, 0.00000001);
        assertEquals(latlng.lng, unprojected.lng, 0.00000001);
    }

    @Test
    public void testCBMLCCSorting() {

        TiledCRS cbmlcc = new TiledCRS("CBMTILE");

        // this query has 9 tiles in it (at zoom=17)
        LatLngBounds query =
                new LatLngBounds(
                        new LatLng(45.39257399473906, -75.72876150576825),
                        new LatLng(45.40352437491144, -75.68484627676823));
        List<TileCoordinates> tiles = cbmlcc.getTilesForExtent(query, 17, 0);
        assertEquals("Expect 9 tiles for extent: ", 9, tiles.size());

        ArrayList<TileCoordinates> expectedOrder = new ArrayList<>();
        // observed order in Proj4Leaflet results:
        expectedOrder.add(new TileCoordinates(30511, 33309, 17));
        expectedOrder.add(new TileCoordinates(30511, 33308, 17));
        expectedOrder.add(new TileCoordinates(30510, 33309, 17));
        expectedOrder.add(new TileCoordinates(30512, 33309, 17));
        expectedOrder.add(new TileCoordinates(30511, 33310, 17));
        expectedOrder.add(new TileCoordinates(30510, 33308, 17));
        expectedOrder.add(new TileCoordinates(30512, 33308, 17));
        expectedOrder.add(new TileCoordinates(30510, 33310, 17));
        expectedOrder.add(new TileCoordinates(30512, 33310, 17));

        Bounds pb = null;
        try {
            pb = cbmlcc.getPixelBounds(query, 17);
        } catch (MismatchedDimensionException | TransformException ex) {
            fail("Exception during project operation");
        }
        Point centre = pb.getCentre().divideBy(256);
        Collections.sort(expectedOrder, cbmlcc.new TileComparator(centre));

        ListIterator<TileCoordinates> li = tiles.listIterator();
        while (li.hasNext()) {
            int index = li.nextIndex();
            TileCoordinates actual = li.next();
            TileCoordinates expected = expectedOrder.get(index);
            boolean coordinatesAreTheSame =
                    (expected.x == actual.x && expected.y == actual.y && expected.z == actual.z);
            assertTrue(coordinatesAreTheSame);
        }
    }

    @Test
    public void testTileRounding() {
        TiledCRS cbmtile = new TiledCRS("CBMTILE");
        // create a rectangle in EPSG:3978 units
        Point pt1 = new Point(-3262924.7, -1554977.6);
        Point pt2 = new Point(3823954.0, 4046262.8);
        Bounds extent = new Bounds(pt1, pt2);
        Bounds pb = cbmtile.getTileRoundedPixelBoundsForExtent(extent, 0);
        Point expectedMin = new Point(768, 768);
        Point expectedMax = new Point(1024, 1280);
        assertTrue(pb.min.distanceTo(expectedMin) < 0.00001);
        assertTrue(pb.max.distanceTo(expectedMax) < 0.00001);
    }

    @Test
    public void testGetTileOrPixelBoundsForProjectedBounds() {
        TiledCRS tcrs = new TiledCRS("OSMTILE");
        boundsTest(tcrs);
        tcrs = new TiledCRS("CBMTILE");
        boundsTest(tcrs);
        tcrs = new TiledCRS("APSTILE");
        boundsTest(tcrs);
        tcrs = new TiledCRS("WGS84");
        boundsTest(tcrs);
    }

    private void boundsTest(TiledCRS tcrs) {
        double metersPerPixelAtZoom18 = 1.0D / tcrs.getScales()[18];
        Point maxProjectedPoint = tcrs.getBounds().max;
        Point projectedOrigin = tcrs.getOrigin();
        Bounds projectedBounds = new Bounds(projectedOrigin, maxProjectedPoint);
        double tcrsWidthInMeters = projectedBounds.getMax().x - projectedBounds.getMin().x;
        double tcrsHeightInMeters = projectedBounds.getMax().y - projectedBounds.getMin().y;
        long whatMaxPixelXShouldBe = (long) (tcrsWidthInMeters / metersPerPixelAtZoom18);
        long whatMaxPixelYShouldBe = (long) (tcrsHeightInMeters / metersPerPixelAtZoom18);
        Bounds pxb = tcrs.getPixelBoundsForProjectedBounds(18, projectedBounds);
        int maxPixelX = (int) pxb.getMax().x;
        int maxPixelY = (int) pxb.getMax().y;
        assertEquals(maxPixelX, whatMaxPixelXShouldBe);
        assertEquals(maxPixelY, whatMaxPixelYShouldBe);
        long whatMaxTileColShouldBe = whatMaxPixelXShouldBe / tcrs.getTileSize();
        long whatMaxTileRowShouldBe = whatMaxPixelYShouldBe / tcrs.getTileSize();
        Bounds tb = tcrs.getTileBoundsForProjectedBounds(18, projectedBounds);
        long maxCol = (long) tb.getMax().x;
        long maxRow = (long) tb.getMax().y;
        assertEquals(maxCol, whatMaxTileColShouldBe);
        assertEquals(maxRow, whatMaxTileRowShouldBe);
    }
}
