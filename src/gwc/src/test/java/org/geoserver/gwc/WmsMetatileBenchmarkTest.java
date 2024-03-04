/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc;

import static org.geoserver.data.test.MockData.BASIC_POLYGONS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.ehcache.impl.internal.concurrent.ConcurrentHashMap;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.layer.TileLayer;
import org.junit.Ignore;
import org.junit.Test;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.springframework.mock.web.MockHttpServletResponse;

public class WmsMetatileBenchmarkTest extends GeoServerSystemTestSupport {

    static final String LAYER_NAME =
            BASIC_POLYGONS.getPrefix() + ":" + BASIC_POLYGONS.getLocalPart();

    /**
     * This isn't a real test. It's a function that is easy to run a profiler against. The JMH
     * benchmark isn't appropriate to run a profiler against because of the way it forks benchmarks
     * in a separate JVM.
     */
    @Test
    @Ignore
    public void profileBenchmark() throws Exception {

        GWC.get().getConfig().setDirectWMSIntegrationEnabled(true);

        long[][] uniqueMetaTileIndices = getTileIndices(LAYER_NAME, 10, 1000, 4, 1);

        for (long[] metaTileIndex : uniqueMetaTileIndices) {

            String request = buildGetMap(LAYER_NAME, metaTileIndex);
            MockHttpServletResponse response = getAsServletResponse(request);

            assertEquals(200, response.getStatus());
            assertEquals("image/png", response.getContentType());
            assertThat(response.getHeader("geowebcache-cache-result"), equalToIgnoringCase("MISS"));
        }
    }

    /**
     * Runs the JMH benchmark. This isn't a really test so it includes the @Ignore annotation; by
     * integrating JMH with Junit it just provides us an easy way to run the benchmark (typically
     * through the IDE).
     */
    @Test
    @Ignore
    public void runBenchmark() throws Exception {

        Options options =
                new OptionsBuilder()
                        .include(WmsMetatileBenchmark.class.getSimpleName() + ".*")
                        .result("./target/benchmark-results.json")
                        .resultFormat(ResultFormatType.JSON)
                        .build();
        new Runner(options).run();
    }

    @BenchmarkMode(Mode.Throughput)
    @Fork(1)
    @Threads(4)
    @Warmup(iterations = 2, time = 1)
    @Measurement(time = 1)
    public static class WmsMetatileBenchmark {

        @State(Scope.Benchmark)
        public static class AbstractBenchmarkState {

            long[][] tileIndices;

            AtomicInteger currentIndex = new AtomicInteger(0);

            GeoServerSystemTestSupport geoServerSystemTestSupport =
                    new GeoServerSystemTestSupport();

            // Track how many cache hits we get just to help validate correctness of our benchmark
            Map<String, Integer> cacheHitRate = new ConcurrentHashMap<>();

            @Setup
            public void setup() throws Exception {
                geoServerSystemTestSupport.doSetup();
            }

            @TearDown
            public void tearDown() throws Exception {

                GeoServerSystemTestSupport.doTearDownClass();

                // Print information about cache rate just to help validate correctness
                int cacheHits = cacheHitRate.getOrDefault("HIT", 0);
                int cacheMisses = cacheHitRate.getOrDefault("MISS", 0);
                int totalRequests = cacheHits + cacheMisses;
                double cacheHitRate = (double) cacheHits / totalRequests;
                System.out.println("Total requests: " + totalRequests);
                System.out.println("Cache hits: " + cacheHits);
                System.out.println("Cache misses: " + cacheMisses);
                System.out.println("Cache hit rate: " + cacheHitRate);
            }
        }

        public static class GwcAndNoCacheHitsState extends AbstractBenchmarkState {

            public void setup() throws Exception {
                super.setup();
                GWC.get().getConfig().setDirectWMSIntegrationEnabled(true);
                tileIndices = getTileIndices(LAYER_NAME, 11, 100000, 4, 1);
            }
        }

        public static class GwcAnd50PercentCacheHitsState extends AbstractBenchmarkState {

            public void setup() throws Exception {
                super.setup();
                GWC.get().getConfig().setDirectWMSIntegrationEnabled(true);
                tileIndices = getTileIndices(LAYER_NAME, 11, 100000, 4, 2);
            }
        }

        public static class GwcAnd75PercentCacheHitsState extends AbstractBenchmarkState {

            public void setup() throws Exception {
                super.setup();
                GWC.get().getConfig().setDirectWMSIntegrationEnabled(true);
                tileIndices = getTileIndices(LAYER_NAME, 11, 100000, 4, 4);
            }
        }

        public static class GwcAnd90PercentCacheHitsState extends AbstractBenchmarkState {

            public void setup() throws Exception {
                super.setup();
                GWC.get().getConfig().setDirectWMSIntegrationEnabled(true);

                tileIndices = getTileIndices(LAYER_NAME, 11, 100000, 4, 16);
            }
        }

        public static class NoGwcState extends AbstractBenchmarkState {

            public void setup() throws Exception {
                super.setup();
                GWC.get().getConfig().setDirectWMSIntegrationEnabled(false);
                tileIndices = getTileIndices(LAYER_NAME, 11, 100000, 4, 1);
            }
        }

        @Benchmark
        public void runWithGwcAndNoCacheHits(GwcAndNoCacheHitsState state) throws Exception {
            run(state);
        }

        @Benchmark
        public void runWithGwcAnd50PercentCacheHits(GwcAnd50PercentCacheHitsState state)
                throws Exception {
            run(state);
        }

        @Benchmark
        public void runWithGwcAnd75PercentCacheHits(GwcAnd75PercentCacheHitsState state)
                throws Exception {
            run(state);
        }

        @Benchmark
        public void runWithGwcAnd90PercentCacheHits(GwcAnd90PercentCacheHitsState state)
                throws Exception {
            run(state);
        }

        @Benchmark
        public void runWithoutGwc(NoGwcState state) throws Exception {
            run(state);
        }

        private void run(AbstractBenchmarkState state) throws Exception {

            int currentIndex = state.currentIndex.getAndIncrement();

            long[] metaTileIndex = state.tileIndices[currentIndex];

            String request = buildGetMap(LAYER_NAME, metaTileIndex);

            MockHttpServletResponse response =
                    state.geoServerSystemTestSupport.getAsServletResponse(request);

            String cacheResult = response.getHeader("geowebcache-cache-result");
            if (cacheResult == null) { // will be null if we aren't even using integrated GWC
                cacheResult = "MISS";
            }
            state.cacheHitRate.compute(cacheResult, (key, value) -> value == null ? 1 : value + 1);
        }
    }

    /**
     * For a given layer and zoom level, generate a certain amount of valid tile indices.
     *
     * <p>It will attempt to evenly distribute the tiles across the entire gridset coverage.
     *
     * @param tilesPerMetatile How many tiles from each metatile. By specifying "1" we can ensure all requests will be cache MISSES,
     *                         whereas anything greater than 1 will ensure some degree of cache HITS.
     */
    private static long[][] getTileIndices(
            String layerName, int zoomLevel, int amount, int metaTileSize, int tilesPerMetatile) {
        final GWC gwc = GWC.get();
        final TileLayer tileLayer = gwc.getTileLayerByName(layerName);
        final GridSubset gridSubset = tileLayer.getGridSubset("EPSG:4326");

        long[] coverage =
                gridSubset.getCoverage(zoomLevel); // coverage={minx,miny,max,maxy,zoomlevel}

        long[][] indices = new long[amount][3]; // each one contains {x,y,zoomLevel}

        long minX, minY, currentX, currentY, maxX, maxY;
        minX = currentX = coverage[0];
        minY = currentY = coverage[1];
        maxX = coverage[2];
        maxY = coverage[3];

        long width = maxX - minX;
        long height = maxY - minY;
        long maxNumberOfNonOverlappingMetaTilesHorizontally = width / metaTileSize;
        long maxNumberOfNonOverlappingMetaTilesVertically = height / metaTileSize;

        long horizontalIncrementBetweenMetaTiles = width / maxNumberOfNonOverlappingMetaTilesHorizontally;
        long verticalIncrementBetweenMetaTiles = height / maxNumberOfNonOverlappingMetaTilesVertically;

        long numberOfMetaTiles = amount / tilesPerMetatile;

        int tileCount = 0;
        // Traverse the overall grid to find each metatile
        for(int metaTileIndex = 0; metaTileIndex < numberOfMetaTiles; metaTileIndex++){

            // Traverse each metatile to pull out individual tiles
            long currentXWithinMetaTile = 0;
            long currentYWithinMetaTile = 0;
            for(int tileIndex = 0; tileIndex < tilesPerMetatile; tileIndex++){

                indices[tileCount] = new long[]{currentX + currentXWithinMetaTile, currentY + currentYWithinMetaTile, zoomLevel};
                tileCount++;

                currentXWithinMetaTile += 1;

                // navigate to next row of metatile if we hit the end of this one
                if(currentXWithinMetaTile > metaTileSize){
                    currentXWithinMetaTile = 0;
                    currentYWithinMetaTile += 1;
                }
            }

            currentX += horizontalIncrementBetweenMetaTiles;

            // Navigate to the next row if we hit the end of this one
            if (currentX > (maxX - metaTileSize)) {
                currentX = minX;
                currentY += verticalIncrementBetweenMetaTiles;
            }

            if (currentY > maxY) {
                throw new RuntimeException(
                        "Grid subset isn't large enough to generate the desired number of non-conflicting metatiles; try a larger zoom level.");
            }
        }
        return indices;
    }

    private static String buildGetMap(final String layerName, long[] tileIndex) {

        String gridsetId = "EPSG:4326";
        final GWC gwc = GWC.get();
        final TileLayer tileLayer = gwc.getTileLayerByName(layerName);
        final GridSubset gridSubset = tileLayer.getGridSubset(gridsetId);

        BoundingBox bounds = gridSubset.boundsFromIndex(tileIndex);

        StringBuilder sb = new StringBuilder("wms");
        sb.append("?service=WMS&request=GetMap&version=1.1.1&format=image/png");
        sb.append("&layers=").append(layerName);
        sb.append("&srs=").append(gridSubset.getSRS());
        sb.append("&width=").append(gridSubset.getGridSet().getTileWidth());
        sb.append("&height=").append(gridSubset.getGridSet().getTileHeight());
        sb.append("&styles=");
        sb.append("&bbox=").append(bounds.toString());
        sb.append("&tilesorigin=-180.0,90.0");
        sb.append("&tiled=true");
        return sb.toString();
    }
}
