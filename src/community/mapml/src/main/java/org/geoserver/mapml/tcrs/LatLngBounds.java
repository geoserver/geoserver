/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml.tcrs;

import org.geotools.geometry.jts.ReferencedEnvelope;

/** @author prushforth */
public class LatLngBounds {

    /** */
    protected LatLng southWest,

            /** */
            northEast;

    /**
     * @param sw
     * @param ne
     */
    public LatLngBounds(LatLng sw, LatLng ne) {
        this.extend(sw);
        this.extend(ne);
    }

    /**
     * @param latlng
     * @return
     */
    public final LatLngBounds extend(LatLng latlng) {
        if (southWest == null && northEast == null) {
            this.southWest = new LatLng(latlng.lat, latlng.lng);
            this.northEast = new LatLng(latlng.lat, latlng.lng);
        } else {
            LatLng sw = new LatLng(0, 0);
            LatLng ne = new LatLng(0, 0);
            sw.lat = southWest != null ? Math.min(latlng.lat, this.southWest.lat) : latlng.lat;
            sw.lng = southWest != null ? Math.min(latlng.lng, this.southWest.lng) : latlng.lng;

            ne.lat = northEast != null ? Math.max(latlng.lat, this.northEast.lat) : latlng.lat;
            ne.lng = northEast != null ? Math.max(latlng.lng, this.northEast.lng) : latlng.lng;
            this.southWest = sw;
            this.northEast = ne;
        }
        return this;
    }

    /** @param e */
    public LatLngBounds(ReferencedEnvelope e) {
        this.setSouthWest(new LatLng(e.getMinY(), e.getMinX()));
        this.setNorthEast(new LatLng(e.getMaxY(), e.getMaxX()));
    }

    /** @return */
    public LatLng getSouthWest() {
        return southWest;
    }

    /** @param southWest */
    public final void setSouthWest(LatLng southWest) {
        this.southWest = southWest;
    }

    /** @return */
    public LatLng getNorthEast() {
        return northEast;
    }

    /** @param northEast */
    public final void setNorthEast(LatLng northEast) {
        this.northEast = northEast;
    }

    /** @return */
    public LatLng getNorthWest() {
        return new LatLng(this.northEast.lat, this.southWest.lng);
    }

    /** @return */
    public LatLng getSouthEast() {
        return new LatLng(this.southWest.lat, this.northEast.lng);
    }

    /** @return */
    public LatLng getCentre() {
        return new LatLng(
                this.northEast.lat - this.southWest.lat, this.northEast.lng - this.southWest.lng);
    }
}
