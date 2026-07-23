/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.containers;

import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import org.geofence.core.services.RuleReaderService;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.geofence.config.GeoFenceConfiguration;
import org.geoserver.geofence.services.RuleReaderServiceFactory;
import org.geoserver.geofence.util.GeomHelper;
import org.geoserver.security.impl.LayerGroupContainmentCache;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Compute the containers auth access.
 *
 * <p>Default implementation that calls GeoFence endpoints and merges the containers' results.
 *
 * @author Emanuele Tajariol- GeoSolutions
 */
@Component
public class DefaultContainerAccessResolver implements ContainerAccessResolver {

    static final Logger LOGGER = Logging.getLogger(DefaultContainerAccessResolver.class);

    private final RuleReaderServiceFactory ruleReaderServiceFactory;

    @Autowired
    public DefaultContainerAccessResolver(
            @Qualifier("ruleReaderFrontendFactory") RuleReaderServiceFactory ruleReaderServiceFactory) {
        this.ruleReaderServiceFactory = ruleReaderServiceFactory;
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
        RuleReaderService ruleReaderService = ruleReaderServiceFactory.getService();
        ContainerLimitResolver resolver;
        if (summaries != null) {
            resolver = new ContainerLimitResolver(
                    summaries, ruleReaderService, user, layer, workspace, callerIp, configuration);
        } else {
            resolver = new ContainerLimitResolver(
                    containers, ruleReaderService, user, layer, workspace, callerIp, configuration);
        }

        ContainerLimitResolver.ProcessingResult result = resolver.resolveResourceInGroupLimits();
        Geometry intersect = result.getIntersectArea();
        Geometry clip = result.getClipArea();
        // areas might be in a srid different from the one of the resource
        // being requested.
        CoordinateReferenceSystem crs = GeomHelper.getCRSFromInfo(resourceInfo);
        if (intersect != null) {
            intersect = GeomHelper.reprojectGeometry(intersect, crs);
            result.setIntersectArea(intersect);
        }
        if (clip != null) {
            clip = GeomHelper.reprojectGeometry(clip, crs);
            result.setClipArea(clip);
        }
        return result;
    }
}
