/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.containers;

import com.google.common.cache.CacheLoader;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.geofence.config.GeoFenceConfiguration;
import org.geoserver.security.impl.LayerGroupContainmentCache;
import org.geotools.util.logging.Logging;
import org.springframework.security.core.Authentication;

/**
 * Provide the CacheLoader that computes the Containers access.
 *
 * @author Emanuele Tajariol- GeoSolutions
 */
public class ContainerAccessCacheLoaderFactory {

    static final Logger LOGGER = Logging.getLogger(ContainerAccessCacheLoaderFactory.class);

    private final DefaultContainerAccessResolver resolver;

    public ContainerAccessCacheLoaderFactory(DefaultContainerAccessResolver resolver) {
        this.resolver = resolver;
    }

    public ProcessingResultLoader createProcessingResultLoader() {
        return new ProcessingResultLoader();
    }

    protected class ProcessingResultLoader extends CacheLoader<ResolveParams, ContainerLimitResolver.ProcessingResult> {

        private ProcessingResultLoader() {}

        @Override
        public ContainerLimitResolver.ProcessingResult load(ResolveParams params) throws Exception {
            if (LOGGER.isLoggable(Level.FINE)) LOGGER.log(Level.FINE, "Loading results for {0}", params.layer);

            return resolver.getContainerResolverResult(
                    params.resourceInfo,
                    params.layer,
                    params.workspace,
                    params.configuration,
                    params.callerIp,
                    params.user,
                    params.containers,
                    params.summaries);
        }

        @Override
        public ListenableFuture<ContainerLimitResolver.ProcessingResult> reload(
                final ResolveParams params, ContainerLimitResolver.ProcessingResult result) throws Exception {
            if (LOGGER.isLoggable(Level.FINE)) LOGGER.log(Level.FINE, "Reloading {0}", params.layer);

            // this is a sync implementation
            ContainerLimitResolver.ProcessingResult ret = resolver.getContainerResolverResult(
                    params.resourceInfo,
                    params.layer,
                    params.workspace,
                    params.configuration,
                    params.callerIp,
                    params.user,
                    params.containers,
                    params.summaries);

            return Futures.immediateFuture(ret);
        }
    }

    public static class ResolveParams {
        CatalogInfo resourceInfo;
        String layer;
        String workspace;
        GeoFenceConfiguration configuration;
        String callerIp;
        Authentication user;
        List<LayerGroupInfo> containers;
        Collection<LayerGroupContainmentCache.LayerGroupSummary> summaries;

        public ResolveParams(
                CatalogInfo resourceInfo,
                String layer,
                String workspace,
                GeoFenceConfiguration configuration,
                String callerIp,
                Authentication user,
                List<LayerGroupInfo> containers,
                Collection<LayerGroupContainmentCache.LayerGroupSummary> summaries) {
            this.resourceInfo = resourceInfo;
            this.layer = layer;
            this.workspace = workspace;
            this.configuration = configuration;
            this.callerIp = callerIp;
            this.user = user;
            this.containers = containers;
            this.summaries = summaries;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 23 * hash + Objects.hashCode(this.resourceInfo);
            hash = 23 * hash + Objects.hashCode(this.layer);
            hash = 23 * hash + Objects.hashCode(this.workspace);
            hash = 23 * hash + Objects.hashCode(this.configuration);
            hash = 23 * hash + Objects.hashCode(this.callerIp);
            hash = 23 * hash + Objects.hashCode(this.user);
            hash = 23 * hash + Objects.hashCode(this.containers);
            hash = 23 * hash + Objects.hashCode(this.summaries);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ResolveParams other = (ResolveParams) obj;
            if (!Objects.equals(this.layer, other.layer)) {
                return false;
            }
            if (!Objects.equals(this.workspace, other.workspace)) {
                return false;
            }
            if (!Objects.equals(this.callerIp, other.callerIp)) {
                return false;
            }
            if (!Objects.equals(this.resourceInfo, other.resourceInfo)) {
                return false;
            }
            if (!Objects.equals(this.configuration, other.configuration)) {
                return false;
            }
            if (!Objects.equals(this.user, other.user)) {
                return false;
            }
            if (!Objects.equals(this.containers, other.containers)) {
                return false;
            }
            return Objects.equals(this.summaries, other.summaries);
        }
    }
}
