/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.geoserver.geofence.core.model.LayerAttribute;
import org.geoserver.geofence.core.model.enums.AccessType;
import org.geoserver.geofence.core.model.enums.GrantType;
import org.geoserver.geofence.services.dto.AccessInfo;
import org.geoserver.geofence.services.dto.CatalogModeDTO;
import org.locationtech.jts.geom.Geometry;

/** @author "etj (Emanuele Tajariol @ GeoSolutions)" */
public class AccessInfoUtils {
    /**
     * @return a WPSAccessInfo where the WKT of the AccessInfo should not be taken into
     *     consideration since the geometries are more up-to-date.
     */
    public static WPSAccessInfo intersect(AccessInfo... accessInfoArr) {

        AccessInfo ret = null;
        Geometry areaRet = null;
        Geometry clipRet = null;

        for (AccessInfo accessInfo : accessInfoArr) {
            if (accessInfo.getGrant() == GrantType.DENY) {
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
            ret.setCqlFilterRead(
                    intersectCQL(ret.getCqlFilterRead(), accessInfo.getCqlFilterRead()));
            ret.setCqlFilterWrite(
                    intersectCQL(ret.getCqlFilterWrite(), accessInfo.getCqlFilterWrite()));

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

    public static Set<LayerAttribute> intersectAttributes(
            Set<LayerAttribute> s1, Set<LayerAttribute> s2) {
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
            LayerAttribute[] arr =
                    map.computeIfAbsent(la.getName(), k -> new LayerAttribute[] {null, la});
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

            LayerAttribute la = new LayerAttribute();
            la.setName(arr[0].getName());
            la.setDatatype(arr[0].getDatatype());
            la.setAccess(getStricter(arr[0].getAccess(), arr[1].getAccess()));

            ret.add(la);
        }
        return ret;
    }

    public static AccessType getStricter(AccessType a1, AccessType a2) {
        if (a1 == null || a2 == null) return AccessType.NONE; // should not happen
        if (a1 == AccessType.NONE || a2 == AccessType.NONE) return AccessType.NONE;
        if (a1 == AccessType.READONLY || a2 == AccessType.READONLY) return AccessType.READONLY;
        return AccessType.READWRITE;
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
