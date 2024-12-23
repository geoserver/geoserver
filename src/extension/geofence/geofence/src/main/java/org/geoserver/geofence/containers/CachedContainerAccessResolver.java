/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.containers;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.geofence.cache.CacheManager;
import org.geoserver.geofence.config.GeoFenceConfiguration;
import org.geoserver.security.impl.LayerGroupContainmentCache;
import org.geotools.util.logging.Logging;
import org.springframework.security.core.Authentication;

/**
 * Provides the Container access results using the cache.
 *
 * @author Emanuele Tajariol- GeoSolutions
 */
public class CachedContainerAccessResolver implements ContainerAccessResolver {

    static final Logger LOGGER = Logging.getLogger(CachedContainerAccessResolver.class);

    private CacheManager cacheManager;

    public CachedContainerAccessResolver(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public ContainerLimitResolver.ProcessingResult getContainerResolverResult(
            CatalogInfo resourceInfo,
            String layer,
            String workspace,
            GeoFenceConfiguration configuration,
            String callerIp,
            Authentication user,
            List<LayerGroupInfo> containers,
            Collection<LayerGroupContainmentCache.LayerGroupSummary> summaries) {

        ContainerAccessCacheLoaderFactory.ResolveParams params = new ContainerAccessCacheLoaderFactory.ResolveParams(
                resourceInfo, layer, workspace, configuration, callerIp, user, containers, summaries);
        try {
            return cacheManager.getContainerCache().get(params);
        } catch (ExecutionException ex) {
            throw new RuntimeException(ex); // fixme: handle me
        }
    }
}
