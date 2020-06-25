/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml.tcrs;

/** @author prushforth */
public class LatLng {

    /** */
    protected double lat;

    /** */
    protected double lng;

    /**
     * @param lat
     * @param lng
     */
    public LatLng(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;
    }

    /** @return */
    public double getLat() {
        return this.lat;
    }

    /** @return */
    public double getLng() {
        return this.lng;
    }
}
