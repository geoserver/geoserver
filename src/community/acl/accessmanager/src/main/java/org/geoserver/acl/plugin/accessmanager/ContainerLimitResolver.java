/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * Original from GeoServer 2.24-SNAPSHOT under GPL 2.0 license
 */
package org.geoserver.acl.plugin.accessmanager;

import static java.util.function.Predicate.not;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Logger;
import org.geoserver.acl.authorization.AccessInfo;
import org.geoserver.acl.authorization.AccessRequest;
import org.geoserver.acl.authorization.AuthorizationService;
import org.geoserver.acl.domain.rules.CatalogMode;
import org.geoserver.acl.domain.rules.GrantType;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.security.impl.LayerGroupContainmentCache.LayerGroupSummary;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * This class is responsible to handle the limit resolving (allowed areas and catalog mode) for a resource when it is
 * contained in one or more layer groups. The methods in this class ensure that if we restrict access when rule are
 * found for the same role, and we enlarge when rules are defined for different roles.
 */
class ContainerLimitResolver {

    private static final Logger LOGGER = Logging.getLogger(ContainerLimitResolver.class);

    private final AuthorizationService authorizationService;

    private final Authentication authentication;

    private final String workspace;

    private final String layer;

    private final List<LayerGroupInfo> groupList;

    private final Collection<LayerGroupSummary> groupSummaries;

    private enum RestrictionType {
        GROUP_INTERSECT, // after intersection found group spatial filter intersects
        GROUP_CLIP, // after intersection found group spatial filter clip
        GROUP_BOTH, // after intersection found group spatial filter both types
        LAYER_INTERSECT,
        LAYER_CLIP,
        LAYER_BOTH // no group spatial filter, might present a layer spatial filter
    }

    public static ProcessingResult resolve(
            AuthorizationService authService,
            Authentication user,
            String workspace,
            String layer,
            List<LayerGroupInfo> containers,
            Collection<LayerGroupSummary> summaries) {

        ContainerLimitResolver resolver;
        if (summaries.isEmpty()) {
            resolver = new ContainerLimitResolver(authService, user, workspace, layer, containers);
        } else {
            resolver = new ContainerLimitResolver(authService, user, workspace, layer, summaries);
        }
        return resolver.resolveResourceInGroupLimits();
    }

    /**
     * Constructor for {@link #groupList}
     *
     * @param authService the ACL access rules admin service.
     * @param user the Authentication object bound to this request.
     * @param workspace the workspace of the layer being requested.
     * @param layer the layer being requested.
     * @param groups the layer groups containing the resource in the context of a WMS request targeting a layer group.
     */
    private ContainerLimitResolver(
            AuthorizationService authService,
            Authentication user,
            String workspace,
            String layer,
            List<LayerGroupInfo> groups) {
        this.authorizationService = authService;
        this.authentication = user;
        this.layer = layer;
        this.workspace = workspace;
        this.groupList = groups;
        this.groupSummaries = null;
    }

    /**
     * Constructor for {@link #groupSummaries}
     *
     * @param authService the ACL access rules admin service.
     * @param user the Authentication object bound to this request.
     * @param workspace the workspace of the layer being requested.
     * @param layer the layer being requested.
     * @param groups the layer groups containing the resource in the context of a WMS request directly targeting a layer
     *     contained in one or more layer groups.
     */
    private ContainerLimitResolver(
            AuthorizationService authService,
            Authentication user,
            String workspace,
            String layer,
            Collection<LayerGroupSummary> groups) {
        this.authorizationService = authService;
        this.authentication = user;
        this.layer = layer;
        this.workspace = workspace;
        this.groupList = null;
        this.groupSummaries = groups;
    }

    /**
     * Resolve the resource limits taking in consideration the limits of a layer group.
     *
     * @return the result of the resolving containing the catalog mode, the allowed area and the clip area.
     */
    ProcessingResult resolveResourceInGroupLimits() {
        Map<String, AccessInfo> publishedAccessByRole = new HashMap<>();
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            Optional<AccessRequest> request = requestForRole(authority, workspace, layer);
            request.map(authorizationService::getAccessInfo)
                    .filter(not(this::isDeny))
                    .ifPresent(accessInfo -> publishedAccessByRole.put(authority.getAuthority(), accessInfo));
        }
        // retrieve the AccessInfo grouped by role
        ListMultimap<String, AccessInfo> groupsByRoleAccess = collectContainersAccessInfoByRole();
        // first we restrict the access
        ListMultimap<RestrictionType, ProcessingResult> restrictionResults =
                intersectAccesses(publishedAccessByRole, groupsByRoleAccess);
        // enlarge and return the result
        return unionAccesses(restrictionResults);
    }

    private ProcessingResult unionAccesses(ListMultimap<RestrictionType, ProcessingResult> restrictionResults) {

        // groups results
        List<ProcessingResult> bothGroup = restrictionResults.get(RestrictionType.GROUP_BOTH);
        List<ProcessingResult> intersectsGroup = restrictionResults.get(RestrictionType.GROUP_INTERSECT);
        List<ProcessingResult> clipGroup = restrictionResults.get(RestrictionType.GROUP_CLIP);

        // layers results
        List<ProcessingResult> bothLayer = restrictionResults.get(RestrictionType.LAYER_BOTH);
        List<ProcessingResult> intersectsLayer = restrictionResults.get(RestrictionType.LAYER_INTERSECT);
        List<ProcessingResult> clipLayer = restrictionResults.get(RestrictionType.LAYER_CLIP);

        // enlarge each result type separately
        ProcessingResult intersectProcessG = enlargeGroupProcessingResult(intersectsGroup);
        ProcessingResult clipProcessG = enlargeGroupProcessingResult(clipGroup);
        ProcessingResult bothProcessG = enlargeGroupProcessingResult(bothGroup);

        ProcessingResult intersectProcessL = enlargeGroupProcessingResult(intersectsLayer);
        ProcessingResult clipProcessL = enlargeGroupProcessingResult(clipLayer);
        ProcessingResult bothProcessL = enlargeGroupProcessingResult(bothLayer);

        // do the final merge
        Geometry intersectArea = null;
        Geometry clipArea = null;
        CatalogMode catalogModeDTO = null;
        if (intersectProcessG != null) {
            LOGGER.fine("Processing group areas with intersect type");
            intersectArea = intersectProcessG.getIntersectArea();
            clipArea = intersectProcessG.getClipArea();
            catalogModeDTO = CatalogMode.lenient(null, intersectProcessG.getCatalogModeDTO());
        }
        if (clipProcessG != null) {
            LOGGER.fine("Processing group areas with clip type");
            final boolean favourNull = false;
            clipArea = unionOrReturnIfNull(clipProcessG::getClipArea, clipArea, favourNull);
            intersectArea = unionOrReturnIfNull(clipProcessG::getIntersectArea, intersectArea, favourNull);
            catalogModeDTO = CatalogMode.lenient(catalogModeDTO, clipProcessG.getCatalogModeDTO());
        }

        if (bothProcessG != null) {
            LOGGER.fine("Processing group areas with both intersects and clip types");
            // if in context of a direct access to the layer, we apply the container limits
            // only if there is no group with null limits.
            boolean favourNull = groupSummaries != null;
            intersectArea = unionOrReturnIfNull(bothProcessG::getIntersectArea, intersectArea, favourNull);
            clipArea = unionOrReturnIfNull(bothProcessG::getClipArea, clipArea, favourNull);
            catalogModeDTO = CatalogMode.lenient(catalogModeDTO, bothProcessG.getCatalogModeDTO());
        }

        if (intersectProcessL != null) {
            LOGGER.fine("Processing layer intersect areas if present");
            intersectArea = unionOrReturnIfNull(intersectProcessL::getIntersectArea, intersectArea, false);
            catalogModeDTO = CatalogMode.lenient(catalogModeDTO, intersectProcessL.getCatalogModeDTO());
        }
        if (clipProcessL != null) {
            LOGGER.fine("Processing layer clip areas if present");
            clipArea = unionOrReturnIfNull(clipProcessL::getClipArea, clipArea, false);
            catalogModeDTO = CatalogMode.lenient(catalogModeDTO, clipProcessL.getCatalogModeDTO());
        }

        if (bothProcessL != null) {
            LOGGER.fine("Processing layer areas with both intersects and clip types");
            intersectArea = unionOrReturnIfNull(bothProcessL::getIntersectArea, intersectArea, false);
            clipArea = unionOrReturnIfNull(bothProcessL::getClipArea, clipArea, false);
            catalogModeDTO = CatalogMode.lenient(catalogModeDTO, bothProcessL.getCatalogModeDTO());
        }

        return new ProcessingResult(intersectArea, clipArea, catalogModeDTO);
    }

    private Geometry unionOrReturnIfNull(Supplier<Geometry> supplier, Geometry area, boolean favourNull) {
        if (area != null) area = GeometryUtils.reprojectAndUnion(supplier.get(), area, favourNull);
        else area = supplier.get();
        return area;
    }

    // enlarge a processing result
    private ProcessingResult enlargeGroupProcessingResult(List<ProcessingResult> processingResult) {
        if (processingResult == null || processingResult.isEmpty()) return null;
        CatalogMode catalogModeDTO = null;
        Geometry intersectArea = null;
        Geometry clipArea = null;
        boolean favourNull = groupSummaries != null;
        for (int i = 0; i < processingResult.size(); i++) {
            ProcessingResult pr = processingResult.get(i);
            catalogModeDTO = CatalogMode.lenient(catalogModeDTO, pr.getCatalogModeDTO());
            Geometry allowedArea = pr.getIntersectArea();
            Geometry allowedAreaClip = pr.getClipArea();
            if (i == 0) {
                intersectArea = allowedArea;
                clipArea = allowedAreaClip;
            } else {
                intersectArea = GeometryUtils.reprojectAndUnion(intersectArea, allowedArea, favourNull);
                clipArea = GeometryUtils.reprojectAndUnion(clipArea, allowedAreaClip, favourNull);
            }
        }

        return new ProcessingResult(intersectArea, clipArea, catalogModeDTO);
    }

    /**
     * Resolve all the AccessInfo belonging to the same role restricting the access for allowed areas (intersection) and
     * catalog mode.
     *
     * @param resourceAccessInfo the resource access infos by role
     * @param groupAccessInfoByRole groups access infos by role.
     * @return a Multimap<String,AccessInfo> gathering all the AccessInfo grouped by role.
     */
    private ListMultimap<RestrictionType, ProcessingResult> intersectAccesses(
            Map<String, AccessInfo> resourceAccessInfo, ListMultimap<String, AccessInfo> groupAccessInfoByRole) {

        ListMultimap<RestrictionType, ProcessingResult> multiMap = ArrayListMultimap.create();

        resourceAccessInfo.forEach((role, accessInfo) -> {
            List<AccessInfo> infosByRole = groupAccessInfoByRole.get(role);
            intersectAccesses(accessInfo, infosByRole, multiMap);
        });

        return multiMap;
    }

    /**
     * Restrict the access for all the access info being passed.
     *
     * @param resAccessInfo the resource accessInfo. Provide the base point from which resolve the container limits.
     * @param groupsAccessInfo the groups access info for same role. To be resolved on top of the resource access info.
     * @param multiMap to be filled with the result.
     */
    private void intersectAccesses(
            AccessInfo resAccessInfo,
            List<AccessInfo> groupsAccessInfo,
            ListMultimap<RestrictionType, ProcessingResult> multiMap) {
        Geometry resIntersectArea = GeometryUtils.toJTS(resAccessInfo.getIntersectArea());
        Geometry resClipArea = GeometryUtils.toJTS(resAccessInfo.getClipArea());
        CatalogMode catalogMode = resAccessInfo.getCatalogMode();
        boolean groupOnIntersect = false;
        boolean groupOnClip = false;
        boolean lessRestrictive = groupSummaries != null;
        Geometry groupsIntersectArea = null;
        Geometry groupClipArea = null;
        if (groupsAccessInfo != null && !groupsAccessInfo.isEmpty()) {
            for (int i = 0; i < groupsAccessInfo.size(); i++) {
                AccessInfo accessInfo = groupsAccessInfo.get(i);
                catalogMode = CatalogMode.stricter(catalogMode, accessInfo.getCatalogMode());
                org.geolatte.geom.Geometry<?> allowedArea = accessInfo.getIntersectArea();
                org.geolatte.geom.Geometry<?> clipAllowedArea = accessInfo.getClipArea();
                if (!groupOnIntersect) groupOnIntersect = allowedArea != null;
                if (!groupOnClip) groupOnClip = clipAllowedArea != null;
                Geometry area = GeometryUtils.toJTS(allowedArea);
                Geometry clipArea = GeometryUtils.toJTS(clipAllowedArea);

                if (i == 0) {
                    // be sure we have an initial value.
                    groupsIntersectArea = area;
                    groupClipArea = clipArea;
                } else {
                    groupsIntersectArea =
                            GeometryUtils.reprojectAndIntersect(groupsIntersectArea, area, lessRestrictive);
                    groupClipArea = GeometryUtils.reprojectAndIntersect(groupClipArea, clipArea, lessRestrictive);
                }
            }
        }
        resIntersectArea = GeometryUtils.reprojectAndIntersect(resIntersectArea, groupsIntersectArea, false);
        resClipArea = GeometryUtils.reprojectAndIntersect(resClipArea, groupClipArea, false);

        ProcessingResult result = new ProcessingResult(resIntersectArea, resClipArea, catalogMode);
        // dived the results according to the fact that an intersect or clip, or both or
        // none
        // were found in the layer group. This is needed to properly handle the
        // enlarging of access
        // to avoid layer group limit to be not considered.
        if (groupOnClip && groupOnIntersect) multiMap.put(RestrictionType.GROUP_BOTH, result);
        else if (groupOnClip) multiMap.put(RestrictionType.GROUP_CLIP, result);
        else if (groupOnIntersect) multiMap.put(RestrictionType.GROUP_INTERSECT, result);
        else if (resIntersectArea != null && resClipArea != null) multiMap.put(RestrictionType.LAYER_BOTH, result);
        else if (resIntersectArea != null) multiMap.put(RestrictionType.LAYER_INTERSECT, result);
        else if (resClipArea != null) multiMap.put(RestrictionType.LAYER_CLIP, result);
    }

    // collect the containers area by role.
    private ListMultimap<String, AccessInfo> collectContainersAccessInfoByRole() {
        ListMultimap<String, AccessInfo> groupAccessInfoByRole = ArrayListMultimap.create();
        if (groupSummaries == null) {
            collectGroupAccessInfoByRole(groupList, authentication, groupAccessInfoByRole);
        } else {
            // in context of a direct access to a layer contained in some tree group
            collectGroupSummaryAccessInfoByRole(groupSummaries, authentication.getAuthorities(), groupAccessInfoByRole);
        }
        return groupAccessInfoByRole;
    }

    /** Collects the AccessInfo by role for all the layer groups as summary (direct access of a contained layer). */
    private void collectGroupSummaryAccessInfoByRole(
            Collection<LayerGroupSummary> summaries,
            Collection<? extends GrantedAuthority> authorities,
            ListMultimap<String, AccessInfo> groupAccessInfoByRole) {

        for (LayerGroupSummary summary : summaries) {
            LayerGroupInfo.Mode mode = summary.getMode();
            if (LayerGroupInfo.Mode.OPAQUE_CONTAINER.equals(mode)) {
                continue;
            }
            String summaryLayer = summary.getName();
            String summaryWorkspace = summary.getWorkspace();
            // temporary map to do additional checks before adding access info to multimap.
            Map<String, AccessInfo> map = new HashMap<>(authorities.size());
            for (GrantedAuthority authority : authorities) {
                Optional<AccessRequest> request = requestForRole(authority, summaryWorkspace, summaryLayer);
                request.map(authorizationService::getAccessInfo)
                        .filter(not(this::isDeny))
                        .ifPresent(accessInfo -> map.put(authority.getAuthority(), accessInfo));
            }

            map.forEach(groupAccessInfoByRole::put);
        }
    }

    // collects group AccessInfo by Role (when layer group is requested)
    private void collectGroupAccessInfoByRole(
            List<LayerGroupInfo> groupList,
            Authentication user,
            ListMultimap<String, AccessInfo> groupAccessInfoByRole) {

        for (LayerGroupInfo group : groupList) {
            if (!isUserAllowed(group)) {
                addAccessInfoByRole(groupAccessInfoByRole, user.getAuthorities(), group);
            }
        }
    }

    // if this query result in allowing the user no need to go on with the
    // limit enlargement/restriction for this group.
    private boolean isUserAllowed(LayerGroupInfo layerGroup) {
        String workspaceName = workspaceName(layerGroup);
        String layerGroupName = layerGroup.getName();

        AccessRequest request = AuthorizationRequestBuilder.data()
                .user(authentication)
                .request(Dispatcher.REQUEST.get())
                .workspace(workspaceName)
                .layer(layerGroupName)
                .build();
        AccessInfo accessInfo = authorizationService.getAccessInfo(request);
        LOGGER.fine("User allowed for the entire layer group. No limit processing is needed.");
        return isAllow(accessInfo) && accessInfo.getIntersectArea() == null && accessInfo.getClipArea() == null;
    }

    private String workspaceName(LayerGroupInfo layerGroup) {
        WorkspaceInfo workspace = layerGroup.getWorkspace();
        String workspaceName = workspace == null ? null : workspace.getName();
        return workspaceName;
    }

    private void addAccessInfoByRole(
            ListMultimap<String, AccessInfo> multimap,
            Collection<? extends GrantedAuthority> authorities,
            LayerGroupInfo layerGroup) {

        String workspaceName = workspaceName(layerGroup);
        String layerGroupName = layerGroup.getName();
        for (GrantedAuthority authority : authorities) {
            Optional<AccessRequest> request = requestForRole(authority, workspaceName, layerGroupName);
            request.map(authorizationService::getAccessInfo)
                    .ifPresent(
                            // we have at least one allow. No limits will be taken in consideration.
                            accessInfo -> multimap.put(authority.getAuthority(), accessInfo));
        }
    }

    private boolean isAllow(AccessInfo accessInfo) {
        return accessInfo != null && accessInfo.getGrant().equals(GrantType.ALLOW);
    }

    private boolean isDeny(AccessInfo accessInfo) {
        return accessInfo != null && accessInfo.getGrant().equals(GrantType.DENY);
    }

    /** Data class meant to return a result for the whole limit resolution. */
    static class ProcessingResult {
        private Geometry intersectArea;
        private Geometry clipArea;
        private CatalogMode catalogModeDTO;

        ProcessingResult(Geometry intersectArea, Geometry clipArea, CatalogMode catalogModeDTO) {
            this.intersectArea = intersectArea;
            this.clipArea = clipArea;
            this.catalogModeDTO = catalogModeDTO;
        }

        Geometry getIntersectArea() {
            return intersectArea;
        }

        Geometry getClipArea() {
            return clipArea;
        }

        CatalogMode getCatalogModeDTO() {
            return catalogModeDTO;
        }

        void setIntersectArea(Geometry intersectArea) {
            this.intersectArea = intersectArea;
        }

        void setClipArea(Geometry clipArea) {
            this.clipArea = clipArea;
        }
    }

    private Optional<AccessRequest> requestForRole(GrantedAuthority grantedAuthority, String workspace, String layer) {
        // is valid then we set role and user name.
        Request request = Dispatcher.REQUEST.get();
        Authentication auth = this.authentication;
        return Optional.of(AuthorizationRequestBuilder.data()
                .user(auth)
                .layer(layer)
                .workspace(workspace)
                .request(request)
                .build()
                .withRoles(Set.of(grantedAuthority.getAuthority())));
    }
}
