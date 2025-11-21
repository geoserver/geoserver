/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * Original from GeoServer 2.24-SNAPSHOT under GPL 2.0 license
 */
package org.geoserver.acl.plugin.accessmanager;

import java.util.List;
import java.util.Optional;
import org.geoserver.acl.authorization.AccessInfo;
import org.geoserver.acl.authorization.AccessRequest;
import org.geoserver.catalog.LayerGroupInfo;
import org.locationtech.jts.geom.Geometry;

/**
 * @author etj (Emanuele Tajariol @ GeoSolutions) - Originally as part of GeoFence's GeoServer extension
 * @author Gabriel Roldan - Camptocamp
 */
public abstract class AclWPSHelper {

    public static final AclWPSHelper NO_OP = new AclWPSHelper() {
        @Override
        public Optional<WPSAccessInfo> resolveWPSAccess(
                AccessRequest accessRequest, AccessInfo wpsAccessInfo, List<LayerGroupInfo> containers) {
            return Optional.empty();
        }
    };

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
    /**
     * Resolve limits according to running process
     *
     * @param accessWithoutProcessIdConstraints Pre-computed accessInfo for default WPS access.
     * @param containers
     * @return a WPSAccessInfo where the WKT of the AccessInfo should not be taken into consideration since the
     *     geometries are more up-to-date. Returns null if no forther resolution was computed.
     */
    public abstract Optional<WPSAccessInfo> resolveWPSAccess(
            final AccessRequest accessRequest,
            final AccessInfo accessWithoutProcessIdConstraints,
            List<LayerGroupInfo> containers);

    protected Geometry toJTS(org.geolatte.geom.Geometry<?> g) {
        return GeometryUtils.toJTS(g);
    }

    protected Geometry reprojectAndIntersect(Geometry g1, Geometry g2) {
        return GeometryUtils.reprojectAndIntersect(g1, g2);
    }
}
