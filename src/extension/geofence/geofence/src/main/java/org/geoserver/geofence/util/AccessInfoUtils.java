/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.geofence.core.services.dto.AccessInfo;
import org.geofence.core.services.dto.AccessTypeDTO;
import org.geofence.core.services.dto.CatalogModeDTO;
import org.geofence.core.services.dto.GrantTypeDTO;
import org.geofence.core.services.dto.LayerAttributeDTO;
import org.locationtech.jts.geom.Geometry;

/** @author "etj (Emanuele Tajariol @ GeoSolutions)" */
public class AccessInfoUtils {
    /**
     * @return a WPSAccessInfo where the WKT of the AccessInfo should not be taken into consideration since the
     *     geometries are more up-to-date.
     */
    public static WPSAccessInfo intersect(AccessInfo... accessInfoArr) {

        AccessInfo ret = null;
        Geometry areaRet = null;
        Geometry clipRet = null;

        for (AccessInfo accessInfo : accessInfoArr) {
            if (accessInfo.getGrant() == GrantTypeDTO.DENY) {
                return new WPSAccessInfo(AccessInfo.DENY_ALL); // shortcut
            }

            Geometry area = GeomHelper.parseWKT(accessInfo.getAreaWkt());
            Geometry clip = GeomHelper.parseWKT(accessInfo.getClipAreaWkt());

            if (ret == null) { // get first entry as base entry
                ret = accessInfo.clone();
                areaRet = area;
                clipRet = clip;
                continue;
            }

            areaRet = GeomHelper.reprojectAndIntersect(areaRet, area);
            clipRet = GeomHelper.reprojectAndIntersect(clipRet, clip);

            ret.setCatalogMode(getStricter(ret.getCatalogMode(), accessInfo.getCatalogMode()));

            // CQL (read + write)
            ret.setCqlFilterRead(intersectCQL(ret.getCqlFilterRead(), accessInfo.getCqlFilterRead()));
            ret.setCqlFilterWrite(intersectCQL(ret.getCqlFilterWrite(), accessInfo.getCqlFilterWrite()));

            // Attributes
            ret.setAttributes(intersectAttributes(ret.getAttributes(), accessInfo.getAttributes()));

            // AdminRights
            ret.setAdminRights(ret.getAdminRights() && accessInfo.getAdminRights());

            // skipping styles (only used in WMS)
        }

        return new WPSAccessInfo(ret, areaRet, clipRet);
    }

    public static String intersectCQL(String c1, String c2) {
        if (c1 == null) {
            return c2;
        }
        if (c2 == null) {
            return c1;
        }

        return "(" + c1 + ") AND (" + c2 + ")";
    }

    public static Set<LayerAttributeDTO> intersectAttributes(Set<LayerAttributeDTO> s1, Set<LayerAttributeDTO> s2) {
        if (s1 == null) {
            return s2;
        }
        if (s2 == null) {
            return s1;
        }

        Map<String, LayerAttributeDTO[]> map = new HashMap<>();
        for (LayerAttributeDTO la : s1) {
            map.put(la.getName(), new LayerAttributeDTO[] {la, null});
        }
        for (LayerAttributeDTO la : s2) {
            LayerAttributeDTO[] arr = map.computeIfAbsent(la.getName(), k -> new LayerAttributeDTO[] {null, la});
            arr[1] = la;
        }

        Set<LayerAttributeDTO> ret = new HashSet<>();
        for (LayerAttributeDTO[] arr : map.values()) {
            if (arr[0] == null) {
                ret.add(arr[1]);
            }
            if (arr[1] == null) {
                ret.add(arr[0]);
            }

            LayerAttributeDTO la = new LayerAttributeDTO();
            la.setName(arr[0].getName());
            la.setDatatype(arr[0].getDatatype());
            la.setAccess(getStricter(arr[0].getAccess(), arr[1].getAccess()));

            ret.add(la);
        }
        return ret;
    }

    public static AccessTypeDTO getStricter(AccessTypeDTO a1, AccessTypeDTO a2) {
        if (a1 == null || a2 == null) return AccessTypeDTO.NONE; // should not happen
        if (a1 == AccessTypeDTO.NONE || a2 == AccessTypeDTO.NONE) return AccessTypeDTO.NONE;
        if (a1 == AccessTypeDTO.READONLY || a2 == AccessTypeDTO.READONLY) return AccessTypeDTO.READONLY;
        return AccessTypeDTO.READWRITE;
    }

    public static CatalogModeDTO getStricter(CatalogModeDTO m1, CatalogModeDTO m2) {
        if (m1 == null) {
            return m2;
        }
        if (m2 == null) {
            return m1;
        }
        if (CatalogModeDTO.HIDE == m1 || CatalogModeDTO.HIDE == m2) {
            return CatalogModeDTO.HIDE;
        }
        if (CatalogModeDTO.MIXED == m1 || CatalogModeDTO.MIXED == m2) {
            return CatalogModeDTO.MIXED;
        }
        return CatalogModeDTO.CHALLENGE;
    }

    public static CatalogModeDTO getLarger(CatalogModeDTO m1, CatalogModeDTO m2) {
        if (m1 == null) {
            return m2;
        }
        if (m2 == null) {
            return m1;
        }
        if (CatalogModeDTO.CHALLENGE == m1 || CatalogModeDTO.CHALLENGE == m2) {
            return CatalogModeDTO.CHALLENGE;
        }
        if (CatalogModeDTO.MIXED == m1 || CatalogModeDTO.MIXED == m2) {
            return CatalogModeDTO.MIXED;
        }
        return CatalogModeDTO.HIDE;
    }

    public static class WPSAccessInfo {
        AccessInfo accessInfo;
        Geometry area;
        Geometry clip;

        public WPSAccessInfo(AccessInfo accessInfo) {
            this.accessInfo = accessInfo;
            this.area = null;
            this.clip = null;
        }

        public WPSAccessInfo(AccessInfo accessInfo, Geometry area, Geometry clip) {
            this.accessInfo = accessInfo;
            this.area = area;
            this.clip = clip;
        }

        public AccessInfo getAccessInfo() {
            return accessInfo;
        }

        public void setAccessInfo(AccessInfo accessInfo) {
            this.accessInfo = accessInfo;
        }

        public Geometry getArea() {
            return area;
        }

        public void setArea(Geometry area) {
            this.area = area;
        }

        public Geometry getClip() {
            return clip;
        }

        public void setClip(Geometry clip) {
            this.clip = clip;
        }
    }
}
