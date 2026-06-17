/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.security;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.impl.LayerGroupContainmentCache;
import org.geoserver.security.impl.LayerGroupContainmentCache.LayerGroupSummary;
import org.geotools.util.logging.Logging;
import org.geowebcache.filter.parameters.ParametersUtils;
import org.geowebcache.layer.TileLayerDispatcher;
import org.geowebcache.storage.StorageBroker;
import org.geowebcache.storage.StorageException;
import org.springframework.beans.factory.InitializingBean;

/**
 * Translates {@link SecurityConfigurationChangeEvent}s into GWC tile deletions. For each affected layer it drops the
 * security-keyed tiles (those carrying {@link SecurityParameterFilter#ACCESS_LIMITS_KEY}); when the event carries a
 * tag, deletion is restricted to the tiles whose {@link SecurityParameterFilter#SECURITY_TAGS_KEY} contains it. Tiles
 * without {@code ACCESS_LIMITS_KEY} (unrestricted access) are never touched.
 *
 * <p>At startup it registers with every {@link SecurityCacheInvalidationSource} bean in the context.
 */
public class SecurityCacheInvalidator implements SecurityCacheInvalidationListener, InitializingBean {

    private static final Logger LOGGER = Logging.getLogger(SecurityCacheInvalidator.class);

    private final StorageBroker storageBroker;
    private final TileLayerDispatcher tileLayerDispatcher;
    private final Catalog catalog;
    private final LayerGroupContainmentCache containmentCache;

    public SecurityCacheInvalidator(
            StorageBroker storageBroker,
            TileLayerDispatcher tileLayerDispatcher,
            Catalog catalog,
            LayerGroupContainmentCache containmentCache) {
        this.storageBroker = storageBroker;
        this.tileLayerDispatcher = tileLayerDispatcher;
        this.catalog = catalog;
        this.containmentCache = containmentCache;
    }

    @Override
    public void afterPropertiesSet() {
        List<SecurityCacheInvalidationSource> sources =
                GeoServerExtensions.extensions(SecurityCacheInvalidationSource.class);
        for (SecurityCacheInvalidationSource source : sources) {
            source.register(this);
        }
        if (!sources.isEmpty()) {
            LOGGER.config("GWC security cache invalidator registered with " + sources.size() + " source(s)");
        }
    }

    @Override
    public void onSecurityConfigChange(SecurityConfigurationChangeEvent event) {
        Set<String> layerNames = resolveLayerNames(event.affectedLayers());
        for (String layerName : layerNames) {
            try {
                invalidateLayer(layerName, event.targetTags());
            } catch (StorageException e) {
                LOGGER.log(
                        Level.WARNING,
                        "Failed to invalidate security tile cache for layer '" + layerName + "': " + e.getMessage(),
                        e);
            }
        }
    }

    private Set<String> resolveLayerNames(Set<LayerInfo> affectedLayers) {
        if (affectedLayers == null) return tileLayerDispatcher.getLayerNames();
        Set<String> tileLayers = tileLayerDispatcher.getLayerNames();
        Set<String> names = new HashSet<>();
        for (LayerInfo layer : affectedLayers) {
            // resolve to the catalog instance: prefixedName can be rewritten by workspace-local decorators that
            // un-prefix layer names, making the passed-in copy unreliable as a tile-layer name
            LayerInfo canonical = catalog.getLayer(layer.getId());
            LayerInfo resolved = canonical != null ? canonical : layer;
            names.add(resolved.prefixedName());
            // layer-group tiles embed each member's limits, so a member change must also invalidate every containing
            // group; the containment cache returns transitive containers without scanning the whole catalog.
            // includeSingle=true: GWC caches SINGLE-mode group tiles too, unlike security rule evaluation
            ResourceInfo resource = resolved.getResource();
            if (resource == null) continue;
            for (LayerGroupSummary group : containmentCache.getContainerGroupsFor(resource, true)) {
                String groupName = group.prefixedName();
                if (tileLayers.contains(groupName)) names.add(groupName);
            }
        }
        return names;
    }

    private void invalidateLayer(String layerName, Set<String> targetTags) throws StorageException {
        Set<Map<String, String>> cached = storageBroker.getCachedParameters(layerName);
        if (cached == null || cached.isEmpty()) return;
        int deleted = 0;
        for (Map<String, String> params : cached) {
            if (!params.containsKey(SecurityParameterFilter.ACCESS_LIMITS_KEY)) continue;
            if (targetTags != null
                    && !containsAnyTag(params.get(SecurityParameterFilter.SECURITY_TAGS_KEY), targetTags)) continue;
            // guard each delete so one failure does not abort the remaining sets for this layer
            try {
                storageBroker.deleteByParametersId(layerName, ParametersUtils.getId(params));
                deleted++;
            } catch (Exception e) {
                LOGGER.log(
                        Level.WARNING,
                        "Failed to delete a security tile set for layer '" + layerName + "': " + e.getMessage(),
                        e);
            }
        }
        if (deleted > 0) {
            LOGGER.fine("GWC security: invalidated " + deleted + " parameter set(s) for layer '" + layerName + "'"
                    + (targetTags != null ? " (tags: " + targetTags + ")" : ""));
        }
    }

    /** True if the comma-separated {@code tagsValue} stored on a tile contains any of {@code targetTags}. */
    static boolean containsAnyTag(String tagsValue, Set<String> targetTags) {
        if (tagsValue == null || tagsValue.isEmpty()) return false;
        for (String t : tagsValue.split(",")) {
            if (targetTags.contains(t.trim())) return true;
        }
        return false;
    }
}
