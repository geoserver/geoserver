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

import jakarta.servlet.ServletResponse;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.ehcache.impl.internal.concurrent.ConcurrentHashMap;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.layer.TileLayer;
import org.junit.Ignore;
import org.junit.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.springframework.mock.web.MockHttpServletResponse;

@SuppressWarnings("PMD.SystemPrintln")
@Ignore
public class WmsMetatileBenchmarkTest extends GeoServerSystemTestSupport {

    static final String LAYER_NAME = BASIC_POLYGONS.getPrefix() + ":" + BASIC_POLYGONS.getLocalPart();

    /**
     * This isn't a real test. It's a function that is easy to run a profiler against. The JMH benchmark isn't
     * appropriate to run a profiler against because of the way it forks benchmarks in a separate JVM.
     */
    @Test
    @Ignore
    public void profileBenchmark() throws Exception {

        GWC.get().getConfig().setDirectWMSIntegrationEnabled(true);

        GWCConfig config = GWC.get().getConfig();
        config.setMetaTilingThreads(2 * Runtime.getRuntime().availableProcessors());
        GWC.get().saveConfig(config);

        long[][] uniqueMetaTileIndices = getTileIndices(LAYER_NAME, null, 10, 1000, 4, 1);

        for (long[] metaTileIndex : uniqueMetaTileIndices) {

            String request = buildGetMap(LAYER_NAME, metaTileIndex);
            MockHttpServletResponse response = getAsServletResponse(request);

            assertEquals(200, response.getStatus());
            assertEquals("image/png", response.getContentType());
            assertThat(response.getHeader("geowebcache-cache-result"), equalToIgnoringCase("MISS"));
        }
    }

    /**
     * Runs the JMH benchmark. This isn't a really test so it includes the @Ignore annotation; by integrating JMH with
     * Junit it just provides us an easy way to run the benchmark (typically through the IDE).
     */
    @Test
    public void runBenchmark() throws Exception {

        Options options = new OptionsBuilder()
                .include(WmsMetatileBenchmark.class.getSimpleName() + ".*")
                .result("./target/benchmark-results.json")
                .resultFormat(ResultFormatType.JSON)
                .build();
        new Runner(options).run();
    }

    private static class GeoServerBenchmarkSuppport extends GeoServerSystemTestSupport {
        @Override
        public MockHttpServletResponse getAsServletResponse(String path) throws Exception {
            return super.getAsServletResponse(path);
        }
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

            GeoServerBenchmarkSuppport geoServerSystemTestSupport = new GeoServerBenchmarkSuppport();

            // Track how many cache hits we get just to help validate correctness of our benchmark
            Map<String, Integer> cacheHitRate = new ConcurrentHashMap<>();

            @Setup
            public void setup() throws Exception {
                geoServerSystemTestSupport.doSetup();
            }

            protected void setMetaTilingThreads(int metaTilingThreads) throws IOException {
                GWCConfig config = GWC.get().getConfig();
                config.setMetaTilingThreads(metaTilingThreads);
                GWC.get().saveConfig(config);
            }

            @TearDown
            public void tearDown() throws Exception {

                GeoServerSystemTestSupport.doTearDownClass();

                // Print information about cache rate just to help validate correctness
                int cacheHits = cacheHitRate.getOrDefault("HIT", 0);
                int cacheMisses = cacheHitRate.getOrDefault("MISS", 0);
                int totalRequests = cacheHits + cacheMisses;
                double cacheHitRate = (double) cacheHits / totalRequests;
                extracted("Total requests: " + totalRequests);
                extracted("Cache hits: " + cacheHits);
                extracted("Cache misses: " + cacheMisses);
                extracted("Cache hit rate: " + cacheHitRate);
            }

            private static void extracted(String totalRequests) {
                System.out.println(totalRequests);
            }
        }

        public static class AbstractGwcWithConcurrency extends AbstractBenchmarkState {
            @Override
            public void setup() throws Exception {
                super.setup();
                GWC.get().getConfig().setDirectWMSIntegrationEnabled(true);
                setMetaTilingThreads(2 * Runtime.getRuntime().availableProcessors());
            }
        }

        public static class AbstractGwcWithoutConcurrency extends AbstractBenchmarkState {
            @Override
            public void setup() throws Exception {
                super.setup();
                GWC.get().getConfig().setDirectWMSIntegrationEnabled(true);
                setMetaTilingThreads(0);
            }
        }

        public static class GwcWithConcurrencyAndNoCacheHitsState extends AbstractGwcWithConcurrency {

            @Override
            public void setup() throws Exception {
                super.setup();
                tileIndices = getTileIndices(LAYER_NAME, null, 11, 100000, 4, 1);
                WmsMetatileBenchmarkTest.saveToCsv("./target/all-misses.csv", tileIndices);
            }
        }

        public static class GwcWithoutConcurrencyAndNoCacheHitsState extends AbstractGwcWithoutConcurrency {
            @Override
            public void setup() throws Exception {
                super.setup();
                tileIndices = getTileIndices(LAYER_NAME, null, 11, 100000, 4, 1);
                WmsMetatileBenchmarkTest.saveToCsv("./target/all-misses.csv", tileIndices);
            }
        }

        public static class GwcWithConcurrencyAnd50PercentCacheHitsState extends AbstractGwcWithConcurrency {
            @Override
            public void setup() throws Exception {
                super.setup();
                tileIndices = getTileIndices(LAYER_NAME, null, 11, 100000, 4, 2);
                WmsMetatileBenchmarkTest.saveToCsv("./target/50-percent-hits.csv", tileIndices);
            }
        }

        public static class GwcWithoutConcurrencyAnd50PercentCacheHitsState extends AbstractGwcWithoutConcurrency {
            @Override
            public void setup() throws Exception {
                super.setup();
                tileIndices = getTileIndices(LAYER_NAME, null, 11, 100000, 4, 2);
                WmsMetatileBenchmarkTest.saveToCsv("./target/50-percent-hits.csv", tileIndices);
            }
        }

        public static class GwcWithConcurrencyAnd75PercentCacheHitsState extends AbstractGwcWithConcurrency {
            @Override
            public void setup() throws Exception {
                super.setup();
                tileIndices = getTileIndices(LAYER_NAME, null, 11, 100000, 4, 4);
                WmsMetatileBenchmarkTest.saveToCsv("./target/75-percent-hits.csv", tileIndices);
            }
        }

        public static class GwcWithoutConcurrencyAnd75PercentCacheHitsState extends AbstractGwcWithoutConcurrency {
            @Override
            public void setup() throws Exception {
                super.setup();
                tileIndices = getTileIndices(LAYER_NAME, null, 11, 100000, 4, 4);
                WmsMetatileBenchmarkTest.saveToCsv("./target/75-percent-hits.csv", tileIndices);
            }
        }

        public static class GwcWithConcurrencyAnd90PercentCacheHitsState extends AbstractGwcWithConcurrency {
            @Override
            public void setup() throws Exception {
                super.setup();
                tileIndices = getTileIndices(LAYER_NAME, null, 11, 100000, 4, 16);
                WmsMetatileBenchmarkTest.saveToCsv("./target/90-percent-hits.csv", tileIndices);
            }
        }

        public static class GwcWithoutConcurrencyAnd90PercentCacheHitsState extends AbstractGwcWithoutConcurrency {
            @Override
            public void setup() throws Exception {
                super.setup();
                tileIndices = getTileIndices(LAYER_NAME, null, 11, 100000, 4, 16);
                WmsMetatileBenchmarkTest.saveToCsv("./target/90-percent-hits.csv", tileIndices);
            }
        }

        public static class NoGwcState extends AbstractBenchmarkState {
            @Override
            public void setup() throws Exception {
                super.setup();
                GWC.get().getConfig().setDirectWMSIntegrationEnabled(false);
                tileIndices = getTileIndices(LAYER_NAME, null, 11, 100000, 4, 1);
            }
        }

        @Benchmark
        public ServletResponse runWithGwcWithConcurrencyAndNoCacheHits(GwcWithConcurrencyAndNoCacheHitsState state)
                throws Exception {
            return run(state);
        }

        @Benchmark
        public ServletResponse runWithGwcWithoutConcurrencyAndNoCacheHits(
                GwcWithoutConcurrencyAndNoCacheHitsState state) throws Exception {
            return run(state);
        }

        @Benchmark
        public ServletResponse runWithGwcWithConcurrencyAnd50PercentCacheHits(
                GwcWithConcurrencyAnd50PercentCacheHitsState state) throws Exception {
            return run(state);
        }

        @Benchmark
        public ServletResponse runWithGwcWithoutConcurrencyAnd50PercentCacheHits(
                GwcWithoutConcurrencyAnd50PercentCacheHitsState state) throws Exception {
            return run(state);
        }

        @Benchmark
        public ServletResponse runWithGwcWithConcurrencyAnd75PercentCacheHits(
                GwcWithConcurrencyAnd75PercentCacheHitsState state) throws Exception {
            return run(state);
        }

        @Benchmark
        public ServletResponse runWithGwcWithoutConcurrencyAnd75PercentCacheHits(
                GwcWithoutConcurrencyAnd75PercentCacheHitsState state) throws Exception {
            return run(state);
        }

        @Benchmark
        public ServletResponse runWithGwcWithConcurrencyAnd90PercentCacheHits(
                GwcWithConcurrencyAnd90PercentCacheHitsState state) throws Exception {
            return run(state);
        }

        @Benchmark
        public ServletResponse runWithGwcWithoutConcurrencyAnd90PercentCacheHits(
                GwcWithoutConcurrencyAnd90PercentCacheHitsState state) throws Exception {
            return run(state);
        }

        @Benchmark
        public ServletResponse runWithoutGwc(NoGwcState state) throws Exception {
            return run(state);
        }

        private ServletResponse run(AbstractBenchmarkState state) throws Exception {

            int currentIndex = state.currentIndex.getAndIncrement();

            long[] metaTileIndex = state.tileIndices[currentIndex];

            String request = buildGetMap(LAYER_NAME, metaTileIndex);

            MockHttpServletResponse response = state.geoServerSystemTestSupport.getAsServletResponse(request);

            String cacheResult = response.getHeader("geowebcache-cache-result");
            if (cacheResult == null) { // will be null if we aren't even using integrated GWC
                cacheResult = "MISS";
            }
            state.cacheHitRate.compute(cacheResult, (key, value) -> value == null ? 1 : value + 1);
            return response;
        }
    }

    /**
     * For a given layer and zoom level, generate a certain amount of valid tile indices.
     *
     * <p>It will attempt to evenly distribute the tiles across the entire gridset coverage.
     *
     * @param boundingBox Optional bounding box to constrain tiles to a particular geographical area.
     * @param tilesPerMetatile How many tiles from each metatile. By specifying "1" we can ensure all requests will be
     *     cache MISSES, whereas anything greater than 1 will ensure some degree of cache HITS.
     */
    private static long[][] getTileIndices(
            String layerName,
            BoundingBox boundingBox,
            int zoomLevel,
            int amount,
            int metaTileSize,
            int tilesPerMetatile) {
        final GWC gwc = GWC.get();
        final TileLayer tileLayer = gwc.getTileLayerByName(layerName);
        final GridSubset gridSubset = tileLayer.getGridSubset("EPSG:4326");

        long[] coverage; // coverage={minx,miny,max,maxy,zoomlevel}
        if (boundingBox == null) {
            coverage = gridSubset.getCoverage(zoomLevel);
        } else {
            coverage = gridSubset.getCoverageIntersection(zoomLevel, boundingBox);
        }

        System.out.printf(
                "Coverage: %d, %d, %d, %d, %d (minx, miny, maxx, maxy, zoomLevel)\n",
                coverage[0], coverage[1], coverage[2], coverage[3], coverage[4]);

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

        System.out.println("Max number of metatiles with coverage: "
                + maxNumberOfNonOverlappingMetaTilesHorizontally * maxNumberOfNonOverlappingMetaTilesVertically);

        long horizontalIncrementBetweenMetaTiles = width / maxNumberOfNonOverlappingMetaTilesHorizontally;
        long verticalIncrementBetweenMetaTiles = height / maxNumberOfNonOverlappingMetaTilesVertically;

        long numberOfMetaTiles = amount / tilesPerMetatile;

        int tileCount = 0;
        // Traverse the overall grid to find each metatile
        for (int metaTileIndex = 0; metaTileIndex < numberOfMetaTiles; metaTileIndex++) {

            // Traverse each metatile to pull out individual tiles
            long currentXWithinMetaTile = 0;
            long currentYWithinMetaTile = 0;
            for (int tileIndex = 0; tileIndex < tilesPerMetatile; tileIndex++) {

                indices[tileCount] =
                        new long[] {currentX + currentXWithinMetaTile, currentY + currentYWithinMetaTile, zoomLevel};
                tileCount++;

                currentXWithinMetaTile += 1;

                // navigate to next row of metatile if we hit the end of this one
                if (currentXWithinMetaTile > metaTileSize) {
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
    /**
     * Save the indicies to a CSV which can optionally be loaded into JMeter for alternative benchmarking.
     *
     * @param location Location of the CSV
     * @param tileIndices The tile indices
     * @throws IOException
     */
    static void saveToCsv(String location, long[][] tileIndices) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(location))) {
            String gridsetId = "EPSG:4326";
            final GWC gwc = GWC.get();
            final TileLayer tileLayer = gwc.getTileLayerByName(LAYER_NAME);
            final GridSubset gridSubset = tileLayer.getGridSubset(gridsetId);

            for (long[] tileIndex : tileIndices) {
                BoundingBox bounds = gridSubset.boundsFromIndex(tileIndex);
                writer.println(bounds.toString());
            }
        }
    }
}
