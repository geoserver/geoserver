/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.coverages.cis;

import java.util.List;

public class GridLimits {

    private String type = "GridLimitsType";
    private String srsName;
    private List<String> axisLabels;
    private List<IndexAxis> axis;

    public GridLimits(List<String> axisLabels, List<IndexAxis> axis) {
        this.srsName = "http://www.opengis.net/def/crs/OGC/0/Index" + axisLabels.size() + "D";
        this.axisLabels = axisLabels;
        this.axis = axis;
    }

    public String getType() {
        return type;
    }

    public String getSrsName() {
        return srsName;
    }

    public List<String> getAxisLabels() {
        return axisLabels;
    }

    public List<IndexAxis> getAxis() {
        return axis;
    }
}
