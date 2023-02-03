/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.LocalWorkspaceCatalog;
import org.geoserver.geofence.config.GeoFenceConfiguration;
import org.geoserver.geofence.config.GeoFenceConfigurationManager;
import org.geoserver.geofence.core.model.LayerAttribute;
import org.geoserver.geofence.core.model.enums.AccessType;
import org.geoserver.geofence.core.model.enums.GrantType;
import org.geoserver.geofence.services.RuleReaderService;
import org.geoserver.geofence.services.dto.AccessInfo;
import org.geoserver.geofence.services.dto.CatalogModeDTO;
import org.geoserver.geofence.services.dto.RuleFilter;
import org.geoserver.geofence.util.AccessInfoUtils;
import org.geoserver.geofence.util.GeomHelper;
import org.geoserver.geofence.wpscommon.WPSHelper;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.DispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.ows.Response;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.platform.ExtensionPriority;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;
import org.geoserver.security.AccessLimits;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.CoverageAccessLimits;
import org.geoserver.security.DataAccessLimits;
import org.geoserver.security.LayerGroupAccessLimits;
import org.geoserver.security.ResourceAccessManager;
import org.geoserver.security.StyleAccessLimits;
import org.geoserver.security.VectorAccessLimits;
import org.geoserver.security.WMSAccessLimits;
import org.geoserver.security.WMTSAccessLimits;
import org.geoserver.security.WorkspaceAccessLimits;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.LayerGroupContainmentCache;
import org.geoserver.wms.GetFeatureInfoRequest;
import org.geoserver.wms.GetLegendGraphicRequest;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMS;
import org.geoserver.wms.map.GetMapKvpRequestReader;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.styling.Style;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.PropertyName;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Makes GeoServer use the Geofence to assess data access rules
 *
 * @author Andrea Aime - GeoSolutions
 * @author Emanuele Tajariol- GeoSolutions
 */
public class GeofenceAccessManager
        implements ResourceAccessManager, DispatcherCallback, ExtensionPriority {

    private static final Logger LOGGER = Logging.getLogger(GeofenceAccessManager.class);

    /** The role given to the administrators */
    static final String ROOT_ROLE = "ROLE_ADMINISTRATOR";

    static final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2(null);

    enum PropertyAccessMode {
        READ,
        WRITE
    }

    static final CatalogMode DEFAULT_CATALOG_MODE = CatalogMode.HIDE;

    RuleReaderService rulesService;

    Catalog catalog;

    private final GeoFenceConfigurationManager configurationManager;

    private LayerGroupContainmentCache groupsCache;

    private WPSHelper wpsHelper;

    public GeofenceAccessManager(
            RuleReaderService rulesService,
            Catalog catalog,
            GeoFenceConfigurationManager configurationManager,
            WPSHelper wpsHelper) {

        this.rulesService = rulesService;
        this.catalog = new LocalWorkspaceCatalog(catalog);
        this.configurationManager = configurationManager;
        this.groupsCache = new LayerGroupContainmentCache(catalog);
        this.wpsHelper = wpsHelper;
    }

    /**
     * sets the layer group cache
     *
     * @param groupsCache
     */
    public void setGroupsCache(LayerGroupContainmentCache groupsCache) {
        this.groupsCache = groupsCache;
    }

    boolean isAdmin(Authentication user) {
        if (user.getAuthorities() != null) {
            for (GrantedAuthority authority : user.getAuthorities()) {
                final String userRole = authority.getAuthority();
                if (ROOT_ROLE.equals(userRole)
                        || GeoServerRole.ADMIN_ROLE.getAuthority().equals(userRole)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public WorkspaceAccessLimits getAccessLimits(Authentication user, WorkspaceInfo workspace) {
        LOGGER.log(Level.FINE, "Getting access limits for workspace {0}", workspace.getName());

        if ((user != null) && !(user instanceof AnonymousAuthenticationToken)) {
            // shortcut, if the user is the admin, he can do everything
            if (isAdmin(user)) {
                LOGGER.log(
                        Level.FINE,
                        "Admin level access, returning full rights for workspace {0}",
                        workspace.getName());

                return new WorkspaceAccessLimits(DEFAULT_CATALOG_MODE, true, true);
            }

            boolean canWrite =
                    configurationManager
                            .getConfiguration()
                            .isGrantWriteToWorkspacesToAuthenticatedUsers();
            boolean canAdmin = isWorkspaceAdmin(user, workspace.getName());

            return new WorkspaceAccessLimits(DEFAULT_CATALOG_MODE, true, canWrite, canAdmin);
        }

        // further logic disabled because of
        // https://github.com/geosolutions-it/geofence/issues/6 (gone)
        final boolean readable = true;
        final boolean writable = false;
        return new WorkspaceAccessLimits(DEFAULT_CATALOG_MODE, readable, writable);
    }

    /** We expect the user not to be null and not to be admin */
    private boolean isWorkspaceAdmin(Authentication user, String workspaceName) {
        LOGGER.log(Level.FINE, "Getting admin auth for Workspace {0}", workspaceName);

        RuleFilter ruleFilter = new RuleFilter(RuleFilter.SpecialFilterType.DEFAULT);
        setRuleFilterUserAndRole(user, ruleFilter);
        ruleFilter.setInstance(configurationManager.getConfiguration().getInstanceName());
        ruleFilter.setWorkspace(workspaceName);

        String sourceAddress = retrieveCallerIpAddress();
        if (sourceAddress != null) {
            ruleFilter.setSourceAddress(sourceAddress);
        } else {
            LOGGER.log(Level.WARNING, "No source IP address found");
        }

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "AdminAuth filter: {0}", ruleFilter);
        }

        AccessInfo auth = rulesService.getAdminAuthorization(ruleFilter);

        LOGGER.log(
                Level.FINE,
                "Admin auth for User:{0} Workspace:{1}: {2}",
                new Object[] {user.getName(), workspaceName, auth.getAdminRights()});

        return auth.getAdminRights();
    }

    String getSourceAddress(HttpServletRequest http) {
        try {
            if (http == null) {
                LOGGER.log(Level.WARNING, "No HTTP connection available.");
                return null;
            }

            String forwardedFor = http.getHeader("X-Forwarded-For");
            if (forwardedFor != null) {
                String[] ips = forwardedFor.split(", ");

                return InetAddress.getByName(ips[0]).getHostAddress();
            } else {
                // Returns an IP address, removes surrounding brackets present in case of IPV6
                // addresses
                return http.getRemoteAddr().replaceAll("[\\[\\]]", "");
            }
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Failed to get remote address", e);
            return null;
        }
    }

    private String retrieveCallerIpAddress() {

        // is this an OWS request
        Request owsRequest = Dispatcher.REQUEST.get();
        if (owsRequest != null) {
            HttpServletRequest httpReq = owsRequest.getHttpRequest();
            String sourceAddress = getSourceAddress(httpReq);
            if (sourceAddress == null) {
                LOGGER.log(Level.WARNING, "Could not retrieve source address from OWSRequest");
            }
            return sourceAddress;
        }

        // try Spring
        try {
            HttpServletRequest request =
                    ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                            .getRequest();
            String sourceAddress = getSourceAddress(request);
            if (sourceAddress == null) {
                LOGGER.log(Level.WARNING, "Could not retrieve source address with Spring Request");
            }
            return sourceAddress;
        } catch (IllegalStateException ex) {
            LOGGER.log(
                    Level.WARNING,
                    "Error retrieving source address with Spring Request: " + ex.getMessage());
            return null;
        }
    }

    @Override
    public StyleAccessLimits getAccessLimits(Authentication user, StyleInfo style) {
        // return getAccessLimits(user, style.getResource());
        LOGGER.fine("Not limiting styles");
        return null;
        // TODO
    }

    @Override
    public LayerGroupAccessLimits getAccessLimits(Authentication user, LayerGroupInfo layerInfo) {
        return getAccessLimits(user, layerInfo, Collections.emptyList());
    }

    @Override
    public DataAccessLimits getAccessLimits(Authentication user, LayerInfo layer) {
        LOGGER.log(Level.FINE, "Getting access limits for Layer {0}", layer.getName());
        return getAccessLimits(user, layer, Collections.emptyList());
    }

    @Override
    public DataAccessLimits getAccessLimits(Authentication user, ResourceInfo resource) {
        LOGGER.log(Level.FINE, "Getting access limits for Resource {0}", resource.getName());
        // extract the user name
        String workspace = resource.getStore().getWorkspace().getName();
        String layer = resource.getName();
        return (DataAccessLimits)
                getAccessLimits(user, resource, layer, workspace, Collections.emptyList());
    }

    @Override
    public DataAccessLimits getAccessLimits(
            Authentication user, LayerInfo layer, List<LayerGroupInfo> containers) {
        String workspace = layer.getResource().getStore().getWorkspace().getName();
        String layerName = layer.getName();
        return (DataAccessLimits) getAccessLimits(user, layer, layerName, workspace, containers);
    }

    @Override
    public LayerGroupAccessLimits getAccessLimits(
            Authentication user, LayerGroupInfo layerGroup, List<LayerGroupInfo> containers) {
        WorkspaceInfo ws = layerGroup.getWorkspace();
        String workspace = ws != null ? ws.getName() : null;
        String layer = layerGroup.getName();
        return (LayerGroupAccessLimits)
                getAccessLimits(user, layerGroup, layer, workspace, containers);
    }

    private AccessLimits getAccessLimits(
            Authentication user,
            CatalogInfo info,
            String layer,
            String workspace,
            List<LayerGroupInfo> containers) {
        if ((user != null) && !(user instanceof AnonymousAuthenticationToken)) {
            // shortcut, if the user is the admin, he can do everything
            if (isAdmin(user)) {
                LOGGER.log(
                        Level.FINE,
                        "Admin level access, returning " + "full rights for layer {0}",
                        layer);
                return buildAdminAccessLimits(info);
            }
        }

        String ipAddress = retrieveCallerIpAddress();
        RuleFilter ruleFilter = buildRuleFilter(workspace, layer, user, ipAddress);
        AccessInfo accessInfo = rulesService.getAccessInfo(ruleFilter);

        if (accessInfo == null) {
            accessInfo = AccessInfo.DENY_ALL;
            LOGGER.log(
                    Level.WARNING, "GeoFence returning null AccessInfo for filter {0}", ruleFilter);
        }

        Request req = Dispatcher.REQUEST.get();
        String service = req != null ? req.getService() : null;
        boolean isWms = "WMS".equalsIgnoreCase(service);
        boolean noLayerGroups = CollectionUtils.isEmpty(containers);

        ContainerLimitResolver.ProcessingResult processingResult = null;
        if (noLayerGroups && isWms) {
            // is direct access we need to retrieve eventually present groups.
            Collection<LayerGroupContainmentCache.LayerGroupSummary> summaries =
                    getGroupSummary(info);
            if (summaries != null && !summaries.isEmpty()) {
                boolean allOpaque = allOpaque(summaries);
                // all opaque we deny and don't perform any resolution of group limits.
                if (allOpaque) accessInfo.setGrant(GrantType.DENY);
                boolean anySingle =
                        summaries.stream()
                                .anyMatch(gs -> gs.getMode().equals(LayerGroupInfo.Mode.SINGLE));
                // if a single group is present we don't apply any limit from containers.
                if (!anySingle && !allOpaque)
                    processingResult =
                            getContainerResolverResult(
                                    info,
                                    layer,
                                    workspace,
                                    configurationManager.getConfiguration(),
                                    ipAddress,
                                    user,
                                    null,
                                    summaries);
            }
        } else if (!noLayerGroups) {
            // layer is requested in context of a layer group.
            // we need to process the containers limits.
            processingResult =
                    getContainerResolverResult(
                            info,
                            layer,
                            workspace,
                            configurationManager.getConfiguration(),
                            ipAddress,
                            user,
                            containers,
                            null);
        }

        if ("WPS".equalsIgnoreCase(service)) {
            if (!noLayerGroups) {
                LOGGER.log(
                        Level.WARNING,
                        "Don't know how to deal with WPS requests for group data. Won't dive into single process limits.");
            } else {
                AccessInfoUtils.WPSAccessInfo resolvedAccessInfo =
                        wpsHelper.resolveWPSAccess(req, ruleFilter, accessInfo);
                if (resolvedAccessInfo != null) {
                    accessInfo = resolvedAccessInfo.getAccessInfo();
                    processingResult =
                            new ContainerLimitResolver.ProcessingResult(
                                    resolvedAccessInfo.getArea(),
                                    resolvedAccessInfo.getClip(),
                                    accessInfo.getCatalogMode());

                    LOGGER.log(
                            Level.FINE,
                            "Got WPS access {0} for layer {1} and user {2}",
                            new Object[] {accessInfo, layer, getUserNameFromAuth(user)});
                }
            }
        }

        AccessLimits limits;
        if (info instanceof LayerGroupInfo) {
            limits = buildLayerGroupAccessLimits(accessInfo);
        } else if (info instanceof ResourceInfo) {
            limits = buildResourceAccessLimits((ResourceInfo) info, accessInfo, processingResult);
        } else {
            limits =
                    buildResourceAccessLimits(
                            ((LayerInfo) info).getResource(), accessInfo, processingResult);
        }

        LOGGER.log(
                Level.FINE,
                "Returning {0} for layer {1} and user {2}",
                new Object[] {limits, layer, getUserNameFromAuth(user)});

        return limits;
    }

    private boolean allOpaque(Collection<LayerGroupContainmentCache.LayerGroupSummary> summaries) {
        LayerGroupInfo.Mode opaque = LayerGroupInfo.Mode.OPAQUE_CONTAINER;
        return summaries.stream().allMatch(gs -> gs.getMode().equals(opaque));
    }

    // build the accessLimits for an admin user
    private AccessLimits buildAdminAccessLimits(CatalogInfo info) {
        AccessLimits accessLimits;
        if (info instanceof LayerGroupInfo)
            accessLimits = buildLayerGroupAccessLimits(AccessInfo.ALLOW_ALL);
        else if (info instanceof ResourceInfo)
            accessLimits =
                    buildResourceAccessLimits((ResourceInfo) info, AccessInfo.ALLOW_ALL, null);
        else
            accessLimits =
                    buildResourceAccessLimits(
                            ((LayerInfo) info).getResource(), AccessInfo.ALLOW_ALL, null);
        return accessLimits;
    }

    private String getUserNameFromAuth(Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        if (username != null && username.isEmpty()) {
            username = null;
        }
        return username;
    }

    private Collection<LayerGroupContainmentCache.LayerGroupSummary> getGroupSummary(
            Object resource) {
        Collection<LayerGroupContainmentCache.LayerGroupSummary> summaries;
        if (resource instanceof ResourceInfo)
            summaries = groupsCache.getContainerGroupsFor((ResourceInfo) resource);
        else if (resource instanceof LayerInfo)
            summaries = groupsCache.getContainerGroupsFor(((LayerInfo) resource).getResource());
        else summaries = groupsCache.getContainerGroupsFor((LayerGroupInfo) resource);
        return summaries;
    }

    private void setRuleFilterUserAndRole(Authentication user, RuleFilter ruleFilter) {
        if (user != null) {
            GeoFenceConfiguration config = configurationManager.getConfiguration();

            // just some loggings here
            if (config.isUseRolesToFilter()) {
                if (config.getRoles().isEmpty()) {
                    LOGGER.log(
                            Level.WARNING,
                            "Role filtering requested, but no roles provided. Will only use user authorizations");
                }

                if (LOGGER.isLoggable(Level.FINE)) {
                    String authList =
                            user.getAuthorities().stream()
                                    .map(a -> a.getAuthority())
                                    .collect(Collectors.joining(",", "[", "]"));
                    LOGGER.log(
                            Level.FINE,
                            "Authorizations found for user {0}: {1}",
                            new Object[] {user.getName(), authList});

                    String allowedAuth =
                            config.getRoles().stream().collect(Collectors.joining(",", "[", "]"));
                    LOGGER.log(
                            Level.FINE, "Authorizations allowed: {0}", new Object[] {allowedAuth});
                }
            }

            if (config.isUseRolesToFilter() && !config.getRoles().isEmpty()) {

                boolean getAllRoles = config.getRoles().contains("*");
                Set<String> excluded =
                        config.getRoles().stream()
                                .filter(r -> r.startsWith("-"))
                                .map(r -> r.substring(1))
                                .collect(Collectors.toSet());

                List<String> roles = new ArrayList<>();
                for (GrantedAuthority authority : user.getAuthorities()) {
                    String authRole = authority.getAuthority();
                    boolean addRole = getAllRoles || config.getRoles().contains(authRole);
                    addRole = addRole && !(excluded.contains(authRole));

                    if (addRole) {
                        roles.add(authRole);
                    }
                }

                if (roles.isEmpty()) {
                    roles.add("UNKNOWN");
                }

                String joinedRoles = String.join(",", roles);

                LOGGER.log(Level.FINE, "Setting role for filter: {0}", new Object[] {joinedRoles});
                ruleFilter.setRole(joinedRoles);
            } else {
                ruleFilter.setRole(RuleFilter.SpecialFilterType.ANY);
            }

            String username = user.getName();
            if (StringUtils.isEmpty(username)) {
                LOGGER.log(Level.WARNING, "Username is null for user: {0}", new Object[] {user});
                ruleFilter.setUser(RuleFilter.SpecialFilterType.DEFAULT);
            } else {
                LOGGER.log(Level.FINE, "Setting user for filter: {0}", new Object[] {username});
                ruleFilter.setUser(username);
            }
        } else {
            LOGGER.log(Level.WARNING, "No user given");
            ruleFilter.setUser(RuleFilter.SpecialFilterType.DEFAULT);
        }
    }

    /**
     * Build the access info for a Resource, taking into account the containerRule if any exists
     *
     * @param info the ResourceInfo object for which the AccessLimits are requested
     * @param accessInfo the AccessInfo associated to the resource need to be reprojected due the
     *     possible difference between container and resource CRS
     * @return the AccessLimits of the Resource
     */
    AccessLimits buildResourceAccessLimits(
            ResourceInfo info,
            AccessInfo accessInfo,
            ContainerLimitResolver.ProcessingResult resultLimits) {

        GrantType actualGrant = accessInfo.getGrant();
        boolean includeFilter = actualGrant == GrantType.ALLOW || actualGrant == GrantType.LIMIT;
        Filter readFilter = includeFilter ? Filter.INCLUDE : Filter.EXCLUDE;
        Filter writeFilter = includeFilter ? Filter.INCLUDE : Filter.EXCLUDE;
        try {
            if (accessInfo.getCqlFilterRead() != null) {
                readFilter = ECQL.toFilter(accessInfo.getCqlFilterRead());
            }
            if (accessInfo.getCqlFilterWrite() != null) {
                writeFilter = ECQL.toFilter(accessInfo.getCqlFilterWrite());
            }
        } catch (CQLException e) {
            throw new IllegalArgumentException("Invalid cql filter found: " + e.getMessage(), e);
        }

        // get the attributes
        List<PropertyName> readAttributes =
                toPropertyNames(accessInfo.getAttributes(), PropertyAccessMode.READ);
        List<PropertyName> writeAttributes =
                toPropertyNames(accessInfo.getAttributes(), PropertyAccessMode.WRITE);

        Geometry intersectsArea;
        Geometry clipArea;
        if (resultLimits != null) {
            intersectsArea = resultLimits.getIntersectArea();
            clipArea = resultLimits.getClipArea();
        } else {
            CoordinateReferenceSystem crs = GeomHelper.getCRSFromInfo(info);

            intersectsArea = GeomHelper.parseWKT(accessInfo.getAreaWkt());
            intersectsArea = GeomHelper.reprojectGeometry(intersectsArea, crs);

            clipArea = GeomHelper.parseWKT(accessInfo.getClipAreaWkt());
            clipArea = GeomHelper.reprojectGeometry(clipArea, crs);
        }
        CatalogMode catalogMode = getCatalogMode(accessInfo, resultLimits);
        LOGGER.log(
                Level.FINE,
                "Returning mode {0} for resource {1}",
                new Object[] {catalogMode, info});

        AccessLimits accessLimits = null;
        if (info instanceof FeatureTypeInfo) {
            // merge the area among the filters
            if (intersectsArea != null) {
                Filter areaFilter = FF.intersects(FF.property(""), FF.literal(intersectsArea));
                if (clipArea != null) {
                    Filter intersectClipArea = FF.intersects(FF.property(""), FF.literal(clipArea));
                    areaFilter = FF.or(areaFilter, intersectClipArea);
                }
                readFilter = mergeFilter(readFilter, areaFilter);
                writeFilter = mergeFilter(writeFilter, areaFilter);
            }

            accessLimits =
                    new VectorAccessLimits(
                            catalogMode, readAttributes, readFilter, writeAttributes, writeFilter);

            if (clipArea != null) {
                ((VectorAccessLimits) accessLimits).setClipVectorFilter(clipArea);
            }
            if (intersectsArea != null)
                ((VectorAccessLimits) accessLimits).setIntersectVectorFilter(intersectsArea);

        } else if (info instanceof CoverageInfo) {

            Geometry finalArea = null;
            if (clipArea != null && intersectsArea != null)
                finalArea = clipArea.union(intersectsArea);
            else if (intersectsArea != null) finalArea = intersectsArea;
            else if (clipArea != null) finalArea = clipArea;

            accessLimits =
                    new CoverageAccessLimits(catalogMode, readFilter, toMultiPoly(finalArea), null);

        } else if (info instanceof WMSLayerInfo) {
            accessLimits =
                    new WMSAccessLimits(catalogMode, readFilter, toMultiPoly(intersectsArea), true);

        } else if (info instanceof WMTSLayerInfo) {
            accessLimits =
                    new WMTSAccessLimits(catalogMode, readFilter, toMultiPoly(intersectsArea));
        } else {
            throw new IllegalArgumentException("Don't know how to handle resource " + info);
        }

        return accessLimits;
    }

    /**
     * @param accessInfo the AccessInfo associated to the LayerGroup
     * @return the AccessLimits of the LayerGroup
     */
    AccessLimits buildLayerGroupAccessLimits(AccessInfo accessInfo) {
        GrantType grant = accessInfo.getGrant();
        // the SecureCatalog will grant access  to the layerGroup
        // if AccessLimits are null
        if (grant.equals(GrantType.ALLOW) || grant.equals(GrantType.LIMIT)) return null;
        else return new LayerGroupAccessLimits(convert(accessInfo.getCatalogMode()));
    }

    private ContainerLimitResolver.ProcessingResult getContainerResolverResult(
            CatalogInfo resourceInfo,
            String layer,
            String workspace,
            GeoFenceConfiguration configuration,
            String callerIp,
            Authentication user,
            List<LayerGroupInfo> containers,
            Collection<LayerGroupContainmentCache.LayerGroupSummary> summaries) {
        ContainerLimitResolver resolver;
        if (summaries != null)
            resolver =
                    new ContainerLimitResolver(
                            summaries,
                            rulesService,
                            user,
                            layer,
                            workspace,
                            callerIp,
                            configuration);
        else
            resolver =
                    new ContainerLimitResolver(
                            containers,
                            rulesService,
                            user,
                            layer,
                            workspace,
                            callerIp,
                            configuration);

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
    // get the catalogMode for the resource privileging the container one if passed
    private CatalogMode getCatalogMode(
            AccessInfo accessInfo, ContainerLimitResolver.ProcessingResult resultLimits) {
        CatalogModeDTO ruleCatalogMode;
        if (resultLimits != null) {
            ruleCatalogMode = resultLimits.getCatalogModeDTO();
        } else {
            ruleCatalogMode = accessInfo.getCatalogMode();
        }
        CatalogMode catalogMode = DEFAULT_CATALOG_MODE;
        if (ruleCatalogMode != null) {
            switch (ruleCatalogMode) {
                case CHALLENGE:
                    catalogMode = CatalogMode.CHALLENGE;
                    break;
                case HIDE:
                    catalogMode = CatalogMode.HIDE;
                    break;
                case MIXED:
                    catalogMode = CatalogMode.MIXED;
                    break;
            }
        }
        return catalogMode;
    }

    private CatalogMode convert(CatalogModeDTO ruleCatalogMode) {
        CatalogMode catalogMode = DEFAULT_CATALOG_MODE;
        if (ruleCatalogMode != null) {
            switch (ruleCatalogMode) {
                case CHALLENGE:
                    catalogMode = CatalogMode.CHALLENGE;
                    break;
                case HIDE:
                    catalogMode = CatalogMode.HIDE;
                    break;
                case MIXED:
                    catalogMode = CatalogMode.MIXED;
                    break;
            }
        }
        return catalogMode;
    }

    // Builds a rule filter to retrieve the AccessInfo for the resource
    private RuleFilter buildRuleFilter(
            String workspace, String layer, Authentication user, String ipAddress) {
        RuleFilterBuilder builder = new RuleFilterBuilder(configurationManager.getConfiguration());
        return builder.withRequest(Dispatcher.REQUEST.get())
                .withIpAddress(ipAddress)
                .withWorkspace(workspace)
                .withLayer(layer)
                .withUser(user)
                .build();
    }

    private MultiPolygon toMultiPoly(Geometry reprojArea) {
        MultiPolygon rasterFilter = null;
        if (reprojArea != null) {
            rasterFilter = Converters.convert(reprojArea, MultiPolygon.class);
            if (rasterFilter == null) {
                throw new RuntimeException(
                        "Error applying security rules, cannot convert "
                                + "the Geofence area restriction "
                                + reprojArea.toText()
                                + " to a multi-polygon");
            }
        }

        return rasterFilter;
    }

    /** Merges the two filters into one by AND */
    private Filter mergeFilter(Filter filter, Filter areaFilter) {
        if ((filter == null) || (filter == Filter.INCLUDE)) {
            return areaFilter;
        } else if (filter == Filter.EXCLUDE) {
            return filter;
        } else {
            return FF.and(filter, areaFilter);
        }
    }

    /** Builds the equivalent {@link PropertyName} list for the specified access mode */
    private List<PropertyName> toPropertyNames(
            Set<LayerAttribute> attributes, PropertyAccessMode mode) {
        // handle simple case
        if (attributes == null || attributes.isEmpty()) {
            return null;
        }

        // filter and translate
        List<PropertyName> result = new ArrayList<>();
        for (LayerAttribute attribute : attributes) {
            if ((attribute.getAccess() == AccessType.READWRITE)
                    || ((mode == PropertyAccessMode.READ)
                            && (attribute.getAccess() == AccessType.READONLY))) {
                PropertyName property = FF.property(attribute.getName());
                result.add(property);
            }
        }

        return result;
    }

    @Override
    public void finished(Request request) {
        // nothing to do
    }

    @Override
    public Request init(Request request) {
        return request;
    }

    @Override
    public Operation operationDispatched(Request gsRequest, Operation operation) {
        // service and request
        String service = gsRequest.getService();
        String request = gsRequest.getRequest();

        // get the user
        Authentication user = SecurityContextHolder.getContext().getAuthentication();
        String username = null;
        if ((user != null) && !(user instanceof AnonymousAuthenticationToken)) {
            // shortcut, if the user is the admin, he can do everything
            if (isAdmin(user)) {
                LOGGER.log(
                        Level.FINE,
                        "Admin level access, not applying default style for this request");

                return operation;
            } else {
                username = user.getName();
                if (username != null && username.isEmpty()) {
                    username = null;
                }
            }
        }

        if ((request != null)
                && "WMS".equalsIgnoreCase(service)
                && ("GetMap".equalsIgnoreCase(request)
                        || "GetFeatureInfo".equalsIgnoreCase(request))) {
            // extract the getmap part
            Object ro = operation.getParameters()[0];
            GetMapRequest getMap;
            if (ro instanceof GetMapRequest) {
                getMap = (GetMapRequest) ro;
            } else if (ro instanceof GetFeatureInfoRequest) {
                getMap = ((GetFeatureInfoRequest) ro).getGetMapRequest();
            } else {
                throw new ServiceException("Unrecognized request object: " + ro);
            }

            overrideGetMapRequest(gsRequest, service, request, user, getMap);
        } else if ((request != null)
                && "WMS".equalsIgnoreCase(service)
                && "GetLegendGraphic".equalsIgnoreCase(request)) {
            overrideGetLegendGraphicRequest(gsRequest, operation, service, request, user);
        }

        return operation;
    }

    void overrideGetLegendGraphicRequest(
            Request gsRequest,
            Operation operation,
            String service,
            String request,
            Authentication user) {
        // get the layer
        String layerName = (String) gsRequest.getKvp().get("LAYER");
        String reqStyle = (String) gsRequest.getKvp().get("STYLE");
        List<String> styles = new ArrayList<>();
        List<LayerInfo> layers = new ArrayList<>();
        LayerInfo candidateLayer = catalog.getLayerByName(layerName);
        if (candidateLayer == null) {
            LayerGroupInfo layerGroup = catalog.getLayerGroupByName(layerName);
            if (layerGroup != null) {
                boolean emptyStyleName = reqStyle == null || "".equals(reqStyle);
                layers.addAll(emptyStyleName ? layerGroup.layers() : layerGroup.layers(reqStyle));
                addGroupStyles(layerGroup, styles, reqStyle);
            }
        } else {
            layers.add(candidateLayer);
            styles.add(reqStyle);
        }

        // get the request object
        GetLegendGraphicRequest getLegend = (GetLegendGraphicRequest) operation.getParameters()[0];
        for (int i = 0; i < layers.size(); i++) {
            LayerInfo layer = layers.get(i);
            ResourceInfo resource = layer.getResource();

            // get the rule, it contains default and allowed styles
            RuleFilter ruleFilter = new RuleFilter(RuleFilter.SpecialFilterType.DEFAULT);
            setRuleFilterUserAndRole(user, ruleFilter);
            ruleFilter.setInstance(configurationManager.getConfiguration().getInstanceName());
            ruleFilter.setService(service);
            ruleFilter.setRequest(request);
            ruleFilter.setWorkspace(resource.getStore().getWorkspace().getName());
            ruleFilter.setLayer(resource.getName());

            LOGGER.log(Level.FINE, "Getting access limits for getLegendGraphic", ruleFilter);
            AccessInfo rule = rulesService.getAccessInfo(ruleFilter);

            // get the requested style
            String styleName = styles.get(i);
            if (styleName == null) {
                if (rule.getDefaultStyle() != null) {
                    try {
                        StyleInfo si = catalog.getStyleByName(rule.getDefaultStyle());
                        if (si == null) {
                            throw new ServiceException(
                                    "Could not find default style suggested "
                                            + "by GeoRepository: "
                                            + rule.getDefaultStyle());
                        }
                        getLegend.setStyle(si.getStyle());
                    } catch (IOException e) {
                        throw new ServiceException(
                                "Unable to load the style suggested by GeoRepository: "
                                        + rule.getDefaultStyle(),
                                e);
                    }
                }
            } else {
                checkStyleAllowed(rule, styleName);
            }
        }
    }

    void overrideGetMapRequest(
            Request gsRequest,
            String service,
            String request,
            Authentication user,
            GetMapRequest getMap) {

        if (gsRequest.getKvp().get("layers") == null
                && gsRequest.getKvp().get("sld") == null
                && gsRequest.getKvp().get("sld_body") == null) {
            throw new ServiceException("GetMap POST requests are forbidden");
        }

        // parse the styles param like the kvp parser would (since we have no way,
        // to know if a certain style was requested explicitly or defaulted, and
        // we need to tell apart the default case from the explicit request case
        List<String> styleNameList = getRequestedStyles(gsRequest, getMap);

        // apply the override/security check for each layer in the request
        List<MapLayerInfo> layers = getMap.getLayers();
        for (int i = 0; i < layers.size(); i++) {
            MapLayerInfo layer = layers.get(i);
            ResourceInfo info = null;
            if (layer.getType() == MapLayerInfo.TYPE_VECTOR
                    || layer.getType() == MapLayerInfo.TYPE_RASTER) {
                info = layer.getResource();
            } else if (!configurationManager.getConfiguration().isAllowRemoteAndInlineLayers()) {
                throw new ServiceException("Remote layers are not allowed");
            }

            // get the rule, it contains default and allowed styles
            RuleFilter ruleFilter = new RuleFilter(RuleFilter.SpecialFilterType.DEFAULT);

            setRuleFilterUserAndRole(user, ruleFilter);
            ruleFilter.setInstance(configurationManager.getConfiguration().getInstanceName());
            ruleFilter.setService(service);
            ruleFilter.setRequest(request);
            if (info != null) {
                ruleFilter.setWorkspace(info.getStore().getWorkspace().getName());
                ruleFilter.setLayer(info.getName());
            } else {
                ruleFilter.setWorkspace(RuleFilter.SpecialFilterType.DEFAULT);
                ruleFilter.setLayer(RuleFilter.SpecialFilterType.DEFAULT);
            }

            LOGGER.log(Level.FINE, "Getting access limits for getMap", ruleFilter);

            AccessInfo rule = rulesService.getAccessInfo(ruleFilter);

            // get the requested style name
            String styleName = styleNameList.get(i);

            // if default use geofence default
            if (styleName != null) {
                checkStyleAllowed(rule, styleName);
            } else if ((rule.getDefaultStyle() != null)) {
                try {
                    StyleInfo si = catalog.getStyleByName(rule.getDefaultStyle());
                    if (si == null) {
                        throw new ServiceException(
                                "Could not find default style suggested "
                                        + "by Geofence: "
                                        + rule.getDefaultStyle());
                    }

                    Style style = si.getStyle();
                    getMap.getStyles().set(i, style);
                } catch (IOException e) {
                    throw new ServiceException(
                            "Unable to load the style suggested by Geofence: "
                                    + rule.getDefaultStyle(),
                            e);
                }
            }
        }
    }

    private void checkStyleAllowed(AccessInfo accessInfo, String styleName) {
        // otherwise check if the requested style is allowed
        Set<String> allowedStyles = new HashSet<>();
        if (accessInfo.getDefaultStyle() != null) {
            allowedStyles.add(accessInfo.getDefaultStyle());
        }
        if (accessInfo.getAllowedStyles() != null) {
            allowedStyles.addAll(accessInfo.getAllowedStyles());
        }

        if ((!allowedStyles.isEmpty()) && !allowedStyles.contains(styleName)) {
            throw new ServiceException(
                    "The '" + styleName + "' style is not available on this layer");
        }
    }

    @Override
    public Filter getSecurityFilter(Authentication user, Class<? extends CatalogInfo> clazz) {
        return Predicates.acceptAll();
    }

    @Override
    public Object operationExecuted(Request request, Operation operation, Object result) {
        return result;
    }

    @Override
    public Response responseDispatched(
            Request request, Operation operation, Object result, Response response) {
        return response;
    }

    @Override
    public Service serviceDispatched(Request request, Service service) throws ServiceException {
        return service;
    }

    /**
     * Returns a list that contains the request styles that will correspond to the
     * GetMap.getLayers().
     */
    private List<String> getRequestedStyles(Request gsRequest, GetMapRequest getMap) {
        List<String> requestedStyles = new ArrayList<>();
        int styleIndex = 0;
        List<String> parsedStyles = parseStylesParameter(gsRequest);
        for (Object layer : parseLayersParameter(gsRequest, getMap)) {
            boolean outOfBound = styleIndex >= parsedStyles.size();
            if (layer instanceof LayerGroupInfo) {
                String styleName = outOfBound ? null : parsedStyles.get(styleIndex);
                addGroupStyles((LayerGroupInfo) layer, requestedStyles, styleName);
            } else {
                // the layer is a LayerInfo or MapLayerInfo (if it is a remote layer)
                if (outOfBound) {
                    requestedStyles.add(null);
                } else {
                    requestedStyles.add(parsedStyles.get(styleIndex));
                }
            }
            styleIndex++;
        }
        return requestedStyles;
    }

    private void addGroupStyles(
            LayerGroupInfo groupInfo, List<String> requestedStyles, String styleName) {
        List<StyleInfo> groupStyles;
        if (styleName != null && !"".equals(styleName)) groupStyles = groupInfo.styles(styleName);
        else groupStyles = groupInfo.styles();

        requestedStyles.addAll(
                groupStyles.stream()
                        .map(s -> s != null ? s.prefixedName() : null)
                        .collect(Collectors.toList()));
    }

    private List<Object> parseLayersParameter(Request gsRequest, GetMapRequest getMap) {
        String rawLayersParameter = (String) gsRequest.getRawKvp().get("LAYERS");
        if (rawLayersParameter != null) {
            List<String> layersNames = KvpUtils.readFlat(rawLayersParameter);
            return LayersParser.getInstance()
                    .parseLayers(layersNames, getMap.getRemoteOwsURL(), getMap.getRemoteOwsType());
        }
        return new ArrayList<>();
    }

    private List<String> parseStylesParameter(Request gsRequest) {
        String rawStylesParameter = (String) gsRequest.getRawKvp().get("STYLES");
        if (rawStylesParameter != null) {
            return KvpUtils.readFlat(rawStylesParameter);
        }
        return new ArrayList<>();
    }

    /** An helper that avoids duplicating the code to parse the layers parameter */
    static final class LayersParser extends GetMapKvpRequestReader {

        private static LayersParser singleton = null;

        public static LayersParser getInstance() {
            if (singleton == null) singleton = new LayersParser();
            return singleton;
        }

        private LayersParser() {
            super(WMS.get());
        }

        @Override
        public List<Object> parseLayers(
                List<String> requestedLayerNames, URL remoteOwsUrl, String remoteOwsType) {
            try {
                return super.parseLayers(requestedLayerNames, remoteOwsUrl, remoteOwsType);
            } catch (Exception exception) {
                throw new ServiceException("Error parsing requested layers.", exception);
            }
        }
    }

    @Override
    public int getPriority() {
        return ExtensionPriority.LOWEST;
    }
}
