/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.changeset;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import org.geowebcache.config.DefaultGridsets;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.GridSet;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.grid.GridSubsetFactory;
import org.junit.Test;

public class TilesIteratorTest {

    static GridSet WGS84 = new DefaultGridsets(false, false).worldEpsg4326();
    static GridSet WEB_MERCATOR = new DefaultGridsets(false, false).worldEpsg3857();

    @Test
    public void iterateTwoLevelWholeWorldWgs84() {
        GridSubset subset = GridSubsetFactory.createGridSubSet(WGS84, null, 0, 1);
        TileIterator iterator = new TileIterator(Arrays.asList(subset), 0, 1);
        assertIterator(
                iterator,
                // zoom level 0
                new long[] {0, 0, 0},
                new long[] {1, 0, 0},
                // zoom level one
                new long[] {0, 0, 1},
                new long[] {1, 0, 1},
                new long[] {2, 0, 1},
                new long[] {3, 0, 1},
                new long[] {0, 1, 1},
                new long[] {1, 1, 1},
                new long[] {2, 1, 1},
                new long[] {3, 1, 1});
    }

    @Test
    public void iterateTwoLevelWholeWorldWebMercator() {
        GridSubset subset = GridSubsetFactory.createGridSubSet(WEB_MERCATOR, null, 0, 1);
        TileIterator iterator = new TileIterator(Arrays.asList(subset), 0, 1);
        assertIterator(
                iterator,
                // zoom level 0
                new long[] {0, 0, 0},
                // zoom level one
                new long[] {0, 0, 1},
                new long[] {1, 0, 1},
                new long[] {0, 1, 1},
                new long[] {1, 1, 1});
    }

    @Test
    public void iterateTwoLevelSubset00Wgs84() {
        GridSubset subset =
                GridSubsetFactory.createGridSubSet(WGS84, new BoundingBox(-10, -10, 10, 10), 0, 1);
        TileIterator iterator = new TileIterator(Arrays.asList(subset), 0, 1);
        assertIterator(
                iterator,
                // zoom level 0
                new long[] {0, 0, 0},
                new long[] {1, 0, 0},
                // zoom level one
                new long[] {1, 0, 1},
                new long[] {2, 0, 1},
                new long[] {1, 1, 1},
                new long[] {2, 1, 1});
    }

    @Test
    public void iterateTwoLevelSubset00Wgs84DuplicateGridset() {
        GridSubset subset1 =
                GridSubsetFactory.createGridSubSet(WGS84, new BoundingBox(-10, -10, 10, 10), 0, 1);
        GridSubset subset2 = new GridSubset(subset1);
        TileIterator iterator = new TileIterator(Arrays.asList(subset1, subset2), 0, 1);
        assertIterator(
                iterator,
                // zoom level 0
                new long[] {0, 0, 0},
                new long[] {1, 0, 0},
                // zoom level one
                new long[] {1, 0, 1},
                new long[] {2, 0, 1},
                new long[] {1, 1, 1},
                new long[] {2, 1, 1});
    }

    @Test
    public void iterateTwoLevelTwoSubsetsOverlapYWebMercator() {
        // two bounds, partially overlapping in the y axis, but not really touching
        // remember that y is counted from the bottom
        // zoom level 0:
        // *
        // zoom level 1:
        // --
        // **
        // zoom level 2:
        // ----
        // ----
        // -***
        // -*--
        // zoom leve 3:
        // --------
        // --------
        // --------
        // --------
        // ----**--
        // -**-**--
        // -**-----
        // --------
        // Grid is roughly divided in squares of 5 million meters, but just roughly,
        // so we need to offset them a bit in order to avoid catching extra tiles
        GridSubset subset1 =
                GridSubsetFactory.createGridSubSet(
                        WEB_MERCATOR,
                        new BoundingBox(-9_900_000, -14_900_000, 0, -5_100_000),
                        0,
                        3);
        GridSubset subset2 =
                GridSubsetFactory.createGridSubSet(
                        WEB_MERCATOR, new BoundingBox(5_100_000, -9_900_000, 15_000_000, 0), 0, 3);
        TileIterator iterator = new TileIterator(Arrays.asList(subset1, subset2), 0, 3);
        assertIterator(
                iterator,
                // zoom level 0
                new long[] {0, 0, 0},
                // zoom level one
                new long[] {0, 0, 1},
                new long[] {1, 0, 1},
                // zoom level two
                new long[] {1, 0, 2},
                new long[] {1, 1, 2},
                new long[] {2, 1, 2},
                new long[] {3, 1, 2},
                // zoom level three
                new long[] {2, 1, 3},
                new long[] {3, 1, 3},
                new long[] {2, 2, 3},
                new long[] {3, 2, 3},
                new long[] {5, 2, 3},
                new long[] {6, 2, 3},
                new long[] {5, 3, 3},
                new long[] {6, 3, 3});
    }

    @Test
    public void iterateTwoLevelTwoSubsetsOverlapXWebMercator() {
        // two bounds, partially overlapping in the x axis, but not really touching
        // remember that y is counted from the bottom
        // zoom level 0:
        // *
        // zoom level 1:
        // **
        // *-
        // zoom level 2:
        // -**-
        // -**-
        // -*--
        // -*--
        // zoom leve 3:
        // --------
        // ---**---
        // ---**---
        // --------
        // --------
        // -**-----
        // -**-----
        // --------
        // Grid is roughly divided in squares of 5 million meters, but just roughly,
        // so we need to offset them a bit in order to avoid catching extra tiles
        GridSubset subset1 =
                GridSubsetFactory.createGridSubSet(
                        WEB_MERCATOR,
                        new BoundingBox(-9_900_000, -14_900_000, 0, -5_100_000),
                        0,
                        3);
        GridSubset subset2 =
                GridSubsetFactory.createGridSubSet(
                        WEB_MERCATOR,
                        new BoundingBox(-5_000_000, 5_100_000, 5_000_000, 14_900_000),
                        0,
                        3);
        TileIterator iterator = new TileIterator(Arrays.asList(subset1, subset2), 0, 3);
        assertIterator(
                iterator,
                // zoom level 0
                new long[] {0, 0, 0},
                // zoom level one
                new long[] {0, 0, 1},
                new long[] {0, 1, 1},
                new long[] {1, 1, 1},
                // zoom level two
                new long[] {1, 0, 2},
                new long[] {1, 1, 2},
                new long[] {1, 2, 2},
                new long[] {2, 2, 2},
                new long[] {1, 3, 2},
                new long[] {2, 3, 2},
                // zoom level three
                new long[] {2, 1, 3},
                new long[] {3, 1, 3},
                new long[] {2, 2, 3},
                new long[] {3, 2, 3},
                new long[] {3, 5, 3},
                new long[] {4, 5, 3},
                new long[] {3, 6, 3},
                new long[] {4, 6, 3});
    }

    @Test
    public void iterateTwoLevelTwoSubsetsOverlapXYWebMercator() {
        // two bounds, partially overlapping on both axis
        // zoom level 0:
        // *
        // zoom level 1:
        // **
        // *-
        // zoom level 2:
        // -**-
        // -**-
        // -*--
        // -*--
        // zoom leve 3:
        // --------
        // --------
        // --------
        // --------
        // --**----
        // -***----
        // -**-----
        // --------
        // Grid is roughly divided in squares of 5 million meters, but just roughly,
        // so we need to offset them a bit in order to avoid catching extra tiles
        GridSubset subset1 =
                GridSubsetFactory.createGridSubSet(
                        WEB_MERCATOR,
                        new BoundingBox(-9_900_000, -14_900_000, 0, -5_100_000),
                        0,
                        3);
        GridSubset subset2 =
                GridSubsetFactory.createGridSubSet(
                        WEB_MERCATOR, new BoundingBox(-5_000_000, -10_000_000, 5_000_000, 0), 0, 3);
        TileIterator iterator = new TileIterator(Arrays.asList(subset1, subset2), 0, 3);
        assertIterator(
                iterator,
                // zoom level 0
                new long[] {0, 0, 0},
                // zoom level one
                new long[] {0, 0, 1},
                new long[] {1, 0, 1},
                // zoom level two
                new long[] {1, 0, 2},
                new long[] {1, 1, 2},
                new long[] {2, 1, 2},
                // zoom level three
                new long[] {2, 1, 3},
                new long[] {3, 1, 3},
                new long[] {2, 2, 3},
                new long[] {3, 2, 3},
                new long[] {4, 2, 3},
                new long[] {3, 3, 3},
                new long[] {4, 3, 3});
    }

    @Test
    public void iterateOneLevelSouthEastCornerWgs84() {
        // y = 0 is the south-est row
        GridSubset subset =
                GridSubsetFactory.createGridSubSet(
                        WGS84, new BoundingBox(170, -90, 180, -80), 0, 3);
        TileIterator iterator = new TileIterator(Arrays.asList(subset), 2, 3);
        assertIterator(
                iterator,
                // zoom level 2
                new long[] {7, 0, 2},
                // zoom level 3
                new long[] {15, 0, 3});
    }

    @Test
    public void iterateOneLevelNorthEastCornerWgs84() {
        // y = 0 is the south-est row, north pole is at the highest y value instead
        GridSubset subset =
                GridSubsetFactory.createGridSubSet(WGS84, new BoundingBox(170, 80, 180, 90), 0, 3);
        TileIterator iterator = new TileIterator(Arrays.asList(subset), 2, 3);
        assertIterator(
                iterator,
                // zoom level 2
                new long[] {7, 3, 2},
                // zoom level 3
                new long[] {15, 7, 3});
    }

    private void assertIterator(TileIterator iterator, long[]... positions) {
        if (positions != null) {
            for (int i = 0; i < positions.length; i++) {
                long[] position = positions[i];
                assertTrue(
                        "Was expecting "
                                + Arrays.toString(position)
                                + " but found no more elements",
                        iterator.hasNext());
                long[] next = iterator.next();
                assertArrayEquals(
                        "Was expecting "
                                + Arrays.toString(position)
                                + " but found "
                                + Arrays.toString(next),
                        position,
                        next);
            }
        }
        assertFalse(
                "Shoud have reached the end but iterator still has more positions",
                iterator.hasNext());
    }
}
