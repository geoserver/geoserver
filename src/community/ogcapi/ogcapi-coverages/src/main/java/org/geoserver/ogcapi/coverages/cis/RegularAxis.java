/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.coverages.cis;

public class IndexAxis extends Axis {

    Integer lowerBound;
    Integer upperBound;

    public IndexAxis(String axisLabel, Integer lowerBound, Integer upperBound) {
        super("IndexAxis", axisLabel);
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }
    
    
}
