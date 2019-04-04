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

    public TilePageCalculator getTilePageCalculator() {
        return delegate.getTilePageCalculator();
    }

    public void createLayer(String layerName) throws InterruptedException {
        delegate.createLayer(layerName);
    }

    public Quota getGloballyUsedQuota() throws InterruptedException {
        return delegate.getGloballyUsedQuota();
    }

    public Quota getUsedQuotaByTileSetId(String tileSetId) throws InterruptedException {
        return delegate.getUsedQuotaByTileSetId(tileSetId);
    }

    public void deleteLayer(String layerName) {
        delegate.deleteLayer(layerName);
    }

    public void renameLayer(String oldLayerName, String newLayerName) throws InterruptedException {
        delegate.renameLayer(oldLayerName, newLayerName);
    }

    public Quota getUsedQuotaByLayerName(String layerName) throws InterruptedException {
        return delegate.getUsedQuotaByLayerName(layerName);
    }

    public long[][] getTilesForPage(TilePage page) throws InterruptedException {
        return delegate.getTilesForPage(page);
    }

    public Set<TileSet> getTileSets() {
        return delegate.getTileSets();
    }

    public TileSet getTileSetById(String tileSetId) throws InterruptedException {
        return delegate.getTileSetById(tileSetId);
    }

    public void accept(TileSetVisitor visitor) {
        delegate.accept(visitor);
    }

    public void addToQuotaAndTileCounts(
            TileSet tileSet, Quota quotaDiff, Collection<PageStatsPayload> tileCountDiffs)
            throws InterruptedException {
        delegate.addToQuotaAndTileCounts(tileSet, quotaDiff, tileCountDiffs);
    }

    public Future<List<PageStats>> addHitsAndSetAccesTime(
            Collection<PageStatsPayload> statsUpdates) {
        return delegate.addHitsAndSetAccesTime(statsUpdates);
    }

    public TilePage getLeastFrequentlyUsedPage(Set<String> layerNames) throws InterruptedException {
        return delegate.getLeastFrequentlyUsedPage(layerNames);
    }

    public TilePage getLeastRecentlyUsedPage(Set<String> layerNames) throws InterruptedException {
        return delegate.getLeastRecentlyUsedPage(layerNames);
    }

    public PageStats setTruncated(TilePage tilePage) throws InterruptedException {
        return delegate.setTruncated(tilePage);
    }

    public void deleteGridSubset(String layerName, String gridSetId) {
        delegate.deleteGridSubset(layerName, gridSetId);
    }

    public void close() throws Exception {
        delegate.close();
    }

    @Override
    public void deleteParameters(String layerName, String parametersId) {
        delegate.deleteParameters(layerName, parametersId);
    }
}
