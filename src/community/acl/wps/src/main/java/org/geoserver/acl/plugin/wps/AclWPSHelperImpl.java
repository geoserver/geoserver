/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * Original from GeoServer 2.24-SNAPSHOT under GPL 2.0 license
 */
package org.geoserver.acl.plugin.wps;

import static org.geoserver.acl.domain.rules.GrantType.DENY;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import org.apache.commons.collections4.CollectionUtils;
import org.geoserver.acl.authorization.AccessInfo;
import org.geoserver.acl.authorization.AccessRequest;
import org.geoserver.acl.authorization.AuthorizationService;
import org.geoserver.acl.domain.rules.CatalogMode;
import org.geoserver.acl.domain.rules.GrantType;
import org.geoserver.acl.domain.rules.LayerAttribute;
import org.geoserver.acl.domain.rules.LayerAttribute.AccessType;
import org.geoserver.acl.plugin.accessmanager.AclWPSHelper;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;

/**
 * @author etj (Emanuele Tajariol @ GeoSolutions) - Originally as part of GeoFence's GeoServer extension
 * @author Gabriel Roldan - Camptocamp
 */
public class AclWPSHelperImpl extends AclWPSHelper {

    private static final Logger LOGGER = Logging.getLogger(AclWPSHelperImpl.class);

    private AuthorizationService aclAuthService;

    private WPSResourceManager wpsManager;

    private final WPSChainStatusHolder statusHolder;

    public AclWPSHelperImpl(
            WPSResourceManager wpsManager, AuthorizationService aclAuthService, WPSChainStatusHolder statusHolder) {
        this.wpsManager = wpsManager;
        this.aclAuthService = aclAuthService;
        this.statusHolder = statusHolder;
    }

    /**
     * Resolve limits according to running process
     *
     * @param accessWithoutProcessIdConstraints Pre-computed accessInfo for default WPS access.
     * @return a WPSAccessInfo where the WKT of the AccessInfo should not be taken into consideration since the
     *     geometries are more up-to-date. Returns null if no forther resolution was computed.
     */
    @Override
    public Optional<WPSAccessInfo> resolveWPSAccess(
            AccessRequest originalRequest,
            AccessInfo accessWithoutProcessIdConstraints,
            List<LayerGroupInfo> containers) {
        final boolean layerGroupsRequested = CollectionUtils.isNotEmpty(containers);
        if (layerGroupsRequested) {
            LOGGER.warning(
                    "Don't know how to deal with WPS requests for LayerGroups. Won't dive into single process limits.");
            return Optional.empty();
        }

        String execId = wpsManager.getCurrentExecutionId();
        final List<String> procNames = statusHolder.getCurrentStack(execId);

        List<AccessInfo> procAccessInfo = new LinkedList<>();

        for (String procName : procNames) {
            LOGGER.fine("Retrieving AccessInfo for proc " + procName);
            AccessRequest processAccessRequest = originalRequest.withSubfield(procName);
            AccessInfo processAccessInfo = aclAuthService.getAccessInfo(processAccessRequest);
            if (processAccessInfo.getGrant() == GrantType.DENY) {
                // shortcut: if at least one process is not allowed for current resource, do not  evaluate the other
                // procs
                LOGGER.fine(() -> "Process %s not allowed to operate on layer".formatted(procName));
                return Optional.of(new WPSAccessInfo(AccessInfo.DENY_ALL, null, null));
            }
            if (processAccessInfo.equals(accessWithoutProcessIdConstraints)) {
                // No specific rules for this proc, we're getting the generic WPS we already have
                LOGGER.fine("Skipping accessInfo for " + procName);
            } else {
                procAccessInfo.add(processAccessInfo);
            }
        }

        // if we have at least one procAccessInfo, we should not consider the main  wpsAccessInfo,
        // bc the rules generating it are also considered in the more cohomprensive procAccessInfo
        if (procAccessInfo.isEmpty()) {
            return Optional.empty();
        }
        WPSAccessInfo intersected = intersect(procAccessInfo);
        return Optional.of(intersected);
    }

    /**
     * @return a WPSAccessInfo where the WKT of the AccessInfo should not be taken into consideration since the
     *     geometries are more up-to-date.
     */
    WPSAccessInfo intersect(List<AccessInfo> accessInfoArr) {

        AccessInfo ret = null;
        Geometry areaRet = null;
        Geometry clipRet = null;

        for (AccessInfo accessInfo : accessInfoArr) {
            if (accessInfo.getGrant() == DENY) {
                return new WPSAccessInfo(AccessInfo.DENY_ALL); // shortcut
            }

            Geometry area = toJTS(accessInfo.getIntersectArea());
            Geometry clip = toJTS(accessInfo.getClipArea());

            if (ret == null) { // get first entry as base entry
                ret = accessInfo;
                areaRet = area;
                clipRet = clip;
                continue;
            }

            areaRet = reprojectAndIntersect(areaRet, area);
            clipRet = reprojectAndIntersect(clipRet, clip);

            CatalogMode stricter = CatalogMode.stricter(ret.getCatalogMode(), accessInfo.getCatalogMode());

            // CQL (read + write)
            String cqlRead = intersectCQL(ret.getCqlFilterRead(), accessInfo.getCqlFilterRead());
            String cqlWrite = intersectCQL(ret.getCqlFilterWrite(), accessInfo.getCqlFilterWrite());

            // Attributes
            Set<LayerAttribute> attributes = intersectAttributes(ret.getAttributes(), accessInfo.getAttributes());

            ret = ret.toBuilder()
                    .catalogMode(stricter)
                    .cqlFilterRead(cqlRead)
                    .cqlFilterWrite(cqlWrite)
                    .attributes(attributes)
                    .build();

            // skipping styles (only used in WMS)
        }

        return new WPSAccessInfo(ret, areaRet, clipRet);
    }

    private String intersectCQL(String c1, String c2) {
        if (c1 == null) {
            return c2;
        }
        if (c2 == null) {
            return c1;
        }

        return "(" + c1 + ") AND (" + c2 + ")";
    }

    private Set<LayerAttribute> intersectAttributes(Set<LayerAttribute> s1, Set<LayerAttribute> s2) {
        if (s1 == null) {
            return s2;
        }
        if (s2 == null) {
            return s1;
        }

        Map<String, LayerAttribute[]> map = new HashMap<>();
        for (LayerAttribute la : s1) {
            map.put(la.getName(), new LayerAttribute[] {la, null});
        }
        for (LayerAttribute la : s2) {
            LayerAttribute[] arr = map.computeIfAbsent(la.getName(), k -> new LayerAttribute[] {null, la});
            arr[1] = la;
        }

        Set<LayerAttribute> ret = new HashSet<>();
        for (LayerAttribute[] arr : map.values()) {
            if (arr[0] == null) {
                ret.add(arr[1]);
            }
            if (arr[1] == null) {
                ret.add(arr[0]);
            }

            LayerAttribute la = LayerAttribute.builder()
                    .name(arr[0].getName())
                    .dataType(arr[0].getDataType())
                    .access(AccessType.stricter(arr[0].getAccess(), arr[1].getAccess()))
                    .build();

            ret.add(la);
        }
        return ret;
    }
}
