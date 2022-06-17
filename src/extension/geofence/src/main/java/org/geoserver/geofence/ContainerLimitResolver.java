/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.geofence.services.RuleReaderService;
import org.geoserver.geofence.services.dto.AccessInfo;
import org.geoserver.geofence.services.dto.CatalogModeDTO;
import org.geoserver.geofence.services.dto.RuleFilter;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.security.impl.LayerGroupContainmentCache;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * This class is responsible to handle the limit resolving (allowed areas and catalog mode) for a
 * resource when it is contained in one or more layer groups. The methods in this class ensure that
 * if we restrict access when rule are found for the same role, and we enlarge when rules are
 * defined for different roles.
 */
class ContainerLimitResolver {

    private RuleReaderService ruleService;

    private List<LayerGroupInfo> groupList;

    private Collection<LayerGroupContainmentCache.LayerGroupSummary> groupSummaries;

    private Authentication authentication;

    private String layer;

    private String workspace;

    private String callerIp;

    private String instanceName;

    private GeoFenceAreaHelper areasHelper;

    private static final Logger LOGGER = Logging.getLogger(ContainerLimitResolver.class);

    /**
     * @param groups the layer groups containing the resource in the context of a WMS request
     *     targeting a layer group.
     * @param ruleService the GeoFence rule service.
     * @param authentication the Authentication object bounded to this request.
     * @param layer the layer being requested.
     * @param workspace the workspace of the layer being requested.
     * @param callerIp the ip of the user.
     * @param instanceName the instance name.
     */
    ContainerLimitResolver(
            List<LayerGroupInfo> groups,
            RuleReaderService ruleService,
            Authentication authentication,
            String layer,
            String workspace,
            String callerIp,
            String instanceName) {
        this(ruleService, authentication, layer, workspace, callerIp, instanceName);
        this.groupList = groups;
    }

    /**
     * @param groups the layer groups containing the resource in the context of a WMS request
     *     directly targeting a layer contained in one or more layer groups.
     * @param ruleService the GeoFence rule service.
     * @param authentication the Authentication object bounded to this request.
     * @param layer the layer being requested.
     * @param workspace the workspace of the layer being requested.
     * @param callerIp the ip of the user.
     * @param instanceName the instance name.
     */
    ContainerLimitResolver(
            Collection<LayerGroupContainmentCache.LayerGroupSummary> groups,
            RuleReaderService ruleService,
            Authentication authentication,
            String layer,
            String workspace,
            String callerIp,
            String instanceName) {
        this(ruleService, authentication, layer, workspace, callerIp, instanceName);
        this.groupSummaries = groups;
    }

    private ContainerLimitResolver(
            RuleReaderService ruleService,
            Authentication authentication,
            String layer,
            String workspace,
            String callerIp,
            String instanceName) {
        this.areasHelper = new GeoFenceAreaHelper();
        this.ruleService = ruleService;
        this.authentication = authentication;
        this.layer = layer;
        this.workspace = workspace;
        this.instanceName = instanceName;
        this.callerIp = callerIp;
    }

    /**
     * Resolve the resource limits taking in consideration the limits of a layer group.
     *
     * @return the result of the resolving containing the catalog mode, the allowed area and the
     *     clip area.
     */
    ProcessingResult resolveResourceInGroupLimits() {
        Map<String, AccessInfo> publishedAccessByRole = new HashMap<>();
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        for (GrantedAuthority authority : authorities) {
            RuleFilter filter =
                    ruleFilterByRole(authority, instanceName, workspace, layer, callerIp);
            AccessInfo accessInfo = ruleService.getAccessInfo(filter);
            if (accessInfo != null) publishedAccessByRole.put(authority.getAuthority(), accessInfo);
        }
        // retrieve the AccessInfo grouped by role
        ListMultimap<String, AccessInfo> groupsByRoleAccess =
                collectContainersAccessInfoByRole(authorities);
        // first we restrict the access
        ListMultimap<RestrictionType, ProcessingResult> restrictionResults =
                intersectAccesses(publishedAccessByRole, groupsByRoleAccess);
        // enlarge and return the result
        return unionAccesses(restrictionResults);
    }

    private ProcessingResult unionAccesses(
            ListMultimap<RestrictionType, ProcessingResult> restrictionResults) {
        // get all the results
        List<ProcessingResult> both = restrictionResults.get(RestrictionType.GROUP_BOTH);
        List<ProcessingResult> intersectsOnly =
                restrictionResults.get(RestrictionType.GROUP_INTERSECT);
        List<ProcessingResult> clipOnly = restrictionResults.get(RestrictionType.GROUP_CLIP);
        List<ProcessingResult> none = restrictionResults.get(RestrictionType.NONE);

        // enlarge each result type separately
        ProcessingResult intersectProcess = enlargeGroupProcessingResult(intersectsOnly);
        ProcessingResult clipProcess = enlargeGroupProcessingResult(clipOnly);
        ProcessingResult bothProcess = enlargeGroupProcessingResult(both);
        ProcessingResult noneProcess = enlargeGroupProcessingResult(none);

        // do the final merge
        Geometry intersectArea = null;
        Geometry clipArea = null;
        CatalogModeDTO catalogModeDTO = null;
        if (intersectProcess != null) {
            intersectArea = intersectProcess.getIntersectArea();
            catalogModeDTO = getLarger(null, intersectProcess.getCatalogModeDTO());
        }
        if (clipProcess != null) {
            clipArea = clipProcess.getClipArea();
            catalogModeDTO = getLarger(catalogModeDTO, clipProcess.getCatalogModeDTO());
        }
        // if we are in context of direct access to a layer
        // we apply limit only if none of the group has null limits.
        boolean lessRestrictive = groupSummaries != null;
        if (bothProcess != null) {
            intersectArea =
                    unionOrReturnIfNull(
                            () -> bothProcess.getIntersectArea(), intersectArea, lessRestrictive);
            clipArea =
                    unionOrReturnIfNull(() -> bothProcess.getClipArea(), clipArea, lessRestrictive);
            catalogModeDTO = getLarger(catalogModeDTO, bothProcess.getCatalogModeDTO());
        }
        if (noneProcess != null) {
            intersectArea =
                    unionOrReturnIfNull(
                            () -> noneProcess.getIntersectArea(), intersectArea, lessRestrictive);
            clipArea =
                    unionOrReturnIfNull(() -> noneProcess.getClipArea(), clipArea, lessRestrictive);
            catalogModeDTO = getLarger(catalogModeDTO, noneProcess.getCatalogModeDTO());
        }
        return new ProcessingResult(intersectArea, clipArea, catalogModeDTO);
    }

    private Geometry unionOrReturnIfNull(
            Supplier<Geometry> supplier, Geometry area, boolean lessRestrictive) {
        if (area != null)
            area = this.areasHelper.reprojectAndUnion(supplier.get(), area, lessRestrictive);
        else area = supplier.get();
        return area;
    }

    // enlarge a processing result
    private ProcessingResult enlargeGroupProcessingResult(List<ProcessingResult> processingResult) {
        if (processingResult == null || processingResult.isEmpty()) return null;
        CatalogModeDTO catalogModeDTO = null;
        Geometry intersectArea = null;
        Geometry clipArea = null;
        boolean lessRestrictive = groupSummaries != null;
        for (int i = 0; i < processingResult.size(); i++) {
            ProcessingResult pr = processingResult.get(i);
            catalogModeDTO = getLarger(catalogModeDTO, pr.getCatalogModeDTO());
            Geometry allowedArea = pr.getIntersectArea();
            Geometry allowedAreaClip = pr.getClipArea();
            if (i == 0) {
                intersectArea = allowedArea;
                clipArea = allowedAreaClip;
            } else {
                intersectArea =
                        areasHelper.reprojectAndUnion(intersectArea, allowedArea, lessRestrictive);
                clipArea =
                        areasHelper.reprojectAndUnion(clipArea, allowedAreaClip, lessRestrictive);
            }
        }

        return new ProcessingResult(intersectArea, clipArea, catalogModeDTO);
    }

    /**
     * Resolve all the AccessInfo beloging to the same role restricting the access for allowed areas
     * (intersection) and catalog mode.
     *
     * @param resourceAccessInfo the resource access infos by role
     * @param groupAccessInfoByRole groups access infos by role.
     * @return a Multimap<String,AccessInfo> gathering all the AccessInfo grouped by role.
     */
    private ListMultimap<RestrictionType, ProcessingResult> intersectAccesses(
            Map<String, AccessInfo> resourceAccessInfo,
            ListMultimap<String, AccessInfo> groupAccessInfoByRole) {
        ListMultimap<RestrictionType, ProcessingResult> multiMap = ArrayListMultimap.create();
        for (String key : resourceAccessInfo.keySet()) {
            List<AccessInfo> infos = groupAccessInfoByRole.get(key);
            intersectAccesses(resourceAccessInfo.get(key), infos, multiMap);
        }
        return multiMap;
    }

    /**
     * Restrict the access for all the access info being passed.
     *
     * @param resAccessInfo the resource accessInfo. Provide the base point from which resolve the
     *     container limits.
     * @param groupsAccessInfo the groups access info for same role. To be resolved on top of the
     *     resource access info.
     * @param multiMap to be filled with the result.
     */
    private void intersectAccesses(
            AccessInfo resAccessInfo,
            List<AccessInfo> groupsAccessInfo,
            ListMultimap<RestrictionType, ProcessingResult> multiMap) {
        Geometry resIntersectArea = areasHelper.parseAllowedArea(resAccessInfo.getAreaWkt());
        Geometry resClipArea = areasHelper.parseAllowedArea(resAccessInfo.getClipAreaWkt());
        CatalogModeDTO catalogMode = resAccessInfo.getCatalogMode();
        boolean groupOnIntersect = false;
        boolean groupOnClip = false;
        boolean lessRestrictive = groupSummaries != null;
        if (groupsAccessInfo != null && !groupsAccessInfo.isEmpty()) {
            for (int i = 0; i < groupsAccessInfo.size(); i++) {
                AccessInfo accessInfo = groupsAccessInfo.get(i);
                catalogMode = getStricter(catalogMode, accessInfo.getCatalogMode());
                String allowedArea = accessInfo.getAreaWkt();
                String clipAllowedArea = accessInfo.getClipAreaWkt();
                if (!groupOnIntersect) groupOnIntersect = allowedArea != null;
                if (!groupOnClip) groupOnClip = clipAllowedArea != null;
                Geometry area = areasHelper.parseAllowedArea(allowedArea);
                Geometry clipArea = areasHelper.parseAllowedArea(clipAllowedArea);

                if (i == 0) {
                    // be sure we have an initial value.
                    if (resIntersectArea == null) resIntersectArea = area;
                    else
                        resIntersectArea =
                                areasHelper.reprojectAndIntersect(
                                        resIntersectArea, area, lessRestrictive);
                    if (resClipArea == null) resClipArea = clipArea;
                    else
                        resClipArea =
                                areasHelper.reprojectAndIntersect(
                                        resClipArea, clipArea, lessRestrictive);
                } else {
                    resIntersectArea =
                            areasHelper.reprojectAndIntersect(
                                    resIntersectArea, area, lessRestrictive);
                    resClipArea =
                            areasHelper.reprojectAndIntersect(
                                    resClipArea, clipArea, lessRestrictive);
                }
            }
        }
        ProcessingResult result = new ProcessingResult(resIntersectArea, resClipArea, catalogMode);
        // dived the results according to the fact that an intersect or clip, or both or none
        // were found in the layer group. This is needed to properly handle the enlarging of access
        // to avoid layer group limit to be not considered.
        if (groupOnClip && groupOnIntersect) multiMap.put(RestrictionType.GROUP_BOTH, result);
        else if (groupOnClip) multiMap.put(RestrictionType.GROUP_CLIP, result);
        else if (groupOnIntersect) multiMap.put(RestrictionType.GROUP_INTERSECT, result);
        else multiMap.put(RestrictionType.NONE, result);
    }

    // collect the containers area by role.
    private ListMultimap<String, AccessInfo> collectContainersAccessInfoByRole(
            Collection<? extends GrantedAuthority> authorities) {
        ListMultimap<String, AccessInfo> groupAccessInfoByRole = ArrayListMultimap.create();
        if (groupSummaries == null)
            collectGroupAccessInfoByRole(groupList, authorities, groupAccessInfoByRole);
        else
            // in context of a direct access to a layer contained in some tree group
            collectGroupSummaryAccessInfoByRole(groupSummaries, authorities, groupAccessInfoByRole);
        return groupAccessInfoByRole;
    }

    // collects the AccessInfo by role for all the layer groups as summary (direct access of a
    // contained layer).
    private void collectGroupSummaryAccessInfoByRole(
            Collection<LayerGroupContainmentCache.LayerGroupSummary> summaries,
            Collection<? extends GrantedAuthority> authorities,
            ListMultimap<String, AccessInfo> groupAccessInfoByRole) {
        for (LayerGroupContainmentCache.LayerGroupSummary summary : summaries) {
            LayerGroupInfo.Mode mode = summary.getMode();
            if (!mode.equals(LayerGroupInfo.Mode.OPAQUE_CONTAINER)) {
                String layer = summary.getName();
                String workspace = summary.getWorkspace();
                // temporary map to do additional checks before adding access info
                // to multimap.
                Map<String, AccessInfo> map = new HashMap<>(authorities.size());
                int wktCounter = 0;
                for (GrantedAuthority authority : authorities) {
                    RuleFilter filter =
                            ruleFilterByRole(authority, instanceName, workspace, layer, callerIp);
                    AccessInfo accessInfo = ruleService.getAccessInfo(filter);
                    if (accessInfo.getAreaWkt() != null || accessInfo.getClipAreaWkt() != null)
                        wktCounter++;
                    map.put(authority.getAuthority(), accessInfo);
                }
                // if not all the access info had a wkt area of any type
                // we remove the one without the area to allow the enlarging of access
                // when one of the named tree involved doesn't have any area defined
                if (wktCounter != 0 && wktCounter < map.size())
                    map.values()
                            .removeIf(v -> v.getClipAreaWkt() == null && v.getAreaWkt() == null);
                map.keySet().forEach(k -> groupAccessInfoByRole.put(k, map.get(k)));
            }
        }
    }

    // collects group AccessInfo by Role (when layer group is requested)
    private void collectGroupAccessInfoByRole(
            List<LayerGroupInfo> groupList,
            Collection<? extends GrantedAuthority> authorities,
            ListMultimap<String, AccessInfo> groupAccessInfoByRole) {
        for (LayerGroupInfo group : groupList) {
            String[] nameParts = group.prefixedName().split(":");
            String layer = null;
            String workspace = null;
            if (nameParts.length == 1) {
                layer = nameParts[0];
            } else {
                workspace = nameParts[0];
                layer = nameParts[1];
            }
            for (GrantedAuthority authority : authorities) {
                RuleFilter filter =
                        ruleFilterByRole(authority, instanceName, workspace, layer, callerIp);
                AccessInfo accessInfo = ruleService.getAccessInfo(filter);
                groupAccessInfoByRole.put(authority.getAuthority(), accessInfo);
            }
        }
    }

    private CatalogModeDTO getStricter(CatalogModeDTO m1, CatalogModeDTO m2) {

        if (m1 == null) return m2;
        if (m2 == null) return m1;

        if (CatalogModeDTO.HIDE == m1 || CatalogModeDTO.HIDE == m2) return CatalogModeDTO.HIDE;

        if (CatalogModeDTO.MIXED == m1 || CatalogModeDTO.MIXED == m2) return CatalogModeDTO.MIXED;

        return CatalogModeDTO.CHALLENGE;
    }

    private CatalogModeDTO getLarger(CatalogModeDTO m1, CatalogModeDTO m2) {

        if (m1 == null) return m2;
        if (m2 == null) return m1;

        if (CatalogModeDTO.CHALLENGE == m1 || CatalogModeDTO.CHALLENGE == m2)
            return CatalogModeDTO.CHALLENGE;

        if (CatalogModeDTO.MIXED == m1 || CatalogModeDTO.MIXED == m2) return CatalogModeDTO.MIXED;

        return CatalogModeDTO.HIDE;
    }

    /** Data class meant to return a result for the whole limit resolution. */
    static class ProcessingResult {
        private Geometry intersectArea;
        private Geometry clipArea;
        private CatalogModeDTO catalogModeDTO;

        ProcessingResult(Geometry intersectArea, Geometry clipArea, CatalogModeDTO catalogModeDTO) {
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

        CatalogModeDTO getCatalogModeDTO() {
            return catalogModeDTO;
        }

        void setIntersectArea(Geometry intersectArea) {
            this.intersectArea = intersectArea;
        }

        void setClipArea(Geometry clipArea) {
            this.clipArea = clipArea;
        }
    }

    private enum RestrictionType {
        GROUP_INTERSECT,
        GROUP_CLIP,
        GROUP_BOTH,
        NONE
    }

    private RuleFilter ruleFilterByRole(
            GrantedAuthority grantedAuthority,
            String instanceName,
            String workspace,
            String layer,
            String ipAddress) {
        RuleFilter ruleFilter = new RuleFilter(RuleFilter.SpecialFilterType.ANY);
        ruleFilter.setRole(grantedAuthority.getAuthority());
        ruleFilter.setInstance(instanceName);
        ruleFilter.setUser(RuleFilter.SpecialFilterType.ANY);
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
        if (ipAddress != null) {
            ruleFilter.setSourceAddress(ipAddress);
        } else {
            LOGGER.log(Level.WARNING, "No source IP address found");
            ruleFilter.setSourceAddress(RuleFilter.SpecialFilterType.DEFAULT);
        }

        LOGGER.log(Level.FINE, "ResourceInfo filter: {0}", ruleFilter);

        return ruleFilter;
    }
}
