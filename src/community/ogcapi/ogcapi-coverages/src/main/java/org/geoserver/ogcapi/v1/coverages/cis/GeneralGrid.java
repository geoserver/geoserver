/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.coverages.cis;

import java.util.List;

/**
 * A general n-D grid is defined through a sequence of axes, each of which can be of a particular
 * axis type.
 */
public class GeneralGrid {

    private String type = "GeneralGridCoverageType";
    private String srsName;
    private List<String> axisLabels;
    private List<Axis> axis;
    private GridLimits gridLimits;

    public GeneralGrid(
            String srsName, List<String> axisLabels, List<Axis> axis, GridLimits gridLimits) {
        this.srsName = srsName;
        this.axisLabels = axisLabels;
        this.axis = axis;
        this.gridLimits = gridLimits;
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

    public List<Axis> getAxis() {
        return axis;
    }

    public GridLimits getGridLimits() {
        return gridLimits;
    }
}
