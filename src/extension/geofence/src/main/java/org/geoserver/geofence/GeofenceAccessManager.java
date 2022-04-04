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
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
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
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.styling.Style;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.PropertyName;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
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

    RuleReaderService rules;

    Catalog catalog;

    private final GeoFenceConfigurationManager configurationManager;

    private LayerGroupContainmentCache groupsCache;

    // list of accepted roles, for the useRolesToFilter option
    // List<String> roles = new ArrayList<String>();

    public GeofenceAccessManager(
            RuleReaderService rules,
            Catalog catalog,
            GeoFenceConfigurationManager configurationManager) {

        this.rules = rules;
        this.catalog = new LocalWorkspaceCatalog(catalog);
        this.configurationManager = configurationManager;
        this.groupsCache = new LayerGroupContainmentCache(catalog);
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
                        "Admin level access, returning " + "full rights for workspace {0}",
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

        // further logic disabled because of https://github.com/geosolutions-it/geofence/issues/6
        return new WorkspaceAccessLimits(DEFAULT_CATALOG_MODE, true, false);
    }

    /** We expect the user not to be null and not to be admin */
    private boolean isWorkspaceAdmin(Authentication user, String workspaceName) {
        LOGGER.log(Level.FINE, "Getting admin auth for Workspace {0}", workspaceName);

        // get the request infos
        RuleFilter ruleFilter = new RuleFilter(RuleFilter.SpecialFilterType.ANY);

        ruleFilter.setInstance(configurationManager.getConfiguration().getInstanceName());
        ruleFilter.setWorkspace(workspaceName);
        String username = user.getName();
        if (username == null || username.isEmpty()) {
            ruleFilter.setUser(RuleFilter.SpecialFilterType.DEFAULT);
        }

        String sourceAddress = retrieveCallerIpAddress();
        if (sourceAddress != null) {
            ruleFilter.setSourceAddress(sourceAddress);
        } else {
            LOGGER.log(Level.WARNING, "No source IP address found");
            ruleFilter.setSourceAddress(RuleFilter.SpecialFilterType.DEFAULT);
        }

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "AdminAuth filter: {0}", ruleFilter);
        }

        AccessInfo auth = rules.getAdminAuthorization(ruleFilter);

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

        RuleFilter ruleFilter = buildRuleFilter(workspace, layer, user);
        AccessInfo rule = rules.getAccessInfo(ruleFilter);

        boolean directAccess = containers == null || containers.isEmpty();
        GeoserverAccessInfo containerRule =
                getAllowedAreaAndAccessInfoFromContainers(info, containers, user, directAccess);

        if (rule == null) rule = AccessInfo.DENY_ALL;

        AccessLimits limits;
        if (info instanceof LayerGroupInfo) {
            limits = buildLayerGroupAccessLimits(rule, containerRule);
        } else if (info instanceof ResourceInfo) {
            limits =
                    buildResourceAccessLimits(
                            (ResourceInfo) info, rule, containerRule, directAccess);
        } else {
            limits =
                    buildResourceAccessLimits(
                            ((LayerInfo) info).getResource(), rule, containerRule, directAccess);
        }

        LOGGER.log(
                Level.FINE,
                "Returning {0} for layer {1} and user {2}",
                new Object[] {limits, layer, getUserNameFromAuth(user)});

        return limits;
    }

    // build the accessLimits for an admin user
    private AccessLimits buildAdminAccessLimits(CatalogInfo info) {
        AccessLimits accessLimits;
        if (info instanceof LayerGroupInfo)
            accessLimits = buildLayerGroupAccessLimits(AccessInfo.ALLOW_ALL, null);
        else if (info instanceof ResourceInfo)
            accessLimits =
                    buildResourceAccessLimits(
                            (ResourceInfo) info, AccessInfo.ALLOW_ALL, null, false);
        else
            accessLimits =
                    buildResourceAccessLimits(
                            ((LayerInfo) info).getResource(), AccessInfo.ALLOW_ALL, null, false);
        return accessLimits;
    }

    private GeoserverAccessInfo getAllowedAreaAndAccessInfoFromContainers(
            CatalogInfo resource,
            List<LayerGroupInfo> containers,
            Authentication user,
            boolean directAccess) {
        Request req = Dispatcher.REQUEST.get();
        String service = req != null ? req.getService() : null;
        boolean isWms = service != null && service.equalsIgnoreCase("WMS");
        GeoserverAccessInfo containerRule = null;
        if (directAccess && isWms) {
            containerRule = getContainerRuleForLayerDirectAccess(resource, user);
        } else {
            containerRule = handleContainers(containers, user);
        }
        return containerRule;
    }

    private String getUserNameFromAuth(Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        if (username != null && username.isEmpty()) {
            username = null;
        }
        return username;
    }

    private GeoserverAccessInfo handleContainers(List<LayerGroupInfo> groups, Authentication user) {
        Geometry allowedArea = null;
        Geometry clipArea = null;
        GeoserverAccessInfo lessRestrictiveRule = null;
        for (LayerGroupInfo lgi : groups) {
            WorkspaceInfo ws = lgi.getWorkspace();
            String workspace = ws != null ? ws.getName() : null;
            String layer = lgi.getName();
            RuleFilter filter = buildRuleFilter(workspace, layer, user);
            AccessInfo accessInfo = rules.getAccessInfo(filter);
            CoordinateReferenceSystem crs = lgi.getBounds().getCoordinateReferenceSystem();
            allowedArea = getAllowedAreaAsGeom(() -> accessInfo.getAreaWkt(), crs);
            clipArea = getAllowedAreaAsGeom(() -> accessInfo.getClipAreaWkt(), crs);
            GeoserverAccessInfo newRule = new GeoserverAccessInfo(accessInfo);
            newRule.setIntersectArea(allowedArea);
            newRule.setClipArea(clipArea);
            lessRestrictiveRule = getLessRestrictiveRule(lessRestrictiveRule, newRule);
        }
        return lessRestrictiveRule;
    }

    // get the AccessInfo from the layer's containers returning
    // the Geometry of the allowed area if found and the less restrictive accessInfo
    // among the ones of associated layerGroups
    private GeoserverAccessInfo getContainerRuleForLayerDirectAccess(
            Object resource, Authentication user) {
        GeoserverAccessInfo result = null;
        Collection<LayerGroupContainmentCache.LayerGroupSummary> summaries;
        if (resource instanceof ResourceInfo)
            summaries = groupsCache.getContainerGroupsFor((ResourceInfo) resource);
        else if (resource instanceof LayerInfo)
            summaries = groupsCache.getContainerGroupsFor(((LayerInfo) resource).getResource());
        else summaries = groupsCache.getContainerGroupsFor((LayerGroupInfo) resource);

        for (LayerGroupContainmentCache.LayerGroupSummary gs : summaries) {
            LayerGroupInfo.Mode mode = gs.getMode();
            if (mode.equals(LayerGroupInfo.Mode.OPAQUE_CONTAINER)) {
                // opaque mode deny access for the layer
                AccessInfo newInfo = AccessInfo.DENY_ALL;
                result = getLessRestrictiveRule(result, new GeoserverAccessInfo(newInfo));
            } else if (!mode.equals(LayerGroupInfo.Mode.SINGLE)) {
                // not opaque and not single mode, the container rule
                // should override the layer rule
                String workspace = gs.getWorkspace();
                String layer = gs.getName();
                RuleFilter filter = buildRuleFilter(workspace, layer, user);
                AccessInfo newAccess = rules.getAccessInfo(filter);
                Geometry intersectArea = getAllowedAreaAsGeomLayerGroup(newAccess.getAreaWkt(), gs);
                Geometry clipArea = getAllowedAreaAsGeomLayerGroup(newAccess.getClipAreaWkt(), gs);
                GeoserverAccessInfo newInfo = new GeoserverAccessInfo(newAccess);
                newInfo.setClipArea(clipArea);
                newInfo.setIntersectArea(intersectArea);
                result = getLessRestrictiveRule(result, newInfo);
            }
        }
        return result;
    }

    private Geometry getAllowedAreaAsGeomLayerGroup(
            String allowedAreaWKT, LayerGroupContainmentCache.LayerGroupSummary gs) {
        if (allowedAreaWKT != null) {
            LayerGroupInfo gi = catalog.getLayerGroupByName(gs.getWorkspace(), gs.getName());
            CoordinateReferenceSystem crs = gi.getBounds().getCoordinateReferenceSystem();
            return getAllowedAreaAsGeom(() -> allowedAreaWKT, crs);
        }

        return null;
    }

    // compares two access info and return the less restrictive one
    private GeoserverAccessInfo getLessRestrictiveRule(
            GeoserverAccessInfo current, GeoserverAccessInfo newInfo) {
        AccessInfo currentAccess = current != null ? current.getAccessInfo() : null;
        AccessInfo newAccess = newInfo != null ? newInfo.getAccessInfo() : null;
        GeoserverAccessInfo result = current;
        if (currentAccess == null) {
            result = newInfo;

        } else if (newAccess != null) {

            if (currentAccess.getGrant().equals(GrantType.DENY)) {
                result = newInfo;

            } else if (currentAccess.getGrant().equals(GrantType.LIMIT)
                    && newAccess.getGrant().equals(GrantType.ALLOW)) {
                result = newInfo;

            } else if (currentAccess.getGrant().equals(newAccess.getGrant())) {
                result = getLessRestrictiveAllowedArea(current, newInfo);
            }
        }
        CatalogModeDTO cm = getLessRestrictiveCatalogMode(currentAccess, newAccess);
        result.getAccessInfo().setCatalogMode(cm);
        return result;
    }

    private CatalogModeDTO getLessRestrictiveCatalogMode(AccessInfo current, AccessInfo newAccess) {
        CatalogModeDTO result;
        CatalogModeDTO currentMode = current != null ? current.getCatalogMode() : null;
        CatalogModeDTO newMode = newAccess != null ? newAccess.getCatalogMode() : null;
        if (currentMode == null) result = newMode;
        else if (newMode == null) result = currentMode;
        else if (currentMode.equals(CatalogModeDTO.HIDE)) result = newMode;
        else if (currentMode.equals(CatalogModeDTO.MIXED)
                && newMode.equals(CatalogModeDTO.CHALLENGE)) result = newMode;
        else result = currentMode;

        if (result == null) result = CatalogModeDTO.HIDE;

        return result;
    }

    // compares two accessRules applied to a LayerGroup and will return the less restrictive.
    // if one of them has null clip or intersect null area, it will be set to null.
    // otherwise they will be united.
    private GeoserverAccessInfo getLessRestrictiveAllowedArea(
            GeoserverAccessInfo current, GeoserverAccessInfo newAccess) {
        Geometry currentIntersectsArea = current.getIntersectArea();
        Geometry newIntersectsArea = newAccess.getIntersectArea();
        Geometry currentClipArea = current.getClipArea();
        Geometry newClipArea = newAccess.getClipArea();

        Geometry unionIntersect =
                reprojectAndUnionLessRestrictive(currentIntersectsArea, newIntersectsArea);
        Geometry unionClip = reprojectAndUnionLessRestrictive(currentClipArea, newClipArea);
        GeoserverAccessInfo result = current;
        result.setIntersectArea(unionIntersect);
        result.setClipArea(unionClip);
        return result;
    }

    // check if newArea geometry needs to be reprojected
    // then perform the union
    private Geometry reprojectAndUnionLessRestrictive(Geometry current, Geometry newArea) {
        if (current == null || newArea == null) return null;

        if (current.getSRID() != newArea.getSRID()) {
            try {
                CoordinateReferenceSystem target = CRS.decode("EPSG:" + current.getSRID());
                CoordinateReferenceSystem source = CRS.decode("EPSG:" + newArea.getSRID());
                MathTransform transformation = CRS.findMathTransform(source, target);
                newArea = JTS.transform(newArea, transformation);
                newArea.setSRID(current.getSRID());
            } catch (FactoryException e) {
                throw new RuntimeException(
                        "Unable to merge allowed areas: can't reproject from "
                                + newArea.getSRID()
                                + " to "
                                + current.getSRID());
            } catch (TransformException e) {
                throw new RuntimeException(
                        "Unable to merge allowed areas: error during transformation from "
                                + newArea.getSRID()
                                + " to "
                                + current.getSRID());
            }
        }
        Geometry result = current.union(newArea);
        result.setSRID(current.getSRID());
        return result;
    }

    private void setRuleFilterUserOrRole(Authentication user, RuleFilter ruleFilter) {
        if (user != null) {
            GeoFenceConfiguration config = configurationManager.getConfiguration();
            if (config.isUseRolesToFilter() && config.getRoles().size() > 0) {

                String role = "UNKNOWN";
                for (GrantedAuthority authority : user.getAuthorities()) {
                    if (config.getRoles().contains(authority.getAuthority())
                            || config.getRoles().contains("*")) {
                        role = authority.getAuthority();
                        break;
                    }
                }
                LOGGER.log(Level.FINE, "Setting role for filter: {0}", new Object[] {role});
                ruleFilter.setRole(role);
                ruleFilter.setUser(RuleFilter.SpecialFilterType.DEFAULT);
            } else {
                String username = user.getName();
                if (username == null || username.isEmpty()) {
                    ruleFilter.setUser(RuleFilter.SpecialFilterType.DEFAULT);
                } else {
                    LOGGER.log(Level.FINE, "Setting user for filter: {0}", new Object[] {username});
                    ruleFilter.setUser(username);
                }
            }
        } else {
            ruleFilter.setUser(RuleFilter.SpecialFilterType.DEFAULT);
        }
    }

    /**
     * Build the access info for a Resource, taking into account the containerRule if any exists
     *
     * @param info the ResourceInfo object for which the AccessLimits are requested
     * @param rule the AccessInfo associated to the resource
     * @param containerRule a tuple with the container's allowedArea Geometry and AccessInfo
     * @param reprojectForDirectAccess a boolean value to determine whether the allowed area might
     *     need to be reprojected due the possible difference between container and resource CRS
     * @return the AccessLimits of the Resource
     */
    AccessLimits buildResourceAccessLimits(
            ResourceInfo info,
            AccessInfo rule,
            GeoserverAccessInfo containerRule,
            boolean reprojectForDirectAccess) {

        GrantType actualGrant = getGrant(rule, containerRule);
        boolean includeFilter = actualGrant == GrantType.ALLOW || actualGrant == GrantType.LIMIT;
        Filter readFilter = includeFilter ? Filter.INCLUDE : Filter.EXCLUDE;
        Filter writeFilter = includeFilter ? Filter.INCLUDE : Filter.EXCLUDE;
        try {
            if (rule.getCqlFilterRead() != null) {
                readFilter = ECQL.toFilter(rule.getCqlFilterRead());
            }
            if (rule.getCqlFilterWrite() != null) {
                writeFilter = ECQL.toFilter(rule.getCqlFilterWrite());
            }
        } catch (CQLException e) {
            throw new IllegalArgumentException("Invalid cql filter found: " + e.getMessage(), e);
        }

        // get the attributes
        List<PropertyName> readAttributes =
                toPropertyNames(rule.getAttributes(), PropertyAccessMode.READ);
        List<PropertyName> writeAttributes =
                toPropertyNames(rule.getAttributes(), PropertyAccessMode.WRITE);

        // reproject the area if necessary
        CoordinateReferenceSystem crs = getCrsFromInfo(info);
        Geometry containersIntersectArea =
                containerRule != null ? containerRule.getIntersectArea() : null;

        Geometry containersClipArea = containerRule != null ? containerRule.getClipArea() : null;

        Geometry intersectsArea =
                getAreaFromGroupOrLayer(
                        containersIntersectArea,
                        () -> rule.getAreaWkt(),
                        reprojectForDirectAccess,
                        crs);

        Geometry clipArea =
                getAreaFromGroupOrLayer(
                        containersClipArea,
                        () -> rule.getClipAreaWkt(),
                        reprojectForDirectAccess,
                        crs);

        CatalogMode catalogMode = getCatalogMode(rule, containerRule);
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
     * @param rule the AccessInfo associated to the LayerGroup
     * @param containerRule a tuple with the container's allowedArea Geometry and AccessInfo
     * @return the AccessLimits of the LayerGroup
     */
    AccessLimits buildLayerGroupAccessLimits(AccessInfo rule, GeoserverAccessInfo containerRule) {
        GrantType grant = getGrant(rule, containerRule);
        // the SecureCatalog will grant access  to the layerGroup
        // if AccessLimits are null
        if (grant.equals(GrantType.ALLOW) || grant.equals(GrantType.LIMIT)) return null;
        else return new LayerGroupAccessLimits(getCatalogMode(rule, containerRule));
    }

    private Geometry getAreaFromGroupOrLayer(
            Geometry containersArea,
            Supplier<String> areaSupplier,
            boolean reprojectForDirectAccess,
            CoordinateReferenceSystem crs) {
        Geometry retArea = null;
        if (containersArea != null && reprojectForDirectAccess) {
            try {
                // direct access, allowed Area might be in container CRS
                // that can be different from the resource one
                retArea = reprojectGeometry(containersArea, crs);
            } catch (Exception e) {
                throw new RuntimeException(
                        "Failed to reproject area from container CRS to resource CRS");
            }
        } else if (containersArea != null && !reprojectForDirectAccess) {
            retArea = containersArea;
        } else {
            retArea = getAllowedAreaAsGeom(areaSupplier, crs);
        }
        return retArea;
    }

    // return the container grant if present otherwise return the resource grant
    private GrantType getGrant(AccessInfo rule, GeoserverAccessInfo containerRule) {
        AccessInfo accessInfoContainer =
                containerRule != null ? containerRule.getAccessInfo() : null;
        GrantType containerGrant =
                accessInfoContainer != null ? accessInfoContainer.getGrant() : null;
        GrantType actualGrant;
        if (containerGrant != null) actualGrant = containerGrant;
        else actualGrant = rule.getGrant();

        return actualGrant;
    }

    // get the catalogMode for the resource privileging the container one if passed
    private CatalogMode getCatalogMode(AccessInfo rule, GeoserverAccessInfo containerRule) {
        AccessInfo accessInfoContainer =
                containerRule != null ? containerRule.getAccessInfo() : null;
        CatalogModeDTO ruleCatalogMode;
        if (accessInfoContainer != null) ruleCatalogMode = accessInfoContainer.getCatalogMode();
        else ruleCatalogMode = rule.getCatalogMode();
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

    private CoordinateReferenceSystem getCrsFromInfo(CatalogInfo info) {
        CoordinateReferenceSystem crs = null;
        if (info instanceof LayerInfo) {
            crs = ((LayerInfo) info).getResource().getCRS();
        } else if (info instanceof ResourceInfo) {
            crs = ((ResourceInfo) info).getCRS();
        } else if (info instanceof LayerGroupInfo) {
            crs = ((LayerGroupInfo) info).getBounds().getCoordinateReferenceSystem();
        } else {
            throw new RuntimeException(
                    "Cannot retrieve CRS from info " + info.getClass().getSimpleName());
        }
        return crs;
    }

    // Builds a rule filter to retrieve the AccessInfo for the resource
    private RuleFilter buildRuleFilter(String workspace, String layer, Authentication user) {
        // get the request infos
        RuleFilter ruleFilter = new RuleFilter(RuleFilter.SpecialFilterType.ANY);
        setRuleFilterUserOrRole(user, ruleFilter);
        ruleFilter.setInstance(configurationManager.getConfiguration().getInstanceName());
        // get info from the current request
        String service = null;
        String request = null;
        Request owsRequest = Dispatcher.REQUEST.get();
        if (owsRequest != null) {
            service = owsRequest.getService();
            request = owsRequest.getRequest();
        }
        if (service != null) {
            if ("*".equals(service)) {
                ruleFilter.setService(RuleFilter.SpecialFilterType.ANY);
            } else {
                ruleFilter.setService(service);
            }
        } else {
            ruleFilter.setService(RuleFilter.SpecialFilterType.DEFAULT);
        }

        if (request != null) {
            if ("*".equals(request)) {
                ruleFilter.setRequest(RuleFilter.SpecialFilterType.ANY);
            } else {
                ruleFilter.setRequest(request);
            }
        } else {
            ruleFilter.setRequest(RuleFilter.SpecialFilterType.DEFAULT);
        }
        ruleFilter.setWorkspace(workspace);
        ruleFilter.setLayer(layer);
        String sourceAddress = retrieveCallerIpAddress();
        if (sourceAddress != null) {
            ruleFilter.setSourceAddress(sourceAddress);
        } else {
            LOGGER.log(Level.WARNING, "No source IP address found");
            ruleFilter.setSourceAddress(RuleFilter.SpecialFilterType.DEFAULT);
        }

        LOGGER.log(Level.FINE, "ResourceInfo filter: {0}", ruleFilter);

        return ruleFilter;
    }

    private Geometry getAllowedAreaAsGeom(
            Supplier<String> areaSupplier, CoordinateReferenceSystem resourceCrs) {
        // reproject the area if necessary
        Geometry reprojArea = null;
        String areaWkt = areaSupplier != null ? areaSupplier.get() : null;
        if (areaWkt != null) {
            try {

                // Geometry area = rule.getArea();
                reprojArea = parseAllowedArea(areaWkt);

                if (reprojArea != null) {
                    reprojArea = reprojectGeometry(reprojArea, resourceCrs);
                }
            } catch (Exception e) {
                throw new RuntimeException(
                        "Failed to reproject the restricted area to the layer's native SRS", e);
            }
        }
        return reprojArea;
    }

    private Geometry reprojectGeometry(Geometry geometry, CoordinateReferenceSystem targetCRS)
            throws FactoryException, TransformException {
        CoordinateReferenceSystem geomCrs = CRS.decode("EPSG:" + geometry.getSRID());
        if ((targetCRS != null) && !CRS.equalsIgnoreMetadata(geomCrs, targetCRS)) {
            MathTransform mt = CRS.findMathTransform(geomCrs, targetCRS, true);
            geometry = JTS.transform(geometry, mt);
            Integer srid = CRS.lookupEpsgCode(targetCRS, false);
            geometry.setSRID(srid);
        }
        return geometry;
    }

    private Geometry parseAllowedArea(String allowedArea) {
        Geometry result = null;
        WKTReader wktReader = new WKTReader();
        try {
            if (allowedArea.indexOf("SRID") != -1) {
                String[] allowedAreaParts = allowedArea.split(";");
                result = wktReader.read(allowedAreaParts[1]);
                int srid = Integer.valueOf(allowedAreaParts[0].split("=")[1]);
                result.setSRID(srid);
            } else {
                result = wktReader.read(allowedArea);
                result.setSRID(4326);
            }
        } catch (ParseException e) {
            throw new RuntimeException("Failed to unmarshal the restricted area wkt", e);
        }
        return result;
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
            RuleFilter ruleFilter = new RuleFilter(RuleFilter.SpecialFilterType.ANY);
            setRuleFilterUserOrRole(user, ruleFilter);
            ruleFilter.setInstance(configurationManager.getConfiguration().getInstanceName());
            ruleFilter.setService(service);
            ruleFilter.setRequest(request);
            ruleFilter.setWorkspace(resource.getStore().getWorkspace().getName());
            ruleFilter.setLayer(resource.getName());

            LOGGER.log(Level.FINE, "Getting access limits for getLegendGraphic", ruleFilter);

            AccessInfo rule = rules.getAccessInfo(ruleFilter);

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
            RuleFilter ruleFilter = new RuleFilter(RuleFilter.SpecialFilterType.ANY);

            setRuleFilterUserOrRole(user, ruleFilter);
            ruleFilter.setInstance(configurationManager.getConfiguration().getInstanceName());
            ruleFilter.setService(service);
            ruleFilter.setRequest(request);
            if (info != null) {
                ruleFilter.setWorkspace(info.getStore().getWorkspace().getName());
                ruleFilter.setLayer(info.getName());

            } else {
                ruleFilter.setWorkspace(RuleFilter.SpecialFilterType.ANY);
                ruleFilter.setLayer(RuleFilter.SpecialFilterType.ANY);
            }

            LOGGER.log(Level.FINE, "Getting access limits for getMap", ruleFilter);

            AccessInfo rule = rules.getAccessInfo(ruleFilter);

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

    private void checkStyleAllowed(AccessInfo rule, String styleName) {
        // otherwise check if the requested style is allowed
        Set<String> allowedStyles = new HashSet<>();
        if (rule.getDefaultStyle() != null) {
            allowedStyles.add(rule.getDefaultStyle());
        }
        if (rule.getAllowedStyles() != null) {
            allowedStyles.addAll(rule.getAllowedStyles());
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

    // a container for accessInfo that can hold spatial filters as Geometry type.
    class GeoserverAccessInfo {

        private AccessInfo accessInfo;

        private Geometry intersectArea;

        private Geometry clipArea;

        GeoserverAccessInfo(AccessInfo accessInfo) {
            this.accessInfo = accessInfo;
        }

        AccessInfo getAccessInfo() {
            return accessInfo;
        }

        Geometry getIntersectArea() {
            return intersectArea;
        }

        void setIntersectArea(Geometry intersectArea) {
            this.intersectArea = intersectArea;
        }

        Geometry getClipArea() {
            return clipArea;
        }

        void setClipArea(Geometry clipArea) {
            this.clipArea = clipArea;
        }
    }
}
