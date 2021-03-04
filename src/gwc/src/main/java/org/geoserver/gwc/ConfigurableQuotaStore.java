/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;
import org.geowebcache.diskquota.QuotaStore;
import org.geowebcache.diskquota.storage.PageStats;
import org.geowebcache.diskquota.storage.PageStatsPayload;
import org.geowebcache.diskquota.storage.Quota;
import org.geowebcache.diskquota.storage.TilePage;
import org.geowebcache.diskquota.storage.TilePageCalculator;
import org.geowebcache.diskquota.storage.TileSet;
import org.geowebcache.diskquota.storage.TileSetVisitor;

/**
 * A {@link QuotaStore} delegating to another instance of {@link QuotaStore}, and allowing the
 * delegate to be changed at runtime.
 *
 * @author Andrea Aime - GeoSolutions
 */
public class ConfigurableQuotaStore implements QuotaStore {

    static final Logger LOGGER = Logging.getLogger(ConfigurableQuotaStore.class);

    private QuotaStore delegate;

    public void setStore(QuotaStore delegate) {
        this.delegate = delegate;
    }

    public QuotaStore getStore() {
        return delegate;
    }

    public ConfigurableQuotaStore(QuotaStore delegate) {
        this.delegate = delegate;
    }

    @Override
    public TilePageCalculator getTilePageCalculator() {
        return delegate.getTilePageCalculator();
    }

    @Override
    public void createLayer(String layerName) throws InterruptedException {
        delegate.createLayer(layerName);
    }

    @Override
    public Quota getGloballyUsedQuota() throws InterruptedException {
        return delegate.getGloballyUsedQuota();
    }

    @Override
    public Quota getUsedQuotaByTileSetId(String tileSetId) throws InterruptedException {
        return delegate.getUsedQuotaByTileSetId(tileSetId);
    }

    @Override
    public void deleteLayer(String layerName) {
        delegate.deleteLayer(layerName);
    }

    @Override
    public void renameLayer(String oldLayerName, String newLayerName) throws InterruptedException {
        delegate.renameLayer(oldLayerName, newLayerName);
    }

    @Override
    public Quota getUsedQuotaByLayerName(String layerName) throws InterruptedException {
        return delegate.getUsedQuotaByLayerName(layerName);
    }

    @Override
    public long[][] getTilesForPage(TilePage page) throws InterruptedException {
        return delegate.getTilesForPage(page);
    }

    @Override
    public Set<TileSet> getTileSets() {
        return delegate.getTileSets();
    }

    @Override
    public TileSet getTileSetById(String tileSetId) throws InterruptedException {
        return delegate.getTileSetById(tileSetId);
    }

    @Override
    public void accept(TileSetVisitor visitor) {
        delegate.accept(visitor);
    }

    @Override
    public void addToQuotaAndTileCounts(
            TileSet tileSet, Quota quotaDiff, Collection<PageStatsPayload> tileCountDiffs)
            throws InterruptedException {
        delegate.addToQuotaAndTileCounts(tileSet, quotaDiff, tileCountDiffs);
    }

    @Override
    public Future<List<PageStats>> addHitsAndSetAccesTime(
            Collection<PageStatsPayload> statsUpdates) {
        return delegate.addHitsAndSetAccesTime(statsUpdates);
    }

    @Override
    public TilePage getLeastFrequentlyUsedPage(Set<String> layerNames) throws InterruptedException {
        return delegate.getLeastFrequentlyUsedPage(layerNames);
    }

    @Override
    public TilePage getLeastRecentlyUsedPage(Set<String> layerNames) throws InterruptedException {
        return delegate.getLeastRecentlyUsedPage(layerNames);
    }

    @Override
    public PageStats setTruncated(TilePage tilePage) throws InterruptedException {
        return delegate.setTruncated(tilePage);
    }

    @Override
    public void deleteGridSubset(String layerName, String gridSetId) {
        delegate.deleteGridSubset(layerName, gridSetId);
    }

    @Override
    public void close() throws Exception {
        delegate.close();
    }

    @Override
    public void deleteParameters(String layerName, String parametersId) {
        delegate.deleteParameters(layerName, parametersId);
    }
}
