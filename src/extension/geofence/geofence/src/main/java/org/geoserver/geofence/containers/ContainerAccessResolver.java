/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.containers;

import java.util.Collection;
import java.util.List;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.geofence.config.GeoFenceConfiguration;
import org.geoserver.security.impl.LayerGroupContainmentCache;
import org.springframework.security.core.Authentication;

/**
 * Provides a method for computing the ProcessingResult. Two implementation for this interface: - the default one, that
 * performs the real computation - the cached one, that provides a layer of caching above the default implementation
 *
 * @author Emanuele Tajariol- GeoSolutions
 */
public interface ContainerAccessResolver {

    /** Resolve the resource limits taking in consideration the limits of a layer group. */
    ContainerLimitResolver.ProcessingResult getContainerResolverResult(
            CatalogInfo resourceInfo,
            String layer,
            String workspace,
            GeoFenceConfiguration configuration,
            String callerIp,
            Authentication user,
            List<LayerGroupInfo> containers,
            Collection<LayerGroupContainmentCache.LayerGroupSummary> summaries);
}
