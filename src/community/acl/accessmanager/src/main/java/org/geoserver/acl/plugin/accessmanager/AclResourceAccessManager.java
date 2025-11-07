/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * Original from GeoServer 2.24-SNAPSHOT under GPL 2.0 license
 */
package org.geoserver.acl.plugin.accessmanager;

import static java.util.logging.Level.FINE;
import static java.util.logging.Level.WARNING;
import static org.geoserver.acl.authorization.AccessInfo.ALLOW_ALL;
import static org.geoserver.acl.domain.rules.GrantType.ALLOW;
import static org.geoserver.acl.domain.rules.GrantType.DENY;
import static org.geoserver.acl.domain.rules.GrantType.LIMIT;
import static org.geoserver.acl.plugin.accessmanager.CatalogSecurityFilterBuilder.buildSecurityFilter;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.collections4.CollectionUtils;
import org.geoserver.acl.authorization.AccessInfo;
import org.geoserver.acl.authorization.AccessRequest;
import org.geoserver.acl.authorization.AccessSummary;
import org.geoserver.acl.authorization.AccessSummaryRequest;
import org.geoserver.acl.authorization.AuthorizationService;
import org.geoserver.acl.domain.rules.GrantType;
import org.geoserver.acl.domain.rules.LayerAttribute;
import org.geoserver.acl.domain.rules.LayerAttribute.AccessType;
import org.geoserver.acl.plugin.accessmanager.AclWPSHelper.WPSAccessInfo;
import org.geoserver.acl.plugin.accessmanager.ContainerLimitResolver.ProcessingResult;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerGroupInfo.Mode;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.platform.ExtensionPriority;
import org.geoserver.security.AbstractResourceAccessManager;
import org.geoserver.security.AccessLimits;
import org.geoserver.security.AdminRequest;
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
import org.geoserver.security.impl.LayerGroupContainmentCache.LayerGroupSummary;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.api.filter.spatial.Intersects;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * {@link ResourceAccessManager} to make GeoServer use the ACL {@link AuthorizationService} to assess data access rules
 *
 * @author Andrea Aime - GeoSolutions - Originally as part of GeoFence's GeoServer extension
 * @author Emanuele Tajariol- GeoSolutions - Originally as part of GeoFence's GeoServer extension
 * @author Gabriel Roldan - Camptocamp
 */
public class AclResourceAccessManager extends AbstractResourceAccessManager
        implements ResourceAccessManager, ExtensionPriority {

    private static final Logger LOGGER = Logging.getLogger(AclResourceAccessManager.class);

    private static final List<LayerGroupInfo> NO_CONTAINERS = List.of();

    private static final FilterFactory FF = CommonFactoryFinder.getFilterFactory();

    private static final CatalogMode DEFAULT_CATALOG_MODE = CatalogMode.HIDE;

    private AuthorizationService authorizationService;

    private final AuthorizationServiceConfig config;

    private LayerGroupContainmentCache groupsCache;

    private AclWPSHelper wpsHelper;

    public AclResourceAccessManager(
            AuthorizationService authorizationService,
            LayerGroupContainmentCache groupsCache,
            AuthorizationServiceConfig configuration,
            AclWPSHelper wpsHelper) {

        this.authorizationService = authorizationService;
        this.config = configuration;
        this.groupsCache = groupsCache;
        this.wpsHelper = wpsHelper;
    }

    /**
     * Whether to allow write access to resources to authenticated users, if false only admins (users with
     * {@literal ROLE_ADMINISTRATOR}) have write access.
     */
    @VisibleForTesting
    public void setGrantWriteToWorkspacesToAuthenticatedUsers(boolean grantWriteToWorkspacesToAuthenticatedUsers) {
        config.setGrantWriteToWorkspacesToAuthenticatedUsers(grantWriteToWorkspacesToAuthenticatedUsers);
    }

    @VisibleForTesting
    public void initDefaults() {
        config.initDefaults();
    }

    @Override
    public int getPriority() {
        return ExtensionPriority.HIGHEST;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns a {@link Filter} selecting only the objects authorized by the manager. May return null in which case
     * the caller is responsible for building a filter based on calls to the manager's other methods.
     *
     * @return {@link Filter#INCLUDE INCLUDE} if {@code user} is an {@link GeoServerRole#ADMIN_ROLE administrator},
     *     {@link Filter#EXCLUDE EXCLUDE} if {@code user == null}, otherwise the filter built by
     *     {@link CatalogSecurityFilterBuilder} for the user's {@link AccessSummary} and {@link CatalogInfo}
     *     {@code infoType}
     * @see AuthorizationService#getUserAccessSummary(AccessSummaryRequest)
     * @see CatalogSecurityFilterBuilder
     */
    @Override
    public Filter getSecurityFilter(Authentication user, Class<? extends CatalogInfo> infoType) {
        if (null == user) {
            return Filter.EXCLUDE;
        }
        if (isAdmin(user)) {
            return Filter.INCLUDE;
        }
        AccessSummary viewables = getAccessSummary(user);
        return buildSecurityFilter(viewables, infoType);
    }

    /** {@inheritDoc} */
    @Override
    public WorkspaceAccessLimits getAccessLimits(Authentication user, WorkspaceInfo workspace) {
        CatalogMode catalogMode = DEFAULT_CATALOG_MODE;
        boolean canRead = true;
        boolean canWrite = false;
        boolean canAdmin = false;
        if (isAdmin(user)) {
            canRead = canWrite = canAdmin = true;
        } else if (isAuthenticated(user)) {
            canRead = true;
            /*
             * TODO: This sets a global
             * flag to enable/disable write access to FeatureTypes (and Coverages?). My understanding is it should be enabled at the Rule level
             * and resolved by the authorization request/response. At the same time, it kind of collides with LayerDetails cqlWrite filters and
             * service request rules (e.g. allow/deny WFS.Transaction). All in all resolving this value would be complementary
             * because probably we can't forsee all the paths that could lead to an attempt to perform a modification on a FT/CV,
             * but I still think a configuration at the Rule level would be more natural, like in a FileSystem ACL, a rule at the
             * workspace granularity that allows write access to that workspace can be overridden for a given layer with a rule at the layer
             * granularity?
             */
            canWrite = config.isGrantWriteToWorkspacesToAuthenticatedUsers();
            canAdmin = isWorkspaceAdmin(user, workspace);
        }
        return new WorkspaceAccessLimits(catalogMode, canRead, canWrite, canAdmin);
    }

    @Override
    public StyleAccessLimits getAccessLimits(Authentication user, StyleInfo style) {
        LOGGER.fine("Not limiting styles");
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public LayerGroupAccessLimits getAccessLimits(Authentication user, LayerGroupInfo layerInfo) {
        return getAccessLimits(user, layerInfo, NO_CONTAINERS);
    }

    /** {@inheritDoc} */
    @Override
    public LayerGroupAccessLimits getAccessLimits(
            Authentication user, LayerGroupInfo layerGroup, List<LayerGroupInfo> containers) {
        WorkspaceInfo ws = layerGroup.getWorkspace();
        String workspace = ws != null ? ws.getName() : null;
        String layerName = layerGroup.getName();
        return (LayerGroupAccessLimits) getAccessLimits(user, layerGroup, workspace, layerName, containers);
    }

    /** {@inheritDoc} */
    @Override
    public DataAccessLimits getAccessLimits(Authentication user, LayerInfo layer) {
        return getAccessLimits(user, layer, NO_CONTAINERS);
    }

    /** {@inheritDoc} */
    @Override
    public DataAccessLimits getAccessLimits(Authentication user, LayerInfo layer, List<LayerGroupInfo> containers) {
        String workspace = layer.getResource().getStore().getWorkspace().getName();
        String layerName = layer.getName();
        return (DataAccessLimits) getAccessLimits(user, layer, workspace, layerName, containers);
    }

    /** {@inheritDoc} */
    @Override
    public DataAccessLimits getAccessLimits(Authentication user, ResourceInfo resource) {
        log(FINE, "Getting access limits for Resource {0}", resource.getName());
        // extract the user name
        String workspace = resource.getStore().getWorkspace().getName();
        String layer = resource.getName();
        return (DataAccessLimits) getAccessLimits(user, resource, workspace, layer, NO_CONTAINERS);
    }

    /**
     * Overrides the default {@link ResourceAccessManager#isWorkspaceAdmin} to use the more efficient
     * {@link AccessSummary#hasAdminRightsToAnyWorkspace()}. {@link AccessSummary} is obtained through
     * {@link AuthorizationService#getUserAccessSummary(AccessSummaryRequest)} and provides a quick view of adminable
     * workspaces and which layers can be seen.
     *
     * @see AuthorizationService#getUserAccessSummary(AccessSummaryRequest)
     * @see #getSecurityFilter(Authentication, Class)
     */
    @Override
    public boolean isWorkspaceAdmin(Authentication user, Catalog catalog) {
        AccessSummary accessSumary = getAccessSummary(user);
        // revisit: catalog is unsused in this implementation, but maybe verify at least
        // one of the workspaces in AccessSummary exists in catalog
        return accessSumary.hasAdminRightsToAnyWorkspace();
    }

    /** We expect the user not to be null and not to be admin */
    private boolean isWorkspaceAdmin(Authentication user, WorkspaceInfo workspace) {
        String workspaceName = workspace.getName();
        return isWorkspaceAdmin(user, workspaceName);
    }

    private boolean isWorkspaceAdmin(Authentication user, String workspaceName) {
        AccessSummary accessSummary = getAccessSummary(user);
        return accessSummary.hasAdminWriteAccess(workspaceName);
    }

    static boolean isAuthenticated(Authentication user) {
        return (user != null) && !(user instanceof AnonymousAuthenticationToken);
    }

    static boolean isAdmin(Authentication user) {
        return isAuthenticated(user) && hasAdminRole(user);
    }

    private static boolean hasAdminRole(Authentication user) {
        return roles(user).anyMatch(GeoServerRole.ADMIN_ROLE.getAuthority()::equals);
    }

    static Stream<String> roles(Authentication user) {
        return user.getAuthorities().stream().map(GrantedAuthority::getAuthority);
    }

    private boolean isAdminRequest() {
        return null != AdminRequest.get();
    }

    private AccessLimits getAccessLimits(
            final Authentication user,
            final CatalogInfo info,
            final String workspace,
            final String layer,
            final List<LayerGroupInfo> containers) {
        // shortcut, if the user is the admin, he can do everything
        if (isAdmin(user)) {
            log(FINE, "Admin level access, returning full rights for layer {0}", layer);
            return buildAdminAccessLimits(info);
        }

        AccessInfo accessInfo;
        ProcessingResult processingResult = null;

        if (isAdminRequest() && isWorkspaceAdmin(user, workspace)) {
            accessInfo = ALLOW_ALL;
        } else {
            AccessRequest accessRequest = buildAccessRequest(workspace, layer, user);
            accessInfo = getAccessInfo(accessRequest);

            final Request req = Dispatcher.REQUEST.get();
            final String service = req != null ? req.getService() : null;
            final boolean isWms = "WMS".equalsIgnoreCase(service);
            final boolean layerGroupsRequested = CollectionUtils.isNotEmpty(containers);

            if (isWms && !layerGroupsRequested) {
                // is direct access we need to retrieve eventually present groups.
                Collection<LayerGroupSummary> summaries = getGroupSummary(info);
                if (!summaries.isEmpty()) {
                    boolean allOpaque = allOpaque(summaries);
                    boolean noneSingle = noneSingle(summaries);
                    // all opaque we deny and don't perform any resolution of group limits.
                    if (allOpaque) {
                        accessInfo = accessInfo.withGrant(DENY);
                    } else if (noneSingle) {
                        // if a single group is present we don't apply any limit from containers.
                        processingResult = getContainerResolverResult(info, layer, workspace, user, null, summaries);
                    }
                }
            } else if (layerGroupsRequested) {
                // layer is requested in context of a layer group, we need to process the
                // containers limits.
                processingResult = getContainerResolverResult(info, layer, workspace, user, containers, List.of());
            }

            final boolean isWps = "WPS".equalsIgnoreCase(service);
            if (isWps) {
                Optional<WPSAccessInfo> wpsAccessInfo;
                wpsAccessInfo = wpsHelper.resolveWPSAccess(accessRequest, accessInfo, containers);
                if (wpsAccessInfo.isPresent()) {
                    WPSAccessInfo resolvedAccessInfo = wpsAccessInfo.orElseThrow();
                    accessInfo = resolvedAccessInfo.getAccessInfo();
                    processingResult = wpsProcessingResult(accessInfo, resolvedAccessInfo);
                    LOGGER.fine(() -> "Got WPS access %s for layer %s and user %s"
                            .formatted(wpsAccessInfo.orElseThrow(), layer, getUserNameFromAuth(user)));
                }
            }
        }

        AccessLimits limits = buildLayerLimits(info, accessInfo, processingResult);

        log(FINE, "Returning {0} for layer {1} and user {2}", limits, layer, getUserNameFromAuth(user));

        return limits;
    }

    private AccessLimits buildLayerLimits(CatalogInfo info, AccessInfo accessInfo, ProcessingResult processingResult) {

        if (info instanceof ResourceInfo resource) {
            return buildResourceAccessLimits(resource, accessInfo, processingResult);
        }
        if (info instanceof LayerInfo layerInfo) {
            ResourceInfo resource = layerInfo.getResource();
            return buildLayerLimits(resource, accessInfo, processingResult);
        }
        if (info instanceof LayerGroupInfo) {
            return buildLayerGroupAccessLimits(accessInfo);
        }
        throw new IllegalArgumentException("Expected LayerInfo|LayerGroupInfo|ResourceInfo, got " + info);
    }

    private ProcessingResult wpsProcessingResult(AccessInfo accessInfo, WPSAccessInfo wpsAccessInfo) {
        Geometry intersectArea = wpsAccessInfo.getArea();
        Geometry clipArea = wpsAccessInfo.getClip();
        org.geoserver.acl.domain.rules.CatalogMode catalogMode = accessInfo.getCatalogMode();
        return new ProcessingResult(intersectArea, clipArea, catalogMode);
    }

    private AccessInfo getAccessInfo(AccessRequest accessRequest) {
        final Level timeLogLevel = FINE;
        final Stopwatch sw = LOGGER.isLoggable(timeLogLevel) ? Stopwatch.createStarted() : null;
        AccessInfo accessInfo = authorizationService.getAccessInfo(accessRequest);
        if (null != sw) {
            sw.stop();
            log(timeLogLevel, "ACL auth run in {0}: {1} -> {2}", sw, accessRequest, accessInfo);
        }

        if (accessInfo == null) {
            accessInfo = AccessInfo.DENY_ALL;
            log(WARNING, "ACL returned null for {0}, defaulting to DENY_ALL", accessRequest);
        }

        return accessInfo;
    }

    private static void log(Level level, String msg, Object... params) {
        if (LOGGER.isLoggable(level)) {
            LOGGER.log(level, msg, params);
        }
    }

    private boolean allOpaque(Collection<LayerGroupSummary> summaries) {
        LayerGroupInfo.Mode opaque = LayerGroupInfo.Mode.OPAQUE_CONTAINER;
        return summaries.stream().map(LayerGroupSummary::getMode).allMatch(opaque::equals);
    }

    private boolean noneSingle(Collection<LayerGroupSummary> summaries) {
        Mode single = LayerGroupInfo.Mode.SINGLE;
        return summaries.stream().map(LayerGroupSummary::getMode).noneMatch(single::equals);
    }

    // build the accessLimits for an admin user
    private AccessLimits buildAdminAccessLimits(CatalogInfo info) {
        AccessLimits accessLimits;
        if (info instanceof LayerGroupInfo) accessLimits = buildLayerGroupAccessLimits(AccessInfo.ALLOW_ALL);
        else if (info instanceof ResourceInfo resourceInfo)
            accessLimits = buildResourceAccessLimits(resourceInfo, AccessInfo.ALLOW_ALL, null);
        else accessLimits = buildResourceAccessLimits(((LayerInfo) info).getResource(), AccessInfo.ALLOW_ALL, null);
        return accessLimits;
    }

    private String getUserNameFromAuth(Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        if (username != null && username.isEmpty()) {
            username = null;
        }
        return username;
    }

    private Collection<LayerGroupSummary> getGroupSummary(Object info) {
        Collection<LayerGroupSummary> summaries;
        if (info instanceof ResourceInfo resource) {
            summaries = groupsCache.getContainerGroupsFor(resource);
        } else if (info instanceof LayerInfo layer) {
            summaries = groupsCache.getContainerGroupsFor(layer.getResource());
        } else if (info instanceof LayerGroupInfo lg) {
            summaries = groupsCache.getContainerGroupsFor(lg);
        } else {
            throw new IllegalArgumentException("Unexpected catalog info " + info);
        }
        return summaries == null ? List.of() : summaries;
    }

    /**
     * Build the access info for a Resource, taking into account the containerRule if any exists
     *
     * @param info the ResourceInfo object for which the AccessLimits are requested
     * @param accessInfo the AccessInfo associated to the resource need to be reprojected due the possible difference
     *     between container and resource CRS
     * @return the AccessLimits of the Resource
     */
    DataAccessLimits buildResourceAccessLimits(
            ResourceInfo info, AccessInfo accessInfo, ProcessingResult resultLimits) {

        final CatalogMode catalogMode = getCatalogMode(accessInfo, resultLimits);

        if (info instanceof FeatureTypeInfo) {

            return buildVectorAccessLimits(info, accessInfo, resultLimits, catalogMode);

        } else if (info instanceof CoverageInfo) {

            return buildCoverageAccessLimits(info, accessInfo, resultLimits, catalogMode);

        } else if (info instanceof WMSLayerInfo) {

            return buildWMSAccessLimits(info, accessInfo, resultLimits, catalogMode);

        } else if (info instanceof WMTSLayerInfo) {

            return buildWMTSAccessLimits(info, accessInfo, resultLimits, catalogMode);
        }
        throw new IllegalArgumentException("Don't know how to handle resource " + info);
    }

    private WMTSAccessLimits buildWMTSAccessLimits(
            ResourceInfo info, AccessInfo accessInfo, ProcessingResult resultLimits, final CatalogMode catalogMode) {

        final Geometry intersectsArea = resolveIntersectsArea(info, accessInfo, resultLimits);
        Filter readFilter = toFilter(accessInfo.getGrant(), accessInfo.getCqlFilterRead());
        MultiPolygon multiPoly = toMultiPoly(intersectsArea);
        return new WMTSAccessLimits(catalogMode, readFilter, multiPoly);
    }

    private WMSAccessLimits buildWMSAccessLimits(
            ResourceInfo info, AccessInfo accessInfo, ProcessingResult resultLimits, final CatalogMode catalogMode) {

        final Geometry intersectsArea = resolveIntersectsArea(info, accessInfo, resultLimits);
        Filter readFilter = toFilter(accessInfo.getGrant(), accessInfo.getCqlFilterRead());
        boolean allowFeatureInfo = true;
        MultiPolygon multiPoly = toMultiPoly(intersectsArea);
        return new WMSAccessLimits(catalogMode, readFilter, multiPoly, allowFeatureInfo);
    }

    private CoverageAccessLimits buildCoverageAccessLimits(
            ResourceInfo info, AccessInfo accessInfo, ProcessingResult resultLimits, final CatalogMode catalogMode) {

        final Geometry intersectsArea = resolveIntersectsArea(info, accessInfo, resultLimits);
        final Geometry clipArea = resolveClipArea(info, accessInfo, resultLimits);
        Filter readFilter = toFilter(accessInfo.getGrant(), accessInfo.getCqlFilterRead());
        Geometry finalArea = null;
        if (clipArea != null && intersectsArea != null) {
            finalArea = clipArea.union(intersectsArea);
        } else if (intersectsArea != null) {
            finalArea = intersectsArea;
        } else if (clipArea != null) {
            finalArea = clipArea;
        }

        MultiPolygon multiPoly = toMultiPoly(finalArea);
        return new CoverageAccessLimits(catalogMode, readFilter, multiPoly, null);
    }

    private VectorAccessLimits buildVectorAccessLimits(
            ResourceInfo info, AccessInfo accessInfo, ProcessingResult resultLimits, final CatalogMode catalogMode) {

        VectorAccessLimits accessLimits =
                new VectorAccessLimits(catalogMode, null, Filter.EXCLUDE, null, Filter.EXCLUDE);

        setFilters(info, accessInfo, resultLimits, accessLimits);

        setAttributesAccessibility(accessInfo.getAttributes(), accessLimits);
        return accessLimits;
    }

    private void setFilters(
            ResourceInfo info, AccessInfo accessInfo, ProcessingResult resultLimits, VectorAccessLimits accessLimits) {
        // merge the area among the filters
        final Geometry intersectsArea = resolveIntersectsArea(info, accessInfo, resultLimits);
        final Geometry clipArea = resolveClipArea(info, accessInfo, resultLimits);
        Filter readFilter = toFilter(accessInfo.getGrant(), accessInfo.getCqlFilterRead());
        Filter writeFilter = toFilter(accessInfo.getGrant(), accessInfo.getCqlFilterWrite());
        if (intersectsArea != null) {
            Filter areaFilter = intersects(intersectsArea);
            if (clipArea != null) {
                Filter intersectClipArea = intersects(clipArea);
                areaFilter = FF.or(areaFilter, intersectClipArea);
            }
            readFilter = and(readFilter, areaFilter);
            writeFilter = and(writeFilter, areaFilter);
        }
        accessLimits.setReadFilter(readFilter);
        accessLimits.setWriteFilter(writeFilter);
        accessLimits.setClipVectorFilter(clipArea);
        accessLimits.setIntersectVectorFilter(intersectsArea);
    }

    private void setAttributesAccessibility(Set<LayerAttribute> attributesConfigs, VectorAccessLimits accessLimits) {
        List<PropertyName> readAttributes = new ArrayList<>();
        List<PropertyName> writeAttributes = new ArrayList<>();
        toPropertyNames(attributesConfigs, readAttributes, writeAttributes);
        // note ResourceAccessManagerWrapper expects null not empty lists
        if (!readAttributes.isEmpty()) {
            accessLimits.setReadAttributes(readAttributes);
        }
        if (!writeAttributes.isEmpty()) {
            accessLimits.setWriteAttributes(writeAttributes);
        }
    }

    private Intersects intersects(final Geometry intersectsArea) {
        return FF.intersects(FF.property(""), FF.literal(intersectsArea));
    }

    private Filter and(Filter filter, Filter areaFilter) {
        if (filter == null || filter == Filter.INCLUDE) {
            return areaFilter;
        }
        if (filter == Filter.EXCLUDE) {
            return filter;
        }
        return FF.and(filter, areaFilter);
    }

    private Geometry resolveIntersectsArea(ResourceInfo info, AccessInfo accessInfo, ProcessingResult resultLimits) {

        if (resultLimits == null) {
            CoordinateReferenceSystem crs = GeometryUtils.getCRSFromInfo(info);
            return adaptAndReproject(accessInfo.getIntersectArea(), crs);
        }
        return resultLimits.getIntersectArea();
    }

    private Geometry resolveClipArea(ResourceInfo info, AccessInfo accessInfo, ProcessingResult resultLimits) {

        if (resultLimits == null) {
            CoordinateReferenceSystem crs = GeometryUtils.getCRSFromInfo(info);
            return adaptAndReproject(accessInfo.getClipArea(), crs);
        }
        return resultLimits.getClipArea();
    }

    private Geometry adaptAndReproject(org.geolatte.geom.Geometry<?> area, CoordinateReferenceSystem crs) {
        Geometry jtsArea = GeometryUtils.toJTS(area);
        return GeometryUtils.reprojectGeometry(jtsArea, crs);
    }

    private Filter toFilter(GrantType actualGrant, @Nullable String cqlFilter) {
        if (cqlFilter != null) {
            try {
                return ECQL.toFilter(cqlFilter);
            } catch (CQLException e) {
                throw new IllegalArgumentException("Invalid cql filter found: " + e.getMessage(), e);
            }
        }
        boolean includeFilter = actualGrant == ALLOW || actualGrant == LIMIT;
        return includeFilter ? Filter.INCLUDE : Filter.EXCLUDE;
    }

    /**
     * @param accessInfo the AccessInfo associated to the LayerGroup
     * @return the AccessLimits of the LayerGroup
     */
    LayerGroupAccessLimits buildLayerGroupAccessLimits(AccessInfo accessInfo) {
        GrantType grant = accessInfo.getGrant();
        // the SecureCatalog will grant access to the layerGroup
        // if AccessLimits are null
        if (grant.equals(ALLOW) || grant.equals(LIMIT)) {
            return null; // null == no-limits
        }
        CatalogMode catalogMode = convert(accessInfo.getCatalogMode());
        return new LayerGroupAccessLimits(catalogMode);
    }

    private ProcessingResult getContainerResolverResult(
            CatalogInfo resourceInfo,
            String layer,
            String workspace,
            Authentication user,
            List<LayerGroupInfo> containers,
            Collection<LayerGroupSummary> summaries) {

        ProcessingResult result =
                ContainerLimitResolver.resolve(authorizationService, user, workspace, layer, containers, summaries);
        Geometry intersect = result.getIntersectArea();
        Geometry clip = result.getClipArea();
        // areas might be in a srid different from the one of the resource being requested.
        if (intersect != null || clip != null) {
            CoordinateReferenceSystem crs = GeometryUtils.getCRSFromInfo(resourceInfo);
            intersect = GeometryUtils.reprojectGeometry(intersect, crs);
            result.setIntersectArea(intersect);

            clip = GeometryUtils.reprojectGeometry(clip, crs);
            result.setClipArea(clip);
        }

        return result;
    }

    // get the catalogMode for the resource prioritizing the container one if passed
    private CatalogMode getCatalogMode(AccessInfo accessInfo, ProcessingResult resultLimits) {
        return switch (accessInfo.getGrant()) {
            case DENY -> DEFAULT_CATALOG_MODE;
            default -> convert(resultLimits == null ? accessInfo.getCatalogMode() : resultLimits.getCatalogModeDTO());
        };
    }

    private CatalogMode convert(org.geoserver.acl.domain.rules.CatalogMode ruleCatalogMode) {
        if (ruleCatalogMode == null) {
            return DEFAULT_CATALOG_MODE;
        }
        return switch (ruleCatalogMode) {
            case CHALLENGE -> CatalogMode.CHALLENGE;
            case HIDE -> CatalogMode.HIDE;
            case MIXED -> CatalogMode.MIXED;
            default -> throw new IllegalArgumentException("Unknown cactalog mode " + ruleCatalogMode);
        };
    }

    // Builds a rule filter to retrieve the AccessInfo for the resource
    private AccessRequest buildAccessRequest(String workspace, String layer, Authentication user) {

        return AuthorizationRequestBuilder.data()
                .request(Dispatcher.REQUEST.get())
                .workspace(workspace)
                .layer(layer)
                .user(user)
                .build();
    }

    private MultiPolygon toMultiPoly(Geometry reprojArea) {
        MultiPolygon rasterFilter = null;
        if (reprojArea != null) {
            rasterFilter = Converters.convert(reprojArea, MultiPolygon.class);
            if (rasterFilter == null) {
                throw new IllegalArgumentException(
                        "Error applying security rules, cannot convert the ACL area restriction to a multi-polygon: "
                                + reprojArea.toText());
            }
        }

        return rasterFilter;
    }

    private void toPropertyNames(
            Set<LayerAttribute> attributes, List<PropertyName> readAttributes, List<PropertyName> writeAttributes) {
        if (attributes == null) {
            return;
        }
        for (LayerAttribute attribute : attributes) {
            AccessType access = attribute.getAccess();
            if (access == null /* shouldn't happen */ || access == AccessType.NONE) {
                continue;
            }
            PropertyName property = FF.property(attribute.getName());
            if (access == AccessType.READWRITE) {
                writeAttributes.add(property);
            } else {
                readAttributes.add(property);
            }
        }
    }

    private AccessSummary getAccessSummary(Authentication user) {
        AccessSummaryRequest request = buildAccessSummaryRequest(user);

        return authorizationService.getUserAccessSummary(request);
    }

    private AccessSummaryRequest buildAccessSummaryRequest(Authentication user) {
        String username = user.getName();
        Set<String> roles = roles(user).collect(Collectors.toSet());
        return AccessSummaryRequest.builder().user(username).roles(roles).build();
    }
}
