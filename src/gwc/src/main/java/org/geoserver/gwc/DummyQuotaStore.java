/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.geowebcache.diskquota.QuotaStore;
import org.geowebcache.diskquota.storage.PageStats;
import org.geowebcache.diskquota.storage.PageStatsPayload;
import org.geowebcache.diskquota.storage.Quota;
import org.geowebcache.diskquota.storage.TilePage;
import org.geowebcache.diskquota.storage.TilePageCalculator;
import org.geowebcache.diskquota.storage.TileSet;
import org.geowebcache.diskquota.storage.TileSetVisitor;

public class DummyQuotaStore implements QuotaStore {

    private static final Quota EMPTY_QUOTA = new Quota(BigInteger.valueOf(0));

    TilePageCalculator calculator;

    public DummyQuotaStore(TilePageCalculator calculator) {
        this.calculator = calculator;
    }

    @Override
    public void createLayer(String layerName) throws InterruptedException {}

    @Override
    public Quota getGloballyUsedQuota() throws InterruptedException {
        return EMPTY_QUOTA;
    }

    @Override
    public Quota getUsedQuotaByTileSetId(String tileSetId) throws InterruptedException {
        return EMPTY_QUOTA;
    }

    @Override
    public void deleteLayer(String layerName) {}

    @Override
    public void renameLayer(String oldLayerName, String newLayerName) throws InterruptedException {}

    @Override
    public Quota getUsedQuotaByLayerName(String layerName) throws InterruptedException {
        return EMPTY_QUOTA;
    }

    @Override
    public long[][] getTilesForPage(TilePage page) throws InterruptedException {
        TileSet tileSet = getTileSetById(page.getTileSetId());
        long[][] gridCoverage = calculator.toGridCoverage(tileSet, page);
        return gridCoverage;
    }

    @Override
    public Set<TileSet> getTileSets() {
        return Collections.emptySet();
    }

    @Override
    public TileSet getTileSetById(String tileSetId) throws InterruptedException {
        return null;
    }

    @Override
    public void accept(TileSetVisitor visitor) {}

    @Override
    public TilePageCalculator getTilePageCalculator() {
        return calculator;
    }

    @Override
    public void addToQuotaAndTileCounts(
            TileSet tileSet, Quota quotaDiff, Collection<PageStatsPayload> tileCountDiffs)
            throws InterruptedException {}

    @Override
    public Future<List<PageStats>> addHitsAndSetAccesTime(
            Collection<PageStatsPayload> statsUpdates) {
        return new Future<List<PageStats>>() {

            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return true;
            }

            @Override
            public boolean isCancelled() {
                return true;
            }

            @Override
            public boolean isDone() {
                return true;
            }

            @Override
            public List<PageStats> get() throws InterruptedException, ExecutionException {
                return Collections.emptyList();
            }

            @Override
            public List<PageStats> get(long timeout, TimeUnit unit)
                    throws InterruptedException, ExecutionException, TimeoutException {
                return Collections.emptyList();
            }
        };
    }

    @Override
    public TilePage getLeastFrequentlyUsedPage(Set<String> layerNames) throws InterruptedException {
        return null;
    }

    @Override
    public TilePage getLeastRecentlyUsedPage(Set<String> layerNames) throws InterruptedException {
        return null;
    }

    @Override
    public PageStats setTruncated(TilePage tilePage) throws InterruptedException {
        return null;
    }

    @Override
    public void deleteGridSubset(String layerName, String gridSetId) {}

    @Override
    public void close() throws Exception {}

    @Override
    public void deleteParameters(String layerName, String parametersId) {}
}
