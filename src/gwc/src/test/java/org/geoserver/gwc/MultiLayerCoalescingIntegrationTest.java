/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc;

import static org.awaitility.Awaitility.await;
import static org.geoserver.data.test.MockData.BASIC_POLYGONS;
import static org.geoserver.data.test.MockData.FORESTS;
import static org.geoserver.data.test.MockData.LAKES;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import jakarta.servlet.http.HttpServletResponse;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import org.apache.hc.client5.http.utils.DateUtils;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.gwc.layer.TileLayerInfoUtil;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.wms.WMSInfo;
import org.geotools.image.test.ImageAssert;
import org.geowebcache.config.XMLGridSubset;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.storage.StorageException;
import org.geowebcache.storage.StorageObject;
import org.geowebcache.storage.TileObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** Multi-layer tile coalescing test. */
public class MultiLayerCoalescingIntegrationTest extends GeoServerSystemTestSupport {

    private String layer1;

    private String layer2;

    private GridSubset gridSubset;

    @Before
    public void enableMultiLayerCaching() throws Exception {
        layer1 = getLayerId(BASIC_POLYGONS);
        layer2 = getLayerId(LAKES);

        GWC gwc = GWC.get();
        GWCConfig config = gwc.getConfig();
        config.setDirectWMSIntegrationEnabled(true);
        config.setMultiLayerCachingEnabled(true);
        gwc.saveConfig(config);

        TileLayer tileLayer = gwc.getTileLayerByName(layer1);
        gridSubset = tileLayer.getGridSubset("EPSG:4326");

        truncate(layer1);
        truncate(layer2);
    }

    @After
    public void resetConfig() throws Exception {
        GWC gwc = GWC.get();
        GWCConfig config = gwc.getConfig();
        config.setDirectWMSIntegrationEnabled(false);
        config.setMultiLayerCachingEnabled(false);
        gwc.saveConfig(config);
    }

    private void truncate(String layerName) throws Exception {
        GWC.get().truncate(layerName);
    }

    private String coalescedGetMap(String layers) {
        return coalescedGetMap(layers, "image/png");
    }

    private String coalescedGetMap(String layers, String format) {
        long[] coverage = gridSubset.getCoverage(0);
        long[] tileIndex = {coverage[0], coverage[1], coverage[4]};
        BoundingBox bounds = gridSubset.boundsFromIndex(tileIndex);

        return "wms?service=WMS&request=GetMap&version=1.1.1&transparent=true&tiled=true"
                + "&format=" + format
                + "&layers=" + layers
                + "&srs=" + gridSubset.getSRS()
                + "&width=" + gridSubset.getGridSet().getTileWidth()
                + "&height=" + gridSubset.getGridSet().getTileHeight()
                + "&bbox=" + bounds;
    }

    private TileObject sampleTile(String layerName) throws StorageException {
        return sampleTile(layerName, "image/png");
    }

    private TileObject sampleTile(String layerName, String format) throws StorageException {
        long[] coverage = gridSubset.getCoverage(0);
        long[] tileIndex = {coverage[0], coverage[1], coverage[4]};
        TileObject tileObject = TileObject.createQueryTileObject(
                layerName, tileIndex, gridSubset.getName(), format, Collections.emptyMap());
        GWC.get().getCompositeBlobStore().get(tileObject);
        return tileObject;
    }

    /**
     * Waits for the member's own tile save to land: the conveyor tile save runs on GWC's metatiling executor, so it can
     * still be in flight when the coalesced request returns.
     */
    private void awaitCached(String layerName) {
        awaitCached(layerName, "image/png");
    }

    private void awaitCached(String layerName, String format) {
        await().atMost(5, TimeUnit.SECONDS)
                .until(() -> sampleTile(layerName, format).getStatus() != StorageObject.Status.MISS);
    }

    /**
     * Waits for a request with these exact params to become a full cache hit, going through the real HTTP dispatch path
     * rather than reconstructing GWC's internal parameters key by hand (e.g. its VIEWPARAMS hashing), which this test
     * has no reliable way to reproduce.
     */
    private void awaitCoalescedHit(String url) {
        await().atMost(5, TimeUnit.SECONDS)
                .until(() -> "HIT".equals(getAsServletResponse(url).getHeader("geowebcache-cache-result")));
    }

    @Test
    public void testCoalescedRequest() throws Exception {
        // caches start empty
        assertEquals(StorageObject.Status.MISS, sampleTile(layer1).getStatus());
        assertEquals(StorageObject.Status.MISS, sampleTile(layer2).getStatus());

        MockHttpServletResponse coalescedResponse = getAsServletResponse(coalescedGetMap(layer1 + "," + layer2));
        assertEquals(200, coalescedResponse.getStatus());
        assertEquals("image/png", coalescedResponse.getContentType());

        // stacking: the coalesced tile is neither member alone
        MockHttpServletResponse layer1Response = getAsServletResponse(coalescedGetMap(layer1));
        MockHttpServletResponse layer2Response = getAsServletResponse(coalescedGetMap(layer2));
        byte[] coalescedBytes = coalescedResponse.getContentAsByteArray();
        assertFalse(Arrays.equals(coalescedBytes, layer1Response.getContentAsByteArray()));
        assertFalse(Arrays.equals(coalescedBytes, layer2Response.getContentAsByteArray()));

        // cache population: each member's own tile cache now holds a real blob, keyed under its own layer name,
        // reusable by an ordinary single-layer request
        awaitCached(layer1);
        awaitCached(layer2);
        TileObject member1Tile = sampleTile(layer1);
        TileObject member2Tile = sampleTile(layer2);
        assertNotNull(member1Tile.getBlob());
        assertNotNull(member2Tile.getBlob());

        // a second coalesced request is now an all-cache-hit
        MockHttpServletResponse secondResponse = getAsServletResponse(coalescedGetMap(layer1 + "," + layer2));
        assertEquals(200, secondResponse.getStatus());
        assertEquals("HIT", secondResponse.getHeader("geowebcache-cache-result"));
        assertArrayEquals(coalescedBytes, secondResponse.getContentAsByteArray());
    }

    @Test
    public void testMemberOutsideItsCachedCoverageIsLiveRenderedOnly() throws Exception {
        // populate layer1's cache through an ordinary single-layer request; layer2's stays empty
        getAsServletResponse(coalescedGetMap(layer1));
        awaitCached(layer1);

        long[] coverage = gridSubset.getCoverage(0);
        long[] tileIndex = {coverage[0], coverage[1], coverage[4]};
        BoundingBox requested = gridSubset.boundsFromIndex(tileIndex);
        BoundingBox gridSetBounds = gridSubset.getGridSetBounds();
        // everything east of the requested tile: layer2 then caches a strip that excludes the tile asked for below
        BoundingBox elsewhere = new BoundingBox(
                requested.getMaxX(), gridSetBounds.getMinY(), gridSetBounds.getMaxX(), gridSetBounds.getMaxY());

        BoundingBox previousExtent = setCachedExtent(layer2, elsewhere);
        try {
            // guards the premise rather than assuming the layer bounds: without this the test could pass for the
            // wrong reason if the narrowed extent still covered the tile
            GeoServerTileLayer narrowed = (GeoServerTileLayer) GWC.get().getTileLayerByName(layer2);
            assertFalse(narrowed.getGridSubset(gridSubset.getName()).covers(tileIndex));

            MockHttpServletResponse response = getAsServletResponse(coalescedGetMap(layer1 + "," + layer2));

            assertEquals(200, response.getStatus());
            assertEquals("image/png", response.getContentType());
            // the whole stack used to drop to a live combined render here; only layer2 does now
            assertEquals("PARTIAL 1/2", response.getHeader("geowebcache-cache-result"));

            // the live-rendered member was spliced in, not dropped
            byte[] coalescedBytes = response.getContentAsByteArray();
            MockHttpServletResponse layer1Response = getAsServletResponse(coalescedGetMap(layer1));
            assertFalse(Arrays.equals(coalescedBytes, layer1Response.getContentAsByteArray()));

            // an out-of-coverage member is never cached for this tile, so the next request splits the same way
            assertEquals(StorageObject.Status.MISS, sampleTile(layer2).getStatus());
            assertEquals(
                    "PARTIAL 1/2",
                    getAsServletResponse(coalescedGetMap(layer1 + "," + layer2)).getHeader("geowebcache-cache-result"));
        } finally {
            setCachedExtent(layer2, previousExtent);
        }
    }

    /**
     * Narrows (or restores) the extent this layer caches for the test gridset.
     *
     * @return the extent that was configured before this call
     */
    private BoundingBox setCachedExtent(String layerName, BoundingBox extent) {
        GeoServerTileLayer tileLayer = (GeoServerTileLayer) GWC.get().getTileLayerByName(layerName);
        XMLGridSubset subset = tileLayer.getInfo().getGridSubsets().stream()
                .filter(candidate -> gridSubset.getName().equals(candidate.getGridSetName()))
                .findFirst()
                .orElseThrow();
        BoundingBox previous = subset.getExtent();
        subset.setExtent(extent);
        GWC.get().save(tileLayer);
        return previous;
    }

    @Test
    public void testCoalescedPng8Request() throws Exception {
        cachePng8(layer1);
        cachePng8(layer2);
        try {
            assertCoalescedPng8();
        } finally {
            uncachePng8(layer1);
            uncachePng8(layer2);
        }
    }

    private void assertCoalescedPng8() throws Exception {
        String url = coalescedGetMap(layer1 + "," + layer2, "image/png8");
        MockHttpServletResponse response = getAsServletResponse(url);

        assertEquals(200, response.getStatus());
        // png8 shares image/png as its response content type, exactly as a single-layer png8 tile does
        assertEquals("image/png", response.getContentType());
        assertNull(response.getHeader("geowebcache-miss-reason"));

        // the stacked canvas is composited in ARGB, so the palette can only come from encoding it as png8
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(response.getContentAsByteArray()));
        assertTrue(image.getColorModel() instanceof IndexColorModel);

        // members are cached under png8, not under png: a png8 request must not be served from png tiles
        awaitCached(layer1, "image/png8");
        awaitCached(layer2, "image/png8");
        assertEquals(StorageObject.Status.MISS, sampleTile(layer1).getStatus());

        awaitCoalescedHit(url);
    }

    @Test
    public void testCoalescedRequestRejectsPng8WithoutAPng8Cache() throws Exception {
        // no cachePng8() here: the members only cache image/png, so no member qualifies for a png8 request
        MockHttpServletResponse response = getAsServletResponse(coalescedGetMap(layer1 + "," + layer2, "image/png8"));

        assertEquals(200, response.getStatus());
        assertEquals("MISS", response.getHeader("geowebcache-cache-result"));
        assertTrue(response.getHeader("geowebcache-miss-reason").contains("no member"));
    }

    @Test
    public void testCoalescedRequestRejectsJpeg() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(coalescedGetMap(layer1 + "," + layer2, "image/jpeg"));

        assertEquals(200, response.getStatus());
        assertEquals("MISS", response.getHeader("geowebcache-cache-result"));
        assertTrue(response.getHeader("geowebcache-miss-reason").contains("transparent image/png or image/png8"));
    }

    /** Adds png8 to a member's cached formats, so a png8 coalesced request can find a tile for it. */
    private void cachePng8(String layerName) {
        setPng8Cached(layerName, true);
    }

    /** Restores the member's cached formats, so the png8 tests don't leak into the ones that expect png only. */
    private void uncachePng8(String layerName) {
        setPng8Cached(layerName, false);
    }

    private void setPng8Cached(String layerName, boolean cached) {
        GeoServerTileLayer tileLayer = (GeoServerTileLayer) GWC.get().getTileLayerByName(layerName);
        if (cached) {
            tileLayer.getInfo().getMimeFormats().add("image/png8");
        } else {
            tileLayer.getInfo().getMimeFormats().remove("image/png8");
        }
        GWC.get().save(tileLayer);
    }

    @Test
    public void testCoalescedRequestSupportsIfModifiedSince() throws Exception {
        String url = coalescedGetMap(layer1 + "," + layer2);

        MockHttpServletResponse response = getAsServletResponse(url);
        assertEquals(200, response.getStatus());
        String lastModifiedHeader = response.getHeader("Last-Modified");
        assertNotNull(lastModifiedHeader);
        Instant lastModified = DateUtils.parseStandardDate(lastModifiedHeader);
        // the assembled tile's own creation time, not the TileObject default of epoch 0: catches the tile being
        // reported as unchanged since 1970 regardless of when it was actually assembled
        assertTrue(lastModified.isAfter(Instant.now().minusSeconds(60)));

        assertEquals(
                HttpServletResponse.SC_NOT_MODIFIED,
                dispatch(getRequest(url, lastModifiedHeader), "UTF-8").getStatus());

        String past = DateUtils.formatStandardDate(lastModified.minusMillis(5000));
        assertEquals(
                HttpServletResponse.SC_OK,
                dispatch(getRequest(url, past), "UTF-8").getStatus());

        String future = DateUtils.formatStandardDate(lastModified.plusMillis(5000));
        assertEquals(
                HttpServletResponse.SC_NOT_MODIFIED,
                dispatch(getRequest(url, future), "UTF-8").getStatus());
    }

    private MockHttpServletRequest getRequest(String url, String ifModifiedSince) {
        MockHttpServletRequest httpReq = createRequest(url);
        httpReq.setMethod("GET");
        httpReq.setContent(new byte[] {});
        httpReq.addHeader("If-Modified-Since", ifModifiedSince);
        return httpReq;
    }

    @Test
    public void testCoalescedResultMatchesLiveRender() throws Exception {
        MockHttpServletResponse coalescedResponse = getAsServletResponse(coalescedGetMap(layer1 + "," + layer2));
        assertEquals(200, coalescedResponse.getStatus());
        BufferedImage coalesced = ImageIO.read(new ByteArrayInputStream(coalescedResponse.getContentAsByteArray()));

        GWC gwc = GWC.get();
        GWCConfig config = gwc.getConfig();
        config.setMultiLayerCachingEnabled(false);
        gwc.saveConfig(config);

        MockHttpServletResponse liveResponse = getAsServletResponse(coalescedGetMap(layer1 + "," + layer2));
        assertEquals(200, liveResponse.getStatus());
        BufferedImage live = ImageIO.read(new ByteArrayInputStream(liveResponse.getContentAsByteArray()));

        // stacking two independently-cached tiles must reproduce the live combined render, modulo antialiasing
        // drift at polygon edges from rendering each member separately instead of onto one shared canvas; flatten
        // onto white first, since a transparent pixel's RGB is otherwise unconstrained and can differ between the
        // two renders without any visible difference
        ImageAssert.assertEquals(flattenOnWhite(live), flattenOnWhite(coalesced), 20);
    }

    /** Composites {@code image} over an opaque white background, so fully-transparent pixels compare equal. */
    private static BufferedImage flattenOnWhite(BufferedImage image) {
        BufferedImage flattened = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = flattened.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.drawImage(image, 0, 0, null);
        graphics.dispose();
        return flattened;
    }

    @Test
    public void testPartialCacheResultWhenOnlySomeMembersAreCached() throws Exception {
        // populate both members' caches
        getAsServletResponse(coalescedGetMap(layer1 + "," + layer2));
        awaitCached(layer1);
        awaitCached(layer2);

        // evict just one member: the next coalesced request hits layer1 but re-renders layer2
        truncate(layer2);
        assertEquals(StorageObject.Status.MISS, sampleTile(layer2).getStatus());

        MockHttpServletResponse response = getAsServletResponse(coalescedGetMap(layer1 + "," + layer2));

        assertEquals(200, response.getStatus());
        assertEquals("PARTIAL 1/2", response.getHeader("geowebcache-cache-result"));

        // the re-rendered member is now cached again too
        awaitCached(layer2);
    }

    @Test
    public void testNonCacheableMemberIsLiveRenderedAndSplicedIntoTheStack() throws Exception {
        // populate layer1's own cache first
        getAsServletResponse(coalescedGetMap(layer1 + "," + layer2));
        awaitCached(layer1);

        GWC gwc = GWC.get();
        TileLayer forestsTileLayer = gwc.getTileLayerByName(getLayerId(FORESTS));
        forestsTileLayer.setEnabled(false);
        try {
            MockHttpServletResponse response =
                    getAsServletResponse(coalescedGetMap(layer1 + "," + getLayerId(FORESTS)));

            assertEquals(200, response.getStatus());
            assertEquals("image/png", response.getContentType());
            // layer1 is a cache hit, the disabled member always misses
            assertEquals("PARTIAL 1/2", response.getHeader("geowebcache-cache-result"));

            // stacking happened: the coalesced result differs from layer1 rendered alone
            byte[] coalescedBytes = response.getContentAsByteArray();
            MockHttpServletResponse layer1Response = getAsServletResponse(coalescedGetMap(layer1));
            assertFalse(Arrays.equals(coalescedBytes, layer1Response.getContentAsByteArray()));
        } finally {
            forestsTileLayer.setEnabled(true);
        }
    }

    @Test
    public void testPartialCacheResultCountsEveryMemberOfABatchedLiveRun() throws Exception {
        // populate both cacheable members' caches first
        getAsServletResponse(coalescedGetMap(layer1 + "," + layer2));
        awaitCached(layer1);
        awaitCached(layer2);

        GWC gwc = GWC.get();
        TileLayer forestsTileLayer = gwc.getTileLayerByName(getLayerId(FORESTS));
        forestsTileLayer.setEnabled(false);
        try {
            String forests = getLayerId(FORESTS);
            String layers = layer1 + "," + forests + "," + forests + "," + layer2;
            MockHttpServletResponse response = getAsServletResponse(coalescedGetMap(layers));

            assertEquals(200, response.getStatus());
            // 4 original members: layer1 and layer2 are cache hits, both forests slots always miss
            assertEquals("PARTIAL 2/4", response.getHeader("geowebcache-cache-result"));
        } finally {
            forestsTileLayer.setEnabled(true);
        }
    }

    @Test
    public void testCacheControl() throws Exception {
        setCachingMetadata(layer1, true, 600);
        setCachingMetadata(layer2, true, 300);

        MockHttpServletResponse response = getAsServletResponse(coalescedGetMap(layer1 + "," + layer2));
        assertEquals(200, response.getStatus());
        assertEquals("max-age=300, must-revalidate", response.getHeader("Cache-Control"));
        assertNotNull(response.getHeader("Expires"));
    }

    @Test
    public void testNoCacheControlWhenAnyLayerDisablesCaching() throws Exception {
        setCachingMetadata(layer1, true, 600);
        setCachingMetadata(layer2, false, 0);

        MockHttpServletResponse response = getAsServletResponse(coalescedGetMap(layer1 + "," + layer2));
        assertEquals(200, response.getStatus());
        assertNull(response.getHeader("Cache-Control"));
    }

    @Test
    public void testLiveRender() throws Exception {
        GWC gwc = GWC.get();
        GWCConfig config = gwc.getConfig();
        config.setMultiLayerCachingEnabled(false);
        gwc.saveConfig(config);

        MockHttpServletResponse response = getAsServletResponse(coalescedGetMap(layer1 + "," + layer2));
        assertEquals(200, response.getStatus());
        assertEquals("image/png", response.getContentType());
        assertEquals("MISS", response.getHeader("geowebcache-cache-result"));
        assertTrue(response.getHeader("geowebcache-miss-reason").contains("more than one layer requested"));

        // and neither member's cache was touched
        assertEquals(StorageObject.Status.MISS, sampleTile(layer1).getStatus());
        assertEquals(StorageObject.Status.MISS, sampleTile(layer2).getStatus());
    }

    @Test
    public void testWithLabeledStyle() throws Exception {
        getTestData().addStyle("labeled", "labeled.sld", MultiLayerCoalescingIntegrationTest.class, getCatalog());

        // layer1 keeps its default style, layer2 uses the labeled one (positional, aligned to LAYERS)
        String url = coalescedGetMap(layer1 + "," + layer2) + "&styles=,labeled";
        MockHttpServletResponse response = getAsServletResponse(url);

        assertEquals("MISS", response.getHeader("geowebcache-cache-result"));
        assertTrue(response.getHeader("geowebcache-miss-reason").contains("draws labels or composites"));

        // the rejected coalesced attempt never populated either member's cache
        assertEquals(StorageObject.Status.MISS, sampleTile(layer1).getStatus());
        assertEquals(StorageObject.Status.MISS, sampleTile(layer2).getStatus());
    }

    @Test
    public void testWithCqlFilters() throws Exception {
        GeoServerTileLayer tileLayer1 = (GeoServerTileLayer) GWC.get().getTileLayerByName(layer1);
        GeoServerTileLayer tileLayer2 = (GeoServerTileLayer) GWC.get().getTileLayerByName(layer2);
        TileLayerInfoUtil.updateAcceptAllRegExParameterFilter(tileLayer1.getInfo(), "CQL_FILTER", true);
        TileLayerInfoUtil.updateAcceptAllRegExParameterFilter(tileLayer2.getInfo(), "CQL_FILTER", true);
        GWC.get().save(tileLayer1);
        GWC.get().save(tileLayer2);

        // one clause per member, aligned to LAYERS order; each member must see only its own slice
        String url = coalescedGetMap(layer1 + "," + layer2) + "&CQL_FILTER=INCLUDE;EXCLUDE";
        MockHttpServletResponse response = getAsServletResponse(url);

        assertEquals(200, response.getStatus());
        assertEquals("image/png", response.getContentType());
        // reaching a real cache dispatch
        assertNull(response.getHeader("geowebcache-miss-reason"));
        assertNotNull(response.getHeader("geowebcache-cache-result"));
    }

    @Test
    public void testReplicatedViewParamsAppliesToEveryMembersCacheKey() throws Exception {
        // VIEWPARAMS has to be a cacheable parameter for both members
        for (String layerName : new String[] {layer1, layer2}) {
            GeoServerTileLayer tileLayer = (GeoServerTileLayer) GWC.get().getTileLayerByName(layerName);
            TileLayerInfoUtil.updateAcceptAllRegExParameterFilter(tileLayer.getInfo(), "VIEWPARAMS", true);
            GWC.get().save(tileLayer);
            truncate(layerName);
        }

        // GetMapKvpRequestReader.applyViewParams replicates a single entry over every layer, so both members
        // render with a:1; the raw VIEWPARAMS mirror used for the cache key must reflect that for BOTH members,
        // not just the first one
        String urlA1 = coalescedGetMap(layer1 + "," + layer2) + "&VIEWPARAMS=a:1";
        MockHttpServletResponse first = getAsServletResponse(urlA1);
        assertEquals("image/png", first.getContentType());
        // both members are now keyed by the replicated value, not just member1: a repeat of this exact request
        // becomes a full HIT only once member2's tile is cached under the replicated a:1 key too - if the fix
        // regressed back to writing "" for member2, that member would never hit and this would time out
        awaitCoalescedHit(urlA1);

        // a:2 renders differently, so neither member may be served from the a:1 run
        MockHttpServletResponse second =
                getAsServletResponse(coalescedGetMap(layer1 + "," + layer2) + "&VIEWPARAMS=a:2");
        assertEquals("image/png", second.getContentType());
        assertEquals("MISS", second.getHeader("geowebcache-cache-result"));

        // and a plain request with no VIEWPARAMS at all must not be served the a:1-rendered tile either
        MockHttpServletResponse plain = getAsServletResponse(coalescedGetMap(layer1 + "," + layer2));
        assertEquals("image/png", plain.getContentType());
        assertEquals("MISS", plain.getHeader("geowebcache-cache-result"));
    }

    /** A two-layer group, so a {@code LAYERS} slot naming it expands into two members. */
    private LayerGroupInfo addTwoLayerGroup(String name) throws Exception {
        LayerGroupInfo group = getCatalog().getFactory().createLayerGroup();
        group.setName(name);
        group.getLayers().add(getCatalog().getLayerByName(getLayerId(BASIC_POLYGONS)));
        group.getLayers().add(getCatalog().getLayerByName(getLayerId(FORESTS)));
        group.getStyles().add(null);
        group.getStyles().add(null);
        new CatalogBuilder(getCatalog()).calculateLayerGroupBounds(group);
        getCatalog().add(group);
        return group;
    }

    @Test
    public void testExpandedLayerGroupIsCoalescedWhenNoPerLayerParameterIsPresent() throws Exception {
        LayerGroupInfo group = addTwoLayerGroup("plainCoalescingGroup");
        try {
            // 2 raw LAYERS slots, 3 members once the group is expanded, but nothing per-layer to slice: the
            // members line up positionally with nothing, so each is cached under its own name as usual
            MockHttpServletResponse response = getAsServletResponse(coalescedGetMap("plainCoalescingGroup," + layer2));

            assertEquals(200, response.getStatus());
            assertEquals("image/png", response.getContentType());
            assertNull(response.getHeader("geowebcache-miss-reason"));
            assertNotNull(response.getHeader("geowebcache-cache-result"));

            // the group's members are cached individually, so the whole request becomes a hit
            awaitCoalescedHit(coalescedGetMap("plainCoalescingGroup," + layer2));
        } finally {
            getCatalog().remove(group);
        }
    }

    @Test
    public void testExpandedLayerGroupFallsBackAndStillMatchesTheLiveRender() throws Exception {
        LayerGroupInfo group = addTwoLayerGroup("coalescingGroup");
        try {
            getTestData().addStyle("labeled", "labeled.sld", MultiLayerCoalescingIntegrationTest.class, getCatalog());

            // 2 raw LAYERS slots but 3 members after the group is expanded: a positional slice of STYLES would
            // hand "labeled" to the group's second member instead of layer2, silently drawing the wrong image
            String url = coalescedGetMap("coalescingGroup," + layer2) + "&styles=,labeled";
            MockHttpServletResponse response = getAsServletResponse(url);

            assertEquals(200, response.getStatus());
            assertEquals("image/png", response.getContentType());
            assertEquals("MISS", response.getHeader("geowebcache-cache-result"));
            assertTrue(response.getHeader("geowebcache-miss-reason").contains("a LAYERS entry was expanded"));
            BufferedImage coalesced = ImageIO.read(new ByteArrayInputStream(response.getContentAsByteArray()));

            GWC gwc = GWC.get();
            GWCConfig config = gwc.getConfig();
            config.setMultiLayerCachingEnabled(false);
            gwc.saveConfig(config);
            MockHttpServletResponse liveResponse = getAsServletResponse(url);
            assertEquals(200, liveResponse.getStatus());
            BufferedImage live = ImageIO.read(new ByteArrayInputStream(liveResponse.getContentAsByteArray()));

            ImageAssert.assertEquals(flattenOnWhite(live), flattenOnWhite(coalesced), 20);
        } finally {
            getCatalog().remove(group);
        }
    }

    @Test
    public void testAllMembersLiveFallsBackWithoutAssembling() throws Exception {
        GWC gwc = GWC.get();
        TileLayer layer1TileLayer = gwc.getTileLayerByName(layer1);
        TileLayer forestsTileLayer = gwc.getTileLayerByName(getLayerId(FORESTS));
        layer1TileLayer.setEnabled(false);
        forestsTileLayer.setEnabled(false);
        try {
            MockHttpServletResponse response =
                    getAsServletResponse(coalescedGetMap(layer1 + "," + getLayerId(FORESTS)));

            assertEquals(200, response.getStatus());
            assertEquals("image/png", response.getContentType());
            assertEquals("MISS", response.getHeader("geowebcache-cache-result"));
            assertTrue(response.getHeader("geowebcache-miss-reason")
                    .contains("no member of the coalesced request is cacheable"));

            // no per-member tile was ever assembled or cached
            assertEquals(StorageObject.Status.MISS, sampleTile(layer1).getStatus());
        } finally {
            layer1TileLayer.setEnabled(true);
            forestsTileLayer.setEnabled(true);
        }
    }

    @Test
    public void testMemoryLimits() throws Exception {
        // GWC's projected peak is (members + 1) * tileWidth * tileHeight * 4 bytes ARGB = 3 * 256 * 256 * 4 = 768 KB
        // for this 2-member request. The live fallback render's own memory check (RenderedImageMapOutputFormat)
        // uses a different, much smaller estimate: just the output buffer (256 * 256 * 4 = 256 KB) plus per-style
        // back-buffers, which is 0 here since both layers use plain single-pass styles. 400 KB sits between the
        // two: below GWC's conservative peak (guard fires) but above what the live render actually needs (fallback
        // succeeds instead of also being denied by the live path's own limit).
        setMaxRequestMemory(400);
        try {
            MockHttpServletResponse response = getAsServletResponse(coalescedGetMap(layer1 + "," + layer2));

            // the guard is a fallback, not a hard error: the live combined render still succeeds
            assertEquals(200, response.getStatus());
            assertEquals("image/png", response.getContentType());
            assertEquals("MISS", response.getHeader("geowebcache-cache-result"));
            assertTrue(response.getHeader("geowebcache-miss-reason").contains("exceed max request memory"));

            // fired before loading any tile: neither member's cache was touched
            assertEquals(StorageObject.Status.MISS, sampleTile(layer1).getStatus());
            assertEquals(StorageObject.Status.MISS, sampleTile(layer2).getStatus());
        } finally {
            setMaxRequestMemory(0);
        }
    }

    private void setMaxRequestMemory(int kilobytes) throws Exception {
        GeoServer geoServer = getGeoServer();
        WMSInfo wms = geoServer.getService(WMSInfo.class);
        wms.setMaxRequestMemory(kilobytes);
        geoServer.save(wms);
    }

    private void setCachingMetadata(String layerId, boolean cachingEnabled, int cacheAgeMax) throws Exception {
        FeatureTypeInfo ft = getCatalog().getResourceByName(layerId, FeatureTypeInfo.class);
        ft.getMetadata().put(ResourceInfo.CACHING_ENABLED, cachingEnabled);
        ft.getMetadata().put(ResourceInfo.CACHE_AGE_MAX, cacheAgeMax);
        getCatalog().save(ft);
    }
}
