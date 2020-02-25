/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.layer;

import java.util.Map;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.GridCoverage;
import org.geowebcache.grid.GridSet;
import org.geowebcache.grid.GridSubset;

public class DynamicGridSubset extends GridSubset {

    public DynamicGridSubset(
            GridSet gridSet,
            Map<Integer, GridCoverage> coverages,
            BoundingBox originalExtent,
            boolean fullCoverage,
            Integer minCachedZoom,
            Integer maxCachedZoom) {
        super(gridSet, coverages, originalExtent, fullCoverage, minCachedZoom, maxCachedZoom);
    }

    public DynamicGridSubset(
            GridSet gridSet,
            Map<Integer, GridCoverage> coverages,
            BoundingBox originalExtent,
            boolean fullCoverage) {
        super(gridSet, coverages, originalExtent, fullCoverage);
    }

    public DynamicGridSubset(GridSubset gridSubset) {
        super(gridSubset);
    }
}
