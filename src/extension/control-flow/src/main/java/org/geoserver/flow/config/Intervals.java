/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.flow.config;

/**
 * The time intervals used in rate control
 *
 * @author Andrea Aime - GeoSolutions
 */
public enum Intervals {
    s(1000),
    m(60000),
    h(3600000),
    d(86400000);

    int duration;

    private Intervals(int seconds) {
        this.duration = seconds;
    }

    public int getDuration() {
        return duration;
    }
}
