/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.coverages.cis;

/** An abstract grid axis */
public abstract class Axis {

    private String type;
    private String axisLabel;

    public Axis(String type, String axisLabel) {
        this.type = type;
        this.axisLabel = axisLabel;
    }

    public String getType() {
        return type;
    }

    public String getAxisLabel() {
        return axisLabel;
    }
}
