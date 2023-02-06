/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.coverages.cis;

import java.util.List;

/**
 * An irregular axis enumerates all possible direct position coordinates. Coordinates are Objects as
 * they can be both numbers and ISO times too.
 */
public class IrregularAxis extends Axis {

    private final List<?> coordinate;
    private final String uomLabel;

    public IrregularAxis(String axisLabel, List<?> coordinate, String uomLabel) {
        super("IrregularAxisType", axisLabel);
        this.coordinate = coordinate;
        this.uomLabel = uomLabel;
    }

    public List<?> getCoordinate() {
        return coordinate;
    }

    public String getUomLabel() {
        return uomLabel;
    }
}
