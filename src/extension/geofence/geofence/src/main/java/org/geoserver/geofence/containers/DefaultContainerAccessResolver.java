/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.containers;

import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.geofence.config.GeoFenceConfiguration;
import org.geoserver.geofence.services.RuleReaderService;
import org.geoserver.geofence.util.GeomHelper;
import org.geoserver.security.impl.LayerGroupContainmentCache;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.springframework.security.core.Authentication;

/**
 * Compute the containers auth access.
 *
 * <p>Default implementation that calls GeoFence endpoints and merges the containers' results.
 *
 * @author Emanuele Tajariol- GeoSolutions
 */
public class DefaultContainerAccessResolver implements ContainerAccessResolver {

    static final Logger LOGGER = Logging.getLogger(DefaultContainerAccessResolver.class);

    private RuleReaderService ruleReaderService;

    public DefaultContainerAccessResolver() {}

    public DefaultContainerAccessResolver(RuleReaderService rulesService) {
        this.ruleReaderService = rulesService;
    }

    public void setRuleReaderService(RuleReaderService ruleReaderService) {
        this.ruleReaderService = ruleReaderService;
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
