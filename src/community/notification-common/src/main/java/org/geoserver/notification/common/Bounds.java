/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.notification.common;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;

/**
 * Store a minimal information about {@link ReferencedEnvelope}:
 *
 * @author Xandros
 */
public class Bounds {

    private Double minx;

    private Double miny;

    private Double maxx;

    private Double maxy;

    private String crs;

    public Bounds() {}

    public Bounds(ReferencedEnvelope bb) {
        this.minx = bb.getMinX();
        this.maxx = bb.getMaxX();
        this.miny = bb.getMinY();
        this.maxy = bb.getMaxY();
        this.crs = CRS.toSRS(bb.getCoordinateReferenceSystem());
    }

    public Double getMinx() {
        return minx;
    }

    public void setMinx(Double minx) {
        this.minx = minx;
    }

    public Double getMiny() {
        return miny;
    }

    public void setMiny(Double miny) {
        this.miny = miny;
    }

    public Double getMaxx() {
        return maxx;
    }

    public void setMaxx(Double maxx) {
        this.maxx = maxx;
    }

    public Double getMaxy() {
        return maxy;
    }

    public void setMaxy(Double maxy) {
        this.maxy = maxy;
    }

    public String getCrs() {
        return crs;
    }

    public void setCrs(String crs) {
        this.crs = crs;
    }

    /** Rebuilds {@link ReferencedEnvelope} from parameters */
    public ReferencedEnvelope getBb() {
        try {
            return new ReferencedEnvelope(minx, maxx, miny, maxy, CRS.decode(crs));
        } catch (Exception e) {
            return null;
        }
    }
}
